/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CodeConstant.java
 * 
 * 描述    : 错误码和警告码常量定义类，定义CPM_Gen_Verify工具使用的所有错误码和警告码
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 *     2026-07-22  开发人员  调整参数校验相关错误码常量。
 *     2026-07-23  开发人员  新增XML正确性校验使用的2015、2016和2017错误码。
 *     2026-07-24  开发人员  清理新版需求中已删除的旧错误码常量。
 *     2026-07-26  开发人员  更新1018错误码的正确性校验失败描述。
 *     2026-07-26  开发人员  更新1018需求模板为完整文件路径格式。
 *     2026-07-28  开发人员  更新2017错误码模板以输出属性值和枚举取值范围。
 *     2026-07-27  开发人员  增加XML一致性校验失败汇总错误码5001。
 *     2026-07-27  开发人员  增加B-02内存、端口和共享数据一致性错误码5002-5009。
 *     2026-07-30  开发人员  增加B-03 Online、系统共享库、BmpSchedule和端口数量一致性错误码5010-5016。
 *     2026-07-30  开发人员  增加B-05 BIN打开失败错误码1022并修正BIN CRC相关错误说明。
 *     2026-08-02  开发人员  按新版需求删除已废止的1020、1021和4007错误码常量。
 */
package com.nwpu.verify.entity;

public class CodeConstant {

    // ==================== 1001-1018: 参数和文件路径相关错误 ====================

    /**
     * 错误码 1001: 输入的辅助参数不正确
     * Input auxiliary parameters error, except for -v,-h,-le.
     */
    public static final Integer ERROR_AUXILIARY_PARAM_INVALID = 1001;

    /**
     * 错误码 1002: 用户指定的参数不符合命令行格式要求
     * Invalid parameters. Use the -h command for more help.
     */
    public static final Integer ERROR_INVALID_PARAMETERS = 1002;

    /**
     * 错误码 1003: 输入参数个数不正确
     * Input parameters number error, please re-enter parameters
     */
    public static final Integer ERROR_PARAMS_COUNT_INVALID = 1003;

    /**
     * 错误码 1005: 用户指定的XML文件夹路径不存在
     * Folder path invalid: '{input_xml_cfg_folder}'. Directory does not exist.
     */
    public static final Integer ERROR_XML_FOLDER_NOT_EXIST = 1005;

    /**
     * 错误码 1006: 用户指定的XML文件夹路径目录下缺失某一文件
     * Missing required file {input_xml_file} in {input_xml_cfg_folder}. Please
     * ensure all required files exist in the directory.
     */
    public static final Integer ERROR_XML_FILE_MISSING = 1006;

    /**
     * 错误码 1007: 可加载文件路径错误
     * Input BIN file path(2nd parameter) error, please re-enter parameters.
     */
    public static final Integer ERROR_BIN_FILE_PATH_INVALID = 1007;
    /**
     * Error 1008: required BIN file is missing from the input BIN folder.
     * Missing required file [payloadMap_rom.bin, cfgRd.bin]in {input_bin_cfg_folder}.
     * Please ensure all required files exist in the directory.
     */
    public static final Integer ERROR_BIN_REQUIRED_FILE_MISSING = 1008;

    /**
     * Compatibility alias for older call sites; 1008 now means required BIN file missing.
     */
    public static final Integer ERROR_BIN_FILE_EXTENSION_INVALID = ERROR_BIN_REQUIRED_FILE_MISSING;

    /**
     * 错误码 1009: 用户指定的用于存储配置验证报告的文件夹路径不存在
     * Folder path {output_verify_report_folder} invalid: Directory does not exist.
     */
    public static final Integer ERROR_REPORT_FOLDER_NOT_EXIST = 1009;

    /**
     * 错误码 1010: 用户指定的用于存储配置验证报告的文件夹路径不可写
     * Write permission denied for folder path: '{output_verify_report_folder}'.
     */
    public static final Integer ERROR_REPORT_FOLDER_NOT_WRITABLE = 1010;

    /**
     * 错误码 1011: 输出的运行状态记录的文件夹路径格式错误
     * Output running status file path(4nd parameter) error, please re-enter
     * parameters.
     */
    public static final Integer ERROR_LOG_PATH_INVALID = 1011;

    /**
     * 错误码 1012: 用户指定的用于存储运行状态记录文件的文件夹路径不存在
     * Folder path invalid: '{output_log_folder}'. Directory does not exist.
     */
    public static final Integer ERROR_LOG_FOLDER_NOT_EXIST = 1012;

    /**
     * 错误码 1013: 用户指定的用于存储运行状态记录文件的文件夹路径不可写
     * Write permission denied for folder path: '{output_log_folder}'.
     */
    public static final Integer ERROR_LOG_FOLDER_NOT_WRITABLE = 1013;

