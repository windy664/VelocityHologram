package org.windy.hologram.rcon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minecraft RCON 客户端。
 * <p>实现 RCON 协议（TCP），支持认证和命令执行。
 *
 * <p>协议格式（所有整数为小端 int32）：
 * <ul>
 *   <li>请求：[length][requestId][type][payload\0][\0]</li>
 *   <li>响应：[length][requestId][type][payload\0][\0]</li>
 * </ul>
 *
 * <p>类型：
 * <ul>
 *   <li>3 = AUTH（认证）</li>
 *   <li>2 = COMMAND（执行命令）</li>
 *   <li>2 = AUTH_RESPONSE（认证响应，requestId=-1 表示失败）</li>
 *   <li>0 = RESPONSE_VALUE（命令响应）</li>
 * </ul>
 */
public class RconClient implements Closeable {

    private static final int TYPE_RESPONSE_VALUE = 0;
    private static final int TYPE_COMMAND = 2;
    private static final int TYPE_AUTH_RESPONSE = 2;
    private static final int TYPE_AUTH = 3;
    private static final int MIN_PACKET_LENGTH = 10;
    private static final int MAX_PACKET_LENGTH = 16 * 1024 * 1024;

    private final String host;
    private final int port;
    private final String password;
    private final int timeoutMs;
    private final AtomicInteger requestId = new AtomicInteger(1);
    private final Object ioLock = new Object();

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private volatile boolean authenticated;

    public RconClient(String host, int port, String password) {
        this(host, port, password, 5000);
    }

    public RconClient(String host, int port, String password, int timeoutMs) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.timeoutMs = timeoutMs;
    }

    /**
     * 连接并认证。
     *
     * @throws IOException       连接失败
     * @throws RconAuthException 认证失败
     */
    public void connect() throws IOException {
        synchronized (ioLock) {
            closeInternal();

            Socket newSocket = new Socket();
            try {
                newSocket.connect(new InetSocketAddress(host, port), timeoutMs);
                newSocket.setSoTimeout(timeoutMs);

                socket = newSocket;
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                int id = nextRequestId();
                sendPacket(id, TYPE_AUTH, password);

                RconResponse response = readPacket();
                if (response.requestId == -1) {
                    throw new RconAuthException(
                            "RCON 认证失败: 密码错误 (" + host + ":" + port + ")"
                    );
                }
                if (response.requestId != id) {
                    throw new RconAuthException("RCON 认证失败: requestId 不匹配");
                }
                if (response.type != TYPE_AUTH_RESPONSE) {
                    throw new RconAuthException(
                            "RCON 认证失败: 响应类型无效 (" + response.type + ")"
                    );
                }

                authenticated = true;
            } catch (IOException | RuntimeException exception) {
                closeInternal();
                throw exception;
            }
        }
    }

    /**
     * 执行 RCON 命令。
     *
     * @param command 命令（不含 / 前缀）
     * @return 服务器响应文本
     * @throws IOException           通信失败
     * @throws IllegalStateException 未连接或未认证
     */
    public String execute(String command) throws IOException {
        synchronized (ioLock) {
            ensureReady();

            int id = nextRequestId();
            sendPacket(id, TYPE_COMMAND, command != null ? command : "");

            RconResponse response = readPacket();
            if (response.requestId != id) {
                throw new IOException(
                        "RCON 响应 requestId 不匹配: expected=" + id
                                + ", actual=" + response.requestId
                );
            }
            if (response.type != TYPE_RESPONSE_VALUE) {
                throw new IOException("RCON 命令响应类型无效: " + response.type);
            }
            return response.payload;
        }
    }

    /**
     * 检查连接是否存活。
     */
    public boolean isConnected() {
        Socket currentSocket = socket;
        return authenticated
                && currentSocket != null
                && currentSocket.isConnected()
                && !currentSocket.isClosed();
    }

    @Override
    public void close() {
        synchronized (ioLock) {
            closeInternal();
        }
    }

    private void ensureReady() {
        if (socket == null || socket.isClosed()) {
            throw new IllegalStateException("RCON 未连接");
        }
        if (!authenticated) {
            throw new IllegalStateException("RCON 未认证");
        }
    }

    private int nextRequestId() {
        return requestId.getAndUpdate(current ->
                current == Integer.MAX_VALUE ? 1 : current + 1
        );
    }

    private void closeInternal() {
        authenticated = false;

        try {
            if (out != null) out.close();
        } catch (IOException ignored) {
        }
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {
        }
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }

        socket = null;
        in = null;
        out = null;
    }

    private void sendPacket(int packetRequestId, int type, String payload) throws IOException {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        int length = 4 + 4 + payloadBytes.length + 2;

        writeIntLE(out, length);
        writeIntLE(out, packetRequestId);
        writeIntLE(out, type);
        out.write(payloadBytes);
        out.writeByte(0);
        out.writeByte(0);
        out.flush();
    }

    private RconResponse readPacket() throws IOException {
        int length = readIntLE(in);
        if (length < MIN_PACKET_LENGTH || length > MAX_PACKET_LENGTH) {
            throw new IOException("RCON 响应长度无效: " + length);
        }

        int id = readIntLE(in);
        int type = readIntLE(in);

        int payloadLength = length - 8;
        byte[] payloadBytes = new byte[payloadLength];
        in.readFully(payloadBytes);

        int contentLength = payloadBytes.length;
        while (contentLength > 0 && payloadBytes[contentLength - 1] == 0) {
            contentLength--;
        }

        String payload = new String(
                payloadBytes,
                0,
                contentLength,
                StandardCharsets.UTF_8
        );
        return new RconResponse(id, type, payload);
    }

    private static void writeIntLE(DataOutputStream output, int value) throws IOException {
        output.writeByte(value);
        output.writeByte(value >>> 8);
        output.writeByte(value >>> 16);
        output.writeByte(value >>> 24);
    }

    private static int readIntLE(DataInputStream input) throws IOException {
        int byte0 = input.readUnsignedByte();
        int byte1 = input.readUnsignedByte();
        int byte2 = input.readUnsignedByte();
        int byte3 = input.readUnsignedByte();

        return byte0
                | (byte1 << 8)
                | (byte2 << 16)
                | (byte3 << 24);
    }

    private static class RconResponse {
        private final int requestId;
        private final int type;
        private final String payload;

        private RconResponse(int requestId, int type, String payload) {
            this.requestId = requestId;
            this.type = type;
            this.payload = payload;
        }
    }

    /**
     * RCON 认证异常。
     */
    public static class RconAuthException extends IOException {
        public RconAuthException(String message) {
            super(message);
        }
    }
}
