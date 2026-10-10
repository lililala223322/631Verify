/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PageAlignmentValidator.java
 * 
 * 描述    : 校验地址和大小等属性是否满足页对齐要求。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
  *     2026-07-27  开发人员  修正页对齐错误信息中地址与数值之间的空格。
  *     2026-08-25  开发人员  按问题报告单TR_PR_002/003（004878）调整3003输出格式为无[Error]前缀、
  *                 错误码后接全角冒号，并按需求模板同步消息体占位符间双空格，
  *                 address与数值之间保留空格（恢复2026-07-27修正）。
*/

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.layers.parsing.XmlParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @brief
 * 功能: 校验地址和大小等属性是否满足页对齐要求。
 */
public class PageAlignmentValidator {

        /* 页大小：4KB = 4096 = 0x1000 */
        private static final long PAGE_SIZE = 0x1000;

        /**
         * tryParseValue
         *
         * @brief
         * 功能: 安全解析数值字符串。
         * @param[in] value 待解析或校验的属性值。
         * @return 解析后的长整型数值。
         * @scope internal
         */
        private static Long tryParseValue(String value) {
                if (value == null || value.trim().isEmpty()) {
                        return null;
                }
                try {
                        return parseValue(value.trim());
                } catch (NumberFormatException ex) {
                        return null;
                }
        }

        /* 存储元素的行号信息（使用元素的唯一标识作为key） */
        private static Map<String, Integer> elementLineNumbers = new HashMap<>();

        /* 通用扫描的属性名关键词（用于捕获可能遗漏的地址属性） */
        /* 策略： */
        /* - 特定检查（表73）已完整覆盖所有Size相关属性 */
        /* - 通用扫描专注于地址类属性，作为补充检查机制 */
        /* - 不包含"Size"和"MemorySize"，避免重复检查和误报 */
        private static final String[] ADDRESS_SIZE_KEYWORDS = {
                        "Address", /* 各种地址属性 */
                        "Base_", /* 基地址（Base_Address, Base_Offset等） */
                        "VirtualAddress", /* 虚拟地址 */
                        "PhysicalAddress" /* 物理地址 */
        };

        /* 通用扫描的排除列表（这些属性虽然包含关键词，但不需要页对齐检查） */
        private static final String[] EXCLUDED_ATTRIBUTES = {
                        "MaxMessageSize", /* 端口消息大小，不需要页对齐 */
                        "MaxNbMessage", /* 消息数量，不需要页对齐 */
                        "MaxNBLoggedMessages", /* 日志消息数量，不需要页对齐 */
                        "MaxNBInProgressMessages", /* 进行中消息数量，不需要页对齐 */
                        "Duration", /* 时间窗口持续时间，不需要页对齐 */
                        "Period", /* 周期，不需要页对齐 */
                        "Offset", /* 某些情况下的偏移量（需要具体分析） */
                        "Id", /* 标识符，不需要页对齐 */
                        "Identifier" /* 标识符，不需要页对齐 */
        };

