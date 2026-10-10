/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CRCAttributeValidator.java
 * 
 * 描述    : 校验XML配置文件中CRC属性的存在性和格式。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-30  开发人员  按B-04删除Deleted的4007 CRC属性缺失逻辑，保留4005格式告警。
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
import java.util.ArrayList;
import java.util.List;

/**
 * @brief
 * 功能: 校验XML配置文件中CRC属性的存在性和格式。
 */
public class CRCAttributeValidator {

    /**
     * validateCRCAttribute
     *
     * @brief
     * 功能: 验证XML文件根元素的CRC32属性是否存在且格式正确。
     *       处理流程如下：
     *       1. XML文件为空或不存在时跳过验证；
     *       2. 使用SAX解析器检查根元素CRC32属性；
     *       3. CRC32缺失、为空或格式非法时记录4005告警。
     * 
     * @param[in]  xmlFile: 待验证的XML文件。
     * @param[out] errorMessages: 错误或告警信息列表。
     * @return
     * boolean: CRC32属性存在且格式正确返回true，否则返回false。
     * @implements 
     * @scope external
     * @errno:
     * WARNING_XML_CRC_ATTRIBUTE_MISSING: CRC32属性缺失或类型错误。
     */
    public static boolean validateCRCAttribute(File xmlFile, List<String> errorMessages) {
        if (xmlFile == null || !xmlFile.exists()) {
            return true; /* 文件不存在，跳过验证 */
        }

        List<String> localErrors = (errorMessages != null) ? errorMessages : new ArrayList<>();

        try {
            /* 使用SAX解析器检查根元素的CRC属性 */
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();

            CRCAttributeChecker handler = new CRCAttributeChecker(xmlFile.getName());
            try {
                parser.parse(xmlFile, handler);
            } catch (SAXException e) {
                /* 忽略我们主动抛出的"Stop parsing"异常 */
                if (!"Stop parsing".equals(e.getMessage())) {
                    throw e;
                }
            }

            /* 检查CRC属性状态 */
            if (handler.hasCRCIssue()) {
                String warningMsg = String.format(
                        "XML file %s CRC attribute missing or type error.",
                        xmlFile.getName());

                localErrors.add(warningMsg);
                Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING, warningMsg);
                return false;
            }

            return true;

        } catch (Exception e) {
            String errorMsg = "CRC attribute validation failed: " + e.getMessage();
            localErrors.add(errorMsg);
            Log.error(null, errorMsg);
            return false;
        }
    }

    /**
     * @brief
     * 功能: 在SAX解析过程中检查根元素CRC32属性状态。
     */
    private static class CRCAttributeChecker extends DefaultHandler {
        private final String fileName;
        private boolean crcIssue = false;
        private boolean rootElementProcessed = false;

        public CRCAttributeChecker(String fileName) {
            this.fileName = fileName;
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
            /* 只检查第一个元素（根元素） */
            if (!rootElementProcessed) {
                rootElementProcessed = true;

                String elementName = localName.isEmpty() ? qName : localName;

                /* 检查是否为Module或CoreOS等根元素 */
                if (elementName.equals("Module") ||
                        elementName.equals("CoreOS") ||
                        elementName.equals("ApplicationDescription") ||
                        elementName.equals("PartitionDescription")) {

                    /* 检查CRC32属性 */
                    String crc32Value = attributes.getValue("CRC32");

                    if (crc32Value == null) {
                        /* CRC属性缺失 */
                        crcIssue = true;
                    } else if (crc32Value.trim().isEmpty()) {
                        /* CRC属性为空 */
                        crcIssue = true;
                    } else if (!isValidCRCFormat(crc32Value)) {
                        /* CRC属性格式错误 */
                        crcIssue = true;
                    }
                }

                /* 停止解析，我们只需要检查根元素 */
                throw new SAXException("Stop parsing");
            }
        }

        /**
         * isValidCRCFormat
         *
         * @brief
         * 功能: 验证CRC32属性值格式是否与文件系统工具(fat-gen-verify XmlCrcValidator)对齐：
         *       必须为小写"0x"开头的8位十六进制数字，即 ^0x[0-9A-Fa-f]{8}$。
         *
         * @param[in]  crcValue: 待验证的CRC32属性值。
         * @return
         * boolean: 格式正确返回true，否则返回false。
         * @implements
         * @scope internal
         * @errno:
         * N/A
         */
        private boolean isValidCRCFormat(String crcValue) {
            if (crcValue == null || crcValue.trim().isEmpty()) {
                return false;
            }

            /* TR_PR_017: 与文件系统工具对齐，0X开头或位数不为8位均视为格式错误 */
            return crcValue.trim().matches("^0x[0-9A-Fa-f]{8}$");
        }
        /**
         * hasCRCIssue
         *
         * @brief
         * 功能: 获取CRC32属性是否存在缺失、为空或格式错误的问题。
         * 
         * @return
         * boolean: 存在CRC32属性问题返回true，否则返回false。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public boolean hasCRCIssue() {
            return crcIssue;
        }

        /**
         * endDocument
         *
         * @brief
         * 功能: 执行 endDocument 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        @Override
        public void endDocument() throws SAXException {
            /* 正常结束，不需要特殊处理 */
        }

        /**
         * fatalError
         *
         * @brief
         * 功能: 执行 fatalError 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        @Override
        public void fatalError(org.xml.sax.SAXParseException e) throws SAXException {
            /* 忽略我们主动抛出的"Stop parsing"异常 */
            if (!"Stop parsing".equals(e.getMessage())) {
                throw e;
            }
        }
    }
}
