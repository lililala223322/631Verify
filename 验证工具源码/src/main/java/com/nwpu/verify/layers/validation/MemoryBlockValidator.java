/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: MemoryBlockValidator.java
 * 
 * 描述    : 校验内存块包含、重叠和容量约束。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
  *     2026-07-27  开发人员  修正地址越界错误信息中的参数格式和空格。
*/

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.layers.parsing.XmlParser;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.File;
import java.util.*;

/**
 * @brief
 * 功能: 校验内存块包含、重叠和容量约束。
 */
public class MemoryBlockValidator {

    /**
     * @brief
     * 功能: 描述待校验的内存块地址、大小和来源行号。
     */
    private static class MemoryBlock {
        String name; /* 内存块名称 */
        long baseAddress; /* 起始地址 */
        long size; /* 大小 */
        boolean exists; /* 是否存在 */
        int lineNumber; /* 在XML中的行号 */

        /**
         * MemoryBlock
         *
         * @brief
         * 功能: 创建内存块描述对象。
         * @param[in] name 内存块名称。
         * @param[in] baseAddress 起始地址。
         * @param[in] size 内存块大小。
         * @param[in] exists 内存块是否存在。
         * @param[in] lineNumber XML 行号。
         * @return 无。
         * @scope internal
         */
        MemoryBlock(String name, long baseAddress, long size, boolean exists, int lineNumber) {
            this.name = name;
            this.baseAddress = baseAddress;
            this.size = size;
            this.exists = exists;
            this.lineNumber = lineNumber;
        }

        /**
         * getEndAddress
         *
         * @brief
         * 功能: 计算内存块结束地址。
         * @return 内存块结束地址。
         * @scope internal
         */
        long getEndAddress() {
            return baseAddress + size;
        }
    }

    /* 存储元素行号映射：elementName -> lineNumber */
    private static Map<String, Integer> elementLineMap = new HashMap<>();
    /* coreOS.xml 文件名 */
    private static String coreOSFileName = "coreOS.xml";

    /**
     * validateMemoryBlocks
     *
     * @brief
     * 功能: 校验内存块包含、重叠和容量约束。
     * @param[in] xmlFile XML 配置文件。
     * @param[out] errorMessages 校验错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateMemoryBlocks(File xmlFile, List<String> errorMessages) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true;
        }

        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();
        boolean allValid = true;

        try {
            /* 查找 coreOS.xml 文件（与主文件同目录或通过环境变量引用） */
            File coreOSFile = findCoreOSFile(xmlFile);
            if (coreOSFile != null && coreOSFile.exists()) {
                /* 对 coreOS.xml 构建元素行号映射 */
                buildElementLineMap(coreOSFile);
                coreOSFileName = coreOSFile.getName();
            } else {
                /* 如果 coreOS.xml 不存在，对主文件构建映射 */
                buildElementLineMap(xmlFile);
                coreOSFileName = xmlFile.getName();
            }

