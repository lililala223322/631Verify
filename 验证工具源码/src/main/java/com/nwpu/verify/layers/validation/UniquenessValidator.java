/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: UniquenessValidator.java
 * 
 * 描述    : 校验XML配置文件中指定属性的唯一性约束。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-08-25  开发人员  按问题报告单TR_PR_004（004904）调整3001输出格式为无[Error]前缀、
 *                 错误码后接全角冒号，并按需求模板同步消息体占位符间双空格。
 */

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.layers.parsing.XmlParser;
import org.w3c.dom.*;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.File;
import java.util.*;

/**
 * @brief
 * 功能: 校验XML配置文件中指定属性的唯一性约束。
 */
public class UniquenessValidator {

    /**
     * @brief
     * 功能: 定义单项唯一性校验规则。
     */
    private static class UniquenessRule {
        String description; /* 中文描述 */
        String[] xpaths; /* XPath表达式数组（某些属性可能有多个路径） */
        String scopeXpath; /* 作用域XPath（用于分组检查，如端口需要按应用/分区分组） */

        /**
         * UniquenessRule
         *
         * @brief
         * 功能: 创建单 XPath 唯一性规则。
         * @param[in] description 规则描述。
         * @param[in] xpath XPath 查询对象或表达式。
         * @return 无。
         * @scope internal
         */
        UniquenessRule(String description, String xpath) {
            this.description = description;
            this.xpaths = new String[] { xpath };
            this.scopeXpath = null;
        }

        /**
         * UniquenessRule
         *
         * @brief
         * 功能: 创建多 XPath 唯一性规则。
         * @param[in] description 规则描述。
         * @param[in] xpaths XPath 表达式数组。
         * @return 无。
         * @scope internal
         */
        UniquenessRule(String description, String[] xpaths) {
            this.description = description;
            this.xpaths = xpaths;
            this.scopeXpath = null;
        }

        /**
         * UniquenessRule
         *
         * @brief
         * 功能: 创建带作用域的唯一性规则。
         * @param[in] description 规则描述。
         * @param[in] xpath XPath 查询对象或表达式。
         * @param[in] scopeXpath 作用域 XPath。
         * @return 无。
         * @scope internal
         */
        UniquenessRule(String description, String xpath, String scopeXpath) {
            this.description = description;
            this.xpaths = new String[] { xpath };
            this.scopeXpath = scopeXpath;
        }
    }