    /**
     * 错误码 1014: 检查用户输入XML对应的同级目录下是否有冗余的XML文件
     * Redundant XML file '{fileName}' detected. The sibling directory of the
     * CPM_XX_Module.xml needs to be checked and removed.
     */
    public static final Integer ERROR_REDUNDANT_XML_FILE = 1014;

    /**
     * 错误码 1015: 检查用户指定的BIN文件夹目录下出现除必须文件列表外任何以.bin为扩展名的文件
     * Redundant BIN file '{fileName}' found in '<input_bin_cfg_folder>'. Please
     * remove it or ensure the directory only contains the required files
     * [payloadMap_rom.bin, cfgRd.bin].
     */
    public static final Integer ERROR_REDUNDANT_BIN_FILE = 1015;

    /**
     * 错误码 1016: XML文件名称不符合命名规则
     * XML file{file name} does not match name rules,rules:CPM_XX_Module.xml,the 1st
     * X means left and right ,value for L or R,the 2nd X means number,value for
     * 0-9.
     */
    public static final Integer ERROR_XML_FILENAME_INVALID_FORMAT = 1016;

    /**
     * 错误码 1017: 解析XML文件失败
     * XML file {fileName} parsing failed. the reason : {reason}.
     * 2026-08-25会议决议：2001错误码删除，CPM模块XML解析失败统一报1017。
     */
    public static final Integer ERROR_XML_PARSE_FAILED = 1017;

    /**
     * 错误码 1018: XML文件的正确性校验未通过
     * Correctness verification for XML file Failed, File Path: [{full_file_path}]。
     */
    public static final Integer ERROR_XML_XSD_VALIDATION_FAILED = 1018;

    /**
     * 错误码 1019: 读取XML文件失败
     * XML file {fileName} read failed.
     */
    public static final Integer ERROR_XML_READ_FAILED = 1019;

    /**
     * 错误码 1022: 打开输入BIN文件失败
     * failed to open Bin File . the folder path of the file: {input_bin_cfg_folder }.
     */
    public static final Integer ERROR_OPEN_BIN_FILE_FAILED = 1022;

    // ==================== 2007-2014: XML元素和属性相关错误 ====================

    /**
     * 错误码 2007: XML文件元素(元素名)下的元素(元素名)在XSD文件中未定义
     * [File: {fileName}, Line: {lineNumber}] undefined XML element {element name}.
     */
    public static final Integer ERROR_XML_UNDEFINED_ELEMENT = 2007;

    /**
     * 错误码 2008: XML文件元素属性未定义
     * [File: {fileName}, Line: {lineNumber}] undefined XML element attribute
     * {attribute name}.
     */
    public static final Integer ERROR_XML_ATTRIBUTE_UNDEFINED = 2008;

    /**
     * 错误码 2009: XML元素数量小于最小出现次数
     * [File: {fileName}, Line: {lineNumber}] element {element name} occurs
     * {actualCount} time(s) less than minimum {minOccurs} time(s).
     */
    public static final Integer ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET = 2009;

    /**
     * 错误码 2010: 元素数量大于最大出现次数
     * [File: {fileName}, Line: {lineNumber}] element {element name} occurs
     * {actualCount} time(s) greater than {maxOccurs} time(s).
     */
    public static final Integer ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED = 2010;

    /**
     * 错误码 2011: XML文件的元素属性缺失
     * [File: {fileName}, Line: {lineNumber}] XML element {element name} attribute
     * {attribute name} missing.
     */
    public static final Integer ERROR_XML_ELEMENT_ATTRIBUTE_MISSING = 2011;

    /**
     * 错误码 2012: 元素下的属性冗余
     * [File: {fileName}, Line: {lineNumber}] XML element {element name} attribute
     * {attribute name} redundancy.
     */
    public static final Integer ERROR_XML_REDUNDANCY_ATTRIBUTES = 2012;

    /**
     * 错误码 2013: 元素的属性的取值数据类型错误
     * [File: {fileName}, Line: {lineNumber}] XML element {element name} attribute
     * {attribute name} type error.
     */
    public static final Integer ERROR_XML_ATTRIBUTE_DATA_TYPE_INVALID = 2013;

    /**
     * 错误码 2014: 超出XSD约束的数据范围
     * [File: {fileName}, Line: {lineNumber}] element {element name} attribute
     * {attribute name} is out of range, range is [{min}, {max}].
     */
    public static final Integer ERROR_XML_ATTRIBUTE_VALUE_OUT_OF_RANGE = 2014;

