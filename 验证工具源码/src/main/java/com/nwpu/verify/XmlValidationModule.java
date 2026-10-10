/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: XmlValidationModule.java
 * 描述    : XML 文件读取、解析和校验接口。
 */
package com.nwpu.verify;

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.entity.Module;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.layers.parsing.XmlParser;
import com.nwpu.verify.layers.validation.ARINC653ConfigValidator;

import java.io.File;
import java.util.List;

/**
 * XML 文件读取解析与校验功能。
 */
final class XmlValidationModule {
    /**
     * TDD-CPMGenVerify-002：XML 文件的读取与解析。
     *
     * 读取并解析 XML 文件，对应文档中的 {@code parseModule()}。
     *
     * @param filePath XML 文件路径
     * @return XML 解析得到的模块对象
     */
    Module parseModule(String filePath) {
        RuntimeStatusModule runtime = new RuntimeStatusModule();
        runtime.startStage("XML Files Parsing");
        Log.info("Initiating XML files parsing.");

        File xmlFile = new File(filePath);
        File xmlFolder = xmlFile.getParentFile();
        if (xmlFolder != null) {
            Application.checkRequiredFiles(xmlFile, xmlFolder);
            Application.checkRedundantXmlFiles(xmlFile);
        }

        ARINC653ConfigValidator.setExecuteErrorsOnXsdFailure(true);
        Module module = null;
        try {
            module = XmlParser.parseModuleXml(Application.getCleanPath(xmlFile));
            if (module != null) {
                Log.info("CPM cfg XML file parsed successfully. File Path: "
                        + Application.getCleanPath(xmlFile));
            } else {
                Application.exitWithErrorCode(CodeConstant.ERROR_XML_PARSE_FAILED);
            }
        } catch (Exception e) {
            Application.exitWithErrorCode(CodeConstant.ERROR_XML_PARSE_FAILED);
        }

        runtime.completeStage("XML Files Parsing", module == null ? 1 : 0, true);
        return module;
    }

    /**
     * TDD-CPMGenVerify-003：XML 文件正确性校验。
     *
     * 执行 XML 正确性校验，对应文档中的 {@code validateXsdOnly()}。
     *
     * @param xmlFile XML 文件
     * @param errorMessageList 错误信息列表
     * @return XSD 校验是否通过
     */
    boolean validateXsdOnly(File xmlFile, List<String> errorMessageList) {
        RuntimeStatusModule runtime = new RuntimeStatusModule();
        runtime.startStage("XML Correctness Verification", true);
        boolean xsdValidationPassed = true;
        try {
            xsdValidationPassed = ARINC653ConfigValidator.validateXsdOnly(xmlFile, errorMessageList);
            if (!xsdValidationPassed) {
                if (Log.hasOnlyRecordableErrors()) {
                    xsdValidationPassed = true;
                } else {
                    Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED,
                            "Correctness verification for XML file Failed, File Path: ["
                                    + xmlFile.getAbsolutePath() + "]。");
                    Application.exitWithErrorCode(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED);
                }
            }

            Log.info("Correctness verification for XML file completed.");
            runtime.completeStage("XML Correctness Verification",
                    Application.countNonWarningErrors(errorMessageList));
            runtime.reportProgress(30);
        } catch (Exception e) {
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED,
                    "Correctness verification for XML file Failed, File Path: ["
                            + xmlFile.getAbsolutePath() + "]。");
            errorMessageList.add("Verification exception: " + e.getMessage());
            xsdValidationPassed = false;
            Application.exitWithErrorCode(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED);
        }
        return xsdValidationPassed;
    }

    /**
     * TDD-CPMGenVerify-004：XML 文件的一致性校验。
     *
     * 执行 XML 一致性校验，对应文档中的 {@code validateConsistencyRules()}。
     *
     * @param xmlFile XML 文件
     * @param errorMessageList 错误信息列表
     * @return 一致性校验是否通过
     */
    boolean validateConsistencyRules(File xmlFile, List<String> errorMessageList) throws java.io.IOException {
        RuntimeStatusModule runtime = new RuntimeStatusModule();
        runtime.startStage("XML Files Consistency Verification");
        Log.info("Initiating consistency validation for XML files.");
        int errorsBeforeStage = Application.countNonWarningErrors(errorMessageList);
        boolean consistencyValidationPassed = ARINC653ConfigValidator.validateConsistencyRulesWithoutCrc(
                xmlFile, errorMessageList);
        runtime.completeStage("XML Files Consistency Verification",
                Application.countNonWarningErrors(errorMessageList) - errorsBeforeStage, false);
        if (consistencyValidationPassed) {
            Log.info("XML files consistency validation completed.");
        }
        runtime.reportProgress(50);
        return consistencyValidationPassed;
    }

    /**
     * TDD-CPMGenVerify-005：XML 文件的 CRC 校验。
     *
     * 执行 XML CRC 校验，对应文档中的 {@code validateXmlCrc()}。
     *
     * @param xmlFile XML 文件
     * @param errorMessageList 错误信息列表
     * @param enableCrcCheck 是否启用 CRC 校验
     * @return XML CRC 校验是否通过
     */
    boolean validateXmlCrc(File xmlFile, List<String> errorMessageList, boolean enableCrcCheck)
            throws java.io.IOException {
        RuntimeStatusModule runtime = new RuntimeStatusModule();
        runtime.startStage("XML Files CRC Verification");
        int errorsBeforeStage = Application.countNonWarningErrors(errorMessageList);
        boolean xmlCrcValidationPassed = ARINC653ConfigValidator.validateXmlCrc(
                xmlFile, errorMessageList, enableCrcCheck);
        runtime.completeStage("XML Files CRC Verification",
                Application.countNonWarningErrors(errorMessageList) - errorsBeforeStage, true);
        runtime.reportProgress(60);
        return xmlCrcValidationPassed;
    }
}