        /**
         * 需要检查页对齐的属性配置（表 73）
         * 每个配置包含：描述、XPath表达式、属性名
         */
        private static final PageAlignmentRule[] ALIGNMENT_RULES = {
                        /* 核心操作系统内核配置 */
                        /* PR_27: 添加addressSpaceRegionAllocationUnit属性的页对齐检查 */
                        new PageAlignmentRule("逻辑空间分配单位",
                                        "//*[local-name()='CoreOSDescription']/*[local-name()='KernelConfiguration']/@addressSpaceRegionAllocationUnit"),

                        /* 核心操作系统内存大小 */
                        new PageAlignmentRule("核心操作系统Bss段大小",
                                        "//*[local-name()='CoreOSDescription']/*[local-name()='MemorySize']/@MemorySizeBss"),
                        new PageAlignmentRule("核心操作系统Data段大小",
                                        "//*[local-name()='CoreOSDescription']/*[local-name()='MemorySize']/@MemorySizeData"),
                        new PageAlignmentRule("核心操作系统Text段大小",
                                        "//*[local-name()='CoreOSDescription']/*[local-name()='MemorySize']/@MemorySizeText"),
                        new PageAlignmentRule("核心操作系统RoData段大小",
                                        "//*[local-name()='CoreOSDescription']/*[local-name()='MemorySize']/@MemorySizeRoData"),

                        /* 核心操作系统物理内存 */
                        new PageAlignmentRule("核心操作系统物理内存起始地址",
                                        "//*[local-name()='PhysicalMemory']/@Base_Address"),
                        new PageAlignmentRule("核心操作系统物理内存大小",
                                        "//*[local-name()='PhysicalMemory']/@Size"),

                        /* 内核内存区域 */
                        new PageAlignmentRule("内核内存空间大小",
                                        "//*[local-name()='kernelMemoryRegion']/@Size"),
                        new PageAlignmentRule("内核配置记录空间大小",
                                        "//*[local-name()='kernelConfigRecordRegion']/@Size"),
                        new PageAlignmentRule("内核页池大小",
                                        "//*[local-name()='kernelPgPool']/@Size"),
                        new PageAlignmentRule("端口缓冲区空间大小",
                                        "//*[local-name()='portRegion']/@Size"),
                        new PageAlignmentRule("健康监控日志记录空间大小",
                                        "//*[local-name()='hmLogRegion']/@Size"),
                        new PageAlignmentRule("驻留在核心操作系统的用户自定义空间大小",
                                        "//*[local-name()='kernelRegion']/@Size"),

                        /* RAM部署空间 */
                        new PageAlignmentRule("RAM部署空间起始物理地址",
                                        "//*[local-name()='ramPayloadRegion']/@Base_Address"),
                        new PageAlignmentRule("RAM部署空间大小",
                                        "//*[local-name()='ramPayloadRegion']/@Size"),

                        /* 共享库空间 */
                        new PageAlignmentRule("共享库空间起始物理地址",
                                        "//*[local-name()='libRegion']/@Base_Address"),
                        new PageAlignmentRule("共享库空间大小",
                                        "//*[local-name()='libRegion']/@Size"),

                        /* 用户内存空间 */
                        new PageAlignmentRule("用户内存空间起始物理地址",
                                        "//*[local-name()='userMemoryRegion']/@Base_Address"),
                        new PageAlignmentRule("用户内存空间大小",
                                        "//*[local-name()='userMemoryRegion']/@Size"),

                        /* 用户配置记录空间 */
                        new PageAlignmentRule("用户配置记录空间起始物理地址",
                                        "//*[local-name()='userConfigRecordRegion']/@Base_Address"),
                        new PageAlignmentRule("用户配置记录空间大小",
                                        "//*[local-name()='userConfigRecordRegion']/@Size"),

                        /* 内核IO空间 */
                        new PageAlignmentRule("内核IO空间起始物理地址",
                                        "//*[local-name()='kernelIO']/@Base_Address"),
                        new PageAlignmentRule("内核IO空间逻辑起始",
                                        "//*[local-name()='kernelIO']/@VirtualAddress"),
                        new PageAlignmentRule("内核IO空间大小",
                                        "//*[local-name()='kernelIO']/@Size"),

                        /* 共享IO空间 */
                        new PageAlignmentRule("共享IO空间起始物理地址",
                                        "//*[local-name()='sharedIO']/@Base_Address"),
                        new PageAlignmentRule("共享IO空间起始逻辑地址",
                                        "//*[local-name()='sharedIO']/@VirtualAddress"),
                        new PageAlignmentRule("共享IO空间大小",
                                        "//*[local-name()='sharedIO']/@Size"),

                        /* ROM部署空间 */
                        new PageAlignmentRule("ROM部署空间起始物理地址",
                                        "//*[local-name()='payloadMemory']/@Base_Address"),
                        new PageAlignmentRule("ROM部署空间大小",
                                        "//*[local-name()='payloadMemory']/@Size"),

                        /* 应用内存大小 */
                        new PageAlignmentRule("应用Text段大小",
                                        "//*[local-name()='Application']/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizeText"),
                        new PageAlignmentRule("应用RoData段大小",
                                        "//*[local-name()='Application']/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizeRoData"),
                        new PageAlignmentRule("应用Data段大小",
                                        "//*[local-name()='Application']/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizeData"),
                        new PageAlignmentRule("应用Bss段大小",
                                        "//*[local-name()='Application']/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizeBss"),
                        new PageAlignmentRule("应用PBss段大小",
                                        "//*[local-name()='Application']/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizePersistentBss"),
                        new PageAlignmentRule("应用PData段大小",
                                        "//*[local-name()='Application']/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizePersistentData"),

                        /* PR_30: 独立ApplicationDescription文件（根元素直接是ApplicationDescription） */
                        new PageAlignmentRule("应用Text段大小(独立文件)",
                                        "/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizeText"),
                        new PageAlignmentRule("应用RoData段大小(独立文件)",
                                        "/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizeRoData"),
                        new PageAlignmentRule("应用Data段大小(独立文件)",
                                        "/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizeData"),
                        new PageAlignmentRule("应用Bss段大小(独立文件)",
                                        "/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizeBss"),
                        new PageAlignmentRule("应用PBss段大小(独立文件)",
                                        "/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizePersistentBss"),
                        new PageAlignmentRule("应用PData段大小(独立文件)",
                                        "/*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']/@MemorySizePersistentData"),

                        /* 共享库内存大小 */
                        new PageAlignmentRule("共享库Text段大小",
                                        "//*[local-name()='SharedLibrary']/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizeText"),
                        new PageAlignmentRule("共享库RoData段大小",
                                        "//*[local-name()='SharedLibrary']/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizeRoData"),
                        new PageAlignmentRule("共享库Data段大小",
                                        "//*[local-name()='SharedLibrary']/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizeData"),
                        new PageAlignmentRule("共享库Bss段大小",
                                        "//*[local-name()='SharedLibrary']/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizeBss"),
                        new PageAlignmentRule("共享库PBss段大小",
                                        "//*[local-name()='SharedLibrary']/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizePersistentBss"),
                        new PageAlignmentRule("共享库PData段大小",
                                        "//*[local-name()='SharedLibrary']/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizePersistentData"),
                        new PageAlignmentRule("共享库虚拟地址",
                                        "//*[local-name()='SharedLibrary']/*[local-name()='SharedLibraryDescription']/@VirtualAddress"),

                        /* PR_30: 独立SharedLibraryDescription文件（根元素直接是SharedLibraryDescription） */
                        new PageAlignmentRule("共享库Text段大小(独立文件)",
                                        "/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizeText"),
                        new PageAlignmentRule("共享库RoData段大小(独立文件)",
                                        "/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizeRoData"),
                        new PageAlignmentRule("共享库Data段大小(独立文件)",
                                        "/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizeData"),
                        new PageAlignmentRule("共享库Bss段大小(独立文件)",
                                        "/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizeBss"),
                        new PageAlignmentRule("共享库PBss段大小(独立文件)",
                                        "/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizePersistentBss"),
                        new PageAlignmentRule("共享库PData段大小(独立文件)",
                                        "/*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']/@MemorySizePersistentData"),
                        new PageAlignmentRule("共享库虚拟地址(独立文件)",
                                        "/*[local-name()='SharedLibraryDescription']/@VirtualAddress"),

                        /* 共享数据 */
                        new PageAlignmentRule("共享数据大小",
                                        "//*[local-name()='SharedData']/*[local-name()='SharedDataDescription']/@Size"),
                        new PageAlignmentRule("共享数据逻辑地址",
                                        "//*[local-name()='SharedData']/*[local-name()='SharedDataDescription']/@VirtualAddress"),

                        /* PR_30: 独立SharedDataDescription文件（根元素直接是SharedDataDescription） */
                        new PageAlignmentRule("共享数据大小(独立文件)",
                                        "/*[local-name()='SharedDataDescription']/@Size"),
                        new PageAlignmentRule("共享数据逻辑地址(独立文件)",
                                        "/*[local-name()='SharedDataDescription']/@VirtualAddress"),

                        /* 分区引用共享数据 */
                        new PageAlignmentRule("分区引用共享数据大小",
                                        "//*[local-name()='Partition']/*[local-name()='PartitionDescription']/*[local-name()='SharedDataRegion']/@Size"),
                        new PageAlignmentRule("分区引用共享数据地址",
                                        "//*[local-name()='Partition']/*[local-name()='PartitionDescription']/*[local-name()='SharedDataRegion']/@Address"),

                        /* PR_51: 独立PartitionDescription文件（根元素直接是PartitionDescription） */
                        new PageAlignmentRule("分区引用共享数据大小(独立文件)",
                                        "/*[local-name()='PartitionDescription']/*[local-name()='SharedDataRegion']/@Size"),
                        new PageAlignmentRule("分区引用共享数据地址(独立文件)",
                                        "/*[local-name()='PartitionDescription']/*[local-name()='SharedDataRegion']/@Address"),

                        /* Payload部署地址 */
                        new PageAlignmentRule("核心部署起始地址",
                                        "//*[local-name()='CoreOSPayload']/@Base_Address"),
                        new PageAlignmentRule("共享库部署起始地址",
                                        "//*[local-name()='SharedLibraryPayload']/@Base_Address"),
                        new PageAlignmentRule("配置记录部署起始地址",
                                        "//*[local-name()='ConfigRecordPayload']/@Base_Address"),
                        new PageAlignmentRule("分区部署起始地址",
                                        "//*[local-name()='PartitionPayload']/@Base_Address")
        };

