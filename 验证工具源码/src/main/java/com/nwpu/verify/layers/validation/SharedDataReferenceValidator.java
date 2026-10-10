/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SharedDataReferenceValidator.java
 * 
 * 描述    : 校验共享数据定义与引用之间的一致性。
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
 * 功能: 校验共享数据定义与引用之间的一致性。
 */
public class SharedDataReferenceValidator {

    /**
     * @brief
     * 功能: 记录共享数据定义所在行号。
     */
    private static class SharedDataDefinition {
        int lineNumber;

        /**
         * SharedDataDefinition
         *
         * @brief
         * 功能: 创建共享数据定义信息对象。
         * @param[in] lineNumber XML 行号。
         * @return 无。
         * @scope internal
         */
        SharedDataDefinition(int lineNumber) {
            this.lineNumber = lineNumber;
        }
    }

    /**
     * validateSharedDataReference
     *
     * @brief
     * 功能: 校验 Module XML 中共享数据定义是否被引用。
     * @param[in]  xmlFile       Module XML 配置文件。
     * @param[out] errorMessages 错误或警告信息列表。
     * @return 全部共享数据定义均被引用返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateSharedDataReference(File xmlFile, List<String> errorMessages) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true; /* 文件不存在，跳过验证 */
        }

        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();

        try {
            /* 第一步：使用SAX解析器建立行号映射（在XInclude展开前） */
            Map<String, Integer> lineNumberMap = buildLineNumberMap(xmlFile);

            /* 第二步：使用支持环境变量的XML解析，获取完整的DOM树 */
            org.w3c.dom.Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, true);

            /* 第三步：使用XPath查询SharedData定义和引用 */
            javax.xml.xpath.XPathFactory xPathFactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xPathFactory.newXPath();

            /* 查找所有定义的SharedData */
            /* 使用 local-name() 来忽略命名空间 */
            String defineXPath = "//*[local-name()='Module']/*[local-name()='SharedDataRegions']/*[local-name()='SharedData']";
            org.w3c.dom.NodeList definedNodes = (org.w3c.dom.NodeList) xpath.evaluate(
                    defineXPath, doc, javax.xml.xpath.XPathConstants.NODESET);

            Map<String, SharedDataDefinition> definedSharedData = new HashMap<>();
            for (int i = 0; i < definedNodes.getLength(); i++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) definedNodes.item(i);
                String name = element.getAttribute("Name");
                if (name != null && !name.isEmpty()) {
                    /* 从行号映射中获取行号 */
                    String key = "SharedData[@Name='" + name + "']";
                    int lineNumber = lineNumberMap.getOrDefault(key, -1);
                    definedSharedData.put(name, new SharedDataDefinition(lineNumber));
                }
            }

            /* 如果没有定义SharedData，跳过验证 */
            if (definedSharedData.isEmpty()) {
                return true;
            }

            /* 查找所有引用的SharedData (在Partition中通过SharedDataRegion引用) */
            /* 使用 local-name() 来忽略命名空间 */
            String refXPath = "//*[local-name()='Module']/*[local-name()='Partitions']//*[local-name()='SharedDataRegion']";
            org.w3c.dom.NodeList refNodes = (org.w3c.dom.NodeList) xpath.evaluate(
                    refXPath, doc, javax.xml.xpath.XPathConstants.NODESET);

            Set<String> referencedSharedData = new HashSet<>();
            for (int i = 0; i < refNodes.getLength(); i++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) refNodes.item(i);
                String name = element.getAttribute("Name");
                if (name != null && !name.isEmpty()) {
                    referencedSharedData.add(name);
                }
            }

            /* 检查未被引用的SharedData */
            boolean allUsed = true;
            for (Map.Entry<String, SharedDataDefinition> entry : definedSharedData.entrySet()) {
                String sharedDataName = entry.getKey();
                SharedDataDefinition definition = entry.getValue();

                if (!referencedSharedData.contains(sharedDataName)) {
                    /* 发现未被引用的SharedData */
                    String warningMsg = String.format(
                            "[File: %s, Line: %d] configured shared data %s not used.",
                            xmlFile.getName(),
                            definition.lineNumber > 0 ? definition.lineNumber : -1,
                            sharedDataName);

                    localErrors.add(warningMsg);
                    Log.warning(CodeConstant.WARNING_SHARED_DATA_NOT_USED, warningMsg);
                    allUsed = false;
                }
            }

            return allUsed;

        } catch (Exception e) {
            /* XML解析失败时不输出错误，避免冗余信息 */
            return false;
        }
    }

    /**
     * @brief
     * 功能: 使用 SAX 解析器建立共享数据元素行号映射。
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
     * 功能: 收集共享数据元素行号信息。
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

            /* 记录SharedData元素的行号 */
            if ("SharedData".equals(elementName)) {
                String name = attributes.getValue("Name");
                if (name != null && !name.isEmpty()) {
                    int lineNumber = (locator != null) ? locator.getLineNumber() : -1;
                    lineNumberMap.put("SharedData[@Name='" + name + "']", lineNumber);
                }
            }
        }
    }

    /**
     * validateSharedDataReference
     *
     * @brief
     * 功能: 跨 Module 文件和 Partition 文件校验共享数据定义是否被引用。
     * @param[in]  moduleFile     Module XML 配置文件。
     * @param[in]  partitionFiles Partition XML 文件列表。
     * @param[out] errorMessages  错误或警告信息列表。
     * @return 全部共享数据定义均被引用返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateSharedDataReference(File moduleFile, List<File> partitionFiles,
            List<String> errorMessages) {
        if (moduleFile == null || !moduleFile.exists()) {
            return true;
        }

        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();

        try {
            /* 从Module文件收集SharedData定义 */
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();

            SharedDataCollector moduleHandler = new SharedDataCollector();
            parser.parse(moduleFile, moduleHandler);

            Map<String, SharedDataDefinition> definedSharedData = moduleHandler.getDefinedSharedData();

            if (definedSharedData.isEmpty()) {
                return true; /* 没有定义SharedData */
            }

            /* 收集所有引用 */
            Set<String> allReferences = new HashSet<>(moduleHandler.getReferencedSharedData());

            /* 从Partition文件收集引用 */
            if (partitionFiles != null) {
                for (File partFile : partitionFiles) {
                    if (partFile != null && partFile.exists()) {
                        SharedDataCollector partHandler = new SharedDataCollector();
                        parser.parse(partFile, partHandler);
                        allReferences.addAll(partHandler.getReferencedSharedData());
                    }
                }
            }

            /* 检查未被引用的SharedData */
            boolean allUsed = true;
            for (Map.Entry<String, SharedDataDefinition> entry : definedSharedData.entrySet()) {
                String sharedDataName = entry.getKey();
                SharedDataDefinition definition = entry.getValue();

                if (!allReferences.contains(sharedDataName)) {
                    String warningMsg = String.format(
                            "[File: %s, Line: %d] configured shared data %s not used.",
                            moduleFile.getName(),
                            definition.lineNumber,
                            sharedDataName);

                    localErrors.add(warningMsg);
                    Log.warning(CodeConstant.WARNING_SHARED_DATA_NOT_USED, warningMsg);
                    allUsed = false;
                }
            }

            return allUsed;

        } catch (Exception e) {
            /* XML解析失败时不输出错误，避免冗余信息 */
            return false;
        }
    }

    /**
     * @brief
     * 功能: 收集共享数据定义和引用信息。
     */
    private static class SharedDataCollector extends DefaultHandler {
        private Locator locator;

        /* 存储定义的SharedData: Name -> (Name, LineNumber) */
        private Map<String, SharedDataDefinition> definedSharedData = new HashMap<>();

        /* 存储引用的SharedData: Set of Names */
        private Set<String> referencedSharedData = new HashSet<>();

        private boolean inSharedDataRegions = false;

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

            /* 跟踪是否在SharedDataRegions内 */
            if ("SharedDataRegions".equals(elementName)) {
                inSharedDataRegions = true;
            }

            /* 收集SharedData定义（在Module文件中） */
            /* <SharedData Name="xxx"> */
            if ("SharedData".equals(elementName) && inSharedDataRegions) {
                String name = attributes.getValue("Name");
                if (name != null && !name.trim().isEmpty()) {
                    int lineNumber = (locator != null) ? locator.getLineNumber() : -1;
                    definedSharedData.put(name, new SharedDataDefinition(lineNumber));
                }
            }

            /* 收集SharedData引用（在Partition文件中或Module内嵌的Partition定义中） */
            /* <SharedDataRegion Name="xxx"> */
            if ("SharedDataRegion".equals(elementName)) {
                String name = attributes.getValue("Name");
                if (name != null && !name.trim().isEmpty()) {
                    referencedSharedData.add(name);
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

            if ("SharedDataRegions".equals(elementName)) {
                inSharedDataRegions = false;
            }
        }

        /**
         * @brief
         * 功能: 获取已定义共享数据映射。
         * @return 已定义共享数据映射。
         * @scope internal
         */
        public Map<String, SharedDataDefinition> getDefinedSharedData() {
            return definedSharedData;
        }

        /**
         * getReferencedSharedData
         *
         * @brief
         * 功能: 获取已引用共享数据名称集合。
         * @return 已引用共享数据名称集合。
         * @scope internal
         */
        public Set<String> getReferencedSharedData() {
            return referencedSharedData;
        }
    }
}