    /**
     * 错误码 2015: XML文件缺少ICD定义的必要元素
     * [File: {fileName}, Line: {lineNumber}] Required element '{elementName}' is missing in parent element '{parentElement}'.
     */
    public static final Integer ERROR_XML_REQUIRED_ELEMENT_MISSING = 2015;

    /**
     * 错误码 2016: XML字符串属性值长度超出ICD定义范围
     * [File: {fileName}, Line: {lineNumber}] Length of {invalidValue} is not allowed
     * for attribute {attributeName} in element {elementName}. Length range:
     * {minLength} to {maxLength}.
     */
    public static final Integer ERROR_XML_ATTRIBUTE_LENGTH_INVALID = 2016;

    /**
     * 错误码 2017: XML枚举类型属性值不在ICD定义的取值范围内
     * [File: {fileName}, Line: {lineNumber}] Value {attributeValue} is not allowed
     * for attribute {attributeName} in element {elementName}. Valid values:
     * {enumValue, enumValue}.
     */
    public static final Integer ERROR_XML_ENUM_VALUE_INVALID = 2017;
    // ==================== 3001-3014: 配置约束和验证相关错误 ====================

    /**
     * 错误码 3001: 有唯一性约束的元素(元素名)的属性(属性名)的取值(属性值)不唯一
     * [File: {fileName}, Line: {lineNumber}] the element {element name} attribute
     * {attribute name} value {number} is not unique.
     */
    public static final Integer ERROR_UNIQUE_CONSTRAINT_ATTRIBUTE_NOT_UNIQUE = 3001;

    /**
     * 错误码 3002: 元素的属性的取值引用不正确
     * [File: {fileName}, Line: {lineNumber}] the element {element name} attribute
     * {attribute name} value {number} of reference relation is error.
     */
    public static final Integer ERROR_REFERENTIAL_INTEGRITY_ATTRIBUTE_INVALID = 3002;

    /**
     * 错误码 3003: 元素的属性的取值不满足页对齐
     * [File: {fileName}, Line: {lineNumber}] the element {element name} address
     * {value} configuration not page-aligned.
     */
    public static final Integer ERROR_ADDRESS_CONFIG_NOT_PAGE_ALIGNED = 3003;

    /**
     * 错误码 3004: 地址不在范围内
     * [File: {fileName}, Line: {lineNumber}] address {value} out of range, the
     * range is [{min}, {max}].
     */
    public static final Integer ERROR_ADDRESS_OUT_OF_RANGE = 3004;

    /**
     * 错误码 3005: 不同元素属性的地址和地址重叠
     * [File: {fileName}, Line: {lineNumber}] the two address intervals with
     * exclusion relation configuration is error.
     */
    public static final Integer ERROR_ADDRESS_OVERLAP = 3005;

    /**
     * 错误码 3006: XML文件的CRC校验未通过
     * XML file {fileName} failed CRC validation. CRC32: 0x[original_crc]; Computed
     * CRC32: 0x[computed_crc]. File Path: {full_file_path}.
     */
    public static final Integer ERROR_XML_CRC_VALIDATION_FAILED = 3006;


    /**
     * 错误码 3010: BIN文件CRC校验失败
     * BIN file failed CRC validation. CRC32: 0x[original_crc]; Computed CRC32:
     * 0x[computed_crc]. File Path: {full_file_path}.
     */
    public static final Integer ERROR_BIN_CRC_VALIDATION_FAILED = 3010;

    /**
     * 错误码 3011: 配置数据的某字段值匹配不一致
     * Field [field_name] verification failed: Offset = [location] Actual Value =
     * "binary_value" Expected Value = "computed_value".
     */
    public static final Integer ERROR_FIELD_VERIFICATION_FAILED = 3011;

    /**
     * 错误码 3012: 配置数据的符合性校验完成后，当BIN文件验证未通过
     * Bin file validation failed. File Path: {full_file_path}.
     */
    public static final Integer ERROR_BIN_VALIDATION_FAILED = 3012;

    /**
     * 错误码 3013: 配置验证报告生成文件失败
     * Failed to generate report file.
     */
    public static final Integer ERROR_GENERATE_REPORT_FAILED = 3013;

    /**
     * 错误码 3014: 生成运行状态文件失败
     * Failed to generate running status file.
     */
    public static final Integer ERROR_GENERATE_LOG_FAILED = 3014;

    /**
     * 错误码 5001: XML文件一致性校验失败
     * XML files failed consistency validation.
     */
    public static final Integer ERROR_XML_CONSISTENCY_VALIDATION_FAILED = 5001;


    /**
     * 错误码 5002: 分区RequiredMemorySize小于应用和共享库所需内存之和。
     */
    public static final Integer ERROR_PARTITION_REQUIRED_MEMORY_SIZE_TOO_SMALL = 5002;

