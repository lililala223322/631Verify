/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: MiscellaneousConstraintValidator.java
 * 
 * 描述    : 校验配置文件中的其他综合约束。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-26  开发人员  使杂项校验触发的1018使用完整文件路径。
 *     2026-07-26  开发人员  统一杂项正确性校验异常的1018输出格式。
 *     2026-07-27  开发人员  按B-02补齐内存、端口和共享数据一致性校验错误码输出。
 *     2026-07-30  开发人员  按B-03补齐Online、系统共享库、BmpSchedule和端口数量一致性校验。
 */

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.layers.parsing.XmlParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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
import java.util.*;

/**
 * @brief
 * 功能: 校验配置文件中的其他综合约束。
 */
public class MiscellaneousConstraintValidator {

    /* 当前一次校验的“元素路径/元素名 -> 来源文件名”映射（用于XInclude展开后BaseURI丢失的场景） */
    private static final ThreadLocal<Map<String, String>> CURRENT_FILE_NAME_MAP = new ThreadLocal<>();

    private static final int LOG_ENTRY_SIZE = 456; /* 健康监控日志每条456字节 */
    private static final int CORE_HM_TASK_MAX_COUNT = 16; /* 文档约束：CoreHMTask 最大 16 */
    private static final int CORE_MAX_COUNT_IN_CORE_HM_TASK = 16; /* 文档约束：CoreHMTask/Core 最大 16 */
    private static final int PORTS_MAX_COUNT = 5120; /* 文档约束：每个 Ports 容器内 QueuingPort + SamplingPort 最大 5120 */

