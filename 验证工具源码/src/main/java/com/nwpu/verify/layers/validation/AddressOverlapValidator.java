/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: AddressOverlapValidator.java
 * 
 * 描述    : 校验XML配置文件中的地址区间是否存在重叠。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
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
 * 功能: 校验XML配置文件中的地址区间是否存在重叠。
 */
public class AddressOverlapValidator {

    /**
     * @brief
     * 功能: 保存参与地址重叠检查的单个地址区间信息。
     */
    private static class AddressInterval {
        String elementName; /* 元素名称 */
        String addressAttribute; /* 地址属性名 */
        String sizeAttribute; /* 大小属性名 */
        long baseAddress; /* 基地址 */
        long size; /* 区间大小 */
        int lineNumber; /* 行号 */
        int groupId; /* 分组ID，同组内的元素需要检查互斥 */

        AddressInterval(String elementName, String addressAttribute, String sizeAttribute,
                long baseAddress, long size, int lineNumber, int groupId) {
            this.elementName = elementName;
            this.addressAttribute = addressAttribute;
            this.sizeAttribute = sizeAttribute;
            this.baseAddress = baseAddress;
            this.size = size;
            this.lineNumber = lineNumber;
            this.groupId = groupId;
        }

        /**
         * getEndAddress
         *
         * @brief
         * 功能: 根据基地址和区间大小计算区间结束地址。
         * 
         * @return
         * long: 区间结束地址。
         * @implements 
         * @scope internal
         * @errno:
         * N/A
         */
        long getEndAddress() {
            return baseAddress + size - 1;
        }

        /**
         * overlapsWith
         *
         * @brief
         * 功能: 判断当前地址区间是否与另一个地址区间发生重叠。
         *       重叠判定关系如下：
         *       当前区间起始地址 <= 另一区间结束地址；
         *       另一区间起始地址 <= 当前区间结束地址。
         * 
         * @param[in]  other: 待比较的另一个地址区间。
         * @return
         * boolean: 存在重叠返回true，否则返回false。
         * @implements 
         * @scope internal
         * @errno:
         * N/A
         */
        boolean overlapsWith(AddressInterval other) {
            /* 两个区间 [start1, end1] 和 [start2, end2] 重叠的条件： */
            /* start1 <= end2 && start2 <= end1 */
            return this.baseAddress <= other.getEndAddress() &&
                    other.baseAddress <= this.getEndAddress();
        }

        /**
         * getDescription
         *
         * @brief
         * 功能: 格式化地址区间描述信息，用于错误消息输出。
         * 
         * @return
         * String: 地址区间描述字符串。
         * @implements 
         * @scope internal
         * @errno:
         * N/A
         */
        String getDescription() {
            return String.format("%s[%s=0x%X, %s=0x%X]",
                    elementName, addressAttribute, baseAddress, sizeAttribute, size);
        }
    }

    /**
     * 互斥分组定义
     * 同一组内的内存块必须互斥（不能重叠）
     * 不同组的内存块关系由 MemoryBlockValidator 处理
     */
    private static final int GROUP_TOP_LEVEL = 1; /* 顶层内存块：PhysicalMemory, kernelIO, userMemoryRegion */
    private static final int GROUP_INSIDE_PHYSICAL = 2; /* PhysicalMemory 内部：ramPayloadRegion, libRegion */
    private static final int GROUP_SHARED_DATA = 3; /* 共享数据区域 */
    private static final int GROUP_PARTITION = 4; /* 分区内存区域 */
    private static final int GROUP_OTHER = 99; /* 其他，单独检查 */

    /**
     * 需要检查地址重叠的元素配置
     * 格式：{ 元素名, 地址属性名, 大小属性名, 分组ID }
     * 
     * 根据需求CPMGEN_TR_021231：
     * 只有物理空间内存块(PhysicalMemory)、内核IO空间内存块(kernelIO)、用户内存块(userMemoryRegion)
     * 之中任意两个内存块之间有重叠时才报3005错误
     */
    private static final Object[][] ADDRESS_INTERVAL_CONFIGS = {
            /* 顶层内存块（互斥组1） */
            /* 根据需求CPMGEN_TR_021231，只检查这三者之间的互斥：PhysicalMemory, kernelIO, userMemoryRegion */
            { "PhysicalMemory", "Base_Address", "Size", GROUP_TOP_LEVEL },
            { "userMemoryRegion", "Base_Address", "Size", GROUP_TOP_LEVEL },
            { "kernelIO", "Base_Address", "Size", GROUP_TOP_LEVEL },
            /* 注意：sharedIO和payloadMemory不在需求规定的互斥检查范围内，已移除 */

            /* PhysicalMemory 内部子块（互斥组2） */
            /* 这些内存块在 PhysicalMemory 内部，相互之间必须互斥 */
            /* 与 PhysicalMemory 的包含关系由 MemoryBlockValidator 检查 */
            { "ramPayloadRegion", "Base_Address", "Size", GROUP_INSIDE_PHYSICAL },
            { "libRegion", "Base_Address", "Size", GROUP_INSIDE_PHYSICAL },
            { "userConfigRecordRegion", "Base_Address", "Size", GROUP_INSIDE_PHYSICAL },
            { "ACE_MEM_POOL", "Base_Address", "Size", GROUP_INSIDE_PHYSICAL },

            /* 共享数据区域（互斥组3） */
            { "SharedDataDescription", "VirtualAddress", "Size", GROUP_SHARED_DATA },

            /* 分区内存区域（互斥组4） */
            { "SharedDataRegion", "Address", "Size", GROUP_PARTITION }
    };