    /**
     * 表74：唯一性约束属性表
     */
    private static final UniquenessRule[] UNIQUENESS_RULES = {
            /* 1. 应用名称 */
            new UniquenessRule(
                    "应用名称",
                    "//*[local-name()='Application']/@Name"),

            /* 2. 共享数据名称 */
            new UniquenessRule(
                    "共享数据名称",
                    "//*[local-name()='SharedData']/@Name"),

            /* 3. 共享库名称 */
            new UniquenessRule(
                    "共享库名称",
                    "//*[local-name()='SharedLibrary']/@Name"),

            /* 4. 分区名称 */
            new UniquenessRule(
                    "分区名称",
                    "//*[local-name()='Partition']/@Name"),

            /* 5. 分区ID */
            new UniquenessRule(
                    "分区ID",
                    "//*[local-name()='Partition']/@Identifier"),

            /* 6. 虚分区名称 */
            new UniquenessRule(
                    "虚分区名称",
                    "//*[local-name()='PseudoPartition']/@Name"),

            /* 7. 虚分区ID */
            new UniquenessRule(
                    "虚分区ID",
                    "//*[local-name()='PseudoPartition']/@Identifier"),

            /* 8. 分区队列端口名称（需要在每个应用内检查唯一性） */
            new UniquenessRule(
                    "分区队列端口名称",
                    "//*[local-name()='Application']//*[local-name()='QueuingPort']/@Name",
                    "//*[local-name()='Application']"),

            /* 9. 分区采样端口名称（需要在每个应用内检查唯一性） */
            new UniquenessRule(
                    "分区采样端口名称",
                    "//*[local-name()='Application']//*[local-name()='SamplingPort']/@Name",
                    "//*[local-name()='Application']"),

            /* 10. 虚分区队列端口名称（需要在每个虚分区内检查唯一性） */
            new UniquenessRule(
                    "虚分区队列端口名称",
                    "//*[local-name()='PseudoPartition']//*[local-name()='QueuingPort']/@Name",
                    "//*[local-name()='PseudoPartition']"),

            /* 11. 虚分区采样端口名称（需要在每个虚分区内检查唯一性） */
            new UniquenessRule(
                    "虚分区采样端口名称",
                    "//*[local-name()='PseudoPartition']//*[local-name()='SamplingPort']/@Name",
                    "//*[local-name()='PseudoPartition']"),

            /* 12. 通道ID号 */
            new UniquenessRule(
                    "通道ID号",
                    "//*[local-name()='Channel']/@Id"),

            /* 13. 调度名称 */
            /* 根据表74有两种结构： */
            /* (1) Module/Schedules/Schedule/@ScheduleName - 普通单核调度，全局唯一 */
            /* (2) Module/Schedules/BmpSchedule/Core/Schedule/@ScheduleName - */
            /* BMP多核调度，每个Core内唯一 */
            /* 这里使用Schedules作为作用域，检查其下所有Schedule的唯一性 */
            new UniquenessRule(
                    "调度名称",
                    "//*[local-name()='Schedules']//*[local-name()='Schedule']/@ScheduleName",
                    "//*[local-name()='Schedules']"),

            /* 14. 调度表ID */
            /* 根据表74有两种结构： */
            /* (1) /Module/Schedules/Schedule/@ScheduleIdentifier - 普通单核调度，全局唯一 */
            /* (2) /Module/Schedules/BmpSchedule/Core/Schedule/@ScheduleIdentifier - */
            /* BMP多核调度，每个Core内唯一 */
            /* 这里使用Schedules作为作用域，检查其下所有Schedule的唯一性 */
            new UniquenessRule(
                    "调度表ID",
                    "//*[local-name()='Schedules']//*[local-name()='Schedule']/@ScheduleIdentifier",
                    "//*[local-name()='Schedules']"),

            /* 15. CPU核号 */
            new UniquenessRule(
                    "CPU核号",
                    "//*[local-name()='BmpSchedule']//*[local-name()='Core']/@cpuId"),

            /* 16. 系统故障类型 */
            new UniquenessRule(
                    "系统故障类型",
                    "//*[local-name()='SystemError']/@ErrorIdentifier"),

            /* 17. 系统状态类型 */
            new UniquenessRule(
                    "系统状态类型",
                    "//*[local-name()='CoreHmState']/@StateIdentifier"),

            /* 18. 核心健康表名称 */
            new UniquenessRule(
                    "核心健康表名称",
                    "//*[local-name()='CoreHMTable']/@Name"),

            /* 19. 多分区健康表ID号 */
            new UniquenessRule(
                    "多分区健康表ID号",
                    "//*[local-name()='MultiPartitionHMTable']/@TableIdentifier"),

            /* 20. 多分区健康表名称 */
            new UniquenessRule(
                    "多分区健康表名称",
                    "//*[local-name()='MultiPartitionHMTable']/@TableName"),

            /* 21. 分区健康表名称 */
            new UniquenessRule(
                    "分区健康表名称",
                    "//*[local-name()='PartitionHMTable']/@TableName"),

            /* 22. 分区健康表ID号 */
            new UniquenessRule(
                    "分区健康表ID号",
                    "//*[local-name()='PartitionHMTable']/@TableIdentifier"),

            /* ============ 表73扩展：唯一性约束（问题报告单6） ============ */
            /* 语义：表中XPath下的属性只能赋值一次。 */
            /* - 元素单例（元素在其父元素下只能出现一次）由 SINGLETON_RULES 检验 */
            /* - 可多实例元素（kernelIO/sharedIO/kernelRegion）按 PoolName 区分实例， */
            /*   PoolName 值唯一防止实例歧义 */
            /* 注：不做跨元素类型的地址值全局唯一检验——部署地址/IO地址/内核与共享 */
            /* IO虚拟地址分属不同用途空间，正常数据中允许值相同（0430数据集实证） */

            /* 23. 内核IO池名称唯一性（kernelIO 可多实例，按 PoolName 区分，PoolName 值唯一） */
            new UniquenessRule(
                    "内核IO池名称",
                    "//*[local-name()='kernelIO']/@PoolName"),

            /* 24. 共享IO池名称唯一性（sharedIO 可多实例，按 PoolName 区分） */
            new UniquenessRule(
                    "共享IO池名称",
                    "//*[local-name()='sharedIO']/@PoolName"),

            /* 25. 内核内存池名称唯一性（kernelRegion 可多实例，按 PoolName 区分） */
            new UniquenessRule(
                    "内核内存池名称",
                    "//*[local-name()='kernelRegion']/@PoolName"),

            /* 26. 分区引用共享数据名称（需要在每个分区内检查唯一性） */
            new UniquenessRule(
                    "分区引用共享数据名称",
                    "//*[local-name()='PartitionDescription']/*[local-name()='SharedDataRegion']/@Name",
                    "//*[local-name()='PartitionDescription']"),

            /* 27. 分区引用共享数据地址（需要在每个分区内检查唯一性） */
            new UniquenessRule(
                    "分区引用共享数据地址",
                    "//*[local-name()='PartitionDescription']/*[local-name()='SharedDataRegion']/@Address",
                    "//*[local-name()='PartitionDescription']")
    };

    /**
     * @brief
     * 功能: 定义元素单例唯一性校验规则（表73扩展）。
     * 表中 XPath 对应的元素在其父元素下只能出现一次（属性只能赋值一次），
     * 重复出现时其属性被赋值多次，违反唯一性约束，报错误码 3001。
     */
    private static class SingletonRule {
        String description; /* 中文描述 */
        String elementXpath; /* 元素 XPath（选中元素本身） */
        String reportAttribute; /* 报错时引用的属性名 */

        SingletonRule(String description, String elementXpath, String reportAttribute) {
            this.description = description;
            this.elementXpath = elementXpath;
            this.reportAttribute = reportAttribute;
        }
    }