    /**
     * 错误码 5003: 端口方向与端口协议类型不匹配。
     */
    public static final Integer ERROR_PORT_DIRECTION_PROTOCOL_TYPE_MISMATCH = 5003;

    /**
     * 错误码 5004: SharedData虚拟地址和partitionSpaceSize均未配置。
     */
    public static final Integer ERROR_SHARED_DATA_VIRTUAL_ADDRESS_AUTO_ALLOC_UNAVAILABLE = 5004;

    /**
     * 错误码 5005: SharedData虚拟地址不在分区空间范围内。
     */
    public static final Integer ERROR_SHARED_DATA_VIRTUAL_ADDRESS_OUT_OF_PARTITION_SPACE = 5005;

    /**
     * 错误码 5006: kernelMemoryRegion大小小于CoreOS MemorySize各段之和。
     */
    public static final Integer ERROR_KERNEL_MEMORY_REGION_SIZE_TOO_SMALL = 5006;

    /**
     * 错误码 5007: portRegion大小小于端口所需空间之和。
     */
    public static final Integer ERROR_PORT_REGION_SIZE_TOO_SMALL = 5007;

    /**
     * 错误码 5008: libRegion大小小于共享库所需空间之和。
     */
    public static final Integer ERROR_LIB_REGION_SIZE_TOO_SMALL = 5008;

    /**
     * 错误码 5009: hmLogRegion大小小于健康监控日志所需空间之和。
     */
    public static final Integer ERROR_HM_LOG_REGION_SIZE_TOO_SMALL = 5009;

    /**
     * 错误码 5010: Online分区部署缺少Base_Address属性。
     */
    public static final Integer ERROR_ONLINE_PARTITION_BASE_ADDRESS_MISSING = 5010;

    /**
     * 错误码 5011: Online分区部署地址空间不在第一个kernelRegion范围内。
     */
    public static final Integer ERROR_ONLINE_PARTITION_OUT_OF_KERNEL_REGION = 5011;

    /**
     * 错误码 5012: Online分区部署时未配置kernelRegion元素。
     */
    public static final Integer ERROR_ONLINE_KERNEL_REGION_MISSING = 5012;

    /**
     * 错误码 5013: 系统共享库名称不是pos。
     */
    public static final Integer ERROR_SYSTEM_SHARED_LIBRARY_NAME_INVALID = 5013;

    /**
     * 错误码 5014: 系统共享库不唯一。
     */
    public static final Integer ERROR_SYSTEM_SHARED_LIBRARY_NOT_UNIQUE = 5014;

    /**
     * 错误码 5015: BmpSchedule中同一分区配置到多个Core元素下。
     */
    public static final Integer ERROR_BMP_SCHEDULE_PARTITION_IN_MULTIPLE_CORES = 5015;

    /**
     * 错误码 5016: Ports下QueuingPort和SamplingPort数量之和超过5120。
     */
    public static final Integer ERROR_PORTS_COUNT_EXCEEDS_LIMIT = 5016;

    // ==================== 4001-4006: 警告码 ====================

    /**
     * 警告码 4001: 共享数据未被任何分区引用
     * [File: {fileName}, Line: {lineNumber}] configured shared data {name} not
     * used.
     */
    public static final Integer WARNING_SHARED_DATA_NOT_USED = 4001;

    /**
     * 警告码 4002: 共享库未被任何分区引用
     * [File: {fileName}, Line: {lineNumber}] configured shared lib {name} not used.
     */
    public static final Integer WARNING_SHARED_LIB_NOT_USED = 4002;

    /**
     * 警告码 4003: 已配置分区未被调度
     * [File: {fileName}, Line: {lineNumber}] configured partition {name} not
     * scheduled.
     */
    public static final Integer WARNING_PARTITION_NOT_SCHEDULED = 4003;

    /**
     * 警告码 4004: 已配置应用未被任何分区引用
     * [File: {fileName}, Line: {lineNumber}] configured app {name} not used.
     */
    public static final Integer WARNING_APP_NOT_USED = 4004;

    /**
     * 警告码 4005: XML文件的CRC属性值缺失或属性格式错误
     * XML file {fileName} CRC attribute missing or type error.
     */
    public static final Integer WARNING_XML_CRC_ATTRIBUTE_MISSING = 4005;

    /**
     * 警告码 4006: 关闭检查输入XML文件CRC校验值功能
     * CRC validation has been disabled for the input XML files. Proceeding without
     * integrity verification. File Path: {full_file_path}.
     */
    public static final Integer WARNING_CRC_VALIDATION_DISABLED = 4006;

}
