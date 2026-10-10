/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: LogBookGenerator32small.java
 * 
 * 描述    : 生成LogBook相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator32small.CPMConfig;


import com.nwpu.verify.entity.LogBook;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class LogBookGenerator32small {

    /**
     * charArrayToString
     *
     * @brief
     * 功能: 将char数组转换为字符串，正确处理\0字符。
     *
     * @param[in]  charArray: char数组
     * @return 转换后的字符串
     * @scope internal
     * @errno:
     * N/A
     */
    private static String charArrayToString(char[] charArray) {
        if (charArray == null) {
            return "";
        }

        // 找到第一个\0字符的位置
        int endIndex = 0;
        while (endIndex < charArray.length && charArray[endIndex] != '\0') {
            endIndex++;
        }

        // 创建字符串，只包含有效字符
        return new String(charArray, 0, endIndex);
    }

    /**
     * stringToFixedLengthBytes
     *
     * @brief
     * 功能: 将字符串转换为定长字节数组，不足部分0字节填充。
     *
     * @param[in]  str:    输入字符串
     * @param[in]  length: 目标长度
     * @return 定长字节数组，用0字节填充
     * @scope internal
     * @errno:
     * N/A
     */
    private static byte[] stringToFixedLengthBytes(String str, int length) {
        byte[] bytes = new byte[length]; // Java中byte数组默认初始化为0
        if (str != null) {
            byte[] src = str.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            System.arraycopy(src, 0, bytes, 0, Math.min(src.length, length));
        }
        return bytes;
    }

    /**
     * calculateLogBookStructSize
     *
     * @brief
     * 功能: 计算日志记事本配置结构的总字节大小每个 LogBook 结构大小为：- fLogbookName: 32 字节- fMaxMessageSize: 4 字节- fMaxNBLoggedMessages: 4 字节- fMaxNBInProgressMessages: 4 字节- fSerialNumber: 4 字节总计48字节。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 包含所有配置信息的Module对象
     * @return 日志记事本配置结构在二进制文件中的总大小
     * @scope external
     * @errno:
     * N/A
     */
    public int calculateLogBookStructSize(com.nwpu.verify.entity.Module module) {
        // 每个LogBook结构固定48字节
        // 总大小取决于Module中LogBook的数量
        if (module.getfLogBooks() == null || module.getfLogBooks().isEmpty()) {
            return 0;
        }
        return module.getfLogBooks().size() * 48;
    }

    /**
     * generateLogBookStruct
     *
     * @brief
     * 功能: 生成日志记事本配置结构的二进制内容。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 包含所有配置信息的Module对象
     * @param[in]  fieldMap:                      用于记录每个字段名及其对应二进制内容的映射
     * @return 生成LogBook 结构二进制字节数组
     * @throws IOException 如果写入失败
     * @scope external
     * @errno:
     * N/A
     */
    public byte[] generateLogBookStruct(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 如果没有LogBook配置，返回空数组
        if (module.getfLogBooks() == null || module.getfLogBooks().isEmpty()) {
            return baos.toByteArray();
        }

        for (Map.Entry<String, LogBook> entry : module.getfLogBooks().entrySet()) {
            String logBookNameKey = entry.getKey();
            LogBook logBook = entry.getValue();

            // 1. fLogbookName (32 bytes, char array, 0 byte padded)
            // 正确处理char数组到字符串的转换，然后转换0字节填充的字节数组
            String logbookName = charArrayToString(logBook.getfLogbookName());
            byte[] nameBytes = stringToFixedLengthBytes(logbookName, 32);
            baos.write(nameBytes);
            fieldMap.put("LogBook_" + logBookNameKey + "_Name", nameBytes);

            // 2. fMaxMessageSize (4 bytes, unsigned int)
            byte[] maxMsgSizeBytes = BinaryUtils.longTo4BytesLittleEndian(logBook.getfMaxMessageSize());
            baos.write(maxMsgSizeBytes);
            fieldMap.put("LogBook_" + logBookNameKey + "_MaxMessageSize", maxMsgSizeBytes);

            // 3. fMaxNBLoggedMessages (4 bytes, unsigned int)
            byte[] maxNBLoggedMessagesBytes = BinaryUtils.longTo4BytesLittleEndian(logBook.getfMaxNBLoggedMessages());
            baos.write(maxNBLoggedMessagesBytes);
            fieldMap.put("LogBook_" + logBookNameKey + "_MaxNBLoggedMessages", maxNBLoggedMessagesBytes);

            // 4. fMaxNBInProgressMessages (4 bytes, unsigned int)
            byte[] maxNBInProgressMessagesBytes = BinaryUtils
                    .longTo4BytesLittleEndian(logBook.getfMaxNBInProgressMessages());
            baos.write(maxNBInProgressMessagesBytes);
            fieldMap.put("LogBook_" + logBookNameKey + "_MaxNBInProgressMessages", maxNBInProgressMessagesBytes);

            // 5. fSerialNumber (4 bytes, unsigned int)
            byte[] serialNumberBytes = BinaryUtils.longTo4BytesLittleEndian(logBook.getfSerialNumber());
            baos.write(serialNumberBytes);
            fieldMap.put("LogBook_" + logBookNameKey + "_SerialNumber", serialNumberBytes);
        }

        return baos.toByteArray();
    }
}