        /**
         * 页对齐规则类
         */
        private static class PageAlignmentRule {
                final String description;
                final String xpath;

                PageAlignmentRule(String description, String xpath) {
                        this.description = description;
                        this.xpath = xpath;
                }
        }

        /**
         * collectLineNumbers
         *
         * @brief
         * 功能: 使用 SAX 收集 XML 元素行号信息。
         * @param[in] xmlFile XML 配置文件。
         * @param[in] fileName XML 文件名。
         * @return 字符串结果。
         * @scope internal
         */
        private static void collectLineNumbers(File xmlFile, String fileName) {
                /* 不清空之前的行号信息，支持多文件累积 */

                try {
                        SAXParserFactory factory = SAXParserFactory.newInstance();
                        factory.setNamespaceAware(true);
                        SAXParser saxParser = factory.newSAXParser();

                        DefaultHandler handler = new DefaultHandler() {
                                private Locator locator;
                                private List<String> elementNames = new ArrayList<>();
                                private Map<String, Integer> elementCounts = new HashMap<>();

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
                                public void startElement(String uri, String localName, String qName,
                                                Attributes attributes)
                                                throws SAXException {
                                        if (locator != null) {
                                                int lineNumber = locator.getLineNumber();

                                                /* 构建元素路径 */
                                                String elementName = localName != null && !localName.isEmpty()
                                                                ? localName
                                                                : qName;
                                                elementNames.add(elementName);

                                                /* 构建唯一的元素路径（包含索引） */
                                                String currentPath = String.join("/", elementNames);

                                                /* 为同名元素添加索引 */
                                                int count = elementCounts.getOrDefault(currentPath, 0);
                                                elementCounts.put(currentPath, count + 1);
                                                String uniquePath = fileName + "::" + currentPath + "[" + count + "]";

                                                /* 保存行号（key中包含文件名） */
                                                elementLineNumbers.put(uniquePath, lineNumber);

                                                /* 保存简单的元素名称 key（作为后备查找方式） */
                                                String simpleKey = fileName + "::" + elementName;
                                                elementLineNumbers.put(simpleKey, lineNumber);

                                                /* 保存完整路径 key（不带索引，用于路径匹配） */
                                                String pathKey = fileName + "::" + currentPath;
                                                elementLineNumbers.put(pathKey, lineNumber);

                                                /* 如果元素有Name属性，使用Name作为额外的key */
                                                String nameAttr = attributes.getValue("Name");
                                                if (nameAttr != null && !nameAttr.isEmpty()) {
                                                        String keyWithName = fileName + "::" + elementName + "[@Name='"
                                                                        + nameAttr + "']";
                                                        elementLineNumbers.put(keyWithName, lineNumber);
                                                }

                                                /* 如果元素有PoolName属性，使用PoolName作为额外的key */
                                                String poolNameAttr = attributes.getValue("PoolName");
                                                if (poolNameAttr != null && !poolNameAttr.isEmpty()) {
                                                        String keyWithPoolName = fileName + "::" + elementName
                                                                        + "[@PoolName='" + poolNameAttr
                                                                        + "']";
                                                        elementLineNumbers.put(keyWithPoolName, lineNumber);
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
                                        if (!elementNames.isEmpty()) {
                                                elementNames.remove(elementNames.size() - 1);
                                        }
                                }
                        };

                        org.xml.sax.InputSource inputSource = XmlParser.preprocessXmlFile(xmlFile);
                        saxParser.parse(inputSource, handler);

                } catch (Exception e) {
                        Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                                        "无法收集行号信息: " + e.getMessage());
                }
        }

        /**
         * getLineInfo
         *
         * @brief
         * 功能: 获取元素对应的行号信息。
         * @param[in] element XML 元素。
         * @param[in] fileName XML 文件名。
         * @return 字符串结果。
         * @scope internal
         */
        private static String getLineInfo(Element element, String fileName) {
                if (element == null) {
                        return "unknown";
                }

                String elementName = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();

                /* 方法1：尝试通过Name属性查找 */
                String nameAttr = element.getAttribute("Name");
                if (nameAttr != null && !nameAttr.isEmpty()) {
                        String key = fileName + "::" + elementName + "[@Name='" + nameAttr + "']";
                        if (elementLineNumbers.containsKey(key)) {
                                return String.valueOf(elementLineNumbers.get(key));
                        }
                }

                /* 方法2：尝试通过PoolName属性查找 */
                String poolNameAttr = element.getAttribute("PoolName");
                if (poolNameAttr != null && !poolNameAttr.isEmpty()) {
                        String key = fileName + "::" + elementName + "[@PoolName='" + poolNameAttr + "']";
                        if (elementLineNumbers.containsKey(key)) {
                                return String.valueOf(elementLineNumbers.get(key));
                        }
                }

                /* 方法3：尝试通过元素路径查找（从根到当前元素） */
                String path = buildElementPath(element);
                if (path != null) {
                        String pathKey = fileName + "::" + path;
                        if (elementLineNumbers.containsKey(pathKey)) {
                                return String.valueOf(elementLineNumbers.get(pathKey));
                        }
                }

                /* 方法4：尝试通过简单的元素名称查找（最后的后备方案） */
                String simpleKey = fileName + "::" + elementName;
                if (elementLineNumbers.containsKey(simpleKey)) {
                        return String.valueOf(elementLineNumbers.get(simpleKey));
                }

                return "unknown";
        }

        /**
         * buildElementPath
         *
         * @brief
         * 功能: 构建元素在 DOM 树中的路径。
         * @param[in] element XML 元素。
         * @return 字符串结果。
         * @scope internal
         */
        private static String buildElementPath(Element element) {
                if (element == null) {
                        return null;
                }

                List<String> pathElements = new ArrayList<>();
                org.w3c.dom.Node current = element;

                while (current != null && current.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        Element elem = (Element) current;
                        String name = elem.getLocalName() != null ? elem.getLocalName() : elem.getNodeName();
                        pathElements.add(0, name); /* 添加到列表开头 */
                        current = current.getParentNode();
                }

                if (pathElements.isEmpty()) {
                        return null;
                }

                return String.join("/", pathElements);
        }

        /**
         * validatePageAlignment
         *
         * @brief
         * 功能: 校验 XML 文件中地址和大小属性的页对齐约束。
         * @param[in] xmlFile XML 配置文件。
         * @param[out] errorMessages 校验错误信息列表。
         * @return 校验或判断通过返回 true，否则返回 false。
         * @scope external
         */
        public static boolean validatePageAlignment(File xmlFile, List<String> errorMessages) {
                if (xmlFile == null || !xmlFile.exists()) {
                        return true; /* 文件不存在，跳过验证 */
                }

                List<String> alignmentErrors = new ArrayList<>();
                boolean allValid = true;
                String fileName = xmlFile.getName();

                /* 用于跟踪已检查的属性，避免重复报错 */
                Set<String> checkedAttributes = new HashSet<>();

                try {
                        /* 首先使用SAX解析器收集行号信息 */
                        collectLineNumbers(xmlFile, fileName);

                        /* 解析XML文档（预处理重复属性/环境变量） */
                        Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, false);

                        XPathFactory xPathFactory = XPathFactory.newInstance();
                        XPath xpath = xPathFactory.newXPath();

                        /* 第一阶段：特定属性检查（表73） */
                        for (PageAlignmentRule rule : ALIGNMENT_RULES) {
                                try {
                                        NodeList nodes = (NodeList) xpath.evaluate(rule.xpath, doc,
                                                        XPathConstants.NODESET);

                                        for (int i = 0; i < nodes.getLength(); i++) {
                                                Attr attr = (Attr) nodes.item(i);
                                                String value = attr.getNodeValue();

                                                /* 记录已检查的属性（用于避免通用检查重复） */
                                                Element parentElement = attr.getOwnerElement();
                                                String attrKey = getAttributeKey(parentElement, attr);
                                                checkedAttributes.add(attrKey);

                                                if (value != null && !value.trim().isEmpty()) {
                                                        Long numericValue = tryParseValue(value);
                                                        if (numericValue == null) {
                                                                continue;
                                                        }

                                                        if ((numericValue % PAGE_SIZE) != 0) {
                                                                String elementName = parentElement.getLocalName();
                                                                String lineInfo = getLineInfo(parentElement, fileName);

                                                                /* 004878: [File: {fileName}, Line: {lineNumber}] */
                                                                /* the element {element name }  address {value} */
                                                                /* configuration not page-aligned. */
                                                                String errorMsg = String.format(
                                                                                "[File: %s, Line: %s] the element %s  address %s configuration not page-aligned.",
                                                                                fileName, lineInfo, elementName, value);

                                                                alignmentErrors.add(errorMsg);
                                                                Log.consistencyError(CodeConstant.ERROR_ADDRESS_CONFIG_NOT_PAGE_ALIGNED,
                                                                                errorMsg);
                                                                allValid = false;
                                                        }
                                                }
                                        }
                                } catch (Exception e) {
                                        /* 某个XPath表达式查询失败，记录并继续 */
                                        Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                                                        "Failed to check page alignment for rule: " + rule.description
                                                                        + " - " + e.getMessage());
                                }
                        }

                        /* 第二阶段：通用属性检查（补充扫描） */
                        boolean genericValid = checkGenericPageAlignment(doc, xpath, fileName, alignmentErrors,
                                        checkedAttributes);
                        allValid = allValid && genericValid;

                } catch (Exception e) {
                        String errorMsg = "Page alignment validation failed due to exception: " + e.getMessage();
                        alignmentErrors.add(errorMsg);
                        Log.consistencyError(CodeConstant.ERROR_ADDRESS_CONFIG_NOT_PAGE_ALIGNED, errorMsg);
                        allValid = false;
                }

