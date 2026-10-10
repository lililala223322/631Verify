/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: ReferenceValidator.java
 * 
 * 描述    : 校验XML配置文件中各类引用关系的有效性。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.layers.parsing.XmlParser;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.File;
import java.util.*;

/**
 * @brief
 * 功能: 校验XML配置文件中各类引用关系的有效性。
 */
public class ReferenceValidator {

    /**
     * @brief
     * 功能: 定义单项引用关系校验规则。
     */
    private static class ReferenceRule {
        String description; /* 中文描述 */
        String referenceXpath; /* 引用属性的XPath */
        String targetXpath; /* 被引用属性的XPath */
        boolean allowMissing; /* 是否允许目标不存在（某些特殊情况） */
        String[] excludeValues; /* 排除的值（如"SPARE"） */
        boolean requireBitwiseMatch; /* 是否需要位运算匹配（如健康表StateIdentifier） */

        /**
         * ReferenceRule
         *
         * @brief
         * 功能: 创建普通引用关系规则。
         * @param[in] description 规则描述。
         * @param[in] referenceXpath 引用属性 XPath。
         * @param[in] targetXpath 被引用属性 XPath。
         * @return 无。
         * @scope internal
         */
        ReferenceRule(String description, String referenceXpath, String targetXpath) {
            this.description = description;
            this.referenceXpath = referenceXpath;
            this.targetXpath = targetXpath;
            this.allowMissing = false;
            this.excludeValues = null;
            this.requireBitwiseMatch = false;
        }

        /**
         * ReferenceRule
         *
         * @brief
         * 功能: 创建带排除值的引用关系规则。
         * @param[in] description 规则描述。
         * @param[in] referenceXpath 引用属性 XPath。
         * @param[in] targetXpath 被引用属性 XPath。
         * @param[in] excludeValues 排除值列表。
         * @return 无。
         * @scope internal
         */
        ReferenceRule(String description, String referenceXpath, String targetXpath, String[] excludeValues) {
            this.description = description;
            this.referenceXpath = referenceXpath;
            this.targetXpath = targetXpath;
            this.allowMissing = false;
            this.excludeValues = excludeValues;
            this.requireBitwiseMatch = false;
        }

        /**
         * ReferenceRule
         *
         * @brief
         * 功能: 创建支持位运算匹配的引用关系规则。
         * @param[in] description 规则描述。
         * @param[in] referenceXpath 引用属性 XPath。
         * @param[in] targetXpath 被引用属性 XPath。
         * @param[in] requireBitwiseMatch 是否启用位运算匹配。
         * @return 无。
         * @scope internal
         */
        ReferenceRule(String description, String referenceXpath, String targetXpath, boolean requireBitwiseMatch) {
            this.description = description;
            this.referenceXpath = referenceXpath;
            this.targetXpath = targetXpath;
            this.allowMissing = false;
            this.excludeValues = null;
            this.requireBitwiseMatch = requireBitwiseMatch;
        }
    }

