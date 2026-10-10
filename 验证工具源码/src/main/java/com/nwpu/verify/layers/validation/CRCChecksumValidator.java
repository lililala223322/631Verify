/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CRCChecksumValidator.java
 * 
 * 描述    : 校验XML配置文件的CRC校验和计算结果。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-30  开发人员  按B-04统一XML CRC失败输出为3006并保留分类成功提示。
 */

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * @brief
 * 功能: 校验XML配置文件的CRC校验和计算结果。
 */
public class CRCChecksumValidator {

    /**
     * getCleanPath
     *
     * @brief
     * 功能: 获取文件的规范路径，去除路径中的冗余表示。
     * 
     * @param[in]  file: 待处理的文件对象。
     * @return
     * String: 文件规范路径；获取失败时返回绝对路径。
     * @implements 
     * @scope internal
     * @errno:
     * IOException: 获取规范路径失败时捕获并返回绝对路径。
     */
    private static String getCleanPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    /* CRC32算法参数 */
    private static final long POLY = 0x04C11DB7L;
    private static final long INIT = 0xFFFFFFFFL;
    private static final long XOROUT = 0xFFFFFFFFL;

    /**
     * validateCRCChecksum
     *
     * @brief
     * 功能: 验证XML文件CRC32属性值与文件内容计算结果是否一致。
     *       处理流程如下：
     *       1. XML文件为空或不存在时跳过验证；
     *       2. 未启用CRC校验时记录告警并返回通过；
     *       3. 读取根元素CRC32属性并移除其值后计算CRC；
     *       4. 计算CRC与期望CRC不一致时记录3006错误。
     * 
     * @param[in]  xmlFile: 待验证的XML文件。
     * @param[out] errorMessages: 错误信息列表。
     * @param[in]  enableCrcCheck: 是否启用CRC校验。
     * @return
     * boolean: CRC校验通过、无需校验或CRC属性缺失时返回true；校验失败返回false。
     * @implements 
     * @scope external
     * @errno:
     * WARNING_CRC_VALIDATION_DISABLED: CRC校验被禁用；
     * WARNING_XML_CRC_ATTRIBUTE_MISSING: CRC32属性缺失或类型错误；
     * ERROR_XML_CRC_VALIDATION_FAILED: XML文件CRC校验失败。
     */
    public static boolean validateCRCChecksum(File xmlFile, List<String> errorMessages, boolean enableCrcCheck) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true; /* 文件不存在，跳过验证 */
        }

        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();

        try {
            /* 获取文件绝对路径和文件名 */
            String filePath = getCleanPath(xmlFile);
            /* PR_09: 获取文件名用于显示 */
            String fileName = xmlFile.getName();

            /* 如果禁用了CRC校验（-nc参数），输出警告并跳过 */
            if (!enableCrcCheck) {
                String warningMsg = String.format(
                        "CRC validation has been disabled for the input XML files. Proceeding without integrity verification. File Path: %s",
                        filePath);
                Log.warning(CodeConstant.WARNING_CRC_VALIDATION_DISABLED, warningMsg);
                return true;
            }

            /* 1. 检查根元素是否有CRC32属性，并获取根元素名称 */
            CRCAttributeReader reader = new CRCAttributeReader();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();

            try {
                parser.parse(xmlFile, reader);
            } catch (SAXException e) {
                /* 忽略"Stop parsing"异常 */
                if (!"Stop parsing".equals(e.getMessage())) {
                    throw e;
                }
            }

            /* 如果没有CRC32属性或为空，输出警告并跳过校验 */
            /* PR_12: 修改CRC attribute missing警告信息 */
            /* PR_21: 将4007归类到4005 */
            if (reader.getCrcValue() == null || reader.getCrcValue().trim().isEmpty()) {
                /* 输出警告：CRC属性缺失 */
                String warningMsg = String.format("XML file %s CRC attribute missing or type error.",
                        xmlFile.getName());
                Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING, warningMsg);
                return true; /* 警告不影响验证结果，返回true继续处理 */
            }

            /*
             * 004930：CRC属性格式错误统一作为4005告警处理。
             * 不能把非法格式当作普通CRC不匹配，否则会错误输出3006。
             */
            if (!reader.getCrcValue().trim().matches("^0x[0-9A-Fa-f]{8}$")) {
                String warningMsg = String.format("XML file %s CRC attribute missing or type error.",
                        xmlFile.getName());
                Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING, warningMsg);
                return true;
            }

            /* 2. 读取文件内容并移除CRC32属性值 */
            String fileContent = new String(Files.readAllBytes(xmlFile.toPath()), "UTF-8");
            String contentForCrc = removeCrcValue(fileContent, reader.getCrcValue());

            /* 3. 计算CRC32 */
            long calculatedCrc = calculateCRC32(contentForCrc.getBytes("UTF-8"));
            String calculatedCrcHex = String.format("%08X", calculatedCrc);

            /* 4. 对比CRC32值 */
            String expectedCrc = reader.getCrcValue().trim().toUpperCase();
            if (expectedCrc.startsWith("0X")) {
                expectedCrc = expectedCrc.substring(2); /* 移除0x前缀 */
            }

            /* 获取文件类型描述 */
            String fileTypeDesc = getFileTypeDescription(reader.getRootElementName());

            if (!calculatedCrcHex.equalsIgnoreCase(expectedCrc)) {
                String errorMsg = String.format(
                        "XML file %s failed CRC validation. CRC32: 0x[%s]; Computed CRC32: 0x[%s]. File Path: %s",
                        fileName,
                        expectedCrc,
                        calculatedCrcHex,
                        filePath);
                localErrors.add(errorMsg);
                Log.error(CodeConstant.ERROR_XML_CRC_VALIDATION_FAILED, errorMsg);
                /* 根据测试规则，3006错误必须立即终止且不输出log/report */
                com.nwpu.verify.Application.exitWithErrorCode(CodeConstant.ERROR_XML_CRC_VALIDATION_FAILED);
                return false; /* 永远不会执行到这里，但保留以满足语法 */
            } else {
                /* CRC校验通过，根据文件类型输出分类信息 */
                String successMsg = String.format("%s passed CRC validation. File Path: %s", fileTypeDesc, filePath);
                Log.info(successMsg);
                return true;
            }

        } catch (Exception e) {
            /* PR_18: 验证CRC校验失败归类到3006 */
            String errorMsg = String.format(
                    "XML file %s failed CRC validation. CRC32: 0x[%s]; Computed CRC32: 0x[%s]. File Path: %s。",
                    xmlFile.getName(),
                    "N/A",
                    "N/A",
                    getCleanPath(xmlFile));
            localErrors.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_CRC_VALIDATION_FAILED, errorMsg);
            /* 根据测试规则，3006错误必须立即终止且不输出log/report */
            com.nwpu.verify.Application.exitWithErrorCode(CodeConstant.ERROR_XML_CRC_VALIDATION_FAILED);
            return false; /* 永远不会执行到这里，但保留以满足语法 */
        }
    }

    /**
     * getFileTypeDescription
     *
     * @brief
     * 功能: 根据XML根元素名称获取文件类型描述。
     *       映射关系如下：
     *       Module、ApplicationDescription、PartitionDescription -> CPM cfg XML file；
     *       CoreOSDescription -> os cfg XML file；
     *       SharedLibraryDescription、SharedDataDescription -> shared lib cfg XML file；
     *       其他或空根元素 -> XML file。
     * 
     * @param[in]  rootElementName: XML根元素名称。
     * @return
     * String: 文件类型描述。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static String getFileTypeDescription(String rootElementName) {
        if (rootElementName == null) {
            return "XML file";
        }

        switch (rootElementName) {
            /* CPM配置文件：Module主文件、应用配置、分区配置 */
            case "Module":
            case "ApplicationDescription":
            case "PartitionDescription":
                return "CPM cfg XML file";

            /* 核心操作系统配置文件 */
            case "CoreOSDescription":
                return "os cfg XML file";

            /* 共享库配置文件：共享库(pos.xml)、共享数据(sdr1.xml等) */
            case "SharedLibraryDescription":
            case "SharedDataDescription":
                return "shared lib cfg XML file";

            default:
                /* 未知类型，使用通用描述 */
                return "XML file";
        }
    }

    /**
     * validateCRCChecksum
     *
     * @brief
     * 功能: 使用默认启用CRC校验的方式验证XML文件CRC32校验码。
     * 
     * @param[in]  xmlFile: 待验证的XML文件。
     * @param[out] errorMessages: 错误信息列表。
     * @return
     * boolean: CRC校验通过或无需校验返回true，否则返回false。
     * @implements 
     * @scope external
     * @errno:
     * ERROR_XML_CRC_VALIDATION_FAILED: XML文件CRC校验失败。
     */
    public static boolean validateCRCChecksum(File xmlFile, List<String> errorMessages) {
        return validateCRCChecksum(xmlFile, errorMessages, true);
    }

    /**
     * removeCrcValue
     *
     * @brief
     * 功能: 移除XML文本中CRC32属性的取值，保留CRC32属性本身用于计算校验和。
     *       示例映射如下：
     *       CRC32="0x12345678" -> CRC32=""；
     *       CRC32='0x12345678' -> CRC32=''
     * 
     * @param[in]  content: XML文件文本内容。
     * @param[in]  crcValue: 原CRC32属性值。
     * @return
     * String: 移除CRC32属性值后的XML文本内容。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static String removeCrcValue(String content, String crcValue) {
        /* 匹配 CRC32="任意值" 或 CRC32='任意值' */
        String pattern = "(CRC32\\s*=\\s*[\"'])[^\"']*([\"'])";
        String replacement = "$1$2";
        return content.replaceAll(pattern, replacement);
    }

    /**
     * calculateCRC32
     *
     * @brief
     * 功能: 使用CRC-32/MPEG-2算法变体计算输入数据CRC32值。
     *       算法流程如下：
     *       1. 初始值使用0xFFFFFFFF；
     *       2. 输入字节按位反转后参与CRC计算；
     *       3. 使用多项式0x04C11DB7；
     *       4. 输出结果按位反转后再与0xFFFFFFFF异或。
     * 
     * @param[in]  data: 待计算CRC的数据字节数组。
     * @return
     * long: CRC32无符号整数值。
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static long calculateCRC32(byte[] data) {
        long crc = INIT;

        for (byte b : data) {
            /* 输入数据反转（RefIn = TRUE） */
            int reversedByte = reverseBits(b & 0xFF);

            /* CRC计算 */
            crc ^= ((long) reversedByte << 24);

            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80000000L) != 0) {
                    crc = (crc << 1) ^ POLY;
                } else {
                    crc = crc << 1;
                }
            }

            crc &= 0xFFFFFFFFL; /* 保持32位 */
        }

        /* 输出数据反转（RefOut = TRUE） */
        crc = reverseBits32(crc);

        /* 结果异或（XorOut） */
        crc ^= XOROUT;

        return crc & 0xFFFFFFFFL;
    }

    /**
     * reverseBits
     *
     * @brief
     * 功能: 反转8位字节的位序。
     * 
     * @param[in]  value: 待反转的8位数值。
     * @return
     * int: 位序反转后的8位数值。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static int reverseBits(int value) {
        int result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 1;
            result |= (value & 1);
            value >>= 1;
        }
        return result;
    }

    /**
     * reverseBits32
     *
     * @brief
     * 功能: 反转32位整数的位序。
     * 
     * @param[in]  value: 待反转的32位数值。
     * @return
     * long: 位序反转后的32位数值。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static long reverseBits32(long value) {
        long result = 0;
        for (int i = 0; i < 32; i++) {
            result <<= 1;
            result |= (value & 1);
            value >>= 1;
        }
        return result;
    }

    /**
     * @brief
     * 功能: 在SAX解析过程中读取根元素名称和CRC32属性值。
     */
    private static class CRCAttributeReader extends DefaultHandler {
        private String crcValue = null;
        private String rootElementName = null;
        private boolean rootProcessed = false;

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
            if (!rootProcessed) {
                rootProcessed = true;

                String elementName = localName.isEmpty() ? qName : localName;
                rootElementName = elementName; /* 保存根元素名称 */

                /* PR_20: 检查是否为配置文件根元素，添加所有可能的根元素类型 */
                if (elementName.equals("CoreOS") ||
                        elementName.equals("CoreOSDescription") ||
                        elementName.equals("FileSystem") ||
                        elementName.equals("Module") ||
                        elementName.equals("ModuleDescription") ||
                        elementName.equals("ApplicationDescription") ||
                        elementName.equals("Application") ||
                        elementName.equals("PartitionDescription") ||
                        elementName.equals("Partition") ||
                        elementName.equals("PseudoPartitionDescription") ||
                        elementName.equals("PseudoPartition") ||
                        elementName.equals("SharedLibraryDescription") ||
                        elementName.equals("SharedLibrary") ||
                        elementName.equals("SharedDataDescription") ||
                        elementName.equals("SharedData") ||
                        elementName.equals("SharedIODescription") ||
                        elementName.equals("SharedIO")) {

                    crcValue = attributes.getValue("CRC32");
                }

                /* 停止解析 */
                throw new SAXException("Stop parsing");
            }
        }
        /**
         * getCrcValue
         *
         * @brief
         * 功能: 获取根元素CRC32属性值。
         * 
         * @return
         * String: CRC32属性值；未读取到时返回null。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getCrcValue() {
            return crcValue;
        }
        /**
         * getRootElementName
         *
         * @brief
         * 功能: 获取XML根元素名称。
         * 
         * @return
         * String: XML根元素名称；未读取到时返回null。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getRootElementName() {
            return rootElementName;
        }
    }
}