            /* 使用支持环境变量的XML解析 */
            Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, true);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            String fileName = xmlFile.getName();

            /* 第一部分：包含关系验证 (3004) */

            /* 1. 提取物理空间块 PhysicalMemory */
            MemoryBlock physicalMemory = extractMemoryBlock(doc, xpath, "PhysicalMemory",
                    "//*[local-name()='HardwareConfiguration']/*[local-name()='PhysicalMemory']/@Base_Address",
                    "//*[local-name()='HardwareConfiguration']/*[local-name()='PhysicalMemory']/@Size");

            /* 2. 验证物理空间块包含其子元素 */
            if (physicalMemory != null && physicalMemory.exists) {
                /* PhysicalMemory 的子元素列表 */
                String[] physicalMemoryChildren = {
                        "kernelMemoryRegion", /* 核心内存块 */
                        "kernelPgPool", /* 核心页内存块 */
                        "portRegion", /* 端口缓冲区内存块 */
                        "hmLogRegion", /* 健康监控日志记录内存块 */
                        "kernelConfigRecordRegion", /* 配置记录内存块 */
                        "ramPayloadRegion", /* RAM部署空间内存块 */
                        "libRegion" /* 分区操作系统共享库内存块 */
                };

                for (String childName : physicalMemoryChildren) {
                    MemoryBlock child = extractPhysicalMemoryChild(doc, xpath, childName);
                    if (child != null && child.exists) {
                        if (!contains(physicalMemory, child)) {
                            String errorMsg = String.format(
                                    "[File: %s, Line: %d] address %s out of range,the range is [0x%X, 0x%X]",
                                    coreOSFileName, child.lineNumber, child.baseAddress,
                                    physicalMemory.baseAddress, physicalMemory.getEndAddress());
                            localErrors.add(errorMsg);
                            Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
                            allValid = false;
                        }
                    }
                }

                /* 检查多个 kernelRegion 实例 */
                List<MemoryBlock> kernelRegions = extractKernelRegions(doc, xpath);
                for (MemoryBlock kr : kernelRegions) {
                    if (kr.exists && !contains(physicalMemory, kr)) {
                        String errorMsg = String.format(
                                "[File: %s, Line: %d] address %s out of range,the range is [0x%X, 0x%X]",
                                coreOSFileName, kr.lineNumber, kr.baseAddress,
                                physicalMemory.baseAddress, physicalMemory.getEndAddress());
                        localErrors.add(errorMsg);
                        Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
                        allValid = false;
                    }
                }
            }

            /* 第二部分：互斥关系验证 (3005) */

            /* 3. 提取用户内存块 */
            MemoryBlock userMemoryRegion = extractMemoryBlock(doc, xpath, "userMemoryRegion",
                    "//*[local-name()='HardwareConfiguration']/*[local-name()='userMemoryRegion']/@Base_Address",
                    "//*[local-name()='HardwareConfiguration']/*[local-name()='userMemoryRegion']/@Size");

            /* 004909：提取 ROM 部署空间，后续按 Payloads 声明顺序推导无地址载荷的累计范围。 */
            MemoryBlock payloadMemory = extractMemoryBlock(doc, xpath, "payloadMemory",
                    "//*[local-name()='HardwareConfiguration']/*[local-name()='payloadMemory']/@Base_Address",
                    "//*[local-name()='HardwareConfiguration']/*[local-name()='payloadMemory']/@Size");

            /* 4. 提取所有 kernelIO */
            List<MemoryBlock> kernelIOBlocks = extractKernelIOBlocks(doc, xpath);

            /* 5. 顶层互斥约束 (e): PhysicalMemory, kernelIO(多个), userMemoryRegion 之间不能重叠 */
            List<MemoryBlock> topLevelBlocks = new ArrayList<>();
            if (physicalMemory != null && physicalMemory.exists)
                topLevelBlocks.add(physicalMemory);
            topLevelBlocks.addAll(kernelIOBlocks);
            if (userMemoryRegion != null && userMemoryRegion.exists)
                topLevelBlocks.add(userMemoryRegion);

            /* 004920：PhysicalMemory、kernelIO、userMemoryRegion属于顶层地址范围，
             * 任意两个重叠按需求报告3004；PhysicalMemory内部子块仍报告3005。 */
            if (!validateNoOverlap(topLevelBlocks, coreOSFileName, localErrors,
                    CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE)) {
                allValid = false;
            }

            /* 6. PhysicalMemory 内部互斥约束 (f): PhysicalMemory的子元素之间不能重叠 */
            /* 对仅有 Size、没有 Base_Address 的块，按 PhysicalMemory 子元素顺序推导地址，参与3005检查 */
            List<MemoryBlock> physicalMemoryInternalBlocks = extractPhysicalMemoryInternalBlocksForOverlap(doc, xpath,
                    physicalMemory);

            /* 检查子元素之间的互斥关系 */
            if (!validateNoOverlap(physicalMemoryInternalBlocks, coreOSFileName, localErrors)) {
                allValid = false;
            }

            /* 004909：ROM部署空间必须包含 CoreOS、共享库、配置记录和分区载荷。 */
            if (payloadMemory != null && payloadMemory.exists) {
                if (!validatePayloadMemoryContainsPayloads(doc, xpath, payloadMemory, fileName, localErrors)) {
                    allValid = false;
                }
            }

            /* 7. 验证 PhysicalMemory 大小必须大于等于所有子元素 Size 的总和 */
            if (physicalMemory != null && physicalMemory.exists) {
                if (!validateChildrenSizeSum(doc, xpath, physicalMemory, fileName, localErrors)) {
                    allValid = false;
                }
            }

            /* 第三部分：用户内存块包含关系验证 (3004) */
            /* 采用快速失败策略：一旦userMemoryRegion验证失败，跳过后续检查，避免多条3004错误（问题报告单12） */
            boolean userMemoryValidationPassed = true;

            /* 8. 验证 userMemoryRegion 包含所有共享数据内存块 */
            if (userMemoryRegion != null && userMemoryRegion.exists) {
                if (!validateUserMemoryContainsSharedData(doc, xpath, userMemoryRegion, xmlFile, localErrors)) {
                    allValid = false;
                    userMemoryValidationPassed = false; /* 标记userMemory验证失败 */
                }
            }

            /* 9. 验证 ramPayloadRegion 包含关系 */
            MemoryBlock ramPayloadRegion = extractPhysicalMemoryChild(doc, xpath, "ramPayloadRegion");
            if (ramPayloadRegion != null && ramPayloadRegion.exists) {
                /* ramPayloadRegion 应包含：核心操作系统RAM部署、分区操作系统共享库RAM部署、分区RAM部署 */
                if (!validateRamPayloadContainment(doc, xpath, ramPayloadRegion, xmlFile, localErrors)) {
                    allValid = false;
                }
            }

            /* 10. 验证 userMemoryRegion 包含分区内存块 */
            /* 只有在前面的userMemory验证通过时才执行，避免重复报错 */
            if (userMemoryValidationPassed && userMemoryRegion != null && userMemoryRegion.exists) {
                if (!validateUserMemoryContainsPartitionMemory(doc, xpath, userMemoryRegion, xmlFile, localErrors)) {
                    allValid = false;
                    userMemoryValidationPassed = false;
                }
            }

            /* 11. 验证分区RequiredMemorySize必须在userMemoryRegion地址范围内 */
            /* 只有在前面的userMemory验证通过时才执行，避免重复报错 */
            if (userMemoryValidationPassed && userMemoryRegion != null && userMemoryRegion.exists) {
                if (!validatePartitionRequiredMemorySizeInRange(doc, xpath, userMemoryRegion, xmlFile, localErrors)) {
                    allValid = false;
                }
            }

            if (allValid) {
                Log.info("Memory block relationship validation passed for file: " + fileName);
            }

        } catch (Exception e) {
            String errorMsg = "Memory block validation failed: " + e.getMessage();
            localErrors.add(errorMsg);
            Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
            allValid = false;
        }

        return allValid;
    }

    /**
     * extractMemoryBlock
     *
     * @brief
     * 功能: 按 XPath 提取内存块基础地址和大小。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] name 内存块名称。
     * @param[in] baseAddressXPath 起始地址属性 XPath。
     * @param[in] sizeXPath 大小属性 XPath。
     * @return 内存块对象；无法提取时返回 null。
     * @scope internal
     */
    private static MemoryBlock extractMemoryBlock(Document doc, XPath xpath, String name,
            String baseAddressXPath, String sizeXPath) throws XPathExpressionException {
        String baseAddressStr = (String) xpath.evaluate(baseAddressXPath, doc, XPathConstants.STRING);
        String sizeStr = (String) xpath.evaluate(sizeXPath, doc, XPathConstants.STRING);

        if (baseAddressStr == null || baseAddressStr.trim().isEmpty() ||
                sizeStr == null || sizeStr.trim().isEmpty()) {
            return new MemoryBlock(name, 0, 0, false, -1);
        }

        long baseAddress = parseHexOrDecimal(baseAddressStr.trim());
        long size = parseHexOrDecimal(sizeStr.trim());
        int lineNumber = getLineNumber(name);

        return new MemoryBlock(name, baseAddress, size, true, lineNumber);
    }

    /**
     * extractPhysicalMemoryChild
     *
     * @brief
     * 功能: 提取 PhysicalMemory 下指定子内存块。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] childName 子内存块元素名称。
     * @return 内存块对象；无法提取时返回 null。
     * @scope internal
     */
    private static MemoryBlock extractPhysicalMemoryChild(Document doc, XPath xpath, String childName)
            throws XPathExpressionException {
        String basePath = "//*[local-name()='PhysicalMemory']/*[local-name()='" + childName + "']";

        /* 尝试获取 Base_Address */
        String baseAddressStr = (String) xpath.evaluate(basePath + "/@Base_Address", doc, XPathConstants.STRING);
        String sizeStr = (String) xpath.evaluate(basePath + "/@Size", doc, XPathConstants.STRING);

        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return new MemoryBlock(childName, 0, 0, false, -1);
        }

        long size = parseHexOrDecimal(sizeStr.trim());
        int lineNumber = getLineNumber(childName);

        /* 如果有 Base_Address，使用它；否则这是动态分配的块，无法验证包含关系 */
        if (baseAddressStr != null && !baseAddressStr.trim().isEmpty()) {
            long baseAddress = parseHexOrDecimal(baseAddressStr.trim());
            return new MemoryBlock(childName, baseAddress, size, true, lineNumber);
        } else {
            /* 只有 Size，没有 Base_Address，标记为不存在（跳过包含关系验证） */
            /* 但对于互斥关系，这些块也无法验证 */
            return new MemoryBlock(childName, 0, size, false, lineNumber);
        }
    }

    /**
     * extractKernelRegions
     *
     * @brief
     * 功能: 提取所有 kernelRegion 内存块。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @return 内存块列表。
     * @scope internal
     */
    private static List<MemoryBlock> extractKernelRegions(Document doc, XPath xpath) throws XPathExpressionException {
        List<MemoryBlock> blocks = new ArrayList<>();

        NodeList nodes = (NodeList) xpath.evaluate(
                "//*[local-name()='PhysicalMemory']/*[local-name()='kernelRegion']", doc, XPathConstants.NODESET);

        for (int i = 0; i < nodes.getLength(); i++) {
            Element elem = (Element) nodes.item(i);
            String baseAddressStr = elem.getAttribute("Base_Address");
            String sizeStr = elem.getAttribute("Size");
            String poolName = elem.getAttribute("PoolName");

            String name = "kernelRegion" + (poolName != null && !poolName.isEmpty() ? "_" + poolName : "_" + i);

            if (sizeStr != null && !sizeStr.isEmpty()) {
                long size = parseHexOrDecimal(sizeStr);
                if (baseAddressStr != null && !baseAddressStr.isEmpty()) {
                    long baseAddress = parseHexOrDecimal(baseAddressStr);
                    /* 尝试获取带 PoolName 的行号，如果没有则使用 kernelRegion 的行号 */
                    int lineNumber = getLineNumber("kernelRegion_" + poolName);
                    if (lineNumber <= 0) {
                        lineNumber = getLineNumber("kernelRegion");
                    }
                    blocks.add(new MemoryBlock(name, baseAddress, size, true, lineNumber));
                }
                /* 如果只有 Size 没有 Base_Address，跳过 */
            }
        }

        return blocks;
    }

    /**
     * extractPhysicalMemoryInternalBlocksForOverlap
     *
     * @brief
     * 功能: 提取 PhysicalMemory 内部参与重叠校验的内存块。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] physicalMemory PhysicalMemory 内存块。
     * @return 内存块列表。
     * @scope internal
     */
    private static List<MemoryBlock> extractPhysicalMemoryInternalBlocksForOverlap(Document doc, XPath xpath,
            MemoryBlock physicalMemory) throws XPathExpressionException {

        List<MemoryBlock> blocks = new ArrayList<>();
        if (physicalMemory == null || !physicalMemory.exists) {
            return blocks;
        }

        NodeList childNodes = (NodeList) xpath.evaluate(
                "//*[local-name()='PhysicalMemory']/*",
                doc, XPathConstants.NODESET);

        long nextDerivedBase = physicalMemory.baseAddress;

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }

            Element elem = (Element) node;
            String localName = elem.getLocalName();

            if (!isPhysicalMemoryInternalBlockForOverlap(localName)) {
                continue;
            }

            String sizeStr = elem.getAttribute("Size");
            if (sizeStr == null || sizeStr.trim().isEmpty()) {
                continue;
            }

            long size = parseHexOrDecimal(sizeStr.trim());
            if (size <= 0) {
                continue;
            }

            String baseAddressStr = elem.getAttribute("Base_Address");
            boolean hasExplicitBase = baseAddressStr != null && !baseAddressStr.trim().isEmpty();
            long baseAddress = hasExplicitBase ? parseHexOrDecimal(baseAddressStr.trim()) : nextDerivedBase;

            String name = localName;
            if ("kernelRegion".equals(localName)) {
                String poolName = elem.getAttribute("PoolName");
                if (poolName != null && !poolName.isEmpty()) {
                    name = "kernelRegion_" + poolName;
                }
            }

            int lineNumber = getLineNumber(name);
            if (lineNumber <= 0) {
                lineNumber = getLineNumber(localName);
            }

            MemoryBlock block = new MemoryBlock(name, baseAddress, size, true, lineNumber);
            blocks.add(block);

            long blockEnd = block.getEndAddress();
            if (blockEnd > nextDerivedBase) {
                nextDerivedBase = blockEnd;
            }
        }

        return blocks;
    }

    /**
     * isPhysicalMemoryInternalBlockForOverlap
     *
     * @brief
     * 功能: 判断元素是否为 PhysicalMemory 内部重叠校验对象。
     * @param[in] localName 元素本地名称。
     * @return 是校验对象返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean isPhysicalMemoryInternalBlockForOverlap(String localName) {
        return "kernelMemoryRegion".equals(localName)
                || "kernelPgPool".equals(localName)
                || "portRegion".equals(localName)
                || "hmLogRegion".equals(localName)
                || "kernelRegion".equals(localName)
                || "ramPayloadRegion".equals(localName)
                || "kernelConfigRecordRegion".equals(localName)
                || "libRegion".equals(localName)
                || "userConfigRecordRegion".equals(localName);
    }

    /**
     * extractKernelIOBlocks
     *
     * @brief
     * 功能: 提取所有 kernelIO 内存块。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @return 内存块列表。
     * @scope internal
     */
    private static List<MemoryBlock> extractKernelIOBlocks(Document doc, XPath xpath) throws XPathExpressionException {
        List<MemoryBlock> blocks = new ArrayList<>();

        NodeList nodes = (NodeList) xpath.evaluate(
                "//*[local-name()='HardwareConfiguration']/*[local-name()='kernelIO']", doc, XPathConstants.NODESET);

        for (int i = 0; i < nodes.getLength(); i++) {
            Element elem = (Element) nodes.item(i);
            String baseAddressStr = elem.getAttribute("Base_Address");
            String sizeStr = elem.getAttribute("Size");
            String poolName = elem.getAttribute("PoolName");

            if (baseAddressStr != null && !baseAddressStr.isEmpty() &&
                    sizeStr != null && !sizeStr.isEmpty()) {
                long baseAddress = parseHexOrDecimal(baseAddressStr);
                long size = parseHexOrDecimal(sizeStr);
                String name = "kernelIO" + (poolName != null && !poolName.isEmpty() ? "_" + poolName : "_" + i);
                /* 尝试获取带 PoolName 的行号，如果没有则使用 kernelIO 的行号 */
                int lineNumber = getLineNumber("kernelIO_" + poolName);
                if (lineNumber <= 0) {
                    lineNumber = getLineNumber("kernelIO");
                }
                blocks.add(new MemoryBlock(name, baseAddress, size, true, lineNumber));
            }
        }

        return blocks;
    }

    /**
     * contains
     *
     * @brief
     * 功能: 判断一个内存块是否完全包含另一个内存块。
     * @param[in] container 父级内存块。
     * @param[in] contained 待检查的子内存块。
     * @return 完全包含返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean contains(MemoryBlock container, MemoryBlock contained) {
        if (!container.exists || !contained.exists) {
            return true;
        }
        return container.baseAddress <= contained.baseAddress &&
                container.getEndAddress() >= contained.getEndAddress();
    }

    /**
     * overlaps
     *
     * @brief
     * 功能: 判断两个内存块地址范围是否重叠。
     * @param[in] a 第一个内存块。
     * @param[in] b 第二个内存块。
     * @return 存在重叠返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean overlaps(MemoryBlock a, MemoryBlock b) {
        if (!a.exists || !b.exists) {
            return false;
        }
        /* 互斥条件：A结束 <= B开始 或 B结束 <= A开始 */
        /* 重叠 = 非互斥 */
        return !(a.getEndAddress() <= b.baseAddress || b.getEndAddress() <= a.baseAddress);
    }

    /**
     * validateNoOverlap
     *
     * @brief
     * 功能: 校验内存块集合内部不存在地址重叠。
     * @param[in] blocks 内存块列表。
     * @param[in] fileName XML 文件名。
     * @param[out] errors 错误信息列表。
     * @return 无重叠返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateNoOverlap(List<MemoryBlock> blocks, String fileName, List<String> errors) {
        return validateNoOverlap(blocks, fileName, errors, CodeConstant.ERROR_ADDRESS_OVERLAP);
    }

    /**
     * 校验内存块集合内部不存在地址重叠，并允许调用方指定需求对应的错误码。
     */
    private static boolean validateNoOverlap(List<MemoryBlock> blocks, String fileName, List<String> errors,
            int errorCode) {
        boolean valid = true;

        for (int i = 0; i < blocks.size(); i++) {
            for (int j = i + 1; j < blocks.size(); j++) {
                MemoryBlock a = blocks.get(i);
                MemoryBlock b = blocks.get(j);

                boolean overlap = overlaps(a, b);

                if (overlap) {
                    /* PR_32: 按照需求格式输出，包含文件名和行号 */
                    int lineNum = b.lineNumber > 0 ? b.lineNumber : (a.lineNumber > 0 ? a.lineNumber : 1);
                    String errorMsg;
                    if (errorCode == CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE) {
                        errorMsg = String.format(
                                "[File: %s, Line: %d] address %s out of range,the range is [0x%X, 0x%X]",
                                fileName, lineNum, formatAddress(b.baseAddress), a.baseAddress, a.getEndAddress());
                    } else {
                        errorMsg = String.format(
                                "[File: %s, Line: %d] the two address intervals with exclusion relation configuration is error.",
                                fileName, lineNum);
                    }
                    errors.add(errorMsg);
                    Log.error(errorCode, errorMsg);
                    valid = false;
                }
            }
        }

        return valid;
    }

    /**
     * formatAddress
     *
     * @brief
     * 功能: 执行 formatAddress 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String formatAddress(long address) {
        return String.format("0x%X", address);
    }

    /**
     * 004909：校验 ROM 部署空间对 Payloads 的包含关系。
     * Payload 本身没有 Size 属性，大小由其引用的描述文件或区域推导；
     * 没有 Base_Address 的 Payload 按 Payloads 的固定顺序连续布局。
     */
    private static boolean validatePayloadMemoryContainsPayloads(Document doc, XPath xpath,
            MemoryBlock payloadMemory, String fileName, List<String> errors) throws XPathExpressionException {
        NodeList payloadNodes = (NodeList) xpath.evaluate(
                "//*[local-name()='Payloads']/*", doc, XPathConstants.NODESET);
        if (payloadNodes.getLength() == 0) {
            return true;
        }

        boolean valid = true;
        long nextDerivedBase = payloadMemory.baseAddress;

        for (int i = 0; i < payloadNodes.getLength(); i++) {
            Node node = payloadNodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }

            Element payload = (Element) node;
            String payloadName = localName(payload);

            /*
             * 006371：Online 分区的 Base_Address 表示其在 kernelRegion 中的部署地址，
             * 不属于 payloadMemory 的 ROM 连续布局。该地址由
             * MiscellaneousConstraintValidator 按 Online 规则与首个 kernelRegion 比较，
             * 这里不能再按普通 PartitionPayload 触发 3004。
             */
            if ("PartitionPayload".equals(payloadName)
                    && "true".equalsIgnoreCase(payload.getAttribute("Online"))) {
                continue;
            }

            long size = extractPayloadSize(doc, xpath, payload);
            if (size <= 0) {
                /* 引用不存在或描述文件没有 MemorySize 时，交给引用/数据校验报告。 */
                continue;
            }

            String baseAddressStr = payload.getAttribute("Base_Address");
            if (baseAddressStr == null || baseAddressStr.trim().isEmpty()) {
                /* 兼容历史数据中的 BaseAddress 写法。 */
                baseAddressStr = payload.getAttribute("BaseAddress");
            }
            boolean hasExplicitBase = baseAddressStr != null && !baseAddressStr.trim().isEmpty();
            long baseAddress = hasExplicitBase
                    ? parseHexOrDecimal(baseAddressStr.trim())
                    : nextDerivedBase;

            MemoryBlock block = new MemoryBlock(payloadName, baseAddress, size, true,
                    getLineNumber(payloadName));
            if (!contains(payloadMemory, block)) {
                String errorMsg = String.format(
                        "[File: %s, Line: %d] address %s out of range,the range is [0x%X, 0x%X]",
                        fileName, block.lineNumber > 0 ? block.lineNumber : 1,
                        formatAddress(block.baseAddress), payloadMemory.baseAddress,
                        payloadMemory.getEndAddress());
                errors.add(errorMsg);
                Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
                valid = false;
            }

            /* 显式地址也是后续无地址载荷的布局锚点。 */
            nextDerivedBase = block.getEndAddress();
        }

        return valid;
    }

    /** 根据 Payload 类型推导其 ROM 部署大小（均不包含 BSS/PBSS）。 */
    private static long extractPayloadSize(Document doc, XPath xpath, Element payload)
            throws XPathExpressionException {
        String declaredSize = payload.getAttribute("Size");
        if (declaredSize != null && !declaredSize.trim().isEmpty()) {
            return parseHexOrDecimal(declaredSize.trim());
        }

        String payloadType = localName(payload);
        if ("CoreOSPayload".equals(payloadType)) {
            String memorySizePath = "//*[local-name()='CoreOSDescription']/*[local-name()='MemorySize']";
            NodeList memorySizes = (NodeList) xpath.evaluate(memorySizePath, doc, XPathConstants.NODESET);
            return memorySizes.getLength() > 0
                    ? sumMemorySizeAttributes((Element) memorySizes.item(0),
                            "MemorySizeText", "MemorySizeRoData", "MemorySizeData")
                    : 0;
        }

        if ("SharedLibraryPayload".equals(payloadType)) {
            String nameRef = payload.getAttribute("NameRef");
            NodeList sharedLibraries = (NodeList) xpath.evaluate(
                    "//*[local-name()='SharedLibraryRegions']/*[local-name()='SharedLibrary']",
                    doc, XPathConstants.NODESET);
            for (int i = 0; i < sharedLibraries.getLength(); i++) {
                Element sharedLibrary = (Element) sharedLibraries.item(i);
                if (!nameRef.equals(sharedLibrary.getAttribute("Name"))) {
                    continue;
                }
                NodeList memorySizes = sharedLibrary.getElementsByTagNameNS("*", "MemorySize");
                return memorySizes.getLength() > 0
                        ? sumMemorySizeAttributes((Element) memorySizes.item(0),
                                "MemorySizeText", "MemorySizeRoData", "MemorySizeData",
                                "MemorySizePersistentData")
                        : 0;
            }
            return 0;
        }

        if ("ConfigRecordPayload".equals(payloadType)) {
            String sizeStr = (String) xpath.evaluate(
                    "//*[local-name()='PhysicalMemory']/*[local-name()='kernelConfigRecordRegion']/@Size",
                    doc, XPathConstants.STRING);
            return parseHexOrDecimal(sizeStr);
        }

        if ("PartitionPayload".equals(payloadType)) {
            return extractPartitionPayloadSize(doc, xpath, payload.getAttribute("NameRef"));
        }

        /* SharedDataPayload 不属于 004909 规定的 ROM 三类载荷。 */
        return 0;
    }

    /**
     * extractPartitionPayloadSize
     *
     * @brief
     * 功能: 执行 extractPartitionPayloadSize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static long extractPartitionPayloadSize(Document doc, XPath xpath, String partitionName)
            throws XPathExpressionException {
        NodeList partitions = (NodeList) xpath.evaluate(
                "//*[local-name()='Partitions']/*[local-name()='Partition']", doc, XPathConstants.NODESET);
        NodeList applications = (NodeList) xpath.evaluate(
                "//*[local-name()='Applications']/*[local-name()='Application']", doc,
                XPathConstants.NODESET);

        for (int i = 0; i < partitions.getLength(); i++) {
            Element partition = (Element) partitions.item(i);
            if (!partitionName.equals(partition.getAttribute("Name"))) {
                continue;
            }

            long total = 0;
            NodeList appRefs = partition.getElementsByTagNameNS("*", "Application");
            for (int j = 0; j < appRefs.getLength(); j++) {
                Element appRef = (Element) appRefs.item(j);
                String nameRef = appRef.getAttribute("NameRef");
                for (int k = 0; k < applications.getLength(); k++) {
                    Element application = (Element) applications.item(k);
                    if (!nameRef.equals(application.getAttribute("Name"))) {
                        continue;
                    }
                    NodeList memorySizes = application.getElementsByTagNameNS("*", "MemorySize");
                    if (memorySizes.getLength() > 0) {
                        total += sumMemorySizeAttributes((Element) memorySizes.item(0),
                                "MemorySizeText", "MemorySizeRoData", "MemorySizeData",
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
                total += parseHexOrDecimal(value.trim());
            }
        }
        return total;
    }

    /**
     * localName
     *
     * @brief
     * 功能: 执行 localName 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String localName(Element element) {
        String name = element.getLocalName();
        return (name == null || name.isEmpty()) ? element.getNodeName() : name;
    }

    /**
     * validateChildrenSizeSum
     *
     * @brief
     * 功能: 校验 PhysicalMemory 子块大小总和不超过父块大小。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] physicalMemory PhysicalMemory 内存块。
     * @param[in] fileName XML 文件名。
     * @param[out] errors 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateChildrenSizeSum(Document doc, XPath xpath,
            MemoryBlock physicalMemory, String fileName, List<String> errors) throws XPathExpressionException {

        boolean valid = true;

        /*
         * 004919：没有 Base_Address 的子块不是“没有地址”，而是按 XML 中的
         * 声明顺序从 PhysicalMemory 起始地址连续分配。旧逻辑只比较每个
         * Size <= 父块 Size，两个子块各自合法时仍可能累计越过父块末尾。
         */
        long dynamicChildrenTotal = 0;
        int lastDynamicLineNumber = -1;
        NodeList allChildren = (NodeList) xpath.evaluate(
                "//*[local-name()='PhysicalMemory']/*", doc, XPathConstants.NODESET);
        for (int i = 0; i < allChildren.getLength(); i++) {
            Node node = allChildren.item(i);
            if (!(node instanceof Element)) {
                continue;
            }

            Element elem = (Element) node;
            String localName = elem.getLocalName();
            if (!isPhysicalMemoryInternalBlockForOverlap(localName)) {
                continue;
            }

            String sizeStr = elem.getAttribute("Size");
            String baseAddressStr = elem.getAttribute("Base_Address");
            if (sizeStr == null || sizeStr.trim().isEmpty()
                    || (baseAddressStr != null && !baseAddressStr.trim().isEmpty())) {
                continue;
            }

            long size = parseHexOrDecimal(sizeStr.trim());
            if (size <= 0) {
                continue;
            }

            dynamicChildrenTotal += size;
            int lineNumber = getLineNumber(localName);
            if (lineNumber > 0) {
                lastDynamicLineNumber = lineNumber;
            }
        }

        if (dynamicChildrenTotal > physicalMemory.size) {
            long derivedEndAddress = physicalMemory.baseAddress + dynamicChildrenTotal;
            String errorMsg = String.format(
                    "[File: %s, Line: %d] address %s out of range,the range is [0x%X, 0x%X]",
                    coreOSFileName, lastDynamicLineNumber > 0 ? lastDynamicLineNumber : 1,
                    formatAddress(derivedEndAddress), physicalMemory.baseAddress,
                    physicalMemory.getEndAddress());
            errors.add(errorMsg);
            Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
            valid = false;
        }

        /* 单实例子元素 */
        String[] singleChildren = {
                "kernelMemoryRegion",
                "kernelConfigRecordRegion",
                "kernelPgPool",
                "portRegion",
                "hmLogRegion",
                "ramPayloadRegion",
                "libRegion"
        };

        for (String childName : singleChildren) {
            NodeList nodes = (NodeList) xpath.evaluate(
                    "//*[local-name()='PhysicalMemory']/*[local-name()='" + childName + "']", doc,
                    XPathConstants.NODESET);

            if (nodes.getLength() > 0) {
                Element elem = (Element) nodes.item(0);
                String sizeStr = elem.getAttribute("Size");

                if (sizeStr != null && !sizeStr.isEmpty()) {
                    long size = parseHexOrDecimal(sizeStr);

                    /* 检查单个元素的 Size 是否超出父级大小 */
                    if (size > physicalMemory.size) {
                        int lineNumber = getLineNumber(childName);
                        String errorMsg = String.format(
                                "[File: %s, Line: %d] address %s out of range,the range is [0x%X, 0x%X]",
                                coreOSFileName, lineNumber > 0 ? lineNumber : 1, parseHexOrDecimal(sizeStr),
                                physicalMemory.baseAddress, physicalMemory.getEndAddress());
                        errors.add(errorMsg);
                        Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
                        valid = false;
                    }
                }
            }
        }

        /* 多实例子元素：kernelRegion */
        NodeList kernelRegionNodes = (NodeList) xpath.evaluate(
                "//*[local-name()='PhysicalMemory']/*[local-name()='kernelRegion']", doc, XPathConstants.NODESET);

        for (int i = 0; i < kernelRegionNodes.getLength(); i++) {
            Element elem = (Element) kernelRegionNodes.item(i);
            String sizeStr = elem.getAttribute("Size");
            String poolName = elem.getAttribute("PoolName");

            if (sizeStr != null && !sizeStr.isEmpty()) {
                long size = parseHexOrDecimal(sizeStr);

                String regionName = "kernelRegion"
                        + (poolName != null && !poolName.isEmpty() ? "_" + poolName : "_" + i);

                /* 检查单个元素的 Size 是否超出父级大小 */
                if (size > physicalMemory.size) {
                    int lineNumber = getLineNumber(
                            poolName != null && !poolName.isEmpty() ? "kernelRegion_" + poolName : "kernelRegion");
                    String errorMsg = String.format(
                            "[File: %s, Line: %d] address %s out of range,the range is [0x%X, 0x%X]",
                            coreOSFileName, lineNumber > 0 ? lineNumber : 1, size,
                            physicalMemory.baseAddress, physicalMemory.getEndAddress());
                    errors.add(errorMsg);
                    Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
                    valid = false;
                }
            }
        }

        return valid;
    }

    /**
     * validateUserMemoryContainsSharedData
     *
     * @brief
     * 功能: 校验 userMemoryRegion 包含所有共享数据内存块。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] userMemoryRegion 用户内存区域。
     * @param[in] xmlFile XML 配置文件。
     * @param[out] errors 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateUserMemoryContainsSharedData(Document doc, XPath xpath,
            MemoryBlock userMemoryRegion, File xmlFile, List<String> errors) throws Exception {

        boolean valid = true;
        String fileName = xmlFile.getName();
        File parentFolder = xmlFile.getParentFile();

        /* 从主文件（Module文件）中提取所有 SharedData 的 Size */
        /* SharedData 定义在 SharedDataRegions/SharedData 下，通过 xi:include 引用外部文件 */
        /* 外部文件中的 SharedDataDescription 元素包含 Size 属性 */

        long totalSharedDataSize = 0;

        /* 首先，需要从未展开XInclude的原始文档中找到所有SharedData引用的文件 */
        /* 重新解析原始文件（不展开XInclude） */
        Document docNoXInclude = XmlParser.parseXmlWithEnvSupport(xmlFile, false);

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpathNoXInclude = xPathFactory.newXPath();

        /* 查找所有 SharedData 元素，获取对应的文件名 */
        NodeList sharedDataNodes = (NodeList) xpathNoXInclude.evaluate(
                "//*[local-name()='SharedDataRegions']/*[local-name()='SharedData']",
                docNoXInclude, XPathConstants.NODESET);

        for (int i = 0; i < sharedDataNodes.getLength(); i++) {
            Element sharedDataElem = (Element) sharedDataNodes.item(i);
            String name = sharedDataElem.getAttribute("Name");

            /* 查找 xi:include 引用的文件 */
            NodeList includeNodes = sharedDataElem.getElementsByTagNameNS(
                    "http://www.w3.org/2001/XInclude", "include");

            String sharedDataFileName = null;
            long sharedDataSize = 0;

            if (includeNodes.getLength() > 0) {
                Element includeElem = (Element) includeNodes.item(0);
                String href = includeElem.getAttribute("href");

                if (href != null && !href.isEmpty()) {
                    sharedDataFileName = href;
                    /* 解析环境变量 */
                    String resolvedHref = XmlParser.resolveEnvVariables(href);
                    File sharedDataFile;
                    /* 如果解析后的路径是绝对路径，直接使用；否则相对于parentFolder */
                    if (new File(resolvedHref).isAbsolute()) {
                        sharedDataFile = new File(resolvedHref);
                    } else {
                        sharedDataFile = new File(parentFolder, resolvedHref);
                    }
                    if (sharedDataFile.exists()) {
                        sharedDataSize = extractSharedDataSize(sharedDataFile);
                    }
                }
            }

            /* 004911：允许 SharedDataDescription 直接写在当前文件中，不能只依赖 xi:include。 */
            if (includeNodes.getLength() == 0) {
                NodeList descriptions = sharedDataElem.getElementsByTagNameNS("*", "SharedDataDescription");
                if (descriptions.getLength() > 0) {
                    String sizeStr = ((Element) descriptions.item(0)).getAttribute("Size");
                    if (sizeStr != null && !sizeStr.trim().isEmpty()) {
                        sharedDataSize = parseHexOrDecimal(sizeStr.trim());
                    }
                }
            }

            totalSharedDataSize += sharedDataSize;
        }

        /* 验证：userMemoryRegion 的 Size 必须 >= 所有共享数据的 Size 总和 */
        /* 只报告一次错误，避免多条3004错误信息（问题报告单12） */
        if (totalSharedDataSize > 0 && totalSharedDataSize > userMemoryRegion.size) {
            int lineNumber = getLineNumber("userMemoryRegion");
            String errorMsg = String.format(
                    "[File: %s, Line: %d] address userMemoryRegion out of range, the range is [0x%X, 0x%X].",
                    coreOSFileName, lineNumber > 0 ? lineNumber : 1,
                    userMemoryRegion.baseAddress, userMemoryRegion.getEndAddress());
            errors.add(errorMsg);
            Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
            valid = false;
        } else if (totalSharedDataSize > 0) {
            Log.info("userMemoryRegion size (0x" + Long.toHexString(userMemoryRegion.size) +
                    ") can contain all SharedData (total: 0x" + Long.toHexString(totalSharedDataSize) + ")");
        }

        return valid;
    }

    /**
     * validateRamPayloadContainment
     *
     * @brief
     * 功能: 校验 ramPayloadRegion 包含各类 RAM 部署空间。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] ramPayloadRegion RAM 部署空间区域。
     * @param[in] xmlFile XML 配置文件。
     * @param[out] errors 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateRamPayloadContainment(Document doc, XPath xpath,
            MemoryBlock ramPayloadRegion, File xmlFile, List<String> errors) throws Exception {

        boolean valid = true;
        File parentFolder = xmlFile.getParentFile();

        long totalRequiredSize = 0;

        /* 1. 核心操作系统RAM部署内存块 - 从 CoreOS/MemorySize 获取 */
        /* MemorySizeText + MemorySizeRoData + MemorySizeData + MemorySizeBss */
        long coreOSRamSize = extractCoreOSMemorySize(doc, xpath, parentFolder);
        totalRequiredSize += coreOSRamSize;

        /* 2. 分区操作系统共享库RAM部署内存块 - 从 SharedLibrary/MemorySize 获取 */
        long sharedLibRamSize = extractSharedLibraryMemorySize(doc, xpath, parentFolder);
        totalRequiredSize += sharedLibRamSize;

        /* 3. 分区RAM部署内存块 - 从各 Partition/MemorySize 获取 */
        long partitionRamSize = extractPartitionMemorySize(doc, xpath, parentFolder);
        totalRequiredSize += partitionRamSize;

        Log.info("ramPayloadRegion containment check: " +
                "CoreOS RAM=" + String.format("0x%X", coreOSRamSize) +
                ", SharedLib RAM=" + String.format("0x%X", sharedLibRamSize) +
                ", Partition RAM=" + String.format("0x%X", partitionRamSize) +
                ", Total=" + String.format("0x%X", totalRequiredSize) +
                ", ramPayloadRegion Size=" + String.format("0x%X", ramPayloadRegion.size));

        /* 验证：ramPayloadRegion 的 Size 必须 >= 所有RAM部署内存块的总和 */
        if (totalRequiredSize > 0 && totalRequiredSize > ramPayloadRegion.size) {
            int lineNumber = getLineNumber("ramPayloadRegion");
            String errorMsg = String.format(
                    "[File: %s, Line: %d] address 0x%X out of range, the range is [0x%X, 0x%X].",
                    coreOSFileName, lineNumber > 0 ? lineNumber : 1,
                    ramPayloadRegion.baseAddress, ramPayloadRegion.baseAddress, ramPayloadRegion.getEndAddress());
            errors.add(errorMsg);
            Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
            valid = false;
        }

        return valid;
    }

    /**
     * extractCoreOSMemorySize
     *
     * @brief
     * 功能: 提取核心操作系统 RAM 部署占用大小。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] parentFolder 父目录。
     * @return 核心操作系统内存大小。
     * @scope internal
     */
    private static long extractCoreOSMemorySize(Document doc, XPath xpath, File parentFolder) {
        try {
            /* 尝试从已解析的文档中获取 */
            String textStr = (String) xpath.evaluate("//*[local-name()='MemorySize']/@MemorySizeText", doc,
                    XPathConstants.STRING);
            String roDataStr = (String) xpath.evaluate("//*[local-name()='MemorySize']/@MemorySizeRoData", doc,
                    XPathConstants.STRING);
            String dataStr = (String) xpath.evaluate("//*[local-name()='MemorySize']/@MemorySizeData", doc,
                    XPathConstants.STRING);
            String bssStr = (String) xpath.evaluate("//*[local-name()='MemorySize']/@MemorySizeBss", doc,
                    XPathConstants.STRING);

            long total = 0;
            if (textStr != null && !textStr.isEmpty())
                total += parseHexOrDecimal(textStr);
            if (roDataStr != null && !roDataStr.isEmpty())
                total += parseHexOrDecimal(roDataStr);
            if (dataStr != null && !dataStr.isEmpty())
                total += parseHexOrDecimal(dataStr);
            if (bssStr != null && !bssStr.isEmpty())
                total += parseHexOrDecimal(bssStr);

            return total;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * extractSharedLibraryMemorySize
     *
     * @brief
     * 功能: 提取分区操作系统共享库 RAM 部署占用大小。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] parentFolder 父目录。
     * @return 共享库内存大小。
     * @scope internal
     */
    private static long extractSharedLibraryMemorySize(Document doc, XPath xpath, File parentFolder) {
        long total = 0;
        try {
            /* 查找所有 SharedLibrary 的 MemorySize */
            NodeList nodes = (NodeList) xpath.evaluate(
                    "//*[local-name()='SharedLibrary']//*[local-name()='MemorySize']", doc, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); i++) {
                Element elem = (Element) nodes.item(i);
                String textStr = elem.getAttribute("MemorySizeText");
                String roDataStr = elem.getAttribute("MemorySizeRoData");
                String dataStr = elem.getAttribute("MemorySizeData");
                String bssStr = elem.getAttribute("MemorySizeBss");
                String persistentStr = elem.getAttribute("MemorySizePersistentData");

                if (textStr != null && !textStr.isEmpty())
                    total += parseHexOrDecimal(textStr);
                if (roDataStr != null && !roDataStr.isEmpty())
                    total += parseHexOrDecimal(roDataStr);
                if (dataStr != null && !dataStr.isEmpty())
                    total += parseHexOrDecimal(dataStr);
                if (bssStr != null && !bssStr.isEmpty())
                    total += parseHexOrDecimal(bssStr);
                if (persistentStr != null && !persistentStr.isEmpty())
                    total += parseHexOrDecimal(persistentStr);
            }
        } catch (Exception e) {
            /* 忽略 */
        }
        return total;
    }

    /**
     * extractPartitionMemorySize
     *
     * @brief
     * 功能: 汇总分区 RAM 部署占用大小。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] parentFolder 父目录。
     * @return 分区内存大小总和。
     * @scope internal
     */
    private static long extractPartitionMemorySize(Document doc, XPath xpath, File parentFolder) {
        long total = 0;
        try {
            /* 查找所有 Partition 的 MemorySize */
            NodeList nodes = (NodeList) xpath.evaluate(
                    "//*[local-name()='Partition']//*[local-name()='MemorySize']", doc, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); i++) {
                Element elem = (Element) nodes.item(i);
                String textStr = elem.getAttribute("MemorySizeText");
                String roDataStr = elem.getAttribute("MemorySizeRoData");
                String dataStr = elem.getAttribute("MemorySizeData");
                String bssStr = elem.getAttribute("MemorySizeBss");
                String persistentStr = elem.getAttribute("MemorySizePersistentData");

                if (textStr != null && !textStr.isEmpty())
                    total += parseHexOrDecimal(textStr);
                if (roDataStr != null && !roDataStr.isEmpty())
                    total += parseHexOrDecimal(roDataStr);
                if (dataStr != null && !dataStr.isEmpty())
                    total += parseHexOrDecimal(dataStr);
                if (bssStr != null && !bssStr.isEmpty())
                    total += parseHexOrDecimal(bssStr);
                if (persistentStr != null && !persistentStr.isEmpty())
                    total += parseHexOrDecimal(persistentStr);
            }

            /*
             * 004910：标准 Module 中 Partition 只保存 Application/@NameRef，
             * ApplicationDescription/MemorySize 位于全局 Applications 节点。
             * 当 Partition 子树没有展开出 MemorySize 时，必须沿 NameRef 回查，
             * 并累计所有分区引用的应用，不能把分区部署空间当成 0。
             */
            if (total == 0) {
                NodeList partitions = (NodeList) xpath.evaluate(
                        "//*[local-name()='Partitions']/*[local-name()='Partition']", doc,
                        XPathConstants.NODESET);
                NodeList applications = (NodeList) xpath.evaluate(
                        "//*[local-name()='Applications']/*[local-name()='Application']", doc,
                        XPathConstants.NODESET);

                for (int i = 0; i < partitions.getLength(); i++) {
                    Element partition = (Element) partitions.item(i);
                    NodeList appRefs = partition.getElementsByTagNameNS("*", "Application");
                    for (int j = 0; j < appRefs.getLength(); j++) {
                        String nameRef = ((Element) appRefs.item(j)).getAttribute("NameRef");
                        for (int k = 0; k < applications.getLength(); k++) {
                            Element application = (Element) applications.item(k);
                            if (!nameRef.equals(application.getAttribute("Name"))) {
                                continue;
                            }
                            NodeList memorySizes = application.getElementsByTagNameNS("*", "MemorySize");
                            if (memorySizes.getLength() > 0) {
                                total += sumMemorySizeAttributes((Element) memorySizes.item(0),
                                        "MemorySizeText", "MemorySizeRoData", "MemorySizeData",
                                        "MemorySizeBss", "MemorySizePersistentData",
                                        "MemorySizePersistentBss");
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            /* 忽略 */
        }
        return total;
    }

    /**
     * validateUserMemoryContainsPartitionMemory
     *
     * @brief
     * 功能: 校验 userMemoryRegion 包含所有分区内存需求。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] userMemoryRegion 用户内存区域。
     * @param[in] xmlFile XML 配置文件。
     * @param[out] errors 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validateUserMemoryContainsPartitionMemory(Document doc, XPath xpath,
            MemoryBlock userMemoryRegion, File xmlFile, List<String> errors) throws Exception {

        boolean valid = true;

        long totalPartitionMemorySize = 0;

        try {
            /*
             * 004911：分区内存块的实际空间由 Settings/RequiredMemorySize 决定。
             * 必须先累计所有分区，再与 userMemoryRegion 比较；逐个比较会漏掉
             * “每个分区都能放下、总和却超出”的情况。
             */
            long requiredMemoryTotal = 0;
            NodeList settingsNodes = (NodeList) xpath.evaluate(
                    "//*[local-name()='Partitions']//*[local-name()='Settings']", doc,
                    XPathConstants.NODESET);
            for (int i = 0; i < settingsNodes.getLength(); i++) {
                Element settings = (Element) settingsNodes.item(i);
                String requiredMemorySize = settings.getAttribute("RequiredMemorySize");
                if (requiredMemorySize != null && !requiredMemorySize.trim().isEmpty()) {
                    requiredMemoryTotal += parseHexOrDecimal(requiredMemorySize.trim());
                }
            }

            /* 查找所有 Partition 的 PartitionMemory 或 Memory 配置 */
            /* 通常在 Partition/PartitionMemory 或 Partition/Memory 下 */
            NodeList nodes = (NodeList) xpath.evaluate(
                    "//*[local-name()='Partition']//*[local-name()='PartitionMemory' or local-name()='Memory']",
                    doc, XPathConstants.NODESET);

            if (requiredMemoryTotal > 0) {
                totalPartitionMemorySize = requiredMemoryTotal;
            } else {
                long declaredPartitionMemorySize = 0;
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element elem = (Element) nodes.item(i);
                    String sizeStr = elem.getAttribute("Size");
                    if (sizeStr != null && !sizeStr.isEmpty()) {
                        declaredPartitionMemorySize += parseHexOrDecimal(sizeStr);
                    }
                }
                totalPartitionMemorySize = declaredPartitionMemorySize > 0
                        ? declaredPartitionMemorySize
                        : extractPartitionMemorySize(doc, xpath, xmlFile.getParentFile());
            }

        } catch (Exception e) {
            /* 忽略 */
        }

        /* 验证：userMemoryRegion 的 Size 必须 >= 所有分区内存块的总和 */
        if (totalPartitionMemorySize > 0 && totalPartitionMemorySize > userMemoryRegion.size) {
            int lineNumber = getLineNumber("userMemoryRegion");
            String errorMsg = String.format(
                    "[File: %s, Line: %d] address userMemoryRegion out of range, the range is [0x%X, 0x%X].",
                    coreOSFileName, lineNumber > 0 ? lineNumber : 1,
                    userMemoryRegion.baseAddress, userMemoryRegion.getEndAddress());
            errors.add(errorMsg);
            Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
            valid = false;
        } else if (totalPartitionMemorySize > 0) {
            Log.info("userMemoryRegion size (0x" + Long.toHexString(userMemoryRegion.size) +
                    ") can contain all Partition memory (total: 0x" + Long.toHexString(totalPartitionMemorySize) + ")");
        }

        return valid;
    }

    /**
     * validatePartitionRequiredMemorySizeInRange
     *
     * @brief
     * 功能: 校验分区 RequiredMemorySize 位于 userMemoryRegion 范围内。
     * @param[in] doc XML DOM 文档。
     * @param[in] xpath XPath 查询对象。
     * @param[in] userMemoryRegion 用户内存区域。
     * @param[in] xmlFile XML 配置文件。
     * @param[out] errors 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validatePartitionRequiredMemorySizeInRange(Document doc, XPath xpath,
            MemoryBlock userMemoryRegion, File xmlFile, List<String> errors) {
        boolean valid = true;

        try {
            /* 查找所有分区文件（包括当前文件和引用的分区文件） */
            File moduleDir = xmlFile.getParentFile();

            /* 如果当前文件是模块文件，查找所有分区文件 */
            if (xmlFile.getName().toLowerCase().contains("module")) {
                /* 查找所有 Partition 元素 */
                NodeList partitionNodes = (NodeList) xpath.evaluate(
                        "//*[local-name()='Partition']", doc, XPathConstants.NODESET);

                for (int i = 0; i < partitionNodes.getLength(); i++) {
                    Element partition = (Element) partitionNodes.item(i);
                    String partitionName = partition.getAttribute("Name");

                    /* 查找分区对应的 _part.xml 文件 */
                    File partitionFile = new File(moduleDir, partitionName + "_part.xml");
                    if (partitionFile.exists()) {
                        /* 解析分区文件并验证 */
                        if (!validatePartitionFileRequiredMemorySize(partitionFile, userMemoryRegion, errors)) {
                            valid = false;
                        }
                    }
                }
            } else if (xmlFile.getName().toLowerCase().endsWith("_part.xml")) {
                /* 如果当前文件就是分区文件，直接验证 */
                if (!validatePartitionFileRequiredMemorySize(xmlFile, userMemoryRegion, errors)) {
                    valid = false;
                }
            }

        } catch (Exception e) {
            /* 忽略异常 */
        }

        return valid;
    }

    /**
     * validatePartitionFileRequiredMemorySize
     *
     * @brief
     * 功能: 校验单个分区文件的 RequiredMemorySize 范围。
     * @param[in] partitionFile 分区 XML 文件。
     * @param[in] userMemoryRegion 用户内存区域。
     * @param[out] errors 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope internal
     */
    private static boolean validatePartitionFileRequiredMemorySize(File partitionFile,
            MemoryBlock userMemoryRegion, List<String> errors) {
        try {
            /* 解析分区文件 */
            Document partDoc = XmlParser.parseXmlWithEnvSupport(partitionFile, false);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            /* 查找Settings元素的RequiredMemorySize属性 */
            NodeList settingsList = (NodeList) xpath.evaluate(
                    "//*[local-name()='Settings']", partDoc, XPathConstants.NODESET);

            if (settingsList.getLength() > 0) {
                Element settings = (Element) settingsList.item(0);
                String requiredMemorySizeStr = settings.getAttribute("RequiredMemorySize");

                if (requiredMemorySizeStr != null && !requiredMemorySizeStr.isEmpty()) {
                    long requiredMemorySize = parseHexOrDecimal(requiredMemorySizeStr);

                    /* 检查RequiredMemorySize是否超出userMemoryRegion的大小范围 */
                    /* RequiredMemorySize表示分区需要的虚拟内存大小，必须在userMemoryRegion的Size范围内 */
                    if (requiredMemorySize > userMemoryRegion.size) {
                        /* 获取行号 */
                        int lineNumber = getSettingsLineNumber(partitionFile);
                        if (lineNumber < 1) {
                            lineNumber = 1;
                        }

                        String errorMsg = String.format(
                                "[File: %s, Line: %d] address %s Out of range,the range is [0x%X，0x%X].",
                                partitionFile.getName(),
                                lineNumber,
                                requiredMemorySizeStr,
                                userMemoryRegion.baseAddress,
                                userMemoryRegion.getEndAddress());

                        errors.add(errorMsg);
                        Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
                        return false;
                    }
                }
            }

        } catch (Exception e) {
            /* 忽略解析错误 */
        }

        return true;
    }

    /**
     * getSettingsLineNumber
     *
     * @brief
     * 功能: 获取分区文件 Settings 元素行号。
     * @param[in] partitionFile 分区 XML 文件。
     * @return Settings 元素行号。
     * @scope internal
     */
    private static int getSettingsLineNumber(File partitionFile) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();

            final int[] lineNumber = { -1 };

            DefaultHandler handler = new DefaultHandler() {
                private Locator locator;

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
                    if ("Settings".equals(localName) || "Settings".equals(qName)) {
                        lineNumber[0] = locator.getLineNumber();
                    }
                }
            };

            saxParser.parse(partitionFile, handler);
            return lineNumber[0];

        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * extractSharedDataSize
     *
     * @brief
     * 功能: 提取共享数据文件中的 Size 属性值。
     * @param[in] sharedDataFile 共享数据 XML 文件。
     * @return 共享数据大小。
     * @scope internal
     */
    private static long extractSharedDataSize(File sharedDataFile) {
        try {
            Document doc = XmlParser.parseXmlWithEnvSupport(sharedDataFile, false);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            String sizeStr = (String) xpath.evaluate(
                    "//*[local-name()='SharedDataDescription']/@Size", doc, XPathConstants.STRING);

            if (sizeStr != null && !sizeStr.isEmpty()) {
                return parseHexOrDecimal(sizeStr);
            }
        } catch (Exception e) {
            /* 忽略解析错误 */
        }
        return 0;
    }

    /**
     * buildElementLineMap
     *
     * @brief
     * 功能: 构建 XML 元素名称与行号映射。
     * @param[in] xmlFile XML 配置文件。
     * @return 无。
     * @scope internal
     */
    private static void buildElementLineMap(File xmlFile) {
        elementLineMap.clear();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {
                private Locator locator;
                private Stack<String> elementStack = new Stack<>();

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
                    if (locator != null) {
                        int lineNumber = locator.getLineNumber();
                        elementStack.push(localName);

                        /* 存储元素名 -> 行号（会被后续同名元素覆盖） */
                        elementLineMap.put(localName, lineNumber);

                        /* 对于有 PoolName 属性的元素，存储特定的键 */
                        String poolName = attributes.getValue("PoolName");
                        if (poolName != null && !poolName.isEmpty()) {
                            elementLineMap.put(localName + "_" + poolName, lineNumber);
                        }

                        /* 对于特定的关键元素，同时记录完整路径以避免冲突 */
                        if ("ramPayloadRegion".equals(localName) ||
                                "userMemoryRegion".equals(localName) ||
                                "libRegion".equals(localName) ||
                                "kernelIO".equals(localName) ||
                                "payloadMemory".equals(localName)) {
                            /* 生成完整路径作为备用键 */
                            String path = String.join("/", elementStack);
                            elementLineMap.put(path, lineNumber);
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
                public void endElement(String uri, String localName, String qName) {
                    if (!elementStack.isEmpty()) {
                        elementStack.pop();
                    }
                }
            };

            org.xml.sax.InputSource inputSource = XmlParser.preprocessXmlFile(xmlFile);
            saxParser.parse(inputSource, handler);
        } catch (Exception e) {
            /* 忽略解析错误，行号将使用默认值 -1 */
        }
    }

    /**
     * getLineNumber
     *
     * @brief
     * 功能: 获取当前文件中指定元素的行号。
     * @param[in] elementName 元素名称。
     * @return 元素行号。
     * @scope internal
     */
    private static int getLineNumber(String elementName) {
        Integer lineNumber = elementLineMap.get(elementName);
        return lineNumber != null ? lineNumber : -1;
    }



    /**
     * parseHexOrDecimal
     *
     * @brief
     * 功能: 解析十六进制或十进制数值字符串。
     * @param[in] value 数值字符串。
     * @return 解析后的数值。
     * @scope internal
     */
    private static long parseHexOrDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        value = value.trim();
        try {
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseLong(value.substring(2), 16);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * findCoreOSFile
     *
     * @brief
     * 功能: 查找与模块配置关联的 coreOS.xml 文件。
     * @param[in] moduleXmlFile 模块 XML 文件。
     * @return coreOS.xml 文件；未找到时返回 null。
     * @scope internal
     */
    private static File findCoreOSFile(File moduleXmlFile) {
        try {
            /* 首先检查同目录下是否有coreOS.xml */
            File localCoreOS = new File(moduleXmlFile.getParentFile(), "coreOS.xml");
            if (localCoreOS.exists()) {
                return localCoreOS;
            }

            /* 解析module文件，查找CoreOS元素下的xi:include引用 */
            Document doc = XmlParser.parseXmlWithEnvSupport(moduleXmlFile, false);

            /* 查找 CoreOS 下的 xi:include */
            NodeList coreOSNodes = doc.getElementsByTagNameNS("*", "CoreOS");
            if (coreOSNodes.getLength() > 0) {
                Element coreOSElem = (Element) coreOSNodes.item(0);
                NodeList includeNodes = coreOSElem.getElementsByTagNameNS(
                        "http://www.w3.org/2001/XInclude", "include");
                if (includeNodes.getLength() > 0) {
                    Element includeElem = (Element) includeNodes.item(0);
                    String href = includeElem.getAttribute("href");
                    if (href != null && !href.isEmpty()) {
                        /* 解析环境变量 */
                        String resolvedHref = XmlParser.resolveEnvVariables(href);
                        File resolvedFile;
                        if (new File(resolvedHref).isAbsolute()) {
                            resolvedFile = new File(resolvedHref);
                        } else {
                            resolvedFile = new File(moduleXmlFile.getParentFile(), resolvedHref);
                        }
                        if (resolvedFile.exists()) {
                            return resolvedFile;
                        }
                    }
                }
            }
        } catch (Exception e) {
            /* 忽略错误，返回null */
        }
        return null;
    }
}