    /**
     * 表75：属性引用关系对照表
     */
    private static final ReferenceRule[] REFERENCE_RULES = {
            /* 1. 应用名称引用 */
            new ReferenceRule(
                    "应用名称引用",
                    "//*[local-name()='Partition']//*[local-name()='Application']/@NameRef",
                    "//*[local-name()='Application']/@Name"),

            /* 2. 共享数据名称引用（NameRef属性） */
            new ReferenceRule(
                    "共享数据名称引用(NameRef)",
                    "//*[local-name()='Partition']//*[local-name()='SharedDataRegion']/@NameRef",
                    "//*[local-name()='SharedData']/@Name"),

            /* 2.1 共享数据名称引用（Name属性）- 兼容实际XML文件中使用Name属性的情况 */
            new ReferenceRule(
                    "共享数据名称引用(Name)",
                    "//*[local-name()='Partition']//*[local-name()='SharedDataRegion']/@Name",
                    "//*[local-name()='SharedData']/@Name"),

            /* 3. 共享IO名称引用（SharedData子元素为SharedIODescription） */
            new ReferenceRule(
                    "共享IO名称引用",
                    "//*[local-name()='SharedData'][*[local-name()='SharedIODescription']]/@Name",
                    "//*[local-name()='sharedIO']/@PoolName"),

            /* 4. 共享库名称引用 */
            new ReferenceRule(
                    "共享库名称引用",
                    "//*[local-name()='Partition']//*[local-name()='SharedLibraryRegion']/@NameRef",
                    "//*[local-name()='SharedLibrary']/@Name"),

            /* 5. 分区名称引用（调度窗口中，SPARE除外） */
            new ReferenceRule(
                    "分区名称引用（调度窗口）",
                    "//*[local-name()='Schedule']//*[local-name()='PartitionWindow']/@PartitionNameRef",
                    "//*[local-name()='Partition']/@Name",
                    new String[] { "SPARE" } /* SPARE分区可以不存在 */
            ),

            /* 6. 通道引用的端口名称（Source） */
            new ReferenceRule(
                    "通道引用的端口名称（Source）",
                    "//*[local-name()='Channel']/*[local-name()='Source']/@PortNameRef | //*[local-name()='Channel']/*[local-name()='Source']/*[local-name()='Standard_Partition']/@PortNameRef",
                    "//*[local-name()='QueuingPort']/@Name | //*[local-name()='SamplingPort']/@Name"),

            /* 7. 通道引用的端口名称（Destination） */
            new ReferenceRule(
                    "通道引用的端口名称（Destination）",
                    "//*[local-name()='Channel']/*[local-name()='Destination']/@PortNameRef | //*[local-name()='Channel']/*[local-name()='Destination']/*[local-name()='Standard_Partition']/@PortNameRef",
                    "//*[local-name()='QueuingPort']/@Name | //*[local-name()='SamplingPort']/@Name"),

            /* 8. 通道引用的分区名称（Source） */
            new ReferenceRule(
                    "通道引用的分区名称（Source）",
                    "//*[local-name()='Channel']/*[local-name()='Source']/@PartitionNameRef | //*[local-name()='Channel']/*[local-name()='Source']/*[local-name()='Standard_Partition']/@PartitionNameRef",
                    "//*[local-name()='Partition']/@Name | //*[local-name()='PseudoPartition']/@Name"),

            /* 9. 通道引用的分区名称（Destination） */
            new ReferenceRule(
                    "通道引用的分区名称（Destination）",
                    "//*[local-name()='Channel']/*[local-name()='Destination']/@PartitionNameRef | //*[local-name()='Channel']/*[local-name()='Destination']/*[local-name()='Standard_Partition']/@PartitionNameRef",
                    "//*[local-name()='Partition']/@Name | //*[local-name()='PseudoPartition']/@Name"),

            /* 10. 日志记录本所在分区名称 */
            new ReferenceRule(
                    "日志记录本所在分区名称",
                    "//*[local-name()='LogBook']/@PartitionNameRef",
                    "//*[local-name()='Partition']/@Name"),

            /* 11. 核心健康表ID号（需要位运算匹配） */
            new ReferenceRule(
                    "核心健康表状态ID引用",
                    "//*[local-name()='CoreHMTable']/@StateIdentifier",
                    "//*[local-name()='CoreHmState']/@StateIdentifier",
                    true /* 需要位运算匹配 */
            ),

            /* 12. 错误ID号引用（CoreHMTable） */
            new ReferenceRule(
                    "核心健康表错误ID引用",
                    "//*[local-name()='CoreHMTable']//*[local-name()='ErrorAction']/@ErrorIdentifierRef",
                    "//*[local-name()='SystemError']/@ErrorIdentifier"),

            /* 13. 错误ID号引用（MultiPartitionHMTable） */
            new ReferenceRule(
                    "多分区健康表错误ID引用",
                    "//*[local-name()='MultiPartitionHMTable']//*[local-name()='ErrorAction']/@ErrorIdentifierRef",
                    "//*[local-name()='SystemError']/@ErrorIdentifier"),

            /* 14. 错误ID号引用（PartitionHMTable） */
            new ReferenceRule(
                    "分区健康表错误ID引用",
                    "//*[local-name()='PartitionHMTable']//*[local-name()='ErrorAction']/@ErrorIdentifierRef",
                    "//*[local-name()='SystemError']/@ErrorIdentifier"),

            /* 15. 分区健康监控表引用多分区健康监控表名称 */
            new ReferenceRule(
                    "分区健康表引用多分区健康表名称",
                    "//*[local-name()='PartitionHMTable']/@MultiPartitionHMTableNameRef",
                    "//*[local-name()='MultiPartitionHMTable']/@TableName"),

            /* 16. 核心健康监控表引用核号 */
            new ReferenceRule(
                    "核心健康表引用核号",
                    "//*[local-name()='CoreHMTable']/*[local-name()='Core']/@cpuId",
                    "//*[local-name()='BmpSchedule']/*[local-name()='Core']/@cpuId"),

            /* 17. 分区健康监控表引用分区ID */
            new ReferenceRule(
                    "分区健康表引用分区ID",
                    "//*[local-name()='PartitionHMTable']/*[local-name()='PartitionRef']/@Id",
                    "//*[local-name()='Partition']/@Identifier"),

            /* 18. 核心健康监控任务引用核号 */
            new ReferenceRule(
                    "核心健康任务引用核号",
                    "//*[local-name()='CoreHMTask']/*[local-name()='Core']/@cpuId",
                    "//*[local-name()='BmpSchedule']/*[local-name()='Core']/@cpuId"),

            /* 19. 分区健康监控任务引用分区ID */
            new ReferenceRule(
                    "分区健康任务引用分区ID",
                    "//*[local-name()='PartitionHMTask']/*[local-name()='PartitionRef']/@Id",
                    "//*[local-name()='Partition']/@Identifier"),

            /* 20. Payload引用的核心名称 */
            new ReferenceRule(
                    "Payload引用核心名称",
                    "//*[local-name()='CoreOSPayload']/@NameRef",
                    "//*[local-name()='CoreOSDescription']/@KernelName"),

            /* 21. Payload引用的配置记录名称 */
            new ReferenceRule(
                    "Payload引用配置记录名称",
                    "//*[local-name()='ConfigRecordPayload']/@NameRef",
                    "//*[local-name()='userConfigRecordRegion']/@Name"),

            /* 22. Payload引用的共享库名称 */
            new ReferenceRule(
                    "Payload引用共享库名称",
                    "//*[local-name()='SharedLibraryPayload']/@NameRef",
                    "//*[local-name()='SharedLibrary']/@Name"),

            /* 23. Payload引用的分区名称 */
            new ReferenceRule(
                    "Payload引用分区名称",
                    "//*[local-name()='PartitionPayload']/@NameRef",
                    "//*[local-name()='Partition']/@Name")
    };