                /* 将所有页对齐错误添加到输出错误列表 */
                if (errorMessages != null) {
                        errorMessages.addAll(alignmentErrors);
                }

                return allValid;
        }



        /**
         * parseValue
         *
         * @brief
         * 功能: 解析十进制、十六进制或八进制数值。
         * @param[in] value 待解析或校验的属性值。
         * @return 解析后的长整型数值。
         * @scope internal
         */
        private static long parseValue(String value) throws NumberFormatException {
                if (value == null || value.trim().isEmpty()) {
                        throw new NumberFormatException("Value is null or empty");
                }

                value = value.trim();

                /* 十六进制：0x 或 0X 前缀 */
                if (value.toLowerCase().startsWith("0x")) {
                        return Long.parseLong(value.substring(2), 16);
                }
                /* 八进制：0 前缀（但不是单个0） */
                else if (value.startsWith("0") && value.length() > 1) {
                        return Long.parseLong(value.substring(1), 8);
                }
                /* 十进制 */
                else {
                        return Long.parseLong(value);
                }
        }

        /**
         * checkGenericPageAlignment
         *
         * @brief
         * 功能: 执行通用地址类属性页对齐检查。
         * @param[in] doc XML DOM 文档。
         * @param[in] xpath XPath 查询对象。
         * @param[in] fileName XML 文件名。
         * @param[out] alignmentErrors 页对齐错误信息列表。
         * @param[out] checkedAttributes 已检查属性集合。
         * @return 校验或判断通过返回 true，否则返回 false。
         * @scope internal
         */
        private static boolean checkGenericPageAlignment(Document doc, XPath xpath, String fileName,
                        List<String> alignmentErrors, Set<String> checkedAttributes) {
                boolean allValid = true;

                try {
                        /* 查询所有属性 */
                        NodeList allAttributes = (NodeList) xpath.evaluate("//@*", doc, XPathConstants.NODESET);

                        for (int i = 0; i < allAttributes.getLength(); i++) {
                                Attr attr = (Attr) allAttributes.item(i);
                                String attrName = attr.getLocalName();

                                /* 检查属性名是否包含地址/大小关键词 */
                                if (containsKeyword(attrName)) {
                                        Element parentElement = attr.getOwnerElement();
                                        String attrKey = getAttributeKey(parentElement, attr);

                                        /* 跳过已经在特定检查中处理过的属性 */
                                        if (checkedAttributes.contains(attrKey)) {
                                                continue;
                                        }

                                        String value = attr.getNodeValue();

                                        if (value != null && !value.trim().isEmpty()) {
                                                Long numericValue = tryParseValue(value);
                                                if (numericValue == null) {
                                                        continue;
                                                }

                                                if ((numericValue % PAGE_SIZE) != 0) {
                                                        String elementName = parentElement.getLocalName();
                                                        String lineInfo = getLineInfo(parentElement, fileName);

                                                        /* 004878: [File: {fileName}, Line: {lineNumber}] */
                                                        /* the element {element name }  address {value} */
                                                        /* configuration not page-aligned. */
                                                        String errorMsg = String.format(
                                                                        "[File: %s, Line: %s] the element %s  address %s configuration not page-aligned.",
                                                                        fileName, lineInfo, elementName, value);

                                                        alignmentErrors.add(errorMsg);
                                                        Log.consistencyError(CodeConstant.ERROR_ADDRESS_CONFIG_NOT_PAGE_ALIGNED,
                                                                        errorMsg);
                                                        allValid = false;
                                                }
                                        }
                                }
                        }

                } catch (Exception e) {
                        Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                                        "Generic page alignment check failed: " + e.getMessage());
                }

                return allValid;
        }

        /**
         * containsKeyword
         *
         * @brief
         * 功能: 判断属性名称是否包含需检查的关键词。
         * @param[in] attributeName 属性名称。
         * @return 校验或判断通过返回 true，否则返回 false。
         * @scope internal
         */
        private static boolean containsKeyword(String attributeName) {
                if (attributeName == null) {
                        return false;
                }

                /* 首先检查是否在排除列表中 */
                for (String excluded : EXCLUDED_ATTRIBUTES) {
                        if (attributeName.equals(excluded)) {
                                return false; /* 在排除列表中，不检查 */
                        }
                }

                /* 然后检查是否包含关键词 */
                for (String keyword : ADDRESS_SIZE_KEYWORDS) {
                        if (attributeName.contains(keyword)) {
                                return true;
                        }
                }

                return false;
        }

        /**
         * getAttributeKey
         *
         * @brief
         * 功能: 构建属性唯一标识键。
         * @param[in] element XML 元素。
         * @param[in] attr XML 属性。
         * @return 字符串结果。
         * @scope internal
         */
        private static String getAttributeKey(Element element, Attr attr) {
                StringBuilder key = new StringBuilder();

                /* 构建元素路径 */
                Element current = element;
                while (current != null && current.getParentNode() instanceof Element) {
                        key.insert(0, "/" + current.getLocalName());
                        current = (Element) current.getParentNode();
                }
                if (key.length() == 0) {
                        key.append("/").append(element.getLocalName());
                }

                /* 添加属性名和值 */
                key.append("@").append(attr.getLocalName());
                key.append("=").append(attr.getNodeValue());

                return key.toString();
        }
}
