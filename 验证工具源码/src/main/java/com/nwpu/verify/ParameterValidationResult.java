/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: ParameterValidationResult.java
 * 描述    : 保存命令行参数解析和校验结果。
 */
package com.nwpu.verify;

/**
 * 命令行参数解析结果。
 *
 * <p>该类只保存参数阶段产生的数据，不承担后续校验业务。</p>
 */
final class ParameterValidationResult {
    /** XML 配置文件路径。 */
    String inputXmlFile = "./src/main/resources/xml/CPM_L1/CPM_L1_module.xml";

    /** BIN 文件目录路径。 */
    String inputPath = "./src/main/resources/bin/OsL1";

    /** 验证报告输出目录。 */
    String outputVerifyReportFolder = "./reports";

    /** 运行状态日志输出目录。 */
    String outputLogPath = "./logs";

    /** 是否启用 XML CRC 校验。 */
    boolean enableXmlCrcCheck = true;

    /** BIN 生成器类型。 */
    String generatorType = "64big";

    /** 是否为 BIN 生成模式。 */
    boolean genMode;

    /** 是否已处理辅助命令。 */
    boolean auxiliaryCommandHandled;

    /** 参数格式是否错误。 */
    boolean parameterFormatError;

    /** 参数数量或可选参数是否错误。 */
    boolean parameterCountError;
}
