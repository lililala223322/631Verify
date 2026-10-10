/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: BinValidationModule.java
 * 描述    : BIN 文件 CRC 校验接口。
 */
package com.nwpu.verify;

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.layers.validation.BinCRCValidator;

import java.util.ArrayList;
import java.io.File;
import java.nio.ByteOrder;
import java.util.List;

/**
 * BIN 文件校验功能，对应设计文档中的 {@code validateBinCrc()} 接口。
 */
final class BinValidationModule {
    /**
     * TDD-CPMGenVerify-006：BIN 文件的 CRC 校验。
     *
     * 执行 BIN 文件 CRC 校验。
     *
     * @param binFiles 待校验的 BIN 文件列表
     */
    void validateBinCrc(List<File> binFiles) {
        RuntimeStatusModule runtime = new RuntimeStatusModule();
        runtime.startStage("BIN Files CRC Verification");
        Log.info("Initiating CRC validation for BIN file.");

        // BIN 文件 CRC 字段按需求固定为大端字节序读取。
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        if (binFiles.size() > 0) {
            for (File binFile : binFiles) {
                boolean valid = validateBinCrc(binFile, new ArrayList<>(), true, byteOrder);
                if (!valid) {
                    Application.exitWithErrorCode(CodeConstant.ERROR_BIN_CRC_VALIDATION_FAILED);
                }
            }
        }

        Log.info("BIN files CRC validation completed.");
        runtime.completeStage("BIN Files CRC Verification", 0);
        runtime.reportProgress(70);
    }

    /**
     * 校验单个 BIN 文件的 CRC。
     *
     * @param binFile BIN 文件
     * @param errorMessageList 错误信息列表
     * @param enableCrcCheck 是否启用 CRC 校验
     * @param byteOrder BIN 文件字节序
     * @return BIN 文件 CRC 是否通过
     */
    boolean validateBinCrc(File binFile, List<String> errorMessageList,
            boolean enableCrcCheck, ByteOrder byteOrder) {
        return BinCRCValidator.validateBinCRC(binFile, errorMessageList, enableCrcCheck, byteOrder);
    }
}
