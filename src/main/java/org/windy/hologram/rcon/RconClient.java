package org.windy.hologram.rcon;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minecraft RCON 客户端。
 * <p>实现 RCON 协议（TCP），支持认证和命令执行。
 *
 * <p>协议格式（所有整数为小端 int32）：
 * <ul>
 *   <li>请求：[length][requestId][type][payload\0][\0]</li>
 *   <li>响应：[length][requestId][type][payload\0]</li>
 * </ul>
 *
 * <p>类型：
 * <ul>
 *   <li>3 = AUTH（认证）</li>
 *   <li>2 = COMMAND（执行命令）</li>
 *   <li>0 = AUTH_RESPONSE（认证响应，requestId=-1 表示失败）</li>
 * </ul>
 */
public class RconClient implements Closeable {

    private static final int TYPE_AUTH = 3;
    private static final int TYPE_COMMAND = 2;
    private static final int TYPE_AUTH_RESPONSE = 0;

    private final String host;
    private final int port;
    private final String password;
    private final int timeoutMs;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final AtomicInteger requestId = new AtomicInteger(1);
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
     * @throws IOException          连接失败
     * @throws RconAuthException    认证失败
     */
    public void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);
        socket.setSoTimeout(timeoutMs);
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        // 发送认证包
        int id = requestId.getAndIncrement();
        sendPacket(id, TYPE_AUTH, password);

        // 读取认证响应
        RconResponse response = readPacket();
        if (response.requestId != id) {
            close();
            throw new RconAuthException("RCON 认证失败: requestId 不匹配");
        }
        if (response.type == -1) {
            close();
            throw new RconAuthException("RCON 认证失败: 密码错误 (" + host + ":" + port + ")");
        }

        authenticated = true;
    }

    /**
     * 执行 RCON 命令。
     *
     * @param command 命令（不含 / 前缀）
     * @return 服务器响应文本
     * @throws IOException          通信失败
     * @throws IllegalStateException 未连接或未认证
     */
    public String execute(String command) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IllegalStateException("RCON 未连接");
        }
        if (!authenticated) {
            throw new IllegalStateException("RCON 未认证");
        }

        int id = requestId.getAndIncrement();
        sendPacket(id, TYPE_COMMAND, command);

        RconResponse response = readPacket();
        return response.payload;
    }

    /**
     * 检查连接是否存活。
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public void close() {
        authenticated = false;
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        socket = null;
        in = null;
        out = null;
    }

    // ===== 协议实现 =====

    private void sendPacket(int requestId, int type, String payload) throws IOException {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        int length = 4 + 4 + payloadBytes.length + 1 + 1;

        byte[] buf = new byte[4];
        writeIntLE(buf, length); out.write(buf);
        writeIntLE(buf, requestId); out.write(buf);
        writeIntLE(buf, type); out.write(buf);
        out.write(payloadBytes);
        out.write(0);
        out.write(0);
        out.flush();
    }

    private RconResponse readPacket() throws IOException {
        byte[] buf = new byte[4];

        in.readFully(buf);
        int length = readIntLE(buf);

        in.readFully(buf);
        int id = readIntLE(buf);

        in.readFully(buf);
        int type = readIntLE(buf);

        int payloadLen = length - 8;
        byte[] payloadBytes = new byte[Math.max(0, payloadLen)];
        if (payloadLen > 0) {
            in.readFully(payloadBytes);
        }

        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        if (!payload.isEmpty() && payload.charAt(payload.length() - 1) == '\0') {
            payload = payload.substring(0, payload.length() - 1);
        }

        return new RconResponse(id, type, payload);
    }

    private static void writeIntLE(byte[] buf, int value) {
        buf[0] = (byte) (value);
        buf[1] = (byte) (value >> 8);
        buf[2] = (byte) (value >> 16);
        buf[3] = (byte) (value >> 24);
    }

    private static int readIntLE(byte[] buf) {
        return (buf[0] & 0xFF) | ((buf[1] & 0xFF) << 8)
                | ((buf[2] & 0xFF) << 16) | ((buf[3] & 0xFF) << 24);
    }

    private static class RconResponse {
        final int requestId;
        final int type;
        final String payload;

        RconResponse(int requestId, int type, String payload) {
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
