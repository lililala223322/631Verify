/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: BinReader.java
 * 
 * 描述    : 读取BIN文件内容，并转换为十六进制字符串或按行格式化的十六进制数组。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.parsing;

/* 头文件 */
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @brief
 * 功能: 提供BIN文件读取和十六进制字符串转换能力。
 */
public class BinReader {

    /**
     * readBinToString
     *
     * @brief
     * 功能: 读取整个BIN文件并转换为连续的小写十六进制字符串。
     *       处理规则如下：
     *       1. 一次性读取文件全部字节；
     *       2. 每个字节格式化为两位小写十六进制；
     *       3. 输出字符串中不插入空格或换行。
     * 
     * @param[in]  bin: 待读取的BIN文件。
     * @return
     * String: BIN文件对应的连续十六进制字符串；读取失败时返回null。
     * @implements 
     * @scope external
     * @errno:
     * IOException: BIN文件读取失败。
     */
    public String readBinToString(File bin) {
        try {
            /* 读取整个文件为字节数组 */
            byte[] data = Files.readAllBytes(bin.toPath());

            /* 创建StringBuilder来构建十六进制字符串 */
            StringBuilder hexString = new StringBuilder(data.length * 2); /* 预分配空间 */

            /* 将所有字节转换为连续的十六进制字符串 */
            for (byte b : data) {
                hexString.append(String.format("%02x", b)); /* 无空格分隔 */
            }

            return hexString.toString();
        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            return null; /* 或抛出异常 */
        }
    }

    /**
     * readBinToString
     *
     * @brief
     * 功能: 读取整个BIN文件并按指定每行字节数格式化为十六进制字符串数组。
     *       处理规则如下：
     *       1. 一次性读取文件全部字节；
     *       2. 每行最多输出bytesPerLine个字节；
     *       3. 同一行内字节使用空格分隔；
     *       4. 返回值按换行拆分为字符串数组。
     * 
     * @param[in]  bin: 待读取的BIN文件。
     * @param[in]  bytesPerLine: 每行输出的字节数。
     * @return
     * String[]: 按行拆分后的十六进制字符串数组；读取失败时返回null。
     * @implements 
     * @scope external
     * @errno:
     * IOException: BIN文件读取失败。
     */
    public String[] readBinToString(File bin, int bytesPerLine){
        try {
            String filePath = bin.getPath();
            /* 读取整个文件为字节数组 */
            byte[] data = Files.readAllBytes(Paths.get(filePath));

            /* 格式化为十六进制字符串，每行固定字节数 */
            StringBuilder hexLines = new StringBuilder();
            for (int i = 0; i < data.length; i += bytesPerLine) {
                /* 每次取固定字节数 */
                int end = Math.min(i + bytesPerLine, data.length);
                byte[] line = new byte[end - i];
                System.arraycopy(data, i, line, 0, end - i);

                /* 转换为十六进制并用空格分隔 */
                StringBuilder hexData = new StringBuilder();
                for (byte b : line) {
                    hexData.append(String.format("%02x ", b));
                }

                /* 去掉最后一个多余的空格并换行 */
                hexLines.append(hexData.toString().trim()).append("\n");
            }
            /* 返回生成的十六进制字符串 */
            return hexLines.toString().split("\n");
        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            return null; /* 或抛出异常 */
        }
    }
}
