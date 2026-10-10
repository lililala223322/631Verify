/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SharedLibraryReferenceValidator.java
 * 
 * 描述    : 校验共享库定义与引用之间的一致性。
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
 * 功能: 校验共享库定义与引用之间的一致性。
 */
public class SharedLibraryReferenceValidator {

    /**
     * @brief
     * 功能: 记录共享库定义所在行号。
     */
    private static class SharedLibraryDefinition {
        int lineNumber;

        /**
         * SharedLibraryDefinition
         *
         * @brief
         * 功能: 创建共享库定义信息对象。
         * @param[in] lineNumber XML 行号。
         * @return 无。
         * @scope internal
         */
        SharedLibraryDefinition(int lineNumber) {
            this.lineNumber = lineNumber;
        }
    }

    /**
     * validateSharedLibraryReference
     *
     * @brief
     * 功能: 校验 Module XML 中共享库定义是否被引用。
     * @param[in]  xmlFile       Module XML 配置文件。
     * @param[out] errorMessages 错误或警告信息列表。
     * @return 全部共享库定义均被引用返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateSharedLibraryReference(File xmlFile, List<String> errorMessages) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true; /* 文件不存在，跳过验证 */
        }

        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();

        try {
            /* 第一步：使用SAX解析器建立行号映射（在XInclude展开前） */
            Map<String, Integer> lineNumberMap = buildLineNumberMap(xmlFile);

            /* 第二步：使用支持环境变量的XML解析，获取完整的DOM树 */
            org.w3c.dom.Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, true);

            /* 第三步：使用XPath查询SharedLibrary定义和引用 */
            javax.xml.xpath.XPathFactory xPathFactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xPathFactory.newXPath();

            /* 查找所有定义的SharedLibrary */
            /* 使用 local-name() 来忽略命名空间 */
            String defineXPath = "//*[local-name()='Module']/*[local-name()='SharedLibraryRegions']/*[local-name()='SharedLibrary']";
            org.w3c.dom.NodeList definedNodes = (org.w3c.dom.NodeList) xpath.evaluate(
                    defineXPath, doc, javax.xml.xpath.XPathConstants.NODESET);

            Map<String, SharedLibraryDefinition> definedLibraries = new HashMap<>();
            for (int i = 0; i < definedNodes.getLength(); i++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) definedNodes.item(i);
                String name = element.getAttribute("Name");
                if (name != null && !name.isEmpty()) {
                    /* 从行号映射中获取行号 */
                    String key = "SharedLibrary[@Name='" + name + "']";
                    int lineNumber = lineNumberMap.getOrDefault(key, -1);
                    definedLibraries.put(name, new SharedLibraryDefinition(lineNumber));
                }
            }

            /* 如果没有定义SharedLibrary，跳过验证 */
            if (definedLibraries.isEmpty()) {
                return true;
            }

            /* 查找所有引用的SharedLibrary (在Partition中通过SharedLibraryRegion引用) */
            /* 使用 local-name() 来忽略命名空间 */
            String refXPath = "//*[local-name()='Module']/*[local-name()='Partitions']//*[local-name()='SharedLibraryRegion']";
            org.w3c.dom.NodeList refNodes = (org.w3c.dom.NodeList) xpath.evaluate(
                    refXPath, doc, javax.xml.xpath.XPathConstants.NODESET);

            Set<String> referencedLibraries = new HashSet<>();
            for (int i = 0; i < refNodes.getLength(); i++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) refNodes.item(i);
                String nameRef = element.getAttribute("NameRef");
                if (nameRef != null && !nameRef.isEmpty()) {
                    referencedLibraries.add(nameRef);
                }
            }

            /* 检查未被引用的SharedLibrary */
            boolean allUsed = true;
            for (Map.Entry<String, SharedLibraryDefinition> entry : definedLibraries.entrySet()) {
                String libraryName = entry.getKey();
                SharedLibraryDefinition definition = entry.getValue();

                if (!referencedLibraries.contains(libraryName)) {
                    /* 发现未被引用的SharedLibrary */
                    String warningMsg = String.format(
                            "[File: %s, Line: %d] configured shared lib %s not used.",
                            xmlFile.getName(),
                            definition.lineNumber > 0 ? definition.lineNumber : -1,
                            libraryName);

                    localErrors.add(warningMsg);
                    Log.warning(CodeConstant.WARNING_SHARED_LIB_NOT_USED, warningMsg);
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
     * 功能: 使用 SAX 解析器建立共享库元素行号映射。
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
     * 功能: 收集共享库元素行号信息。
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

            /* 记录SharedLibrary元素的行号 */
            if ("SharedLibrary".equals(elementName)) {
                String name = attributes.getValue("Name");
                if (name != null && !name.isEmpty()) {
                    int lineNumber = (locator != null) ? locator.getLineNumber() : -1;
                    lineNumberMap.put("SharedLibrary[@Name='" + name + "']", lineNumber);
                }
            }
        }
    }

    /**
     * validateSharedLibraryReference
     *
     * @brief
     * 功能: 跨 Module 文件和 Partition 文件校验共享库定义是否被引用。
     * @param[in]  moduleFile     Module XML 配置文件。
     * @param[in]  partitionFiles Partition XML 文件列表。
     * @param[out] errorMessages  错误或警告信息列表。
     * @return 全部共享库定义均被引用返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateSharedLibraryReference(File moduleFile, List<File> partitionFiles,
            List<String> errorMessages) {
        if (moduleFile == null || !moduleFile.exists()) {
            return true;
        }

        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();

        try {
            /* 从Module文件收集SharedLibrary定义 */
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();

            SharedLibraryCollector moduleHandler = new SharedLibraryCollector();
            parser.parse(moduleFile, moduleHandler);

            Map<String, SharedLibraryDefinition> definedLibraries = moduleHandler.getDefinedSharedLibraries();

            if (definedLibraries.isEmpty()) {
                return true; /* 没有定义SharedLibrary */
            }

            /* 收集所有引用 */
            Set<String> allReferences = new HashSet<>(moduleHandler.getReferencedSharedLibraries());

            /* 从Partition文件收集引用 */
            if (partitionFiles != null) {
                for (File partFile : partitionFiles) {
                    if (partFile != null && partFile.exists()) {
                        SharedLibraryCollector partHandler = new SharedLibraryCollector();
                        parser.parse(partFile, partHandler);
                        allReferences.addAll(partHandler.getReferencedSharedLibraries());
                    }
                }
            }

            /* 检查未被引用的SharedLibrary */
            boolean allUsed = true;
            for (Map.Entry<String, SharedLibraryDefinition> entry : definedLibraries.entrySet()) {
                String libraryName = entry.getKey();
                SharedLibraryDefinition definition = entry.getValue();

                if (!allReferences.contains(libraryName)) {
                    String warningMsg = String.format(
                            "[File: %s, Line: %d] configured shared lib %s not used.",
                            moduleFile.getName(),
                            definition.lineNumber,
                            libraryName);

                    localErrors.add(warningMsg);
                    Log.warning(CodeConstant.WARNING_SHARED_LIB_NOT_USED, warningMsg);
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
     * 功能: 收集共享库定义和引用信息。
     */
    private static class SharedLibraryCollector extends DefaultHandler {
        private Locator locator;

        /* 存储定义的SharedLibrary: Name -> (LineNumber) */
        private Map<String, SharedLibraryDefinition> definedSharedLibraries = new HashMap<>();

        /* 存储引用的SharedLibrary: Set of Names */
        private Set<String> referencedSharedLibraries = new HashSet<>();

        private boolean inSharedLibraryRegions = false;

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

            /* 跟踪是否在SharedLibraryRegions内 */
            if ("SharedLibraryRegions".equals(elementName)) {
                inSharedLibraryRegions = true;
            }

            /* 收集SharedLibrary定义（在Module文件中） */
            /* <SharedLibrary Name="xxx"> */
            if ("SharedLibrary".equals(elementName) && inSharedLibraryRegions) {
                String name = attributes.getValue("Name");
                if (name != null && !name.trim().isEmpty()) {
                    int lineNumber = (locator != null) ? locator.getLineNumber() : -1;
                    definedSharedLibraries.put(name, new SharedLibraryDefinition(lineNumber));
                }
            }

            /* 收集SharedLibrary引用（在Partition文件中或Module内嵌的Partition定义中） */
            /* <SharedLibraryRegion NameRef="xxx"> */
            if ("SharedLibraryRegion".equals(elementName)) {
                String nameRef = attributes.getValue("NameRef");
                if (nameRef != null && !nameRef.trim().isEmpty()) {
                    referencedSharedLibraries.add(nameRef);
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

            if ("SharedLibraryRegions".equals(elementName)) {
                inSharedLibraryRegions = false;
            }
        }

        /**
         * @brief
         * 功能: 获取已定义共享库映射。
         * @return 已定义共享库映射。
         * @scope internal
         */
        public Map<String, SharedLibraryDefinition> getDefinedSharedLibraries() {
            return definedSharedLibraries;
        }

        /**
         * getReferencedSharedLibraries
         *
         * @brief
         * 功能: 获取已引用共享库名称集合。
         * @return 已引用共享库名称集合。
         * @scope internal
         */
        public Set<String> getReferencedSharedLibraries() {
            return referencedSharedLibraries;
        }
    }
}