    /**
     * 表73扩展：元素单例唯一性规则。
     * 覆盖 XSD 的 xs:choice maxOccurs="unbounded" 容器内、业务上只能定义一次的元素。
     */
    private static final SingletonRule[] SINGLETON_RULES = {
            /* 核心操作系统物理内存（HardwareConfiguration 下只能定义一次） */
            new SingletonRule("核心操作系统物理内存",
                    "//*[local-name()='PhysicalMemory']", "Base_Address"),

            /* 用户内存空间（HardwareConfiguration 下只能定义一次） */
            new SingletonRule("用户内存空间",
                    "//*[local-name()='userMemoryRegion']", "Base_Address"),

            /* ROM部署空间（HardwareConfiguration 下只能定义一次） */
            new SingletonRule("ROM部署空间",
                    "//*[local-name()='payloadMemory']", "Base_Address"),

            /* 内核内存空间（PhysicalMemory 下只能定义一次） */
            new SingletonRule("内核内存空间",
                    "//*[local-name()='kernelMemoryRegion']", "Size"),

            /* 内核配置记录空间（PhysicalMemory 下只能定义一次） */
            new SingletonRule("内核配置记录空间",
                    "//*[local-name()='kernelConfigRecordRegion']", "Size"),

            /* 内核页池（PhysicalMemory 下只能定义一次） */
            new SingletonRule("内核页池",
                    "//*[local-name()='kernelPgPool']", "Size"),

            /* 端口缓冲区空间（PhysicalMemory 下只能定义一次） */
            new SingletonRule("端口缓冲区空间",
                    "//*[local-name()='portRegion']", "Size"),

            /* 健康监控日志记录空间（PhysicalMemory 下只能定义一次） */
            new SingletonRule("健康监控日志记录空间",
                    "//*[local-name()='hmLogRegion']", "Size"),

            /* RAM部署空间（PhysicalMemory 下只能定义一次） */
            new SingletonRule("RAM部署空间",
                    "//*[local-name()='ramPayloadRegion']", "Base_Address"),

            /* 共享库空间（PhysicalMemory 下只能定义一次） */
            new SingletonRule("共享库空间",
                    "//*[local-name()='libRegion']", "Base_Address"),

            /* 用户配置记录空间（PhysicalMemory 下只能定义一次） */
            new SingletonRule("用户配置记录空间",
                    "//*[local-name()='userConfigRecordRegion']", "Base_Address")
    };

