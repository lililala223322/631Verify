/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: AddressRangeValidator.java
 * 
 * 描述    : 校验XML配置文件中的地址值是否处于有效范围。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

/**
 * @brief
 * 功能: 校验XML配置文件中的地址值是否处于有效范围。
 */
public class AddressRangeValidator {
    
    /**
     * @brief
     * 功能: 保存单类XML元素地址属性的有效范围配置。
     */
    private static class AddressRangeConfig {
        String elementName;
        String attributeName;
        long minAddress;
        long maxAddress;
        
        AddressRangeConfig(String elementName, String attributeName, long minAddress, long maxAddress) {
            this.elementName = elementName;
            this.attributeName = attributeName;
            this.minAddress = minAddress;
            this.maxAddress = maxAddress;
        }
    }
    
    /**
     * 地址范围配置列表
     * 根据ARINC 653规范和系统架构定义
     * 
     * 默认范围（32位系统）：
     * - 物理地址：0x00000000 - 0xFFFFFFFF
     * - 虚拟地址：0x00000000 - 0xFFFFFFFF
     * - 载荷基地址：0x00000000 - 0xFFFFFFFF
     */
    private static final List<AddressRangeConfig> ADDRESS_RANGE_CONFIGS = Arrays.asList(
        /* CoreOS地址范围 */
        new AddressRangeConfig("CoreOS", "PhysicalAddress", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("CoreOS", "VirtualAddress", 0x00000000L, 0xFFFFFFFFL),
        
        /* PhysicalMemory地址范围 */
        new AddressRangeConfig("PhysicalMemory", "Base_Address", 0x00000000L, 0xFFFFFFFFL),
        
        /* 内存区域地址范围 */
        new AddressRangeConfig("ramPayloadRegion", "Base_Address", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("libRegion", "Base_Address", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("userMemoryRegion", "Base_Address", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("kernelIO", "Base_Address", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("kernelIO", "VirtualAddress", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("payloadMemory", "Base_Address", 0x00000000L, 0xFFFFFFFFL),
        
        /* SharedLibrary地址范围 */
        new AddressRangeConfig("SharedLibrary", "VirtualAddress", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("SharedLibraryDescription", "VirtualAddress", 0x00000000L, 0xFFFFFFFFL),
        
        /* SharedData地址范围 */
        new AddressRangeConfig("SharedData", "VirtualAddress", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("SharedDataDescription", "VirtualAddress", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("SharedDataRegion", "Address", 0x00000000L, 0xFFFFFFFFL),
        
        /* SharedIO地址范围 */
        new AddressRangeConfig("SharedIODescription", "VirtualAddress", 0x00000000L, 0xFFFFFFFFL),
        
        /* Partition地址范围 */
        new AddressRangeConfig("Settings", "partitionVirtualAddress", 0x00000000L, 0xFFFFFFFFL),
        
        /* Payload地址范围 */
        new AddressRangeConfig("CoreOSPayload", "Base_Address", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("SharedLibraryPayload", "Base_Address", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("ConfigRecordPayload", "Base_Address", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("SharedDataPayload", "Base_Address", 0x00000000L, 0xFFFFFFFFL),
        new AddressRangeConfig("PartitionPayload", "Base_Address", 0x00000000L, 0xFFFFFFFFL)
    );
    
    /**
     * validateAddressRange
     *
     * @brief
     * 功能: 验证XML文件中的地址属性值是否位于配置的有效范围内。
     *       处理流程如下：
     *       1. XML文件为空或不存在时跳过验证；
     *       2. 使用SAX解析器读取元素和属性并获取行号；
     *       3. 根据ADDRESS_RANGE_CONFIGS匹配需要校验的地址属性；
     *       4. 地址值越界时记录3004错误。
     * 
     * @param[in]  xmlFile: 待验证的XML文件。
     * @param[out] errorMessages: 错误信息列表。
     * @return
     * boolean: 全部地址位于有效范围内返回true，否则返回false。
     * @implements 
     * @scope external
     * @errno:
     * ERROR_ADDRESS_OUT_OF_RANGE: 地址值超出允许范围。
     */
    public static boolean validateAddressRange(File xmlFile, List<String> errorMessages) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true; /* 文件不存在，跳过验证 */
        }
        
        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();
        
        try {
            /* 使用SAX解析器，可以准确获取行号 */
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            
            AddressRangeHandler handler = new AddressRangeHandler(xmlFile.getName(), localErrors);
            saxParser.parse(xmlFile, handler);
            
            return handler.isAllPassed();
            
        } catch (Exception e) {
            String errorMsg = "Address range validation failed: " + e.getMessage();
            localErrors.add(errorMsg);
            Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
            return false;
        }
    }
    
    /**
     * @brief
     * 功能: 在SAX解析过程中检查目标元素地址属性的取值范围。
     */
    private static class AddressRangeHandler extends DefaultHandler {
        private final String fileName;
        private final List<String> errorMessages;
        private boolean allPassed = true;
        private Locator locator;
        
        public AddressRangeHandler(String fileName, List<String> errorMessages) {
            this.fileName = fileName;
            this.errorMessages = errorMessages;
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
            /* 检查当前元素是否需要验证地址范围 */
            for (AddressRangeConfig config : ADDRESS_RANGE_CONFIGS) {
                if (qName.equals(config.elementName) || localName.equals(config.elementName)) {
                    String addressValue = attributes.getValue(config.attributeName);
                    
                    if (addressValue != null && !addressValue.isEmpty()) {
                        long address = parseAddress(addressValue);
                        
                        if (address != -1) { /* -1表示解析失败，已在parseAddress中处理 */
                            if (address < config.minAddress || address > config.maxAddress) {
                                int lineNumber = locator != null ? locator.getLineNumber() : -1;
                                
                                /* 格式化最小值和最大值为十六进制 */
                                String minHex = String.format("0x%X", config.minAddress);
                                String maxHex = String.format("0x%X", config.maxAddress);
                                
                                /* 格式: [File: {fileName}, Line: {lineNumber}] address {value} is out of range, the range is [{min}, {max}]. */
                                String errorMsg = String.format("[File: %s, Line: %d] address %s is out of range, the range is [%s, %s].",
                                    fileName,
                                    lineNumber,
                                    addressValue,
                                    minHex,
                                    maxHex);
                                
                                errorMessages.add(errorMsg);
                                Log.error(CodeConstant.ERROR_ADDRESS_OUT_OF_RANGE, errorMsg);
                                allPassed = false;
                            }
                        }
                    }
                }
            }
        }
        
        /**
         * parseAddress
         *
         * @brief
         * 功能: 解析地址字符串，支持十进制和0x/0X前缀的十六进制格式。
         *       空值、空白字符串或非法数值均返回-1。
         * 
         * @param[in]  addressValue: 待解析的地址值字符串。
         * @return
         * long: 解析后的地址值；解析失败返回-1。
         * @implements 
         * @scope internal
         * @errno:
         * NumberFormatException: 地址字符串格式非法时捕获并返回-1。
         */
        private long parseAddress(String addressValue) {
            if (addressValue == null || addressValue.trim().isEmpty()) {
                return -1;
            }
            
            try {
                addressValue = addressValue.trim();
                
                /* 解析地址值（支持十进制和十六进制） */
                if (addressValue.startsWith("0x") || addressValue.startsWith("0X")) {
                    /* 十六进制 */
                    return Long.parseLong(addressValue.substring(2), 16);
                } else {
                    /* 十进制 */
                    return Long.parseLong(addressValue);
                }
            } catch (NumberFormatException e) {
                /* 无法解析的值，返回-1 */
                return -1;
            }
        }
        /**
         * isAllPassed
         *
         * @brief
         * 功能: 获取地址范围校验是否全部通过的状态。
         * 
         * @return
         * boolean: 全部通过返回true，否则返回false。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public boolean isAllPassed() {
            return allPassed;
        }
    }
    
    /**
     * setCustomAddressRange
     *
     * @brief
     * 功能: 设置指定元素属性的自定义地址范围。
     *       处理规则如下：
     *       1. 已存在相同元素名和属性名配置时更新该配置；
     *       2. 不存在对应配置时新增地址范围配置。
     * 
     * @param[in]  elementName: 元素名。
     * @param[in]  attributeName: 属性名。
     * @param[in]  minAddress: 最小地址。
     * @param[in]  maxAddress: 最大地址。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void setCustomAddressRange(String elementName, String attributeName, 
                                             long minAddress, long maxAddress) {
        /* 查找是否已存在配置 */
        for (int i = 0; i < ADDRESS_RANGE_CONFIGS.size(); i++) {
            AddressRangeConfig config = ADDRESS_RANGE_CONFIGS.get(i);
            if (config.elementName.equals(elementName) && config.attributeName.equals(attributeName)) {
                /* 更新配置 */
                ADDRESS_RANGE_CONFIGS.set(i, new AddressRangeConfig(elementName, attributeName, minAddress, maxAddress));
                return;
            }
        }
        
        /* 如果不存在，添加新配置 */
        ADDRESS_RANGE_CONFIGS.add(new AddressRangeConfig(elementName, attributeName, minAddress, maxAddress));
    }
}
