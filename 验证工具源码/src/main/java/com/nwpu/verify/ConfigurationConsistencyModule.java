/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: ConfigurationConsistencyModule.java
 * 描述    : 配置数据符合性校验和 BIN 比对接口。
 */
package com.nwpu.verify;

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.entity.ComparisonResult;
import com.nwpu.verify.entity.Module;
import com.nwpu.verify.layers.comparison.BinComparator;
import com.nwpu.verify.layers.generation.BinaryFileDispatcher;
import com.nwpu.verify.layers.parsing.BinReader;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.layers.output.XmlFieldSourceResolver;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 配置数据符合性校验功能，对应设计文档中的 {@code checkBinConfigConsistency()}。
 */
final class ConfigurationConsistencyModule {
    /**
     * TDD-CPMGenVerify-007：配置数据的符合性校验。
     *
     * 校验所有 BIN 文件并汇总字段比对结果。
     *
     * @param module XML 解析得到的模块对象
     * @param binFiles 待校验的 BIN 文件列表
     * @param xmlFile XML 配置文件
     * @param generatorType BIN 生成器类型
     * @param outputVerifyReportFolder 验证报告输出目录
     * @param sessionTimestamp 本次运行时间戳
     * @return 配置符合性校验结果
     */
    ConsistencyValidationResult checkBinConfigConsistency(Module module, List<File> binFiles,
            File xmlFile, String generatorType, String outputVerifyReportFolder,
            String sessionTimestamp) {
        ConsistencyValidationResult validationResult = new ConsistencyValidationResult();
        validationResult.hasBinFiles = module != null && !binFiles.isEmpty();

        if (module == null) {
            return validationResult;
        }

        if (binFiles.isEmpty()) {
            Log.info("No BIN files, skipping comparison");
            return validationResult;
        }

        Log.info("Initiating configuration data consistency validation.");
        BinaryFileDispatcher.setGeneratorType(generatorType);
        XmlFieldSourceResolver lineResolver = new XmlFieldSourceResolver(xmlFile);

        for (File binFile : binFiles) {
            if (!binFile.exists()) {
                Log.warning(CodeConstant.ERROR_BIN_FILE_PATH_INVALID,
                        "Binary file does not exist: " + binFile.getName());
                continue;
            }

            validationResult.processedBinFileNames.add(binFile.getName());

            BinaryFileDispatcher.GenerationResult generationResult = BinaryFileDispatcher
                    .dispatchGeneration(binFile, module);
            if (!generationResult.isSuccess()) {
                Log.error(CodeConstant.ERROR_BIN_VALIDATION_FAILED,
                        String.format("Bin file validation failed. File Path: %s",
                                Application.getCleanPath(binFile)));
                Application.exitWithErrorCode(CodeConstant.ERROR_BIN_VALIDATION_FAILED,
                        validationResult.comparisonResults, outputVerifyReportFolder, sessionTimestamp);
            }

            byte[] generatedBin = generationResult.getGeneratedData();
            Map<String, byte[]> currentFieldMap = generationResult.getFieldMap();

            // 可加载文件的配置一致性校验结果。
            checkBinConfigConsistency(generatedBin, binFile);

            BinComparator binComparator = new BinComparator();
            BinReader binReader = new BinReader();
            String binContent = binReader.readBinToString(binFile);
            List<ComparisonResult> comparisonResults = binComparator.compare(binContent, currentFieldMap);

            for (ComparisonResult comparisonResult : comparisonResults) {
                comparisonResult.setfBinaryFileName(binFile.getName());
                if ("Fail".equals(comparisonResult.getfVerificationResult())) {
                    comparisonResult.setfLine(lineResolver.resolve(
                            comparisonResult.getfBinaryFieldName(),
                            comparisonResult.getfBinaryFieldValue()));
                } else {
                    comparisonResult.setfLine("");
                }
            }
            validationResult.comparisonResults.addAll(comparisonResults);

            int currentSuccessCount = 0;
            int currentFailCount = 0;
            for (ComparisonResult comparisonResult : comparisonResults) {
                String fieldName = comparisonResult.getfBinaryFieldName();
                String verificationResult = comparisonResult.getfVerificationResult();
                if ("Success".equals(verificationResult)) {
                    currentSuccessCount++;
                } else {
                    currentFailCount++;
                    String errorMsg = String.format(
                            "Field %s verification failed: Offset = %s Actual Value = %s Expected Value = %s",
                            fieldName,
                            comparisonResult.getfOffset() != null ? comparisonResult.getfOffset() : "unknown",
                            comparisonResult.getfActualValue(),
                            comparisonResult.getfBinaryFieldValue());
                    Log.error(CodeConstant.ERROR_FIELD_VERIFICATION_FAILED, errorMsg);
                    validationResult.stage7ErrorCount++;
                }
            }

            String binFilePath = Application.getCleanPath(binFile);
            if (currentFailCount > 0) {
                Log.error(CodeConstant.ERROR_BIN_VALIDATION_FAILED,
                        String.format("Bin file validation failed. File Path: %s", binFilePath));
                validationResult.stage7ErrorCount++;
                Application.exitWithErrorCode(CodeConstant.ERROR_BIN_VALIDATION_FAILED,
                        validationResult.comparisonResults, outputVerifyReportFolder, sessionTimestamp);
            } else {
                Log.info(String.format("Bin file validation passed. File Path: %s", binFilePath));
            }
        }

        for (ComparisonResult comparisonResult : validationResult.comparisonResults) {
            if ("Success".equals(comparisonResult.getfVerificationResult())) {
                validationResult.totalSuccessCount++;
            } else {
                validationResult.totalFailCount++;
            }
        }

        String verificationStatus = validationResult.totalFailCount == 0 ? "Passed" : "Failed";
        Log.infoNoPrefix("Verification Status : " + verificationStatus);
        Log.infoNoPrefix("Total Success : " + validationResult.totalSuccessCount);
        Log.infoNoPrefix("Total Fails : " + validationResult.totalFailCount);
        validationResult.validationPassed = validationResult.totalFailCount == 0;
        return validationResult;
    }

    /**
     * 文档接口：根据模块对象生成数据并校验 BIN 文件配置一致性。
     *
     * @param module XML 解析得到的模块对象
     * @param binFile 实际 BIN 文件
     * @return 配置是否一致
     */
    boolean checkBinConfigConsistency(Module module, File binFile) {
        BinaryFileDispatcher.GenerationResult result = BinaryFileDispatcher.dispatchGeneration(binFile, module);
        if (!result.isSuccess()) {
            return false;
        }
        return checkBinConfigConsistency(result.getGeneratedData(), binFile);
    }

    /**
     * 校验生成的二进制数据与实际 BIN 文件是否一致，CRC 字段不参与比较。
     *
     * @param generatedBin 生成的二进制数据
     * @param binFile 实际 BIN 文件
     * @return 配置是否一致
     */
    boolean checkBinConfigConsistency(byte[] generatedBin, File binFile) {
        try {
            byte[] actualBin = java.nio.file.Files.readAllBytes(binFile.toPath());
            if (generatedBin.length != actualBin.length) {
                return false;
            }

            int compareLength = Math.min(generatedBin.length - 4, actualBin.length - 4);
            for (int i = 0; i < compareLength; i++) {
                if (generatedBin[i] != actualBin[i]) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Log.error(CodeConstant.ERROR_BIN_VALIDATION_FAILED,
                    "Configuration consistency check failed: " + e.getMessage());
            Application.exitWithErrorCode(CodeConstant.ERROR_BIN_VALIDATION_FAILED);
            return false;
        }
    }
}
