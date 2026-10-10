/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: BinCRCValidator.java
 * 
 * 描述    : 校验BIN文件中的CRC校验和数据完整性。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-30  开发人员  按B-05修正BIN CRC计算、固定输出文本和打开失败错误码。
 *     2026-09-04  开发人员  按004860修正BIN CRC计算：RefIn/RefOut=FALSE，XorOut=0。
 */

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;


/**
 * @brief
 * 功能: 校验BIN文件中的CRC校验和数据完整性。
 */
public class BinCRCValidator {

    private static final long POLY = 0x04C11DB7L;
    private static final long INIT = 0xFFFFFFFFL;
    private static final long XOROUT = 0x00000000L;

    /**
     * getCleanPath
     *
     * @brief
     * 功能: 获取文件的规范路径，去除路径中的冗余表示。
     * 
     * @param[in]  file: 待处理的文件对象。
     * @return
     * String: 文件规范路径；获取失败时返回绝对路径。
     * @implements 
     * @scope internal
     * @errno:
     * IOException: 获取规范路径失败时捕获并返回绝对路径。
     */
    private static String getCleanPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * validateBinCRC
     *
     * @brief
     * 功能: 使用默认大端序验证BIN文件CRC32校验码。
     * 
     * @param[in]  binFile: 待校验的BIN文件。
     * @param[out] errorMessages: 错误信息列表。
     * @param[in]  enableCrcCheck: 是否启用CRC校验。
     * @return
     * boolean: CRC校验通过或未启用校验返回true，否则返回false。
     * @implements 
     * @scope external
     * @errno:
     * ERROR_OPEN_BIN_FILE_FAILED: BIN文件打开失败；
     * ERROR_BIN_CRC_VALIDATION_FAILED: BIN文件CRC校验失败。
     */
    public static boolean validateBinCRC(File binFile, List<String> errorMessages, boolean enableCrcCheck) {
        /* 默认使用大端序（向后兼容） */
        return validateBinCRC(binFile, errorMessages, enableCrcCheck, ByteOrder.BIG_ENDIAN);
    }

    /**
     * validateBinCRC
     *
     * @brief
     * 功能: 按需求固定使用大端序验证BIN文件末尾CRC32校验码。
     *       处理流程如下：
     *       1. 校验BIN文件是否存在且长度不小于4字节；
     *       2. 读取文件末尾4字节作为期望CRC值；
     *       3. 对除末尾CRC字段外的数据计算CRC32值；
     *       4. 比对期望CRC和计算CRC，并记录运行状态。
     * 
     * @param[in]  binFile: 待校验的BIN文件。
     * @param[out] errorMessages: 错误信息列表。
     * @param[in]  enableCrcCheck: 是否启用CRC校验。
     * @param[in]  byteOrder: 兼容保留参数，CRC字段按需求固定以BIG_ENDIAN解析。
     * @return
     * boolean: CRC校验通过或未启用校验返回true，否则返回false。
     * @implements 
     * @scope external
     * @errno:
     * ERROR_OPEN_BIN_FILE_FAILED: BIN文件打开失败；
     * ERROR_BIN_CRC_VALIDATION_FAILED: BIN文件CRC校验失败。
     */
    public static boolean validateBinCRC(File binFile, List<String> errorMessages, boolean enableCrcCheck, ByteOrder byteOrder) {
        if (binFile == null || !binFile.exists()) {
            String filePath = binFile != null ? getCleanPath(binFile) : "null";
            String errorMsg = String.format("failed to open Bin File . the folder path of the file: %s .", filePath);
            errorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_OPEN_BIN_FILE_FAILED, errorMsg);
            /* 根据测试规则，1022错误必须立即终止且不输出log/report */
            com.nwpu.verify.Application.exitWithErrorCode(CodeConstant.ERROR_OPEN_BIN_FILE_FAILED);
            return false; /* 永远不会执行到这里，但保留以满足语法 */
        }

        String filePath = getCleanPath(binFile);
        /* PR_09: 使用文件名而不是完整路径 */
        String fileName = binFile.getName();


        /* 如果未启用CRC校验，输出警告并返回true */
        if (!enableCrcCheck) {
            String warningMsg = String.format(
                    "CRC validation has been disabled for the input BIN files. Proceeding without integrity verification. File Path: %s",
                    filePath);
            Log.warning(CodeConstant.WARNING_CRC_VALIDATION_DISABLED, warningMsg);
            return true;
        }

