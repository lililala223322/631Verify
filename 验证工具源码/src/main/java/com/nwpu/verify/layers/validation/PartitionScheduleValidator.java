/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PartitionScheduleValidator.java
 * 
 * 描述    : 校验分区定义与分区调度引用之间的一致性。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.layers.parsing.XmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.*;

/**
 * @brief
 * 功能: 校验分区定义与分区调度引用之间的一致性。
 */
public class PartitionScheduleValidator {

    /**
     * @brief
     * 功能: 记录分区定义所在行号。
     */
    private static class PartitionDefinition {
        int lineNumber;

        /**
         * PartitionDefinition
         *
         * @brief
         * 功能: 创建分区定义信息对象。
         * @param[in] lineNumber XML 行号。
         * @return 无。
         * @scope internal
         */
        PartitionDefinition(int lineNumber) {
            this.lineNumber = lineNumber;
        }
    }

    /**
     * validatePartitionSchedule
     *
     * @brief
     * 功能: 校验 Module XML 中定义的分区是否在调度窗口中被引用。
     * @param[in]  xmlFile       Module XML 配置文件。
     * @param[out] errorMessages 错误或警告信息列表。
     * @return 所有分区均被调度返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validatePartitionSchedule(File xmlFile, List<String> errorMessages) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true; /* 文件不存在，跳过验证 */
        }

        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();

        try {
            /* 第一步：使用SAX解析器建立行号映射（在XInclude展开前） */
            Map<String, Integer> lineNumberMap = buildLineNumberMap(xmlFile);

            /* 第二步：使用支持环境变量的XML解析，获取完整的DOM树 */
            org.w3c.dom.Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, true);

            /* 第三步：使用XPath查询Partition定义和调度引用 */
            javax.xml.xpath.XPathFactory xPathFactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xPathFactory.newXPath();

            /* 查找所有定义的Partition（只包括Partition，不包括PseudoPartition，因为虚分区不需要调度） */
            /* 使用 local-name() 来忽略命名空间 */
            String defineXPath = "//*[local-name()='Module']/*[local-name()='Partitions']/*[local-name()='Partition']";
            org.w3c.dom.NodeList definedNodes = (org.w3c.dom.NodeList) xpath.evaluate(
                    defineXPath, doc, javax.xml.xpath.XPathConstants.NODESET);

            Map<String, PartitionDefinition> definedPartitions = new HashMap<>();
            for (int i = 0; i < definedNodes.getLength(); i++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) definedNodes.item(i);
                String name = element.getAttribute("Name");
                if (name != null && !name.isEmpty()) {
                    String elementName = element.getLocalName();
                    /* 从行号映射中获取行号 */
                    String key = elementName + "[@Name='" + name + "']";
                    int lineNumber = lineNumberMap.getOrDefault(key, -1);
                    definedPartitions.put(name, new PartitionDefinition(lineNumber));
                }
            }

            /* 如果没有定义Partition，跳过验证 */
            if (definedPartitions.isEmpty()) {
                return true;
            }

            /* 查找所有调度的Partition（在PartitionWindow中） */
            /* 使用 local-name() 来忽略命名空间 */
            String refXPath = "//*[local-name()='Module']/*[local-name()='Schedules']//*[local-name()='PartitionWindow']";
            org.w3c.dom.NodeList refNodes = (org.w3c.dom.NodeList) xpath.evaluate(
                    refXPath, doc, javax.xml.xpath.XPathConstants.NODESET);

            Set<String> scheduledPartitions = new HashSet<>();
            for (int i = 0; i < refNodes.getLength(); i++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) refNodes.item(i);
                String partitionName = element.getAttribute("PartitionNameRef");
                if (partitionName != null && !partitionName.isEmpty()) {
                    scheduledPartitions.add(partitionName);
                }
            }

            /* 检查未被调度的Partition */
            boolean allScheduled = true;
            for (Map.Entry<String, PartitionDefinition> entry : definedPartitions.entrySet()) {
                String partitionName = entry.getKey();
                PartitionDefinition definition = entry.getValue();

                if (!scheduledPartitions.contains(partitionName)) {
                    /* 发现未被调度的Partition */
                    String warningMsg = String.format(
                            "[File: %s, Line: %d] configured partition %s not scheduled.",
                            xmlFile.getName(),
                            definition.lineNumber > 0 ? definition.lineNumber : -1,
                            partitionName);

                    localErrors.add(warningMsg);
                    Log.warning(CodeConstant.WARNING_PARTITION_NOT_SCHEDULED, warningMsg);
                    allScheduled = false;
                }
            }

            return allScheduled;

        } catch (Exception e) {
            /* XML解析失败时不输出错误，避免冗余信息 */
            return false;
        }
    }

    /**
     * @brief
     * 功能: 使用 SAX 解析器建立分区元素行号映射。
     * @param[in] xmlFile XML 配置文件。
     * @return 元素行号映射。
     * @scope internal
     */
    private static Map<String, Integer> buildLineNumberMap(File xmlFile) {
        Map<String, Integer> lineNumberMap = new HashMap<>();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();

            LineNumberCollector handler = new LineNumberCollector(lineNumberMap);
            parser.parse(xmlFile, handler);
        } catch (Exception e) {
            /* 忽略错误，返回空映射 */
        }
        return lineNumberMap;
    }

    /**
     * @brief
     * 功能: 收集分区元素行号信息。
     */
    private static class LineNumberCollector extends DefaultHandler {
        private Locator locator;
        private Map<String, Integer> lineNumberMap;

        LineNumberCollector(Map<String, Integer> lineNumberMap) {
            this.lineNumberMap = lineNumberMap;
        }

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
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String elementName = localName.isEmpty() ? qName : localName;

            /* 记录Partition和PseudoPartition元素的行号 */
            if ("Partition".equals(elementName) || "PseudoPartition".equals(elementName)) {
                String name = attributes.getValue("Name");
                if (name != null && !name.isEmpty()) {
                    int lineNumber = (locator != null) ? locator.getLineNumber() : -1;
                    lineNumberMap.put(elementName + "[@Name='" + name + "']", lineNumber);
                }
            }
        }
    }

    /**
     * @brief
     * 功能: 收集 Partition 定义和调度引用信息。
     */
    private static class PartitionScheduleCollector extends DefaultHandler {
        private Locator locator;

        /* 存储定义的Partition: Name -> (LineNumber) */
        private Map<String, PartitionDefinition> definedPartitions = new HashMap<>();

        /* 存储调度的Partition: Set of Names (来自PartitionWindow) */
        private Set<String> scheduledPartitions = new HashSet<>();

        private boolean inPartitions = false;

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
            String elementName = localName.isEmpty() ? qName : localName;

            /* 跟踪是否在Partitions内 */
            if ("Partitions".equals(elementName)) {
                inPartitions = true;
            }

            /* 收集Partition定义（在Module文件中） */
            /* <Partition Name="xxx"> */
            if ("Partition".equals(elementName) && inPartitions) {
                String name = attributes.getValue("Name");
                if (name != null && !name.trim().isEmpty()) {
                    int lineNumber = (locator != null) ? locator.getLineNumber() : -1;
                    definedPartitions.put(name, new PartitionDefinition(lineNumber));
                }
            }

            /* PseudoPartition（虚分区）通常是IO分区，不需要被调度，所以不检查 */
            /* <PseudoPartition Name="xxx"> */
            /* 注释掉：虚分区不需要被调度 */
            /*
             * if ("PseudoPartition".equals(elementName) && inPartitions) {
             * String name = attributes.getValue("Name");
             * if (name != null && !name.trim().isEmpty()) {
             * int lineNumber = (locator != null) ? locator.getLineNumber() : -1;
             * definedPartitions.put(name, new PartitionDefinition(lineNumber));
             * }
             * }
             */

            /* 收集调度引用（在Schedule中的PartitionWindow） */
            /* <PartitionWindow PartitionNameRef="xxx"> */
            if ("PartitionWindow".equals(elementName)) {
                String partitionNameRef = attributes.getValue("PartitionNameRef");
                if (partitionNameRef != null && !partitionNameRef.trim().isEmpty()) {
                    /* "SPARE"是特殊值，不是真实的Partition定义 */
                    if (!"SPARE".equals(partitionNameRef)) {
                        scheduledPartitions.add(partitionNameRef);
                    }
                }
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
            String elementName = localName.isEmpty() ? qName : localName;

            if ("Partitions".equals(elementName)) {
                inPartitions = false;
            }
        }

        /**
         * getDefinedPartitions
         *
         * @brief
         * 功能: 执行 getDefinedPartitions 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        public Map<String, PartitionDefinition> getDefinedPartitions() {
            return definedPartitions;
        }

        /**
         * getScheduledPartitions
         *
         * @brief
         * 功能: 执行 getScheduledPartitions 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        public Set<String> getScheduledPartitions() {
            return scheduledPartitions;
        }
    }
}