    /* 存储元素位置信息的映射：elementPath -> lineNumber */
    private static Map<String, Integer> elementLineMap = new HashMap<>();

    /**
     * validateReferences
     *
     * @brief
     * 功能: 校验 XML 配置文件中的引用关系有效性。
     * @param[in] xmlFile XML 配置文件。
     * @param[out] errorMessages 错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateReferences(File xmlFile, List<String> errorMessages) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true; /* 文件不存在，跳过验证 */
        }

        List<String> referenceErrors = new ArrayList<>();
        boolean allValid = true;

        try {
            /* 第一步：使用 SAX 解析器建立行号映射 */
            buildLineNumberMap(xmlFile);

            /* 第二步：解析XML文档（启用 XInclude 展开，支持环境变量解析） */
            /* 使用 XmlParser.parseXmlWithEnvSupport 来支持 $(XML_BASE) 等环境变量 */
            Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, true);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 遍历所有引用关系规则 */
            for (ReferenceRule rule : REFERENCE_RULES) {
                try {
                    boolean ruleValid = checkReference(doc, xpath, rule, xmlFile, referenceErrors);
                    if (!ruleValid) {
                        allValid = false;
                    }
                } catch (Exception e) {
                    String elementName = "N/A";
                    String attributeName = "N/A";
                    String refValue = "N/A";
                    String sourceFileName = xmlFile.getName();
                    int lineNumber = -1;
                    try {
                        NodeList nodes = (NodeList) xpath.evaluate(rule.referenceXpath, doc, XPathConstants.NODESET);
                        if (nodes != null && nodes.getLength() > 0) {
                            Node n = nodes.item(0);
                            refValue = getNodeValue(n) != null ? getNodeValue(n).trim() : "N/A";
                            String[] src = getSourceFileInfo(n, xmlFile);
                            sourceFileName = src[0] != null ? src[0] : sourceFileName;
                            try {
                                lineNumber = Integer.parseInt(src[1]);
                            } catch (Exception ex) {
                                lineNumber = -1;
                            }
                            elementName = getElementName(n);
                            attributeName = getAttributeName(n);
                        }
                    } catch (Exception ex) {
                        /* ignore and fallback to N/A values */
                    }

                    String errorMsg = String.format(
                            "[File: %s, Line: %d] the element %s attribute %s value %s of reference relation is error.",
                            sourceFileName, lineNumber, elementName, attributeName, refValue);
                    referenceErrors.add(errorMsg);
                    Log.error(CodeConstant.ERROR_REFERENTIAL_INTEGRITY_ATTRIBUTE_INVALID, errorMsg);
                    allValid = false;
                }
            }

        } catch (Exception e) {
            String errorMsg = String.format(
                    "[File: %s, Line: %d] the element %s attribute %s value %s of reference relation is error.",
                    xmlFile.getName(), -1, "N/A", "N/A", "N/A");
            referenceErrors.add(errorMsg);
            Log.error(CodeConstant.ERROR_REFERENTIAL_INTEGRITY_ATTRIBUTE_INVALID, errorMsg);
            allValid = false;
        }

        /* 将所有引用错误添加到输出错误列表 */
        if (errorMessages != null) {
            errorMessages.addAll(referenceErrors);
        }

        if (allValid) {
            Log.info("Reference validation passed for file: " + xmlFile.getName());
        }

        return allValid;
    }

    /**
     * checkReference
     *
     * @brief
     * 功能: 按指定规则校验一类引用关系。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] rule 引用关系规则。
     * @param[in] xmlFile XML 配置文件。
     * @param[out] errors 错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean checkReference(Document doc, XPath xpath, ReferenceRule rule,
            File xmlFile, List<String> errors) {
        try {
            /* 获取所有引用属性的值 */
            NodeList referenceNodes = (NodeList) xpath.evaluate(rule.referenceXpath, doc, XPathConstants.NODESET);
            if (referenceNodes.getLength() == 0) {
                /* 没有引用属性，跳过检查 */
                return true;
            }

            /* 获取所有被引用属性的值 */
            NodeList targetNodes = (NodeList) xpath.evaluate(rule.targetXpath, doc, XPathConstants.NODESET);
            Set<String> targetValues = new HashSet<>();
            for (int i = 0; i < targetNodes.getLength(); i++) {
                Node node = targetNodes.item(i);
                String value = getNodeValue(node);
                if (value != null && !value.trim().isEmpty()) {
                    targetValues.add(value.trim());
                }
            }

            /* 对于 ConfigRecordPayload，添加特殊值 "configRecord" */
            if (rule.description.contains("配置记录名称")) {
                targetValues.add("configRecord");
            }

            /* PR_XX: 对于 cpuId 引用，单核系统允许 cpuId=0 */
            /* 根据需求：对于单核处理器取值固定为 0，对于多核处理器，则允许的最大值为处理器核数量减 1 */
            /* 当 BmpSchedule 中没有定义 Core 元素时（目标值集合为空），视为单核系统，允许 cpuId=0 */
            if (rule.description.contains("核号") && targetValues.isEmpty()) {
                targetValues.add("0"); /* 单核系统默认允许 cpuId=0 */
            }

            /* 检查每个引用是否有效 */
            boolean valid = true;
            for (int i = 0; i < referenceNodes.getLength(); i++) {
                Node refNode = referenceNodes.item(i);
                String refValue = getNodeValue(refNode);

                if (refValue == null || refValue.trim().isEmpty()) {
                    continue;
                }

                refValue = refValue.trim();

                /* 检查是否在排除列表中 */
                if (rule.excludeValues != null) {
                    boolean excluded = false;
                    for (String excludeValue : rule.excludeValues) {
                        if (refValue.equals(excludeValue)) {
                            excluded = true;
                            break;
                        }
                    }
                    if (excluded) {
                        continue;
                    }
                }

                /* 检查引用是否有效 */
                boolean isValid;
                if (rule.description.contains("核号") || rule.description.contains("分区ID")) {
                    try {
                        parseDecOrHex(refValue);
                    } catch (NumberFormatException nfe) {
                        valid = false;
                        continue;
                    }
                }

                if (rule.requireBitwiseMatch) {
                    /* 位运算匹配（用于StateIdentifier） */
                    isValid = checkBitwiseMatch(refValue, targetValues);
                } else {
                    /* 直接匹配 */
                    isValid = targetValues.contains(refValue);
                }

                if (!isValid) {
                    /* 提取元素名和属性名 */
                    String elementName = getElementName(refNode);
                    String attributeName = getAttributeName(refNode);

                    /* 获取元素的源文件信息（处理XInclude的情况） */
                    String[] sourceInfo = getSourceFileInfo(refNode, xmlFile);
                    String sourceFileName = sourceInfo[0];
                    int lineNumber = Integer.parseInt(sourceInfo[1]);

                    /* 格式化错误信息 */
                    String errorMsg = String.format(
                            "[File: %s, Line: %d] the element %s attribute %s value %s of reference relation is error.",
                            sourceFileName, lineNumber, elementName, attributeName, refValue);
                    errors.add(errorMsg);
                    Log.error(CodeConstant.ERROR_REFERENTIAL_INTEGRITY_ATTRIBUTE_INVALID, errorMsg);
                    valid = false;
                }
            }

            return valid;

        } catch (Exception e) {
            String elementName = "N/A";
            String attributeName = "N/A";
            String refValue = "N/A";
            String sourceFileName = xmlFile.getName();
            int lineNumber = -1;
            try {
                /* 尝试定位第一个引用节点以获得更精确的位置信息 */
                NodeList nodes = (NodeList) xpath.evaluate(rule.referenceXpath, doc, XPathConstants.NODESET);
                if (nodes != null && nodes.getLength() > 0) {
                    Node n = nodes.item(0);
                    refValue = getNodeValue(n) != null ? getNodeValue(n).trim() : "N/A";
                    String[] src = getSourceFileInfo(n, xmlFile);
                    sourceFileName = src[0] != null ? src[0] : sourceFileName;
                    try {
                        lineNumber = Integer.parseInt(src[1]);
                    } catch (Exception ex) {
                        lineNumber = -1;
                    }
                    elementName = getElementName(n);
                    attributeName = getAttributeName(n);
                }
            } catch (Exception ex) {
                /* 忽略定位失败，使用回退值 */
            }

            String errorMsg = String.format(
                    "[File: %s, Line: %d] the element %s attribute %s value %s of reference relation is error.",
                    sourceFileName, lineNumber, elementName, attributeName, refValue);
            if (errors != null) {
                errors.add(errorMsg);
            }
            Log.error(CodeConstant.ERROR_REFERENTIAL_INTEGRITY_ATTRIBUTE_INVALID, errorMsg);
            return false;
        }
    }

    /**
     * checkBitwiseMatch
     *
     * @brief
     * 功能: 检查引用值是否能与目标值集合进行位运算匹配。
     * @param[in] refValue 引用值。
     * @param[in] targetValues 目标值集合。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean checkBitwiseMatch(String refValue, Set<String> targetValues) {
        try {
            /* 解析引用值（支持十进制和十六进制） */
            long refNum = parseDecOrHex(refValue);

            /* 如果直接字符串匹配，返回true */
            if (targetValues.contains(refValue)) {
                return true;
            }

            /* 检查是否可以通过位或运算得到 */
            /* 收集所有目标值（支持十进制和十六进制） */
            List<Long> targetNums = new ArrayList<>();
            for (String target : targetValues) {
                try {
                    long targetNum = parseDecOrHex(target);
                    targetNums.add(targetNum);

                    /* 数值相等也视为匹配（支持 "1" 匹配 "0x1"） */
                    if (targetNum == refNum) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    /* 忽略非数字值 */
                }
            }

            /* CoreHMTable/@StateIdentifier 可以包含 CoreHmState/@StateIdentifier 的某一位。 */
            /* 只要 tableStateIdentifier & stateId == stateId，就认为该状态被引用。 */
            for (long targetNum : targetNums) {
                if (targetNum != 0 && (refNum & targetNum) == targetNum) {
                    return true;
                }
            }

            return false;

        } catch (NumberFormatException e) {
            /* 如果不是数字，使用直接匹配 */
            return targetValues.contains(refValue);
        }
    }

    /**
     * parseDecOrHex
     *
     * @brief
     * 功能: 解析十进制或十六进制数值。
     * @param[in] value 数值字符串。
     * @return 解析后的长整型数值。
     * @scope internal
     */
    private static long parseDecOrHex(String value) throws NumberFormatException {
        if (value == null || value.trim().isEmpty()) {
            throw new NumberFormatException("Empty value");
        }

        String trimmed = value.trim();

        /* 处理十六进制格式 (0x 或 0X 开头) */
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            return Long.parseLong(trimmed.substring(2), 16);
        }

        /* 处理十进制格式 */
        return Long.parseLong(trimmed);
    }

    /**
     * getNodeValue
     *
     * @brief
     * 功能: 获取节点值。
     * @param[in] node DOM 节点。
     * @return 字符串结果。
     * @scope internal
     */
    private static String getNodeValue(Node node) {
        if (node instanceof Attr) {
            return ((Attr) node).getValue();
        } else if (node instanceof Text) {
            return node.getNodeValue();
        } else if (node instanceof Element) {
            return node.getTextContent();
        }
        return null;
    }

    /**
     * getElementName
     *
     * @brief
     * 功能: 获取节点所属元素名称。
     * @param[in] node DOM 节点。
     * @return 字符串结果。
     * @scope internal
     */
    private static String getElementName(Node node) {
        if (node instanceof Attr) {
            Attr attr = (Attr) node;
            Element element = attr.getOwnerElement();
            return element.getLocalName();
        } else if (node instanceof Element) {
            return ((Element) node).getLocalName();
        }
        return "unknown";
    }

    /**
     * getAttributeName
     *
     * @brief
     * 功能: 获取属性节点名称。
     * @param[in] node DOM 节点。
     * @return 字符串结果。
     * @scope internal
     */
    private static String getAttributeName(Node node) {
        if (node instanceof Attr) {
            return ((Attr) node).getLocalName();
        }
        return "unknown";
    }



    /**
     * buildLineNumberMap
     *
     * @brief
     * 功能: 构建 XML 元素行号映射。
     * @param[in] xmlFile XML 配置文件。
     * @return 无。
     * @scope internal
     */
    private static void buildLineNumberMap(File xmlFile) {
        elementLineMap.clear();
        try {
            javax.xml.parsers.SAXParserFactory saxFactory = javax.xml.parsers.SAXParserFactory.newInstance();
            saxFactory.setNamespaceAware(true);
            javax.xml.parsers.SAXParser saxParser = saxFactory.newSAXParser();

            org.xml.sax.helpers.DefaultHandler handler = new org.xml.sax.helpers.DefaultHandler() {
                private org.xml.sax.Locator locator;
                private List<String> elementStack = new ArrayList<>();
                private Map<String, Integer> elementCounters = new HashMap<>(); /* 记录每个路径下各类型元素的计数 */

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
                    if (locator != null) {
                        int lineNumber = locator.getLineNumber();

                        /* 对于关键元素，使用 Name/Identifier 作为唯一标识 */
                        String identifier = null;
                        if (localName.equals("Application") || localName.equals("Partition") ||
                                localName.equals("PseudoPartition") || localName.equals("SharedLibrary") ||
                                localName.equals("SharedData") || localName.equals("Schedule")) {
                            identifier = attributes.getValue("Name");
                            if (identifier == null || identifier.isEmpty()) {
                                identifier = attributes.getValue("Identifier");
                            }
                        }

                        /* 构建元素路径（使用位置索引来区分相同元素） */
                        String elementPath;
                        if (identifier != null && !identifier.isEmpty()) {
                            elementPath = localName + "[@" + identifier + "]";
                        } else {
                            /* 使用位置计数器 */
                            String currentPath = String.join("/", elementStack);
                            String counterKey = currentPath + "/" + localName;
                            int position = elementCounters.getOrDefault(counterKey, 0) + 1;
                            elementCounters.put(counterKey, position);
                            elementPath = localName + "[" + position + "]";
                        }

                        elementStack.add(elementPath);

                        /* 记录完整路径和行号 */
                        String fullPath = String.join("/", elementStack);
                        elementLineMap.put(fullPath, lineNumber);

                        /* 同时记录简化路径（仅元素名，用于向上查找） */
                        elementLineMap.put(localName, lineNumber);
                    }
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
                        elementStack.remove(elementStack.size() - 1);
                    }
                }
            };

            saxParser.parse(xmlFile, handler);
        } catch (Exception e) {
            /* 如果SAX解析失败，映射为空，后续将使用-1作为行号 */
            Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                    "Failed to build line number map: " + e.getMessage());
        }
    }

    /**
     * getSourceFileInfo
     *
     * @brief
     * 功能: 获取节点来源文件与行号信息。
     * @param[in] node DOM 节点。
     * @param[in] mainXmlFile 主 XML 文件。
     * @return 文件名与行号信息。
     * @scope internal
     */
    private static String[] getSourceFileInfo(Node node, File mainXmlFile) {
        Element element = null;
        if (node instanceof Attr) {
            element = ((Attr) node).getOwnerElement();
        } else if (node instanceof Element) {
            element = (Element) node;
        }

        if (element == null) {
            return new String[] { mainXmlFile.getName(), "-1" };
        }

        /* 查找xml:base属性（XInclude展开后会添加此属性） */
        String xmlBase = findXmlBase(element);

        if (xmlBase != null && !xmlBase.isEmpty()) {
            /* 从xml:base中提取文件名 */
            String sourceFileName = extractFileName(xmlBase);

            /* 获取源文件并计算行号 */
            File sourceFile = resolveSourceFile(mainXmlFile, xmlBase);
            if (sourceFile != null && sourceFile.exists()) {
                int lineNumber = getLineNumberFromSourceFile(element, sourceFile);
                return new String[] { sourceFileName, String.valueOf(lineNumber) };
            }

            return new String[] { sourceFileName, "-1" };
        }

        /* 没有xml:base，使用主文件 */
        int lineNumber = getElementLineNumber(element);
        return new String[] { mainXmlFile.getName(), String.valueOf(lineNumber) };
    }

    /**
     * findXmlBase
     *
     * @brief
     * 功能: 向上查找元素的 xml:base 属性。
     * @param[in] element XML 元素。
     * @return 字符串结果。
     * @scope internal
     */
    private static String findXmlBase(Element element) {
        Node current = element;
        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            Element elem = (Element) current;
            String xmlBase = elem.getAttributeNS("http://www.w3.org/XML/1998/namespace", "base");
            if (xmlBase != null && !xmlBase.isEmpty()) {
                return xmlBase;
            }
            /* 也检查不带命名空间的xml:base */
            xmlBase = elem.getAttribute("xml:base");
            if (xmlBase != null && !xmlBase.isEmpty()) {
                return xmlBase;
            }
            current = current.getParentNode();
        }
        return null;
    }

    /**
     * extractFileName
     *
     * @brief
     * 功能: 从路径中提取文件名。
     * @param[in] path 路径字符串。
     * @return 字符串结果。
     * @scope internal
     */
    private static String extractFileName(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        /* 处理URL和文件路径 */
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }

    /**
     * resolveSourceFile
     *
     * @brief
     * 功能: 根据主文件和 xml:base 解析来源文件。
     * @param[in] mainXmlFile 主 XML 文件。
     * @param[in] xmlBase xml:base 路径。
     * @return 字符串结果。
     * @scope internal
     */
    private static File resolveSourceFile(File mainXmlFile, String xmlBase) {
        try {
            /* 相对于主文件的目录解析 */
            File parentDir = mainXmlFile.getParentFile();
            if (parentDir != null) {
                return new File(parentDir, extractFileName(xmlBase));
            }
        } catch (Exception e) {
            /* 忽略 */
        }
        return null;
    }

    /**
     * getLineNumberFromSourceFile
     *
     * @brief
     * 功能: 从来源文件中定位元素行号。
     * @param[in] element XML 元素。
     * @param[in] sourceFile 来源 XML 文件。
     * @return 元素行号。
     * @scope internal
     */
    private static int getLineNumberFromSourceFile(Element element, File sourceFile) {
        /* 使用元素的本地名称和属性值来定位 */
        String elementName = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();

        /* 获取元素的关键属性用于匹配 */
        String nameAttr = element.getAttribute("Name");
        String nameRefAttr = element.getAttribute("NameRef");
        String identifierAttr = element.getAttribute("Identifier");

        try {
            /* 使用SAX解析源文件来获取行号 */
            final int[] foundLine = { -1 };

            javax.xml.parsers.SAXParserFactory saxFactory = javax.xml.parsers.SAXParserFactory.newInstance();
            saxFactory.setNamespaceAware(true);
            javax.xml.parsers.SAXParser saxParser = saxFactory.newSAXParser();

            final String targetElementName = elementName;
            final String targetName = nameAttr;
            final String targetNameRef = nameRefAttr;

            org.xml.sax.helpers.DefaultHandler handler = new org.xml.sax.helpers.DefaultHandler() {
                private org.xml.sax.Locator locator;

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
                    if (foundLine[0] > 0)
                        return; /* 已找到 */

                    if (localName.equals(targetElementName) || qName.endsWith(":" + targetElementName)
                            || qName.equals(targetElementName)) {
                        /* 检查属性是否匹配 */
                        String attrName = attributes.getValue("Name");
                        String attrNameRef = attributes.getValue("NameRef");

                        boolean matched = false;
                        if (targetName != null && !targetName.isEmpty() && targetName.equals(attrName)) {
                            matched = true;
                        } else if (targetNameRef != null && !targetNameRef.isEmpty()
                                && targetNameRef.equals(attrNameRef)) {
                            matched = true;
                        } else if ((targetName == null || targetName.isEmpty())
                                && (targetNameRef == null || targetNameRef.isEmpty())) {
                            /* 如果目标没有Name/NameRef属性，按元素名匹配 */
                            matched = true;
                        }

                        if (matched && locator != null) {
                            foundLine[0] = locator.getLineNumber();
                        }
                    }
                }
            };

            saxParser.parse(sourceFile, handler);
            return foundLine[0];

        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * getLineNumber
     *
     * @brief
     * 功能: 获取节点在 XML 文件中的行号。
     * @param[in] node DOM 节点。
     * @param[in] xmlFile XML 配置文件。
     * @return 元素行号。
     * @scope internal
     */
    private static int getLineNumber(Node node, File xmlFile) {
        if (node instanceof Attr) {
            Element element = ((Attr) node).getOwnerElement();
            return getElementLineNumber(element);
        } else if (node instanceof Element) {
            return getElementLineNumber((Element) node);
        }
        return -1;
    }

    /**
     * getElementLineNumber
     *
     * @brief
     * 功能: 根据元素映射获取行号。
     * @param[in] element XML 元素。
     * @return 元素行号。
     * @scope internal
     */
    private static int getElementLineNumber(Element element) {
        /* 构建元素路径（从根到当前元素） */
        List<Element> pathElements = new ArrayList<>();
        Node current = element;
        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            pathElements.add(0, (Element) current);
            current = current.getParentNode();
        }

        /* 构建完整路径 */
        List<String> pathParts = new ArrayList<>();
        for (Element elem : pathElements) {
            String nodeName = elem.getLocalName() != null ? elem.getLocalName() : elem.getNodeName();

            /* 对于关键元素，使用 Name/Identifier */
            String identifier = null;
            if (nodeName.equals("Application") || nodeName.equals("Partition") ||
                    nodeName.equals("PseudoPartition") || nodeName.equals("SharedLibrary") ||
                    nodeName.equals("SharedData") || nodeName.equals("Schedule")) {
                identifier = elem.getAttribute("Name");
                if (identifier == null || identifier.isEmpty()) {
                    identifier = elem.getAttribute("Identifier");
                }
            }

            if (identifier != null && !identifier.isEmpty()) {
                pathParts.add(nodeName + "[@" + identifier + "]");
            } else {
                /* 计算元素在同类型兄弟节点中的位置 */
                int position = calculateElementPosition(elem);
                pathParts.add(nodeName + "[" + position + "]");
            }
        }

        String fullPath = String.join("/", pathParts);

        /* 精确匹配 */
        Integer lineNumber = elementLineMap.get(fullPath);
        if (lineNumber != null) {
            return lineNumber;
        }

        /* 尝试向上查找父元素 */
        for (int i = pathParts.size() - 1; i >= 0; i--) {
            String partialPath = String.join("/", pathParts.subList(0, i + 1));
            lineNumber = elementLineMap.get(partialPath);
            if (lineNumber != null) {
                return lineNumber;
            }
        }

        /* 最后尝试仅使用元素名 */
        String elementName = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
        lineNumber = elementLineMap.get(elementName);
        return lineNumber != null ? lineNumber : -1;
    }

    /**
     * calculateElementPosition
     *
     * @brief
     * 功能: 计算元素在同类节点中的位置。
     * @param[in] element XML 元素。
     * @return 元素行号。
     * @scope internal
     */
    private static int calculateElementPosition(Element element) {
        String elementName = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
        Node parent = element.getParentNode();

        if (parent == null) {
            return 1;
        }

        int position = 0;
        NodeList siblings = parent.getChildNodes();
        for (int i = 0; i < siblings.getLength(); i++) {
            Node sibling = siblings.item(i);
            if (sibling.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element siblingElement = (Element) sibling;
            String siblingName = siblingElement.getLocalName() != null ? siblingElement.getLocalName()
                    : siblingElement.getNodeName();

            if (siblingName.equals(elementName)) {
                position++;
                if (sibling == element) {
                    return position;
                }
            }
        }

        return 1; /* 默认返回1 */
    }
}
