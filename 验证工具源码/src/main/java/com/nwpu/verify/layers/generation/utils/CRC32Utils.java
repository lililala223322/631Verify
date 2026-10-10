/**
 * 文件名称: CRC32Utils.java
 * 描述    : 提供项目统一的 CRC32 校验计算工具。
 */
package com.nwpu.verify.layers.generation.utils;

/**
 * CRC32校验工具类
 * 
 * 使用多项式：X^32+X^26+X^23+X^22+X^16+X^12+X^11+X^10+X^8+X^7+X^5+X^4+X^2+X^1+1
 * 对应的16进制值：0x04C11DB7
 * 初始值：0xFFFFFFFF
 * 输入/输出数据反转：FALSE
 * 结果异或值：0x00000000
 */
public class CRC32Utils {
    
    // CRC32多项式：X^32+X^26+X^23+X^22+X^16+X^12+X^11+X^10+X^8+X^7+X^5+X^4+X^2+X^1+1
    private static final long CRC32_POLYNOMIAL = 0x04C11DB7L;
    
    // CRC32初始值
    private static final long CRC32_INITIAL_VALUE = 0xFFFFFFFFL;
    
    /**
     * 计算字节数组的CRC32校验码
     * 
     * @param data 要计算校验码的数据
     * @return CRC32校验码
     */
    public static long calculateCRC32(byte[] data) {
        return calculateCRC32(data, 0, data.length);
    }
    
    /**
     * 计算字节数组指定范围的CRC32校验码
     * 
     * @param data 要计算校验码的数据
     * @param offset 起始偏移量
     * @param length 数据长度
     * @return CRC32校验码
     */
    public static long calculateCRC32(byte[] data, int offset, int length) {
        long crc = CRC32_INITIAL_VALUE;

        for (int i = offset; i < offset + length; i++) {
            // RefIn=FALSE：不反转输入字节，按最高有效位方向处理。
            crc ^= ((long) (data[i] & 0xFF)) << 24;
            for (int bit = 0; bit < 8; bit++) {
                if ((crc & 0x80000000L) != 0) {
                    crc = (crc << 1) ^ CRC32_POLYNOMIAL;
                } else {
                    crc = crc << 1;
                }
                crc &= 0xFFFFFFFFL;
            }
        }

        // RefOut=FALSE，XorOut=0。
        return crc & 0xFFFFFFFFL;
    }
    
    /**
     * 将CRC32值转换为4字节的大端字节数组
     * 
     * @param crc32 CRC32值
     * @return 4字节的大端字节数组
     */
    public static byte[] crc32ToBytes(long crc32) {
        return BinaryUtils.longTo4BytesBigEndian(crc32);
    }
    
    /**
     * 验证数据的CRC32校验码
     * 
     * @param data 原始数据（不包含CRC字段）
     * @param expectedCRC 期望的CRC32值
     * @return 校验是否通过
     */
    public static boolean verifyCRC32(byte[] data, long expectedCRC) {
        long calculatedCRC = calculateCRC32(data);
        return calculatedCRC == expectedCRC;
    }
}