        try (RandomAccessFile raf = new RandomAccessFile(binFile, "r")) {
            long fileSize = raf.length();

            /* BIN文件至少需要4字节（CRC字段） */
            if (fileSize < 4) {
                String errorMsg = String.format(
                        "failed to open Bin File . the folder path of the file: %s .",
                        filePath);
                errorMessages.add(errorMsg);
                Log.error(CodeConstant.ERROR_OPEN_BIN_FILE_FAILED, errorMsg);
                /* 根据测试规则，1022错误必须立即终止且不输出log/report */
                com.nwpu.verify.Application.exitWithErrorCode(CodeConstant.ERROR_OPEN_BIN_FILE_FAILED);
                return false; /* 永远不会执行到这里，但保留以满足语法 */
            }

            /* 读取文件最后4字节作为期望的CRC值 */
            raf.seek(fileSize - 4);
            byte[] crcBytes = new byte[4];
            raf.readFully(crcBytes);

            /* 将字节数组转换为无符号整数（固定使用大端字节序） */
            ByteBuffer buffer = ByteBuffer.wrap(crcBytes);
            buffer.order(ByteOrder.BIG_ENDIAN);
            long expectedCrc = Integer.toUnsignedLong(buffer.getInt());
            String expectedCrcHex = String.format("%08X", expectedCrc);

            /* 读取除最后4字节外的所有内容 */
            raf.seek(0);
            long dataSize = fileSize - 4;
            byte[] fileData = new byte[(int) dataSize];
            raf.readFully(fileData);

            /* 计算CRC32（004860: RefIn/RefOut=FALSE，XorOut=0） */
            long calculatedCrc = calculateCRC32(fileData);
            String calculatedCrcHex = String.format("%08X", calculatedCrc);

            /* 比对CRC值 */
            /* PR_08 & PR_09: 修复CRC验证失败时的输出，使用文件名 */
            /* PR_41: 识别文件类型，输出CRC32比对结果到运行状态记录文件 */
            String fileType = identifyBinFileType(fileName);

            if (calculatedCrc != expectedCrc) {
                String errorMsg = String.format(
                        "BIN file failed CRC validation. CRC32: 0x%s; Computed CRC32: 0x%s. File Path: %s.",
                        expectedCrcHex,
                        calculatedCrcHex,
                        filePath);
                errorMessages.add(errorMsg);
                Log.error(CodeConstant.ERROR_BIN_CRC_VALIDATION_FAILED, errorMsg);

                /* PR_41: 输出CRC32比对结果到运行状态记录文件 */
                String crcCompareResult = String.format(
                        "%s CRC32 validation result: File=%s, Expected CRC32=0x%s, Computed CRC32=0x%s, Result=Failed",
                        fileType, fileName, expectedCrcHex, calculatedCrcHex);
                Log.info(crcCompareResult);

                /* 根据测试规则，3010错误必须立即终止且不输出log/report */
                com.nwpu.verify.Application.exitWithErrorCode(CodeConstant.ERROR_BIN_CRC_VALIDATION_FAILED);
                return false; /* 永远不会执行到这里，但保留以满足语法 */
            } else {
                /* PR_22: CRC验证通过时输出到日志 */
                /* PR_20: 修改输出格式为 "BIN file passed CRC validation.File Path:文件路径" */
                String successMsg = String.format("BIN file passed CRC validation. File Path: %s", filePath);
                Log.info(successMsg);

                /* PR_41: 输出CRC32比对结果到运行状态记录文件 */
                String crcCompareResult = String.format(
                        "%s CRC32 validation result: File=%s, Expected CRC32=0x%s, Computed CRC32=0x%s, Result=Passed",
                        fileType, fileName, expectedCrcHex, calculatedCrcHex);
                Log.info(crcCompareResult);

                return true;
            }

        } catch (IOException e) {
            String errorMsg = String.format(
                    "failed to open Bin File . the folder path of the file: %s .",
                    filePath);
            errorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_OPEN_BIN_FILE_FAILED, errorMsg);
            /* 根据测试规则，1022错误必须立即终止且不输出log/report */
            com.nwpu.verify.Application.exitWithErrorCode(CodeConstant.ERROR_OPEN_BIN_FILE_FAILED);
            return false; /* 永远不会执行到这里，但保留以满足语法 */
        }
    }

    /**
     * calculateCRC32
     *
     * @brief
     * 功能: 计算输入数据的CRC32校验值（TR_PR_001）。
     *       算法参数如下：
     *       POLY   0x04C11DB7；
     *       INIT   0xFFFFFFFF；
     *       RefIn  FALSE；
     *       RefOut FALSE；
     *       XorOut 0x00000000。
     * 
     * @param[in]  data: 待计算CRC的数据。
     * @return
     * long: CRC32无符号整数值。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static long calculateCRC32(byte[] data) {
         long crc = INIT;

         for (byte b : data) {
             /* RefIn=FALSE：按原始字节顺序，从最高有效位开始处理。 */
             crc ^= ((long) (b & 0xFF)) << 24;

             for (int i = 0; i < 8; i++) {
                 if ((crc & 0x80000000L) != 0) {
                     crc = (crc << 1) ^ POLY;
                 } else {
                     crc = crc << 1;
                 }
                 crc &= 0xFFFFFFFFL;
             }
         }

         /* RefOut=FALSE，不做结果位反转；最后执行XorOut。 */
         return (crc ^ XOROUT) & 0xFFFFFFFFL;
    }

    /**
     * identifyBinFileType
     *
     * @brief
     * 功能: 根据BIN文件名识别文件类型描述。
     *       映射关系如下：
     *       包含cfgrd或等于cfgrd.bin时返回CPM配置信息可加载文件；
     *       包含payloadmap或payload_map时返回CPM部署信息可加载文件；
     *       其他情况返回BIN文件。
     * 
     * @param[in]  fileName: BIN文件名。
     * @return
     * String: 文件类型描述。
     * @implements PR_41
     * @scope internal
     * @errno:
     * N/A
     */
    private static String identifyBinFileType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.contains("cfgrd") || lowerName.equals("cfgrd.bin")) {
            return "CPM Configuration Information Loadable File";
        } else if (lowerName.contains("payloadmap") || lowerName.contains("payload_map")) {
            return "CPM Deployment Information Loadable File";
        } else {
            return "BIN File";
        }
    }
}
