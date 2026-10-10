/**
 * 文件名称: BinaryUtils.java
 * 描述    : 提供二进制数据转换和填充处理工具。
 */
package com.nwpu.verify.layers.generation.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * 二进制生成工具类
 * 提供各种数据类型转换和处理的静态方法
 */
public class BinaryUtils {  

    /**
     * 将 long 类型的值转换为 4 字节的大端字节数组。
     * 适用于 unsigned long 在32位系统下的表示。
     * 注意：此操作会截断long的高32位，这是符合32位地址预期的。
     * @param value 需要转换的long值
     * @return 4字节的大端字节数组
     */
    public static byte[] longTo4BytesBigEndian(long value) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt((int)value).array();
    }

    /**
     * 将 int 类型的值转换为 4 字节的大端字节数组
     * @param value 需要转换的int值
     * @return 4字节的大端字节数组
     */
    public static byte[] intToBytesBigEndian(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
    }

    /**
     * 将 int 类型的值转换为 4 字节的小端字节数组
     * @param value 需要转换的int值
     * @return 4字节的小端字节数组
     */
    public static byte[] intToBytesLittleEndian(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    /**
     * 将 int 类型的值转换为 8 字节的大端字节数组
     * @param value 需要转换的int值
     * @return 8字节的大端字节数组
     */
    public static byte[] intTo8BytesBigEndian(int value) {
        return ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong((long)value).array();
    }

    /**
     * 将 int 类型的值转换为 8 字节的小端字节数组
     * @param value 需要转换的int值
     * @return 8字节的小端字节数组
     */
    public static byte[] intTo8BytesLittleEndian(int value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    /**
     * 将long转换为8字节大端字节数组
     * @param value long值
     * @return 8字节大端字节数组
     */
    public static byte[] longTo8BytesBigEndian(long value) {
        return ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(value).array();
    }

    /**
     * 将long转换为8字节小端字节数组
     * @param value long值
     * @return 8字节小端字节数组
     */
    public static byte[] longTo8BytesLittleEndian(long value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
    }

    /**
     * 将 long 类型的值转换为 4 字节的小端字节数组。
     * 适用于 unsigned long 在32位系统下的表示。
     * 注意：此操作会截断long的高32位，这是符合32位地址预期的。
     * @param value 需要转换的long值
     * @return 4字节的小端字节数组
     */
    public static byte[] longTo4BytesLittleEndian(long value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int)value).array();
    }

    /**
     * 转换字符串为定长字节数组，不足部分用指定字符填充。
     * @param str 输入字符串
     * @param length 目标长度
     * @param padChar 用于填充的字符
     * @return 定长字节数组
     */
    public static byte[] fixedLengthAscii(String str, int length, char padChar) {
        byte[] bytes = new byte[length];
        // 先用填充字符填满整个数组
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) padChar;
        }
        // 然后用实际字符串内容覆盖前面部分
        if (str != null) {
            byte[] src = str.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(src, 0, bytes, 0, Math.min(src.length, length));
        }
        return bytes;
    }

    /**
     * 将size对齐到8字节边界
     * @param size 原始大小
     * @return 对齐后的大小
     */
    public static int alignTo8Bytes(int size) {
        return (size + 7) & ~7;  // 等价于 ((size + 7) / 8) * 8
    }

    /**
     * 将size对齐到指定字节边界
     * @param size 原始大小
     * @param alignment 对齐字节数（必须是2的幂）
     * @return 对齐后的大小
     */
    public static int alignToBytes(int size, int alignment) {
        return (size + alignment - 1) & ~(alignment - 1);
    }

    /**
     * 将Direction字符串映射为数值
     * @param direction 方向字符串（"Source" 或 "Destination"）
     * @return 0表示Source，1表示Destination
     */
    public static int mapDirection(String direction) {
        if ("Source".equalsIgnoreCase(direction)) {
            return 0;
        } else if ("Destination".equalsIgnoreCase(direction)) {
            return 1;
        }
        return 0; // 默认为Source
    }

    /**
     * 将RefreshRate字符串转换为long值
     * @param refreshRate 刷新率字符串
     * @return long值，解析失败返回0
     */
    public static long mapRefreshRate(String refreshRate) {
        if (refreshRate == null || refreshRate.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(refreshRate);
        } catch (NumberFormatException e) {
            System.err.println("Warning: cannot parse RefreshRate '" + refreshRate + "', returning 0");
            return 0;
        }
    }

    /**
     * 安全地解析整数字符串，支持十六进制格式
     * @param str 待解析的字符串
     * @return 解析后的整数值，失败时返回0
     */
    public static int parseInt(String str) {
        if (str == null || str.isEmpty()) return 0;
        try {
            if (str.startsWith("0x") || str.startsWith("0X")) {
                return Integer.parseInt(str.substring(2), 16);
            }
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            System.err.println("Warning: parseInt failed, input '" + str + "', returning 0");
            return 0;
        }
    }

    /**
     * 安全地解析长整数字符串，支持十六进制格式
     * @param str 待解析的字符串
     * @return 解析后的长整数值，失败时返回0
     */
    public static long parseLong(String str) {
        if (str == null || str.isEmpty()) return 0L;
        try {
            if (str.startsWith("0x") || str.startsWith("0X")) {
                return Long.parseLong(str.substring(2), 16);
            }
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            System.err.println("Warning: parseLong failed, input '" + str + "', returning 0");
            return 0L;
        }
    }

    /**
     * 解析布尔值字符串
     * @param str 待解析的字符串
     * @return 解析后的布尔值
     */
    public static boolean parseBool(String str) {
        return "true".equalsIgnoreCase(str) || "1".equals(str);
    }

    /**
     * 计算字节数组的简单校验和
     * @param data 字节数组
     * @return 校验和
     */
    public static int calculateChecksum(byte[] data) {
        int checksum = 0;
        for (byte b : data) {
            checksum += (b & 0xFF);
        }
        return checksum;
    }

    /**
     * 将字节数组转换为十六进制字符串（用于调试）
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * 将字节数组转换为十六进制字符串（紧凑格式）
     * @param bytes 字节数组
     * @return 十六进制字符串（无空格）
     */
    public static String bytesToHexCompact(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 创建指定大小的零填充字节数组
     * @param size 数组大小
     * @return 零填充的字节数组
     */
    public static byte[] createZeroFilledArray(int size) {
        return new byte[size]; // Java中新创建的数组默认就是零填充的
    }

    /**
     * 创建指定大小和填充值的字节数组
     * @param size 数组大小
     * @param fillValue 填充值
     * @return 填充后的字节数组
     */
    public static byte[] createFilledArray(int size, byte fillValue) {
        byte[] array = new byte[size];
        for (int i = 0; i < size; i++) {
            array[i] = fillValue;
        }
        return array;
    }
}