    /**
     * validateUniqueness
     *
     * @brief
     * 功能: 校验 XML 配置文件中指定属性的唯一性约束。
     * @param[in] xmlFile XML 配置文件。
     * @param[out] errorMessages 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateUniqueness(File xmlFile, List<String> errorMessages) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true; /* 文件不存在，跳过验证 */
        }

        List<String> uniquenessErrors = new ArrayList<>();
        boolean allValid = true;

        try {
            /* Stage 1: 收集所有被 XInclude 引用的文件 */
            Document docNoXInclude = XmlParser.parseXmlWithEnvSupport(xmlFile, false);

            List<File> allFiles = new ArrayList<>();
            allFiles.add(xmlFile);
            collectIncludedFiles(docNoXInclude, xmlFile, allFiles);

            /* Stage 2: 使用 SAX 构建行号映射（包含所有文件） */
            Map<String, Integer> lineNumberMap = new HashMap<>();
            for (File file : allFiles) {
                Map<String, Integer> fileMap = buildLineNumberMap(file);
                lineNumberMap.putAll(fileMap);
            }

            /* Stage 3: 使用支持环境变量的XML解析（启用 XInclude 展开，以便检查跨文件的唯一性） */
            Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, true);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 遍历所有唯一性规则 */
            for (UniquenessRule rule : UNIQUENESS_RULES) {
                boolean ruleValid;
                if (rule.scopeXpath != null) {
                    /* 有作用域限制的检查（如端口名称需要在各自的应用/分区内唯一） */
                    ruleValid = checkUniquenessWithScope(doc, xpath, rule, xmlFile, lineNumberMap, uniquenessErrors);
                } else {
                    /* 全局唯一性检查 */
                    ruleValid = checkGlobalUniqueness(doc, xpath, rule, xmlFile, lineNumberMap, uniquenessErrors);
                }

                if (!ruleValid) {
                    allValid = false;
                }
            }

            /* 表73扩展：元素单例唯一性检查（元素在父元素下只能出现一次， */
            /* 重复出现时其属性被赋值多次，违反唯一性约束） */
            for (SingletonRule rule : SINGLETON_RULES) {
                boolean ruleValid = checkElementSingleton(doc, xpath, rule, xmlFile, lineNumberMap,
                        uniquenessErrors);
                if (!ruleValid) {
                    allValid = false;
                }
            }

        } catch (Exception e) {
            /* XML 解析失败，使用错误码 1017 */
            String reason = formatXmlErrorMessage(e);
            String errorMsg = "XML file " + xmlFile.getName() + " parsing failed. the reason : " + reason + ".";
            uniquenessErrors.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            /* 错误码1017: XML解析失败时立即终止程序，不输出log和report */
            com.nwpu.verify.Application.exitWithErrorCode(CodeConstant.ERROR_XML_PARSE_FAILED);
            allValid = false;
        }

        /* 将所有唯一性错误添加到输出错误列表 */
        if (errorMessages != null) {
            errorMessages.addAll(uniquenessErrors);
        }

        if (allValid) {
            Log.info("Uniqueness validation passed for file: " + xmlFile.getName());
        }

        return allValid;
    }

    /**
     * checkElementSingleton
     *
     * @brief
     * 功能: 执行元素单例唯一性检查（表73扩展）。
     * 规则对应的元素在其父元素下只能出现一次（属性只能赋值一次），
     * 同一父元素下出现多个同名元素时，其属性被赋值多次，报错误码 3001。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象或表达式。
     * @param[in] rule 元素单例唯一性规则。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errors 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean checkElementSingleton(Document doc, XPath xpath, SingletonRule rule,
            File xmlFile, Map<String, Integer> lineNumberMap,
            List<String> errors) {
        try {
            NodeList nodes = (NodeList) xpath.evaluate(rule.elementXpath, doc, XPathConstants.NODESET);

            /* 按父元素分组统计同名元素出现次数 */
            Map<Node, List<Element>> parentChildrenMap = new LinkedHashMap<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                Node parent = element.getParentNode();
                parentChildrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(element);
            }

            boolean valid = true;

            /* 同一父元素下出现多于一个同名元素时，对每个重复位置（从第二个开始）输出错误 */
            for (List<Element> siblings : parentChildrenMap.values()) {
                if (siblings.size() > 1) {
                    for (int i = 1; i < siblings.size(); i++) {
                        Element element = siblings.get(i);
                        String elementName = element.getLocalName();
                        String value = element.getAttribute(rule.reportAttribute);
                        if (value == null || value.trim().isEmpty()) {
                            value = "(undefined)";
                        }

                        /* 获取行号和文件名 */
                        int lineNumber = getLineNumber(element, lineNumberMap);
                        String fileName = getFileNameFromElement(element, xmlFile);

                        String errorMsg = String.format(
                                "[File: %s, Line: %d] the element %s  attribute  %s  value %s is not unique.",
                                fileName, lineNumber, elementName, rule.reportAttribute, value);

                        errors.add(errorMsg);
                        Log.consistencyError(CodeConstant.ERROR_UNIQUE_CONSTRAINT_ATTRIBUTE_NOT_UNIQUE, errorMsg);
                        valid = false;
                    }
                }
            }

            return valid;

        } catch (Exception e) {
            Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                    "Failed to check element singleton for: " + rule.description + " - " + e.getMessage());
            return true; /* 检验异常时不阻断其他规则 */
        }
    }

    /**
     * checkGlobalUniqueness
     *
     * @brief
     * 功能: 执行全局唯一性检查。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象或表达式。
     * @param[in] rule 唯一性规则。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errors 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean checkGlobalUniqueness(Document doc, XPath xpath, UniquenessRule rule,
            File xmlFile, Map<String, Integer> lineNumberMap,
            List<String> errors) {
        try {
            /* 记录每个值第一次和后续出现的元素 */
            Map<String, List<Element>> valueElementMap = new HashMap<>();

            /* 遍历所有XPath表达式 */
            for (String xpathExpr : rule.xpaths) {
                try {
                    NodeList nodes = (NodeList) xpath.evaluate(xpathExpr, doc, XPathConstants.NODESET);

                    for (int i = 0; i < nodes.getLength(); i++) {
                        Attr attr = (Attr) nodes.item(i);
                        String value = attr.getNodeValue();

                        if (value != null && !value.trim().isEmpty()) {
                            Element parentElement = attr.getOwnerElement();
                            valueElementMap.computeIfAbsent(value, k -> new ArrayList<>()).add(parentElement);
                        }
                    }
                } catch (Exception e) {
                    /* 某个XPath表达式查询失败，继续处理其他的 */
                    Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                            "Failed to check uniqueness for rule: " + rule.description + " - " + e.getMessage());
                }
            }

            /* 检查是否有重复值，对每个重复出现的位置输出一条错误 */
            boolean valid = true;
            for (Map.Entry<String, List<Element>> entry : valueElementMap.entrySet()) {
                if (entry.getValue().size() > 1) {
                    String value = entry.getKey();
                    List<Element> elements = entry.getValue();

                    /* 对于每个重复位置（从第二个开始），输出一条错误 */
                    for (int i = 1; i < elements.size(); i++) {
                        Element element = elements.get(i);
                        String elementName = element.getLocalName();
                        String attributeName = getAttributeNameFromRule(rule);

                        /* 获取行号和文件名 */
                        int lineNumber = getLineNumber(element, lineNumberMap);
                        String fileName = getFileNameFromElement(element, xmlFile);

                        String errorMsg = String.format(
                                "[File: %s, Line: %d] the element %s  attribute  %s  value %s is not unique.",
                                fileName, lineNumber, elementName, attributeName, value);

                        errors.add(errorMsg);
                        Log.consistencyError(CodeConstant.ERROR_UNIQUE_CONSTRAINT_ATTRIBUTE_NOT_UNIQUE, errorMsg);
                        valid = false;
                    }
                }
            }

            return valid;

        } catch (Exception e) {
            Log.consistencyError(CodeConstant.ERROR_UNIQUE_CONSTRAINT_ATTRIBUTE_NOT_UNIQUE,
                    "Failed to check global uniqueness for: " + rule.description + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * checkUniquenessWithScope
     *
     * @brief
     * 功能: 按作用域执行唯一性检查。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象或表达式。
     * @param[in] rule 唯一性规则。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errors 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean checkUniquenessWithScope(Document doc, XPath xpath, UniquenessRule rule,
            File xmlFile, Map<String, Integer> lineNumberMap,
            List<String> errors) {
        try {
            boolean valid = true;

            /* 获取所有作用域节点（如所有Application节点） */
            NodeList scopeNodes = (NodeList) xpath.evaluate(rule.scopeXpath, doc, XPathConstants.NODESET);

            for (int i = 0; i < scopeNodes.getLength(); i++) {
                Element scopeElement = (Element) scopeNodes.item(i);

                /* 在当前作用域内收集属性值和元素 */
                Map<String, List<Element>> valueElementMap = new HashMap<>();

                for (String xpathExpr : rule.xpaths) {
                    try {
                        /* 构建相对XPath：从作用域元素内部搜索 */
                        /* 例如：从 Application 内搜索所有 QueuingPort/@Name */
                        String relativeXpath = buildRelativeXpath(xpathExpr, rule.scopeXpath);
                        NodeList attrNodes = (NodeList) xpath.evaluate(relativeXpath, scopeElement,
                                XPathConstants.NODESET);

                        for (int j = 0; j < attrNodes.getLength(); j++) {
                            Attr attr = (Attr) attrNodes.item(j);
                            String value = attr.getNodeValue();

                            if (value != null && !value.trim().isEmpty()) {
                                Element parentElement = attr.getOwnerElement();
                                valueElementMap.computeIfAbsent(value, k -> new ArrayList<>()).add(parentElement);
                            }
                        }
                    } catch (Exception e) {
                        /* 继续处理 */
                    }
                }

                /* 检查当前作用域内是否有重复值，对每个重复出现的位置输出一条错误 */
                for (Map.Entry<String, List<Element>> entry : valueElementMap.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        String value = entry.getKey();
                        List<Element> elements = entry.getValue();

                        /* 对于每个重复位置（从第二个开始），输出一条错误 */
                        for (int idx = 1; idx < elements.size(); idx++) {
                            Element element = elements.get(idx);
                            String elementName = element.getLocalName();
                            String attributeName = getAttributeNameFromRule(rule);

                            /* 获取行号和文件名 */
                            int lineNumber = getLineNumber(element, lineNumberMap);
                            String fileName = getFileNameFromElement(element, xmlFile);

                            String errorMsg = String.format(
                                    "[File: %s, Line: %d] the element %s  attribute  %s  value %s is not unique.",
                                    fileName, lineNumber, elementName, attributeName, value);

                            errors.add(errorMsg);
                            Log.consistencyError(CodeConstant.ERROR_UNIQUE_CONSTRAINT_ATTRIBUTE_NOT_UNIQUE, errorMsg);
                            valid = false;
                        }
                    }
                }
            }

            return valid;

        } catch (Exception e) {
            Log.consistencyError(CodeConstant.ERROR_UNIQUE_CONSTRAINT_ATTRIBUTE_NOT_UNIQUE,
                    "Failed to check scoped uniqueness for: " + rule.description + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * buildRelativeXpath
     *
     * @brief
     * 功能: 根据作用域构建相对 XPath。
     * @param[in] fullXpath 完整 XPath。
     * @param[in] scopeXpath 作用域 XPath。
     * @return 字符串结果。
     * @scope internal
     */
    private static String buildRelativeXpath(String fullXpath, String scopeXpath) {
        /* 例如： */
        /* fullXpath = */
        /* "//*[local-name()='Application']//*[local-name()='QueuingPort']/@Name" */
        /* scopeXpath = "//*[local-name()='Application']" */
        /* 结果 = ".//*[local-name()='QueuingPort']/@Name" */

        /* 提取作用域元素名称 */
        /* 从 "//*[local-name()='Application']" 提取 "Application" */
        String scopePattern = "\\[local-name\\(\\)='([^']+)'\\]";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(scopePattern);
        java.util.regex.Matcher matcher = pattern.matcher(scopeXpath);

        if (matcher.find()) {
            String scopeElementName = matcher.group(1);
            /* 在fullXpath中找到作用域元素之后的部分 */
            String searchPattern = "\\[local-name\\(\\)='" + scopeElementName + "'\\]";
            int index = fullXpath.indexOf(searchPattern);
            if (index >= 0) {
                /* 从作用域元素之后开始 */
                String afterScope = fullXpath.substring(index + searchPattern.length());
                /* 确保保留完整的路径（包括元素类型筛选） */
                return "." + afterScope;
            }
        }

        /* 如果无法解析，使用备用方案 */
        /* 尝试提取目标元素名称和属性 */
        java.util.regex.Matcher targetMatcher = pattern.matcher(fullXpath);
        String lastElementName = null;
        while (targetMatcher.find()) {
            lastElementName = targetMatcher.group(1);
        }

        if (lastElementName != null) {
            String attrName = fullXpath.substring(fullXpath.lastIndexOf("/@") + 2);
            return ".//*[local-name()='" + lastElementName + "']/@" + attrName;
        }

        /* 最后的备用方案 */
        return ".//" + fullXpath.substring(fullXpath.lastIndexOf("']/") + 3);
    }

    /**
     * getAttributeNameFromRule
     *
     * @brief
     * 功能: 从唯一性规则中提取属性名称。
     * @param[in] rule 唯一性规则。
     * @return 字符串结果。
     * @scope internal
     */
    private static String getAttributeNameFromRule(UniquenessRule rule) {
        /* 从 XPath 表达式中提取属性名称 */
        /* 例如："//*[local-name()='Application']/@Name" -> "Name" */
        String xpath = rule.xpaths[0];
        int atIndex = xpath.lastIndexOf("/@");
        if (atIndex >= 0) {
            return xpath.substring(atIndex + 2);
        }
        return "UnknownAttribute";
    }

    /**
     * getLineNumber
     *
     * @brief
     * 功能: 根据元素和行号映射获取行号。
     * @param[in] element XML 元素。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @return 元素行号。
     * @scope internal
     */
    private static int getLineNumber(Element element, Map<String, Integer> lineNumberMap) {
        /* 获取文件名 */
        String fileName = getFileNameFromElement(element, null);
        if (fileName == null) {
            fileName = "unknown";
        }

        /* 构建元素路径（从根到当前元素） */
        List<Element> pathElements = new ArrayList<>();
        Node current = element;
        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            pathElements.add(0, (Element) current);
            current = current.getParentNode();
        }

        /* 构建路径前缀 */
        StringBuilder pathPrefix = new StringBuilder();
        for (int i = 0; i < pathElements.size() - 1; i++) { /* 不包括当前元素 */
            Element elem = pathElements.get(i);
            String nodeName = elem.getLocalName() != null ? elem.getLocalName() : elem.getNodeName();

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
                pathPrefix.append(nodeName).append("[@").append(identifier).append("]/");
            } else {
                pathPrefix.append(nodeName).append("/");
            }
        }

        /* 获取当前元素的名称和标识 */
        String elementName = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
        String identifier = null;
        if (elementName.equals("Application") || elementName.equals("Partition") ||
                elementName.equals("PseudoPartition") || elementName.equals("SharedLibrary") ||
                elementName.equals("SharedData") || elementName.equals("Schedule")) {
            identifier = element.getAttribute("Name");
            if (identifier == null || identifier.isEmpty()) {
                identifier = element.getAttribute("Identifier");
            }
        }

        /* 构建完整的位置路径 */
        String positionKey;
        if (identifier != null && !identifier.isEmpty()) {
            positionKey = fileName + "::" + pathPrefix + elementName + "[@" + identifier + "]";
        } else {
            /* 计算元素在同类型兄弟节点中的位置 */
            int position = calculateElementPosition(element);
            positionKey = fileName + "::" + pathPrefix + elementName + "[" + position + "]";
        }

        /* 精确匹配 */
        Integer lineNumber = lineNumberMap.get(positionKey);
        if (lineNumber != null) {
            return lineNumber;
        }

        /* 尝试使用后缀匹配（处理 XInclude 展开后路径不同的问题） */
        /* 例如：DOM路径是 */
        /* Module/Applications/Application[@X]/ApplicationDescription/Ports/QueuingPort[1] */
        /* SAX路径是 ApplicationDescription/Ports/QueuingPort[1] */
        String filePrefix = fileName + "::";
        for (Map.Entry<String, Integer> entry : lineNumberMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(filePrefix)) {
                String keyPath = key.substring(filePrefix.length());
                String searchPath = positionKey.substring(filePrefix.length());

                /* 检查是否是后缀匹配 */
                if (searchPath.endsWith(keyPath) || keyPath.endsWith(searchPath)) {
                    /* 进一步验证：确保元素名称和位置索引匹配 */
                    if (searchPath.contains(elementName + "[") && keyPath.contains(elementName + "[")) {
                        String searchPos = searchPath.substring(searchPath.lastIndexOf(elementName + "["));
                        String keyPos = keyPath.substring(keyPath.lastIndexOf(elementName + "["));
                        if (searchPos.equals(keyPos)) {
                            return entry.getValue();
                        }
                    }
                }
            }
        }

        /* 向上查找父元素的行号（作为后备） */
        for (int i = pathElements.size() - 2; i >= 0; i--) {
            Element parentElem = pathElements.get(i);
            String parentName = parentElem.getLocalName() != null ? parentElem.getLocalName()
                    : parentElem.getNodeName();
            String parentIdentifier = null;

            if (parentName.equals("Application") || parentName.equals("Partition") ||
                    parentName.equals("PseudoPartition") || parentName.equals("SharedLibrary") ||
                    parentName.equals("SharedData") || parentName.equals("Schedule")) {
                parentIdentifier = parentElem.getAttribute("Name");
                if (parentIdentifier == null || parentIdentifier.isEmpty()) {
                    parentIdentifier = parentElem.getAttribute("Identifier");
                }
            }

            if (parentIdentifier != null && !parentIdentifier.isEmpty()) {
                /* 构建到父元素的路径 */
                StringBuilder parentPathBuilder = new StringBuilder();
                for (int j = 0; j <= i; j++) {
                    Element e = pathElements.get(j);
                    String nodeName = e.getLocalName() != null ? e.getLocalName() : e.getNodeName();
                    String ident = null;

                    if (nodeName.equals("Application") || nodeName.equals("Partition") ||
                            nodeName.equals("PseudoPartition") || nodeName.equals("SharedLibrary") ||
                            nodeName.equals("SharedData") || nodeName.equals("Schedule")) {
                        ident = e.getAttribute("Name");
                        if (ident == null || ident.isEmpty()) {
                            ident = e.getAttribute("Identifier");
                        }
                    }

                    if (ident != null && !ident.isEmpty()) {
                        parentPathBuilder.append(nodeName).append("[@").append(ident).append("]/");
                    } else {
                        parentPathBuilder.append(nodeName).append("/");
                    }
                }

                String parentPath = fileName + "::" + parentPathBuilder.substring(0, parentPathBuilder.length() - 1);
                lineNumber = lineNumberMap.get(parentPath);
                if (lineNumber != null) {
                    return lineNumber;
                }
            }
        }

        return -1;
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

    /**
     * getFileNameFromElement
     *
     * @brief
     * 功能: 获取元素来源文件名。
     * @param[in] element XML 元素。
     * @param[in] defaultFile 默认来源文件。
     * @return 字符串结果。
     * @scope internal
     */
    private static String getFileNameFromElement(Element element, File defaultFile) {
        String baseURI = element.getBaseURI();
        if (baseURI != null && !baseURI.isEmpty()) {
            try {
                File file = new File(new java.net.URI(baseURI));
                return file.getName();
            } catch (Exception e) {
                /* 忽略 */
            }
        }

        /* 尝试从文档 URI 获取 */
        if (element.getOwnerDocument() != null) {
            String docURI = element.getOwnerDocument().getDocumentURI();
            if (docURI != null && !docURI.isEmpty()) {
                try {
                    File file = new File(new java.net.URI(docURI));
                    return file.getName();
                } catch (Exception e) {
                    /* 从路径字符串中提取文件名 */
                    int lastSlash = Math.max(docURI.lastIndexOf('/'), docURI.lastIndexOf('\\'));
                    if (lastSlash >= 0) {
                        return docURI.substring(lastSlash + 1);
                    }
                }
            }
        }

        return defaultFile != null ? defaultFile.getName() : "unknown.xml";
    }

    /**
     * @brief
     * 功能: 构建 XML 元素行号映射。
     * @param[in] xmlFile XML 配置文件。
     * @return 元素行号映射。
     * @scope internal
     */
    private static Map<String, Integer> buildLineNumberMap(File xmlFile) {
        Map<String, Integer> lineMap = new HashMap<>();

        try {
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            saxFactory.setNamespaceAware(true);
            SAXParser saxParser = saxFactory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {
                private Locator locator;
                private final StringBuilder currentPath = new StringBuilder();
                private final String currentFileName = xmlFile.getName();
                private final Map<String, Integer> elementCounters = new HashMap<>(); /* 记录每个路径下各类型元素的计数 */

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
                public void setDocumentLocator(Locator locator) {
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
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {
                    String elementName = localName != null && !localName.isEmpty() ? localName : qName;
                    int lineNumber = locator != null ? locator.getLineNumber() : -1;

                    /* 对于关键元素，使用 Name/Identifier 作为唯一标识 */
                    String identifier = null;
                    if (elementName.equals("Application") || elementName.equals("Partition") ||
                            elementName.equals("PseudoPartition") || elementName.equals("SharedLibrary") ||
                            elementName.equals("SharedData") || elementName.equals("Schedule")) {
                        identifier = attributes.getValue("Name");
                        if (identifier == null || identifier.isEmpty()) {
                            identifier = attributes.getValue("Identifier");
                        }
                    }

                    /* 为当前元素构建位置索引键 */
                    String counterKey = currentPath.toString() + elementName;
                    int position = elementCounters.getOrDefault(counterKey, 0) + 1;
                    elementCounters.put(counterKey, position);

                    /* 构建完整的位置路径 */
                    String positionKey;
                    if (identifier != null && !identifier.isEmpty()) {
                        positionKey = currentFileName + "::" + currentPath.toString() + elementName + "[@" + identifier
                                + "]";
                    } else {
                        positionKey = currentFileName + "::" + currentPath.toString() + elementName + "[" + position
                                + "]";
                    }

                    /* 记录行号 */
                    lineMap.put(positionKey, lineNumber);

                    /* 更新当前路径 */
                    if (identifier != null && !identifier.isEmpty()) {
                        currentPath.append(elementName).append("[@").append(identifier).append("]/");
                    } else {
                        currentPath.append(elementName).append("/");
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
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    /* 移除当前元素路径 */
                    int lastSlash = currentPath.lastIndexOf("/", currentPath.length() - 2);
                    if (lastSlash >= 0) {
                        currentPath.setLength(lastSlash + 1);
                    } else {
                        currentPath.setLength(0);
                    }
                }
            };

            saxParser.parse(xmlFile, handler);
        } catch (Exception e) {
            Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                    "Failed to build line number map for file: " + xmlFile.getName() + " - " + e.getMessage());
        }

        return lineMap;
    }

    /**
     * collectIncludedFiles
     *
     * @brief
     * 功能: 收集 XInclude 引用的 XML 文件。
     * @param[in] doc XML DOM 文档。
     * @param[in] baseFile 基准 XML 文件。
     * @param[out] allFiles 文件收集列表。
     * @return 无。
     * @scope internal
     */
    private static void collectIncludedFiles(Document doc, File baseFile, List<File> allFiles) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 查找所有 xi:include 元素 */
            NodeList includes = (NodeList) xpath.evaluate(
                    "//*[local-name()='include']",
                    doc,
                    XPathConstants.NODESET);

            for (int i = 0; i < includes.getLength(); i++) {
                Element includeElement = (Element) includes.item(i);
                String href = includeElement.getAttribute("href");

                if (href != null && !href.isEmpty()) {
                    /* 解析环境变量 */
                    String resolvedHref = XmlParser.resolveEnvVariables(href);
                    File includedFile;
                    /* 如果解析后的路径是绝对路径，直接使用；否则相对于baseFile父目录 */
                    if (new File(resolvedHref).isAbsolute()) {
                        includedFile = new File(resolvedHref);
                    } else {
                        includedFile = new File(baseFile.getParentFile(), resolvedHref);
                    }
                    if (includedFile.exists() && !allFiles.contains(includedFile)) {
                        allFiles.add(includedFile);

                        /* 递归处理被包含的文件 */
                        try {
                            Document includedDoc = XmlParser.parseXmlWithEnvSupport(includedFile, false);
                            collectIncludedFiles(includedDoc, includedFile, allFiles);
                        } catch (Exception e) {
                            /* 忽略解析错误 */
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                    "Failed to collect included files: " + e.getMessage());
        }
    }

    /**
     * formatXmlErrorMessage
     *
     * @brief
     * 功能: 格式化 XML 解析异常信息。
     * @param[in] e 异常对象。
     * @return 字符串结果。
     * @scope internal
     */
    private static String formatXmlErrorMessage(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().trim() : "";

        /* 去除末尾的点和空格 */
        if (message != null && !message.isEmpty()) {
            message = message.replaceAll("[.\\s]+$", "");
        }

        if (message.isEmpty()) {
            message = e.getClass().getSimpleName();
        }

        /* 翻译错误信息 */
        return translateErrorMessage(message);
    }

    /**
     * translateErrorMessage
     *
     * @brief
     * 功能: 将 XML 错误信息转换为标准原因。
     * @param[in] message 错误信息。
     * @return 字符串结果。
     * @scope internal
     */
    private static String translateErrorMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String lower = message.toLowerCase();

        /* 1. XML File Encoding Format Error（编码相关错误） */
        if (lower.contains("invalid byte") || lower.contains("malformed")) {
            if (lower.contains("utf")) {
                return "XML File Encoding Format Error";
            }
            return "XML File Encoding Format Error";
        }
        /* 检测无效的编码名称（如 "Invalid encoding name"） */
        if (lower.contains("encoding") && (lower.contains("invalid") || lower.contains("unsupported"))) {
            return "XML File Encoding Format Error";
        }

        /* 2. Incorrect Start Tag Format（起始标签格式错误） */
        if (lower.contains("element type")
                && (lower.contains("must be followed") || lower.contains("not terminated"))) {
            return "Incorrect Start Tag Format";
        }
        if (lower.contains("content of elements must consist")) {
            return "Incorrect Start Tag Format";
        }

        /* 5. Incorrect closing tag name（结束标签格式错误） */
        if (lower.contains("end-tag") || (lower.contains("closing tag") && lower.contains("does not match"))) {
            return "Incorrect closing tag name";
        }

        /* 3. Incorrect XML header format（表头/声明错误） */
        /* 必须优先于根元素检测，因为XML头部错误也可能触发"root element"错误消息 */
        if (lower.contains("xml declaration") || lower.contains("prolog")) {
            return "Incorrect XML header format";
        }
        /* 检测"根元素之前的标记"相关错误 - 这实际上是指XML声明部分 */
        if ((lower.contains("preceding") || lower.contains("before")) && lower.contains("root element")) {
            return "Incorrect XML header format";
        }
        /* 检测处理指令相关错误（XML声明本身就是一个处理指令） */
        if (lower.contains("processing instruction")) {
            /* 如果明确提到"target"，可能是注释格式问题 */
            if (lower.contains("target") && lower.contains("matching")) {
                return "Incorrect XML comment format";
            }
            /* 其他处理指令错误通常是XML头部问题 */
            return "Incorrect XML header format";
        }
        /* 检测"content is not allowed in prolog"等前导内容错误 */
        if (lower.contains("content") && lower.contains("prolog")) {
            return "Incorrect XML header format";
        }
        /* 检测"markup must be well-formed"相关错误 */
        if (lower.contains("markup") && lower.contains("well-formed")) {
            return "Incorrect XML header format";
        }

        /* 4. Incorrect XML comment format（注释格式错误） */
        if (lower.contains("comment") && (lower.contains("not well-formed") || lower.contains("invalid"))) {
            return "Incorrect XML comment format";
        }

        /* 内存错误（归入7种reason之一：Root element parsing exception） */
        if (lower.contains("out of memory") || lower.contains("java heap space")) {
            return "Root element parsing exception";
        }

        /* 6. Root element parsing exception（根元素错误） */
        /* 检测真正的根元素错误（如根元素外有内容） */
        if (lower.contains("root element") || lower.contains("document element")) {
            return "Root element parsing exception";
        }

        /* 7. Attribute Definition Exception（属性错误） */
        if (lower.contains("attribute")
                && (lower.contains("invalid") || lower.contains("duplicate") || lower.contains("malformed"))) {
            return "Attribute Definition Exception";
        }

        /* 其他常见错误（归入7种reason之一：Root element parsing exception） */
        if (lower.contains("premature end of file") || lower.contains("unexpected end of file")) {
            return "Root element parsing exception";
        }
        if (lower.contains("not well-formed")) {
            return "Root element parsing exception";
        }

        /* 未匹配的场景统一归入7种reason之一，保证reason取值始终为需求表中的7种之一 */
        return "Root element parsing exception";
    }
}
