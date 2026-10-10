/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: ApplicationReferenceValidator.java
 * 
 * 描述    : 校验应用定义与应用引用之间的一致性。
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
 * 功能: 校验应用定义与应用引用之间的一致性。
 */
public class ApplicationReferenceValidator {

    /**
     * @brief
     * 功能: 保存Application定义所在的XML行号。
     */
    private static class ApplicationDefinition {
        int lineNumber;

        ApplicationDefinition(int lineNumber) {
            this.lineNumber = lineNumber;
        }
    }

    /**
     * validateApplicationReference
     *
     * @brief
     * 功能: 验证Module XML文件中定义的Application是否被分区引用。
     *       处理流程如下：
     *       1. XML文件为空或不存在时跳过验证；
     *       2. 使用SAX解析器建立Application定义行号映射；
     *       3. 使用支持环境变量和XInclude的解析结果查询Application定义和引用；
     *       4. 对未被引用的Application记录4004告警。
     * 
     * @param[in]  xmlFile: 待验证的Module XML文件。
     * @param[out] errorMessages: 错误或告警信息列表。
     * @return
     * boolean: 全部Application均被引用返回true，存在未使用Application或解析异常返回false。
     * @implements 
     * @scope external
     * @errno:
     * WARNING_APP_NOT_USED: 配置的Application未被使用。
     */
    public static boolean validateApplicationReference(File xmlFile, List<String> errorMessages) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true; /* 文件不存在，跳过验证 */
        }

        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();

        try {
            /* 第一步：使用SAX解析器建立行号映射（在XInclude展开前） */
            Map<String, Integer> lineNumberMap = buildLineNumberMap(xmlFile);

            /* 第二步：使用支持环境变量的XML解析，获取完整的DOM树 */
            org.w3c.dom.Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, true);

            /* 第三步：使用XPath查询Application定义和引用 */
            javax.xml.xpath.XPathFactory xPathFactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xPathFactory.newXPath();

            /* 查找所有定义的Application */
            /* 使用 local-name() 来忽略命名空间 */
            String defineXPath = "//*[local-name()='Module']/*[local-name()='Applications']/*[local-name()='Application']";
            org.w3c.dom.NodeList definedNodes = (org.w3c.dom.NodeList) xpath.evaluate(
                    defineXPath, doc, javax.xml.xpath.XPathConstants.NODESET);

            Map<String, ApplicationDefinition> definedApplications = new LinkedHashMap<>();
            for (int i = 0; i < definedNodes.getLength(); i++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) definedNodes.item(i);
                String name = element.getAttribute("Name");
                if (name != null && !name.isEmpty()) {
                    /* 从行号映射中获取行号 */
                    String key = "Application[@Name='" + name + "']";
                    int lineNumber = lineNumberMap.getOrDefault(key, -1);
                    definedApplications.put(name, new ApplicationDefinition(lineNumber));
                }
            }

            /* 如果没有定义Application，跳过验证 */
            if (definedApplications.isEmpty()) {
                return true;
            }

            /* 查找所有引用的Application（在Partition的PartitionDescription中） */
            /* 使用 local-name() 来忽略命名空间 */
            String refXPath = "//*[local-name()='Module']/*[local-name()='Partitions']//*[local-name()='PartitionDescription']/*[local-name()='Application']";
            org.w3c.dom.NodeList refNodes = (org.w3c.dom.NodeList) xpath.evaluate(
                    refXPath, doc, javax.xml.xpath.XPathConstants.NODESET);

            Set<String> referencedApplications = new HashSet<>();
            for (int i = 0; i < refNodes.getLength(); i++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) refNodes.item(i);
                String nameRef = element.getAttribute("NameRef");
                if (nameRef != null && !nameRef.isEmpty()) {
                    referencedApplications.add(nameRef);
                }
            }

            /* 检查未被引用的Application */
            boolean allUsed = true;
            for (Map.Entry<String, ApplicationDefinition> entry : definedApplications.entrySet()) {
                String appName = entry.getKey();
                ApplicationDefinition definition = entry.getValue();

                if (!referencedApplications.contains(appName)) {
                    /* 发现未被引用的Application */
                    String warningMsg = String.format(
                            "[File: %s, Line: %d] configured app %s not used.",
                            xmlFile.getName(),
                            definition.lineNumber > 0 ? definition.lineNumber : -1,
                            appName);

                    localErrors.add(warningMsg);
                    Log.warning(CodeConstant.WARNING_APP_NOT_USED, warningMsg);
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
     * buildLineNumberMap
     *
     * @brief
     * 功能: 使用SAX解析器建立Application名称到XML行号的映射。
     * 
     * @param[in]  xmlFile: 待解析的XML文件。
     * @return
     * Map<String, Integer>: Application定位键到行号的映射；解析失败时返回空映射。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
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
     * 功能: 在SAX解析过程中收集Application元素的行号信息。
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

            /* 记录Application元素的行号 */
            if ("Application".equals(elementName)) {
                String name = attributes.getValue("Name");
                if (name != null && !name.isEmpty()) {
                    int lineNumber = (locator != null) ? locator.getLineNumber() : -1;
                    lineNumberMap.put("Application[@Name='" + name + "']", lineNumber);
                }
            }
        }
    }

    /**
     * @brief
     * 功能: 在SAX解析过程中收集Application定义集合和引用集合。
     */
    private static class ApplicationCollector extends DefaultHandler {
        private Map<String, ApplicationDefinition> definedApplications = new LinkedHashMap<>();
        private Set<String> referencedApplications = new HashSet<>();
        private Locator locator;

        /* 上下文追踪 */
        private boolean inApplicationsSection = false;
        private boolean inPartitionsSection = false;

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

            /* 进入 Applications 节点 */
            if (elementName.equals("Applications")) {
                inApplicationsSection = true;
            }
            /* 进入 Partitions 节点 */
            else if (elementName.equals("Partitions")) {
                inPartitionsSection = true;
            }
            /* 在 Applications 节点中找到 Application 定义 */
            else if (inApplicationsSection && elementName.equals("Application")) {
                String appName = attributes.getValue("Name");
                if (appName != null && !appName.isEmpty()) {
                    int lineNumber = (locator != null) ? locator.getLineNumber() : -1;
                    definedApplications.put(appName, new ApplicationDefinition(lineNumber));
                }
            }
            /* 在 Partitions 节点中找到 Partition 引用 */
            /* 注意：Partition 通过 Name 属性与 Application 关联（相同名称） */
            else if (inPartitionsSection && elementName.equals("Partition")) {
                String partitionName = attributes.getValue("Name");
                if (partitionName != null && !partitionName.isEmpty()) {
                    referencedApplications.add(partitionName);
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

            if (elementName.equals("Applications")) {
                inApplicationsSection = false;
            } else if (elementName.equals("Partitions")) {
                inPartitionsSection = false;
            }
        }
        /**
         * getDefinedApplications
         *
         * @brief
         * 功能: 获取已收集的Application定义集合。
         * 
         * @return
         * Map<String, ApplicationDefinition>: Application名称到定义信息的映射。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public Map<String, ApplicationDefinition> getDefinedApplications() {
            return definedApplications;
        }
        /**
         * getReferencedApplications
         *
         * @brief
         * 功能: 获取已收集的Application引用集合。
         * 
         * @return
         * Set<String>: 被引用的Application名称集合。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public Set<String> getReferencedApplications() {
            return referencedApplications;
        }
    }
}