    /**
     * validateMiscellaneousConstraints
     *
     * @brief
     * 功能: 校验 XML 配置中的综合约束。
     * @param[in] xmlFile XML 配置文件。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateMiscellaneousConstraints(File xmlFile, List<String> errorMessages) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true; /* 文件不存在，跳过验证 */
        }

        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();

        try {
            /* 第一步：使用DOM解析器（不展开XInclude）来找出所有被引用的文件 */
            Document docNoXInclude = XmlParser.parseXmlWithEnvSupport(xmlFile, false);

            /* 收集所有被引用的文件 */
            List<File> allFiles = new ArrayList<>();
            allFiles.add(xmlFile); /* 主文件 */
            collectIncludedFiles(docNoXInclude, xmlFile, allFiles);

            /* 为所有文件建立行号映射，并同时建立“键->来源文件名”的映射。 */
            /* 目的：当DOM节点没有BaseURI/DocumentURI时，仍能回溯到被包含文件（如coreOS.xml）。 */
            Map<String, Integer> lineNumberMap = new HashMap<>();
            Map<String, String> fileNameMap = new HashMap<>();
            for (File file : allFiles) {
                SourceMaps maps = buildSourceMaps(file);
                lineNumberMap.putAll(maps.lineNumberMap);
                fileNameMap.putAll(maps.fileNameMap);
            }

            /* 保存到ThreadLocal，供 getFileNameFromElement 兜底使用 */
            CURRENT_FILE_NAME_MAP.set(fileNameMap);

            /* 第二步：使用支持环境变量的XML解析，获取完整的DOM树 */
            Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, true);

            /* 执行所有约束检查 */
            boolean allValid = true;

            allValid &= validatePortDirectionProtocol(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validatePartitionMemorySize(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validateSharedDataVirtualAddress(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validateSharedDataPartitionSpace(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validateKernelMemorySize(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validateLibRegionSize(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validatePortRegionSize(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validateHmLogRegionSize(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validateOnlinePartitionPayloads(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validateSystemSharedLibrary(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validateBmpSchedulePartitionCoreUniqueness(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validateCoreHMTaskCount(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validateCoreCountInCoreHMTask(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validatePortsCount(doc, xmlFile, lineNumberMap, localErrors);
            allValid &= validateCoreOSChildElementCounts(doc, xmlFile, lineNumberMap, localErrors);

            return allValid;

        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            localErrors.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        } finally {
            CURRENT_FILE_NAME_MAP.remove();
        }
    }

    /**
     * validatePortDirectionProtocol
     *
     * @brief
     * 功能: 校验端口方向与协议类型匹配关系。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validatePortDirectionProtocol(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 查找所有端口（队列端口和采样端口） */
            /* 包括：应用、分区（Partition）、虚分区（PseudoPartition） */
            /* 使用 local-name() 来忽略命名空间 */
            String portXPath = "//*[local-name()='QueuingPort'] | " +
                    "//*[local-name()='SamplingPort']";

            NodeList ports = (NodeList) xpath.evaluate(portXPath, doc, XPathConstants.NODESET);
            boolean allValid = true;

            for (int i = 0; i < ports.getLength(); i++) {
                Element port = (Element) ports.item(i);
                String direction = port.getAttribute("Direction");
                String protocolTypeStr = port.getAttribute("ProtocolType");
                String portName = port.getAttribute("Name");



                if (direction.isEmpty() || protocolTypeStr.isEmpty()) {
                    continue; /* 跳过缺少属性的端口 */
                }

                int protocolType = parseIntSafe(protocolTypeStr);

                /* 验证匹配规则 */
                boolean valid = false;
                if ("SOURCE".equalsIgnoreCase(direction)) {
                    /* 根据数据词典：SOURCE端口只能使用ProtocolType 0或1 */
                    valid = (protocolType == 0 || protocolType == 1);
                } else if ("DESTINATION".equalsIgnoreCase(direction)) {
                    valid = (protocolType == 2);
                }

                if (!valid) {
                    int lineNumber = getLineNumber(port, lineNumberMap);
                    String fileName = getFileNameFromElement(port, xmlFile);
                    if (lineNumber < 1) {
                        lineNumber = 1;
                    }

                    String formatted = String.format(
                            "The port direction %sof port%sdoes not match the port protocol type%s",
                            direction,
                            portName,
                            protocolTypeStr);

                    errorMessages.add(formatted);
                    Log.error(CodeConstant.ERROR_PORT_DIRECTION_PROTOCOL_TYPE_MISMATCH, formatted);
                    allValid = false;
                }
            }

            return allValid;

        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            errorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }

    /**
     * validatePartitionMemorySize
     *
     * @brief
     * 功能: 校验分区内存配置满足容量约束。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validatePartitionMemorySize(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 使用 local-name() 来忽略命名空间 */
            NodeList partitions = (NodeList) xpath.evaluate("//*[local-name()='Partition']", doc,
                    XPathConstants.NODESET);
            boolean allValid = true;

            for (int i = 0; i < partitions.getLength(); i++) {
                Element partition = (Element) partitions.item(i);
                String partitionName = partition.getAttribute("Name");

                /* 获取RequiredMemorySize (使用 local-name) */
                NodeList settingsList = (NodeList) xpath.evaluate(".//*[local-name()='Settings']", partition,
                        XPathConstants.NODESET);
                if (settingsList.getLength() == 0) {
                    continue;
                }

                Element settings = (Element) settingsList.item(0);
                long requiredSize = parseLongSafe(settings.getAttribute("RequiredMemorySize"));

                /* 计算应用内存 */
                long appMemory = calculateApplicationMemory(partition, xpath, doc);

                /* 计算共享库内存（除text和rodata） */
                long sharedLibMemory = calculateSharedLibraryMemory(partition, xpath, doc);

                long totalRequired = appMemory + sharedLibMemory;

                if (requiredSize <= totalRequired) {
                    int lineNumber = getLineNumber(settings, lineNumberMap);
                    String fileName = getFileNameFromElement(settings, xmlFile);
                    if (lineNumber < 1) {
                        lineNumber = 1;
                    }

                    String formatted = String.format(
                            "The RequiredMemorySize value %sof the partition%s is less than the sum%sof the application and shared libraries[excluding text,rodata]referenced by it",
                            String.valueOf(requiredSize),
                            partitionName,
                            String.valueOf(totalRequired));

                    errorMessages.add(formatted);
                    Log.error(CodeConstant.ERROR_PARTITION_REQUIRED_MEMORY_SIZE_TOO_SMALL, formatted);
                    allValid = false;
                }
            }

            return allValid;

        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            errorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }

    /**
     * validateSharedDataVirtualAddress
     *
     * @brief
     * 功能: 校验共享数据虚拟地址配置有效性。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateSharedDataVirtualAddress(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 006365：虚拟分区空间参数定义在 KernelConfiguration 上，不能从 userMemoryRegion 读取。 */
            NodeList kernelConfigList = (NodeList) xpath.evaluate(
                    "//*[local-name()='CoreOSDescription']/*[local-name()='KernelConfiguration']",
                    doc, XPathConstants.NODESET);

            if (kernelConfigList.getLength() == 0) {
                return true; /* 没有核心配置，跳过检查 */
            }

            Element kernelConfig = (Element) kernelConfigList.item(0);
            String partitionSpaceSizeStr = kernelConfig.getAttribute("partitionSpaceSize");

            if (partitionSpaceSizeStr == null || partitionSpaceSizeStr.isEmpty()) {
                return true; /* partitionSpaceSize不存在，跳过此检查 */
            }

            long partitionVirtualAddress = parseLongSafe(kernelConfig.getAttribute("partitionVirtualAddress"));
            long partitionSpaceSize = parseLongSafe(partitionSpaceSizeStr);

            /* 获取最大的分区RequiredMemorySize */
            long maxRequiredMemorySize = getMaxPartitionRequiredMemorySize(doc, xpath);

            /* 计算有效范围 */
            long rangeStart = partitionVirtualAddress + maxRequiredMemorySize;
            long rangeEnd = partitionVirtualAddress + partitionSpaceSize;

            /* 检查所有SharedData (使用 local-name) */
            NodeList sharedDataList = (NodeList) xpath.evaluate(
                    "//*[local-name()='SharedDataRegions']/*[local-name()='SharedData']", doc, XPathConstants.NODESET);

            boolean allValid = true;

            for (int i = 0; i < sharedDataList.getLength(); i++) {
                Element sharedData = (Element) sharedDataList.item(i);
                String virtualAddrStr = sharedData.getAttribute("VirtualAddress");
                String sizeStr = sharedData.getAttribute("Size");

                /* 标准配置把 VirtualAddress/Size 放在 SharedDataDescription 中；兼容直接挂在
                 * SharedData 上的旧格式，避免只读取容器属性而漏检。 */
                if (virtualAddrStr == null || virtualAddrStr.isEmpty()) {
                    virtualAddrStr = (String) xpath.evaluate(
                            "./*[local-name()='SharedDataDescription']/@VirtualAddress",
                            sharedData, XPathConstants.STRING);
                }
                if (sizeStr == null || sizeStr.isEmpty()) {
                    sizeStr = (String) xpath.evaluate(
                            "./*[local-name()='SharedDataDescription']/@Size",
                            sharedData, XPathConstants.STRING);
                }

                if (virtualAddrStr != null && !virtualAddrStr.isEmpty()) {
                    long virtualAddr = parseLongSafe(virtualAddrStr);
                    if (virtualAddr != 0) {
                        long size = parseLongSafe(sizeStr);
                        long endAddr = virtualAddr + size;

                        if (virtualAddr < rangeStart || endAddr > rangeEnd) {
                            int lineNumber = getLineNumber(sharedData, lineNumberMap);
                            String fileName = getFileNameFromElement(sharedData, xmlFile);
                            if (lineNumber < 1) {
                                lineNumber = 1;
                            }

                            String minHex = String.format("0x%X", rangeStart);
                            String maxHex = String.format("0x%X", rangeEnd);

                            String formatted = String.format(
                                    "[File: %s, Line: %d] The virtual address of shared data %s is not within the range [%s,%s].",
                                    fileName,
                                    lineNumber,
                                    sharedData.getAttribute("Name"),
                                    minHex,
                                    maxHex);

                            errorMessages.add(formatted);
                            Log.error(CodeConstant.ERROR_SHARED_DATA_VIRTUAL_ADDRESS_OUT_OF_PARTITION_SPACE, formatted);
                            allValid = false;
                        }
                    }
                }
            }

            return allValid;

        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            errorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }

    /**
     * validateSharedDataPartitionSpace
     *
     * @brief
     * 功能: 校验共享数据分区空间引用关系。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateSharedDataPartitionSpace(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 检查是否有SharedData的VirtualAddress为空或0 (使用 local-name) */
            NodeList sharedDataList = (NodeList) xpath.evaluate(
                    "//*[local-name()='SharedDataRegions']/*[local-name()='SharedData']", doc, XPathConstants.NODESET);

            boolean hasUnaddressedSharedData = false;
            Element firstUnaddressedSharedData = null;
            for (int i = 0; i < sharedDataList.getLength(); i++) {
                Element sharedData = (Element) sharedDataList.item(i);
                String virtualAddrStr = sharedData.getAttribute("VirtualAddress");

                if (virtualAddrStr == null || virtualAddrStr.isEmpty() || parseLongSafe(virtualAddrStr) == 0) {
                    hasUnaddressedSharedData = true;
                    firstUnaddressedSharedData = sharedData;
                    break;
                }
            }

            /* 如果有未指定地址的共享数据，检查 KernelConfiguration 的 partitionSpaceSize */
            if (hasUnaddressedSharedData) {
                NodeList kernelConfigList = (NodeList) xpath.evaluate(
                        "//*[local-name()='CoreOSDescription']/*[local-name()='KernelConfiguration']",
                        doc, XPathConstants.NODESET);

                if (kernelConfigList.getLength() == 0) {
                    Element anchor = firstUnaddressedSharedData;
                    int lineNumber = (anchor != null) ? getLineNumber(anchor, lineNumberMap) : -1;
                    if (lineNumber < 1) {
                        lineNumber = 1;
                    }
                    String fileName = (anchor != null) ? getFileNameFromElement(anchor, xmlFile) : xmlFile.getName();

                    String formatted = "The VirtualAddress of the shared data and partitionSpaceSize are both not configured;the operating system cannot automatically allocate a virtual address";

                    errorMessages.add(formatted);
                    Log.error(CodeConstant.ERROR_SHARED_DATA_VIRTUAL_ADDRESS_AUTO_ALLOC_UNAVAILABLE, formatted);
                    return false;
                }

                Element kernelConfig = (Element) kernelConfigList.item(0);
                String partitionSpaceSizeStr = kernelConfig.getAttribute("partitionSpaceSize");

                if (partitionSpaceSizeStr == null || partitionSpaceSizeStr.isEmpty()) {
                    int lineNumber = getLineNumber(kernelConfig, lineNumberMap);
                    String fileName = getFileNameFromElement(kernelConfig, xmlFile);
                    if (lineNumber < 1) {
                        lineNumber = 1;
                    }

                    String formatted = "The VirtualAddress of the shared data and partitionSpaceSize are both not configured;the operating system cannot automatically allocate a virtual address";

                    errorMessages.add(formatted);
                    Log.error(CodeConstant.ERROR_SHARED_DATA_VIRTUAL_ADDRESS_AUTO_ALLOC_UNAVAILABLE, formatted);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            errorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }

    /**
     * validateKernelMemorySize
     *
     * @brief
     * 功能: 校验核心内存区域大小约束。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateKernelMemorySize(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 获取kernelMemoryRegion/@Size (使用 local-name，注意它在 PhysicalMemory 下) */
            NodeList kernelMemList = (NodeList) xpath.evaluate(
                    "//*[local-name()='CoreOSDescription']//*[local-name()='HardwareConfiguration']//*[local-name()='PhysicalMemory']/*[local-name()='kernelMemoryRegion']",
                    doc, XPathConstants.NODESET);

            if (kernelMemList.getLength() == 0) {
                return true; /* 没有配置kernelMemoryRegion，跳过检查 */
            }

            Element kernelMem = (Element) kernelMemList.item(0);
            long kernelRegionSize = parseLongSafe(kernelMem.getAttribute("Size"));

            /* 计算CoreOS的所有内存段总和 (使用 local-name，MemorySize 直接在 CoreOSDescription 下) */
            NodeList memorySizeList = (NodeList) xpath.evaluate(
                    "//*[local-name()='CoreOSDescription']/*[local-name()='MemorySize']",
                    doc, XPathConstants.NODESET);

            if (memorySizeList.getLength() == 0) {
                return true; /* 没有MemorySize配置，跳过检查 */
            }

            Element memorySize = (Element) memorySizeList.item(0);
            long coreOSMemoryTotal = sumMemorySizeChildren(memorySize);

            if (kernelRegionSize < coreOSMemoryTotal) {
                int lineNumber = getLineNumber(kernelMem, lineNumberMap);
                String fileName = getFileNameFromElement(kernelMem, xmlFile);
                if (lineNumber < 1) {
                    lineNumber = 1;
                }

                String formatted = String.format(
                        "The size attribute value %sof kernelMemoryRegion is less than the sum %sof segments under CoreOSDescription/MemorySize.",
                        String.valueOf(kernelRegionSize),
                        String.valueOf(coreOSMemoryTotal));

                errorMessages.add(formatted);
                Log.error(CodeConstant.ERROR_KERNEL_MEMORY_REGION_SIZE_TOO_SMALL, formatted);
                return false;
            }

            return true;

        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            errorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }

    /**
     * validateLibRegionSize
     *
     * @brief
     * 功能: 校验共享库区域大小约束。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateLibRegionSize(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 获取libRegion/@Size (使用 local-name，注意它在 PhysicalMemory 下) */
            NodeList libRegionList = (NodeList) xpath.evaluate(
                    "//*[local-name()='CoreOSDescription']//*[local-name()='HardwareConfiguration']//*[local-name()='PhysicalMemory']/*[local-name()='libRegion']",
                    doc, XPathConstants.NODESET);

            if (libRegionList.getLength() == 0) {
                return true; /* 没有配置libRegion，跳过检查 */
            }

            Element libRegion = (Element) libRegionList.item(0);
            long libRegionSize = parseLongSafe(libRegion.getAttribute("Size"));

            /* 计算所有SharedLibrary的内存总和 (使用 local-name) */
            NodeList sharedLibList = (NodeList) xpath.evaluate(
                    "//*[local-name()='SharedLibraryRegions']/*[local-name()='SharedLibrary']//*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']",
                    doc, XPathConstants.NODESET);

            long totalLibMemory = 0;
            for (int i = 0; i < sharedLibList.getLength(); i++) {
                Element memSize = (Element) sharedLibList.item(i);

                /* 006368：每个 SharedLibrary 实例都代表一份空间需求；来源文件相同
                 * 不等于配置实例相同，不能按文件名去重。 */
                totalLibMemory += sumMemorySizeChildren(memSize);
            }

            if (libRegionSize < totalLibMemory) {
                int lineNumber = getLineNumber(libRegion, lineNumberMap);
                String fileName = getFileNameFromElement(libRegion, xmlFile);
                if (lineNumber < 1) {
                    lineNumber = 1;
                }

                String formatted = String.format(
                        "The size attribute value %sof libRegion is less than the sum %sof the space required by all shared libraries.",
                        String.valueOf(libRegionSize),
                        String.valueOf(totalLibMemory));

                errorMessages.add(formatted);
                Log.error(CodeConstant.ERROR_LIB_REGION_SIZE_TOO_SMALL, formatted);
                return false;
            }

            return true;

        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            errorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }

    /**
     * validatePortRegionSize
     *
     * @brief
     * 功能: 校验端口区域大小约束。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validatePortRegionSize(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 获取portRegion/@Size (使用 local-name，注意它在 PhysicalMemory 下) */
            NodeList portRegionList = (NodeList) xpath.evaluate(
                    "//*[local-name()='CoreOSDescription']//*[local-name()='HardwareConfiguration']//*[local-name()='PhysicalMemory']/*[local-name()='portRegion']",
                    doc, XPathConstants.NODESET);

            if (portRegionList.getLength() == 0) {
                return true; /* 没有配置portRegion，跳过检查 */
            }

            Element portRegion = (Element) portRegionList.item(0);
            long portRegionSize = parseLongSafe(portRegion.getAttribute("Size"));

            long totalPortMemory = 0;
            long totalQueuingPortMemory = 0;
            long totalSamplingPortMemory = 0;

            /* 计算队列端口所需空间 */
            /* 使用 local-name() 来忽略命名空间 */
            String queuingXPath = "//*[local-name()='QueuingPort']";
            NodeList queuingPorts = (NodeList) xpath.evaluate(queuingXPath, doc, XPathConstants.NODESET);

            for (int i = 0; i < queuingPorts.getLength(); i++) {
                Element port = (Element) queuingPorts.item(i);
                long maxMsgSize = parseLongSafe(port.getAttribute("MaxMessageSize"));
                long maxNbMsg = parseLongSafe(port.getAttribute("MaxNbMessage"));
                long mem = maxMsgSize * maxNbMsg;
                totalQueuingPortMemory += mem;
                totalPortMemory += mem;
            }

            /* 计算采样端口所需空间 */
            /* 使用 local-name() 来忽略命名空间 */
            String samplingXPath = "//*[local-name()='SamplingPort']";
            NodeList samplingPorts = (NodeList) xpath.evaluate(samplingXPath, doc, XPathConstants.NODESET);

            for (int i = 0; i < samplingPorts.getLength(); i++) {
                Element port = (Element) samplingPorts.item(i);
                long maxMsgSize = parseLongSafe(port.getAttribute("MaxMessageSize"));
                totalSamplingPortMemory += maxMsgSize;
                totalPortMemory += maxMsgSize;
            }

            if (portRegionSize < totalPortMemory) {
                int lineNumber = getLineNumber(portRegion, lineNumberMap);
                String fileName = getFileNameFromElement(portRegion, xmlFile);
                if (lineNumber < 1) {
                    lineNumber = 1;
                }

                String formatted = String.format(
                        "The size attribute value %sof portRegion is less than the sum %sof the space required by the ports configured in all partitions and virtual partitions",
                        String.valueOf(portRegionSize),
                        String.valueOf(totalPortMemory));

                errorMessages.add(formatted);
                Log.error(CodeConstant.ERROR_PORT_REGION_SIZE_TOO_SMALL, formatted);
                return false;
            }

            return true;

        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            errorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }

    /**
     * validateHmLogRegionSize
     *
     * @brief
     * 功能: 校验健康监控日志区域大小约束。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateHmLogRegionSize(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 获取HmLogRegion/@Size (使用 local-name，注意首字母大小写，在 PhysicalMemory 下是 hmLogRegion) */
            NodeList hmLogRegionList = (NodeList) xpath.evaluate(
                    "//*[local-name()='CoreOSDescription']//*[local-name()='HardwareConfiguration']//*[local-name()='PhysicalMemory']/*[local-name()='hmLogRegion']",
                    doc, XPathConstants.NODESET);

            if (hmLogRegionList.getLength() == 0) {
                return true; /* 没有配置HmLogRegion，跳过检查 */
            }

            Element hmLogRegion = (Element) hmLogRegionList.item(0);
            long hmLogRegionSize = parseLongSafe(hmLogRegion.getAttribute("Size"));

            long totalLogMemory = 0;

            /* 计算核心健康监控日志空间 (使用 local-name) */
            NodeList coreHMTasks = (NodeList) xpath.evaluate(
                    "//*[local-name()='HealthMonitor']//*[local-name()='HMTask']/*[local-name()='CoreHMTaskList']/*[local-name()='CoreHMTask']",
                    doc, XPathConstants.NODESET);

            for (int i = 0; i < coreHMTasks.getLength(); i++) {
                Element task = (Element) coreHMTasks.item(i);
                long maxLogEntries = parseLongSafe(task.getAttribute("maxLogEntries"));
                totalLogMemory += maxLogEntries * LOG_ENTRY_SIZE;
            }

            /* 计算分区健康监控日志空间 (使用 local-name) */
            NodeList partHMTasks = (NodeList) xpath.evaluate(
                    "//*[local-name()='HealthMonitor']//*[local-name()='HMTask']/*[local-name()='PartitionHMTaskList']/*[local-name()='PartitionHMTask']",
                    doc, XPathConstants.NODESET);

            for (int i = 0; i < partHMTasks.getLength(); i++) {
                Element task = (Element) partHMTasks.item(i);
                long maxLogEntries = parseLongSafe(task.getAttribute("maxLogEntries"));
                totalLogMemory += maxLogEntries * LOG_ENTRY_SIZE;
            }

            if (hmLogRegionSize < totalLogMemory) {
                int lineNumber = getLineNumber(hmLogRegion, lineNumberMap);
                String fileName = getFileNameFromElement(hmLogRegion, xmlFile);
                if (lineNumber < 1) {
                    lineNumber = 1;
                }

                String formatted = String.format(
                        "The size attribute value %sof HmLogRegion is less than the sum %sof the space required by all health monitoring logs.",
                        String.valueOf(hmLogRegionSize),
                        String.valueOf(totalLogMemory));

                errorMessages.add(formatted);
                Log.error(CodeConstant.ERROR_HM_LOG_REGION_SIZE_TOO_SMALL, formatted);
                return false;
            }

            return true;

        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            errorMessages.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }

    /**
     * validateOnlinePartitionPayloads
     *
     * @brief
     * 功能: 执行 validateOnlinePartitionPayloads 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean validateOnlinePartitionPayloads(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap, List<String> errorMessages) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList onlinePayloads = (NodeList) xpath.evaluate(
                    "//*[local-name()='Payloads']/*[local-name()='PartitionPayload' and @Online and @Online='true']",
                    doc, XPathConstants.NODESET);
            if (onlinePayloads.getLength() == 0) {
                return true;
            }
            NodeList kernelRegions = (NodeList) xpath.evaluate("//*[local-name()='CoreOSDescription']//*[local-name()='HardwareConfiguration']//*[local-name()='kernelRegion']", doc, XPathConstants.NODESET);
            if (kernelRegions.getLength() == 0) {
                String formatted = "When Payloads//PartitionPayload/@Online is true,the element HardwareCofiguration/ kernelRegion must be configured";
                errorMessages.add(formatted);
                Log.error(CodeConstant.ERROR_ONLINE_KERNEL_REGION_MISSING, formatted);
                return false;
            }
            Element firstKernelRegion = (Element) kernelRegions.item(0);
            /*
             * kernelRegion 不是带 Name/Base_Address 属性的内存块。其名称就是
             * 元素名，首个区域的物理起始地址按 ICD 的固定布局推导：
             * PhysicalMemory.Base_Address + kernelMemoryRegion +
             * kernelConfigRecordRegion + kernelPgPool + portRegion + hmLogRegion。
             */
            String kernelRegionName = safeLocalName(firstKernelRegion);
            long kernelStart = calculateFirstKernelRegionBaseAddress(doc, xpath);
            long kernelEnd = kernelStart + parseLongSafe(firstKernelRegion.getAttribute("Size"));
            boolean allValid = true;
            for (int i = 0; i < onlinePayloads.getLength(); i++) {
                Element payload = (Element) onlinePayloads.item(i);
                String partitionName = payload.getAttribute("NameRef");
                if (partitionName == null || partitionName.trim().isEmpty()) {
                    partitionName = payload.getAttribute("Name");
                }
                String baseAddress = payload.getAttribute("Base_Address");
                if (baseAddress == null || baseAddress.trim().isEmpty()) {
                    int lineNumber = getLineNumber(payload, lineNumberMap);
                    if (lineNumber < 1) {
                        lineNumber = 1;
                    }
                    String fileName = getFileNameFromElement(payload, xmlFile);
                    String formatted = String.format("[File: %s, Line: %d]In Online,partition %s must have the Base Address attribute configured.", fileName, lineNumber, partitionName);
                    errorMessages.add(formatted);
                    Log.error(CodeConstant.ERROR_ONLINE_PARTITION_BASE_ADDRESS_MISSING, formatted);
                    allValid = false;
                    continue;
                }
                long payloadStart = parseLongSafe(baseAddress);
                /* PartitionPayload 没有 Size 属性，大小由其分区引用的所有
                 * Application 的四个 ROM 部署段组成。 */
                long payloadSize = calculatePartitionPayloadSize(doc, xpath, partitionName);
                long payloadEnd = payloadStart + payloadSize;
                if (payloadStart < kernelStart || payloadEnd > kernelEnd) {
                    String formatted = String.format("In Online,the range [%s,%s]of partition %s is not within the range [%s,%s] of %s", formatHex(payloadStart), formatHex(payloadEnd), partitionName, formatHex(kernelStart), formatHex(kernelEnd), kernelRegionName);
                    errorMessages.add(formatted);
                    Log.error(CodeConstant.ERROR_ONLINE_PARTITION_OUT_OF_KERNEL_REGION, formatted);
                    allValid = false;
                }
            }
            return allValid;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 计算第一个 kernelRegion 的物理起始地址。
     *
     * kernelRegion 的 XSD 定义只有 Size、WriteProtected 和 PoolName，
     * 没有 Base_Address；地址由 PhysicalMemory 内部固定布局确定。
     */
    private static long calculateFirstKernelRegionBaseAddress(Document doc, XPath xpath) throws Exception {
        NodeList physicalMemoryList = (NodeList) xpath.evaluate(
                "//*[local-name()='CoreOSDescription']//*[local-name()='PhysicalMemory'][1]",
                doc, XPathConstants.NODESET);
        if (physicalMemoryList.getLength() == 0) {
            return 0;
        }

        Element physicalMemory = (Element) physicalMemoryList.item(0);
        long baseAddress = parseLongSafe(physicalMemory.getAttribute("Base_Address"));
        String[] precedingRegions = {
                "kernelMemoryRegion",
                "kernelConfigRecordRegion",
                "kernelPgPool",
                "portRegion",
                "hmLogRegion"
        };

        for (String regionName : precedingRegions) {
            NodeList regions = (NodeList) xpath.evaluate(
                    "./*[local-name()='" + regionName + "'][1]",
                    physicalMemory, XPathConstants.NODESET);
            if (regions.getLength() > 0) {
                baseAddress += parseLongSafe(((Element) regions.item(0)).getAttribute("Size"));
            }
        }
        return baseAddress;
    }

    /**
     * 计算 PartitionPayload 的 ROM 部署大小。
     * 按需求只累加 Text、RoData、Data、PersistentData，不包含 Bss/PersistentBss。
     */
    private static long calculatePartitionPayloadSize(Document doc, XPath xpath, String partitionName)
            throws Exception {
        if (partitionName == null || partitionName.trim().isEmpty()) {
            return 0;
        }

        NodeList partitions = (NodeList) xpath.evaluate(
                "//*[local-name()='Partitions']/*[local-name()='Partition']",
                doc, XPathConstants.NODESET);
        NodeList applications = (NodeList) xpath.evaluate(
                "//*[local-name()='Applications']/*[local-name()='Application']",
                doc, XPathConstants.NODESET);

        for (int i = 0; i < partitions.getLength(); i++) {
            Element partition = (Element) partitions.item(i);
            if (!partitionName.equals(partition.getAttribute("Name"))) {
                continue;
            }

            long total = 0;
            NodeList appRefs = (NodeList) xpath.evaluate(
                    ".//*[local-name()='Application' and @NameRef]",
                    partition, XPathConstants.NODESET);
            for (int j = 0; j < appRefs.getLength(); j++) {
                String appNameRef = ((Element) appRefs.item(j)).getAttribute("NameRef");
                for (int k = 0; k < applications.getLength(); k++) {
                    Element application = (Element) applications.item(k);
                    if (!appNameRef.equals(application.getAttribute("Name"))) {
                        continue;
                    }

                    NodeList memorySizes = (NodeList) xpath.evaluate(
                            ".//*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']",
                            application, XPathConstants.NODESET);
                    if (memorySizes.getLength() > 0) {
                        Element memorySize = (Element) memorySizes.item(0);
                        total += sumMemorySizeAttributes(memorySize,
                                "MemorySizeText",
                                "MemorySizeRoData",
                                "MemorySizeData",
                                "MemorySizePersistentData");
                    }
                    break;
                }
            }
            return total;
        }

        return 0;
    }

    /**
     * sumMemorySizeAttributes
     *
     * @brief
     * 功能: 执行 sumMemorySizeAttributes 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static long sumMemorySizeAttributes(Element memorySize, String... attributes) {
        long total = 0;
        for (String attribute : attributes) {
            String value = memorySize.getAttribute(attribute);
            if (value != null && !value.trim().isEmpty()) {
                total += parseLongSafe(value);
            }
        }
        return total;
    }

    /**
     * validateSystemSharedLibrary
     *
     * @brief
     * 功能: 执行 validateSystemSharedLibrary 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean validateSystemSharedLibrary(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap, List<String> errorMessages) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList sharedLibraries = (NodeList) xpath.evaluate("//*[local-name()='SharedLibraryRegions']/*[local-name()='SharedLibrary']", doc, XPathConstants.NODESET);
            boolean allValid = true;
            int systemSharedLibraryCount = 0;
            for (int i = 0; i < sharedLibraries.getLength(); i++) {
                Element sharedLibrary = (Element) sharedLibraries.item(i);
                NodeList descriptions = (NodeList) xpath.evaluate(".//*[local-name()='SharedLibraryDescription' and translate(@SystemSharedLibrary,'TRUE','true')='true']", sharedLibrary, XPathConstants.NODESET);
                if (descriptions.getLength() == 0) {
                    continue;
                }
                systemSharedLibraryCount++;
                String sharedLibraryName = sharedLibrary.getAttribute("Name");
                if (!"pos".equals(sharedLibraryName)) {
                    String formatted = String.format("The name of the system shared library%sis incorrectly configured;it must be configured as “pos”", sharedLibraryName);
                    errorMessages.add(formatted);
                    Log.error(CodeConstant.ERROR_SYSTEM_SHARED_LIBRARY_NAME_INVALID, formatted);
                    allValid = false;
                }
            }
            if (systemSharedLibraryCount > 1) {
                String formatted = "The system shared library is not unique.";
                errorMessages.add(formatted);
                Log.error(CodeConstant.ERROR_SYSTEM_SHARED_LIBRARY_NOT_UNIQUE, formatted);
                allValid = false;
            }
            return allValid;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * validateBmpSchedulePartitionCoreUniqueness
     *
     * @brief
     * 功能: 执行 validateBmpSchedulePartitionCoreUniqueness 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean validateBmpSchedulePartitionCoreUniqueness(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap, List<String> errorMessages) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList bmpSchedules = (NodeList) xpath.evaluate("//*[local-name()='Schedules']/*[local-name()='BmpSchedule']", doc, XPathConstants.NODESET);
            boolean allValid = true;
            for (int i = 0; i < bmpSchedules.getLength(); i++) {
                Element bmpSchedule = (Element) bmpSchedules.item(i);
                NodeList cores = (NodeList) xpath.evaluate("./*[local-name()='Core']", bmpSchedule, XPathConstants.NODESET);
                Map<String, String> partitionCoreMap = new HashMap<>();
                Set<String> reportedPartitions = new HashSet<>();
                for (int j = 0; j < cores.getLength(); j++) {
                    Element core = (Element) cores.item(j);
                    String coreId = core.getAttribute("cpuId");
                    NodeList partitionWindows = (NodeList) xpath.evaluate(".//*[local-name()='PartitionWindow' and @PartitionNameRef]", core, XPathConstants.NODESET);
                    for (int k = 0; k < partitionWindows.getLength(); k++) {
                        Element partitionWindow = (Element) partitionWindows.item(k);
                        String partitionName = partitionWindow.getAttribute("PartitionNameRef");
                        if (partitionName == null || partitionName.trim().isEmpty()) {
                            continue;
                        }
                        String previousCore = partitionCoreMap.get(partitionName);
                        if (previousCore == null) {
                            partitionCoreMap.put(partitionName, coreId);
                            continue;
                        }
                        if (!previousCore.equals(coreId) && reportedPartitions.add(partitionName)) {
                            String formatted = String.format("Partiontion%scannot be configured under multiple Core elements", partitionName);
                            errorMessages.add(formatted);
                            Log.error(CodeConstant.ERROR_BMP_SCHEDULE_PARTITION_IN_MULTIPLE_CORES, formatted);
                            allValid = false;
                        }
                    }
                }
            }
            return allValid;
        } catch (Exception e) {
            return true;
        }
    }
    /**
     * validateCoreHMTaskCount
     *
     * @brief
     * 功能: 校验 CoreHMTask 数量约束。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateCoreHMTaskCount(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            NodeList coreHMTasks = (NodeList) xpath.evaluate(
                    "//*[local-name()='HealthMonitor']//*[local-name()='HMTask']/*[local-name()='CoreHMTaskList']/*[local-name()='CoreHMTask']",
                    doc, XPathConstants.NODESET);

            /* MIN 约束：至少应有 1 个 CoreHMTask */
            if (coreHMTasks.getLength() < 1) {
                /* 尽量定位到 CoreHMTaskList 或 HMTask 的行号 */
                Element anchor = null;
                NodeList coreHMTaskLists = (NodeList) xpath.evaluate(
                        "//*[local-name()='HealthMonitor']//*[local-name()='HMTask']/*[local-name()='CoreHMTaskList']",
                        doc,
                        XPathConstants.NODESET);
                if (coreHMTaskLists.getLength() > 0) {
                    anchor = (Element) coreHMTaskLists.item(0);
                } else {
                    NodeList hmTasks = (NodeList) xpath.evaluate(
                            "//*[local-name()='HealthMonitor']//*[local-name()='HMTask']",
                            doc,
                            XPathConstants.NODESET);
                    if (hmTasks.getLength() > 0) {
                        anchor = (Element) hmTasks.item(0);
                    }
                }

                int lineNumber = (anchor != null) ? getLineNumber(anchor, lineNumberMap) : -1;
                if (lineNumber < 1) {
                    lineNumber = 1;
                }

                String fileName = (anchor != null) ? getFileNameFromElement(anchor, xmlFile) : xmlFile.getName();
                String formatted = formatOccursLessThanMessage(fileName, lineNumber, "CoreHMTask",
                        coreHMTasks.getLength(), 1);

                errorMessages.add(formatted);
                Log.error(CodeConstant.ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET, formatted);
                return false;
            }

            if (coreHMTasks.getLength() <= CORE_HM_TASK_MAX_COUNT) {
                return true;
            }

            /* 定位到第 (MAX+1) 个元素，尽量给出准确行号 */
            int index = CORE_HM_TASK_MAX_COUNT;
            Element offending = (Element) coreHMTasks.item(index);

            int lineNumber = getLineNumber(offending, lineNumberMap);
            if (lineNumber < 0) {
                /* 回退：如果取不到具体元素行号，则用列表父节点的行号 */
                Node parent = offending.getParentNode();
                if (parent instanceof Element) {
                    lineNumber = getLineNumber((Element) parent, lineNumberMap);
                }
            }

            String fileName = getFileNameFromElement(offending, xmlFile);
            String formatted = formatOccursGreaterThanMessage(fileName, lineNumber, "CoreHMTask",
                    coreHMTasks.getLength(), CORE_HM_TASK_MAX_COUNT);

            errorMessages.add(formatted);
            Log.error(CodeConstant.ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED, formatted);
            return false;

        } catch (Exception e) {
            /* 不因辅助校验异常阻断主流程 */
            return true;
        }
    }

    /**
     * validateCoreCountInCoreHMTask
     *
     * @brief
     * 功能: 校验 CoreHMTask 内 Core 数量约束。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateCoreCountInCoreHMTask(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            NodeList coreHMTasks = (NodeList) xpath.evaluate(
                    "//*[local-name()='HealthMonitor']//*[local-name()='HMTask']/*[local-name()='CoreHMTaskList']/*[local-name()='CoreHMTask']",
                    doc,
                    XPathConstants.NODESET);

            for (int i = 0; i < coreHMTasks.getLength(); i++) {
                Element task = (Element) coreHMTasks.item(i);
                NodeList cores = (NodeList) xpath.evaluate(
                        "./*[local-name()='Core']",
                        task,
                        XPathConstants.NODESET);

                if (cores.getLength() <= CORE_MAX_COUNT_IN_CORE_HM_TASK) {
                    continue;
                }

                int index = CORE_MAX_COUNT_IN_CORE_HM_TASK;
                Element offending = (Element) cores.item(index);

                int lineNumber = getLineNumber(offending, lineNumberMap);
                if (lineNumber < 0) {
                    /* 回退：如果取不到具体元素行号，则用任务节点的行号 */
                    lineNumber = getLineNumber(task, lineNumberMap);
                }

                String fileName = getFileNameFromElement(offending, xmlFile);
                String formatted = formatOccursGreaterThanMessage(fileName, lineNumber, "Core",
                        cores.getLength(), CORE_MAX_COUNT_IN_CORE_HM_TASK);

                errorMessages.add(formatted);
                Log.error(CodeConstant.ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED, formatted);
                return false;
            }

            return true;

        } catch (Exception e) {
            /* 不因辅助校验异常阻断主流程 */
            return true;
        }
    }

    /**
     * validatePortsCount
     *
     * @brief
     * 功能: 校验 Ports 容器内端口数量约束。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validatePortsCount(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 查找所有 Ports 容器 */
            NodeList portsContainers = (NodeList) xpath.evaluate(
                    "//*[local-name()='Ports']",
                    doc, XPathConstants.NODESET);

            boolean allValid = true;

            for (int i = 0; i < portsContainers.getLength(); i++) {
                Element portsContainer = (Element) portsContainers.item(i);

                /* 端口数量约束作用于同一个 Ports 容器内两类端口的合计数量。 */
                List<Element> allPorts = new ArrayList<>();
                NodeList children = portsContainer.getChildNodes();
                for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                    Node child = children.item(childIndex);
                    if (child.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    Element port = (Element) child;
                    String portName = safeLocalName(port);
                    if ("QueuingPort".equals(portName) || "SamplingPort".equals(portName)) {
                        allPorts.add(port);
                    }
                }

                int totalPorts = allPorts.size();

                /*
                 * XSD 1.0 permits maxOccurs only up to 5000 in the JDK Xerces
                 * implementation used by this tool.  The ICD limit for each
                 * port element is 5120, so the XSD keeps the unbounded particle
                 * needed to compile and this semantic check supplies the exact
                 * 2010 diagnostic for a single type exceeding 5120.  The
                 * aggregate 5120 rule below remains the separate 5016 check.
                 */
                for (String portType : new String[] { "QueuingPort", "SamplingPort" }) {
                    List<Element> sameTypePorts = new ArrayList<>();
                    for (Element port : allPorts) {
                        if (portType.equals(safeLocalName(port))) {
                            sameTypePorts.add(port);
                        }
                    }
                    if (sameTypePorts.size() > PORTS_MAX_COUNT) {
                        Element offending = sameTypePorts.get(PORTS_MAX_COUNT);
                        int lineNumber = getLineNumber(offending, lineNumberMap);
                        if (lineNumber < 1) {
                            lineNumber = getLineNumber(portsContainer, lineNumberMap);
                        }
                        if (lineNumber < 1) {
                            lineNumber = 1;
                        }
                        String fileName = getFileNameFromElement(offending, xmlFile);
                        String formatted = formatOccursGreaterThanMessage(fileName, lineNumber, portType,
                                sameTypePorts.size(), PORTS_MAX_COUNT);
                        errorMessages.add(formatted);
                        Log.error(CodeConstant.ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED, formatted);
                        allValid = false;
                    }
                }

                if (totalPorts <= PORTS_MAX_COUNT) {
                    continue;
                }

                /* 将第 (MAX+1) 个端口作为诊断定位点，错误码由一致性校验统一输出。 */
                Element offending = allPorts.get(PORTS_MAX_COUNT);
                int lineNumber = getLineNumber(offending, lineNumberMap);
                if (lineNumber < 1) {
                    lineNumber = getLineNumber(portsContainer, lineNumberMap);
                }
                if (lineNumber < 1) {
                    lineNumber = 1;
                }

                String formatted = String.format(
                        "The element of sum Ports/QueuingPort and Ports/SamplingPort is %d, which is more than %d.",
                        totalPorts, PORTS_MAX_COUNT);

                errorMessages.add(formatted);
                Log.error(CodeConstant.ERROR_PORTS_COUNT_EXCEEDS_LIMIT, formatted);
                allValid = false;
            }

            return allValid;

        } catch (Exception e) {
            /* 不因辅助校验异常阻断主流程 */
            return true;
        }
    }

    /**
     * validateCoreOSChildElementCounts
     *
     * @brief
     * 功能: 校验 coreOS 配置中子元素数量约束。
     * @param[in] doc XML DOM 文档。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateCoreOSChildElementCounts(Document doc, File xmlFile,
            Map<String, Integer> lineNumberMap,
            List<String> errorMessages) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            boolean allValid = true;

            NodeList hardwareConfigurations = (NodeList) xpath.evaluate(
                    "//*[local-name()='CoreOSDescription']//*[local-name()='HardwareConfiguration']",
                    doc,
                    XPathConstants.NODESET);

            for (int i = 0; i < hardwareConfigurations.getLength(); i++) {
                Element hardwareConfiguration = (Element) hardwareConfigurations.item(i);
                allValid &= validateChildCount(hardwareConfiguration, "PhysicalMemory", 1, 1,
                        xmlFile, lineNumberMap, errorMessages);
                allValid &= validateChildCount(hardwareConfiguration, "userMemoryRegion", 1, 1,
                        xmlFile, lineNumberMap, errorMessages);
                allValid &= validateChildCount(hardwareConfiguration, "payloadMemory", 1, Integer.MAX_VALUE,
                        xmlFile, lineNumberMap, errorMessages);
            }

            NodeList physicalMemories = (NodeList) xpath.evaluate(
                    "//*[local-name()='CoreOSDescription']//*[local-name()='HardwareConfiguration']"
                            + "/*[local-name()='PhysicalMemory']",
                    doc,
                    XPathConstants.NODESET);

            for (int i = 0; i < physicalMemories.getLength(); i++) {
                Element physicalMemory = (Element) physicalMemories.item(i);
                allValid &= validateChildCount(physicalMemory, "kernelMemoryRegion", 1, 1,
                        xmlFile, lineNumberMap, errorMessages);
                allValid &= validateChildCount(physicalMemory, "kernelConfigRecordRegion", 1, 1,
                        xmlFile, lineNumberMap, errorMessages);
                allValid &= validateChildCount(physicalMemory, "kernelPgPool", 0, 1,
                        xmlFile, lineNumberMap, errorMessages);
                allValid &= validateChildCount(physicalMemory, "portRegion", 1, 1,
                        xmlFile, lineNumberMap, errorMessages);
                allValid &= validateChildCount(physicalMemory, "hmLogRegion", 1, 1,
                        xmlFile, lineNumberMap, errorMessages);
                allValid &= validateChildCount(physicalMemory, "ramPayloadRegion", 0, 1,
                        xmlFile, lineNumberMap, errorMessages);
                allValid &= validateChildCount(physicalMemory, "libRegion", 1, 1,
                        xmlFile, lineNumberMap, errorMessages);
            }

            return allValid;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * validateChildCount
     *
     * @brief
     * 功能: 校验指定父元素下子元素数量范围。
     * @param[in] parent 父元素。
     * @param[in] childName 子元素名称。
     * @param[in] min 允许的最小数量。
     * @param[in] max 允许的最大数量。
     * @param[in] xmlFile XML 配置文件。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验或判断通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateChildCount(Element parent, String childName, int min, int max,
            File xmlFile, Map<String, Integer> lineNumberMap, List<String> errorMessages) {
        NodeList children = parent.getChildNodes();
        List<Element> matchedChildren = new ArrayList<>();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element childElement = (Element) child;
            if (childName.equals(safeLocalName(childElement))) {
                matchedChildren.add(childElement);
            }
        }

        if (matchedChildren.size() < min) {
            int lineNumber = getLineNumber(parent, lineNumberMap);
            if (lineNumber < 1) {
                lineNumber = 1;
            }
            String fileName = getFileNameFromElement(parent, xmlFile);
            String formatted = formatOccursLessThanMessage(fileName, lineNumber, childName,
                    matchedChildren.size(), min);
            errorMessages.add(formatted);
            Log.error(CodeConstant.ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET, formatted);

            // 006358: 必要元素完全缺失时，除数量约束错误2009外，
            // 还必须报告必要元素缺失错误2015。两类需求可以由同一次缺失同时触发。
            if (min >= 1 && matchedChildren.isEmpty()) {
                String parentName = safeLocalName(parent);
                String requiredMessage = String.format(
                        "[File: %s, Line: %d] Required element '%s' is missing in parent element '%s'.",
                        fileName, lineNumber, childName, parentName);
                errorMessages.add(requiredMessage);
                Log.error(CodeConstant.ERROR_XML_REQUIRED_ELEMENT_MISSING, requiredMessage);
            }
            return false;
        }

        if (matchedChildren.size() > max) {
            Element offending = matchedChildren.get(max);
            int lineNumber = getLineNumber(offending, lineNumberMap);
            if (lineNumber < 1) {
                lineNumber = getLineNumber(parent, lineNumberMap);
            }
            if (lineNumber < 1) {
                lineNumber = 1;
            }
            String fileName = getFileNameFromElement(offending, xmlFile);
            String formatted = formatOccursGreaterThanMessage(fileName, lineNumber, childName,
                    matchedChildren.size(), max);
            errorMessages.add(formatted);
            Log.error(CodeConstant.ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED, formatted);
            return false;
        }

        return true;
    }

    /* 辅助工具方法 */

    /**
     * formatOccursLessThanMessage
     *
     * @brief
     * 功能: TR_PR_005 2009 新文案 "element X occurs N time(s) less than minimum M time(s)."
     *       （time(s) 为字面输出，与表1模板一致）
     * @scope internal
     */
    private static String formatOccursLessThanMessage(String fileName, int lineNumber, String elementName,
            int actualCount, int min) {
        return String.format("[File: %s, Line: %d] element %s occurs %d time(s) less than minimum %d time(s).",
                fileName, lineNumber, elementName, actualCount, min);
    }

    /**
     * formatOccursGreaterThanMessage
     *
     * @brief
     * 功能: TR_PR_006 2010 新文案 "element X occurs N time(s) greater than M time(s)."
     *       （time(s) 为字面输出，与表1模板一致）
     * @scope internal
     */
    private static String formatOccursGreaterThanMessage(String fileName, int lineNumber, String elementName,
            int actualCount, int max) {
        return String.format("[File: %s, Line: %d] element %s occurs %d time(s) greater than %d time(s).",
                fileName, lineNumber, elementName, actualCount, max);
    }

    /**
     * getElementName
     *
     * @brief
     * 功能: 获取元素的本地名称或节点名称。
     * @param[in] element XML 元素。
     * @return 字符串结果。
     * @scope internal
     */
    private static String getElementName(Element element) {
        if (element == null) {
            return "<unknown>";
        }
        String localName = element.getLocalName();
        if (localName != null && !localName.isEmpty()) {
            return localName;
        }
        String nodeName = element.getNodeName();
        if (nodeName == null || nodeName.isEmpty()) {
            return "<unknown>";
        }
        int idx = nodeName.indexOf(':');
        if (idx >= 0 && idx < nodeName.length() - 1) {
            return nodeName.substring(idx + 1);
        }
        return nodeName;
    }

    /**
     * calculateApplicationMemory
     *
     * @brief
     * 功能: 计算分区关联应用所需内存总量。
     * @param[in] partition 分区元素。
     * @param[in] xpath XPath 查询对象。
     * @param[in] doc XML DOM 文档。
     * @return 计算或解析得到的长整型数值。
     * @scope internal
     */
    private static long calculateApplicationMemory(Element partition, XPath xpath, Document doc) throws Exception {
        /* 获取应用引用 (使用 local-name) */
        NodeList appNodes = (NodeList) xpath.evaluate(".//*[local-name()='Application']", partition,
                XPathConstants.NODESET);
        if (appNodes.getLength() == 0) {
            return 0;
        }

        long total = 0;
        for (int i = 0; i < appNodes.getLength(); i++) {
            Element app = (Element) appNodes.item(i);
            String appNameRef = app.getAttribute("NameRef");

            /* 查找当前应用定义；一个分区可以引用多个应用，必须逐个累加。 */
            NodeList appDefList = (NodeList) xpath.evaluate(
                    "//*[local-name()='Application' and @Name='" + appNameRef
                            + "']//*[local-name()='ApplicationDescription']/*[local-name()='MemorySize']",
                    doc, XPathConstants.NODESET);

            if (appDefList.getLength() > 0) {
                Element memSize = (Element) appDefList.item(0);
                total += sumMemorySizeChildren(memSize);
            }
        }

        return total;
    }

    /**
     * calculateSharedLibraryMemory
     *
     * @brief
     * 功能: 计算分区关联共享库所需内存总量。
     * @param[in] partition 分区元素。
     * @param[in] xpath XPath 查询对象。
     * @param[in] doc XML DOM 文档。
     * @return 计算或解析得到的长整型数值。
     * @scope internal
     */
    private static long calculateSharedLibraryMemory(Element partition, XPath xpath, Document doc) throws Exception {
        /* 获取共享库引用 (使用 local-name，注意是 SharedLibraryRegion) */
        NodeList libNodes = (NodeList) xpath.evaluate(".//*[local-name()='SharedLibraryRegion']", partition,
                XPathConstants.NODESET);

        long total = 0;
        for (int i = 0; i < libNodes.getLength(); i++) {
            Element lib = (Element) libNodes.item(i);
            String libNameRef = lib.getAttribute("NameRef");

            /* 查找共享库定义 (使用 local-name) */
            NodeList libDefList = (NodeList) xpath.evaluate(
                    "//*[local-name()='SharedLibrary' and @Name='" + libNameRef
                            + "']//*[local-name()='SharedLibraryDescription']/*[local-name()='MemorySize']",
                    doc, XPathConstants.NODESET);

            if (libDefList.getLength() > 0) {
                Element memSize = (Element) libDefList.item(0);
                /* 累加除text和rodata之外的段 */
                total += sumMemorySizeChildrenExcluding(memSize, "MemorySizeText", "MemorySizeRoData");
            }
        }

        return total;
    }

    /**
     * getMaxPartitionRequiredMemorySize
     *
     * @brief
     * 功能: 获取分区 RequiredMemorySize 最大值。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @return 计算或解析得到的长整型数值。
     * @scope internal
     */
    private static long getMaxPartitionRequiredMemorySize(Document doc, XPath xpath) throws Exception {
        /* 使用 local-name() 来忽略命名空间 */
        NodeList partitions = (NodeList) xpath.evaluate("//*[local-name()='Partition']", doc, XPathConstants.NODESET);

        long maxSize = 0;
        for (int i = 0; i < partitions.getLength(); i++) {
            Element partition = (Element) partitions.item(i);
            NodeList settingsList = (NodeList) xpath.evaluate(".//*[local-name()='Settings']", partition,
                    XPathConstants.NODESET);

            if (settingsList.getLength() > 0) {
                Element settings = (Element) settingsList.item(0);
                long requiredSize = parseLongSafe(settings.getAttribute("RequiredMemorySize"));
                if (requiredSize > maxSize) {
                    maxSize = requiredSize;
                }
            }
        }

        return maxSize;
    }

    /**
     * sumMemorySizeChildren
     *
     * @brief
     * 功能: 汇总 MemorySize 子元素数值。
     * @param[in] memorySize MemorySize 元素。
     * @return 计算或解析得到的长整型数值。
     * @scope internal
     */
    private static long sumMemorySizeChildren(Element memorySize) {
        long total = 0;

        /* MemorySize的各个段都是属性 */
        String[] attrs = {
                "MemorySizeText", "MemorySizeRoData", "MemorySizeData",
                "MemorySizeBss", "MemorySizePersistentData", "MemorySizePersistentBss"
        };

        for (String attr : attrs) {
            String value = memorySize.getAttribute(attr);
            if (value != null && !value.trim().isEmpty()) {
                total += parseLongSafe(value);
            }
        }

        return total;
    }

    /**
     * sumMemorySizeChildrenExcluding
     *
     * @brief
     * 功能: 按排除列表汇总 MemorySize 子元素数值。
     * @param[in] memorySize MemorySize 元素。
     * @param[in] excludedNames 排除统计的子元素名称。
     * @return 计算或解析得到的长整型数值。
     * @scope internal
     */
    private static long sumMemorySizeChildrenExcluding(Element memorySize, String... excludedNames) {
        Set<String> excludedSet = new HashSet<>(Arrays.asList(excludedNames));
        long total = 0;

        /* MemorySize的各个段都是属性 */
        String[] attrs = {
                "MemorySizeText", "MemorySizeRoData", "MemorySizeData",
                "MemorySizeBss", "MemorySizePersistentData", "MemorySizePersistentBss"
        };

        for (String attr : attrs) {
            if (!excludedSet.contains(attr)) {
                String value = memorySize.getAttribute(attr);
                if (value != null && !value.trim().isEmpty()) {
                    total += parseLongSafe(value);
                }
            }
        }

        return total;
    }

    /**
     * collectIncludedFiles
     *
     * @brief
     * 功能: 收集 XInclude 引用的配置文件。
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
                    doc, XPathConstants.NODESET);

            for (int i = 0; i < includes.getLength(); i++) {
                Element include = (Element) includes.item(i);
                String href = include.getAttribute("href");

                if (href != null && !href.trim().isEmpty()) {
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

                        /* 递归处理被引用文件中的 xi:include */
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
            /* 忽略错误 */
        }
    }

    /**
     * @brief
     * 功能: 构建 XML 元素行号映射。
     * @param[in] xmlFile XML 配置文件。
     * @return 映射表。
     * @scope internal
     */
    private static Map<String, Integer> buildLineNumberMap(File xmlFile) {
        Map<String, Integer> lineNumberMap = new HashMap<>();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();

            LineNumberCollector handler = new LineNumberCollector(lineNumberMap);
            org.xml.sax.InputSource inputSource = XmlParser.preprocessXmlFile(xmlFile);
            parser.parse(inputSource, handler);
        } catch (Exception e) {
            /* 忽略错误，返回空映射 */
        }
        return lineNumberMap;
    }

    /**
     * buildSourceMaps
     *
     * @brief
     * 功能: 构建元素行号与来源文件映射。
     * @param[in] xmlFile XML 配置文件。
     * @return 来源文件与行号映射。
     * @scope internal
     */
    private static SourceMaps buildSourceMaps(File xmlFile) {
        Map<String, Integer> lineNumberMap = buildLineNumberMap(xmlFile);
        Map<String, String> fileNameMap = new HashMap<>();
        String fileName = (xmlFile != null) ? xmlFile.getName() : "unknown.xml";
        for (String key : lineNumberMap.keySet()) {
            fileNameMap.put(key, fileName);
        }
        return new SourceMaps(lineNumberMap, fileNameMap);
    }

    private static class SourceMaps {
        final Map<String, Integer> lineNumberMap;
        final Map<String, String> fileNameMap;

        SourceMaps(Map<String, Integer> lineNumberMap, Map<String, String> fileNameMap) {
            this.lineNumberMap = lineNumberMap;
            this.fileNameMap = fileNameMap;
        }
    }

    /**
     * SAX Handler用于收集行号信息
     */
    private static class LineNumberCollector extends DefaultHandler {
        private Locator locator;
        private Map<String, Integer> lineNumberMap;
        private List<String> elementStack = new ArrayList<>();

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
            int lineNumber = (locator != null) ? locator.getLineNumber() : -1;

            /* 构建带属性的路径键 */
            String pathKey = buildPathKey(elementName, attributes);
            elementStack.add(pathKey);

            /* 存储完整路径和简单名称 */
            String fullPath = String.join("/", elementStack);
            lineNumberMap.put(fullPath, lineNumber);
            lineNumberMap.put(elementName, lineNumber);
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

    /**
     * buildPathKey
     *
     * @brief
     * 功能: 按元素名称和属性构建路径键。
     * @param[in] elementName 元素名称。
     * @param[in] attributes 元素属性集合。
     * @return 字符串结果。
     * @scope internal
     */
        private String buildPathKey(String elementName, Attributes attributes) {
            String name = attributes.getValue("Name");
            if (name != null && !name.isEmpty()) {
                return elementName + "[@Name='" + name + "']";
            }
            return elementName;
        }
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
        /* 先尝试：使用本次校验构建的映射回溯来源文件（用于XInclude展开后BaseURI/DocumentURI指向主文档的场景） */
        String mapped = getFileNameFromMap(element);
        if (mapped != null && !mapped.isEmpty()) {
            if (defaultFile == null) {
                return mapped;
            }
            /* 只有当映射结果与默认文件不同才优先采用；否则继续走URI推断。 */
            if (!mapped.equalsIgnoreCase(defaultFile.getName())) {
                return mapped;
            }
        }

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
     * safeLocalName
     *
     * @brief
     * 功能: 安全获取元素本地名称。
     * @param[in] element XML 元素。
     * @return 字符串结果。
     * @scope internal
     */
    private static String safeLocalName(Element element) {
        if (element == null) {
            return null;
        }
        String ln = element.getLocalName();
        if (ln != null && !ln.isEmpty()) {
            return ln;
        }
        String nodeName = element.getNodeName();
        if (nodeName != null && !nodeName.isEmpty()) {
            int idx = nodeName.indexOf(':');
            return idx >= 0 ? nodeName.substring(idx + 1) : nodeName;
        }
        return null;
    }

    /**
     * getFileNameFromMap
     *
     * @brief
     * 功能: 从来源映射中获取元素文件名。
     * @param[in] element XML 元素。
     * @return 字符串结果。
     * @scope internal
     */
    private static String getFileNameFromMap(Element element) {
        Map<String, String> fileNameMap = CURRENT_FILE_NAME_MAP.get();
        if (fileNameMap == null || element == null) {
            return null;
        }

        /* 尝试用“从根到当前元素”的路径键匹配（与 getLineNumber 的构造规则保持一致） */
        List<String> pathParts = new ArrayList<>();
        Node current = element;
        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            Element elem = (Element) current;
            String ln = safeLocalName(elem);
            if (ln == null) {
                break;
            }
            String name = elem.getAttribute("Name");
            String part;
            if (name != null && !name.isEmpty()) {
                part = ln + "[@Name='" + name + "']";
            } else {
                part = ln;
            }
            pathParts.add(0, part);
            current = current.getParentNode();
        }

        for (int i = 0; i < pathParts.size(); i++) {
            String path = String.join("/", pathParts.subList(i, pathParts.size()));
            String hit = fileNameMap.get(path);
            if (hit != null && !hit.isEmpty()) {
                return hit;
            }
        }
        return null;
    }

    /**
     * getLineNumber
     *
     * @brief
     * 功能: 根据元素和行号映射获取行号。
     * @param[in] element XML 元素。
     * @param[in] lineNumberMap 元素行号映射。
     * @param[in] lineNumberMap 元素行号映射。
     * @return 计算或解析得到的整型数值。
     * @scope internal
     */
    private static int getLineNumber(Element element, Map<String, Integer> lineNumberMap) {
        /* 构建从根到当前元素的完整路径 */
        List<String> pathParts = new ArrayList<>();
        Node current = element;

        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            Element elem = (Element) current;
            String localName = elem.getLocalName();
            String name = elem.getAttribute("Name");

            String pathPart;
            if (name != null && !name.isEmpty()) {
                pathPart = localName + "[@Name='" + name + "']";
            } else {
                pathPart = localName;
            }

            pathParts.add(0, pathPart); /* 在开头插入 */
            current = current.getParentNode();
        }

        /* 尝试从完整路径开始逐级缩短来查找行号 */
        for (int i = 0; i < pathParts.size(); i++) {
            String path = String.join("/", pathParts.subList(i, pathParts.size()));
            Integer line = lineNumberMap.get(path);
            if (line != null)
                return line;
        }

        return -1;
    }

    /**
     * parseLongSafe
     *
     * @brief
     * 功能: 安全解析长整型数值。
     * @param[in] value 数值字符串。
     * @return 计算或解析得到的长整型数值。
     * @scope internal
     */
    private static long parseLongSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        try {
            value = value.trim();
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseLong(value.substring(2), 16);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }



    /**
     * formatHex
     *
     * @brief
     * 功能: 执行 formatHex 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String formatHex(long value) {
        return String.format("0x%X", value);
    }
    /**
     * parseIntSafe
     *
     * @brief
     * 功能: 安全解析整型数值。
     * @param[in] value 数值字符串。
     * @return 计算或解析得到的整型数值。
     * @scope internal
     */
    private static int parseIntSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        try {
            value = value.trim();
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Integer.parseInt(value.substring(2), 16);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
