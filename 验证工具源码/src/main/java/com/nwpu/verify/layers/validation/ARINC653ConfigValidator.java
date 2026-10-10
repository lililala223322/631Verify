/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: ARINC653ConfigValidator.java
 * 
 * 描述    : 对ARINC653配置文件执行XSD、CRC和约束校验。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-22  开发人员  调整XML正确性校验相关错误码和校验结果处理。
 *     2026-07-23  开发人员  按XML正确性需求区分长度越界、枚举越界及其他结构校验错误。
 *     2026-07-24  开发人员  清理已删除的旧XSD校验错误及相关输出处理。
 *     2026-07-25  开发人员  拆分XML一致性、XML CRC和BIN CRC校验对应的流程节点。
 *     2026-07-26  开发人员  使1018使用实际主XML或被引用XML的完整路径。
 *     2026-07-26  开发人员  统一各XML正确性校验异常的1018输出格式。
 *     2026-07-27  开发人员  补充HealthMonitor子元素识别，正确报告CoreHmStatelist重复错误。
 *     2026-07-28  开发人员  更新2017枚举输出并将2014范围校验移入XML正确性阶段。
 *     2026-07-27  开发人员  按B-01迁移XML一致性校验框架，新增5001失败汇总输出。
 *     2026-07-30  开发人员  按B-04调整XML CRC关闭分支，删除关闭CRC时的校验成功和CRC属性告警输出。
 *     2026-08-01  开发人员  删除需求未规定的XML一致性校验开始及成功提示。
 */

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.layers.parsing.XmlParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @brief
 * 功能: 对ARINC653配置文件执行XSD、CRC和约束校验。
 */
public class ARINC653ConfigValidator {

    /**
     * getCleanPath
     *
     * @brief
     * 功能: 获取文件规范路径。
     * @param[in] file 文件对象。
     * @return 规范路径；获取失败时返回绝对路径。
     * @scope internal
     */
    private static String getCleanPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * isFileReadError
     *
     * @brief
     * 功能: 判断异常是否属于文件读取错误。
     * @param[in] e 异常对象。
     * @return 属于文件读取错误返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean isFileReadError(Throwable e) {
        // 检查异常类型
        if (e instanceof java.io.FileNotFoundException
                || e instanceof java.security.AccessControlException
                || e instanceof java.nio.file.AccessDeniedException) {
            return true;
        }

        // 检查完整的异常链（递归检查所有cause）
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof java.io.IOException
                    || cause instanceof java.io.FileNotFoundException
                    || cause instanceof java.security.AccessControlException
                    || cause instanceof java.nio.file.AccessDeniedException) {
                return true;
            }

            // 检查cause的消息
            String causeMessage = cause.getMessage();
            if (causeMessage != null) {
                String lowerCauseMessage = causeMessage.toLowerCase();
                if (lowerCauseMessage.contains("access denied")
                        || lowerCauseMessage.contains("permission denied")
                        || lowerCauseMessage.contains("denied")
                        || lowerCauseMessage.contains("no such file")
                        || lowerCauseMessage.contains("cannot find")
                        || lowerCauseMessage.contains("file not found")
                        || lowerCauseMessage.contains("cannot open")) {
                    return true;
                }
            }

