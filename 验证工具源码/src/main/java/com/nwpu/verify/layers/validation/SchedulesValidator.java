/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SchedulesValidator.java
 * 
 * 描述    : 校验调度配置节点是否满足非空要求。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-26  开发人员  使调度校验触发的1018使用完整文件路径。
 *     2026-07-26  开发人员  统一调度正确性校验异常的1018输出格式。
 */

package com.nwpu.verify.layers.validation;

/* 头文件 */

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.List;

/**
 * @brief
 * 功能: 校验调度配置节点是否满足非空要求。
 */
public class SchedulesValidator {

    /**
     * validateSchedulesNotEmpty
     *
     * @brief
     * 功能: 校验 Schedules 元素至少包含一个 Schedule 或 BmpSchedule 子元素。
     * @param[in]  xmlFile           XML 配置文件。
     * @param[out] errorMessagesList 错误信息列表。
     * @return 校验通过返回 true，否则返回 false。
     * @scope external
     */
    public static boolean validateSchedulesNotEmpty(File xmlFile, List<String> errorMessagesList) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);

            /* 查找Schedules元素 */
            NodeList schedulesList = document.getElementsByTagNameNS("*", "Schedules");
            
            if (schedulesList.getLength() == 0) {
                /* 没有Schedules元素，跳过验证（这种情况应该由XSD捕获） */
                return true;
            }

            Element schedulesElement = (Element) schedulesList.item(0);
            
            /* 检查Schedules是否有子元素 */
            NodeList scheduleChildren = schedulesElement.getElementsByTagNameNS("*", "Schedule");
            NodeList bmpScheduleChildren = schedulesElement.getElementsByTagNameNS("*", "BmpSchedule");
            
            if (scheduleChildren.getLength() == 0 && bmpScheduleChildren.getLength() == 0) {
                /* Schedules为空，报告错误 */
                int lineNumber = getLineNumber(xmlFile, "Schedules");
                /* TR_PR_005: "element Schedule occurs 0 time less than minimum 1 time." */
                String errorMsg = String.format(
                    "[File: %s, Line: %d] element Schedule occurs 0 time less than minimum 1 time.",
                    xmlFile.getName(),
                    lineNumber
                );
                
                errorMessagesList.add(errorMsg);
                Log.error(CodeConstant.ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET, errorMsg);
                return false;
            }

            return true;

        } catch (Exception e) {
            String errorMsg = "Correctness verification for XML file Failed, File Path: [" + xmlFile.getAbsolutePath() + "]。";
            errorMessagesList.add(errorMsg);
            Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED, errorMsg);
            return false;
        }
    }

    /**
     * getLineNumber
     *
     * @brief
     * 功能: 使用 SAX 解析器获取指定元素在文件中的行号。
     * @param[in] xmlFile     XML 配置文件。
     * @param[in] elementName 元素名称。
     * @return 元素行号；获取失败时返回 1。
     * @scope internal
     */
    private static int getLineNumber(File xmlFile, String elementName) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            
            final int[] foundLine = {-1};
            
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
                    /* 查找第一个匹配的元素 */
                    if (elementName.equals(localName) && foundLine[0] == -1) {
                        if (locator != null) {
                            foundLine[0] = locator.getLineNumber();
                        }
                    }
                }
            };
            
            saxParser.parse(xmlFile, handler);
            return foundLine[0] > 0 ? foundLine[0] : 1;
            
        } catch (Exception e) {
            /* 如果获取行号失败，返回默认值 */
            return 1;
        }
    }
}