    /**
     * validateAddressOverlap
     *
     * @brief
     * 功能: 验证XML文件中的地址区间是否存在重叠。
     *       处理流程如下：
     *       1. XML文件为空或不存在时跳过验证；
     *       2. 使用SAX解析器收集配置中的地址区间；
     *       3. 仅对同一互斥分组内的地址区间进行重叠判断；
     *       4. 发现重叠时记录3005错误并返回验证失败。
     * 
     * @param[in]  xmlFile: 待验证的XML文件。
     * @param[out] errorMessages: 错误信息列表。
     * @return
     * boolean: 不存在重叠返回true，存在重叠或解析异常返回false。
     * @implements CPMGEN_TR_021231
     * @scope external
     * @errno:
     * ERROR_ADDRESS_OVERLAP: 地址区间存在互斥关系配置错误。
     */
    public static boolean validateAddressOverlap(File xmlFile, List<String> errorMessages) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true; /* 文件不存在，跳过验证 */
        }

        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();

        try {
            /* 使用SAX解析器收集所有地址区间 */
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();

            AddressIntervalCollector handler = new AddressIntervalCollector();
            parser.parse(xmlFile, handler);

            List<AddressInterval> intervals = handler.getIntervals();

            /* 按分组检查重叠（只检查同组内的重叠） */
            boolean allPassed = true;
            for (int i = 0; i < intervals.size(); i++) {
                for (int j = i + 1; j < intervals.size(); j++) {
                    AddressInterval interval1 = intervals.get(i);
                    AddressInterval interval2 = intervals.get(j);

                    /* 只检查同组内的重叠 */
                    if (interval1.groupId != interval2.groupId) {
                        continue;
                    }

                    if (interval1.overlapsWith(interval2)) {
                        /* 发现重叠 */
                        /* PR_32: 按照需求格式输出 */
                        String errorMsg = String.format(
                                "[File: %s, Line: %d] the two address intervals with exclusion relation configuration is error.",
                                xmlFile.getName(),
                                interval2.lineNumber);

                        localErrors.add(errorMsg);
                        Log.error(CodeConstant.ERROR_ADDRESS_OVERLAP, errorMsg);
                        allPassed = false;
                    }
                }
            }

            return allPassed;

        } catch (Exception e) {
            String errorMsg = String.format(
                    "[File: %s, Line: 1] the two address intervals with exclusion relation configuration is error.",
                    xmlFile.getName());
            localErrors.add(errorMsg);
            Log.error(CodeConstant.ERROR_ADDRESS_OVERLAP, errorMsg);
            return false;
        }
    }

    /**
     * @brief
     * 功能: 在SAX解析过程中收集需要参与重叠检查的地址区间。
     */
    private static class AddressIntervalCollector extends DefaultHandler {
        private Locator locator;
        private List<AddressInterval> intervals = new ArrayList<>();
        private Map<String, Object[]> configMap = new HashMap<>();

        public AddressIntervalCollector() {
            /* 构建元素名到配置的映射 */
            for (Object[] config : ADDRESS_INTERVAL_CONFIGS) {
                configMap.put((String) config[0], config);
            }
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
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            String elementName = localName.isEmpty() ? qName : localName;

            /* 检查是否是我们关注的元素 */
            Object[] config = configMap.get(elementName);
            if (config != null) {
                String addressAttr = (String) config[1];
                String sizeAttr = (String) config[2];
                int groupId = (Integer) config[3];

                String addressValue = attributes.getValue(addressAttr);
                String sizeValue = attributes.getValue(sizeAttr);

                if (addressValue != null && !addressValue.isEmpty() &&
                        sizeValue != null && !sizeValue.isEmpty()) {

                    long baseAddress = parseAddress(addressValue);
                    long size = parseAddress(sizeValue);

                    if (baseAddress != -1 && size != -1 && size > 0) {
                        int lineNumber = (locator != null) ? locator.getLineNumber() : -1;
                        intervals.add(new AddressInterval(
                                elementName, addressAttr, sizeAttr,
                                baseAddress, size, lineNumber, groupId));
                    }
                }
            }
        }

        /**
         * getIntervals
         *
         * @brief
         * 功能: 执行 getIntervals 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        public List<AddressInterval> getIntervals() {
            return intervals;
        }

        /**
         * parseAddress
         *
         * @brief
         * 功能: 解析地址字符串，支持十进制和0x/0X前缀的十六进制格式。
         *       解析规则如下：
         *       空值或空白字符串返回-1；
         *       以+开头时先移除正号；
         *       以0x或0X开头时按十六进制解析；
         *       其他情况按十进制解析。
         * 
         * @param[in]  value: 待解析的地址字符串。
         * @return
         * long: 解析后的地址值；解析失败返回-1。
         * @implements 
         * @scope internal
         * @errno:
         * NumberFormatException: 地址字符串格式非法时捕获并返回-1。
         */
        private long parseAddress(String value) {
            if (value == null || value.trim().isEmpty()) {
                return -1;
            }

            try {
                value = value.trim();

                /* 移除可能的正负号 */
                if (value.startsWith("+")) {
                    value = value.substring(1);
                }

                if (value.startsWith("0x") || value.startsWith("0X")) {
                    /* 十六进制 */
                    return Long.parseLong(value.substring(2), 16);
                } else {
                    /* 十进制 */
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }
}
