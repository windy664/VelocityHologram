package org.windy.hologram.convertor;

/**
 * 悬浮字插件转换器接口。
 */
public interface IConvertor {

    /**
     * 从其他插件格式转换。
     *
     * @param sourcePath 源配置文件路径
     * @return true 如果转换成功
     */
    boolean convert(String sourcePath);

    /**
     * 获取转换器名称。
     */
    String getName();
}