            cause = cause.getCause();
        }

        // 检查异常消息中是否包含文件读取相关的关键词
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("access denied")
                    || lowerMessage.contains("permission denied")
                    || lowerMessage.contains("denied")
                    || lowerMessage.contains("permission")
                    || lowerMessage.contains("no such file")
                    || lowerMessage.contains("cannot find")
                    || lowerMessage.contains("file not found")
                    || lowerMessage.contains("cannot open")) {
                return true;
            }

            // PR_03: 如果异常消息看起来像是文件路径（只包含路径，没有其他错误描述）
            // 这通常意味着底层的文件访问错误被包装了，异常消息只剩下文件路径
            // 例如："E:\CPM_verify\CPMs_631\CPM_L1\APP_ATC_INSTANCE.xml"
            if (message.matches(".*[A-Za-z]:\\\\.*\\.xml.*") // Windows路径格式
                    || message.matches(".*/.*\\.xml.*")) { // Unix路径格式
                // 检查消息是否基本只包含路径（去除空格后长度接近）
                String trimmedMessage = message.trim();
                if (!trimmedMessage.contains(" ") ||
                        trimmedMessage.split("\\s+").length <= 2) {
                    // 消息中几乎没有空格或只有很少单词，很可能只是文件路径
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * translateErrorMessage
     *
     * @brief
     * 功能: 将 XML/XSD 原始错误信息转换为标准错误原因。
     * @param[in] message 原始错误信息。
     * @return 标准错误原因。
     * @scope internal
     */
    private static String translateErrorMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String lower = message.toLowerCase();

        // 1. XML File Encoding Format Error（编码相关错误）
        // 检测编码格式错误，包括无效的编码名称（如UTF-16 LE）和UTF-8编码异常
        if (lower.contains("invalid byte") || lower.contains("malformed")) {
            if (lower.contains("utf")) {
                return "XML File Encoding Format Error";
            }
            return "XML File Encoding Format Error";
        }
        // 检测无效的编码名称（如 "Invalid encoding name"）
        if (lower.contains("encoding") && (lower.contains("invalid") || lower.contains("unsupported"))) {
            return "XML File Encoding Format Error";
        }

        // 2. Incorrect Start Tag Format（起始标签格式错误）
        if (lower.contains("element type")
                && (lower.contains("must be followed") || lower.contains("not terminated"))) {
            return "Incorrect Start Tag Format";
        }
        if (lower.contains("content of elements must consist")) {
            return "Incorrect Start Tag Format";
        }
        if (lower.contains("markup") && lower.contains("preceding the root element")) {
            return "Incorrect Start Tag Format";
        }

        // 5. Incorrect closing tag name（结束标签格式错误）
        if (lower.contains("end-tag") || (lower.contains("closing tag") && lower.contains("does not match"))) {
            return "Incorrect closing tag name";
        }

        // 3. Incorrect XML header format（表头/声明错误）
        // 必须优先于根元素检测，因为XML头部错误也可能触发"root element"错误消息
        if (lower.contains("xml declaration") || lower.contains("prolog")) {
            return "Incorrect XML header format";
        }
        // 检测"根元素之前的标记"相关错误 - 这实际上是指XML声明部分
        if (lower.contains("pseudo attribute name")) {
            return "Incorrect XML header format";
        }
        // 检测处理指令相关错误（XML声明本身就是一个处理指令）
        if (lower.contains("processing instruction")) {
            // 如果明确提到"target"，可能是注释格式问题
            if (lower.contains("target") && lower.contains("matching")) {
                return "Incorrect XML comment format";
            }
            // 其他处理指令错误通常是XML头部问题
            return "Incorrect XML header format";
        }
        // 检测"content is not allowed in prolog"等前导内容错误
        if (lower.contains("content") && lower.contains("prolog")) {
            return "Incorrect XML header format";
        }
        // 检测"markup must be well-formed"相关错误
        if (lower.contains("markup") && lower.contains("well-formed")) {
            return "Incorrect XML header format";
        }

        // 4. Incorrect XML comment format（注释格式错误）
        if (lower.contains("comment") && (lower.contains("not well-formed") || lower.contains("invalid"))) {
            return "Incorrect XML comment format";
        }
        if (lower.contains("comments") && lower.contains("not permitted")) {
            return "Incorrect XML comment format";
        }

        // 内存错误（归入7种reason之一：Root element parsing exception）
        if (lower.contains("out of memory") || lower.contains("java heap space")) {
            return "Root element parsing exception";
        }

        // 6. Root element parsing exception（根元素错误）
        // 检测真正的根元素错误（如根元素外有内容）
        if (lower.contains("root element") || lower.contains("document element")
                || lower.contains("trailing section")) {
            return "Root element parsing exception";
        }

        // 7. Attribute Definition Exception（属性错误）
        if (lower.contains("attribute")
                && (lower.contains("invalid") || lower.contains("duplicate") || lower.contains("malformed")
                        || lower.contains("open quote is expected"))) {
            return "Attribute Definition Exception";
        }

        // 其他常见错误（归入7种reason之一：Root element parsing exception）
        if (lower.contains("premature end of file") || lower.contains("unexpected end of file")) {
            return "Root element parsing exception";
        }
        if (lower.contains("not well-formed")) {
            return "Root element parsing exception";
        }

        // 未匹配的场景统一归入7种reason之一，保证reason取值始终为需求表中的7种之一
        return "Root element parsing exception";
    }

    /**
     * formatXmlErrorMessage
     *
     * @brief
     * 功能: 格式化 XML 解析异常信息。
     * @param[in] e XML 解析异常。
     * @return 格式化后的错误原因。
     * @scope internal
     */
    private static String formatXmlErrorMessage(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().trim() : "";

        // 去除末尾的点和空格
        if (message != null && !message.isEmpty()) {
            message = message.replaceAll("[.\\s]+$", "");
        }

        if (message.isEmpty()) {
            message = e.getClass().getSimpleName();
        }

        // 翻译错误信息
        return translateErrorMessage(message);
    }

    // ========== 常量定义 ==========

    // XSD文件路径定义 - 相对于JAR所在目录的XSD文件夹
    private static final String XSD_FOLDER_NAME = "XSD";
    private static final String TYPE_LIB_XSD_FILE = "TypeLib.xsd";
    private static final String COREOS_XSD_FILE = "CoreOS.xsd";
    private static final String APPLICATION_XSD_FILE = "Application.xsd";
    private static final String PARTITION_XSD_FILE = "Partition.xsd";
    private static final String MODULE_XSD_FILE = "Module.xsd";
    // PR_09: 添加XInclude.xsd路径
    private static final String XINCLUDE_XSD_FILE = "XInclude.xsd";

    // XSD文件夹的绝对路径（在静态初始化时计算）
    private static String xsdFolderPath = null;

    // 根元素名称常量
    private static final String ROOT_COREOS_DESCRIPTION = "CoreOSDescription";
    private static final String ROOT_COREOS = "CoreOS";
    private static final String ROOT_MODULE = "Module";
    private static final String ROOT_MODULE_DESCRIPTION = "ModuleDescription";
    private static final String ROOT_APPLICATION_DESCRIPTION = "ApplicationDescription";
    private static final String ROOT_APPLICATION = "Application";
    private static final String ROOT_SHARED_DATA_DESCRIPTION = "SharedDataDescription";
    private static final String ROOT_SHARED_IO_DESCRIPTION = "SharedIODescription";
    private static final String ROOT_SHARED_LIBRARY_DESCRIPTION = "SharedLibraryDescription";
    private static final String ROOT_PARTITION_DESCRIPTION = "PartitionDescription";
    private static final String ROOT_PARTITION = "Partition";
    private static final String ROOT_PSEUDO_PARTITION_DESCRIPTION = "PseudoPartitionDescription";
    private static final String ROOT_PSEUDO_PARTITION = "PseudoPartition";

    // Schema实例
    private static Schema typeLibSchema;
    private static Schema coreOSSchema;
    private static Schema applicationSchema;
    private static Schema partitionSchema;
    private static Schema moduleSchema;

    // Schema初始化状态
    private static boolean isTypeLibSchemaInitialized = false;
    private static boolean isCoreOSSchemaInitialized = false;
    private static boolean isApplicationSchemaInitialized = false;
    private static boolean isPartitionSchemaInitialized = false;
    private static boolean isModuleSchemaInitialized = false;
    private static boolean isXIncludeSchemaInitialized = false;

    // PR_10/PR_11: XSD文件读取成功状态（用于区分读取成功和解析失败的情况）
    private static boolean isTypeLibXsdReadSuccess = false;
    private static boolean isCoreOSXsdReadSuccess = false;
    private static boolean isApplicationXsdReadSuccess = false;
    private static boolean isPartitionXsdReadSuccess = false;
    private static boolean isModuleXsdReadSuccess = false;
    private static boolean isXIncludeXsdReadSuccess = false;

    // PR_10: XSD文件读取失败状态（用于区分读取失败和解析失败）
    private static String typeLibReadError = null;
    private static String coreOSReadError = null;
    private static String applicationReadError = null;
    private static String partitionReadError = null;
    private static String moduleReadError = null;
    private static String xincludeReadError = null;

    // PR_11: XSD文件解析错误状态（用于区分读取成功但解析失败的情况）
    private static String typeLibParseError = null;
    private static String coreOSParseError = null;
    private static String applicationParseError = null;
    private static String partitionParseError = null;
    private static String moduleParseError = null;
    private static String xincludeParseError = null;

    // 从XSD中解析的类型约束缓存（typeName -> "min, max"）
    private static Map<String, String> typeConstraintCache = new HashMap<>();
    private static Map<String, String> enumValueCache = new HashMap<>();

    /**
     * XSD 元素出现次数声明（来自 schema 的 minOccurs/maxOccurs，不在 Java 中写死数值）。
     */
    private static final class XsdElementDecl {
        final String name;
        final String minOccurs;
        final String maxOccurs;
        final boolean required;

        XsdElementDecl(String name, String minOccurs, String maxOccurs) {
            this(name, minOccurs, maxOccurs, false);
        }

        XsdElementDecl(String name, String minOccurs, String maxOccurs, boolean optionalGroup) {
            this.name = name;
            this.minOccurs = (minOccurs == null || minOccurs.isEmpty()) ? "1" : minOccurs;
            this.maxOccurs = (maxOccurs == null || maxOccurs.isEmpty()) ? "1" : maxOccurs;
            boolean minRequired;
            try {
                minRequired = Integer.parseInt(this.minOccurs) >= 1;
            } catch (NumberFormatException ignored) {
                minRequired = false;
            }
            // choice或minOccurs=0的组合本身可以不出现，不能把其成员当作必选元素。
            this.required = !optionalGroup && minRequired;
        }
    }

    /** 父元素（或已解析到元素名的容器）-> 直接子元素声明 */
    private static final Map<String, List<XsdElementDecl>> xsdChildrenByParent = new HashMap<>();
    /** complexType 名 -> 直接子元素声明 */
    private static final Map<String, List<XsdElementDecl>> xsdChildrenByType = new HashMap<>();
    /** 父容器是否为 xs:sequence（用于区分重复超限与缺必选） */
    private static final Map<String, Boolean> xsdParentIsSequence = new HashMap<>();
    /** 元素名 -> 在各父容器中的出现声明 */
    private static final Map<String, List<XsdElementDecl>> xsdOccurrencesByElement = new HashMap<>();
    private static boolean xsdStructureIndexReady = false;

    // 验证行为控制开关
    /**
     * 当XSD验证失败时是否仍然执行错误类验证（3xxx）
     * - true (推荐): 即使XSD失败也执行错误类验证，帮助用户一次性发现所有问题
     * 例如：2007元素顺序错误和3005地址冲突错误可以同时检测出来
     * - false: XSD失败时跳过错误类验证，避免在结构错误的情况下产生误报
     */
    private static boolean executeErrorsOnXsdFailure = false;

    /**
     * 当XSD验证失败时是否仍然执行警告类验证（4xxx）
     * - true: 即使XSD失败也执行警告验证，帮助用户一次性发现所有问题
     * - false (默认): XSD失败时跳过警告验证，避免在结构错误的情况下产生误报
     */
    private static boolean executeWarningsOnXsdFailure = false;

    static {
        initializeSchemas();
    }

    /**
     * 设置当XSD验证失败时是否仍然执行错误类验证
     * 
     * @param execute true-即使XSD失败也执行错误类验证; false-XSD失败时跳过错误类验证
     */
    public static void setExecuteErrorsOnXsdFailure(boolean execute) {
        executeErrorsOnXsdFailure = execute;
    }

    /**
     * 获取当前的错误类验证执行策略
     * 
     * @return true-即使XSD失败也执行错误类验证; false-XSD失败时跳过错误类验证
     */
    public static boolean isExecuteErrorsOnXsdFailure() {
        return executeErrorsOnXsdFailure;
    }

    /**
     * 设置当XSD验证失败时是否仍然执行警告类验证
     * 
     * @param execute true-即使XSD失败也执行警告验证; false-XSD失败时跳过警告验证
     */
    public static void setExecuteWarningsOnXsdFailure(boolean execute) {
        executeWarningsOnXsdFailure = execute;
    }

    /**
     * 获取当前的警告验证执行策略
     * 
     * @return true-即使XSD失败也执行警告验证; false-XSD失败时跳过警告验证
     */
    public static boolean isExecuteWarningsOnXsdFailure() {
        return executeWarningsOnXsdFailure;
    }

    /**
     * initializeSchemas
     *
     * @brief
     * 功能: 初始化各类 ARINC653 配置文件对应的 XSD Schema。
     * @return 无。
     * @scope internal
     */
    private static void initializeSchemas() {
        try {
            // 设置默认 Locale 为英文，使 XSD 验证错误消息以英文显示
            java.util.Locale.setDefault(java.util.Locale.ENGLISH);

            // 初始化XSD文件夹路径（JAR所在目录下的XSD文件夹）
            initializeXsdFolderPath();

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // 初始化旧的schemas目录结构
            // TypeLib不需要单独验证，它被其他schema引用
            initializeSchema(factory, TYPE_LIB_XSD_FILE, "TypeLib",
                    () -> typeLibSchema = factory.newSchema(getSchemaFile(TYPE_LIB_XSD_FILE)),
                    () -> isTypeLibSchemaInitialized = true);

            // 初始化CoreOS schema
            initializeSchema(factory, COREOS_XSD_FILE, "CoreOS",
                    () -> coreOSSchema = factory.newSchema(getSchemaFile(COREOS_XSD_FILE)),
                    () -> isCoreOSSchemaInitialized = true);

            // 初始化Application schema
            initializeSchema(factory, APPLICATION_XSD_FILE, "Application",
                    () -> applicationSchema = factory.newSchema(getSchemaFile(APPLICATION_XSD_FILE)),
                    () -> isApplicationSchemaInitialized = true);

            // 初始化Partition schema
            initializeSchema(factory, PARTITION_XSD_FILE, "Partition",
                    () -> partitionSchema = factory.newSchema(getSchemaFile(PARTITION_XSD_FILE)),
                    () -> isPartitionSchemaInitialized = true);

            // 初始化Module schema
            initializeSchema(factory, MODULE_XSD_FILE, "Module",
                    () -> moduleSchema = factory.newSchema(getSchemaFile(MODULE_XSD_FILE)),
                    () -> isModuleSchemaInitialized = true);

            // PR_09: 初始化XInclude schema
            // 注：XInclude.xsd不需要作为独立Schema实例，它被其他XSD引用
            // 但为了代码一致性，使用相同的初始化模式
            initializeSchema(factory, XINCLUDE_XSD_FILE, "XInclude",
                    () -> {
                        /* XInclude不需要创建Schema实例 */ },
                    () -> isXIncludeSchemaInitialized = true);

            // 从 XSD 构建元素结构/出现次数索引，供 ErrorHandler 查询（禁止 Java 写死 ICD 次数）
            loadXsdStructureIndex();

        } catch (Exception e) {
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED,
                    "An unexpected error occurred during XSD schema loading: " + e.getMessage());
        }
    }

    /**
     * initializeSchema
     *
     * @brief
     * 功能: 初始化单个 XSD Schema 并记录读取或解析状态。
     * @param[in] factory     Schema 工厂对象。
     * @param[in] xsdFileName XSD 文件名。
     * @param[in] schemaName  Schema 逻辑名称。
     * @param[in] setter      Schema 写入回调。
     * @return 无。
     * @scope internal
     */
    private static void initializeSchema(SchemaFactory factory, String xsdFileName, String schemaName,
            SchemaInitializer initializer, Runnable onSuccess) {
        try {
            File schemaFile = getSchemaFile(xsdFileName);
            if (schemaFile != null && schemaFile.exists()) {
                // PR_10: 检查文件是否可读
                if (!schemaFile.canRead()) {
                    // 文件无法读取（权限问题）
                    setXsdReadError(schemaName, "XSD file " + schemaName + ".xsd read permission denied.");
                    return;
                }

                // PR_10/PR_11: 文件存在且可读，标记读取成功
                setXsdReadSuccess(schemaName);

                initializer.initialize();

                // PR_11: 检查 Schema 是否真的被成功创建（借鉴问题10的思路）
                // 即使没有抛出异常，如果 Schema 没有被正确初始化，也认为是解析失败
                if (!isSchemaActuallyInitialized(schemaName)) {
                    String errorMsg = "XSD file " + schemaName
                            + ".xsd parse failed. Schema initialization returned null or invalid.";
                    setXsdParseError(schemaName, errorMsg);
                    return;
                }

                onSuccess.run();
                // XSD解析成功，但不在这里输出日志
                // 因为此时日志系统可能还未初始化
            } else {
                String errorMsg = "Cannot find " + schemaName + " XSD file: " + getXsdFilePath(xsdFileName);
                setXsdReadError(schemaName, errorMsg);
            }
        } catch (SAXException e) {
            // PR_10/PR_11: 检查异常原因，区分读取错误和解析错误
            Throwable cause = e.getCause();
            if (cause instanceof java.io.IOException || cause instanceof java.io.FileNotFoundException
                    || cause instanceof java.security.AccessControlException) {
                // 文件读取相关的异常，使用读取错误
                setXsdReadError(schemaName, "XSD file " + schemaName + ".xsd read failed.");
            } else {
                // XSD解析失败
                String errorMsg = "XSD file " + schemaName + ".xsd parse failed. " + formatXmlErrorMessage(e);
                setXsdParseError(schemaName, errorMsg);
            }
        } catch (Exception e) {
            // PR_10: 捕获其他异常（包括 IOException、SecurityException 等）
            // 这些通常是文件读取相关的问题
            if (e instanceof java.io.IOException || e instanceof java.io.FileNotFoundException
                    || e instanceof java.security.AccessControlException
                    || e.getMessage() != null && (e.getMessage().contains("Access")
                            || e.getMessage().contains("denied") || e.getMessage().contains("permission"))) {
                setXsdReadError(schemaName, "XSD file " + schemaName + ".xsd read failed.");
            } else {
                // 其他未知错误，按解析错误处理
                String errorMsg = "XSD file " + schemaName + ".xsd parse failed. " + formatXmlErrorMessage(e);
                setXsdParseError(schemaName, errorMsg);
            }
        }
    }

    // PR_11: 检查 Schema 是否真的被成功初始化（借鉴问题10的思路）
    /**
     * isSchemaActuallyInitialized
     *
     * @brief
     * 功能: 执行 isSchemaActuallyInitialized 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean isSchemaActuallyInitialized(String schemaName) {
        switch (schemaName) {
            case "TypeLib":
                return typeLibSchema != null;
            case "CoreOS":
                return coreOSSchema != null;
            case "Application":
                return applicationSchema != null;
            case "Partition":
                return partitionSchema != null;
            case "Module":
                return moduleSchema != null;
            case "XInclude":
                // XInclude 不需要创建 Schema 实例，所以总是返回 true
                return true;
            default:
                return false;
        }
    }

    // PR_10: 设置XSD读取错误
    /**
     * setXsdReadError
     *
     * @brief
     * 功能: 执行 setXsdReadError 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void setXsdReadError(String schemaName, String errorMsg) {
        switch (schemaName) {
            case "TypeLib":
                typeLibReadError = errorMsg;
                break;
            case "CoreOS":
                coreOSReadError = errorMsg;
                break;
            case "Application":
                applicationReadError = errorMsg;
                break;
            case "Partition":
                partitionReadError = errorMsg;
                break;
            case "Module":
                moduleReadError = errorMsg;
                break;
            case "XInclude":
                xincludeReadError = errorMsg;
                break;
        }
    }

    // PR_10/PR_11: 设置XSD读取成功状态
    /**
     * setXsdReadSuccess
     *
     * @brief
     * 功能: 执行 setXsdReadSuccess 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void setXsdReadSuccess(String schemaName) {
        switch (schemaName) {
            case "TypeLib":
                isTypeLibXsdReadSuccess = true;
                break;
            case "CoreOS":
                isCoreOSXsdReadSuccess = true;
                break;
            case "Application":
                isApplicationXsdReadSuccess = true;
                break;
            case "Partition":
                isPartitionXsdReadSuccess = true;
                break;
            case "Module":
                isModuleXsdReadSuccess = true;
                break;
            case "XInclude":
                isXIncludeXsdReadSuccess = true;
                break;
        }
    }

    // PR_11: 设置XSD解析错误
    /**
     * setXsdParseError
     *
     * @brief
     * 功能: 执行 setXsdParseError 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void setXsdParseError(String schemaName, String errorMsg) {
        switch (schemaName) {
            case "TypeLib":
                typeLibParseError = errorMsg;
                break;
            case "CoreOS":
                coreOSParseError = errorMsg;
                break;
            case "Application":
                applicationParseError = errorMsg;
                break;
            case "Partition":
                partitionParseError = errorMsg;
                break;
            case "Module":
                moduleParseError = errorMsg;
                break;
            case "XInclude":
                xincludeParseError = errorMsg;
                break;
        }
    }

    /**
     * initializeXsdFolderPath
     *
     * @brief
     * 功能: 初始化 XSD 文件夹路径。
     * @return 无。
     * @scope internal
     */
    private static void initializeXsdFolderPath() {
        List<File> candidates = new ArrayList<>();
        try {
            URL classUrl = ARINC653ConfigValidator.class.getProtectionDomain().getCodeSource().getLocation();
            File codeSource = new File(classUrl.toURI());
            File baseDir = codeSource.isFile() ? codeSource.getParentFile() : codeSource;
            candidates.add(new File(baseDir, XSD_FOLDER_NAME));
            candidates.add(new File(baseDir, "test" + File.separator + XSD_FOLDER_NAME));
            File parent = baseDir;
            while (parent != null) {
                candidates.add(new File(parent, XSD_FOLDER_NAME));
                candidates.add(new File(parent, "test" + File.separator + XSD_FOLDER_NAME));
                parent = parent.getParentFile();
            }
        } catch (Exception ignored) {
            // 使用工作目录候选路径。
        }

        File userDir = new File(System.getProperty("user.dir"));
        candidates.add(new File(userDir, XSD_FOLDER_NAME));
        candidates.add(new File(userDir, "test" + File.separator + XSD_FOLDER_NAME));

        for (File candidate : candidates) {
            if (candidate != null && candidate.isDirectory()) {
                xsdFolderPath = candidate.getAbsolutePath();
                return;
            }
        }

        xsdFolderPath = new File(userDir, XSD_FOLDER_NAME).getAbsolutePath();
    }

    /**
     * 获取XSD文件的完整路径
     * 
     * @param xsdFileName XSD文件名
     * @return XSD文件的完整路径
     */
    private static String getXsdFilePath(String xsdFileName) {
        if (xsdFolderPath == null) {
            initializeXsdFolderPath();
        }
        return xsdFolderPath + File.separator + xsdFileName;
    }

    /**
     * 获取XSD文件对象
     * 
     * @param xsdFileName XSD文件名
     * @return XSD文件对象，如果文件不存在则返回null
     */
    private static File getSchemaFile(String xsdFileName) {
        String filePath = getXsdFilePath(xsdFileName);
        File file = new File(filePath);
        return file.exists() ? file : null;
    }

    /**
     * 获取XSD文件夹的路径（供外部调用）
     * 
     * @return XSD文件夹的绝对路径
     */
    public static String getXsdFolderPath() {
        if (xsdFolderPath == null) {
            initializeXsdFolderPath();
        }
        return xsdFolderPath;
    }

    /**
     * 从XSD文件中动态读取类型的约束范围
     * 
     * @param typeName XSD中定义的类型名称（如"TimeValueType"）
     * @return 约束范围字符串（如"1, 9223372036854775807"），如果未找到则返回null
     */
    private static String getTypeConstraintFromXsd(String typeName) {
        // 先检查缓存
        if (typeConstraintCache.containsKey(typeName)) {
            return typeConstraintCache.get(typeName);
        }

        try {
            // 从TypeLib.xsd中解析类型定义
            File typeLibXsdFile = getSchemaFile(TYPE_LIB_XSD_FILE);
            if (typeLibXsdFile == null || !typeLibXsdFile.exists()) {
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(typeLibXsdFile);

            // 查找simpleType定义
            NodeList simpleTypes = doc.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "simpleType");
            for (int i = 0; i < simpleTypes.getLength(); i++) {
                Element simpleType = (Element) simpleTypes.item(i);
                String name = simpleType.getAttribute("name");

                if (typeName.equals(name)) {
                    // 找到匹配的类型，提取约束
                    NodeList restrictions = simpleType.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
                            "restriction");
                    if (restrictions.getLength() > 0) {
                        Element restriction = (Element) restrictions.item(0);

                        // 提取minInclusive
                        String minValue = null;
                        NodeList minNodes = restriction.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
                                "minInclusive");
                        if (minNodes.getLength() > 0) {
                            minValue = ((Element) minNodes.item(0)).getAttribute("value");
                        }

                        // 提取maxInclusive
                        String maxValue = null;
                        NodeList maxNodes = restriction.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
                                "maxInclusive");
                        if (maxNodes.getLength() > 0) {
                            maxValue = ((Element) maxNodes.item(0)).getAttribute("value");
                        }

                        if (minValue != null && maxValue != null) {
                            String constraint = minValue + ", " + maxValue;
                            // 缓存结果
                            typeConstraintCache.put(typeName, constraint);
                            return constraint;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败时返回null，调用方会使用默认值
            return null;
        }

        return null;
    }


    /** Reads enumeration values for an XSD simple type from TypeLib.xsd. */
    private static String getEnumValuesFromXsd(String typeName) {
        if (typeName == null || typeName.isEmpty()) return "";
        if (enumValueCache.containsKey(typeName)) return enumValueCache.get(typeName);
        try {
            File file = getSchemaFile(TYPE_LIB_XSD_FILE);
            if (file == null || !file.exists()) return "";
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(file);
            NodeList types = doc.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "simpleType");
            for (int i = 0; i < types.getLength(); i++) {
                Element type = (Element) types.item(i);
                if (!typeName.equals(type.getAttribute("name"))) continue;
                NodeList nodes = type.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "enumeration");
                List<String> values = new ArrayList<>();
                for (int j = 0; j < nodes.getLength(); j++) {
                    String value = ((Element) nodes.item(j)).getAttribute("value");
                    if (value != null && !value.isEmpty()) values.add(value);
                }
                String result = String.join(", ", values);
                enumValueCache.put(typeName, result);
                return result;
            }
        } catch (Exception e) {
            // Keep the existing fallback when the XSD cannot be read.
        }
        return "";
    }

    private static final Map<String, String> patternConstraintCache = new HashMap<>();

    private static final Map<String, String> numericRangeCache = new HashMap<>();

    /**
     * 读取命名 simpleType（含 union 匿名成员）的 minInclusive/maxInclusive 数值范围。
     * 返回 "min, max"；无范围约束时返回空串。TR_PR_011: 数值超范围应报2014而非2017。
     */
    private static String getNumericRangeFromXsdType(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return "";
        }
        if (numericRangeCache.containsKey(typeName)) {
            return numericRangeCache.get(typeName);
        }
        String result = "";
        try {
            File file = getSchemaFile(TYPE_LIB_XSD_FILE);
            if (file != null && file.exists()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                Document doc = factory.newDocumentBuilder().parse(file);
                NodeList types = doc.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "simpleType");
                for (int i = 0; i < types.getLength(); i++) {
                    Element type = (Element) types.item(i);
                    if (!typeName.equals(type.getAttribute("name"))) {
                        continue;
                    }
                    result = readRangeFacets(type);
                    break;
                }
            }
        } catch (Exception ignored) {
            result = "";
        }
        numericRangeCache.put(typeName, result);
        return result;
    }

    /** 提取 simpleType（含 union 成员）上的 minInclusive/maxInclusive 面，返回 "min, max"。 */
    private static String readRangeFacets(Element simpleType) {
        NodeList mins = simpleType.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "minInclusive");
        NodeList maxs = simpleType.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "maxInclusive");
        if (mins.getLength() == 0 && maxs.getLength() == 0) {
            return "";
        }
        String min = mins.getLength() > 0 ? ((Element) mins.item(0)).getAttribute("value") : "";
        String max = maxs.getLength() > 0 ? ((Element) maxs.item(0)).getAttribute("value") : "";
        if (min.isEmpty() && max.isEmpty()) {
            return "";
        }
        return min + ", " + max;
    }

    /** 返回自定义基础整数联合类型所对应的 XML Schema 内置类型范围。 */
    private static String getBuiltinIntegerRange(String typeName) {
        if (typeName == null) {
            return "";
        }
        switch (typeName) {
            case "UnsignedIntValueType":
                return "0, 4294967295";
            case "UnsignedLongValueType":
            case "UnsignedLongLongValueType":
                return "0, 18446744073709551615";
            case "SignedIntValueType":
                return "-2147483648, 2147483647";
            case "SignedLongLongValueType":
                return "-9223372036854775808, 9223372036854775807";
            default:
                return "";
        }
    }

    /** Reads xs:pattern value for a named simpleType from TypeLib.xsd. */
    private static String getPatternFromXsd(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return "";
        }
        if (patternConstraintCache.containsKey(typeName)) {
            return patternConstraintCache.get(typeName);
        }
        try {
            File file = getSchemaFile(TYPE_LIB_XSD_FILE);
            if (file == null || !file.exists()) {
                return "";
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(file);
            NodeList types = doc.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "simpleType");
            for (int i = 0; i < types.getLength(); i++) {
                Element type = (Element) types.item(i);
                if (!typeName.equals(type.getAttribute("name"))) {
                    continue;
                }
                NodeList patterns = type.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "pattern");
                if (patterns.getLength() > 0) {
                    String pattern = ((Element) patterns.item(0)).getAttribute("value");
                    if (pattern == null) {
                        pattern = "";
                    }
                    patternConstraintCache.put(typeName, pattern);
                    return pattern;
                }
            }
        } catch (Exception ignored) {
            // Caller falls back to other XSD-derived text.
        }
        patternConstraintCache.put(typeName, "");
        return "";
    }

    /**
     * 从 CRC32Type 的 XSD pattern（如 0[xX][0-9A-Fa-f]{8}）推导范围展示文本。
     * 不在 Java 中写死 0x00000000/0xFFFFFFFF。
     */
    private static String getCrc32RangeDisplayFromXsd() {
        String pattern = getPatternFromXsd("CRC32Type");
        if (pattern == null || pattern.isEmpty()) {
            return "";
        }
        Matcher matcher = Pattern.compile("\\[0-9A-Fa-f\\]\\{(\\d+)\\}").matcher(pattern);
        if (!matcher.find()) {
            return pattern;
        }
        int width;
        try {
            width = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return pattern;
        }
        if (width <= 0) {
            return pattern;
        }
        StringBuilder zeros = new StringBuilder(width);
        StringBuilder maxHex = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            zeros.append('0');
            maxHex.append('F');
        }
        return "0x" + zeros + ", 0x" + maxHex;
    }

    /**
     * 枚举/合法取值展示：优先消息/枚举列表，其次 XSD pattern，再次类型名；不使用固定英文兜底句。
     */
    private static String resolveAllowedValuesDisplayFromXsd(String typeName, String enumFromMessage) {
        if (enumFromMessage != null && !enumFromMessage.isEmpty()) {
            return enumFromMessage;
        }
        String enumValues = getEnumValuesFromXsd(typeName);
        if (enumValues != null && !enumValues.isEmpty()) {
            return enumValues;
        }
        String pattern = getPatternFromXsd(typeName);
        if (pattern != null && !pattern.isEmpty()) {
            return pattern;
        }
        return typeName != null ? typeName : "";
    }

    /**
     * 从 CPM 配置 XSD 文件构建元素结构索引。
     * ErrorHandler 只能通过本索引查询 min/maxOccurs 与父子关系，不得在 Java 中写死 ICD 数值。
     */
    private static void loadXsdStructureIndex() {
        xsdChildrenByParent.clear();
        xsdChildrenByType.clear();
        xsdParentIsSequence.clear();
        xsdOccurrencesByElement.clear();
        xsdStructureIndexReady = false;

        Map<String, String> elementToType = new HashMap<>();
        Map<String, String> typeExtensionBase = new HashMap<>();
        String[] schemaFiles = new String[] {
                TYPE_LIB_XSD_FILE, COREOS_XSD_FILE, APPLICATION_XSD_FILE, PARTITION_XSD_FILE, MODULE_XSD_FILE
        };

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (String schemaFileName : schemaFiles) {
                File schemaFile = getSchemaFile(schemaFileName);
                if (schemaFile == null || !schemaFile.exists()) {
                    continue;
                }
                Document doc = builder.parse(schemaFile);
                collectNamedComplexTypes(doc, typeExtensionBase);
                collectElementTypeBindings(doc.getDocumentElement(), elementToType, null);
            }

            // 合并 xs:extension 基类型子元素
            boolean changed;
            do {
                changed = false;
                for (Map.Entry<String, String> entry : typeExtensionBase.entrySet()) {
                    String derived = entry.getKey();
                    String base = localXsdName(entry.getValue());
                    List<XsdElementDecl> baseChildren = xsdChildrenByType.get(base);
                    if (baseChildren == null || baseChildren.isEmpty()) {
                        continue;
                    }
                    List<XsdElementDecl> derivedChildren = xsdChildrenByType.computeIfAbsent(derived,
                            k -> new ArrayList<>());
                    Set<String> existing = new HashSet<>();
                    for (XsdElementDecl decl : derivedChildren) {
                        existing.add(decl.name);
                    }
                    List<XsdElementDecl> merged = new ArrayList<>();
                    for (XsdElementDecl decl : baseChildren) {
                        if (existing.add(decl.name)) {
                            merged.add(decl);
                            changed = true;
                        }
                    }
                    if (!merged.isEmpty()) {
                        merged.addAll(derivedChildren);
                        xsdChildrenByType.put(derived, merged);
                    }
                }
            } while (changed);

            // 将 type= 引用解析到元素名
            for (Map.Entry<String, String> entry : elementToType.entrySet()) {
                String elementName = entry.getKey();
                String typeName = localXsdName(entry.getValue());
                List<XsdElementDecl> typeChildren = xsdChildrenByType.get(typeName);
                if (typeChildren == null) {
                    continue;
                }
                if (!xsdChildrenByParent.containsKey(elementName)) {
                    xsdChildrenByParent.put(elementName, new ArrayList<>(typeChildren));
                    Boolean seq = xsdParentIsSequence.get(typeName);
                    if (seq != null) {
                        xsdParentIsSequence.put(elementName, seq);
                    }
                }
            }

            // 汇总元素出现次数（按元素名索引）
            for (List<XsdElementDecl> children : xsdChildrenByParent.values()) {
                for (XsdElementDecl decl : children) {
                    xsdOccurrencesByElement.computeIfAbsent(decl.name, k -> new ArrayList<>()).add(decl);
                }
            }
            for (List<XsdElementDecl> children : xsdChildrenByType.values()) {
                for (XsdElementDecl decl : children) {
                    xsdOccurrencesByElement.computeIfAbsent(decl.name, k -> new ArrayList<>()).add(decl);
                }
            }

            xsdStructureIndexReady = !xsdChildrenByParent.isEmpty() || !xsdChildrenByType.isEmpty();
        } catch (Exception e) {
            xsdStructureIndexReady = false;
        }
    }

    /**
     * collectNamedComplexTypes
     *
     * @brief
     * 功能: 执行 collectNamedComplexTypes 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void collectNamedComplexTypes(Document doc, Map<String, String> typeExtensionBase) {
        NodeList complexTypes = doc.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element complexType = (Element) complexTypes.item(i);
            String typeName = complexType.getAttribute("name");
            if (typeName == null || typeName.isEmpty()) {
                continue;
            }
            List<XsdElementDecl> children = new ArrayList<>();
            boolean[] sequenceFlag = new boolean[] { false };
            collectParticles(complexType, children, sequenceFlag, typeExtensionBase, typeName);
            if (!children.isEmpty()) {
                xsdChildrenByType.put(typeName, children);
                xsdParentIsSequence.put(typeName, sequenceFlag[0]);
            }
        }
    }

    /**
     * collectElementTypeBindings
     *
     * @brief
     * 功能: 执行 collectElementTypeBindings 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void collectElementTypeBindings(Element node, Map<String, String> elementToType,
            String enclosingElementName) {
        if (node == null) {
            return;
        }

        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(node.getNamespaceURI())
                && "element".equals(node.getLocalName())) {
            String elementName = resolveXsdElementName(node);
            String typeName = node.getAttribute("type");
            if (elementName != null && !elementName.isEmpty() && typeName != null && !typeName.isEmpty()) {
                elementToType.putIfAbsent(elementName, typeName);
            }

            Element inlineComplex = firstChildElementNS(node, "complexType");
            if (elementName != null && !elementName.isEmpty() && inlineComplex != null) {
                List<XsdElementDecl> children = new ArrayList<>();
                boolean[] sequenceFlag = new boolean[] { false };
                collectParticles(inlineComplex, children, sequenceFlag, null, null);
                if (!children.isEmpty()) {
                    xsdChildrenByParent.put(elementName, children);
                    xsdParentIsSequence.put(elementName, sequenceFlag[0]);
                }
            }
            enclosingElementName = elementName != null && !elementName.isEmpty() ? elementName : enclosingElementName;
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                collectElementTypeBindings((Element) children.item(i), elementToType, enclosingElementName);
            }
        }
    }

    /**
     * collectParticles
     *
     * @brief
     * 功能: 执行 collectParticles 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void collectParticles(Element container, List<XsdElementDecl> out, boolean[] sequenceFlag,
            Map<String, String> typeExtensionBase, String owningTypeName) {
        collectParticles(container, out, sequenceFlag, typeExtensionBase, owningTypeName, null, false);
    }

    /**
     * collectParticles
     *
     * @brief
     * 功能: 执行 collectParticles 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void collectParticles(Element container, List<XsdElementDecl> out, boolean[] sequenceFlag,
            Map<String, String> typeExtensionBase, String owningTypeName, String inheritedMaxOccurs) {
        collectParticles(container, out, sequenceFlag, typeExtensionBase, owningTypeName, inheritedMaxOccurs, false);
    }

    /**
     * collectParticles
     *
     * @brief
     * 功能: 执行 collectParticles 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void collectParticles(Element container, List<XsdElementDecl> out, boolean[] sequenceFlag,
            Map<String, String> typeExtensionBase, String owningTypeName, String inheritedMaxOccurs,
            boolean optionalGroup) {
        NodeList children = container.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) {
                continue;
            }
            Element child = (Element) children.item(i);
            if (!XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(child.getNamespaceURI())) {
                continue;
            }
            String local = child.getLocalName();
            if ("sequence".equals(local) || "choice".equals(local) || "all".equals(local)) {
                if ("sequence".equals(local) && sequenceFlag != null && !sequenceFlag[0]
                        && out.isEmpty()) {
                    sequenceFlag[0] = true;
                }
                if (("choice".equals(local) || "all".equals(local)) && sequenceFlag != null && out.isEmpty()) {
                    sequenceFlag[0] = false;
                }
                String particleMaxOccurs = child.getAttribute("maxOccurs");
                if (particleMaxOccurs == null || particleMaxOccurs.isEmpty()) {
                    particleMaxOccurs = inheritedMaxOccurs;
                }
                String particleMinOccurs = child.getAttribute("minOccurs");
                boolean childOptionalGroup = optionalGroup
                        || "choice".equals(local)
                        || (("sequence".equals(local) || "all".equals(local))
                                && "0".equals(particleMinOccurs));
                collectParticles(child, out, sequenceFlag, typeExtensionBase, owningTypeName,
                        particleMaxOccurs, childOptionalGroup);
            } else if ("complexContent".equals(local) || "simpleContent".equals(local)
                    || "restriction".equals(local) || "group".equals(local)) {
                String particleMaxOccurs = child.getAttribute("maxOccurs");
                if (particleMaxOccurs == null || particleMaxOccurs.isEmpty()) {
                    particleMaxOccurs = inheritedMaxOccurs;
                }
                collectParticles(child, out, sequenceFlag, typeExtensionBase, owningTypeName,
                        particleMaxOccurs, optionalGroup);
            } else if ("extension".equals(local)) {
                if (typeExtensionBase != null && owningTypeName != null) {
                    String base = child.getAttribute("base");
                    if (base != null && !base.isEmpty()) {
                        typeExtensionBase.put(owningTypeName, base);
                    }
                }
                String particleMaxOccurs = child.getAttribute("maxOccurs");
                if (particleMaxOccurs == null || particleMaxOccurs.isEmpty()) {
                    particleMaxOccurs = inheritedMaxOccurs;
                }
                collectParticles(child, out, sequenceFlag, typeExtensionBase, owningTypeName,
                        particleMaxOccurs, optionalGroup);
            } else if ("element".equals(local)) {
                String name = resolveXsdElementName(child);
                if (name != null && !name.isEmpty()) {
                    String maxOccurs = child.getAttribute("maxOccurs");
                    if (("QueuingPort".equals(name) || "SamplingPort".equals(name))
                            && (maxOccurs == null || maxOccurs.isEmpty())) {
                        maxOccurs = inheritedMaxOccurs;
                    }
                    out.add(new XsdElementDecl(name, child.getAttribute("minOccurs"), maxOccurs, optionalGroup));
                }
            }
        }
    }

    /**
     * resolveXsdElementName
     *
     * @brief
     * 功能: 执行 resolveXsdElementName 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String resolveXsdElementName(Element element) {
        String name = element.getAttribute("name");
        if (name != null && !name.isEmpty()) {
            return localXsdName(name);
        }
        String ref = element.getAttribute("ref");
        if (ref != null && !ref.isEmpty()) {
            return localXsdName(ref);
        }
        return null;
    }

    /**
     * localXsdName
     *
     * @brief
     * 功能: 执行 localXsdName 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String localXsdName(String qName) {
        if (qName == null || qName.isEmpty()) {
            return "";
        }
        int idx = qName.indexOf(':');
        return idx >= 0 ? qName.substring(idx + 1) : qName;
    }

    /**
     * firstChildElementNS
     *
     * @brief
     * 功能: 执行 firstChildElementNS 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static Element firstChildElementNS(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) {
                continue;
            }
            Element child = (Element) children.item(i);
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(child.getNamespaceURI())
                    && localName.equals(child.getLocalName())) {
                return child;
            }
        }
        return null;
    }

    /**
     * ensureXsdStructureIndex
     *
     * @brief
     * 功能: 执行 ensureXsdStructureIndex 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void ensureXsdStructureIndex() {
        if (!xsdStructureIndexReady) {
            loadXsdStructureIndex();
        }
    }

    /** 元素在 XSD 中是否声明了子元素（可用于同名嵌套误报区分）。 */
    private static boolean hasDeclaredChildrenInXsd(String elementName) {
        ensureXsdStructureIndex();
        if (elementName == null || elementName.isEmpty()) {
            return false;
        }
        List<XsdElementDecl> children = xsdChildrenByParent.get(elementName);
        return children != null && !children.isEmpty();
    }

    /** 查询 parent 在 XSD 中是否声明了 child 为直接子元素。 */
    private static boolean isValidChildOfContainerInXsd(String childName, String parentName) {
        ensureXsdStructureIndex();
        if (childName == null || childName.isEmpty() || parentName == null || parentName.isEmpty()) {
            return false;
        }
        List<XsdElementDecl> children = xsdChildrenByParent.get(parentName);
        if (children == null) {
            children = xsdChildrenByType.get(parentName);
        }
        if (children == null) {
            return false;
        }
        for (XsdElementDecl decl : children) {
            if (childName.equals(decl.name)) {
                return true;
            }
        }
        return false;
    }

    /** 当前父元素的内容模型是否由 xs:sequence 定义。 */
    private static boolean isSequenceContainerInXsd(String parentName) {
        ensureXsdStructureIndex();
        return parentName != null && Boolean.TRUE.equals(xsdParentIsSequence.get(parentName));
    }

    /** actual 与任一期望元素是否在 XSD 的同一个 sequence 父结构中声明。 */
    private static boolean isDeclaredInSameSequence(String actualElement, List<String> expectedElements) {
        ensureXsdStructureIndex();
        if (actualElement == null || actualElement.isEmpty()
                || expectedElements == null || expectedElements.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, List<XsdElementDecl>> entry : xsdChildrenByParent.entrySet()) {
            if (!Boolean.TRUE.equals(xsdParentIsSequence.get(entry.getKey()))
                    || !containsXsdChild(entry.getValue(), actualElement)) {
                continue;
            }
            for (String expectedElement : expectedElements) {
                if (containsXsdChild(entry.getValue(), expectedElement)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 查询父元素下某个直接子元素的 XSD 声明。 */
    private static XsdElementDecl findDirectChildDeclaration(String parentName, String childName) {
        ensureXsdStructureIndex();
        if (parentName == null || childName == null) {
            return null;
        }
        List<XsdElementDecl> children = xsdChildrenByParent.get(parentName);
        if (children == null) {
            children = xsdChildrenByType.get(parentName);
        }
        if (children != null) {
            for (XsdElementDecl declaration : children) {
                if (childName.equals(declaration.name)) {
                    return declaration;
                }
            }
        }
        return null;
    }

    /** 返回父元素的直接子元素声明，优先使用元素名映射，再回退到complexType映射。 */
    private static List<XsdElementDecl> getDirectChildDeclarations(String parentName) {
        ensureXsdStructureIndex();
        if (parentName == null || parentName.isEmpty()) {
            return null;
        }
        List<XsdElementDecl> children = xsdChildrenByParent.get(parentName);
        return children != null ? children : xsdChildrenByType.get(parentName);
    }

    /** 从“元素名@起始行#序号”的实例键中取出元素起始行号。 */
    private static int elementStartLineFromInstanceKey(String instanceKey) {
        if (instanceKey == null || instanceKey.isEmpty()) {
            return -1;
        }
        int at = instanceKey.indexOf('@');
        int hash = instanceKey.indexOf('#', at + 1);
        if (at < 0) {
            return -1;
        }
        try {
            return Integer.parseInt(instanceKey.substring(at + 1, hash > at ? hash : instanceKey.length()));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /** 从“元素名@起始行#序号”的实例键中取出元素名。 */
    private static String parentNameFromInstanceKey(String instanceKey) {
        if (instanceKey == null || instanceKey.isEmpty()) {
            return null;
        }
        int at = instanceKey.indexOf('@');
        return at > 0 ? instanceKey.substring(0, at) : null;
    }

    /** 取得指定父元素实例下某个直接子元素的实际数量。 */
    private static Integer directChildCount(String parentInstance, String childName) {
        if (parentInstance == null || childName == null || childName.isEmpty()) {
            return null;
        }
        Map<String, Integer> counts = directChildCountByParentInstance.get(parentInstance);
        return counts == null ? null : counts.getOrDefault(childName, 0);
    }

    /**
     * 从 XSD 错误给出的候选元素中，选择同一父元素实例下真正未满足 minOccurs 的元素。
     * XSD 的候选列表表示“当前位置允许出现什么”，不是“哪个元素缺失”。
     */
    private static String findMissingRequiredChild(String parentName, String parentInstance,
            List<String> candidates) {
        if (parentName == null || parentInstance == null) {
            return null;
        }
        if (candidates != null) {
            for (String candidate : candidates) {
                XsdElementDecl declaration = findDirectChildDeclaration(parentName, candidate);
                Integer count = directChildCount(parentInstance, candidate);
                if (declaration != null && declaration.required && count != null) {
                    try {
                        if (count < Integer.parseInt(declaration.minOccurs)) {
                            return candidate;
                        }
                    } catch (NumberFormatException ignored) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 当XSD把错误定位在父元素结束标签时，行号无法直接映射到父实例。
     * 仅在当前文件中该父元素只有一个实例时，才允许根据实际子元素数量反推缺失项。
     */
    private static String findMissingRequiredChildForUniqueParent(String parentName,
            List<String> candidates) {
        if (parentName == null || parentName.isEmpty()) {
            return null;
        }
        String uniqueParentInstance = null;
        for (String instanceKey : directChildCountByParentInstance.keySet()) {
            if (!parentName.equals(parentNameFromInstanceKey(instanceKey))) {
                continue;
            }
            if (uniqueParentInstance != null) {
                return null;
            }
            uniqueParentInstance = instanceKey;
        }
        return uniqueParentInstance == null
                ? null
                : findMissingRequiredChild(parentName, uniqueParentInstance, candidates);
    }



    /**
     * 判断元素名称是否在当前 CPM XSD 集合中有任何声明。
     *
     * <p>不能只根据 Xerces 的 "No child element is expected" 报文判定为 2010：
     * 当报文中的实际元素根本没有出现在 XSD 中时，应优先报告 2007。</p>
     */
    private static boolean isElementDefinedInXsd(String elementName) {
        ensureXsdStructureIndex();
        if (elementName == null || elementName.isEmpty()) {
            return false;
        }
        return xsdOccurrencesByElement.containsKey(elementName)
                || xsdChildrenByParent.containsKey(elementName)
                || xsdChildrenByType.containsKey(elementName);
    }

    /**
     * TR_PR_010: 查询 child 是否为 parent 下 minOccurs>=1 的必要子元素。
     * 必要元素完全缺失时同时满足“必要元素缺失”和“数量低于 minOccurs”两类需求，
     * 因此需要同时报告 2015 和 2009；minOccurs=0 的可选元素不报告这两类错误。
     */
    private static boolean isRequiredChildInXsd(String childName, String parentName) {
        ensureXsdStructureIndex();
        if (childName == null || childName.isEmpty() || parentName == null || parentName.isEmpty()) {
            return false;
        }
        List<XsdElementDecl> children = xsdChildrenByParent.get(parentName);
        if (children == null) {
            children = xsdChildrenByType.get(parentName);
        }
        if (children == null) {
            return false;
        }
        for (XsdElementDecl decl : children) {
            if (!childName.equals(decl.name)) {
                continue;
            }
            return decl.required;
        }
        return false;
    }

    /**
     * 从 XSD 读取元素 maxOccurs。
     * relatedElement 非空时，优先取“同一父容器下与 related 并列”的那条声明。
     */
    private static String getMaxOccursFromXsd(String elementName, String relatedElement) {
        XsdElementDecl decl = findOccurrenceInXsd(elementName, relatedElement);
        return decl != null ? decl.maxOccurs : "";
    }

    /**
     * 从 XSD 读取元素 minOccurs。
     * relatedElement 非空时，优先取“同一父容器下与 related 并列”的那条声明。
     */
    private static String getMinOccursFromXsd(String elementName, String relatedElement) {
        XsdElementDecl decl = findOccurrenceInXsd(elementName, relatedElement);
        return decl != null ? decl.minOccurs : "";
    }

    /**
     * findOccurrenceInXsd
     *
     * @brief
     * 功能: 执行 findOccurrenceInXsd 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static XsdElementDecl findOccurrenceInXsd(String elementName, String relatedElement) {
        ensureXsdStructureIndex();
        if (elementName == null || elementName.isEmpty()) {
            return null;
        }

        if (relatedElement != null && !relatedElement.isEmpty()) {
            for (Map.Entry<String, List<XsdElementDecl>> entry : xsdChildrenByParent.entrySet()) {
                XsdElementDecl matched = findSiblingDecl(entry.getValue(), elementName, relatedElement);
                if (matched != null) {
                    return matched;
                }
            }
            for (Map.Entry<String, List<XsdElementDecl>> entry : xsdChildrenByType.entrySet()) {
                XsdElementDecl matched = findSiblingDecl(entry.getValue(), elementName, relatedElement);
                if (matched != null) {
                    return matched;
                }
            }
        }

        List<XsdElementDecl> occurrences = xsdOccurrencesByElement.get(elementName);
        if (occurrences == null || occurrences.isEmpty()) {
            return null;
        }
        // 无上下文时：若所有声明的 maxOccurs 相同则返回该值；否则返回第一条有限次数声明。
        String uniqueMax = occurrences.get(0).maxOccurs;
        boolean allSame = true;
        for (XsdElementDecl decl : occurrences) {
            if (!uniqueMax.equals(decl.maxOccurs)) {
                allSame = false;
                break;
            }
        }
        if (allSame) {
            return occurrences.get(0);
        }
        for (XsdElementDecl decl : occurrences) {
            if (!"unbounded".equals(decl.maxOccurs)) {
                return decl;
            }
        }
        return occurrences.get(0);
    }

    /**
     * findSiblingDecl
     *
     * @brief
     * 功能: 执行 findSiblingDecl 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static XsdElementDecl findSiblingDecl(List<XsdElementDecl> children, String elementName,
            String relatedElement) {
        boolean hasElement = false;
        boolean hasRelated = false;
        XsdElementDecl elementDecl = null;
        for (XsdElementDecl decl : children) {
            if (elementName.equals(decl.name)) {
                hasElement = true;
                elementDecl = decl;
            }
            if (relatedElement.equals(decl.name)) {
                hasRelated = true;
            }
        }
        return hasElement && hasRelated ? elementDecl : null;
    }

    /**
     * 若 XSD 某 sequence 中 actual 在 expected 之前，且 actual 的实际数量超过
     * maxOccurs，才判为数量超限（2010）。仅仅发生顺序变化不能判为2010。
     */
    private static boolean isElementRepetitionInXsdSequence(String actualElement, String expectedElement,
            boolean actualExceedsMaxOccurs) {
        ensureXsdStructureIndex();
        if (actualElement == null || expectedElement == null || actualElement.isEmpty()
                || expectedElement.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, List<XsdElementDecl>> entry : xsdChildrenByParent.entrySet()) {
            if (!Boolean.TRUE.equals(xsdParentIsSequence.get(entry.getKey()))) {
                continue;
            }
            if (actualExceedsMaxOccurs
                    && isBeforeWithFiniteMax(entry.getValue(), actualElement, expectedElement)) {
                return true;
            }
        }
        for (Map.Entry<String, List<XsdElementDecl>> entry : xsdChildrenByType.entrySet()) {
            if (!Boolean.TRUE.equals(xsdParentIsSequence.get(entry.getKey()))) {
                continue;
            }
            if (actualExceedsMaxOccurs
                    && isBeforeWithFiniteMax(entry.getValue(), actualElement, expectedElement)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 若 XSD 某 sequence 中 expected 在 actual 之前，且 expected 的 minOccurs &gt; 0，
     * 则遇到 actual 而期望 expected 时应判为缺必选（2009）。
     */
    private static boolean isMissingRequiredElementInXsdSequence(String actualElement, String expectedElement) {
        ensureXsdStructureIndex();
        if (actualElement == null || expectedElement == null || actualElement.isEmpty()
                || expectedElement.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, List<XsdElementDecl>> entry : xsdChildrenByParent.entrySet()) {
            if (!Boolean.TRUE.equals(xsdParentIsSequence.get(entry.getKey()))) {
                continue;
            }
            if (isRequiredBefore(entry.getValue(), expectedElement, actualElement)) {
                return true;
            }
        }
        for (Map.Entry<String, List<XsdElementDecl>> entry : xsdChildrenByType.entrySet()) {
            if (!Boolean.TRUE.equals(xsdParentIsSequence.get(entry.getKey()))) {
                continue;
            }
            if (isRequiredBefore(entry.getValue(), expectedElement, actualElement)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 问题报告单5: 查找同时包含 child 与 sibling 的 sequence 父容器名。
     * 用于"必选单例元素缺失+无效内容并存"场景（cvc-complex-type.2.4.a）下
     * 构造2015错误消息中的父元素名。
     */
    private static String findParentOfChildInXsdSequence(String childName, String siblingName) {
        ensureXsdStructureIndex();
        if (childName == null || childName.isEmpty() || siblingName == null || siblingName.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, List<XsdElementDecl>> entry : xsdChildrenByParent.entrySet()) {
            if (!Boolean.TRUE.equals(xsdParentIsSequence.get(entry.getKey()))) {
                continue;
            }
            if (containsXsdChild(entry.getValue(), childName) && containsXsdChild(entry.getValue(), siblingName)) {
                return entry.getKey();
            }
        }
        for (Map.Entry<String, List<XsdElementDecl>> entry : xsdChildrenByType.entrySet()) {
            if (!Boolean.TRUE.equals(xsdParentIsSequence.get(entry.getKey()))) {
                continue;
            }
            if (containsXsdChild(entry.getValue(), childName) && containsXsdChild(entry.getValue(), siblingName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * containsXsdChild
     *
     * @brief
     * 功能: 执行 containsXsdChild 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean containsXsdChild(List<XsdElementDecl> children, String name) {
        for (XsdElementDecl decl : children) {
            if (name.equals(decl.name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * isBeforeWithFiniteMax
     *
     * @brief
     * 功能: 执行 isBeforeWithFiniteMax 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean isBeforeWithFiniteMax(List<XsdElementDecl> children, String earlier, String later) {
        int earlierIndex = -1;
        int laterIndex = -1;
        XsdElementDecl earlierDecl = null;
        for (int i = 0; i < children.size(); i++) {
            XsdElementDecl decl = children.get(i);
            if (earlierIndex < 0 && earlier.equals(decl.name)) {
                earlierIndex = i;
                earlierDecl = decl;
            }
            if (laterIndex < 0 && later.equals(decl.name)) {
                laterIndex = i;
            }
        }
        return earlierDecl != null && earlierIndex >= 0 && laterIndex > earlierIndex
                && !"unbounded".equals(earlierDecl.maxOccurs);
    }

    /**
     * isRequiredBefore
     *
     * @brief
     * 功能: 执行 isRequiredBefore 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean isRequiredBefore(List<XsdElementDecl> children, String earlier, String later) {
        int earlierIndex = -1;
        int laterIndex = -1;
        XsdElementDecl earlierDecl = null;
        for (int i = 0; i < children.size(); i++) {
            XsdElementDecl decl = children.get(i);
            if (earlierIndex < 0 && earlier.equals(decl.name)) {
                earlierIndex = i;
                earlierDecl = decl;
            }
            if (laterIndex < 0 && later.equals(decl.name)) {
                laterIndex = i;
            }
        }
        if (earlierDecl == null || earlierIndex < 0 || laterIndex <= earlierIndex) {
            return false;
        }
        try {
            return Integer.parseInt(earlierDecl.minOccurs) > 0;
        } catch (NumberFormatException e) {
            return !"0".equals(earlierDecl.minOccurs);
        }
    }

    @FunctionalInterface
    private interface SchemaInitializer {
        void initialize() throws SAXException;
    }

    // ========== 公共验证方法 ==========

    /**
     * validateCoreOSConfig
     *
     * @brief
     * 功能: 校验 CoreOS XML 配置文件。
     * @param[in]  xmlFile           XML 配置文件。
     * @param[out] errorMessagesList 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateCoreOSConfig(File xmlFile, List<String> errorMessagesList) throws IOException {
        return validateXml(xmlFile, errorMessagesList, coreOSSchema, isCoreOSSchemaInitialized, "CoreOS");
    }

    /**
     * validateModuleConfig
     *
     * @brief
     * 功能: 校验 Module XML 配置文件。
     * @param[in]  xmlFile           XML 配置文件。
     * @param[out] errorMessagesList 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateModuleConfig(File xmlFile, List<String> errorMessagesList) throws IOException {
        return validateXml(xmlFile, errorMessagesList, moduleSchema, isModuleSchemaInitialized, "Module");
    }

    /**
     * validateApplicationConfig
     *
     * @brief
     * 功能: 校验 Application XML 配置文件。
     * @param[in]  xmlFile           XML 配置文件。
     * @param[out] errorMessagesList 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateApplicationConfig(File xmlFile, List<String> errorMessagesList) throws IOException {
        return validateXml(xmlFile, errorMessagesList, applicationSchema, isApplicationSchemaInitialized,
                "Application");
    }

    /**
     * validatePartitionConfig
     *
     * @brief
     * 功能: 校验 Partition XML 配置文件。
     * @param[in]  xmlFile           XML 配置文件。
     * @param[out] errorMessagesList 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validatePartitionConfig(File xmlFile, List<String> errorMessagesList) throws IOException {
        return validateXml(xmlFile, errorMessagesList, partitionSchema, isPartitionSchemaInitialized, "Partition");
    }

    /**
     * validateTypeLibConfig
     *
     * @brief
     * 功能: 校验 TypeLib XML 配置文件。
     * @param[in]  xmlFile           XML 配置文件。
     * @param[out] errorMessagesList 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateTypeLibConfig(File xmlFile, List<String> errorMessagesList) throws IOException {
        return validateXml(xmlFile, errorMessagesList, typeLibSchema, isTypeLibSchemaInitialized, "TypeLib");
    }

    /**
     * validateXmlAuto
     *
     * @brief
     * 功能: 根据根元素自动选择配置类型并执行 XML 校验。
     * @param[in]  xmlFile           XML 配置文件。
     * @param[out] errorMessagesList 错误信息列表。
     * @param[in]  enableCrcCheck    是否启用 CRC 校验。
     * @return 校验通过返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateXmlAuto(File xmlFile, List<String> errorMessagesList, boolean enableCrcCheck)
            throws IOException {
        boolean xsdValid = validateXsdOnly(xmlFile, errorMessagesList);
        boolean consistencyValid = true;

        if (xsdValid || executeErrorsOnXsdFailure) {
            consistencyValid = validateConsistencyRulesInternal(xmlFile, errorMessagesList, enableCrcCheck,
                    xsdValid || executeWarningsOnXsdFailure, true, true);
        }

        return xsdValid && consistencyValid;
    }

    /**
     * validateXsdOnly
     *
     * @brief
     * Function: validate XML only against schema-level constraints. This stage
     * covers XML structure, type, format, occurrence and XSD-defined value
     * restrictions. Consistency rules from data dictionary 4.3 are not executed
     * here.
     */
    public static boolean validateXsdOnly(File xmlFile, List<String> errorMessagesList) throws IOException {
        try {
            String rootElement = getRootElement(xmlFile);
            return validateSchemaByRootElement(xmlFile, errorMessagesList, rootElement);
        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            errorMessagesList.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }


    /**
     * Compatibility entry for older callers. Element and attribute constraints are
     * validated by the active XSD Schema instead of a Java-side range table.
     */
    public static boolean validateDataRangesForCorrectness(File xmlFile, List<String> errorMessagesList) {
        try {
            return validateXsdOnly(xmlFile, errorMessagesList);
        } catch (IOException e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            errorMessagesList.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }
    /**
     * validateConsistencyRules
     *
     * @brief
     * Function: validate consistency rules defined by data dictionary 4.3. This
     * stage contains page alignment, data range, uniqueness, reference relation,
     * memory block and other business constraints. It intentionally does not run
     * XSD schema validation.
     */
    /**
     * 004788: 执行XML一致性校验，不包含XML CRC校验。
     */
    public static boolean validateConsistencyRulesWithoutCrc(File xmlFile, List<String> errorMessagesList)
            throws IOException {
        return validateConsistencyRulesInternal(xmlFile, errorMessagesList, false, true, false, true);
    }

    /**
     * 004788: 单独执行XML文件CRC校验。
     */
    public static boolean validateXmlCrc(File xmlFile, List<String> errorMessagesList, boolean enableCrcCheck)
            throws IOException {
        List<File> allFiles = new ArrayList<>();
        allFiles.add(xmlFile);
        try {
            String rootElement = getRootElement(xmlFile);
            if (isModuleFile(rootElement)) {
                for (File includedFile : extractIncludedFiles(xmlFile)) {
                    if (includedFile.exists()) {
                        allFiles.add(includedFile);
                    }
                }
            }
        } catch (Exception e) {
            Log.info("Failed to extract included files, validating main file only: " + e.getMessage());
        }

        Log.info("Initiating CRC validation for XML files.");
        boolean crcValid = true;
        for (File fileToValidate : allFiles) {
            if (!CRCChecksumValidator.validateCRCChecksum(fileToValidate, errorMessagesList, enableCrcCheck)) {
                crcValid = false;
            }
            CRCAttributeValidator.validateCRCAttribute(fileToValidate, errorMessagesList);
        }
        if (crcValid) {
            Log.info("XML file pass CRC validation.");
        }
        return crcValid;
    }
    /**
     * validateConsistencyRules
     *
     * @brief
     * 功能: 执行 validateConsistencyRules 相关处理。
     *
     * @scope external
     * @errno:
     * N/A
     */
    public static boolean validateConsistencyRules(File xmlFile, List<String> errorMessagesList,
            boolean enableCrcCheck) throws IOException {
        return validateConsistencyRulesInternal(xmlFile, errorMessagesList, enableCrcCheck, true, true, true);
    }

    /**
     * validateSchemaByRootElement
     *
     * @brief
     * 功能: 执行 validateSchemaByRootElement 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean validateSchemaByRootElement(File xmlFile, List<String> errorMessagesList,
            String rootElement) throws IOException {
        switch (rootElement) {
            case ROOT_COREOS_DESCRIPTION:
            case ROOT_COREOS:
                return validateCoreOSConfig(xmlFile, errorMessagesList);
            case ROOT_MODULE:
            case ROOT_MODULE_DESCRIPTION:
                return validateModuleConfig(xmlFile, errorMessagesList);
            case ROOT_APPLICATION_DESCRIPTION:
            case ROOT_APPLICATION:
            case ROOT_SHARED_DATA_DESCRIPTION:
            case ROOT_SHARED_IO_DESCRIPTION:
            case ROOT_SHARED_LIBRARY_DESCRIPTION:
                return validateApplicationConfig(xmlFile, errorMessagesList);
            case ROOT_PARTITION_DESCRIPTION:
            case ROOT_PARTITION:
            case ROOT_PSEUDO_PARTITION_DESCRIPTION:
            case ROOT_PSEUDO_PARTITION:
                return validatePartitionConfig(xmlFile, errorMessagesList);
            default:
                Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                        "Unknown root element: " + rootElement + ", trying Module schema");
                return validateModuleConfig(xmlFile, errorMessagesList);
        }
    }

    /**
     * validateConsistencyRulesInternal
     *
     * @brief
     * 功能: 执行 validateConsistencyRulesInternal 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean validateConsistencyRulesInternal(File xmlFile, List<String> errorMessagesList,
            boolean enableCrcCheck, boolean includeModuleWarnings, boolean includeCrcValidation,
            boolean emitSummaryError) throws IOException {

        String rootElement = getRootElement(xmlFile);
        boolean commonValidationsPass = performCommonValidations(xmlFile, errorMessagesList, enableCrcCheck,
                includeCrcValidation);

        if (includeModuleWarnings && isModuleFile(rootElement)) {
            performModuleSpecificValidations(xmlFile, errorMessagesList);
        }

        boolean consistencyValid = commonValidationsPass;
        if (!consistencyValid && emitSummaryError) {
            String summaryError = "XML files failed consistency validation.";
            errorMessagesList.add(summaryError);
            Log.error(CodeConstant.ERROR_XML_CONSISTENCY_VALIDATION_FAILED, summaryError);
        }

        return consistencyValid;
    }

    /**
     * 判断是否为Module类型文件
     * 
     * @param rootElement 根元素名称
     * @return 是否为Module文件
     */
    private static boolean isModuleFile(String rootElement) {
        return ROOT_MODULE.equals(rootElement) || ROOT_MODULE_DESCRIPTION.equals(rootElement);
    }

    /**
     * performCommonValidations
     *
     * @brief
     * 功能: 执行 XML 文件通用校验流程。
     * @param[in]  xmlFile           XML 配置文件。
     * @param[out] errorMessagesList 错误信息列表。
     * @param[in]  enableCrcCheck    是否启用 CRC 校验。
     * @return 通用校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean performCommonValidations(File xmlFile, List<String> errorMessagesList,
            boolean enableCrcCheck, boolean includeCrcValidation) {
        // 获取所有需要验证的文件列表（主文件 + 引用的子文件）
        List<File> allFiles = new ArrayList<>();
        allFiles.add(xmlFile); // 主文件

        // 如果是 Module 文件，添加所有引用的子文件
        try {
            String rootElement = getRootElement(xmlFile);
            if (isModuleFile(rootElement)) {
                List<File> includedFiles = extractIncludedFiles(xmlFile);
                for (File includedFile : includedFiles) {
                    if (includedFile.exists()) {
                        allFiles.add(includedFile);
                    }
                }
            }
        } catch (Exception e) {
            // 如果无法提取引用文件，只验证主文件
            Log.info("Failed to extract included files, validating main file only: " + e.getMessage());
        }

        // 1. 页对齐验证 (错误码3003) - 对所有文件验证
        boolean pageAlignmentValid = true;
        for (File fileToValidate : allFiles) {
            boolean fileValid = PageAlignmentValidator.validatePageAlignment(fileToValidate, errorMessagesList);
            pageAlignmentValid = pageAlignmentValid && fileValid;
        }

        // 2. 地址范围验证 (错误码3004) - 只对主文件验证
        boolean addressRangeValid = AddressRangeValidator.validateAddressRange(xmlFile, errorMessagesList);

        // 2.1 内存块关系验证 (错误码3004和3005) - 4.3.5 内存块检查
        // 验证内存块之间的包含关系和互斥关系
        // 问题报告单8：MemoryBlockValidator已包含完整的互斥关系验证（3005），
        // 注释掉旧的AddressOverlapValidator以避免重复报错
        boolean memoryBlockValid = MemoryBlockValidator.validateMemoryBlocks(xmlFile, errorMessagesList);

        // 3. 地址重叠验证 (错误码3005) - 已由MemoryBlockValidator替代
        // 问题报告单8：注释掉重复的AddressOverlapValidator调用
        // 保留此部分是为了避免误报两次3005错误（一次CPM_L1_module.xml，一次coreOS.xml）
        // boolean addressOverlapValid = true;
        // for (File fileToValidate : allFiles) {
        // boolean fileValid =
        // AddressOverlapValidator.validateAddressOverlap(fileToValidate,
        // errorMessagesList);
        // addressOverlapValid = addressOverlapValid && fileValid;
        // }


        // 4.5. Schedules非空验证 (错误码2009) - 问题报告单5
        // 验证Schedules元素不为空（必须包含Schedule或BmpSchedule子元素）
        // 背景：XSD的xs:choice无法表达"BmpSchedule可选(minOccurs=0)但Schedules整体必选"的约束
        boolean schedulesValid = true;
        try {
            String rootElement = getRootElement(xmlFile);
            if (isModuleFile(rootElement)) {
                schedulesValid = SchedulesValidator.validateSchedulesNotEmpty(xmlFile, errorMessagesList);
            }
        } catch (Exception e) {
            Log.info("Schedules validation skipped: " + e.getMessage());
        }

        // 5. 唯一性验证 (错误码3001) - 4.3.3 唯一性检查
        // 验证配置数据是否满足表 74 所定义的唯一性约束
        boolean uniquenessValid = UniquenessValidator.validateUniqueness(xmlFile, errorMessagesList);

        // 6. 引用关系验证 (错误码3002) - 4.3.4 引用关系检查
        // 验证配置数据是否满足表 75 所定义的引用关系约束
        boolean referenceValid = ReferenceValidator.validateReferences(xmlFile, errorMessagesList);

        // 7. CRC校验码验证 (错误码3006/警告码4005) - 对所有文件验证
        boolean crcChecksumValid = true;
        if (includeCrcValidation) {
            if (!enableCrcCheck) {
                String warningMsg = String.format(
                        "CRC validation has been disabled for the input XML files. Proceeding without integrity verification. File Path: %s",
                        xmlFile.getAbsolutePath());
                Log.warning(CodeConstant.WARNING_CRC_VALIDATION_DISABLED, warningMsg);
            } else {
                // PR_17: 在CRC校验之前输出日志，确保在xml校验错误信息之后打印
                Log.info("Initiating CRC validation for XML files.");
                for (File fileToValidate : allFiles) {
                    boolean fileValid = CRCChecksumValidator.validateCRCChecksum(fileToValidate, errorMessagesList,
                            true);
                    if (!fileValid) {
                        // 错误码3006: XML CRC校验失败
                        // 不在此处直接System.exit，允许继续收集其它验证错误，最终由上层统一终止
                        crcChecksumValid = false;
                    }
                }
                // CPMVerify-TOR-001163: 当所有XML文件的CRC校验通过时，输出汇总信息
                if (crcChecksumValid) {
                    Log.info("XML file pass CRC validation.");
                }

                // 8. CRC属性验证 (警告码4005) - 对所有XML文件验证CRC32属性格式（警告类，不影响返回结果）
                for (File fileToValidate : allFiles) {
                    CRCAttributeValidator.validateCRCAttribute(fileToValidate, errorMessagesList);
                }
            }
        }
        // 9. 杂项约束验证 (错误码1018暂用) - 4.3.6 其他检查
        // 验证端口方向/协议、分区内存、共享数据地址等业务约束
        boolean miscellaneousValid = MiscellaneousConstraintValidator.validateMiscellaneousConstraints(xmlFile,
                errorMessagesList);

        // 问题报告单8：移除addressOverlapValid，因为已由memoryBlockValid包含
        // 问题报告单5：添加schedulesValid验证
        return pageAlignmentValid && addressRangeValid && memoryBlockValid
                && schedulesValid && uniquenessValid && referenceValid && crcChecksumValid
                && miscellaneousValid;
    }

    /**
     * performModuleSpecificValidations
     *
     * @brief
     * 功能: 执行 Module 文件专有约束校验。
     * @param[in]  xmlFile           Module XML 配置文件。
     * @param[out] errorMessagesList 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean performModuleSpecificValidations(File xmlFile, List<String> errorMessagesList) {
        // 4. 共享数据引用验证 (警告码4001) - 在整个Module（包含所有引用文件）中检查
        boolean sharedDataValid = SharedDataReferenceValidator.validateSharedDataReference(xmlFile, errorMessagesList);

        // 5. 共享库引用验证 (警告码4002) - 在整个Module（包含所有引用文件）中检查
        boolean sharedLibraryValid = SharedLibraryReferenceValidator.validateSharedLibraryReference(xmlFile,
                errorMessagesList);

        // 6. 分区调度验证 (警告码4003) - 在整个Module（包含所有引用文件）中检查
        boolean partitionScheduleValid = PartitionScheduleValidator.validatePartitionSchedule(xmlFile,
                errorMessagesList);

        // 7. 应用引用验证 (警告码4004) - 在整个Module（包含所有引用文件）中检查
        boolean applicationValid = ApplicationReferenceValidator.validateApplicationReference(xmlFile,
                errorMessagesList);

        return sharedDataValid && sharedLibraryValid && partitionScheduleValid && applicationValid;
    }

    /**
     * 获取XML文件的根元素名称
     * 
     * @param xmlFile XML文件
     * @return 根元素名称（去除命名空间前缀）
     * @throws IOException 文件读取异常
     */
    private static String getRootElement(File xmlFile) throws IOException {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(xmlFile))) {
            StringBuilder rootTagBuilder = new StringBuilder();
            boolean foundStart = false;
            String line;

            while ((line = reader.readLine()) != null) {
                // 跳过XML声明和注释
                if (line.startsWith("<?xml") || line.startsWith("<!--")) {
                    continue;
                }

                // 处理根元素标签
                if (line.startsWith("<") || foundStart) {
                    if (!foundStart) {
                        foundStart = true;
                        int start = line.indexOf('<') + 1;
                        rootTagBuilder.append(line.substring(start));
                    } else if (line.trim().length() > 0) {
                        rootTagBuilder.append(line);
                    }

                    // 尝试提取完整的标签
                    String extractedTag = extractTagName(rootTagBuilder.toString());
                    if (extractedTag != null && !extractedTag.isEmpty()) {
                        return extractedTag;
                    }
                }
            }
        }

        // 无法提取根元素
        Log.warning(null, "Could not extract root element from " + xmlFile.getName());
        return "";
    }

    /**
     * 从标签字符串中提取标签名
     * 
     * @param tagContent 标签内容
     * @return 标签名（去除命名空间前缀和属性），如果未找到返回null
     */
    private static String extractTagName(String tagContent) {
        int endIndex = tagContent.indexOf('>');
        if (endIndex <= 0) {
            return null; // 标签未结束
        }

        String tag = tagContent.substring(0, endIndex).trim();

        // 去除属性（空格后的内容）
        int spaceIndex = tag.indexOf(' ');
        if (spaceIndex > 0) {
            tag = tag.substring(0, spaceIndex);
        }

        // 去除命名空间前缀（冒号前的内容）
        int colonIndex = tag.indexOf(':');
        if (colonIndex > 0) {
            return tag.substring(colonIndex + 1);
        }

        return tag;
    }

    // ========== 文件列表验证方法 ==========

    /**
     * 验证CoreOS配置文件列表
     */
    public static Map<File, List<String>> validateCoreOSConfigList(List<File> filesToValidate) {
        return validateXmlFileList(filesToValidate, coreOSSchema, isCoreOSSchemaInitialized, "CoreOS");
    }

    /**
     * 验证Module配置文件列表
     */
    public static Map<File, List<String>> validateModuleConfigList(List<File> filesToValidate) {
        return validateXmlFileList(filesToValidate, moduleSchema, isModuleSchemaInitialized, "Module");
    }

    /**
     * 验证Application配置文件列表
     */
    public static Map<File, List<String>> validateApplicationConfigList(List<File> filesToValidate) {
        return validateXmlFileList(filesToValidate, applicationSchema, isApplicationSchemaInitialized, "Application");
    }

    /**
     * 验证Partition配置文件列表
     */
    public static Map<File, List<String>> validatePartitionConfigList(List<File> filesToValidate) {
        return validateXmlFileList(filesToValidate, partitionSchema, isPartitionSchemaInitialized, "Partition");
    }

    /**
     * 验证TypeLib配置文件列表（通常不需要单独验证）
     */
    public static Map<File, List<String>> validateTypeLibConfigList(List<File> filesToValidate) {
        return validateXmlFileList(filesToValidate, typeLibSchema, isTypeLibSchemaInitialized, "TypeLib");
    }

    // ========== 通用验证方法 ==========

    /**
     * validateXml
     *
     * @brief
     * 功能: 使用指定 Schema 执行 XML 校验。
     * @param[in]  xmlFile           XML 配置文件。
     * @param[out] errorMessagesList 错误信息列表。
     * @param[in]  schema            XSD Schema 对象。
     * @param[in]  configType        配置类型名称。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateXml(File xmlFile, List<String> errorMessagesList,
            Schema schema, boolean isSchemaInitialized, String schemaType) throws IOException {
        if (!isSchemaInitialized) {
            // Schema未初始化时由主流程统一归类为1018。
            String message = schemaType + " Schema not initialized. Cannot validate XML file.";
            if (errorMessagesList != null) {
                errorMessagesList.add(message);
            }
            return false;
        }

        List<String> localErrorMessages = (errorMessagesList != null) ? errorMessagesList : new ArrayList<>();

        if (xmlFile == null || !xmlFile.exists() || !xmlFile.isFile()) {
            String message = "XML file is null, does not exist, or is not a file: " +
                    (xmlFile != null ? getCleanPath(xmlFile) : "null");
            localErrorMessages.add(message);
            Log.error(CodeConstant.ERROR_XML_FOLDER_NOT_EXIST, message);
            return false;
        }

        // 两阶段验证方案：
        // 阶段1：先验证主文件本身及其引用的子文件（保留精确的文件名和行号）
        // 阶段2：再验证 XInclude 展开后的整体文档（用于跨文件的完整性检查）

        boolean phase1Valid = true;
        boolean phase2Valid = true;

        // ===== 阶段1：验证主文件和各个子文件（精确定位） =====
        try {
            // PR_XX: Module文件在阶段1也构建行号映射，保证类型错误定位稳定，避免同一属性错误因不同cvc-*回调导致行号微偏而重复输出。
            if ("Module".equals(schemaType)) {
                buildLineNumberMapping(xmlFile);
            }

            // 验证主文件本身（不展开 XInclude）
            phase1Valid = validateSingleFile(xmlFile, schema, localErrorMessages, schemaType);

            // 如果是 Module 文件，还需要验证所有引用的子文件
            if ("Module".equals(schemaType)) {
                List<File> includedFiles = extractIncludedFiles(xmlFile);
                for (File includedFile : includedFiles) {
                    if (includedFile.exists()) {
                        // 根据子文件类型选择合适的 Schema
                        boolean subFileValid = validateIncludedFile(includedFile, localErrorMessages);
                        phase1Valid = phase1Valid && subFileValid;
                    } else {
                        String error = "Referenced file not found: " + includedFile.getName();
                        localErrorMessages.add(error);
                        Log.error(CodeConstant.ERROR_XML_FILE_MISSING, error);
                        phase1Valid = false;
                    }
                }
            }
        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            localErrorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            phase1Valid = false;
        }

        // ===== 阶段2：验证 XInclude 展开后的整体文档（用于跨文件检查） =====
        // 只有在阶段1通过后才执行阶段2，避免误报
        if (phase1Valid && "Module".equals(schemaType)) {
            try {
                // PR_XX: 在XInclude验证前构建行号映射表
                buildLineNumberMapping(xmlFile);
                phase2Valid = validateWithXInclude(xmlFile, schema, localErrorMessages, schemaType);
            } catch (Exception e) {
                String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
                localErrorMessages.add(errorMsg);
                Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
                phase2Valid = false;
            }
        }

        return phase1Valid && phase2Valid;
    }

    /**
     * validateSingleFile
     *
     * @brief
     * 功能: 对单个 XML 文件执行 Schema 校验。
     * @param[in]  xmlFile       XML 配置文件。
     * @param[in]  schema        XSD Schema 对象。
     * @param[out] errorMessages 错误信息列表。
     * @param[in]  configType    配置类型名称。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateSingleFile(File xmlFile, Schema schema, List<String> errorMessages,
            String schemaType) {
        ValidationErrorHandler errorHandler = null;
        try {
            // 每个被校验文件都建立父子关系映射，供2007顺序豁免进行父节点级判断。
            buildLineNumberMapping(xmlFile);

            // PR_XX: 使用 SAXSource 配合禁用 XInclude 的解析器，以获取原始文件的精确行号
            javax.xml.parsers.SAXParserFactory spf = javax.xml.parsers.SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setXIncludeAware(false); // 禁用 XInclude 展开

            // 设置 feature 确保不处理 XInclude
            try {
                spf.setFeature("http://apache.org/xml/features/xinclude", false);
            } catch (Exception e) {
                // 某些解析器可能不支持这个 feature，忽略
            }

            javax.xml.parsers.SAXParser saxParser = spf.newSAXParser();
            org.xml.sax.XMLReader xmlReader = saxParser.getXMLReader();

            // 预处理：替换href环境变量 + 修复重复属性（2012）
            // 将2012结构性错误也写入 errorMessages，确保后续阶段统计到“有错误”
            java.util.List<String> structuralErrors = new java.util.ArrayList<>();
            org.xml.sax.InputSource inputSource = XmlParser.preprocessXmlFile(xmlFile, structuralErrors);
            // 预处理虽然会删除重复属性以便继续完成XSD校验，但重复属性本身仍属于
            // XML正确性错误，不能因为修复后的临时内容能够通过XSD就返回成功。
            boolean hasStructuralErrors = structuralErrors != null && !structuralErrors.isEmpty();
            if (errorMessages != null && structuralErrors != null && !structuralErrors.isEmpty()) {
                for (String msg : structuralErrors) {
                    if (msg != null && !msg.isEmpty() && !errorMessages.contains(msg)) {
                        errorMessages.add(msg);
                    }
                }
            }

            Validator validator = schema.newValidator();
            errorHandler = new ValidationErrorHandler(errorMessages);
            validator.setErrorHandler(errorHandler);

            // 使用 SAXSource 验证，保留原始位置信息且不展开 XInclude
            javax.xml.transform.sax.SAXSource source = new javax.xml.transform.sax.SAXSource(
                    xmlReader, inputSource);
            validator.validate(source);

            // 004838/006358：独立核对每个XML父节点实例下的必选子元素数量，
            // 不受XSD将“缺失”报告为顺序/位置错误的具体报文形式影响。
            errorHandler.auditMissingRequiredElements(xmlFile);
            boolean handlerHasErrors = errorHandler.hasErrors();
            if (handlerHasErrors || hasStructuralErrors) {
                // TR_PR_016 (CPMVerify-TR-004835): XSD校验未通过时输出1018，报出实际校验失败的文件
                Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED,
                        "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。");
                return false;
            }
            return true;
        } catch (Exception e) {
            // 某些JAXP实现即使错误处理器忽略了CRC32格式错误，仍会从
            // Validator.validate抛出SAXParseException。该场景已按004930记录4005，
            // 不应再被这里转换成XSD失败1018。
            if (errorHandler != null && errorHandler.hasOnlyCrc32FormatWarnings()) {
                return true;
            }
            String errorMsg = schemaType + " file validation failed: " + xmlFile.getName() + " - " + e.getMessage();
            if (errorHandler == null || !errorHandler.hasErrors()) {
                errorMessages.add(errorMsg);
            }
            return false;
        }
    }

    /**
     * 验证包含的子文件
     */
    private static boolean validateIncludedFile(File includedFile, List<String> errorMessages) {
        try {
            // 根据文件内容判断应该使用哪个 Schema
            String rootElement = getRootElement(includedFile);

            if (rootElement.contains("CoreOS") || rootElement.contains("CoreOSDescription")) {
                return validateSingleFile(includedFile, coreOSSchema, errorMessages, "CoreOS");
            } else if (rootElement.contains("Application") || rootElement.contains("SharedLibrary") ||
                    rootElement.contains("SharedData") || rootElement.contains("SharedIO")) {
                return validateSingleFile(includedFile, applicationSchema, errorMessages, "Application");
            } else if (rootElement.contains("Partition") || rootElement.contains("PseudoPartition")) {
                return validateSingleFile(includedFile, partitionSchema, errorMessages, "Partition");
            } else {
                // 默认使用 Application Schema
                return validateSingleFile(includedFile, applicationSchema, errorMessages, "Application");
            }
        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + includedFile.getAbsolutePath() + "]。";
            errorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }

    /**
     * validateWithXInclude
     *
     * @brief
     * 功能: 展开 XInclude 后执行 XML Schema 校验。
     * @param[in]  xmlFile       XML 配置文件。
     * @param[in]  schema        XSD Schema 对象。
     * @param[out] errorMessages 错误信息列表。
     * @param[in]  configType    配置类型名称。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateWithXInclude(File xmlFile, Schema schema, List<String> errorMessages,
            String schemaType) {
        ValidationErrorHandler errorHandler = null;
        try {
            // 检查被包含的文件是否存在
            List<File> includedFiles = extractIncludedFiles(xmlFile);
            for (File includedFile : includedFiles) {
                if (!includedFile.exists()) {
                    Log.error(CodeConstant.ERROR_XML_READ_FAILED,
                            "被包含的文件不存在: " + includedFile.getAbsolutePath());
                }
            }

            // 使用支持环境变量的XML解析
            org.w3c.dom.Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, true);

            // 注意：这里的错误会显示为 Module 文件，因为是整体验证
            // 但由于阶段1已经单独验证了每个文件，这里主要用于跨文件检查
            Validator validator = schema.newValidator();
            List<String> xincludeErrors = new ArrayList<>();
            // 阶段1已经逐个校验并报告各被包含文件的CRC32格式；XInclude整体验证
            // 的systemId是Module.xml，不能再次把CoreOS等被包含文件误报成Module文件。
            errorHandler = new ValidationErrorHandler(xincludeErrors, true);
            validator.setErrorHandler(errorHandler);

            javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(doc);
            domSource.setSystemId(xmlFile.toURI().toString());

            validator.validate(domSource);

            // 只添加阶段1没有发现的新错误（通常是跨文件的整体性问题）
            // 过滤掉重复的错误
            for (String error : xincludeErrors) {
                if (!isErrorAlreadyReported(error, errorMessages)) {
                    errorMessages.add("[XInclude整体验证] " + error);
                }
            }

            return !errorHandler.hasErrors();
        } catch (Exception e) {
            // 同validateSingleFile：仅有CRC32格式告警时允许XInclude整体验证通过。
            if (errorHandler != null && errorHandler.hasOnlyCrc32FormatWarnings()) {
                return true;
            }
            // 添加详细的异常信息
            Log.info("XInclude 展开失败，异常类型: " + e.getClass().getName());
            Log.info("异常消息: " + e.getMessage());
            if (e.getCause() != null) {
                Log.info("异常原因: " + e.getCause().getMessage());
                Log.info("异常原因类型: " + e.getCause().getClass().getName());
            }

            // 如果是 SAXParseException，输出更详细的信息
            if (e instanceof org.xml.sax.SAXParseException) {
                org.xml.sax.SAXParseException saxEx = (org.xml.sax.SAXParseException) e;
                Log.info("SAX 解析错误详情:");
                Log.info("  - 文件: " + saxEx.getSystemId());
                Log.info("  - 行号: " + saxEx.getLineNumber());
                Log.info("  - 列号: " + saxEx.getColumnNumber());
                Log.info("  - 公共ID: " + saxEx.getPublicId());

                // 检查是否有嵌套的异常
                if (saxEx.getException() != null) {
                    Log.info("  - 嵌套异常: " + saxEx.getException().getClass().getName());
                    Log.info("  - 嵌套异常消息: " + saxEx.getException().getMessage());

                    // 递归打印所有嵌套异常
                    Throwable nested = saxEx.getException();
                    int level = 1;
                    while (nested != null && level < 5) {
                        Log.info("  - 嵌套异常 [" + level + "]: " + nested.getClass().getName());
                        Log.info("  - 嵌套异常消息 [" + level + "]: " + nested.getMessage());
                        nested = nested.getCause();
                        level++;
                    }
                }
            }

            // PR_33: XInclude展开失败时应使用错误码（1019读取失败或1017解析失败），而非警告码4005
            // 检查是否为文件读取错误（如权限被拒绝）使用1019，其他解析错误使用1017
            // 根据测试规则，1017和1019属于1001-1019范围，必须立即终止且不输出log/report
            if (isFileReadError(e)) {
                String errorMsg = "XML file "
                        + XmlParser.extractFailedReadFileName(e, xmlFile.getPath()) + " read failed.";
                Log.error(CodeConstant.ERROR_XML_READ_FAILED, errorMsg);
                // 1019错误立即终止
                com.nwpu.verify.Application.exitWithErrorCode(CodeConstant.ERROR_XML_READ_FAILED);
            } else {
                String errorMsg = "XML file " + xmlFile.getName() + " parsing failed. the reason : "
                        + formatXmlErrorMessage(e) + ".";
                Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
                // 1017错误立即终止
                com.nwpu.verify.Application.exitWithErrorCode(CodeConstant.ERROR_XML_PARSE_FAILED);
            }
            return false; // 永远不会执行到这里，但保留以满足语法
        }
    }

    /**
     * 从 XML 文件中提取所有 xi:include 引用的文件
     */
    private static List<File> extractIncludedFiles(File xmlFile) {
        List<File> includedFiles = new ArrayList<>();
        try {
            // 使用支持环境变量/重复属性容错的解析器
            org.w3c.dom.Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, false);

            // 查找所有 xi:include 元素
            org.w3c.dom.NodeList includes = doc.getElementsByTagNameNS("http://www.w3.org/2001/XInclude", "include");

            File parentDir = xmlFile.getParentFile();
            for (int i = 0; i < includes.getLength(); i++) {
                org.w3c.dom.Element includeElement = (org.w3c.dom.Element) includes.item(i);
                String href = includeElement.getAttribute("href");
                if (href != null && !href.isEmpty()) {
                    // 解析环境变量
                    String resolvedHref = XmlParser.resolveEnvVariables(href);
                    File includedFile;
                    // 如果解析后的路径是绝对路径，直接使用；否则相对于parentDir
                    if (new File(resolvedHref).isAbsolute()) {
                        includedFile = new File(resolvedHref);
                    } else {
                        includedFile = new File(parentDir, resolvedHref);
                    }
                    includedFiles.add(includedFile);
                }
            }
        } catch (Exception e) {
            Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                    "Failed to extract included files from: " + xmlFile.getName() + " - " + e.getMessage());
        }
        return includedFiles;
    }

    /**
     * 检查错误是否已经被报告过
     */
    private static boolean isErrorAlreadyReported(String newError, List<String> existingErrors) {
        // 提取错误的核心信息（忽略文件名和行号的差异）
        String coreError = newError.replaceAll("\\[File:.*?\\]", "").replaceAll("\\[Line:.*?\\]", "").trim();

        for (String existing : existingErrors) {
            String coreExisting = existing.replaceAll("\\[File:.*?\\]", "").replaceAll("\\[Line:.*?\\]", "").trim();
            if (coreError.equals(coreExisting)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @brief
     * 功能: 批量校验 XML 文件列表。
     * @param[in] filesToValidate 待校验文件列表。
     * @param[in] schema          XSD Schema 对象。
     * @param[in] configType      配置类型名称。
     * @return 文件到错误信息列表的映射。
     * @scope internal
     */
    private static Map<File, List<String>> validateXmlFileList(List<File> filesToValidate,
            Schema schema, boolean isSchemaInitialized, String schemaType) {
        Map<File, List<String>> validationResults = new HashMap<>();

        if (!isSchemaInitialized) {
            // Schema未初始化时由主流程统一归类为1018。
            return validationResults;
        }

        if (filesToValidate == null || filesToValidate.isEmpty()) {
            Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                    "No files provided for " + schemaType + " validation.");
            return validationResults;
        }

        Log.info("Starting " + schemaType + " validation of " + filesToValidate.size() + " files");

        for (File xmlFile : filesToValidate) {
            List<String> errorMessagesForFile = new ArrayList<>();
            try {
                boolean isValid = validateXml(xmlFile, errorMessagesForFile, schema, isSchemaInitialized, schemaType);
                if (isValid) {
                    Log.info("Successfully validated " + schemaType + " file: " + xmlFile.getName());
                }
            } catch (IOException e) {
                Log.error(CodeConstant.ERROR_XML_PARSE_FAILED,
                        "IOException for " + schemaType + " file '" + xmlFile.getName() + "': " + e.getMessage());
                errorMessagesForFile.add("IOException: " + e.getMessage());
                // 错误码1017: XML解析失败时立即终止程序，不输出log和report
                com.nwpu.verify.Application.exitWithErrorCode(CodeConstant.ERROR_XML_PARSE_FAILED);
            } catch (Exception e) {
                Log.error(CodeConstant.ERROR_XML_PARSE_FAILED,
                        "Unexpected error for " + schemaType + " file '" + xmlFile.getName() + "': " + e.getMessage());
                errorMessagesForFile.add("Error: " + e.getMessage());
                // 错误码1017: XML解析失败时立即终止程序，不输出log和report
                com.nwpu.verify.Application.exitWithErrorCode(CodeConstant.ERROR_XML_PARSE_FAILED);
            }
            validationResults.put(xmlFile, errorMessagesForFile);
        }

        Log.info(schemaType + " validation process completed for all files");
        return validationResults;
    }

    // PR_XX: 行号映射表 - 用于将XInclude展开后的行号映射回原始文件行号
    // Key: "elementName:attributeName:attributeValue" 或 "elementName:attributeName"
    // Value: 原始文件中的行号
    private static Map<String, Integer> lineNumberMapping = new HashMap<>();

    // PR_37: 元素行号映射表 - 记录每个元素的行号（包括没有属性的元素）
    // Key: "elementName:ELEMENT:lineNumber"
    // Value: 行号
    private static Map<String, Integer> elementLineMapping = new HashMap<>();

    // 元素起始行 -> 直接父元素名，用于区分“父节点下未定义”和“仅 sequence 顺序不符”。
    private static Map<String, String> elementParentMapping = new HashMap<>();

    // 元素起始行 -> 父元素实例；父元素实例 -> 直接子元素数量。
    private static Map<String, String> elementParentInstanceMapping = new HashMap<>();
    private static Map<String, Map<String, Integer>> directChildCountByParentInstance = new HashMap<>();
    private static Map<Integer, String> lineParentInstanceMapping = new HashMap<>();

    private static File currentMappingFile = null;

    /**
     * 构建行号映射表 - 扫描原始XML文件（不展开XInclude）
     * PR_37: 同时记录元素本身的行号，用于修正属性缺失错误的行号
     */
    private static void buildLineNumberMapping(File xmlFile) {
        if (xmlFile.equals(currentMappingFile)) {
            return; // 已经为这个文件构建过映射
        }

        lineNumberMapping.clear();
        elementLineMapping.clear(); // PR_37: 清空元素行号映射
        elementParentMapping.clear();
        elementParentInstanceMapping.clear();
        directChildCountByParentInstance.clear();
        lineParentInstanceMapping.clear();
        currentMappingFile = xmlFile;

        try {
            javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);

            javax.xml.parsers.SAXParser parser = factory.newSAXParser();
            org.xml.sax.XMLReader reader = parser.getXMLReader();

            // 使用自定义Handler来记录行号
            reader.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
                private org.xml.sax.Locator locator;
                private final java.util.Deque<String> elementStack = new java.util.ArrayDeque<>();
                private final java.util.Deque<String> elementInstanceStack = new java.util.ArrayDeque<>();
                private int elementSequence = 0;

                /**
                 * setDocumentLocator
                 *
                 * @brief
                 * 功能: 执行 setDocumentLocator 相关处理。
                 *
                 * @scope external
                 * @errno:
                 * N/A
                 */
                @Override
                public void setDocumentLocator(org.xml.sax.Locator locator) {
                    this.locator = locator;
                }

                /**
                 * startElement
                 *
                 * @brief
                 * 功能: 执行 startElement 相关处理。
                 *
                 * @scope external
                 * @errno:
                 * N/A
                 */
                @Override
                public void startElement(String uri, String localName, String qName,
                        org.xml.sax.Attributes attributes) {
                    String elementName = localName.isEmpty() ? qName : localName;
                    int lineNum = locator != null ? locator.getLineNumber() : -1;
                    String elementInstance = elementName + "@" + lineNum + "#" + (++elementSequence);
                    if (locator != null) {
                        // 为没有任何子元素的父节点也建立空计数表，便于独立发现必选子元素缺失。
                        directChildCountByParentInstance.putIfAbsent(elementInstance, new HashMap<>());
                        // PR_37: 记录元素本身的行号
                        String elementKey = elementName + ":ELEMENT:" + lineNum;
                        elementLineMapping.put(elementKey, lineNum);

                        if (!elementStack.isEmpty()) {
                            elementParentMapping.put(elementName + ":PARENT:" + lineNum, elementStack.peek());
                            String parentInstance = elementInstanceStack.peek();
                            elementParentInstanceMapping.put(
                                    elementName + ":PARENT_INSTANCE:" + lineNum, parentInstance);
                            lineParentInstanceMapping.put(lineNum, parentInstance);
                            Map<String, Integer> childCounts = directChildCountByParentInstance.computeIfAbsent(
                                    parentInstance, key -> new HashMap<>());
                            childCounts.put(elementName, childCounts.getOrDefault(elementName, 0) + 1);
                        }

                        // 为每个属性创建映射键
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String attrName = attributes.getLocalName(i);
                            if (attrName.isEmpty()) {
                                attrName = attributes.getQName(i);
                            }
                            String attrValue = attributes.getValue(i);

                            // 创建唯一键：元素名:属性名:属性值
                            String key = elementName + ":" + attrName + ":" + attrValue;
                            lineNumberMapping.put(key, lineNum);

                            // 也存储不带值的键，用于后备匹配
                            String keyWithoutValue = elementName + ":" + attrName + ":LINE:" + lineNum;
                            lineNumberMapping.put(keyWithoutValue, lineNum);
                        }
                    }
                    elementStack.push(elementName);
                    elementInstanceStack.push(elementInstance);
                }

                /**
                 * endElement
                 *
                 * @brief
                 * 功能: 执行 endElement 相关处理。
                 *
                 * @scope external
                 * @errno:
                 * N/A
                 */
                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (!elementStack.isEmpty()) {
                        elementStack.pop();
                    }
                    if (!elementInstanceStack.isEmpty()) {
                        elementInstanceStack.pop();
                    }
                }
            });

            reader.parse(new org.xml.sax.InputSource(xmlFile.toURI().toString()));
        } catch (Exception e) {
            // 映射构建失败不影响验证，只是行号可能不准确
            Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                    "Failed to build line number mapping: " + e.getMessage());
        }
    }

    /**
     * 根据元素名、属性名和属性值查找原始行号
     */
    private static int findOriginalLineNumber(String elementName, String attributeName,
            String attributeValue, int fallbackLineNumber) {
        if (lineNumberMapping.isEmpty()) {
            return fallbackLineNumber;
        }

        // 尝试精确匹配
        String exactKey = elementName + ":" + attributeName + ":" + attributeValue;
        Integer lineNum = lineNumberMapping.get(exactKey);
        if (lineNum != null) {
            return lineNum;
        }

        // 尝试模糊匹配（只匹配元素名和属性名）
        String prefix = elementName + ":" + attributeName + ":";
        for (Map.Entry<String, Integer> entry : lineNumberMapping.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix)) {
                return entry.getValue();
            }
        }
        return fallbackLineNumber;
    }

    /**
     * 自定义的SAX错误处理器
     */
    private static class ValidationErrorHandler extends DefaultHandler {
        private final List<String> externalMessagesList;
        private final boolean suppressCrc32FormatWarnings;
        private boolean errorsFound = false;
        private boolean crc32FormatWarningFound = false;

        // PR_02: 用于去重，避免同一错误被error()和fatalError()重复记录
        private final java.util.Set<String> processedErrors = new java.util.HashSet<>();

        // PR_XX: 用于去重，避免不同原始cvc-*错误映射成同一条业务错误消息时重复记录
        private final java.util.Set<String> processedFormattedMessages = new java.util.HashSet<>();

        // PR_04: 缓存最近一次cvc-attribute.3错误中的元素名和属性名
        // 用于在处理cvc-enumeration-valid时获取上下文信息
        private String lastAttributeElementName = null;
        private String lastAttributeName = null;
        private int lastAttributeLineNumber = -1;

        // PR_XX: 缓存最后一次看到的枚举值列表（从cvc-enumeration-valid中提取）
        // 用于cvc-enumeration-valid过滤后传递给cvc-attribute.3使用
        private String lastEnumValues = null;
        private int lastEnumLineNumber = -1;

        // TR_PR_022 (CPMVerify-TR-006361): 可扩展枚举属性白名单。
        // XSD已在两个精确位置使用xs:string约束；该白名单仅作为不同验证器仍返回
        // 枚举错误时的兼容处理，不能按属性名全局放行同名的严格属性。
        private static final java.util.Set<String> EXTENSIBLE_ENUM_ATTRIBUTES =
                new java.util.HashSet<>(java.util.Arrays.asList(
                        "ModuleRecoveryAction",
                        "PartitionRecoveryAction"));

        // TR_PR_022: 待豁免判定的 cvc-enumeration-valid 错误计数
        // cvc-enumeration-valid 先于伴随的 cvc-attribute.3 到达且不含属性名，
        // 暂缓置位 errorsFound：若伴随错误命中白名单则整体豁免，否则由伴随错误置位失败
        private int pendingEnumErrorCount = 0;

        // PR_XX: 缓存最后一次看到的长度范围（从cvc-maxLength-valid等中提取）
        // 用于长度验证错误过滤后传递给cvc-attribute.3使用
        private String lastLengthRange = null;
        private int lastLengthLineNumber = -1;

        // PR_37: 用于记录已经报告过同名子元素错误（2007）的行号
        // 避免对同一行的多个XSD错误重复报告
        private final java.util.Set<Integer> reportedSameNameChildElementLines = new java.util.HashSet<>();

        // 用于抑制该元素的所有2011错误（属性缺失）
        private final java.util.Set<String> reportedSameNameChildElements = new java.util.HashSet<>();

        // 已输出2009/2015的“父元素实例+缺失子元素”，避免XSD回调和独立计数重复输出。
        private final java.util.Set<String> reportedMissingRequiredElements = new java.util.HashSet<>();

        // PR_12: 用于存储待确认的顶层元素属性缺失错误
        // 这些错误在验证结束后，根据是否有同名子元素错误来决定是否报告
        private final java.util.List<PendingAttributeMissingError> pendingAttributeMissingErrors = new java.util.ArrayList<>();

        /**
         * PR_12: 待确认的属性缺失错误信息
         */
        private static class PendingAttributeMissingError {
            final String fileName;
            final int lineNumber;
            final String elementName;
            final String attributeName;

            PendingAttributeMissingError(String fileName, int lineNumber, String elementName, String attributeName) {
                this.fileName = fileName;
                this.lineNumber = lineNumber;
                this.elementName = elementName;
                this.attributeName = attributeName;
            }
        }

        public ValidationErrorHandler(List<String> messagesList) {
            this(messagesList, false);
        }

        public ValidationErrorHandler(List<String> messagesList, boolean suppressCrc32FormatWarnings) {
            this.externalMessagesList = messagesList;
            this.suppressCrc32FormatWarnings = suppressCrc32FormatWarnings;
        }

        /**
         * 元素在 XSD 中是否声明了子元素（用于同名嵌套误报区分）。
         * 不再维护 Java 侧顶层元素白名单。
         */
        private boolean isTopLevelElement(String elementName) {
            return hasDeclaredChildrenInXsd(elementName);
        }

        /**
         * 期望元素是否是实际元素在 XSD 中声明的子元素（同名嵌套场景）。
         */
        private boolean isSameNameChildElement(String actualElement, java.util.List<String> expectedElements) {
            if (actualElement == null || expectedElements == null || expectedElements.isEmpty()) {
                return false;
            }
            for (String expected : expectedElements) {
                if (isValidChildOfContainerInXsd(expected, actualElement)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 元素是否是指定容器在 XSD 中声明的合法子元素。
         */
        private boolean isValidChildOfContainer(String elementName, String containerName) {
            return isValidChildOfContainerInXsd(elementName, containerName);
        }

        /**
         * 处理“必选父元素标签被删除，但其原有子元素仍保留并越级出现”的场景。
         *
         * <p>XSD通常只回调一条“Invalid content ... is expected”错误，原有逻辑会
         * 将越级子元素报告为2007，但不会再从该回调推导出必选父元素缺失。这里根据
         * 原始XML的父节点实例和XSD候选列表确认缺失项，补报2009和2015。</p>
         */
        private void recordMissingRequiredElementForMisplacedChild(String level, String fileName,
                int lineNumber, String actualElement, java.util.List<String> expectedElements,
                String errorMsg, String systemId) {
            if (actualElement == null || actualElement.isEmpty() || expectedElements.isEmpty()) {
                return;
            }

            String parentName = elementParentMapping.get(actualElement + ":PARENT:" + lineNumber);
            String parentInstance = elementParentInstanceMapping.get(
                    actualElement + ":PARENT_INSTANCE:" + lineNumber);
            if (parentInstance == null) {
                parentInstance = lineParentInstanceMapping.get(lineNumber);
            }
            if (parentName == null) {
                parentName = parentNameFromInstanceKey(parentInstance);
            }
            if (parentName == null || parentInstance == null) {
                return;
            }

            String missingElement = findMissingRequiredChild(parentName, parentInstance, expectedElements);
            if (missingElement == null || !isRequiredChildInXsd(missingElement, parentName)) {
                return;
            }

            String missingKey = parentInstance + ":" + missingElement;
            if (hasReportedMissingRequiredElement(parentInstance, parentName, missingElement)) {
                return;
            }

            Integer directCount = directChildCount(parentInstance, missingElement);
            if (directCount == null) {
                return;
            }
            String min = resolveMinOccursForMessage(missingElement, actualElement, errorMsg);
            String occursMessage = formatOccursLessThanMessage(fileName, lineNumber, missingElement,
                    directCount, min);
            recordFormattedError(level, CodeConstant.ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET, occursMessage);

            String requiredMessage = String.format(
                    "[File: %s, Line: %d] Required element '%s' is missing in parent element '%s'.",
                    fileName, lineNumber, missingElement, parentName);
            recordFormattedError(level, CodeConstant.ERROR_XML_REQUIRED_ELEMENT_MISSING, requiredMessage);
            markMissingRequiredElementReported(parentInstance, parentName, missingElement);
        }

        /**
         * hasReportedMissingRequiredElement
         *
         * @brief
         * 功能: 执行 hasReportedMissingRequiredElement 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private boolean hasReportedMissingRequiredElement(String parentInstance, String parentName,
                String elementName) {
            if (elementName == null || elementName.isEmpty()) {
                return false;
            }
            if (parentInstance != null
                    && reportedMissingRequiredElements.contains(parentInstance + ":" + elementName)) {
                return true;
            }
            return parentName != null
                    && reportedMissingRequiredElements.contains("PARENT:" + parentName + ":" + elementName);
        }

        /**
         * markMissingRequiredElementReported
         *
         * @brief
         * 功能: 执行 markMissingRequiredElementReported 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private void markMissingRequiredElementReported(String parentInstance, String parentName,
                String elementName) {
            if (elementName == null || elementName.isEmpty()) {
                return;
            }
            if (parentInstance != null) {
                reportedMissingRequiredElements.add(parentInstance + ":" + elementName);
            } else if (parentName != null) {
                // 某些XSD错误定位在父元素结束标签，无法映射到实例起始行；
                // 这时用父元素名作为回退键，避免独立审计重复报告。
                reportedMissingRequiredElements.add("PARENT:" + parentName + ":" + elementName);
            }
        }

        /**
         * 独立按原始XML父节点实例和XSD最小出现次数检查必选子元素。
         * 不依赖XSD错误回调是否把“缺失”描述成当前位置错误，保证各种缺失方式都能输出2009/2015。
         */
        private void auditMissingRequiredElements(File xmlFile) {
            if (xmlFile == null || externalMessagesList == null) {
                return;
            }
            ensureXsdStructureIndex();
            String fileName = xmlFile.getName();
            for (Map.Entry<String, Map<String, Integer>> entry : directChildCountByParentInstance.entrySet()) {
                String parentInstance = entry.getKey();
                String parentName = parentNameFromInstanceKey(parentInstance);
                java.util.List<XsdElementDecl> declarations = getDirectChildDeclarations(parentName);
                if (declarations == null || declarations.isEmpty()) {
                    continue;
                }

                for (XsdElementDecl declaration : declarations) {
                    if (!declaration.required) {
                        continue;
                    }
                    int minOccurs;
                    try {
                        minOccurs = Integer.parseInt(declaration.minOccurs);
                    } catch (NumberFormatException ignored) {
                        continue;
                    }
                    int actualCount = entry.getValue().getOrDefault(declaration.name, 0);
                    if (actualCount >= minOccurs) {
                        continue;
                    }

                    String missingKey = parentInstance + ":" + declaration.name;
                    if (hasReportedMissingRequiredElement(parentInstance, parentName, declaration.name)) {
                        continue;
                    }
                    int lineNumber = elementStartLineFromInstanceKey(parentInstance);
                    if (lineNumber < 1) {
                        lineNumber = 1;
                    }
                    String occursMessage = formatOccursLessThanMessage(fileName, lineNumber, declaration.name,
                            actualCount, declaration.minOccurs);
                    recordFormattedError("Error", CodeConstant.ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET,
                            occursMessage);
                    String requiredMessage = String.format(
                            "[File: %s, Line: %d] Required element '%s' is missing in parent element '%s'.",
                            fileName, lineNumber, declaration.name, parentName);
                    recordFormattedError("Error", CodeConstant.ERROR_XML_REQUIRED_ELEMENT_MISSING,
                            requiredMessage);
                    markMissingRequiredElementReported(parentInstance, parentName, declaration.name);
                }
            }
        }

        /**
         * 豁免纯 sequence 顺序错误产生的2007、2009/2015或2010误报。
         * 元素必须是报错行父节点在 XSD 中声明的直接子元素，并按同一父元素实例
         * 核对完整数量；未定义、放错父节点、数量超限和必要元素真实缺失均不豁免。
         */
        private boolean isIgnorableSequenceOrderError(SAXParseException e) {
            String errorMsg = e.getMessage();
            boolean invalidContent = errorMsg != null
                    && errorMsg.contains("Invalid content was found starting with element");
            boolean hasExpectedElements = invalidContent && errorMsg.contains("is expected");
            boolean noChildExpected = invalidContent
                    && (errorMsg.contains("cvc-complex-type.2.4.d")
                            || errorMsg.contains("No child element is expected"));
            if (!hasExpectedElements && !noChildExpected) {
                return false;
            }

            String actualElement = normalizeElementName(extractInvalidContentElement(errorMsg));
            if (actualElement.isEmpty() || "unknown".equals(actualElement)) {
                return false;
            }

            java.util.List<String> expectedElements = extractExpectedElements(errorMsg);

            String parentElement = elementParentMapping.get(
                    actualElement + ":PARENT:" + e.getLineNumber());
            if (parentElement != null) {
                if (!isSequenceContainerInXsd(parentElement)
                        || !isValidChildOfContainer(actualElement, parentElement)) {
                    return false;
                }

                String parentInstance = elementParentInstanceMapping.get(
                        actualElement + ":PARENT_INSTANCE:" + e.getLineNumber());
                Map<String, Integer> childCounts = directChildCountByParentInstance.get(parentInstance);
                if (parentInstance == null || childCounts == null) {
                    return false;
                }

                XsdElementDecl actualDeclaration = findDirectChildDeclaration(parentElement, actualElement);
                if (actualDeclaration == null
                        || exceedsMaxOccurs(childCounts.getOrDefault(actualElement, 0), actualDeclaration.maxOccurs)) {
                    return false;
                }

                // “应在当前位置之前出现”不等于“整个父节点中缺失”。只有当错误报文
                // 提到的必需元素在同一父节点内确实不足 minOccurs 时，才保留2009/2015。
                for (String expectedElement : expectedElements) {
                    XsdElementDecl expectedDeclaration = findDirectChildDeclaration(parentElement, expectedElement);
                    if (expectedDeclaration != null
                            && isBelowMinOccurs(childCounts.getOrDefault(expectedElement, 0),
                                    expectedDeclaration.minOccurs)) {
                        return false;
                    }
                }
                return true;
            } else {
                // XInclude 整体验证使用 DOMSource，JAXP 通常只给出 Module 和行号 -1。
                // 此阶段仅在所有原始文件逐个校验通过后执行，因此可用同一 XSD
                // sequence 中的兄弟关系确认这仍是已在第一阶段核实过的纯顺序错误。
                if (!suppressCrc32FormatWarnings
                        || e.getLineNumber() >= 0
                        || (!noChildExpected && !isDeclaredInSameSequence(actualElement, expectedElements))) {
                    return false;
                }
            }

            return !exceedsDeclaredMaxOccurs(e.getSystemId(), actualElement);
        }

        /**
         * exceedsMaxOccurs
         *
         * @brief
         * 功能: 执行 exceedsMaxOccurs 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private boolean exceedsMaxOccurs(int actualCount, String maxOccurs) {
            if (maxOccurs == null || maxOccurs.isEmpty() || "unbounded".equalsIgnoreCase(maxOccurs)) {
                return false;
            }
            try {
                return actualCount > Integer.parseInt(maxOccurs);
            } catch (NumberFormatException ignored) {
                return true;
            }
        }

        /**
         * isBelowMinOccurs
         *
         * @brief
         * 功能: 执行 isBelowMinOccurs 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private boolean isBelowMinOccurs(int actualCount, String minOccurs) {
            try {
                return actualCount < Integer.parseInt(minOccurs);
            } catch (NumberFormatException ignored) {
                return true;
            }
        }

        /**
         * PR_37: 记录已报告同名子元素错误的行号和元素名
         */
        private void markSameNameChildElementReported(int lineNumber, String elementName) {
            reportedSameNameChildElementLines.add(lineNumber);
            if (elementName != null && !elementName.isEmpty() && !"unknown element".equals(elementName)) {
                reportedSameNameChildElements.add(elementName);
            }
        }

        /**
         * PR_37: 检查指定行号是否已经报告过同名子元素错误
         */
        private boolean hasSameNameChildElementReported(int lineNumber) {
            return reportedSameNameChildElementLines.contains(lineNumber);
        }

        /**
         * PR_37: 检查指定元素名是否已经报告过同名子元素错误
         */
        private boolean hasSameNameChildElementReported(String elementName) {
            return elementName != null && reportedSameNameChildElements.contains(elementName);
        }

        // TR_PR_005/006: 2009/2010 新文案需要实际出现次数，按 systemId+元素名 缓存统计结果
        private final java.util.Map<String, Integer> elementCountCache = new java.util.HashMap<>();

        /**
         * countElementOccurrences
         *
         * @brief
         * 功能: 执行 countElementOccurrences 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private int countElementOccurrences(String systemId, String elementName) {
            if (elementName == null || elementName.isEmpty() || systemId == null || systemId.isEmpty()) {
                return -1;
            }
            String key = systemId + "#" + elementName;
            Integer cached = elementCountCache.get(key);
            if (cached != null) {
                return cached;
            }
            int count = -1;
            try {
                java.io.File file = new java.io.File(new java.net.URI(systemId));
                if (file.exists()) {
                    javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory
                            .newInstance();
                    factory.setNamespaceAware(true);
                    org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(file);
                    count = 0;
                    org.w3c.dom.NodeList all = doc.getElementsByTagName("*");
                    for (int i = 0; i < all.getLength(); i++) {
                        if (elementName.equals(all.item(i).getLocalName())
                                || elementName.equals(all.item(i).getNodeName())) {
                            count++;
                        }
                    }
                }
            } catch (Exception ex) {
                count = -1;
            }
            elementCountCache.put(key, count);
            return count;
        }

        /**
         * actualOccursCount
         *
         * @brief
         * 功能: 执行 actualOccursCount 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private int actualOccursCount(String systemId, String elementName) {
            return Math.max(countElementOccurrences(systemId, elementName), 0);
        }

        /**
         * 判断当前报文中的元素总数是否已经超过 XSD 声明的有限 maxOccurs。
         *
         * <p>Xerces 在 sequence/choice 中遇到重复元素时，经常将错误写成
         * "Invalid content ... No child element is expected"。这条报文与
         * “合法元素嵌套了同名的未定义元素”很相似，不能只靠元素是否拥有
         * 子元素来区分。对于已经定义且出现次数超过 XSD 上限的元素，应优先
         * 归类为 2010。</p>
         */
        private boolean exceedsDeclaredMaxOccurs(String systemId, String elementName) {
            if (elementName == null || elementName.isEmpty()) {
                return false;
            }
            String max = getMaxOccursFromXsd(elementName, null);
            if (max == null || max.isEmpty() || "unbounded".equalsIgnoreCase(max)) {
                return false;
            }
            try {
                int actual = actualOccursCount(systemId, elementName);
                return actual > Integer.parseInt(max);
            } catch (NumberFormatException ex) {
                return false;
            }
        }

        // TR_PR_011: "Valid range: '0' to '255'" —— 将 "0, 255" 拆为两端
        /**
         * splitRangePair
         *
         * @brief
         * 功能: 执行 splitRangePair 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private static String[] splitRangePair(String rangeStr) {
            String[] result = new String[] { "", "" };
            if (rangeStr != null && !rangeStr.isEmpty()) {
                String[] parts = rangeStr.split(",", 2);
                result[0] = parts[0].trim();
                result[1] = parts.length > 1 ? parts[1].trim() : "";
            }
            return result;
        }

        // TR_PR_008: 2013 新文案，附加 Expected type
        /**
         * formatTypeErrorMessage
         *
         * @brief
         * 功能: 执行 formatTypeErrorMessage 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String formatTypeErrorMessage(String fileName, int lineNumber, String elementName,
                String attributeName, String errorMsg) {
            String expectedType = extractTypeNameFromMessage(errorMsg);
            if (expectedType != null && !expectedType.isEmpty()) {
                return String.format(
                        "[File: %s, Line: %d] XML element %s attribute %s type error, Expected type: '%s'.",
                        fileName, lineNumber, elementName, attributeName, expectedType);
            }
            return String.format("[File: %s, Line: %d] XML element %s attribute %s type error.",
                    fileName, lineNumber, elementName, attributeName);
        }

        // TR_PR_009/011: 2014 新文案
        /**
         * formatValueOutOfRangeMessage
         *
         * @brief
         * 功能: 执行 formatValueOutOfRangeMessage 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private static String formatValueOutOfRangeMessage(String fileName, int lineNumber, String attributeValue,
                String attributeName, String elementName, String rangeStr) {
            String[] range = splitRangePair(rangeStr);
            return String.format(
                    "[File: %s, Line: %d] Value '%s' is not allowed for attribute '%s' in element '%s'. Valid range: '%s' to '%s'.",
                    fileName, lineNumber, attributeValue == null ? "" : attributeValue,
                    attributeName, elementName, range[0], range[1]);
        }

        // 006360: 字符串长度违规专用文案
        /**
         * formatLengthInvalidMessage
         *
         * @brief
         * 功能: 执行 formatLengthInvalidMessage 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private static String formatLengthInvalidMessage(String fileName, int lineNumber, String attributeValue,
                String attributeName, String elementName, String rangeStr) {
            String[] range = splitRangePair(rangeStr);
            return String.format(
                    "[File: %s, Line: %d] Length of '%s' is not allowed for attribute '%s' in element '%s'. Length range: '%s' to '%s'.",
                    fileName, lineNumber, attributeValue == null ? "" : attributeValue,
                    attributeName, elementName, range[0], range[1]);
        }

        // TR_PR_005: 2009 新文案（time(s) 为字面输出，与表1模板一致）
        /**
         * formatOccursLessThanMessage
         *
         * @brief
         * 功能: 执行 formatOccursLessThanMessage 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private static String formatOccursLessThanMessage(String fileName, int lineNumber, String elementName,
                int actualCount, String min) {
            return String.format("[File: %s, Line: %d] element %s occurs %d time(s) less than minimum %s time(s).",
                    fileName, lineNumber, elementName, actualCount, min);
        }

        // TR_PR_006: 2010 新文案（time(s) 为字面输出，与表1模板一致）
        /**
         * formatOccursGreaterThanMessage
         *
         * @brief
         * 功能: 执行 formatOccursGreaterThanMessage 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private static String formatOccursGreaterThanMessage(String fileName, int lineNumber, String elementName,
                int actualCount, String max) {
            return String.format("[File: %s, Line: %d] element %s occurs %d time(s) greater than %s time(s).",
                    fileName, lineNumber, elementName, actualCount, max);
        }

        /**
         * 记录一条已经完成格式化的校验错误。
         *
         * <p>同一条 XSD 错误有时同时对应多个需求错误码（例如必要元素
         * 缺失同时满足 2009 和 2015），因此不能只依赖 addMessage 末尾的
         * 单条 errorCode/formattedMessage 变量。</p>
         */
        private void recordFormattedError(String level, Integer errorCode, String message) {
            if (errorCode == null || message == null || message.isEmpty()
                    || processedFormattedMessages.contains(message)) {
                return;
            }
            processedFormattedMessages.add(message);
            externalMessagesList.add(message);
            if ("Error".equals(level) || "Fatal Error".equals(level)) {
                Log.error(errorCode, message);
            }
        }

        /**
         * addMessage
         *
         * @brief
         * 功能: 执行 addMessage 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private void addMessage(String level, SAXParseException e) {
            String errorMsg = e.getMessage();
            int lineNumber = e.getLineNumber();
            String fileName = extractFileName(e.getSystemId(), e.getPublicId());
            String systemId = e.getSystemId();
            // 记录“必选父元素缺失但其子元素越级出现”的场景。
            // 先输出原始2007，再在当前错误处理完成后补充2009和2015。
            String misplacedElementForMissingCheck = null;
            java.util.List<String> expectedElementsForMissingCheck = null;

            // 调试开关：输出原始XSD验证器报文（默认关闭）
            // 通过设置环境变量 CPM_XSD_DEBUG_RAW=1 开启
            // 注意：使用 stdout，避免 PowerShell 将 stderr 视为错误流。
            if ("1".equals(System.getenv("CPM_XSD_DEBUG_RAW"))) {
                System.out.println(
                        "CPM_XSD_RAW: [File: " + fileName + ", Line: " + lineNumber + "] " + errorMsg);
            }

            // PR_XX: 对于无法从报文中提取 element/attribute 的类型错误，直接跳过
            // 这类报文（如 cvc-type.3.1.3 / cvc-datatype-valid）通常会同时伴随更详细的 cvc-attribute.3。
            // 若继续处理会产生 "unknown element/unknown attribute" 的脏输出。
            if (errorMsg != null && (errorMsg.contains("cvc-type.3.1.3") || errorMsg.contains("cvc-datatype-valid"))) {
                String extractedElement = extractElementName(errorMsg);
                String extractedAttribute = extractAttributeName(errorMsg);
                if ("unknown element".equals(extractedElement) && "unknown attribute".equals(extractedAttribute)) {
                    return;
                }
            }

            // PR_XX: 尝试使用行号映射修正XInclude展开后的行号
            // 从错误消息中提取元素名、属性名和属性值，然后查找原始行号
            if (!lineNumberMapping.isEmpty() && errorMsg != null && isSameAsCurrentMappingFile(systemId)) {
                String elementName = extractElementName(errorMsg);
                String attributeName = extractAttributeName(errorMsg);
                String attributeValue = extractAttributeValue(errorMsg);

                if (elementName != null && !elementName.equals("unknown element") &&
                        attributeName != null && !attributeName.equals("unknown attribute")) {
                    int originalLineNumber = findOriginalLineNumber(elementName, attributeName,
                            attributeValue != null ? attributeValue : "", lineNumber);
                    if (originalLineNumber != lineNumber) {
                        lineNumber = originalLineNumber;
                    }
                }
            }

            // PR_02: 生成唯一标识，用于去重
            String errorKey = fileName + ":" + lineNumber + ":" + errorMsg;
            if (processedErrors.contains(errorKey)) {
                return; // 已处理过，跳过
            }
            processedErrors.add(errorKey);

            // DEBUG: 打印原始错误消息到日志（仅用于调试）
            // System.err.println("DEBUG [File: " + fileName + ", Line " + lineNumber + "]:
            // " + errorMsg);

            // 过滤掉 cvc-pattern-valid 错误（因为会同时报告 cvc-attribute.3，后者更详细）
            if (errorMsg != null && errorMsg.contains("cvc-pattern-valid")) {
                return; // 直接跳过，不报告
            }

            // PR_XX: 过滤掉 cvc-minInclusive-valid/cvc-maxInclusive-valid 等错误
            // 因为会同时报告 cvc-attribute.3，后者包含元素名和属性名，更详细
            if (errorMsg != null && (errorMsg.contains("cvc-minInclusive-valid") ||
                    errorMsg.contains("cvc-maxInclusive-valid") ||
                    errorMsg.contains("cvc-minExclusive-valid") ||
                    errorMsg.contains("cvc-maxExclusive-valid") ||
                    errorMsg.contains("cvc-totalDigits-valid") ||
                    errorMsg.contains("cvc-fractionDigits-valid"))) {
                return; // 直接跳过，等待 cvc-attribute.3 提供更详细的信息
            }

            // PR_XX: 过滤掉 cvc-enumeration-valid 错误
            // 因为会同时报告 cvc-attribute.3，后者包含元素名和属性名，更详细
            // cvc-enumeration-valid 消息格式不包含元素名和属性名
            // PR_XX: 在过滤前先缓存枚举值列表，供后续cvc-attribute.3使用
            if (errorMsg != null && errorMsg.contains("cvc-enumeration-valid")) {
                String enumValues = extractEnumValues(errorMsg);
                if (!enumValues.isEmpty()) {
                    lastEnumValues = enumValues;
                    lastEnumLineNumber = lineNumber;
                }
                return; // 直接跳过，等待 cvc-attribute.3 提供更详细的信息
            }

            // PR_13: 处理 cvc-datatype-valid.1.2.1 错误（不再无条件跳过）
            // 尝试从错误消息中提取元素名和属性名，如果能提取到则直接报告2013错误
            // 如果提取不到，则跳过等待后续的 cvc-attribute.3
            if (errorMsg != null && errorMsg.contains("cvc-datatype-valid.1.2.1")) {
                String extractedElement = extractElementName(errorMsg);
                String extractedAttribute = extractAttributeName(errorMsg);

                // 如果无法提取到有效信息，跳过等待后续的 cvc-attribute.3
                if ("unknown element".equals(extractedElement) || "unknown".equals(extractedElement) ||
                        "unknown attribute".equals(extractedAttribute) || "unknown".equals(extractedAttribute)) {
                    return; // 跳过，等待 cvc-attribute.3 提供更详细的信息
                }

                // 能提取到有效信息，直接报告2013错误（类型错误）
                // TR_PR_008: 附加 Expected type 信息
                String formattedMsg = formatTypeErrorMessage(fileName, lineNumber, extractedElement,
                        extractedAttribute, errorMsg);

                // 检查去重
                if (!processedFormattedMessages.contains(formattedMsg)) {
                    processedFormattedMessages.add(formattedMsg);
                    Log.error(CodeConstant.ERROR_XML_ATTRIBUTE_DATA_TYPE_INVALID, formattedMsg);
                    externalMessagesList.add(formattedMsg);
                    errorsFound = true;
                }
                return; // 处理完成，不再继续
            }

            // PR_XX: 过滤掉 cvc-minLength-valid/cvc-maxLength-valid/cvc-length-valid 错误
            // 因为会同时报告 cvc-attribute.3，后者包含元素名和属性名，更详细
            // PR_XX: 在过滤前先缓存长度范围，供后续cvc-attribute.3使用
            if (errorMsg != null && (errorMsg.contains("cvc-minLength-valid") ||
                    errorMsg.contains("cvc-maxLength-valid") ||
                    errorMsg.contains("cvc-length-valid"))) {
                String lengthRange = extractLengthRange(errorMsg);
                if (!lengthRange.isEmpty()) {
                    lastLengthRange = lengthRange;
                    lastLengthLineNumber = lineNumber;
                }
                return; // 直接跳过，等待 cvc-attribute.3 提供更详细的信息
            }

            // PR_04/PR_28: 缓存元素名和属性名供后续cvc-enumeration-valid/cvc-minInclusive-valid等错误使用
            // 扩展触发条件：不仅cvc-attribute.3，还包括cvc-type.3.1.3和cvc-datatype-valid
            // PR_XX: 即使cvc-attribute.3包含范围错误信息，也要缓存元素名和属性名
            if (errorMsg != null && (errorMsg.contains("cvc-attribute.3") ||
                    errorMsg.contains("cvc-type.3.1.3") ||
                    errorMsg.contains("cvc-datatype-valid"))) {
                String extractedElement = extractElementName(errorMsg);
                String extractedAttribute = extractAttributeName(errorMsg);
                // 只有成功提取到有效信息时才更新缓存
                if (extractedElement != null && !extractedElement.isEmpty() &&
                        !"unknown element".equals(extractedElement) && !"unknown".equals(extractedElement)) {
                    lastAttributeElementName = extractedElement;
                    lastAttributeLineNumber = lineNumber;
                }
                if (extractedAttribute != null && !extractedAttribute.isEmpty() &&
                        !"unknown attribute".equals(extractedAttribute) && !"unknown".equals(extractedAttribute)) {
                    lastAttributeName = extractedAttribute;
                    lastAttributeLineNumber = lineNumber;
                }
            }

            // 识别错误类型并记录相应的日志
            Integer errorCode = null;
            String formattedMessage = null;

            // 2007: 元素未定义
            // 主要匹配：元素在XSD中确实未定义的情况
            // 1. cvc-elt.1: 找不到元素声明（根元素未定义）
            // 2. cvc-wildcard-strict: 通配符位置的未声明元素
            // 3. 明确包含 "cannot be found in the schema" 的错误
            // 注意：排除 "No child element is expected"（那是2010错误）
            // PR_12: 排除 "Invalid content was found starting with element" + "is expected"
            // 这种错误表示元素出现在错误位置，而非元素未定义
            // PR_37: 添加对 cvc-complex-type.2.1 的处理（元素不应该有子元素，但有子元素）
            if (errorMsg != null &&
                    !errorMsg.contains("No child element is expected") &&
                    !errorMsg.contains("is expected") && // PR_12: 排除位置错误
                    (// cvc-elt.1: 找不到元素声明
                    errorMsg.contains("Cannot find the declaration of element") ||
                    // cvc-wildcard-strict: 通配符的未声明元素
                            errorMsg.contains("cvc-wildcard-strict") ||
                            errorMsg.contains("cannot be found in the schema") ||
                            // 兼容中文和其他表述
                            errorMsg.contains("未定义") ||
                            errorMsg.contains("not declared") ||
                            errorMsg.contains("undefined element"))) {
                errorCode = CodeConstant.ERROR_XML_UNDEFINED_ELEMENT;
                String elementName = extractElementName(errorMsg);
                formattedMessage = String.format("[File: %s, Line: %d] undefined XML element %s.",
                        fileName, lineNumber, elementName);
            }
            // PR_37: cvc-complex-type.2.1: 元素的content type是empty，但有子元素
            // 这种情况通常是同名子元素导致的，应该报告2007错误
            // 例如：<PartitionWindow><PartitionWindow .../></PartitionWindow>
            // 错误消息格式：Element 'PartitionWindow' must have no character or element
            // information item [children]
            // 注意：XSD验证器报告的行号可能指向外层元素的开始标签位置，这是XSD验证器的行为，无法修改
            else if (errorMsg != null && errorMsg.contains("cvc-complex-type.2.1") &&
                    errorMsg.contains("content type is empty")) {
                errorCode = CodeConstant.ERROR_XML_UNDEFINED_ELEMENT;

                // 从错误消息中提取元素名
                // 格式：Element 'XXX' must have no character or element information item
                String elementName = "unknown element";
                if (errorMsg.contains("Element '")) {
                    int start = errorMsg.indexOf("Element '") + 9;
                    int end = errorMsg.indexOf("'", start);
                    if (end > start) {
                        elementName = errorMsg.substring(start, end);
                    }
                }

                formattedMessage = String.format("[File: %s, Line: %d] undefined XML element %s.",
                        fileName, lineNumber, elementName);
                markSameNameChildElementReported(lineNumber, elementName);
            }
            // 2017: 枚举值/布尔值验证失败
            // 当布尔/枚举属性值超出取值范围时，XSD验证器报告 cvc-attribute.3 with type 'booleanType' 或其他枚举类型
            // 匹配策略：
            // 1. 显式匹配booleanType、xs:boolean和null类型
            // 2. 检查是否是枚举类型（错误消息包含"is not facet-valid with respect to
            // enumeration"的后继cvc-attribute.3）
            // 3. 检查类型名称模式：如果type后面跟着单引号中的类型名，且该类型以Type结尾，很可能是枚举类型
            // 4. 检查是否有缓存的枚举值（来自之前的cvc-enumeration-valid错误）
            else if (errorMsg != null && errorMsg.contains("cvc-attribute.3") &&
                    (errorMsg.contains("'booleanType'") ||
                            errorMsg.contains("'boolean'") || // xs:boolean 内置类型
                            errorMsg.contains("'null'") ||
                            errorMsg.contains("\"null\"") ||
                            // PR_XX: 检查是否有缓存的枚举值（来自之前的cvc-enumeration-valid）
                            (lastEnumValues != null && Math.abs(lastEnumLineNumber - lineNumber) <= 1) ||
                            // PR_XX: 扩展匹配其他枚举类型
                            // 模式: "with respect to its type, 'XXXType'" 或 "type 'XXXType'"
                            // 大多数枚举类型名称以Type结尾，且不包含数值范围关键字
                            (isLikelyEnumerationType(errorMsg)))) {
                
                String elementName = extractElementName(errorMsg);
                String attributeName = extractAttributeName(errorMsg);

                // PR_XX: 确定范围显示的优先级：
                // 1. 布尔类型 - 直接使用 [true, false]
                // 2. 有缓存的枚举值 - 使用枚举值
                // 3. 有缓存的长度范围 - 使用长度范围
                // 4. 其他情况 - 从 XSD 枚举/pattern 读取
                String rangeDisplay = null;
                // 006360: 长度约束违规应报2016，而非2017(枚举值错误)
                boolean isLengthViolation = false;
                // TR_PR_011: 数值范围违规(含union类型)应报2014(Valid range)，而非2017(Valid values)
                boolean isRangeViolation = false;

                // PR_XX: 优先检查布尔类型
                if (errorMsg.contains("'booleanType'") || errorMsg.contains("'boolean'")
                        || errorMsg.contains("'null'")) {
                    rangeDisplay = "true, false"; // 布尔类型（包括自定义booleanType和内置xs:boolean）
                }
                // PR_XX: 然后检查是否有缓存的枚举值（来自之前的cvc-enumeration-valid）
                else if (lastEnumValues != null && Math.abs(lastEnumLineNumber - lineNumber) <= 1) {
                    rangeDisplay = lastEnumValues;
                    // PR_XX: 使用后清除缓存，避免影响同一行的其他属性
                    lastEnumValues = null;
                    lastEnumLineNumber = -1;
                }
                // PR_XX: 再检查是否有缓存的长度范围（来自之前的cvc-maxLength-valid等）
                else if (lastLengthRange != null && Math.abs(lastLengthLineNumber - lineNumber) <= 1) {
                    rangeDisplay = lastLengthRange;
                    isLengthViolation = true;
                    // PR_XX: 使用后清除缓存，避免影响同一行的其他属性
                    lastLengthRange = null;
                    lastLengthLineNumber = -1;
                }
                // Xerces 在部分 JDK/Locale 下只把配对的 cvc-attribute.3 传到这里，
                // 前一个 cvc-maxLength-valid 可能没有进入同一个 ErrorHandler 实例。
                // 通过 XSD 类型补回长度约束，避免受长度限制的字符串被误判为 2013。
                if (!isLengthViolation) {
                    String typeNameForLength = extractTypeNameFromMessage(errorMsg);
                    String lengthConstraint = getKnownLengthConstraint(typeNameForLength);
                    if (lengthConstraint != null) {
                        rangeDisplay = lengthConstraint;
                        isLengthViolation = true;
                    }
                }
                // PR_XX: 最后从错误消息 / TypeLib.xsd 解析合法取值（枚举或 pattern）
                if (rangeDisplay == null) {
                    String typeName = extractTypeNameFromMessage(errorMsg);
                    // TR_PR_011: 类型（或其union成员）带数值范围面时，按2014范围违规处理
                    String numericRange = getNumericRangeFromXsdType(typeName);
                    // 基础整数联合类型的数值 facet 位于其 XML Schema 内置基类型上，
                    // 不能仅靠读取命名 simpleType 的 min/max 得到范围。
                    if (numericRange == null || numericRange.isEmpty()) {
                        numericRange = getBuiltinIntegerRange(typeName);
                    }
                    if (numericRange != null && !numericRange.isEmpty()) {
                        rangeDisplay = numericRange;
                        isRangeViolation = true;
                    } else {
                        rangeDisplay = resolveAllowedValuesDisplayFromXsd(typeName,
                                extractEnumValuesFromMessage(errorMsg));
                    }
                }

                String attributeValue = extractAttributeValue(errorMsg);
                // Xerces 对数值 union/simpleType 的非法词法值有时会走到 cvc-attribute.3，
                // 并被上面的枚举/范围推断误判为 2017/2014。先根据实际属性值识别词法类型错误。
                // 例如 004842 的 "not-a-number" 必须返回 2013。
                String typeName = extractTypeNameFromMessage(errorMsg);
                // 枚举/布尔类型的非法值属于 2017；不能因为非法值包含字母，
                // 就被通用词法判断错误地归类为 2013。
                boolean isEnumerationViolation = errorMsg.contains("'booleanType'")
                        || errorMsg.contains("'boolean'")
                        || errorMsg.contains("'null'")
                        || lastEnumValues != null
                        || isLikelyEnumerationType(errorMsg);
                boolean isTypeViolation = !isEnumerationViolation && isLikelyDataTypeErrorValue(attributeValue);
                boolean isNumericTimeRangeViolation = attributeValue != null
                        && attributeValue.matches("^[+-]?\\d+$")
                        && (errorMsg.contains("TimeValueType") || errorMsg.contains("DurationType")
                                || (rangeDisplay != null && rangeDisplay.contains("INFINITE_TIME")));
                if (isTypeViolation) {
                    errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_DATA_TYPE_INVALID;
                    formattedMessage = formatTypeErrorMessage(fileName, lineNumber, elementName,
                            attributeName, errorMsg);
                } else if (isNumericTimeRangeViolation) {
                    errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_VALUE_OUT_OF_RANGE;
                    String numericRange = getNumericRangeFromXsdType(typeName);
                    if (numericRange == null || numericRange.isEmpty()) {
                        numericRange = "1, 9223372036854775807";
                    }
                    formattedMessage = formatValueOutOfRangeMessage(fileName, lineNumber, attributeValue,
                            attributeName, elementName, numericRange);
                } else if (isLengthViolation) {
                    errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_LENGTH_INVALID;
                    formattedMessage = formatLengthInvalidMessage(fileName, lineNumber, attributeValue,
                            attributeName, elementName, rangeDisplay);
                } else if (isRangeViolation) {
                    // TR_PR_011: 数值范围违规 → 2014 "Value 'X' is not allowed ... Valid range: 'min' to 'max'."
                    errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_VALUE_OUT_OF_RANGE;
                    formattedMessage = formatValueOutOfRangeMessage(fileName, lineNumber, attributeValue,
                            attributeName, elementName, rangeDisplay);
                } else {
                    errorCode = CodeConstant.ERROR_XML_ENUM_VALUE_INVALID;
                    formattedMessage = String.format(
                            "[File: %s, Line: %d] Value '%s' is not allowed for attribute '%s' in element '%s'. Valid values: '%s'.",
                            fileName, lineNumber, attributeValue == null ? "" : attributeValue,
                            attributeName, elementName, rangeDisplay);
                }
            }
            // 2013: 元素属性类型错误
            // 只匹配 cvc-attribute.3 和
            // cvc-datatype-valid（cvc-pattern-valid/cvc-minInclusive-valid等已被过滤）
            // PR_XX: 排除范围错误（min/max 等 facet），这些应该报 2014 而不是 2013
            else if (errorMsg != null &&
                    (errorMsg.contains("cvc-attribute.3") || errorMsg.contains("cvc-datatype-valid")
                            || errorMsg.contains("不匹配"))
                    &&
                    !errorMsg.contains("minInclusive") && !errorMsg.contains("maxInclusive") &&
                    !errorMsg.contains("minExclusive") && !errorMsg.contains("maxExclusive") &&
                    !errorMsg.contains("totalDigits") && !errorMsg.contains("fractionDigits")) {
                String elementName = extractElementName(errorMsg);
                String attributeName = extractAttributeName(errorMsg);

                // PR_XX: 检查是否有缓存的长度范围（来自之前的cvc-maxLength-valid等）
                // 如果有，说明这是字符串长度错误，应该报2014而不是2013
                if (lastLengthRange != null && Math.abs(lastLengthLineNumber - lineNumber) <= 1) {
                    errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_VALUE_OUT_OF_RANGE;
                    // TR_PR_009: "Value 'X' is not allowed for attribute 'A' in element 'E'. Valid range: 'min' to 'max'."
                    formattedMessage = formatValueOutOfRangeMessage(fileName, lineNumber,
                            extractAttributeValue(errorMsg), attributeName, elementName, lastLengthRange);
                }
                // 检查是否是空字符串导致的类型错误，如果是则报2011（属性缺失）而不是2013（类型错误）
                // XSD验证器对空字符串 "" 会报类似 "'' is not valid" 的错误
                // 特殊处理：CRC32属性的验证逻辑
                // - Module.xml中CRC32是required（必填），缺失应报2011错误
                // - 其他文件（CoreOS.xml等）中CRC32是optional（可选），缺失不报错，空值由CRCAttributeValidator报4005警告
                else if (attributeName.equals("CRC32")) {
                    // PR_XX: 对 CRC32 进行额外语义区分：
                    // - 若值形如 0x[0-9A-F]+ 且超过 32bit（>8位十六进制），应报告 2014（超范围）
                    // - 其他不匹配 pattern 的情况（例如 test/0.1/缺少0x等），仍报告 2013（类型错误）
                    String attributeValue = extractAttributeValue(errorMsg);

                    // 判断是否是Module文件
                    boolean isModuleFile = fileName != null && fileName.toLowerCase().contains("module");

                    if (isModuleFile) {
                        // Module文件中CRC32是必填的，缺失应报2011错误
                        if (errorMsg.contains("''") || errorMsg.contains("\"\"") ||
                                errorMsg.contains("is not a valid value") && errorMsg.contains("''")) {
                            errorCode = CodeConstant.ERROR_XML_ELEMENT_ATTRIBUTE_MISSING;
                            // TR_PR_007: "Required attribute 'X' is missing in element 'Y'."
                            formattedMessage = String.format(
                                    "[File: %s, Line: %d] Required attribute '%s' is missing in element '%s'.",
                                    fileName, lineNumber, attributeName, elementName);
                        } else {
                            if (attributeValue != null && attributeValue.matches("0[xX][0-9a-fA-F]{9,}")) {
                                errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_VALUE_OUT_OF_RANGE;
                                String crcRange = getCrc32RangeDisplayFromXsd();
                                formattedMessage = formatValueOutOfRangeMessage(fileName, lineNumber,
                                        attributeValue, attributeName, elementName, crcRange);
                            } else {
                                // CRC32Type 在 XSD 中是 pattern 约束的字符串（例如 0xXXXXXXXX）。
                                // 对于 1 / 0.1 / test / 0x1 等不匹配 pattern 的值，应报告 2013（类型错误）。
                                errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_DATA_TYPE_INVALID;
                                formattedMessage = formatTypeErrorMessage(fileName, lineNumber, elementName,
                                        attributeName, errorMsg);
                            }
                        }
                    } else {
                        if (attributeValue != null && attributeValue.matches("0[xX][0-9a-fA-F]{9,}")) {
                            errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_VALUE_OUT_OF_RANGE;
                            String crcRange = getCrc32RangeDisplayFromXsd();
                            formattedMessage = formatValueOutOfRangeMessage(fileName, lineNumber,
                                    attributeValue, attributeName, elementName, crcRange);
                        } else {
                            // 非Module文件中CRC32如果出现类型/格式错误，仍报告2013错误
                            errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_DATA_TYPE_INVALID;
                            formattedMessage = formatTypeErrorMessage(fileName, lineNumber, elementName,
                                    attributeName, errorMsg);
                        }
                    }
                } else if (errorMsg.contains("''") || errorMsg.contains("\"\"") ||
                        errorMsg.contains("is not a valid value") && errorMsg.contains("''")) {
                    errorCode = CodeConstant.ERROR_XML_ELEMENT_ATTRIBUTE_MISSING;
                    // TR_PR_007: "Required attribute 'X' is missing in element 'Y'."
                    formattedMessage = String.format(
                            "[File: %s, Line: %d] Required attribute '%s' is missing in element '%s'.",
                            fileName, lineNumber, attributeName, elementName);
                } else {
                    errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_DATA_TYPE_INVALID;
                    formattedMessage = formatTypeErrorMessage(fileName, lineNumber, elementName, attributeName,
                            errorMsg);
                }
            }
            // 2008: 元素属性未定义
            // 主要匹配：
            // 1. cvc-complex-type.3.2.2: 属性不允许出现在元素中（最常见）
            // 2. 其他"属性不允许"的表述
            else if (errorMsg != null && (
            // cvc-complex-type.3.2.2: 属性不允许出现
            errorMsg.contains("cvc-complex-type.3.2.2") ||
            // 通用匹配：Attribute + not allowed
                    (errorMsg.contains("Attribute") &&
                            (errorMsg.contains("not allowed") ||
                                    errorMsg.contains("is not allowed") ||
                                    errorMsg.contains("undefined attribute"))))) {
                errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_UNDEFINED;
                String attributeName = extractAttributeName(errorMsg);
                formattedMessage = String.format("[File: %s, Line: %d] undefined XML element attribute %s.",
                        fileName, lineNumber, attributeName);
            }
            // A-08/006341: 字符串属性超出取值范围（长度限制）
            // 处理 cvc-maxLength-valid, cvc-minLength-valid, cvc-length-valid 错误
            else if (errorMsg != null && (errorMsg.contains("cvc-maxLength-valid") ||
                    errorMsg.contains("cvc-minLength-valid") ||
                    errorMsg.contains("cvc-length-valid") ||
                    errorMsg.contains("cvc-type.3.1.3") ||
                    (errorMsg.contains("length") && (errorMsg.contains("less than") || errorMsg.contains("greater than")
                            || errorMsg.contains("exceeds"))))) {
                // 006360: 字符串长度违规应报告2016，而不是数值范围错误2014
                errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_LENGTH_INVALID;
                String elementName = extractElementName(errorMsg);
                String attributeName = extractAttributeName(errorMsg);

                // PR_05/PR_28: 使用缓存的元素名和属性名（允许行号相差1以内）
                if ((elementName.isEmpty() || "unknown element".equals(elementName) || "unknown".equals(elementName))
                        && lastAttributeElementName != null && Math.abs(lastAttributeLineNumber - lineNumber) <= 1) {
                    elementName = lastAttributeElementName;
                }
                if ((attributeName.isEmpty() || "unknown attribute".equals(attributeName)
                        || "unknown".equals(attributeName))
                        && lastAttributeName != null && Math.abs(lastAttributeLineNumber - lineNumber) <= 1) {
                    attributeName = lastAttributeName;
                }

                // 提取长度范围
                String lengthRange = extractLengthRange(errorMsg);
                formattedMessage = formatLengthInvalidMessage(fileName, lineNumber,
                        extractAttributeValue(errorMsg),
                        attributeName.isEmpty() || "unknown attribute".equals(attributeName)
                                || "unknown".equals(attributeName) ? "unknown attribute" : attributeName,
                        elementName.isEmpty() || "unknown element".equals(elementName)
                                || "unknown".equals(elementName) ? "unknown element" : elementName,
                        lengthRange);
            }
            // PR_06: 2014: 数值属性超出取值范围（十六进制/整数范围）
            // 处理 cvc-maxInclusive-valid, cvc-minInclusive-valid, cvc-totalDigits-valid 错误
            // PR_XX: 也处理 cvc-attribute.3 + TimeValueType（范围约束类型）的情况
            else if (errorMsg != null && (errorMsg.contains("cvc-maxInclusive-valid") ||
                    errorMsg.contains("cvc-minInclusive-valid") ||
                    errorMsg.contains("cvc-totalDigits-valid") ||
                    errorMsg.contains("cvc-fractionDigits-valid") ||
                    errorMsg.contains("cvc-minExclusive-valid") ||
                    errorMsg.contains("cvc-maxExclusive-valid") ||
                    // PR_XX: cvc-attribute.3 + TimeValueType 表示范围错误
                    (errorMsg.contains("cvc-attribute.3") && errorMsg.contains("TimeValueType")) ||
                    (errorMsg.contains("is not facet-valid") &&
                            (errorMsg.contains("maxInclusive") || errorMsg.contains("minInclusive") ||
                                    errorMsg.contains("totalDigits") || errorMsg.contains("fractionDigits"))))) {
                String elementName = extractElementName(errorMsg);
                String attributeName = extractAttributeName(errorMsg);

                // PR_FIX: 对于像 0.1 / test 这种“词法/数据类型不合法”的值，尽管错误消息里会带 minInclusive/maxInclusive，
                // 但从错误码语义上应报告 2013（类型错误），而不是 2014（数值越界）。
                String offendingValue = extractValue(errorMsg);

                // PR_06/PR_28: 使用缓存的元素名和属性名
                // PR_XX: 优先使用同一行的缓存，如果没有则使用最近的缓存（行号相差2以内）
                // 因为XSD验证器可能在同一行报告多个错误，或者错误顺序不确定
                if ((elementName.isEmpty() || "unknown element".equals(elementName) || "unknown".equals(elementName))
                        && lastAttributeElementName != null && Math.abs(lastAttributeLineNumber - lineNumber) <= 2) {
                    elementName = lastAttributeElementName;
                }
                if ((attributeName.isEmpty() || "unknown attribute".equals(attributeName)
                        || "unknown".equals(attributeName))
                        && lastAttributeName != null && Math.abs(lastAttributeLineNumber - lineNumber) <= 2) {
                    attributeName = lastAttributeName;
                }

                if (isLikelyDataTypeErrorValue(offendingValue)) {
                    errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_DATA_TYPE_INVALID;
                    // TR_PR_008: 附加 Expected type 信息
                    formattedMessage = formatTypeErrorMessage(
                            fileName,
                            lineNumber,
                            elementName.isEmpty() || "unknown element".equals(elementName)
                                    || "unknown".equals(elementName) ? "unknown element" : elementName,
                            attributeName.isEmpty() || "unknown attribute".equals(attributeName)
                                    || "unknown".equals(attributeName) ? "unknown attribute" : attributeName,
                            errorMsg);
                } else {
                    errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_VALUE_OUT_OF_RANGE;

                    // 提取数值范围
                    String numericRange = extractNumericRange(errorMsg);
                    // TR_PR_011: "Value 'X' is not allowed for attribute 'A' in element 'E'. Valid range: 'min' to 'max'."
                    formattedMessage = formatValueOutOfRangeMessage(fileName, lineNumber, offendingValue,
                            attributeName.isEmpty() || "unknown attribute".equals(attributeName)
                                    || "unknown".equals(attributeName) ? "unknown attribute" : attributeName,
                            elementName.isEmpty() || "unknown element".equals(elementName)
                                    || "unknown".equals(elementName) ? "unknown element" : elementName,
                            numericRange);
                }
            }
            // 2009: 元素数量小于最小出现次数（包括元素内容不完整/缺少必需子元素）
            // 注意：排除"Invalid content was found starting with element"的情况（那是2007错误）
            else if (errorMsg != null &&
                    !errorMsg.contains("Invalid content was found starting with element") &&
                    (errorMsg.contains("Expected elements") ||
                            errorMsg.contains("is expected") || // XSD: "One of {...} is expected"
                            errorMsg.contains("is not complete") || // XSD: "content ... is not complete"
                            errorMsg.contains("cvc-complex-type.2.4") || // XSD: 元素内容模型错误
                            errorMsg.contains("less than") ||
                            errorMsg.contains("minOccurs") ||
                            errorMsg.contains("必须后跟"))) { // 中文：元素必须后跟子元素

                // PR_37: 如果同一行已经报告过同名子元素错误（2007），跳过此2009错误
                if (hasSameNameChildElementReported(lineNumber)) {
                    return; // 跳过，不报告此错误
                }

                errorCode = CodeConstant.ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET;
                String elementName = extractElementName(errorMsg);
                String expectedElement = extractExpectedElement(errorMsg);
                String min = extractMinValue(errorMsg);

                // 问题报告单5: "One of '{...}' 候选列表中逐个检验必选单例元素，
                // 不只检验第一个候选，确保XSD表中所有必选单例元素缺失时都能报2015。
                if (elementName != null && !"unknown element".equals(elementName)) {
                    java.util.List<String> expectedCandidates = extractExpectedElements(errorMsg);
                    String parentInstance = lineParentInstanceMapping.get(lineNumber);
                    String parentName = parentNameFromInstanceKey(parentInstance);
                    String missingCandidate = findMissingRequiredChild(parentName, parentInstance,
                            expectedCandidates);
                    if (missingCandidate != null) {
                        expectedElement = missingCandidate;
                    } else if (parentName == null) {
                        // XSD错误可能定位在父元素结束标签，不能直接使用候选列表第一个元素。
                        // 仅当当前文件中该父元素实例唯一且实际数量确实低于minOccurs时才选中候选。
                        String inferredMissingCandidate = findMissingRequiredChildForUniqueParent(
                                elementName, expectedCandidates);
                        if (inferredMissingCandidate != null) {
                            expectedElement = inferredMissingCandidate;
                        } else if (!expectedCandidates.isEmpty()) {
                            // 独立数量审计会在校验结束时按父节点实例补报；此处不再产生
                            // 无法确认目标的2009/2015，避免把候选列表首项误报为缺失元素。
                            return;
                        }
                    }
                }

                // PR_13: 按照需求格式输出错误消息
                // 如果能提取到期望的子元素名，用它作为元素名；否则使用父元素名
                String targetElement = (!expectedElement.isEmpty() && !"unknown".equals(expectedElement))
                        ? expectedElement
                        : elementName;
                String parentInstance = lineParentInstanceMapping.get(lineNumber);
                String parentName = parentNameFromInstanceKey(parentInstance);
                XsdElementDecl targetDeclaration = findDirectChildDeclaration(parentName, targetElement);
                String minValue = targetDeclaration != null ? targetDeclaration.minOccurs
                        : ((!min.isEmpty()) ? min : "1");
                Integer directCount = directChildCount(parentInstance, targetElement);
                int targetCount = directCount != null
                        ? directCount
                        : actualOccursCount(systemId, targetElement);

                // TR_PR_010: 必要元素(minOccurs>=1)完全缺失。
                // 该场景同时满足 2009（实际出现次数低于 minOccurs）和
                // 2015（必选元素缺失），两个错误都必须保留。
                if (!expectedElement.isEmpty() && !"unknown".equals(expectedElement)
                        && elementName != null && !"unknown element".equals(elementName)
                        && isRequiredChildInXsd(expectedElement,
                                parentName != null ? parentName : elementName)) {
                    String occursMessage = formatOccursLessThanMessage(fileName, lineNumber, expectedElement,
                            targetCount, minValue);
                    recordFormattedError(level, CodeConstant.ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET, occursMessage);
                    errorCode = CodeConstant.ERROR_XML_REQUIRED_ELEMENT_MISSING;
                    formattedMessage = String.format(
                            "[File: %s, Line: %d] Required element '%s' is missing in parent element '%s'.",
                            fileName, lineNumber, expectedElement,
                            parentName != null ? parentName : elementName);
                    markMissingRequiredElementReported(parentInstance,
                            parentName != null ? parentName : elementName, expectedElement);
                } else {
                    // TR_PR_005: "element X occurs N time(s) less than minimum M time(s)."
                    formattedMessage = formatOccursLessThanMessage(fileName, lineNumber, targetElement,
                            targetCount, minValue);
                }
            }
            // 2010: 元素数量大于最大出现次数
            // 主要匹配：
            // 1. cvc-complex-type.2.4.d: 不应该有更多子元素
            // 2. cvc-complex-type.2.4.a: 当元素重复出现超过maxOccurs时，会报告"Invalid content was found
            // starting with element"
            // 3. 明确包含 maxOccurs 或 "more than" 的错误
            // PR_14: 添加对 "Invalid content was found starting with element" + "is expected"
            // 的匹配
            // 这种情况表示元素出现次数超过了maxOccurs
            // PR_12: 区分元素未定义(2007)和元素数量超过最大值(2010)
            // 当实际元素名称与期望元素名称不同时，报告2007错误
            else if (errorMsg != null && (
            // cvc-complex-type.2.4.d: 不期望有子元素（超过最大值）
            (errorMsg.contains("cvc-complex-type.2.4.d") ||
                    errorMsg.contains("No child element is expected")) ||
            // PR_14: cvc-complex-type.2.4.a: 元素重复出现超过maxOccurs
                    (errorMsg.contains("Invalid content was found starting with element") &&
                            errorMsg.contains("is expected"))
                    ||
                    // 明确的 maxOccurs 相关错误
                    errorMsg.contains("more than") ||
                    errorMsg.contains("maxOccurs") ||
                    errorMsg.contains("greater than") ||
                    errorMsg.contains("exceeds maximum"))) {

                // PR_37/PR_14: 特殊处理：当错误消息同时包含 "Invalid content was found starting with element"
                // 和 "No
                // child element is expected" 时
                // 需要区分两种情况：
                // 1. 同名子元素：如<Module><Module></Module></Module> → 2007
                // 2. 数量超限：如<Applications><Application>...<Application>...</Applications> → 2010
                // PR_14: "No child element is expected" 通常表示元素不应该有子元素，可能是同名子元素或数量超限
                // 由于无法从错误消息中提取期望元素，保守处理：如果是顶层元素，才判定为同名子元素
                if (errorMsg.contains("Invalid content was found starting with element") &&
                        errorMsg.contains("No child element is expected")) {
                    String actualElement = normalizeElementName(extractInvalidContentElement(errorMsg));

                    // "No child element is expected"：先排除实际元素在 XSD 中完全未定义的情况。
                    // 未定义元素必须报告2007；只有已定义元素才可能继续按同名嵌套或数量超限判断。
                    boolean exceedsMaxOccurs = exceedsDeclaredMaxOccurs(systemId, actualElement);
                    boolean isSameNameChild = !exceedsMaxOccurs && hasDeclaredChildrenInXsd(actualElement);
                    boolean isUndefinedElement = !isElementDefinedInXsd(actualElement);

                    if (exceedsMaxOccurs) {
                        errorCode = CodeConstant.ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED;
                        String max = resolveMaxOccursForMessage(actualElement, null, errorMsg);
                        formattedMessage = formatOccursGreaterThanMessage(fileName, lineNumber, actualElement,
                                actualOccursCount(systemId, actualElement), max);
                    } else if (isSameNameChild || isUndefinedElement) {
                        errorCode = CodeConstant.ERROR_XML_UNDEFINED_ELEMENT;
                        formattedMessage = String.format("[File: %s, Line: %d] undefined XML element %s.",
                                fileName, lineNumber, actualElement);
                        markSameNameChildElementReported(lineNumber, actualElement);
                    } else {
                        // 正常的2010错误处理（包括Application, Partition等可能重复的元素）
                        errorCode = CodeConstant.ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED;
                        String repeatElement = actualElement.isEmpty()
                                ? normalizeElementName(extractElementName(errorMsg))
                                : actualElement;
                        String max = resolveMaxOccursForMessage(repeatElement, null, errorMsg);
                        // TR_PR_006: "element X occurs N time(s) greater than M time(s)."
                        formattedMessage = formatOccursGreaterThanMessage(fileName, lineNumber, repeatElement,
                                actualOccursCount(systemId, repeatElement), max);
                    }
                }
                // PR_12: 检查是否是元素未定义的情况
                // 当错误消息包含 "Invalid content was found starting with element" + "is expected" 时
                // 需要比较实际元素名称和期望元素名称
                // PR_37/PR_14: 优先检查是否是同名子元素
                else if (errorMsg.contains("Invalid content was found starting with element") &&
                        errorMsg.contains("is expected")) {
                    String actualElement = normalizeElementName(extractInvalidContentElement(errorMsg));

                    // PR_14: 提取期望元素列表，用于更精确的判断
                    java.util.List<String> expectedElements = extractExpectedElements(errorMsg);

                    // PR_14: 改进的同名子元素判断逻辑
                    // 检查期望元素是否是实际元素的内部子元素
                    boolean isSameNameChild = false;
                    if (isTopLevelElement(actualElement) && !expectedElements.isEmpty()) {
                        isSameNameChild = isSameNameChildElement(actualElement, expectedElements);
                    }

                    if (isSameNameChild) {
                        // 确认是同名子元素错误
                        errorCode = CodeConstant.ERROR_XML_UNDEFINED_ELEMENT;
                        formattedMessage = String.format("[File: %s, Line: %d] undefined XML element %s.",
                                fileName, lineNumber, actualElement);
                        markSameNameChildElementReported(lineNumber, actualElement);
                    } else {
                        // 继续现有的逻辑（提取expectedElement，检查sequence等）
                        // 说明："One of '{...}' is expected" 可能同时包含多个候选元素。
                        // 仅取第一个候选会导致误判：例如缺少必选 SharedLibraryPayload 时，候选列表中也可能包含可选
                        // 的 SharedDataPayload，若只取第一个就无法识别 minOccurs 未满足。
                        // PR_14: expectedElements 已在上面提取，不需要重复提取
                        String expectedElement = expectedElements.isEmpty()
                                ? normalizeElementName(extractExpectedElement(errorMsg))
                                : expectedElements.get(0);

                        // PR_XX: 根据 XSD sequence 关系判断：actual 在 expected 之前且 maxOccurs 有限 → 2010
                        boolean isSequenceRepetition = false;
                        for (String candidate : expectedElements.isEmpty()
                                ? java.util.Collections.singletonList(expectedElement)
                                : expectedElements) {
                            if (isElementRepetitionInXsdSequence(actualElement, candidate,
                                    exceedsDeclaredMaxOccurs(systemId, actualElement))) {
                                isSequenceRepetition = true;
                                expectedElement = candidate;
                                break;
                            }
                        }

                        // PR_XX: 根据 XSD sequence 关系判断：expected 在 actual 之前且 minOccurs>0 → 2009
                        boolean isMissingRequiredElement = false;
                        for (String candidate : expectedElements.isEmpty()
                                ? java.util.Collections.singletonList(expectedElement)
                                : expectedElements) {
                            if (isMissingRequiredElementInXsdSequence(actualElement, candidate)) {
                                isMissingRequiredElement = true;
                                expectedElement = candidate;
                                break;
                            }
                        }

                        if (!actualElement.isEmpty() && !expectedElement.isEmpty() &&
                                !actualElement.equals(expectedElement) && !"unknown".equals(actualElement)) {

                            if (isSequenceRepetition) {
                                // 元素在sequence中重复出现，超过maxOccurs → 2010
                                errorCode = CodeConstant.ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED;
                                String max = resolveMaxOccursForMessage(actualElement, expectedElement, errorMsg);
                                // TR_PR_006: "element X occurs N time(s) greater than M time(s)."
                                formattedMessage = formatOccursGreaterThanMessage(fileName, lineNumber,
                                        actualElement, actualOccursCount(systemId, actualElement), max);
                            } else if (isMissingRequiredElement) {
                                // 问题报告单5/TR_PR_010: 缺少的必要元素(minOccurs>=1)。
                                // 完全缺失同时报告 2009 和 2015。
                                String errorParentInstance = lineParentInstanceMapping.get(lineNumber);
                                String errorParentName = parentNameFromInstanceKey(errorParentInstance);
                                String missingCandidate = findMissingRequiredChild(errorParentName,
                                        errorParentInstance, expectedElements);
                                if (missingCandidate != null) {
                                    // XSD 候选列表是“当前位置可出现的元素”，列表第一个
                                    // 不一定是缺失元素；按当前父元素实例的实际数量重新选择。
                                    expectedElement = missingCandidate;
                                }
                                String missingParent = findParentOfChildInXsdSequence(expectedElement,
                                        actualElement);
                                if (errorParentName != null) {
                                    missingParent = errorParentName;
                                }
                                if (missingParent != null
                                        && isRequiredChildInXsd(expectedElement, missingParent)) {
                                    String min = resolveMinOccursForMessage(expectedElement, actualElement, errorMsg);
                                    Integer directCount = directChildCount(errorParentInstance, expectedElement);
                                    String occursMessage = formatOccursLessThanMessage(fileName, lineNumber,
                                            expectedElement,
                                            directCount != null ? directCount : actualOccursCount(systemId, expectedElement),
                                            min);
                                    recordFormattedError(level, CodeConstant.ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET,
                                            occursMessage);
                                    errorCode = CodeConstant.ERROR_XML_REQUIRED_ELEMENT_MISSING;
                                    formattedMessage = String.format(
                                            "[File: %s, Line: %d] Required element '%s' is missing in parent element '%s'.",
                                            fileName, lineNumber, expectedElement, missingParent);
                                    markMissingRequiredElementReported(errorParentInstance, missingParent,
                                            expectedElement);
                                } else {
                                    // 缺少必需元素（minOccurs未满足） → 2009
                                    errorCode = CodeConstant.ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET;
                                    String min = resolveMinOccursForMessage(expectedElement, actualElement, errorMsg);
                                    // TR_PR_005: "element X occurs N time(s) less than minimum M time(s)."
                                    formattedMessage = formatOccursLessThanMessage(fileName, lineNumber,
                                            expectedElement, actualOccursCount(systemId, expectedElement), min);
                                    if (isRequiredChildInXsd(expectedElement, errorParentName)) {
                                        markMissingRequiredElementReported(errorParentInstance, errorParentName,
                                                expectedElement);
                                    }
                                }
                            } else {
                                // 只有当 actual 出现在当前错误报文给出的候选子元素中时，
                                // 才按当前 sequence 中的数量超限处理；不能使用全局元素索引，
                                // 否则“在其他容器中定义、在当前容器中未定义”的元素会误报2010。
                                boolean isValidChild = expectedElements.contains(actualElement);

                                if (isValidChild) {
                                    // 元素是容器的合法子元素，但超过maxOccurs → 2010
                                    errorCode = CodeConstant.ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED;
                                    String max = resolveMaxOccursForMessage(actualElement, expectedElement, errorMsg);
                                    formattedMessage = formatOccursGreaterThanMessage(fileName, lineNumber,
                                            actualElement, actualOccursCount(systemId, actualElement), max);
                                } else {
                                    // 元素未定义或位置错误 → 2007
                                    errorCode = CodeConstant.ERROR_XML_UNDEFINED_ELEMENT;
                                    formattedMessage = String.format("[File: %s, Line: %d] undefined XML element %s.",
                                            fileName, lineNumber, actualElement);
                                    // 例如仅删除HardwareConfiguration标签而保留PhysicalMemory：
                                    // PhysicalMemory会越级到CoreOSDescription下，2007正确；
                                    // 同时HardwareConfiguration作为必选元素也必须报告2015。
                                    misplacedElementForMissingCheck = actualElement;
                                    expectedElementsForMissingCheck = expectedElements;
                                }
                            }
                        } else {
                            // 实际元素名称与期望元素名称相同，是元素数量超过最大值错误(2010)
                            errorCode = CodeConstant.ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED;
                            String elementName = normalizeElementName(extractElementName(errorMsg));
                            String max = resolveMaxOccursForMessage(elementName, null, errorMsg);
                            formattedMessage = formatOccursGreaterThanMessage(fileName, lineNumber, elementName,
                                    actualOccursCount(systemId, elementName), max);
                        }
                    }
                } else {
                    // 其他情况，直接报告2010错误
                    errorCode = CodeConstant.ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED;
                    String elementName = normalizeElementName(extractElementName(errorMsg));
                    String max = resolveMaxOccursForMessage(elementName, null, errorMsg);
                    formattedMessage = formatOccursGreaterThanMessage(fileName, lineNumber, elementName,
                            actualOccursCount(systemId, elementName), max);
                }
            }
            // 2011: 元素属性缺失
            // PR_37: 如果同一行已经报告过同名子元素错误（2007），跳过此2011错误
            else if (errorMsg != null && ((errorMsg.contains("Attribute") &&
                    (errorMsg.contains("must appear") ||
                            errorMsg.contains("required") ||
                            errorMsg.contains("missing")))
                    ||
                    errorMsg.contains("cvc-complex-type.4") || // XSD: 必需属性缺失
                    errorMsg.contains("必须包含属性"))) { // 中文：必须包含属性

                // PR_37: 如果同一行已经报告过同名子元素错误（2007），或者该元素已经报告过2007错误，跳过此2011错误
                // 因为同名子元素本身就是错误的，它缺少属性是正常的
                if (hasSameNameChildElementReported(lineNumber)) {
                    return; // 跳过，不报告此错误
                }

                errorCode = CodeConstant.ERROR_XML_ELEMENT_ATTRIBUTE_MISSING;
                String elementName = extractElementName(errorMsg);
                String attributeName = extractAttributeName(errorMsg);

                // PR_37: 再次检查元素名，如果该元素已经报告过2007错误，也跳过
                if (hasSameNameChildElementReported(elementName)) {
                    return; // 跳过，不报告此错误
                }

                // PR_12: 改进判断逻辑（原PR_37）
                // 如果该元素是顶层元素（如Application, Partition, PartitionWindow等），
                // 先将错误存到pending列表，等验证结束后再判断是否需要报告
                // 这样可以区分真正的属性缺失和同名子元素导致的假性属性缺失
                if (isTopLevelElement(elementName)) {
                    // 存储到pending列表，等待验证结束后处理
                    pendingAttributeMissingErrors.add(new PendingAttributeMissingError(
                            fileName, lineNumber, elementName, attributeName));
                    return;
                }

                // TR_PR_007: "Required attribute 'X' is missing in element 'Y'."
                formattedMessage = String.format(
                        "[File: %s, Line: %d] Required attribute '%s' is missing in element '%s'.",
                        fileName, lineNumber, attributeName, elementName);
            }
            // 2012: 元素属性冗余（重复的属性）
            // 检测同一个属性在一个元素中出现多次的情况
            else if (errorMsg != null && (errorMsg.contains("already specified") ||
                    errorMsg.contains("Duplicate attribute") ||
                    errorMsg.contains("was already specified") ||
                    errorMsg.contains("重复") ||
                    (errorMsg.contains("Attribute") && errorMsg.contains("duplicate")))) {
                errorCode = CodeConstant.ERROR_XML_REDUNDANCY_ATTRIBUTES;
                String elementName = extractElementName(errorMsg);
                String attributeName = extractAttributeName(errorMsg);
                formattedMessage = String.format("[File: %s, Line: %d] XML element %s attribute %s redundancy.",
                        fileName, lineNumber, elementName, attributeName);
            }
            // 2017: 超出XSD约束的数据范围
            // 主要匹配：cvc-facet-valid (facet约束验证失败)
            // PR_29: 添加对NumberFormatException相关错误的处理（超大数值导致的解析失败）
            else if (errorMsg != null && (errorMsg.contains("cvc-facet-valid") ||
                    errorMsg.contains("out of range") ||
                    errorMsg.contains("minInclusive") ||
                    errorMsg.contains("maxInclusive") ||
                    errorMsg.contains("minExclusive") ||
                    errorMsg.contains("maxExclusive") ||
                    errorMsg.contains("For input string") ||
                    errorMsg.contains("NumberFormatException"))) {
                String elementName = extractElementName(errorMsg);
                String attributeName = extractAttributeName(errorMsg);
                String offendingValue = extractValue(errorMsg);

                // PR_28: 使用缓存的元素名和属性名（允许行号相差1以内）
                if ((elementName.isEmpty() || "unknown element".equals(elementName) || "unknown".equals(elementName))
                        && lastAttributeElementName != null && Math.abs(lastAttributeLineNumber - lineNumber) <= 1) {
                    elementName = lastAttributeElementName;
                }
                if ((attributeName.isEmpty() || "unknown attribute".equals(attributeName)
                        || "unknown".equals(attributeName))
                        && lastAttributeName != null && Math.abs(lastAttributeLineNumber - lineNumber) <= 1) {
                    attributeName = lastAttributeName;
                }

                if (isLikelyDataTypeErrorValue(offendingValue)) {
                    errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_DATA_TYPE_INVALID;
                    // TR_PR_008: 附加 Expected type 信息
                    formattedMessage = formatTypeErrorMessage(fileName, lineNumber,
                            elementName.isEmpty() || "unknown element".equals(elementName) ? "unknown element"
                                    : elementName,
                            attributeName.isEmpty() || "unknown attribute".equals(attributeName) ? "unknown attribute"
                                    : attributeName,
                            errorMsg);
                } else {
                    errorCode = CodeConstant.ERROR_XML_ATTRIBUTE_VALUE_OUT_OF_RANGE;
                    String[] range = extractRange(errorMsg);

                    // PR_14: 确保显示元素名和属性名
                    // TR_PR_011: "Value 'X' is not allowed for attribute 'A' in element 'E'. Valid range: 'min' to 'max'."
                    formattedMessage = formatValueOutOfRangeMessage(fileName, lineNumber, offendingValue,
                            attributeName.isEmpty() || "unknown attribute".equals(attributeName) ? "unknown attribute"
                                    : attributeName,
                            elementName.isEmpty() || "unknown element".equals(elementName) ? "unknown element"
                                    : elementName,
                            range[0] + ", " + range[1]);
                }
            }


            // 如果能识别错误类型，使用特定错误码和格式化消息
            if (errorCode != null && formattedMessage != null) {
                // 将消息添加到列表中（不带中文前缀）
                if (!processedFormattedMessages.contains(formattedMessage)) {
                    processedFormattedMessages.add(formattedMessage);
                    externalMessagesList.add(formattedMessage);
                    if ("Error".equals(level) || "Fatal Error".equals(level)) {
                        Log.error(errorCode, formattedMessage);
                    }
                }
            } else {
                // 使用默认格式
                Integer defaultCode = "Warning".equals(level) ? CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING
                        : CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED;

                // 格式化消息，不包含level和中文前缀
                String message = String.format("[File: %s, Line: %d] %s",
                        fileName,
                        e.getLineNumber(),
                        e.getMessage());

                if (!processedFormattedMessages.contains(message)) {
                    processedFormattedMessages.add(message);
                    externalMessagesList.add(message);

                    // 根据不同级别记录日志
                    switch (level) {
                        case "Warning":
                            Log.warning(defaultCode, message);
                            break;
                        case "Error":
                        case "Fatal Error":
                            Log.error(defaultCode, message);
                            break;
                    }
                }
            }

            // 2007已记录后，再补报同一XSD错误中明确缺失的必选元素。
            // 仅当原始XML父节点映射和实际子元素计数均可确认时执行，避免把纯顺序错误误报为2015。
            if (misplacedElementForMissingCheck != null && expectedElementsForMissingCheck != null) {
                recordMissingRequiredElementForMisplacedChild(level, fileName, lineNumber,
                        misplacedElementForMissingCheck, expectedElementsForMissingCheck, errorMsg, systemId);
            }
        }

        /**
         * isSameAsCurrentMappingFile
         *
         * @brief
         * 功能: 执行 isSameAsCurrentMappingFile 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private boolean isSameAsCurrentMappingFile(String systemId) {
            if (systemId == null || currentMappingFile == null) {
                return false;
            }
            try {
                String currentUri = currentMappingFile.toURI().toString();
                return systemId.equalsIgnoreCase(currentUri);
            } catch (Exception ignored) {
                return false;
            }
        }

        // 辅助方法：从System ID或Public ID中提取文件名
        /**
         * extractFileName
         *
         * @brief
         * 功能: 执行 extractFileName 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractFileName(String systemId, String publicId) {
            String id = systemId != null ? systemId : publicId;
            if (id == null)
                return "Unknown";
            int lastSlash = Math.max(id.lastIndexOf('/'), id.lastIndexOf('\\'));
            return lastSlash >= 0 ? id.substring(lastSlash + 1) : id;
        }

        // 辅助方法：从错误消息中提取元素名
        /**
         * extractElementName
         *
         * @brief
         * 功能: 执行 extractElementName 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractElementName(String message) {
            if (message == null)
                return "unknown";

            // 尝试多种模式提取元素名（支持英文和中文）

            // PR_04: 格式0: cvc-attribute.3 格式 "on element 'XXX'"
            // 例如: "The value 'xxx' of attribute 'attrName' on element 'elemName' is not
            // valid..."
            if (message.contains("on element '")) {
                int start = message.indexOf("on element '") + 12;
                int end = message.indexOf("'", start);
                if (end > start)
                    return message.substring(start, end);
            }

            // 格式1: "在元素 'XXX' 中"
            if (message.contains("在元素 '")) {
                int start = message.indexOf("在元素 '") + 5;
                int end = message.indexOf("'", start);
                if (end > start)
                    return message.substring(start, end);
            }

            // 格式2: "以元素 '{namespace:XXX}' 开头" - 提取命名空间后的元素名
            if (message.contains("以元素 '")) {
                int start = message.indexOf("以元素 '") + 5;
                int end = message.indexOf("'", start);
                if (end > start) {
                    String fullName = message.substring(start, end);
                    // 提取 {namespace:ElementName} 中的 ElementName
                    if (fullName.contains(":")) {
                        int colonIndex = fullName.indexOf(":");
                        int braceIndex = fullName.indexOf("}");
                        if (colonIndex > 0 && braceIndex > colonIndex) {
                            return fullName.substring(colonIndex + 1, braceIndex);
                        }
                    }
                    return fullName;
                }
            }

            // 格式3: "element 'XXX'"
            if (message.contains("element '")) {
                int start = message.indexOf("element '") + 9;
                int end = message.indexOf("'", start);
                if (end > start)
                    return message.substring(start, end);
            }

            // 格式4: "element \"XXX\""
            if (message.contains("element \"")) {
                int start = message.indexOf("element \"") + 9;
                int end = message.indexOf("\"", start);
                if (end > start)
                    return message.substring(start, end);
            }

            return "unknown element";
        }

        // 辅助方法：从错误消息中提取属性名
        /**
         * extractAttributeName
         *
         * @brief
         * 功能: 执行 extractAttributeName 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractAttributeName(String message) {
            if (message == null)
                return "unknown";

            // 尝试多种模式提取属性名（支持英文和中文，支持单引号和双引号）

            // PR_04: cvc-attribute.3 格式: "of attribute 'XXX' on element"
            // 例如: "The value 'xxx' of attribute 'attrName' on element 'elemName' is not
            // valid..."
            if (message.contains("of attribute '")) {
                int start = message.indexOf("of attribute '") + 14;
                int end = message.indexOf("'", start);
                if (end > start)
                    return message.substring(start, end);
            }

            // 中文格式: "属性 'XXX' 的值"
            if (message.contains("属性 '")) {
                int start = message.indexOf("属性 '") + 4;
                int end = message.indexOf("'", start);
                if (end > start)
                    return message.substring(start, end);
            }

            // 英文格式（大写）: Attribute "XXX" (双引号)
            if (message.contains("Attribute \"")) {
                int start = message.indexOf("Attribute \"") + 11;
                int end = message.indexOf("\"", start);
                if (end > start)
                    return message.substring(start, end);
            }

            // 英文格式（小写）: attribute "XXX" (双引号)
            if (message.contains("attribute \"")) {
                int start = message.indexOf("attribute \"") + 11;
                int end = message.indexOf("\"", start);
                if (end > start)
                    return message.substring(start, end);
            }

            // 英文格式（小写）: "attribute 'XXX'" (单引号)
            if (message.contains("attribute '")) {
                int start = message.indexOf("attribute '") + 11;
                int end = message.indexOf("'", start);
                if (end > start)
                    return message.substring(start, end);
            }

            // 英文格式（大写）: "Attribute 'XXX'" (单引号)
            if (message.contains("Attribute '")) {
                int start = message.indexOf("Attribute '") + 11;
                int end = message.indexOf("'", start);
                if (end > start)
                    return message.substring(start, end);
            }

            return "unknown attribute";
        }

        // PR_XX: 辅助方法：从错误消息中提取属性值
        /**
         * extractAttributeValue
         *
         * @brief
         * 功能: 执行 extractAttributeValue 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractAttributeValue(String message) {
            if (message == null)
                return null;

            // cvc-attribute.3 格式: "The value 'XXX' of attribute"
            // 例如: "The value '9223372036854775808' of attribute 'Duration' on element..."
            if (message.contains("The value '")) {
                int start = message.indexOf("The value '") + 11;
                int end = message.indexOf("'", start);
                if (end > start) {
                    return message.substring(start, end);
                }
            }

            // Xerces 版本差异：有的版本输出 "Value 'XXX'"（没有 The）。
            if (message.contains("Value '")) {
                int start = message.indexOf("Value '") + 7;
                int end = message.indexOf("'", start);
                if (end > start) {
                    return message.substring(start, end);
                }
            }

            // 中文格式: "值 'XXX'"
            if (message.contains("值 '")) {
                int start = message.indexOf("值 '") + 3;
                int end = message.indexOf("'", start);
                if (end > start) {
                    return message.substring(start, end);
                }
            }

            // 另一种格式: "'XXX' is not"
            if (message.contains("' is not") && message.indexOf("'") >= 0) {
                int start = message.indexOf("'") + 1;
                int end = message.indexOf("'", start);
                if (end > start) {
                    return message.substring(start, end);
                }
            }

            return null;
        }

        // 辅助方法：从错误消息中提取期望的元素名称
        /**
         * extractExpectedElement
         *
         * @brief
         * 功能: 执行 extractExpectedElement 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractExpectedElement(String message) {
            if (message == null)
                return "";

            // 格式1: "One of '{...}' is expected"
            // 注意：expected 列表可能包含多个候选元素；这里需要提取“第一个”期望元素，
            // 否则会误判（例如 CoreOSPayload 重复出现时，期望的后继元素是 SharedDataPayload，
            // 但错误消息中还会包含其他候选元素，取最后一个会导致被当成2007）。
            if (message.contains("is expected")) {
                try {
                    // 只在期望段落中提取 expected，避免误匹配到 actual 元素的 QName
                    // 例如: Invalid content ... element '{ns}Partition'. One of '{ns}PseudoPartition'
                    // is expected.
                    String expectedSegment = message;
                    int oneOfIdx = message.indexOf("One of");
                    if (oneOfIdx >= 0) {
                        expectedSegment = message.substring(oneOfIdx);
                    }

                    // 先匹配 JAXP 常见 QName 格式：{namespace}LocalName
                    // 例如: One of '{http://www.actri.avic/ConfigRecord}PseudoPartition' is expected.
                    java.util.regex.Matcher braceQNameMatcher = java.util.regex.Pattern
                            .compile("\\{[^}]+\\}([A-Za-z0-9_]+)")
                            .matcher(expectedSegment);
                    if (braceQNameMatcher.find()) {
                        return braceQNameMatcher.group(1).trim();
                    }

                    // 优先匹配带命名空间的QName列表：{"ns":ElementName, ...}
                    java.util.regex.Matcher nsMatcher = java.util.regex.Pattern
                            .compile("\\{\\s*\\\"[^\\\"]+\\\"\\s*:\\s*([A-Za-z0-9_]+)")
                            .matcher(expectedSegment);
                    if (nsMatcher.find()) {
                        return nsMatcher.group(1).trim();
                    }

                    // 再匹配不带命名空间的列表：{ElementName, ...}
                    java.util.regex.Matcher plainMatcher = java.util.regex.Pattern
                            .compile("\\{\\s*([A-Za-z0-9_]+)")
                            .matcher(expectedSegment);
                    if (plainMatcher.find()) {
                        return plainMatcher.group(1).trim();
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }

            // 格式2: "Expected elements 'XXX'"
            if (message.contains("Expected elements '")) {
                try {
                    int start = message.indexOf("Expected elements '") + 19;
                    int end = message.indexOf("'", start);
                    if (end > start) {
                        return message.substring(start, end);
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }

            return "";
        }

        // 辅助方法：从错误消息中提取所有“期望的元素名称”候选列表
        // 适用场景："One of '{...}' is expected" 里包含多个候选元素。
        private java.util.List<String> extractExpectedElements(String message) {
            java.util.List<String> results = new java.util.ArrayList<>();
            if (message == null || message.isEmpty()) {
                return results;
            }

            if (!message.contains("is expected")) {
                return results;
            }

            try {
                String expectedSegment = message;
                int oneOfIdx = message.indexOf("One of");
                if (oneOfIdx >= 0) {
                    expectedSegment = message.substring(oneOfIdx);
                }

                // 尝试提取 One of '...' / One of "..."
                String inner = null;
                char quoteChar = '\0';
                int quoteStart = expectedSegment.indexOf("One of '");
                if (quoteStart >= 0) {
                    quoteChar = '\'';
                } else {
                    quoteStart = expectedSegment.indexOf("One of \"");
                    if (quoteStart >= 0) {
                        quoteChar = '"';
                    }
                }

                if (quoteStart >= 0) {
                    int start = quoteStart + 8;
                    String endToken = String.valueOf(quoteChar) + " is expected";
                    int end = expectedSegment.indexOf(endToken, start);
                    if (end > start) {
                        inner = expectedSegment.substring(start, end);
                    } else {
                        // 兜底：找到最近的同类引号作为结束
                        end = expectedSegment.indexOf(String.valueOf(quoteChar), start);
                        if (end > start) {
                            inner = expectedSegment.substring(start, end);
                        }
                    }
                }

                if (inner == null || inner.trim().isEmpty()) {
                    // 回退：直接在 expectedSegment 中找所有 {namespace}LocalName
                    java.util.regex.Matcher braceQNameMatcher = java.util.regex.Pattern
                            .compile("\\{[^}]+\\}([A-Za-z0-9_]+)")
                            .matcher(expectedSegment);
                    while (braceQNameMatcher.find()) {
                        String name = normalizeElementName(braceQNameMatcher.group(1));
                        if (!name.isEmpty()) {
                            results.add(name);
                        }
                    }

                    // 如果能拿到带命名空间的QName候选，直接返回
                    if (!results.isEmpty()) {
                        return new java.util.ArrayList<>(new java.util.LinkedHashSet<>(results));
                    }

                    // 再回退：处理不带命名空间的列表：{A, B, C}
                    java.util.regex.Matcher listMatcher = java.util.regex.Pattern
                            .compile("\\{([^}]*)\\}")
                            .matcher(expectedSegment);
                    while (listMatcher.find()) {
                        String content = listMatcher.group(1);
                        if (content == null || content.trim().isEmpty()) {
                            continue;
                        }
                        for (String part : content.split(",")) {
                            String token = (part == null) ? "" : part.trim();
                            token = normalizeElementName(token);
                            if (!token.isEmpty()) {
                                results.add(token);
                            }
                        }
                    }

                    return new java.util.ArrayList<>(new java.util.LinkedHashSet<>(results));
                }

                // inner 可能包含形如 "{uri}A, {uri}B" 或 "A, B"。
                String[] parts = inner.split(",");
                for (String part : parts) {
                    if (part == null) {
                        continue;
                    }
                    String token = part.trim();
                    if (token.isEmpty()) {
                        continue;
                    }

                    // token 可能是 {namespace}LocalName
                    if (token.startsWith("{") && token.contains("}")) {
                        int braceIdx = token.indexOf('}');
                        if (braceIdx >= 0 && braceIdx + 1 < token.length()) {
                            token = token.substring(braceIdx + 1);
                        }
                        // 某些实现会在列表末尾多带一个 '}'，这里做一次兜底清理
                        if (token.endsWith("}")) {
                            token = token.substring(0, token.length() - 1);
                        }
                    }
                    // token 可能是 prefix:LocalName
                    int colonIdx = token.lastIndexOf(':');
                    if (colonIdx >= 0 && colonIdx + 1 < token.length()) {
                        token = token.substring(colonIdx + 1);
                    }

                    // 清理列表拼接带来的包裹符号：例如 SharedLibraryPayload} / "Name" / {Name
                    while (!token.isEmpty() && (token.endsWith("}") || token.endsWith("'") || token.endsWith("\""))) {
                        token = token.substring(0, token.length() - 1).trim();
                    }
                    while (!token.isEmpty()
                            && (token.startsWith("{") || token.startsWith("'") || token.startsWith("\""))) {
                        token = token.substring(1).trim();
                    }

                    token = normalizeElementName(token);
                    if (!token.isEmpty()) {
                        results.add(token);
                    }
                }

                return new java.util.ArrayList<>(new java.util.LinkedHashSet<>(results));
            } catch (Exception e) {
                return results;
            }
        }

        // PR_12: 辅助方法：从 "Invalid content was found starting with element 'XXX'"
        // 中提取实际元素名称
        /**
         * extractInvalidContentElement
         *
         * @brief
         * 功能: 执行 extractInvalidContentElement 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractInvalidContentElement(String message) {
            if (message == null)
                return "";

            // 格式: "Invalid content was found starting with element 'XXX'" 或
            // "Invalid content was found starting with element '{namespace:XXX}'"
            try {
                if (message.contains("Invalid content was found starting with element")) {
                    // 查找 "element '" 或 "element \""
                    int elementStart = message.indexOf("starting with element");
                    if (elementStart >= 0) {
                        // 查找元素名称的开始位置（单引号或双引号）
                        int quoteStart = message.indexOf("'", elementStart);
                        int doubleQuoteStart = message.indexOf("\"", elementStart);

                        int start = -1;
                        char quoteChar = '\'';
                        if (quoteStart >= 0 && (doubleQuoteStart < 0 || quoteStart < doubleQuoteStart)) {
                            start = quoteStart + 1;
                            quoteChar = '\'';
                        } else if (doubleQuoteStart >= 0) {
                            start = doubleQuoteStart + 1;
                            quoteChar = '"';
                        }

                        if (start >= 0) {
                            int end = message.indexOf(String.valueOf(quoteChar), start);
                            if (end > start) {
                                String fullName = message.substring(start, end);
                                // 如果包含命名空间，提取元素名
                                // 常见格式：{namespaceURI}LocalName 或 {namespaceURI}prefix:LocalName
                                // 注意：namespaceURI 可能包含 "http://"，不能用 ':' 直接切分
                                if (fullName.startsWith("{") && fullName.contains("}")) {
                                    int braceIndex = fullName.indexOf('}');
                                    if (braceIndex >= 0 && braceIndex + 1 < fullName.length()) {
                                        String local = fullName.substring(braceIndex + 1);
                                        // 处理可能的 prefix:LocalName
                                        if (local.contains(":")) {
                                            local = local.substring(local.lastIndexOf(':') + 1);
                                        }
                                        return local;
                                    }
                                }
                                return fullName;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
            }

            return "";
        }

        // PR_XX: 规范化元素名，去除命名空间与前缀
        // 支持：{namespaceURI}LocalName / {namespaceURI}prefix:LocalName /
        // prefix:LocalName
        /**
         * normalizeElementName
         *
         * @brief
         * 功能: 执行 normalizeElementName 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String normalizeElementName(String name) {
            if (name == null || name.isEmpty()) {
                return "";
            }
            String normalized = name.trim();
            // Xerces 在带命名空间时输出 '{"URI":LocalName}'，先剥掉成对花括号
            if (normalized.startsWith("{") && normalized.endsWith("}") && normalized.length() > 1) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }
            int braceIdx = normalized.lastIndexOf('}');
            if (braceIdx >= 0 && braceIdx + 1 < normalized.length()) {
                normalized = normalized.substring(braceIdx + 1);
            }
            // XML 名称不允许 '}'，残留尾花括号直接剔除
            if (normalized.endsWith("}")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            int colonIdx = normalized.lastIndexOf(':');
            if (colonIdx >= 0 && colonIdx + 1 < normalized.length()) {
                normalized = normalized.substring(colonIdx + 1);
            }
            return normalized.trim();
        }

        /**
         * 解析 2010 文案中的 maxOccurs：优先错误消息，其次 XSD 声明，禁止 Java 常数表。
         */
        private String resolveMaxOccursForMessage(String elementName, String relatedElement, String errorMsg) {
            String fromMessage = extractMaxValue(errorMsg);
            if (fromMessage != null && !fromMessage.isEmpty()) {
                return fromMessage;
            }
            String fromXsd = getMaxOccursFromXsd(elementName, relatedElement);
            return fromXsd != null && !fromXsd.isEmpty() ? fromXsd : "";
        }

        /**
         * 解析 2009 文案中的 minOccurs：优先错误消息，其次 XSD 声明，禁止 Java 常数表。
         */
        private String resolveMinOccursForMessage(String elementName, String relatedElement, String errorMsg) {
            String fromMessage = extractMinValue(errorMsg);
            if (fromMessage != null && !fromMessage.isEmpty()) {
                return fromMessage;
            }
            String fromXsd = getMinOccursFromXsd(elementName, relatedElement);
            return fromXsd != null && !fromXsd.isEmpty() ? fromXsd : "";
        }

        // 辅助方法：从错误消息中提取最小值
        /**
         * extractMinValue
         *
         * @brief
         * 功能: 执行 extractMinValue 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractMinValue(String message) {
            if (message == null)
                return "";
            if (message.contains("minOccurs")) {
                // 尝试提取数字
                try {
                    String[] parts = message.split("minOccurs");
                    if (parts.length > 1) {
                        String numPart = parts[1].replaceAll("[^0-9]", "");
                        if (!numPart.isEmpty())
                            return numPart;
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
            return "";
        }

        // 辅助方法：从错误消息中提取最大值
        /**
         * extractMaxValue
         *
         * @brief
         * 功能: 执行 extractMaxValue 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractMaxValue(String message) {
            if (message == null) {
                return "";
            }

            // XSD验证器的错误消息在不同实现/语言环境下会有多种格式。
            // 这里尽量从消息中提取 maxOccurs/maximum 的数值，且支持 unbounded。
            // 示例（不同实现可能略有差异）：
            // - ... maxOccurs '255' ...
            // - ... maxOccurs="255" ...
            // - ... more than "1" occurrence(s) ...
            // - ... greater than maximum 255 ...
            // - ... maximum 'unbounded' ...
            try {
                String[] patterns = new String[] {
                        "(?i)maxOccurs\\s*(?:=|is|:)?\\s*['\"]?(unbounded|\\d+)['\"]?",
                        "(?i)maximum\\s*['\"]?(unbounded|\\d+)['\"]?",
                        "(?i)exceeds\\s+maximum\\s*['\"]?(unbounded|\\d+)['\"]?",
                        "(?i)greater\\s+than\\s+maximum\\s*['\"]?(unbounded|\\d+)['\"]?",
                        "(?i)more\\s+than\\s*['\"]?(unbounded|\\d+)['\"]?" // occurrences/element(s)
                };

                for (String patternText : patterns) {
                    Matcher matcher = Pattern.compile(patternText).matcher(message);
                    if (matcher.find()) {
                        String value = matcher.group(1);
                        return value == null ? "" : value;
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
            }

            return "";
        }

        // 辅助方法：从错误消息中提取范围
        /**
         * extractRange
         *
         * @brief
         * 功能: 执行 extractRange 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String[] extractRange(String message) {
            String[] result = new String[] { "", "" };
            if (message == null)
                return result;

            try {
                // cvc-facet-valid 格式: "minInclusive '18'"
                // 尝试提取带引号的值
                if (message.contains("minInclusive '")) {
                    int start = message.indexOf("minInclusive '") + 14;
                    int end = message.indexOf("'", start);
                    if (end > start) {
                        result[0] = message.substring(start, end);
                    }
                } else if (message.contains("minInclusive")) {
                    // 备用方案：提取数字
                    String[] parts = message.split("minInclusive");
                    if (parts.length > 1) {
                        String numPart = parts[1].replaceAll("[^0-9.-]", "");
                        if (!numPart.isEmpty())
                            result[0] = numPart;
                    }
                }

                if (message.contains("maxInclusive '")) {
                    int start = message.indexOf("maxInclusive '") + 14;
                    int end = message.indexOf("'", start);
                    if (end > start) {
                        result[1] = message.substring(start, end);
                    }
                } else if (message.contains("maxInclusive")) {
                    String[] parts = message.split("maxInclusive");
                    if (parts.length > 1) {
                        String numPart = parts[1].replaceAll("[^0-9.-]", "");
                        if (!numPart.isEmpty())
                            result[1] = numPart;
                    }
                }

                // 也支持 minExclusive 和 maxExclusive
                if (result[0].isEmpty()) {
                    if (message.contains("minExclusive '")) {
                        int start = message.indexOf("minExclusive '") + 14;
                        int end = message.indexOf("'", start);
                        if (end > start) {
                            result[0] = message.substring(start, end);
                        }
                    } else if (message.contains("minExclusive")) {
                        String[] parts = message.split("minExclusive");
                        if (parts.length > 1) {
                            String numPart = parts[1].replaceAll("[^0-9.-]", "");
                            if (!numPart.isEmpty())
                                result[0] = numPart;
                        }
                    }
                }

                if (result[1].isEmpty()) {
                    if (message.contains("maxExclusive '")) {
                        int start = message.indexOf("maxExclusive '") + 14;
                        int end = message.indexOf("'", start);
                        if (end > start) {
                            result[1] = message.substring(start, end);
                        }
                    } else if (message.contains("maxExclusive")) {
                        String[] parts = message.split("maxExclusive");
                        if (parts.length > 1) {
                            String numPart = parts[1].replaceAll("[^0-9.-]", "");
                            if (!numPart.isEmpty())
                                result[1] = numPart;
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
            }

            return result;
        }

        // 辅助方法：从错误消息中提取值
        /**
         * extractValue
         *
         * @brief
         * 功能: 执行 extractValue 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractValue(String message) {
            if (message == null)
                return "unknown";

            // 尝试多种模式提取值
            if (message.contains("value '")) {
                int start = message.indexOf("value '") + 7;
                int end = message.indexOf("'", start);
                if (end > start)
                    return message.substring(start, end);
            }
            if (message.contains("Value '")) {
                int start = message.indexOf("Value '") + 7;
                int end = message.indexOf("'", start);
                if (end > start)
                    return message.substring(start, end);
            }
            if (message.contains("value \"")) {
                int start = message.indexOf("value \"") + 7;
                int end = message.indexOf("\"", start);
                if (end > start)
                    return message.substring(start, end);
            }
            // 尝试提取引号中的任何值
            if (message.contains("'")) {
                int start = message.indexOf("'") + 1;
                int end = message.indexOf("'", start);
                if (end > start)
                    return message.substring(start, end);
            }

            return "unknown";
        }

        /**
         * isLikelyDataTypeErrorValue
         *
         * @brief
         * 功能: 执行 isLikelyDataTypeErrorValue 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private boolean isLikelyDataTypeErrorValue(String value) {
            if (value == null) {
                return false;
            }

            String trimmed = value.trim();
            if (trimmed.isEmpty() || "unknown".equalsIgnoreCase(trimmed)) {
                return false;
            }

            // 常见“非整数词法”：小数/科学计数法
            if (trimmed.contains(".") || trimmed.contains("e") || trimmed.contains("E")) {
                return true;
            }

            // 纯十进制整数或十六进制值（0x...）不视为类型错误
            if (trimmed.matches("^[+-]?\\d+$") || trimmed.matches("^0[xX][0-9a-fA-F]+$")) {
                return false;
            }

            // 其他包含字母/符号的值，通常是类型错误（如 test）
            return true;
        }

        // PR_15: 辅助方法：从cvc-enumeration-valid错误消息中提取枚举值列表
        /**
         * extractEnumValues
         *
         * @brief
         * 功能: 执行 extractEnumValues 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractEnumValues(String message) {
            if (message == null)
                return "";

            // cvc-enumeration-valid错误格式:
            // "Value 'XXX' is not facet-valid with respect to enumeration '[VALUE1, VALUE2,
            // VALUE3]'."
            try {
                if (message.contains("enumeration '[")) {
                    int start = message.indexOf("enumeration '[") + 14;
                    int end = message.indexOf("]'", start);
                    if (end > start) {
                        String enumList = message.substring(start, end);
                        // 移除多余的空格
                        return enumList.trim();
                    }
                }
                // 备用方案：查找方括号中的内容
                if (message.contains("[") && message.contains("]")) {
                    int start = message.indexOf("[") + 1;
                    int end = message.indexOf("]", start);
                    if (end > start) {
                        return message.substring(start, end).trim();
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
            }

            return ""; // 返回空字符串，让调用方决定默认值
        }

        // PR_XX: 辅助方法：判断cvc-attribute.3错误是否与枚举类型相关
        // 枚举类型的特征：
        // 1. 不包含数值范围关键字（minInclusive, maxInclusive等）
        // 2. 类型名通常以Type结尾（如CachePolicyType, UserAccessRightsType）
        // 3. 错误消息中不包含数字范围或长度限制
        /**
         * isLikelyEnumerationType
         *
         * @brief
         * 功能: 执行 isLikelyEnumerationType 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private boolean isLikelyEnumerationType(String errorMsg) {
            if (errorMsg == null) {
                return false;
            }

            // 排除数值范围类型
            if (errorMsg.contains("minInclusive") || errorMsg.contains("maxInclusive") ||
                    errorMsg.contains("minExclusive") || errorMsg.contains("maxExclusive") ||
                    errorMsg.contains("totalDigits") || errorMsg.contains("fractionDigits")) {
                return false;
            }

            // 排除长度限制类型
            if (errorMsg.contains("minLength") || errorMsg.contains("maxLength") ||
                    errorMsg.contains("length")) {
                return false;
            }

            // 检查是否包含类型名（以Type或Enum结尾）
            // 模式: "with respect to its type, 'XXXType'" 或 "type 'XXXType'" 或 "type
            // 'XXXEnum'"
            if (errorMsg.contains("type '") || errorMsg.contains("type, '")) {
                try {
                    int typeStart = errorMsg.indexOf("type");
                    if (typeStart >= 0) {
                        int quoteStart = errorMsg.indexOf("'", typeStart);
                        if (quoteStart >= 0) {
                            int quoteEnd = errorMsg.indexOf("'", quoteStart + 1);
                            if (quoteEnd > quoteStart) {
                                String typeName = errorMsg.substring(quoteStart + 1, quoteEnd);
                                String typeNameLower = typeName.toLowerCase();
                                // 检查是否以Type或Enum结尾（忽略大小写），且不是已知的数值类型
                                if ((typeNameLower.endsWith("type") || typeNameLower.endsWith("enum")
                                        || typeName.equalsIgnoreCase("PortAttributeTypeApp")
                                        || typeName.equalsIgnoreCase("PortAttributeTypePar")) &&
                                        !typeName.equals("TimeValueType") &&
                                        !typeName.equals("DecOrHexValueType") &&
                                        !typeName.equals("IdentifierValueType") &&
                                        !typeName.equals("HexValueType") &&
                                        !typeName.equals("CRC32Type")) {
                                    return true;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }

            return false;
        }

        // PR_XX: 辅助方法：从cvc-attribute.3错误消息中提取枚举值范围
        // 尝试从XSD中读取该类型的枚举定义，如果读取失败则返回空字符串
        /**
         * extractEnumValuesFromMessage
         *
         * @brief
         * 功能: 执行 extractEnumValuesFromMessage 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractEnumValuesFromMessage(String errorMsg) {
            if (errorMsg == null) {
                return "";
            }

            // 目前简化实现：返回空字符串，让调用方使用默认值
            // 未来可以扩展：解析XSD文件获取枚举类型的真实值列表
            // 例如: CachePolicyType -> [IO, Cached, CachedNotShared, IO_CACHED, ...]

            return "";
        }

        // 辅助方法：从错误消息中提取类型名称
        // 错误消息格式示例："... for type 'TimeValueType'."
        /**
         * extractTypeNameFromMessage
         *
         * @brief
         * 功能: 执行 extractTypeNameFromMessage 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractTypeNameFromMessage(String message) {
            if (message == null)
                return null;

            try {
                String[] markers = { "for type '", "type, '", "type '" };
                for (String marker : markers) {
                    int markerStart = message.indexOf(marker);
                    if (markerStart >= 0) {
                        int start = markerStart + marker.length();
                        int end = message.indexOf("'", start);
                        if (end > start) {
                            return message.substring(start, end);
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
            }

            return null;
        }

        // PR_05: 辅助方法：从字符串长度验证错误消息中提取长度范围
        /**
         * extractLengthRange
         *
         * @brief
         * 功能: 执行 extractLengthRange 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractLengthRange(String message) {
            if (message == null)
                return "string length constraint";

            // PR_XX: 尝试从错误消息中提取类型名，然后从XSD读取完整的长度约束
            String typeName = extractTypeNameFromMessage(message);
            if (typeName != null && !typeName.isEmpty()) {
                String lengthConstraint = getLengthConstraintFromXsd(typeName);
                if (lengthConstraint != null && !lengthConstraint.isEmpty()) {
                    return lengthConstraint;
                }
            }

            try {
                // 回退方案：从错误消息中解析（但只能获取部分信息）
                // cvc-maxLength-valid格式:
                // "Value 'xxx' with length = '10' is not facet-valid with respect to maxLength
                // '8' for type 'xxx'."
                if (message.contains("maxLength '")) {
                    int start = message.indexOf("maxLength '") + 11;
                    int end = message.indexOf("'", start);
                    if (end > start) {
                        return "0, " + message.substring(start, end);
                    }
                }

                // cvc-minLength-valid格式:
                // "Value 'xxx' with length = '2' is not facet-valid with respect to minLength
                // '4' for type 'xxx'."
                if (message.contains("minLength '")) {
                    int start = message.indexOf("minLength '") + 11;
                    int end = message.indexOf("'", start);
                    if (end > start) {
                        return message.substring(start, end) + ", unbounded";
                    }
                }

                // cvc-length-valid格式:
                // "Value 'xxx' with length = '5' is not facet-valid with respect to length '8'
                // for type 'xxx'."
                if (message.contains("length '")) {
                    int start = message.indexOf("length '") + 8;
                    int end = message.indexOf("'", start);
                    if (end > start) {
                        String len = message.substring(start, end);
                        return len + ", " + len; // 精确长度
                    }
                }

                // 尝试提取数字
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    return "0, " + matcher.group(1);
                }
            } catch (Exception e) {
                // 忽略解析错误
            }

            return "string length constraint";
        }

        // 在 XSD 校验器只回调 cvc-attribute.3 时，使用类型名识别长度约束。
        /**
         * getKnownLengthConstraint
         *
         * @brief
         * 功能: 执行 getKnownLengthConstraint 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String getKnownLengthConstraint(String typeName) {
            if (typeName == null || typeName.isEmpty()) {
                return null;
            }
            switch (typeName) {
                case "NameType":
                case "PortNameType":
                    return "1, 30";
                case "DriverNameType":
                    return "1, 255";
                case "CallbackType":
                    return "0, 31";
                case "ACTRI_ModuleLevelErrorRecoveryActionType":
                case "ACTRI_PartitionLevelErrorRecoveryActionType":
                    return "1, 63";
                default:
                    return null;
            }
        }

        // PR_XX: 辅助方法：从XSD中读取类型的长度约束（minLength和maxLength）
        // 返回格式："minLength, maxLength"，例如："1, 30"
        /**
         * getLengthConstraintFromXsd
         *
         * @brief
         * 功能: 执行 getLengthConstraintFromXsd 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String getLengthConstraintFromXsd(String typeName) {
            if (typeName == null || typeName.isEmpty()) {
                return null;
            }

            try {
                // 检查TypeLib.xsd（与Schema加载一致的路径解析，禁止依赖工作目录）
                File typeLibFile = getSchemaFile(TYPE_LIB_XSD_FILE);
                if (typeLibFile == null || !typeLibFile.exists()) {
                    return null;
                }

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(typeLibFile);

                // 查找指定类型的simpleType定义
                NodeList simpleTypes = doc.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI,
                        "simpleType");
                for (int i = 0; i < simpleTypes.getLength(); i++) {
                    Element simpleType = (Element) simpleTypes.item(i);
                    String name = simpleType.getAttribute("name");
                    if (typeName.equals(name)) {
                        // 找到类型定义，查找minLength和maxLength
                        String minLength = null;
                        String maxLength = null;

                        NodeList minLengthNodes = simpleType.getElementsByTagNameNS(
                                XMLConstants.W3C_XML_SCHEMA_NS_URI, "minLength");
                        if (minLengthNodes.getLength() > 0) {
                            minLength = ((Element) minLengthNodes.item(0)).getAttribute("value");
                        }

                        NodeList maxLengthNodes = simpleType.getElementsByTagNameNS(
                                XMLConstants.W3C_XML_SCHEMA_NS_URI, "maxLength");
                        if (maxLengthNodes.getLength() > 0) {
                            maxLength = ((Element) maxLengthNodes.item(0)).getAttribute("value");
                        }

                        // 如果两者都找到，返回范围
                        if (minLength != null && maxLength != null) {
                            return minLength + ", " + maxLength;
                        } else if (minLength != null) {
                            return minLength + ", unbounded";
                        } else if (maxLength != null) {
                            return "0, " + maxLength;
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
            }

            return null;
        }

        // PR_06: 辅助方法：从数值范围验证错误消息中提取范围
        // PR_15: 修复范围提取，确保同时显示最小值和最大值
        /**
         * extractNumericRange
         *
         * @brief
         * 功能: 执行 extractNumericRange 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private String extractNumericRange(String message) {
            if (message == null)
                return "numeric range constraint";

            try {
                // PR_XX: cvc-attribute.3 + TimeValueType 格式:
                // "The value '0' of attribute 'Offset' on element 'PartitionWindow' is not
                // valid with respect to its type, 'TimeValueType'."
                // 从XSD动态读取TimeValueType的范围约束
                if (message.contains("TimeValueType")) {
                    String constraint = getTypeConstraintFromXsd("TimeValueType");
                    // 如果动态读取失败，使用默认值（这不应该发生，但作为后备）
                    return constraint != null ? constraint : "1, 92233";
                }

                // cvc-maxInclusive-valid格式:
                // "Value 'xxx' is not facet-valid with respect to maxInclusive 'yyy' for type
                // 'zzz'."
                if (message.contains("maxInclusive '")) {
                    int start = message.indexOf("maxInclusive '") + 14;
                    int end = message.indexOf("'", start);
                    if (end > start) {
                        String maxValue = message.substring(start, end);
                        // 尝试从错误消息中提取类型名称
                        String typeName = extractTypeNameFromMessage(message);
                        if (typeName != null) {
                            String constraint = getTypeConstraintFromXsd(typeName);
                            if (constraint != null) {
                                return constraint;
                            }
                        }
                        // 回退：使用消息中的maxValue和假设的minValue=1
                        return "1, " + maxValue;
                    }
                }

                // cvc-minInclusive-valid格式:
                // "Value 'xxx' is not facet-valid with respect to minInclusive 'yyy' for type
                // 'zzz'."
                if (message.contains("minInclusive '")) {
                    int start = message.indexOf("minInclusive '") + 14;
                    int end = message.indexOf("'", start);
                    if (end > start) {
                        String minValue = message.substring(start, end);
                        // 尝试从错误消息中提取类型名称
                        String typeName = extractTypeNameFromMessage(message);
                        if (typeName != null) {
                            String constraint = getTypeConstraintFromXsd(typeName);
                            if (constraint != null) {
                                return constraint;
                            }
                        }
                        // 回退：使用消息中的minValue和默认的maxValue
                        return minValue + ", 9223372036854775807";
                    }
                }

                // cvc-totalDigits-valid格式:
                // "Value 'xxx' has yyy total digits, but the number of total digits has been
                // limited to zzz."
                if (message.contains("totalDigits")) {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("limited to (\\d+)");
                    java.util.regex.Matcher matcher = pattern.matcher(message);
                    if (matcher.find()) {
                        return "max " + matcher.group(1) + " digits";
                    }
                }

                // 尝试提取任何数字作为范围
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("'([0-9a-fA-Fx]+)'");
                java.util.regex.Matcher matcher = pattern.matcher(message);
                java.util.List<String> numbers = new java.util.ArrayList<>();
                while (matcher.find() && numbers.size() < 2) {
                    numbers.add(matcher.group(1));
                }
                if (numbers.size() >= 2) {
                    return numbers.get(0) + ", " + numbers.get(1);
                } else if (numbers.size() == 1) {
                    return "1, " + numbers.get(0);
                }
            } catch (Exception e) {
                // 忽略解析错误
            }

            return "numeric range constraint";
        }

        /**
         * warning
         *
         * @brief
         * 功能: 执行 warning 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        @Override
        public void warning(SAXParseException e) {
            addMessage("Warning", e);
        }

        /**
         * 004930：CRC32属性的格式错误属于CRC属性告警，不应被XSD阶段归类为2013。
         * CRC32Type在XSD中有pattern约束，但需求要求缺失或格式错误统一输出4005，
         * 并继续执行CRC阶段进行后续处理。
         */
        private boolean isCrc32FormatError(SAXParseException e) {
            String message = e == null ? null : e.getMessage();
            if (message == null || !message.contains("CRC32")) {
                return false;
            }

            String attributeName = extractAttributeName(message);
            boolean crc32Attribute = "CRC32".equals(attributeName);
            boolean crc32Type = message.contains("CRC32Type");
            if (!crc32Attribute && !crc32Type) {
                return false;
            }

            return message.contains("cvc-attribute.3")
                    || message.contains("cvc-pattern-valid")
                    || message.contains("cvc-datatype-valid")
                    || message.contains("not valid")
                    || message.contains("不匹配");
        }

        /**
         * recordCrc32FormatWarning
         *
         * @brief
         * 功能: 执行 recordCrc32FormatWarning 相关处理。
         *
         * @scope internal
         * @errno:
         * N/A
         */
        private void recordCrc32FormatWarning(SAXParseException e) {
            crc32FormatWarningFound = true;
            if (suppressCrc32FormatWarnings) {
                return;
            }
            String fileName = extractFileName(e.getSystemId(), e.getPublicId());
            String warningMessage = String.format(
                    "XML file %s CRC attribute missing or type error.", fileName);
            if (!externalMessagesList.contains(warningMessage)) {
                externalMessagesList.add(warningMessage);
            }
            Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING, warningMessage);
        }

        /**
         * isExtensibleEnumExempted
         *
         * @brief TR_PR_022 (CPMVerify-TR-006361): 可扩展枚举属性豁免判定。
         *
         * 功能：数据词典中标记为「可扩展」的枚举属性按会议 817617243 豁免决议，
         *       无论输入何值均不报错、不计入校验失败。判定规则：
         *       1. cvc-attribute.3 且属性名命中白名单 → 豁免（消耗前序枚举错误待定计数）；
         *       2. cvc-enumeration-valid（先于伴随 cvc-attribute.3 到达且不含属性名）→
         *          暂缓失败判定：先计入待定计数并缓存枚举值，由伴随错误决定是否豁免；
         *          若始终未等到伴随错误，则在 hasErrors() 中保守计为失败。
         *
         * @param[in] e XSD 验证器上报的 SAX 解析异常
         * @return true 表示该错误被豁免（不报错、不置位 errorsFound）
         */
        private boolean isExtensibleEnumExempted(SAXParseException e) {
            String msg = e.getMessage();
            if (msg == null) {
                return false;
            }
            // cvc-attribute.3 且属性命中可扩展白名单 → 豁免
            // （伴随的前序 cvc-enumeration-valid 成对出现且先行到达，一并消耗待定计数）
            if (msg.contains("cvc-attribute.3") && isExtensibleEnumAttributeError(e)) {
                if (pendingEnumErrorCount > 0) {
                    pendingEnumErrorCount--;
                }
                return true;
            }
            // cvc-enumeration-valid：暂缓失败判定，仍走 addMessage 缓存枚举值（其内部会过滤不输出）
            if (msg.contains("cvc-enumeration-valid")) {
                pendingEnumErrorCount++;
                addMessage("Error", e);
                return true;
            }
            return false;
        }

        /**
         * isExtensibleEnumAttributeError
         *
         * @brief TR_PR_022: 判断 cvc-attribute.3 报文中的属性名是否命中可扩展枚举白名单。
         *
         * @param[in] errorMsg XSD 验证器原始报文
         * @return true 表示属性在白名单中（可扩展枚举属性）
         */
        private boolean isExtensibleEnumAttributeError(SAXParseException e) {
            if (e == null || e.getMessage() == null) {
                return false;
            }
            String errorMsg = e.getMessage();
            String attributeName = extractAttributeName(errorMsg);
            if (attributeName == null || !EXTENSIBLE_ENUM_ATTRIBUTES.contains(attributeName)) {
                return false;
            }

            // ModuleRecoveryAction在CoreHMTable和MultiPartitionHMTable中均可扩展；
            // 其他元素中的同名属性不能按属性名全局放行。
            String elementName = extractElementName(errorMsg);
            int errorLine = e.getLineNumber();
            if ("ModuleRecoveryAction".equals(attributeName)) {
                if (!"ErrorAction".equals(elementName)) {
                    return false;
                }
                String parentElement = findAttributeParentElement("ErrorAction", errorLine,
                        errorMsg, attributeName);
                return "CoreHMTable".equals(parentElement)
                        || "MultiPartitionHMTable".equals(parentElement);
            }

            // PartitionRecoveryAction只出现在PartitionHMTable的ErrorAction中。
            return "PartitionRecoveryAction".equals(attributeName)
                    && "ErrorAction".equals(elementName)
                    && "PartitionHMTable".equals(findAttributeParentElement("ErrorAction", errorLine,
                            errorMsg, attributeName));
        }

        /**
         * 根据错误行号找到属性所在ErrorAction的直接父元素。
         * XSD校验器有时使用展开文档行号，属性值映射可作为行号回退依据。
         */
        private String findAttributeParentElement(String childElement, int errorLine,
                String errorMsg, String attributeName) {
            String parent = elementParentMapping.get(childElement + ":PARENT:" + errorLine);
            if (parent != null) {
                return parent;
            }

            String attributeValue = extractAttributeValue(errorMsg);
            if (attributeValue != null) {
                Integer sourceLine = lineNumberMapping.get(
                        childElement + ":" + attributeName + ":" + attributeValue);
                if (sourceLine != null) {
                    parent = elementParentMapping.get(childElement + ":PARENT:" + sourceLine);
                    if (parent != null) {
                        return parent;
                    }
                }
            }

            // 最后按同名元素和错误行号附近范围查找，避免不同文件/不同HM表串用。
            for (int line = Math.max(1, errorLine - 2); line <= errorLine + 2; line++) {
                parent = elementParentMapping.get(childElement + ":PARENT:" + line);
                if (parent != null) {
                    return parent;
                }
            }
            return null;
        }

        /**
         * error
         *
         * @brief
         * 功能: 执行 error 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        @Override
        public void error(SAXParseException e) {
            // TR_PR_022: 可扩展枚举属性豁免（不报错、不计入校验失败）
            if (isExtensibleEnumExempted(e)) {
                return;
            }
            // 004930：CRC32格式错误按4005告警处理，不计入XSD错误。
            if (isCrc32FormatError(e)) {
                recordCrc32FormatWarning(e);
                return;
            }
            if (isIgnorableSequenceOrderError(e)) {
                return;
            }
            errorsFound = true;
            addMessage("Error", e);
        }

        /**
         * fatalError
         *
         * @brief
         * 功能: 执行 fatalError 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        @Override
        public void fatalError(SAXParseException e) {
            // TR_PR_022: 可扩展枚举属性豁免（不报错、不计入校验失败）
            if (isExtensibleEnumExempted(e)) {
                return;
            }
            // 004930：CRC32格式错误按4005告警处理，不计入XSD错误。
            if (isCrc32FormatError(e)) {
                recordCrc32FormatWarning(e);
                return;
            }
            if (isIgnorableSequenceOrderError(e)) {
                return;
            }
            errorsFound = true;
            // PR_02: 将Fatal Error当作普通Error处理，避免输出"Fatal Error"字样
            addMessage("Error", e);
        }

        /**
         * PR_12: 处理待确认的属性缺失错误
         * 在验证结束后调用，检查pending列表中的错误，
         * 如果对应元素没有报告过2007错误（同名子元素），则报告属性缺失错误
         */
        public void flushPendingAttributeMissingErrors() {
            for (PendingAttributeMissingError pending : pendingAttributeMissingErrors) {
                // 检查该元素是否报告过2007错误（同名子元素）
                if (!hasSameNameChildElementReported(pending.elementName) &&
                        !hasSameNameChildElementReported(pending.lineNumber)) {
                    // 没有报告过同名子元素错误，说明这是真正的属性缺失
                    // TR_PR_007: "Required attribute 'X' is missing in element 'Y'."
                    String formattedMessage = String.format(
                            "[File: %s, Line: %d] Required attribute '%s' is missing in element '%s'.",
                            pending.fileName, pending.lineNumber, pending.attributeName, pending.elementName);

                    // 检查是否已经报告过这条错误（去重）
                    if (!processedFormattedMessages.contains(formattedMessage)) {
                        processedFormattedMessages.add(formattedMessage);
                        Log.error(CodeConstant.ERROR_XML_ELEMENT_ATTRIBUTE_MISSING, formattedMessage);
                        externalMessagesList.add(formattedMessage);
                        errorsFound = true;
                    }
                }
            }
            // 清空pending列表
            pendingAttributeMissingErrors.clear();
        }

        /**
         * hasErrors
         *
         * @brief
         * 功能: 执行 hasErrors 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        public boolean hasErrors() {
            // TR_PR_022: 存在始终未等到伴随 cvc-attribute.3 的 cvc-enumeration-valid 时，
            // 保守计为校验失败（正常情况枚举错误均有伴随错误，此分支不触发）
            if (pendingEnumErrorCount > 0) {
                errorsFound = true;
            }
            // PR_12: 在返回结果前，先处理pending的属性缺失错误
            flushPendingAttributeMissingErrors();
            return errorsFound;
        }

        /**
         * 判断当前XSD验证器是否只遇到了CRC32格式错误。
         * 部分Validator实现会在回调error之后继续抛出异常，因此由外层验证方法
         * 使用该状态避免将004930错误再次包装为1018/1017。
         */
        public boolean hasOnlyCrc32FormatWarnings() {
            flushPendingAttributeMissingErrors();
            return crc32FormatWarningFound && !errorsFound && pendingEnumErrorCount == 0;
        }
    }

    // ========== 报告生成 ==========

    /**
     * generateValidationReport
     *
     * @brief
     * 功能: 生成单文件 XML 校验报告。
     * @param[in] xmlFile    XML 配置文件。
     * @param[in] isValid    校验是否通过。
     * @param[in] errors     错误信息列表。
     * @param[in] reportPath 报告输出路径。
     * @return 无。
     * @scope internal
     */
    private static void generateValidationReport(File xmlFile, boolean isValid, List<String> errors, String reportPath)
            throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(reportPath));

        try {
            // 报告标题
            writer.println("========================================");
            writer.println("    ARINC 653 配置验证报告");
            writer.println("========================================");
            writer.println();

            // 基本信息
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            writer.println("验证时间: " + sdf.format(new Date()));
            writer.println("验证文件: " + getCleanPath(xmlFile));
            writer.println("文件名称: " + xmlFile.getName());
            writer.println();

            // 验证结果摘要
            writer.println("----------------------------------------");
            writer.println("  验证结果摘要");
            writer.println("----------------------------------------");
            writer.println("总体状态: " + (isValid ? "通过 ✓" : "失败 ✗"));
            writer.println("错误/警告数量: " + errors.size());
            writer.println();

            // 执行的验证项目
            writer.println("----------------------------------------");
            writer.println("  执行的验证项");
            writer.println("----------------------------------------");
            writer.println("✓ XSD Schema 验证");
            writer.println("✓ 页对齐验证 (错误码 3003)");
            writer.println("✓ 地址范围验证 (错误码 3004)");
            writer.println("✓ 地址重叠验证 (错误码 3005)");

            // 如果是Module文件，还会执行引用验证
            if (xmlFile.getName().contains("module")) {
                writer.println("✓ 共享数据引用验证 (警告码 4001)");
                writer.println("✓ 共享库引用验证 (警告码 4002)");
                writer.println("✓ 分区调度验证 (警告码 4003)");
            }
            writer.println();

            // 详细错误/警告信息
            if (!errors.isEmpty()) {
                writer.println("----------------------------------------");
                writer.println("  详细错误/警告信息");
                writer.println("----------------------------------------");
                int count = 1;
                for (String error : errors) {
                    writer.println(count + ". " + error);
                    count++;
                }
                writer.println();
            }

            // 报告结尾
            writer.println("========================================");
            writer.println("              报告结束");
            writer.println("========================================");

        } finally {
            writer.close();
        }
    }

    /**
     * 批量验证结果类
     */
    private static class ValidationResult {
        File xmlFile;
        boolean isValid;
        List<String> errors;

        ValidationResult(File xmlFile, boolean isValid, List<String> errors) {
            this.xmlFile = xmlFile;
            this.isValid = isValid;
            this.errors = new ArrayList<>(errors);
        }
    }

    /**
     * generateBatchValidationReport
     *
     * @brief
     * 功能: 生成批量 XML 校验报告。
     * @param[in] results    校验结果列表。
     * @param[in] reportPath 报告输出路径。
     * @return 无。
     * @scope internal
     */
    private static void generateBatchValidationReport(List<ValidationResult> results, String reportPath)
            throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(reportPath));

        try {
            // 报告标题
            writer.println("========================================");
            writer.println("  ARINC 653 批量配置验证报告");
            writer.println("========================================");
            writer.println();

            // 基本信息
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            writer.println("验证时间: " + sdf.format(new Date()));
            writer.println("验证文件总数: " + results.size());
            writer.println();

            // 统计信息
            int passCount = 0;
            int failCount = 0;
            int totalErrors = 0;

            for (ValidationResult result : results) {
                if (result.isValid) {
                    passCount++;
                } else {
                    failCount++;
                }
                totalErrors += result.errors.size();
            }

            writer.println("----------------------------------------");
            writer.println("  验证结果统计");
            writer.println("----------------------------------------");
            writer.println("通过文件数: " + passCount);
            writer.println("失败文件数: " + failCount);
            writer.println("错误/警告总数: " + totalErrors);
            writer.println("总体状态: " + (failCount == 0 ? "全部通过 ✓" : "存在失败 ✗"));
            writer.println();

            // 执行的验证项目说明
            writer.println("----------------------------------------");
            writer.println("  执行的验证项");
            writer.println("----------------------------------------");
            writer.println("✓ XSD Schema 验证");
            writer.println("✓ 页对齐验证 (错误码 3003)");
            writer.println("✓ 地址范围验证 (错误码 3004)");
            writer.println("✓ 地址重叠验证 (错误码 3005)");
            writer.println("✓ 共享数据引用验证 (警告码 4001) - 仅Module文件");
            writer.println("✓ 共享库引用验证 (警告码 4002) - 仅Module文件");
            writer.println("✓ 分区调度验证 (警告码 4003) - 仅Module文件");
            writer.println();

            // 每个文件的详细结果
            writer.println("========================================");
            writer.println("  详细验证结果");
            writer.println("========================================");
            writer.println();

            int fileIndex = 1;
            for (ValidationResult result : results) {
                writer.println("【文件 " + fileIndex + "】");
                writer.println("文件名称: " + result.xmlFile.getName());
                writer.println("文件路径: " + getCleanPath(result.xmlFile));
                writer.println("验证状态: " + (result.isValid ? "通过 ✓" : "失败 ✗"));
                writer.println("错误/警告数量: " + result.errors.size());

                if (!result.errors.isEmpty()) {
                    writer.println();
                    writer.println("错误/警告详情:");
                    int errorIndex = 1;
                    for (String error : result.errors) {
                        writer.println("  " + errorIndex + ". " + error);
                        errorIndex++;
                    }
                }

                writer.println();
                writer.println("------------------------------------------------------------");
                writer.println();
                fileIndex++;
            }

            // 报告结尾
            writer.println("========================================");
            writer.println("            报告结束");
            writer.println("========================================");

        } finally {
            writer.close();
        }
    }

    // ========== 使用示例 ==========

    /**
     * 命令行工具入口
     * 
     * <p>
     * <b>使用开关控制警告验证行为：</b>
     * </p>
     * 
     * <pre>
     * // 默认：XSD验证失败时跳过警告验证
     * ARINC653ConfigValidator.setExecuteWarningsOnXsdFailure(false);
     * 
     * // 即使XSD失败也执行警告验证（一次性发现所有问题）
     * ARINC653ConfigValidator.setExecuteWarningsOnXsdFailure(true);
     * </pre>
     */
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  ARINC 653 Configuration Validation Tool");
        System.out.println("========================================\n");

        // 可以在这里设置开关：即使XSD失败也执行警告验证
        setExecuteWarningsOnXsdFailure(true);

        List<File> filesToValidate = new ArrayList<>();

        // 处理命令行参数
        if (args.length == 0) {
            // 没有参数，使用默认测试目录
            System.out.println("No files specified, using default test directory...\n");
            File defaultDir = new File("src/main/resources/xml/CPM_L1");
            if (defaultDir.isDirectory()) {
                File[] xmlFiles = defaultDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
                if (xmlFiles != null) {
                    for (File xmlFile : xmlFiles) {
                        filesToValidate.add(xmlFile);
                    }
                }
            } else {
                System.err.println("Error: default test directory does not exist: " + defaultDir.getPath());
            }
        } else {
            // 解析命令行参数
            for (String arg : args) {
                File file = new File(arg);
                if (file.isDirectory()) {
                    // 如果是目录，扫描所有XML文件
                    File[] xmlFiles = file.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
                    if (xmlFiles != null && xmlFiles.length > 0) {
                        for (File xmlFile : xmlFiles) {
                            filesToValidate.add(xmlFile);
                        }
                        System.out.println("Found " + xmlFiles.length + " XML file(s) in directory " + file.getPath());
                    } else {
                        System.err.println("Warning: no XML files found in directory - " + arg);
                    }
                } else if (file.exists()) {
                    filesToValidate.add(file);
                } else {
                    System.err.println("Warning: file or directory does not exist - " + arg);
                }
            }
        }

        if (filesToValidate.isEmpty()) {
            System.err.println("Error: no files found to validate");
            System.out.println("\nUsage:");
            System.out.println(
                    "  java -cp target/classes com.nwpu.verify.layers.validation.ARINC653ConfigValidator <file1> [file2] [file3] ...");
            System.out.println(
                    "  java -cp target/classes com.nwpu.verify.layers.validation.ARINC653ConfigValidator <directory>");
            System.out.println("\nExamples:");
            System.out.println(
                    "  java -cp target/classes com.nwpu.verify.layers.validation.ARINC653ConfigValidator src/main/resources/xml/CPM_L1/CPM_L1_module.xml");
            System.out.println(
                    "  java -cp target/classes com.nwpu.verify.layers.validation.ARINC653ConfigValidator src/main/resources/xml/CPM_L1/");
            return;
        }

        // 执行验证
        System.out.println("Number of files to validate: " + filesToValidate.size());
        System.out.println();

        // 设置即使XSD验证失败也执行警告验证（可选）
        setExecuteWarningsOnXsdFailure(true);

        List<ValidationResult> results = new ArrayList<>();
        int passCount = 0;
        int failCount = 0;

        for (File xmlFile : filesToValidate) {
            System.out.println("Validating: " + xmlFile.getName());
            List<String> errors = new ArrayList<>();

            try {
                boolean isValid = validateXmlAuto(xmlFile, errors, true); // 默认启用CRC校验
                results.add(new ValidationResult(xmlFile, isValid, errors));

                if (isValid) {
                    System.out.println("  ✓ Passed");
                    passCount++;
                } else {
                    System.out.println("  ✗ Failed (" + errors.size() + " error(s)/warning(s))");
                    failCount++;
                }
            } catch (Exception e) {
                System.out.println("  ✗ Exception: " + e.getMessage());
                List<String> errorList = new ArrayList<>();
                errorList.add("Validation exception: " + e.getMessage());
                results.add(new ValidationResult(xmlFile, false, errorList));
                failCount++;
            }
        }

        // 生成验证报告
        System.out.println();
        System.out.println("----------------------------------------");
        System.out.println("Validation complete: " + passCount + " passed, " + failCount + " failed");
        System.out.println("----------------------------------------");

        String reportPath = "validation_report.txt";
        try {
            if (results.size() == 1) {
                // 单个文件，生成简单报告
                ValidationResult result = results.get(0);
                generateValidationReport(result.xmlFile, result.isValid, result.errors, reportPath);
            } else {
                // 多个文件，生成批量报告
                generateBatchValidationReport(results, reportPath);
            }
            System.out.println("\nValidation report generated: " + reportPath);
        } catch (IOException e) {
            System.err.println("Failed to generate report: " + e.getMessage());
            e.printStackTrace();
        }

        // 返回退出码
        System.exit(failCount > 0 ? 1 : 0);
    }

    /**
     * PR_03: 静默ErrorHandler，用于抑制SAX解析器的默认[Fatal Error]输出
     * 不输出任何内容到stderr，错误由我们自己的ValidationErrorHandler处理
     */
    private static class SilentErrorHandler implements org.xml.sax.ErrorHandler {
        /**
         * warning
         *
         * @brief
         * 功能: 执行 warning 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        @Override
        public void warning(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
            // 静默处理，不输出
        }

        /**
         * error
         *
         * @brief
         * 功能: 执行 error 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        @Override
        public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
            // 静默处理，不输出
        }

        /**
         * fatalError
         *
         * @brief
         * 功能: 执行 fatalError 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        @Override
        public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
            // 静默处理，不输出，但需要抛出异常让调用者知道解析失败
            throw e;
        }
    }
}
