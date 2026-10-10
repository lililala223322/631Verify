/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: XmlParser.java
 * 
 * 描述    : 解析ARINC653相关XML配置文件，并构建验证工具使用的内部配置对象。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-23  开发人员  按XML解析需求细化编码、起始标签、表头、注释、结束标签、根元素和属性定义错误原因。
 */

package com.nwpu.verify.layers.parsing;

/* 头文件 */
/* 引入了HealthMonitor 相关类 */
/* 引入了PartitionHMTask 相关类 */
/* 引入了CoreHMTask 相关类 */
/* 引入了CoreHMTable 相关类 */
/* 引入了MultiPartitionHMTable 相关类 */
/* 引入了PartitionHMTable 相关类 */
import com.nwpu.verify.entity.*;
import com.nwpu.verify.entity.Module;
import com.nwpu.verify.layers.output.Log;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList; /* 用于创建动态 List */
import java.util.List; /* 列表接口 (集合框架) */

/**
 * @brief
 * 功能: 提供XML配置文件预处理、解析和实体对象构建能力。
 */
public class XmlParser {
    /* 工具方法 */

    /** 解析阶段使用的编码格式错误，不应被误报为文件读取失败。 */
    private static final class XmlEncodingFormatException extends RuntimeException {
        XmlEncodingFormatException(String message) {
            super(message);
        }

        XmlEncodingFormatException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /* 2026-08-26 TR_PR_015会议决议: 1017消息带文件名。顶层解析方法设置当前文件名，
     * 供Element级解析方法（无filePath参数）构造1017消息使用。 */
    private static String currentParsingFileName = null;

    /**
     * setCurrentParsingFile
     *
     * @brief
     * 功能: 执行 setCurrentParsingFile 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void setCurrentParsingFile(String filePath) {
        currentParsingFileName = (filePath != null) ? new File(filePath).getName() : null;
    }

    /**
     * getCurrentParsingFileName
     *
     * @brief
     * 功能: 执行 getCurrentParsingFileName 相关处理。
     *
     * @scope external
     * @errno:
     * N/A
     */
    public static String getCurrentParsingFileName() {
        return currentParsingFileName;
    }

    /**
     * 构造1017错误消息（004901/会议修订模板）:
     * XML file {fileName} parsing failed. the reason : {reason}.
     */
    public static String buildParseFailedMessage(String reason) {
        String fileName = (currentParsingFileName != null) ? currentParsingFileName : "unknown";
        return "XML file " + fileName + " parsing failed. the reason : " + reason + ".";
    }

    /**
     * isDuplicateAttributeError
     *
     * @brief
     * 功能: 执行 isDuplicateAttributeError 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean isDuplicateAttributeError(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null) {
                if (msg.contains("already specified") || msg.contains("was already specified")
                        || msg.contains("Duplicate attribute")
                        || (msg.contains("Attribute") && msg.contains("duplicate"))
                        || msg.contains("重复")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * extractElementAndAttributeFromDuplicateMsg
     *
     * @brief
     * 功能: 执行 extractElementAndAttributeFromDuplicateMsg 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String[] extractElementAndAttributeFromDuplicateMsg(String msg) {
        if (msg == null) {
            return new String[] { "unknown", "unknown" };
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Attribute \\\"([^\\\"]+)\\\".*element \\\"([^\\\"]+)\\\"",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(msg);
        if (m.find()) {
            return new String[] { m.group(2), m.group(1) };
        }
        java.util.regex.Matcher m2 = java.util.regex.Pattern
                .compile("Attribute \\\"([^\\\"]+)\\\"", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(msg);
        if (m2.find()) {
            return new String[] { "unknown", m2.group(1) };
        }
        return new String[] { "unknown", "unknown" };
    }

    /**
     * 获取XML文件中指定元素的行号
     * 
     * @param file        XML文件
     * @param elementName 要查找的元素名称
     * @return 元素所在行号，如果找不到则返回-1
     */
    private static int getElementLineNumber(File file, String elementName) {
        try {
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            saxFactory.setNamespaceAware(true);
            SAXParser saxParser = saxFactory.newSAXParser();

            final int[] foundLine = { -1 };

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
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if (foundLine[0] > 0)
                        return; /* 已找到 */

                    if (localName.equals(elementName) || qName.endsWith(":" + elementName)
                            || qName.equals(elementName)) {
                        if (locator != null) {
                            foundLine[0] = locator.getLineNumber();
                        }
                    }
                }
            };

            saxParser.parse(file, handler);
            return foundLine[0];
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 获取文件的规范路径（去除冗余的.等）
     */
    private static String getCleanPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * parseInt
     *
     * @brief
     * 功能: 执行 parseInt 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static int parseInt(String s) {
        if (s == null || s.isEmpty())
            return 0;
        /* 特殊字符串处理开始 */
        if ("LOCAL_PORT".equalsIgnoreCase(s)) { /* 忽略字符串 "LOCAL_PORT" (特殊处理) */
            return 0; /* 对于特殊字符串返回默认值 */
        }
        /* 特殊字符串处理结束 */
        try {
            if (s.startsWith("0x") || s.startsWith("0X"))
                return Integer.parseInt(s.substring(2), 16);
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * parseLong
     *
     * @brief
     * 功能: 执行 parseLong 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static long parseLong(String s) {
        if (s == null || s.isEmpty())
            return 0L;
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                /* PR_06: 处理十六进制字符串解析 */
                String hexStr = s.substring(2);
                /* 如果超过Long.MAX_VALUE则使用BigInteger处理 */
                if (hexStr.length() > 16 || (hexStr.length() == 16 && hexStr.charAt(0) > '7')) {
                    java.math.BigInteger bigInt = new java.math.BigInteger(hexStr, 16);
                    return bigInt.longValue(); /* 截取低64位 */
                }
                return Long.parseLong(hexStr, 16);
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            /* PR_06: 解析失败时返回0作为默认值 */
            return 0L;
        }
    }

    /**
     * parseBool
     *
     * @brief
     * 功能: 执行 parseBool 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean parseBool(String s) {
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    /**
     * 解析路径中的环境变量，将 $(VAR_NAME) 或 ${VAR_NAME} 格式的变量替换为实际值
     * 例如: $(XML_BASE)/coreos.xml -> /actual/path/coreos.xml
     * ${XML_BASE}/coreos.xml -> /actual/path/coreos.xml
     * 
     * @param path 包含环境变量的路径字符串
     * @return 替换环境变量后的路径字符串，如果环境变量不存在则保留原样
     */
    public static String resolveEnvVariables(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        /* 使用正则表达式匹配 $(VAR_NAME) 或 ${VAR_NAME} 格式 */
        /* \\$\\(([^)]+)\\) 匹配 $(VAR_NAME) */
        /* \\$\\{([^}]+)\\} 匹配 ${VAR_NAME} */
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$[({]([^)}]+)[)}]");
        java.util.regex.Matcher matcher = pattern.matcher(path);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1); /* 获取括号中的变量名 */
            String envValue = System.getenv(varName); /* 获取环境变量值 */

            if (envValue != null) {
                /* 替换为环境变量的值，注意需要转义特殊字符 */
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(envValue));
            } else {
                /* 环境变量不存在，保留原样（不再记录4005警告日志） */
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 预处理XML文件，替换其中所有href属性中的环境变量
     * 返回一个可以被DocumentBuilder解析的InputSource
     * 
     * @param xmlFile XML文件
     * @return 预处理后的InputSource
     * @throws IOException 如果读取文件失败
     */
    public static org.xml.sax.InputSource preprocessXmlFile(File xmlFile) throws IOException {
        return preprocessXmlFile(xmlFile, null);
    }

    /**
     * 预处理XML文件：
     * 1) 替换href属性中的环境变量
     * 2) 修复同一元素内重复属性（会记录2012错误，但不再导致致命解析失败）
     *
     * @param xmlFile             XML文件
     * @param structuralErrorsOut 若不为null，收集结构性错误消息（如2012）
     */
    public static org.xml.sax.InputSource preprocessXmlFile(File xmlFile, List<String> structuralErrorsOut)
            throws IOException {
        /* 读取XML文件内容 */
        String content = readXmlContent(xmlFile);

        /* 替换所有href属性中的环境变量 */
        /* 匹配 href="...${}..." 或 href='...${}...' 模式 */
        java.util.regex.Pattern hrefPattern = java.util.regex.Pattern.compile(
                "(href\\s*=\\s*[\"'])([^\"']*)(\\$[({][^)}]+[)}][^\"']*)([\"'])");
        java.util.regex.Matcher hrefMatcher = hrefPattern.matcher(content);

        StringBuffer sb = new StringBuffer();
        while (hrefMatcher.find()) {
            String prefix = hrefMatcher.group(1); /* href=" */
            String beforeEnv = hrefMatcher.group(2); /* 环境变量之前的部分 */
            String envPart = hrefMatcher.group(3); /* 包含环境变量的部分 */
            String suffix = hrefMatcher.group(4); /* 结束引号 */

            /* 解析环境变量 */
            String resolvedEnvPart = resolveEnvVariables(envPart);
            String replacement = prefix + beforeEnv + resolvedEnvPart + suffix;
            hrefMatcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        hrefMatcher.appendTail(sb);
        content = sb.toString();

        /* 修复重复属性（2012），避免XML解析器直接fatal退出 */
        SanitizationResult sanitizeResult = sanitizeDuplicateAttributes(content, xmlFile.getName());
        content = sanitizeResult.content;
        if (sanitizeResult.errors != null && !sanitizeResult.errors.isEmpty()) {
            for (String msg : sanitizeResult.errors) {
                if (structuralErrorsOut != null && !structuralErrorsOut.contains(msg)) {
                    structuralErrorsOut.add(msg);
                }
                /* 输出为错误码2012，但不中断流程 */
                Log.error(CodeConstant.ERROR_XML_REDUNDANCY_ATTRIBUTES, msg);
            }
        }

        /* 空命名空间视为默认命名空间 */
        String defaultNamespace = extractRootDefaultNamespace(content);
        if (defaultNamespace != null && !defaultNamespace.isEmpty()) {
            content = replaceEmptyDefaultNamespace(content, defaultNamespace);
        }

        /* 创建InputSource */
        org.xml.sax.InputSource inputSource = new org.xml.sax.InputSource(
                new java.io.StringReader(content));
        /* XInclude 需要可解析的 ASCII URI；中文目录必须先百分号转义。 */
        inputSource.setSystemId(xmlFile.toURI().toASCIIString());

        return inputSource;
    }

    /**
     * 按 XML BOM 或声明识别字符集，并以 REPORT 模式严格解码。
     * 预处理后的 InputSource 使用 StringReader，因此不能再让解析器根据
     * 原始字节流自行判断编码；这里必须先正确解码，避免非 UTF-8 文件被
     * 固定按 UTF-8 读取后产生替换字符。
     */
    private static String readXmlContent(File xmlFile) throws IOException {
        byte[] bytes = java.nio.file.Files.readAllBytes(xmlFile.toPath());
        Charset charset = detectXmlCharset(bytes);

        int offset = hasUtf8Bom(bytes) ? 3 : (hasUtf16Bom(bytes) ? 2 : 0);
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            String content = decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset)).toString();
            /* UTF-16 无 BOM但按声明解码时，仍防止 BOM 字符进入 XML 文本。 */
            return content.startsWith("\uFEFF") ? content.substring(1) : content;
        } catch (CharacterCodingException e) {
            throw new XmlEncodingFormatException("XML File Encoding Format Error", e);
        }
    }

    /**
     * 检测 XML 编码。没有 BOM 且没有 encoding 声明时，XML 默认按 UTF-8 处理。
     */
    private static Charset detectXmlCharset(byte[] bytes) {
        if (hasUtf8Bom(bytes)) {
            return StandardCharsets.UTF_8;
        }
        if (hasUtf16Bom(bytes)) {
            return bytes[0] == (byte) 0xFF ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_16BE;
        }

        /* 无 BOM 的 UTF-16 文件仍可由 XML 的固定字节序列识别。 */
        if (bytes.length >= 4) {
            if (bytes[0] == 0x00 && bytes[1] == 0x3C && bytes[2] == 0x00 && bytes[3] == 0x3F) {
                return StandardCharsets.UTF_16BE;
            }
            if (bytes[0] == 0x3C && bytes[1] == 0x00 && bytes[2] == 0x3F && bytes[3] == 0x00) {
                return StandardCharsets.UTF_16LE;
            }
        }

        String prefix = new String(bytes, 0, Math.min(bytes.length, 1024), StandardCharsets.US_ASCII);
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("encoding\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(prefix);
        if (matcher.find()) {
            String declaredEncoding = matcher.group(1).trim();
            try {
                return Charset.forName(declaredEncoding);
            } catch (Exception e) {
                throw new XmlEncodingFormatException("XML File Encoding Format Error", e);
            }
        }
        return StandardCharsets.UTF_8;
    }

    /**
     * hasUtf8Bom
     *
     * @brief
     * 功能: 执行 hasUtf8Bom 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF;
    }

    /**
     * hasUtf16Bom
     *
     * @brief
     * 功能: 执行 hasUtf16Bom 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean hasUtf16Bom(byte[] bytes) {
        return bytes.length >= 2
                && ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE
                        || (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF);
    }

    /** 需求规定配置 XML 必须使用 UTF-8；非 UTF-8 统一进入 1017 的编码原因。 */
    private static void ensureUtf8XmlEncoding(File xmlFile) {
        byte[] bytes;
        try {
            bytes = java.nio.file.Files.readAllBytes(xmlFile.toPath());
        } catch (IOException e) {
            /* 保留原有文件读取错误处理路径。 */
            return;
        }

        Charset charset = detectXmlCharset(bytes);
        if (!StandardCharsets.UTF_8.equals(charset)) {
            throw new XmlEncodingFormatException("XML File Encoding Format Error");
        }
    }

    private static final class SanitizationResult {
        final String content;
        final List<String> errors;

        SanitizationResult(String content, List<String> errors) {
            this.content = content;
            this.errors = errors;
        }
    }

    /**
     * 仅针对“同一元素内重复属性”的常见场景做轻量修复：保留第一次出现的属性，移除后续重复。
     * 注意：这是容错逻辑，目的是让后续校验尽可能跑完。
     */
    private static SanitizationResult sanitizeDuplicateAttributes(String content, String fileName) {
        if (content == null || content.isEmpty()) {
            return new SanitizationResult(content, java.util.Collections.emptyList());
        }

        StringBuilder out = new StringBuilder(content.length());
        List<String> errors = new java.util.ArrayList<>();

        int i = 0;
        int line = 1;
        final int n = content.length();

        while (i < n) {
            char ch = content.charAt(i);
            if (ch != '<') {
                out.append(ch);
                if (ch == '\n') {
                    line++;
                }
                i++;
                continue;
            }

            /* 处理注释/PI/CDATA/结束标签/doctype：原样复制 */
            if (content.startsWith("<!--", i)) {
                int end = content.indexOf("-->", i + 4);
                if (end < 0) {
                    out.append(content.substring(i));
                    break;
                }
                String seg = content.substring(i, end + 3);
                out.append(seg);
                line += countNewlines(seg);
                i = end + 3;
                continue;
            }
            if (content.startsWith("<?", i)) {
                int end = content.indexOf("?>", i + 2);
                if (end < 0) {
                    out.append(content.substring(i));
                    break;
                }
                String seg = content.substring(i, end + 2);
                out.append(seg);
                line += countNewlines(seg);
                i = end + 2;
                continue;
            }
            if (content.startsWith("<![CDATA[", i)) {
                int end = content.indexOf("]]>", i + 9);
                if (end < 0) {
                    out.append(content.substring(i));
                    break;
                }
                String seg = content.substring(i, end + 3);
                out.append(seg);
                line += countNewlines(seg);
                i = end + 3;
                continue;
            }
            if (content.startsWith("</", i) || content.startsWith("<!DOCTYPE", i)
                    || content.startsWith("<!ENTITY", i)) {
                int end = content.indexOf('>', i + 1);
                if (end < 0) {
                    out.append(content.substring(i));
                    break;
                }
                String seg = content.substring(i, end + 1);
                out.append(seg);
                line += countNewlines(seg);
                i = end + 1;
                continue;
            }

            /* 开始标签：尝试解析属性并去重 */
            int tagStartLine = line;
            int tagStart = i;
            int j = i + 1;

            /* 读取标签名 */
            while (j < n && isNameChar(content.charAt(j))) {
                j++;
            }
            if (j <= i + 1) {
                /* 非法，原样复制一个字符 */
                out.append('<');
                i++;
                continue;
            }

            String rawTagName = content.substring(i + 1, j);
            String elementName = rawTagName;
            int colon = elementName.indexOf(':');
            if (colon >= 0 && colon + 1 < elementName.length()) {
                elementName = elementName.substring(colon + 1);
            }

            out.append('<').append(rawTagName);
            i = j;

            java.util.HashSet<String> seenAttr = new java.util.HashSet<>();

            /* 属性解析循环 */
            while (i < n) {
                char c = content.charAt(i);

                /* 标签结束 */
                if (c == '>') {
                    out.append('>');
                    i++;
                    break;
                }
                if (c == '/' && i + 1 < n && content.charAt(i + 1) == '>') {
                    out.append("/>");
                    i += 2;
                    break;
                }

                /* 空白 */
                if (Character.isWhitespace(c)) {
                    out.append(c);
                    if (c == '\n') {
                        line++;
                    }
                    i++;
                    continue;
                }

                /* 读取属性名 */
                int nameStart = i;
                while (i < n && isNameChar(content.charAt(i))) {
                    i++;
                }
                if (i == nameStart) {
                    /* 异常字符，原样复制 */
                    out.append(content.charAt(i));
                    if (content.charAt(i) == '\n') {
                        line++;
                    }
                    i++;
                    continue;
                }

                String attrName = content.substring(nameStart, i);

                /* 收集属性剩余部分（直到完整 value 结束），以便跳过重复时能整体丢弃 */
                int afterName = i;
                /* 跳过空白 */
                while (i < n && Character.isWhitespace(content.charAt(i))) {
                    if (content.charAt(i) == '\n') {
                        line++;
                    }
                    i++;
                }
                if (i >= n || content.charAt(i) != '=') {
                    /* 没有=，原样输出属性名及后续空白 */
                    out.append(content.substring(nameStart, i));
                    continue;
                }
                i++; /* skip '=' */
                /* 复制 '=' 以及其后的空白 */
                int valuePrefixStart = afterName;
                /* afterName .. i includes attrName and maybe spaces; but we already have */
                /* attrName in hand. */

                while (i < n && Character.isWhitespace(content.charAt(i))) {
                    if (content.charAt(i) == '\n') {
                        line++;
                    }
                    i++;
                }
                if (i >= n) {
                    out.append(content.substring(nameStart));
                    break;
                }
                char quote = content.charAt(i);
                if (quote != '\'' && quote != '"') {
                    /* 非引号值，原样输出 */
                    out.append(content.substring(nameStart, i));
                    continue;
                }
                i++; /* skip quote */
                int valueStart = i;
                while (i < n && content.charAt(i) != quote) {
                    if (content.charAt(i) == '\n') {
                        line++;
                    }
                    i++;
                }
                if (i >= n) {
                    out.append(content.substring(nameStart));
                    break;
                }
                i++; /* include ending quote */

                String fullAttrSegment = content.substring(nameStart, i);

                if (seenAttr.contains(attrName)) {
                    /* 记录并跳过重复属性 */
                    String msg = String.format("[File: %s, Line: %d] XML element %s attribute %s redundancy.",
                            fileName, tagStartLine, elementName, attrName);
                    errors.add(msg);
                    /* 不输出 fullAttrSegment */
                } else {
                    seenAttr.add(attrName);
                    out.append(fullAttrSegment);
                }
            }

            /* 如果在属性循环里没有正确推进，避免死循环 */
            if (i == tagStart) {
                out.append('<');
                i++;
            }
        }

        return new SanitizationResult(out.toString(), errors);
    }

    /**
     * extractRootDefaultNamespace
     *
     * @brief
     * 功能: 执行 extractRootDefaultNamespace 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String extractRootDefaultNamespace(String content) {
        String rootTag = findRootStartTag(content);
        if (rootTag == null || rootTag.isEmpty()) {
            return null;
        }

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\bxmlns\\s*=\\s*(['\"])([^'\"]*)\\1")
                .matcher(rootTag);
        if (m.find()) {
            String ns = m.group(2);
            if (ns != null && !ns.trim().isEmpty()) {
                return ns.trim();
            }
        }
        return null;
    }

    /**
     * replaceEmptyDefaultNamespace
     *
     * @brief
     * 功能: 执行 replaceEmptyDefaultNamespace 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String replaceEmptyDefaultNamespace(String content, String defaultNamespace) {
        java.util.regex.Pattern p = java.util.regex.Pattern
                .compile("\\bxmlns\\s*=\\s*(['\"])([^'\"]*)\\1");
        java.util.regex.Matcher m = p.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String value = m.group(2);
            if (value == null || value.trim().isEmpty()) {
                String quote = m.group(1);
                String replacement = "xmlns=" + quote + defaultNamespace + quote;
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * findRootStartTag
     *
     * @brief
     * 功能: 执行 findRootStartTag 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String findRootStartTag(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        int idx = 0;
        int n = content.length();
        while (idx < n) {
            int lt = content.indexOf('<', idx);
            if (lt < 0 || lt + 1 >= n) {
                return null;
            }

            char next = content.charAt(lt + 1);
            if (next == '?') {
                int end = content.indexOf("?>", lt + 2);
                if (end < 0) {
                    return null;
                }
                idx = end + 2;
                continue;
            }

            if (next == '!') {
                if (content.startsWith("<!--", lt)) {
                    int end = content.indexOf("-->", lt + 4);
                    if (end < 0) {
                        return null;
                    }
                    idx = end + 3;
                    continue;
                }
                if (content.startsWith("<![CDATA[", lt)) {
                    int end = content.indexOf("]]>", lt + 9);
                    if (end < 0) {
                        return null;
                    }
                    idx = end + 3;
                    continue;
                }
                int end = content.indexOf('>', lt + 2);
                if (end < 0) {
                    return null;
                }
                idx = end + 1;
                continue;
            }

            if (next == '/') {
                idx = lt + 2;
                continue;
            }

            int end = content.indexOf('>', lt + 1);
            if (end < 0) {
                return null;
            }
            return content.substring(lt, end + 1);
        }

        return null;
    }

    /**
     * isNameChar
     *
     * @brief
     * 功能: 执行 isNameChar 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == ':' || c == '_' || c == '-' || c == '.';
    }

    /**
     * countNewlines
     *
     * @brief
     * 功能: 执行 countNewlines 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static int countNewlines(String s) {
        int count = 0;
        for (int k = 0; k < s.length(); k++) {
            if (s.charAt(k) == '\n') {
                count++;
            }
        }
        return count;
    }

    /**
     * 预处理XML文件内容字符串，替换其中所有href属性中的环境变量
     * 
     * @param content XML文件内容
     * @return 预处理后的内容
     */
    public static String preprocessXmlContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        /* 替换所有href属性中的环境变量 */
        java.util.regex.Pattern hrefPattern = java.util.regex.Pattern.compile(
                "(href\\s*=\\s*[\"'])([^\"']*\\$[({][^)}]+[)}][^\"']*)([\"'])");
        java.util.regex.Matcher hrefMatcher = hrefPattern.matcher(content);

        StringBuffer sb = new StringBuffer();
        while (hrefMatcher.find()) {
            String prefix = hrefMatcher.group(1);
            String hrefValue = hrefMatcher.group(2);
            String suffix = hrefMatcher.group(3);

            String resolvedHref = resolveEnvVariables(hrefValue);
            String replacement = prefix + resolvedHref + suffix;
            hrefMatcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        hrefMatcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 解析XML文件，支持环境变量替换
     * 
     * @param xmlFile       XML文件
     * @param xIncludeAware 是否启用XInclude处理
     * @return 解析后的Document
     * @throws Exception 如果解析失败
     */
    public static Document parseXmlWithEnvSupport(File xmlFile, boolean xIncludeAware) throws Exception {
        ensureUtf8XmlEncoding(xmlFile);
        /* 预处理XML文件 */
        org.xml.sax.InputSource inputSource = preprocessXmlFile(xmlFile);

        /* 设置正确的 SystemId，确保中文路径被百分号转义后可被 XInclude 解析。 */
        inputSource.setSystemId(xmlFile.toURI().toASCIIString());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(xIncludeAware);

        /* 禁用 xml:base 属性的自动添加，避免 XSD 验证时报错 */
        if (xIncludeAware) {
            try {
                factory.setFeature("http://apache.org/xml/features/xinclude/fixup-base-uris", false);
                factory.setFeature("http://apache.org/xml/features/xinclude/fixup-language", false);
            } catch (Exception e) {
                /* 忽略不支持的特性 */
            }
        }

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(SILENT_ERROR_HANDLER);

        return builder.parse(inputSource);
    }

    /**
     * 判断异常是否为文件读取错误（如权限被拒绝、文件不存在等）
     */
    private static boolean isFileReadError(Throwable e) {
        /* 检查异常类型 */
        if (e instanceof java.io.FileNotFoundException
                || e instanceof java.security.AccessControlException
                || e instanceof java.nio.file.AccessDeniedException) {
            return true;
        }

        /* 检查完整的异常链（递归检查所有cause） */
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof java.io.IOException
                    || cause instanceof java.io.FileNotFoundException
                    || cause instanceof java.security.AccessControlException
                    || cause instanceof java.nio.file.AccessDeniedException) {
                return true;
            }

            /* 检查cause的消息 */
            String causeMessage = cause.getMessage();
            if (causeMessage != null) {
                String lowerCauseMessage = causeMessage.toLowerCase();
                if (lowerCauseMessage.contains("拒绝访问")
                        || lowerCauseMessage.contains("access denied")
                        || lowerCauseMessage.contains("permission denied")
                        || lowerCauseMessage.contains("denied")
                        || lowerCauseMessage.contains("no such file")
                        || lowerCauseMessage.contains("cannot find")) {
                    return true;
                }
            }

            cause = cause.getCause();
        }

        /* 检查异常消息中是否包含文件读取相关的关键词 */
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("拒绝访问")
                    || lowerMessage.contains("access denied")
                    || lowerMessage.contains("permission denied")
                    || lowerMessage.contains("denied")
                    || lowerMessage.contains("permission")
                    || lowerMessage.contains("no such file")
                    || lowerMessage.contains("cannot find")) {
                return true;
            }

            /* PR_03: 如果异常消息看起来像是文件路径（只包含路径，没有其他错误描述） */
            /* 这通常意味着底层的文件访问错误被包装了，异常消息只剩下文件路径 */
            /* 例如："E:\CPM_verify\CPMs_631\CPM_L1\APP_ATC_INSTANCE.xml" */
            if (message.matches(".*[A-Za-z]:\\\\.*\\.xml.*") /* Windows路径格式 */
                    || message.matches(".*/.*\\.xml.*")) { /* Unix路径格式 */
                /* 检查消息是否基本只包含路径（去除空格后长度接近） */
                String trimmedMessage = message.trim();
                if (!trimmedMessage.contains(" ") ||
                        trimmedMessage.split("\\s+").length <= 2) {
                    /* 消息中几乎没有空格或只有很少单词，很可能只是文件路径 */
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * extractFailedReadFileName
     *
     * @brief
     * 功能: 从异常链中提取实际读取失败的文件名（TR_PR_014: 1019消息应报出
     *       实际无法读取的文件，而非触发XInclude展开的外层文件）。
     * @param[in] e                解析过程中捕获的异常。
     * @param[in] fallbackFilePath 当前正在解析的文件路径（无法提取时回退使用）。
     * @return 实际读取失败的文件名。
     * @scope external
     */
    public static String extractFailedReadFileName(Throwable e, String fallbackFilePath) {
        String fallbackName = new File(fallbackFilePath).getName();
        java.util.regex.Pattern pathPattern = java.util.regex.Pattern.compile(
                "[A-Za-z]:\\\\[^<>:\"|?*]*?\\.(?:xml|xsd)",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Pattern uriPattern = java.util.regex.Pattern.compile(
                "file:/+[^<>:\"|?*\\s]*?\\.(?:xml|xsd)",
                java.util.regex.Pattern.CASE_INSENSITIVE);

        Throwable current = e;
        int depth = 0;
        while (current != null && depth < 8) {
            if (current instanceof org.xml.sax.SAXParseException) {
                String systemId = ((org.xml.sax.SAXParseException) current).getSystemId();
                if (systemId != null) {
                    String idName = new File(systemId.replaceFirst("^file:/+", "")).getName();
                    if (!idName.isEmpty() && !idName.equalsIgnoreCase(fallbackName)) {
                        return idName;
                    }
                }
            }
            String message = current.getMessage();
            if (message != null) {
                java.util.regex.Matcher uriMatcher = uriPattern.matcher(message);
                while (uriMatcher.find()) {
                    String candidate = uriMatcher.group().replaceFirst("file:/+", "");
                    String candidateName = new File(candidate).getName();
                    if (!candidateName.equalsIgnoreCase(fallbackName)) {
                        return candidateName;
                    }
                }
                java.util.regex.Matcher pathMatcher = pathPattern.matcher(message);
                while (pathMatcher.find()) {
                    String candidateName = new File(pathMatcher.group()).getName();
                    if (!candidateName.equalsIgnoreCase(fallbackName)) {
                        return candidateName;
                    }
                }
            }
            current = current.getCause();
            depth++;
        }
        return fallbackName;
    }

    /**
     * 将英文错误信息翻译成中文
     */
    private static String translateErrorMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String lower = message.toLowerCase();

        /* 1. XML File Encoding Format Error（编码相关错误） */
        /* 检测编码格式错误，包括无效的编码名称（如UTF-16 LE）和UTF-8编码异常 */
        if (lower.contains("xml file encoding format error")) {
            return "XML File Encoding Format Error";
        }
        if (lower.contains("invalid byte") || lower.contains("malformed")) {
            if (lower.contains("utf")) {
                return "XML File Encoding Format Error";
            }
            return "XML File Encoding Format Error";
        }
        /* 检测无效的编码名称（如 "Invalid encoding name"） */
        if (lower.contains("encoding") && (lower.contains("invalid") || lower.contains("unsupported"))) {
            return "XML File Encoding Format Error";
        }

        /* 2. Incorrect Start Tag Format（起始标签格式错误） */
        if (lower.contains("element type")
                && (lower.contains("must be followed") || lower.contains("not terminated"))) {
            return "Incorrect Start Tag Format";
        }
        if (lower.contains("content of elements must consist")) {
            return "Incorrect Start Tag Format";
        }
        if (lower.contains("markup") && lower.contains("preceding the root element")) {
            return "Incorrect Start Tag Format";
        }
        if (lower.contains("根元素前") && lower.contains("标记")) {
            return "Incorrect Start Tag Format";
        }

        /* 5. Incorrect closing tag name（结束标签格式错误） */
        if (lower.contains("end-tag") || (lower.contains("closing tag") && lower.contains("does not match"))) {
            return "Incorrect closing tag name";
        }
        if ((lower.contains("结束标签") || lower.contains("结束标记")) && lower.contains("匹配")) {
            return "Incorrect closing tag name";
        }

        /* 6. Root element parsing exception（根元素外存在内容） */
        /* Xerces对“根元素后还有内容”的报错通常为：
         * "The markup in the document following the root element must be well-formed."
         * 该消息同时包含 markup 和 well-formed，不能被表头错误规则抢先匹配。
         */
        if (lower.contains("following the root element")
                || lower.contains("after the root element")
                || lower.contains("trailing section")) {
            return "Root element parsing exception";
        }

        /* 3. Incorrect XML header format（表头/声明错误） */
        /* 必须优先于根元素检测，因为XML头部错误也可能触发"root element"错误消息 */
        if (lower.contains("xml declaration") || lower.contains("prolog")) {
            return "Incorrect XML header format";
        }
        /* 检测"根元素之前的标记"相关错误 - 这实际上是指XML声明部分 */
        if (lower.contains("pseudo attribute name")) {
            return "Incorrect XML header format";
        }
        if (lower.contains("伪属性")) {
            return "Incorrect XML header format";
        }
        /* 检测处理指令相关错误（XML声明本身就是一个处理指令） */
        if (lower.contains("processing instruction")) {
            /* 如果明确提到"target"，可能是注释格式问题 */
            if (lower.contains("target") && lower.contains("matching")) {
                return "Incorrect XML comment format";
            }
            /* 其他处理指令错误通常是XML头部问题 */
            return "Incorrect XML header format";
        }
        /* 检测"content is not allowed in prolog"等前导内容错误 */
        if (lower.contains("content") && lower.contains("prolog")) {
            return "Incorrect XML header format";
        }
        /* 检测"markup must be well-formed"相关错误 */
        if (lower.contains("markup") && lower.contains("well-formed")) {
            return "Incorrect XML header format";
        }

        /* 4. Incorrect XML comment format（注释格式错误） */
        if (lower.contains("comment") && (lower.contains("not well-formed") || lower.contains("invalid"))) {
            return "Incorrect XML comment format";
        }
        if (lower.contains("comments") && lower.contains("not permitted")) {
            return "Incorrect XML comment format";
        }
        if (lower.contains("注释") && (lower.contains("不允许") || lower.contains("--"))) {
            return "Incorrect XML comment format";
        }

        /* 内存错误（归入7种reason之一：Root element parsing exception） */
        if (lower.contains("out of memory") || lower.contains("java heap space")) {
            return "Root element parsing exception";
        }

        /* 6. Root element parsing exception（根元素错误） */
        /* 检测真正的根元素错误（如根元素外有内容） */
        if (lower.contains("root element") || lower.contains("document element")
                || lower.contains("trailing section")) {
            return "Root element parsing exception";
        }
        if (lower.contains("根元素后") && lower.contains("标记")) {
            return "Root element parsing exception";
        }

        /* 7. Attribute Definition Exception（属性错误） */
        if (lower.contains("attribute")
                && (lower.contains("invalid") || lower.contains("duplicate") || lower.contains("malformed")
                        || lower.contains("open quote is expected"))) {
            return "Attribute Definition Exception";
        }
        if (lower.contains("属性") && (lower.contains("引号") || lower.contains("重复")
                || lower.contains("属性值"))) {
            return "Attribute Definition Exception";
        }

        /* 其他常见错误（归入7种reason之一：Root element parsing exception） */
        if (lower.contains("premature end of file") || lower.contains("unexpected end of file")) {
            return "Root element parsing exception";
        }
        if (lower.contains("not well-formed")) {
            return "Root element parsing exception";
        }

        /* 未匹配的场景统一归入7种reason之一（Root element parsing exception），
         * 保证reason取值始终为需求表中的7种之一 */
        return "Root element parsing exception";
    }

    /**
     * 处理XML解析异常消息，将不友好的错误信息转换为更易理解的中文提示
     * 使用Throwable而非Exception以便处理OutOfMemoryError等Error类型
     */
    private static String formatXmlErrorMessage(Throwable e) {
        String message = e.getMessage() != null ? e.getMessage().trim() : "";

        /* 去除末尾的点和空格 */
        if (message != null && !message.isEmpty()) {
            message = message.replaceAll("[.\\s]+$", "");
        }

        if (message.isEmpty()) {
            message = e.getClass().getSimpleName();
        }

        /* 翻译错误信息 */
        return translateErrorMessage(message);
    }

    /**
     * PR_03: 静默ErrorHandler，用于抑制SAX解析器的默认[Fatal Error]输出
     */
    private static final ErrorHandler SILENT_ERROR_HANDLER = new ErrorHandler() {
        /**
         * warning
         *
         * @brief
         * 功能: 执行 warning 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        @Override
        public void warning(SAXParseException e) throws SAXException {
            /* 静默处理 */
        }

        /**
         * error
         *
         * @brief
         * 功能: 执行 error 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        @Override
        public void error(SAXParseException e) throws SAXException {
            /* 静默处理 */
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
        public void fatalError(SAXParseException e) throws SAXException {
            /* 静默处理，但抛出异常让调用者知道解析失败 */
            throw e;
        }
    };

    /**
     * resolveInclude
     *
     * @brief
     * 功能: 执行 resolveInclude 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static Element resolveInclude(Element elem, String baseDir) throws Exception {
        NodeList includeList = elem.getElementsByTagNameNS("http://www.w3.org/2001/XInclude", "include");
        if (includeList.getLength() > 0) {
            Element includeElem = (Element) includeList.item(0);
            String href = includeElem.getAttribute("href");
            /* 解析环境变量 */
            String resolvedHref = resolveEnvVariables(href);
            File includeFile;
            /* 如果解析后的路径是绝对路径，直接使用；否则相对于baseDir */
            if (new File(resolvedHref).isAbsolute()) {
                includeFile = new File(resolvedHref);
            } else {
                includeFile = new File(baseDir, resolvedHref);
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(SILENT_ERROR_HANDLER);
            Document doc = builder.parse(includeFile);
            return doc.getDocumentElement();
        }
        return elem;
    }

    /**
     * 返回元素下第一个 XInclude 文件的实际路径；没有 include 时返回 null。
     * 缺失必选子元素属于 XSD 正确性校验问题，不能在这里因空节点解引用被误报为 1017。
     */
    private static String resolveIncludedFilePath(Element parent, String baseDir) {
        if (parent == null) {
            return null;
        }
        NodeList includeList = parent.getElementsByTagNameNS("http://www.w3.org/2001/XInclude", "include");
        if (includeList.getLength() == 0) {
            return null;
        }
        Element include = (Element) includeList.item(0);
        String href = resolveEnvVariables(include.getAttribute("href"));
        if (href == null || href.trim().isEmpty()) {
            return null;
        }
        File included = new File(href);
        return included.isAbsolute() ? included.getPath() : new File(baseDir, href).getPath();
    }

    /* CoreOS */
    /**
     * parseCoreOSXml
     *
     * @brief
     * 功能: 执行 parseCoreOSXml 相关处理。
     *
     * @scope external
     * @errno:
     * N/A
     */
    public static CoreOS parseCoreOSXml(String filePath) {
        setCurrentParsingFile(filePath);
        try {
            File file = new File(filePath);
            ensureUtf8XmlEncoding(file);
            String baseDir = file.getParent();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(SILENT_ERROR_HANDLER);
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();
            CoreOS coreOS = new CoreOS();

            /* KernelName */
            if (root.hasAttribute("KernelName")) {
                coreOS.setKernelName(root.getAttribute("KernelName"));
            }

            /* KernelConfiguration */
            NodeList kernelConfigList = root.getElementsByTagNameNS("*", "KernelConfiguration");
            if (kernelConfigList.getLength() > 0) {
                Element e = (Element) kernelConfigList.item(0);
                CoreOS.KernelConfiguration kc = new CoreOS.KernelConfiguration();
                kc.setfAddressSpaceRegionAllocationUnit(parseInt(e.getAttribute("addressSpaceRegionAllocationUnit")));
                kc.setfAddressSpaceSize(parseLong(e.getAttribute("addressSpaceSize")));
                kc.setfKernelVirtualAddress(parseLong(e.getAttribute("kernelVirtualAddress")));
                kc.setfAMemoryAllocationDisabled(parseBool(e.getAttribute("memoryAllocationDisabled")) ? 1 : 0);
                kc.setfPartitionVirtualAddress(parseLong(e.getAttribute("partitionVirtualAddress")));
                kc.setfSysClkRateHz(parseInt(e.getAttribute("sysClkRateHz")));
                kc.setfUnsafeMethodTimeout(parseLong(e.getAttribute("unsafeMethodTimeout")));
                kc.setfWatchDogTimerDuration(parseLong(e.getAttribute("watchDogTimerDuration")));
                kc.setfPartitionSpaceSize(parseLong(e.getAttribute("partitionSpaceSize")));
                if (e.hasAttribute("user1")) {
                    kc.setfUser1(parseInt(e.getAttribute("user1")));
                } 
                if (e.hasAttribute("user2")) {
                    kc.setfUser2(parseInt(e.getAttribute("user2")));
                }
                /* 解析payloadMapSize属性（支持十六进制格式如0x3300000） */
                if (e.hasAttribute("payloadMapSize")) {
                    kc.setfPayloadMapSize(e.getAttribute("payloadMapSize"));
                }
                coreOS.setfKernelConfiguration(kc);
            }

            /* MemorySize */
            NodeList memorySizeList = root.getElementsByTagNameNS("*", "MemorySize");
            if (memorySizeList.getLength() > 0) {
                Element e = (Element) memorySizeList.item(0);
                CoreOS.MemorySize ms = new CoreOS.MemorySize();
                ms.setfMemorySizeBss(parseLong(e.getAttribute("MemorySizeBss")));
                ms.setfMemorySizeData(parseLong(e.getAttribute("MemorySizeData")));
                ms.setfMemorySizeRoData(parseLong(e.getAttribute("MemorySizeRoData")));
                ms.setfMemorySizeText(parseLong(e.getAttribute("MemorySizeText")));
                coreOS.setfMemorySize(ms);
            }

            /* HardwareConfiguration */
            NodeList hardwareConfigList = root.getElementsByTagNameNS("*", "HardwareConfiguration");
            if (hardwareConfigList.getLength() > 0) {
                Element hcElem = (Element) hardwareConfigList.item(0);
                CoreOS.HardwareConfiguration hc = new CoreOS.HardwareConfiguration();

                /* PhysicalMemory */
                NodeList pmList = hcElem.getElementsByTagNameNS("*", "PhysicalMemory");
                if (pmList.getLength() > 0) {
                    Element pmElem = (Element) pmList.item(0);
                    CoreOS.HardwareConfiguration.PhysicalMemory pm = new CoreOS.HardwareConfiguration.PhysicalMemory();
                    pm.setfBaseAddress(parseLong(pmElem.getAttribute("Base_Address")));
                    pm.setfSize(parseLong(pmElem.getAttribute("Size")));
                    /* kernelMemoryRegion */
                    NodeList kmrList = pmElem.getElementsByTagNameNS("*", "kernelMemoryRegion");
                    if (kmrList.getLength() > 0)
                        pm.setfKernelMemoryRegionSize(parseLong(((Element) kmrList.item(0)).getAttribute("Size")));
                    /* kernelConfigRecordRegion */
                    NodeList kcrrList = pmElem.getElementsByTagNameNS("*", "kernelConfigRecordRegion");
                    if (kcrrList.getLength() > 0)
                        pm.setfKernelConfigRecordRegionSize(((Element) kcrrList.item(0)).getAttribute("Size"));
                    /* kernelPgPool */
                    NodeList kppList = pmElem.getElementsByTagNameNS("*", "kernelPgPool");
                    if (kppList.getLength() > 0)
                        pm.setfKernelPgPoolSize(parseLong(((Element) kppList.item(0)).getAttribute("Size")));
                    /* portRegion */
                    NodeList prList = pmElem.getElementsByTagNameNS("*", "portRegion");
                    if (prList.getLength() > 0)
                        pm.setfPortRegionSize(parseLong(((Element) prList.item(0)).getAttribute("Size")));
                    /* hmLogRegion */
                    NodeList hlrList = pmElem.getElementsByTagNameNS("*", "hmLogRegion");
                    if (hlrList.getLength() > 0)
                        pm.setfHmLogRegionSize(parseLong(((Element) hlrList.item(0)).getAttribute("Size")));
                    /* ramPayloadRegion */
                    NodeList rprList = pmElem.getElementsByTagNameNS("*", "ramPayloadRegion");
                    if (rprList.getLength() > 0) {
                        Element rprElem = (Element) rprList.item(0);
                        pm.setfRamPayloadRegionBaseAddress(rprElem.getAttribute("Base_Address"));
                        pm.setfRamPayloadRegionSize(rprElem.getAttribute("Size"));
                    }
                    /* libRegion */
                    NodeList lrList = pmElem.getElementsByTagNameNS("*", "libRegion");
                    if (lrList.getLength() > 0) {
                        Element lrElem = (Element) lrList.item(0);
                        pm.setfLibRegionBaseAddress(lrElem.getAttribute("Base_Address"));
                        pm.setfLibRegionSize(lrElem.getAttribute("Size"));
                    }
                    /* kernelRegion - 应该 */
                    NodeList krList = pmElem.getElementsByTagNameNS("*", "kernelRegion");
                    for (int i = 0; i < krList.getLength(); i++) {
                        Element krElem = (Element) krList.item(i);
                        CoreOS.HardwareConfiguration.KernelRegion kernelRegion = new CoreOS.HardwareConfiguration.KernelRegion();
                        kernelRegion.setfSize(krElem.getAttribute("Size"));
                        kernelRegion.setfPoolName(krElem.getAttribute("PoolName"));
                        /* WriteProtected 应该 false */
                        String writeProtected = krElem.getAttribute("WriteProtected");
                        if (writeProtected != null && !writeProtected.isEmpty()) {
                            kernelRegion.setfWriteProtected(Boolean.parseBoolean(writeProtected));
                        }
                        pm.addKernelRegion(kernelRegion);
                    }
                    hc.setfPhysicalMemory(pm);
                }

                /* userMemoryRegion */
                NodeList umrList = hcElem.getElementsByTagNameNS("*", "userMemoryRegion");
                if (umrList.getLength() > 0) {
                    Element umrElem = (Element) umrList.item(0);
                    CoreOS.HardwareConfiguration.UserMemoryRegion umr = new CoreOS.HardwareConfiguration.UserMemoryRegion();
                    umr.setfBaseAddress(parseLong(umrElem.getAttribute("Base_Address")));
                    umr.setfSize(parseLong(umrElem.getAttribute("Size")));
                    hc.setfUserMemoryRegion(umr);
                }

                /* kernelIO */
                NodeList kioList = hcElem.getElementsByTagNameNS("*", "kernelIO");
                for (int i = 0; i < kioList.getLength(); i++) {
                    Element kioElem = (Element) kioList.item(i);
                    CoreOS.HardwareConfiguration.KernelIO kio = new CoreOS.HardwareConfiguration.KernelIO();
                    kio.setfBaseAddress(parseLong(kioElem.getAttribute("Base_Address")));
                    kio.setfCachePolicy(kioElem.getAttribute("CachePolicy"));
                    kio.setKernelName(kioElem.getAttribute("PoolName"));
                    kio.setfSize(parseLong(kioElem.getAttribute("Size")));
                    kio.setfVirtualAddress(parseLong(kioElem.getAttribute("VirtualAddress")));
                    hc.addKernelIO(kio);
                }

                /* payloadMemory */
                NodeList pmemList = hcElem.getElementsByTagNameNS("*", "payloadMemory");
                if (pmemList.getLength() > 0) {
                    Element pmemElem = (Element) pmemList.item(0);
                    CoreOS.HardwareConfiguration.PayloadMemory pmem = new CoreOS.HardwareConfiguration.PayloadMemory();
                    pmem.setfBaseAddress(parseLong(pmemElem.getAttribute("Base_Address")));
                    pmem.setfSize(parseLong(pmemElem.getAttribute("Size")));
                    hc.setfPayloadMemory(pmem);
                }

                /* sharedIO - 解析coreOS.xml中的sharedIO元素 */
                NodeList sioList = hcElem.getElementsByTagNameNS("*", "sharedIO");
                if (sioList.getLength() > 0) {
                    Element sioElem = (Element) sioList.item(0);
                    CoreOS.HardwareConfiguration.SharedIO sio = new CoreOS.HardwareConfiguration.SharedIO();
                    sio.setfBaseAddress(parseLong(sioElem.getAttribute("Base_Address")));
                    sio.setfCachePolicy(sioElem.getAttribute("CachePolicy"));
                    sio.setfPoolName(sioElem.getAttribute("PoolName"));
                    sio.setfSize(parseLong(sioElem.getAttribute("Size")));
                    sio.setfVirtualAddress(parseLong(sioElem.getAttribute("VirtualAddress")));
                    hc.setfSharedIO(sio);
                }

                coreOS.setfHardwareConfiguration(hc);
            }

            /* PR_07: 打印解析XML文件成功 */
            Log.info("os cfg XML file parsed successfully. File Path: " + getCleanPath(file));
            return coreOS;
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            /* 检查是否为文件读取错误（如权限被拒绝） */
            if (isFileReadError(e)) {
                String fileName = extractFailedReadFileName(e, filePath);
                String errorMsg = "XML file " + fileName + " read failed.";
                Log.error(CodeConstant.ERROR_XML_READ_FAILED, errorMsg);
            } else {
                if (isDuplicateAttributeError(e)) {
                    int line = (e instanceof org.xml.sax.SAXParseException)
                            ? ((org.xml.sax.SAXParseException) e).getLineNumber()
                            : -1;
                    String[] elemAttr = extractElementAndAttributeFromDuplicateMsg(reason);
                    String fileName = new File(filePath).getName();
                    String msg = String.format("[File: %s, Line: %d] XML element %s attribute %s redundancy.",
                            fileName, line, elemAttr[0], elemAttr[1]);
                    Log.error(CodeConstant.ERROR_XML_REDUNDANCY_ATTRIBUTES, msg);
                } else {
                    String errorMsg = buildParseFailedMessage(reason);
                    Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
                }
            }
            return null;
        }
    }

    public static com.nwpu.verify.entity.Module parseModuleXml(String filePath) {
        setCurrentParsingFile(filePath);
        try {
            File file = new File(filePath);
            ensureUtf8XmlEncoding(file);
            String baseDir = file.getParent();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            /* 解析分区XInclude应该Include解析 */
            /* factory.setXIncludeAware(true); // 不展开XInclude */
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(SILENT_ERROR_HANDLER);
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();
            Module module = new Module();

            /* Id 范围由XSD的ModuleIdType约束校验（TR_PR_011: 超范围报2014） */
            if (root.hasAttribute("Id")) {
                String idStr = root.getAttribute("Id");
                try {
                    int id;
                    if (idStr.toLowerCase().startsWith("0x")) {
                        id = Integer.parseInt(idStr.substring(2), 16);
                    } else {
                        id = Integer.parseInt(idStr);
                    }
                    module.setfId(id);
                } catch (NumberFormatException e) {
                    Log.warning(CodeConstant.ERROR_XML_ATTRIBUTE_DATA_TYPE_INVALID,
                            "Failed to parse Module Id: " + idStr);
                }
            }

            /* CRC32 - TR_PR_017: CRC属性缺失或格式错误报4005警告 */
            if (root.hasAttribute("CRC32")) {
                String crc32Value = root.getAttribute("CRC32");
                if (crc32Value == null || crc32Value.trim().isEmpty()) {
                    /* CRC32属性存在但值为空，报4005警告 */
                    Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                            "XML file " + file.getName() + " CRC attribute missing or type error.");
                }
                module.setfCrc32(crc32Value);
            } else {
                /* CRC32属性缺失，报4005警告（TR_PR_017） */
                Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                        "XML file " + file.getName() + " CRC attribute missing or type error.");
            }

            /* CoreOS */
            NodeList coreOSList = root.getElementsByTagNameNS("*", "CoreOS");
            if (coreOSList.getLength() > 0) {
                Element coreOSElem = (Element) coreOSList.item(0);
                String coreOSPath = resolveIncludedFilePath(coreOSElem, baseDir);
                if (coreOSPath != null) {
                    CoreOS coreOS = parseCoreOSXml(coreOSPath);
                    module.setfCoreOS(coreOS);
                }
            }

            /* Applications */
            NodeList applicationsList = root.getElementsByTagNameNS("*", "Applications");
            if (applicationsList.getLength() > 0) {
                Element applicationsElem = (Element) applicationsList.item(0);
                NodeList applicationList = applicationsElem.getElementsByTagNameNS("*", "Application");
                for (int i = 0; i < applicationList.getLength(); i++) {
                    Element appElem = (Element) applicationList.item(i);
                    String appName = appElem.getAttribute("Name");
                    String appPath = resolveIncludedFilePath(appElem, baseDir);
                    if (appPath == null) {
                        continue;
                    }
                    ApplicationEntity application = parseApplicationXml(appPath);
                    /* 解析分区? 2属性 null 解析? NullPointerException */
                    if (application == null) {
                        /* parseApplicationXml应该 */
                        continue; /* 应该NPE */
                    }
                    application.setfName(appName);
                    module.addApplication(appName, application);

                }
            }

            /* SharedLibraryRegions */
            NodeList sharedLibraryRegionsList = root.getElementsByTagNameNS("*", "SharedLibraryRegions");
            if (sharedLibraryRegionsList.getLength() > 0) {
                Element sharedLibraryRegionsElem = (Element) sharedLibraryRegionsList.item(0);
                NodeList sharedLibraryList = sharedLibraryRegionsElem.getElementsByTagNameNS("*", "SharedLibrary");
                for (int i = 0; i < sharedLibraryList.getLength(); i++) {
                    Element libElem = (Element) sharedLibraryList.item(i);
                    String libName = libElem.getAttribute("Name");
                    String libPath = resolveIncludedFilePath(libElem, baseDir);
                    if (libPath == null) {
                        continue;
                    }
                    SharedLibrary sharedLibrary = parseSharedLibraryXml(libPath);
                    if (sharedLibrary == null) {
                        continue;
                    }
                    sharedLibrary.setfName(libName);
                    module.addSharedLibrary(libName, sharedLibrary);
                }
            }

            /* Partitions */
            NodeList partitionsList = root.getElementsByTagNameNS("*", "Partitions");
            if (partitionsList.getLength() > 0) {
                Element partitionsElem = (Element) partitionsList.item(0);

                /* 解析分区 */
                NodeList partitionList = partitionsElem.getElementsByTagNameNS("*", "Partition");
                for (int i = 0; i < partitionList.getLength(); i++) {
                    Element partElem = (Element) partitionList.item(i);
                    String partName = partElem.getAttribute("Name");
                    String partIdentifierAttr = partElem.getAttribute("Identifier");
                    boolean hasRenamedIdentifier = partElem.hasAttribute("Identifier_test");
                    if (hasRenamedIdentifier) {
                        /* 交给 XSD 校验层统一输出 2008，避免与解析层重复输出；
                         * 解析层也不再因这个替换属性附带输出 Identifier 缺失的 2011。 */
                        continue;
                    }
                    if (partIdentifierAttr == null || partIdentifierAttr.trim().isEmpty()) {
                        int line = getElementLineNumber(file, "Partition");
                        Log.error(CodeConstant.ERROR_XML_ELEMENT_ATTRIBUTE_MISSING,
                                String.format("[File: %s, Line: %d] XML element Partition attribute Identifier missing.",
                                        file.getName(), line));
                        continue;
                    }
                    int partIdentifier;
                    try {
                        partIdentifier = Integer.decode(partIdentifierAttr);
                    } catch (NumberFormatException ex) {
                        int line = getElementLineNumber(file, "Partition");
                        Log.error(CodeConstant.ERROR_XML_ATTRIBUTE_UNDEFINED,
                                String.format("[File: %s, Line: %d] undefined XML element attribute Identifier.",
                                        file.getName(), line));
                        continue;
                    }
                    String partPath = resolveIncludedFilePath(partElem, baseDir);
                    if (partPath == null) {
                        continue;
                    }
                    Partition partition = parsePartitionXml(partPath);
                    if (partition == null) {
                        continue;
                    }
                    partition.setfName(partName);
                    partition.setfIdentifier(partIdentifier);
                    module.addPartition(partName, partition);
                }

                /* 解析分区PseudoPartition? */
                NodeList pseudoPartitionList = partitionsElem.getElementsByTagNameNS("*", "PseudoPartition");
                for (int i = 0; i < pseudoPartitionList.getLength(); i++) {
                    Element pseudoPartElem = (Element) pseudoPartitionList.item(i);
                    String partName = pseudoPartElem.getAttribute("Name");
                    String partIdentifierAttr = pseudoPartElem.getAttribute("Identifier");
                    if (partIdentifierAttr == null || partIdentifierAttr.trim().isEmpty()) {
                        int line = getElementLineNumber(file, "PseudoPartition");
                        Log.error(CodeConstant.ERROR_XML_ELEMENT_ATTRIBUTE_MISSING,
                                String.format("[File: %s, Line: %d] XML element PseudoPartition attribute Identifier missing.",
                                        file.getName(), line));
                        continue;
                    }
                    int partIdentifier;
                    try {
                        partIdentifier = Integer.decode(partIdentifierAttr);
                    } catch (NumberFormatException ex) {
                        int line = getElementLineNumber(file, "PseudoPartition");
                        Log.error(CodeConstant.ERROR_XML_ATTRIBUTE_UNDEFINED,
                                String.format("[File: %s, Line: %d] undefined XML element attribute Identifier.",
                                        file.getName(), line));
                        continue;
                    }

                    /* 应该 */
                    Partition pseudoPartition = parsePseudoPartitionXml(pseudoPartElem);
                    pseudoPartition.setfName(partName);
                    pseudoPartition.setfIdentifier(partIdentifier);
                    pseudoPartition.setfIsPseudoPartition(true);
                    module.addPartition(partName, pseudoPartition);
                }
            }

            /* 解析 LogBookList */
            NodeList logBookListElements = root.getElementsByTagNameNS("*", "LogBookList"); /*  */
            if (logBookListElements.getLength() > 0) {
                Element logBookListElem = (Element) logBookListElements.item(0);
                NodeList logBooks = logBookListElem.getElementsByTagNameNS("*", "LogBook"); /*  */
                for (int i = 0; i < logBooks.getLength(); i++) {
                    Element logBookElem = (Element) logBooks.item(i);
                    LogBook logBook = new LogBook();

                    /* 读取 LogbookName 属性 */
                    logBook.setfLogbookName(logBookElem.getAttribute("LogbookName").toCharArray());

                    /* 读取 MaxMessageSize 属性 */
                    logBook.setfMaxMessageSize(parseLong(logBookElem.getAttribute("MaxMessageSize")));

                    /* 读取 MaxNBLoggedMessages 属性 */
                    logBook.setfMaxNBLoggedMessages(parseLong(logBookElem.getAttribute("MaxNBLoggedMessages")));

                    /* 读取 MaxNBInProgressMessages 属性 */
                    logBook.setfMaxNBInProgressMessages(parseLong(logBookElem.getAttribute("MaxNBInProgressMessages")));

                    /* 读取 serialNumber 属性 */
                    logBook.setfSerialNumber(parseLong(logBookElem.getAttribute("serialNumber")));

                    /* 读取 PartitionNameRef 属性 */
                    if (logBookElem.hasAttribute("PartitionNameRef")) {
                        logBook.setPartitionNameRef(logBookElem.getAttribute("PartitionNameRef"));
                    }

                    module.addLogBook(new String(logBook.getfLogbookName()), logBook);
                }
            }

            /* ========================================================================================================= */
            /* 解析 HealthMonitor -> HMTask -> PartitionHMTaskList + CoreHMTaskList + */
            /* CoreHMTable + MultiPartitionHMTable */
            /* ========================================================================================================= */
            NodeList healthMonitorList = root.getElementsByTagNameNS("*", "HealthMonitor");
            if (healthMonitorList.getLength() > 0) {
                Element healthMonitorElem = (Element) healthMonitorList.item(0);
                HealthMonitor healthMonitor = new HealthMonitor(); /* 创建 HealthMonitor 对象 */

                /* 解析 HMTask -> PartitionHMTaskList */
                NodeList hmTaskList = healthMonitorElem.getElementsByTagNameNS("*", "HMTask");
                if (hmTaskList.getLength() > 0) {
                    Element hmTaskElem = (Element) hmTaskList.item(0);

                    NodeList partitionHMTaskList = hmTaskElem.getElementsByTagNameNS("*", "PartitionHMTaskList");
                    if (partitionHMTaskList.getLength() > 0) {
                        Element partitionHMTaskListElem = (Element) partitionHMTaskList.item(0);
                        NodeList partitionHMTasks = partitionHMTaskListElem.getElementsByTagNameNS("*",
                                "PartitionHMTask");

                        for (int i = 0; i < partitionHMTasks.getLength(); i++) {
                            Element taskElem = (Element) partitionHMTasks.item(i);
                            PartitionHMTask task = new PartitionHMTask();

                            /* 解析 PartitionHMTask 各属性 */
                            task.setfTaskName(taskElem.getAttribute("Name"));
                            task.setfCallbackName(taskElem.getAttribute("Callback"));
                            task.setfAttributeMask(parseLong(taskElem.getAttribute("attributeMask")));
                            task.setfErrorHandlerQueueThreshold(
                                    parseLong(taskElem.getAttribute("errorHandlerQueueThreshold")));
                            task.setfEventFilterMask(parseLong(taskElem.getAttribute("eventFilterMask")));
                            task.setfLogEntriesThreshold(parseLong(taskElem.getAttribute("logEntriesThreshold")));
                            task.setfMaxErrorHandlerQueueDepth(
                                    parseLong(taskElem.getAttribute("maxErrorHandlerQueueDepth")));
                            task.setfMaxLogEntries(parseLong(taskElem.getAttribute("maxLogEntries")));
                            task.setfMaxQueueDepth(parseLong(taskElem.getAttribute("maxQueueDepth")));
                            task.setfQueueThreshold(parseLong(taskElem.getAttribute("queueThreshold")));
                            /* stackSize 缺省时保留 PartitionHMTask 定义的 0x1800 默认值 */
                            if (taskElem.hasAttribute("stackSize")) {
                                task.setfStackSize(parseLong(taskElem.getAttribute("stackSize")));
                            }

                            /* 解析 PartitionRef 引用的ID 列表 */
                            NodeList partitionRefs = taskElem.getElementsByTagNameNS("*", "PartitionRef");
                            List<Long> partitionRefIds = new ArrayList<>();
                            for (int j = 0; j < partitionRefs.getLength(); j++) {
                                Element refElem = (Element) partitionRefs.item(j);
                                partitionRefIds.add(parseLong(refElem.getAttribute("Id")));
                            }
                            task.setfPartitionRefIds(partitionRefIds);
                            /* fPartitionRefIdsCount (用 ID 用) 应该 */

                            healthMonitor.addPartitionHMTask(task.getfTaskNameAsString(), task);
                        }
                    }

                    /* 解析 CoreHMTaskList */
                    NodeList coreHMTaskList = hmTaskElem.getElementsByTagNameNS("*", "CoreHMTaskList");
                    if (coreHMTaskList.getLength() > 0) {
                        Element coreHMTaskListElem = (Element) coreHMTaskList.item(0);
                        NodeList coreHMTasks = coreHMTaskListElem.getElementsByTagNameNS("*", "CoreHMTask");

                        for (int i = 0; i < coreHMTasks.getLength(); i++) {
                            Element taskElem = (Element) coreHMTasks.item(i);
                            CoreHMTask task = new CoreHMTask();

                            /* 解析 CoreHMTask 各属性 */
                            task.setfTaskName(taskElem.getAttribute("Name"));
                            task.setfCallbackName(taskElem.getAttribute("Callback"));
                            task.setfAttributeMask(parseLong(taskElem.getAttribute("attributeMask")));
                            task.setfErrorHandlerQueueThreshold(
                                    parseLong(taskElem.getAttribute("errorHandlerQueueThreshold")));
                            task.setfEventFilterMask(parseLong(taskElem.getAttribute("eventFilterMask")));
                            task.setfLogEntriesThreshold(parseLong(taskElem.getAttribute("logEntriesThreshold")));
                            task.setfMaxErrorHandlerQueueDepth(
                                    parseLong(taskElem.getAttribute("maxErrorHandlerQueueDepth")));
                            task.setfMaxLogEntries(parseLong(taskElem.getAttribute("maxLogEntries")));
                            task.setfMaxQueueDepth(parseLong(taskElem.getAttribute("maxQueueDepth")));
                            task.setfQueueThreshold(parseLong(taskElem.getAttribute("queueThreshold")));
                            task.setfStackSize(parseLong(taskElem.getAttribute("stackSize")));

                            /* 解析 CoreRef 引用的核心ID列表 */
                            NodeList coreRefs = taskElem.getElementsByTagNameNS("*", "CoreRef");
                            List<Long> coreRefIds = new ArrayList<>();
                            for (int j = 0; j < coreRefs.getLength(); j++) {
                                Element refElem = (Element) coreRefs.item(j);
                                coreRefIds.add(parseLong(refElem.getAttribute("Id")));
                            }
                            task.setfCoreRefIds(coreRefIds);
                            if (coreRefIds.isEmpty()) {
                                NodeList coreNodes = taskElem.getElementsByTagNameNS("*", "Core");
                                for (int j = 0; j < coreNodes.getLength(); j++) {
                                    Element coreElem = (Element) coreNodes.item(j);
                                    if (coreElem.hasAttribute("cpuId")) {
                                        coreRefIds.add(parseLong(coreElem.getAttribute("cpuId")));
                                    }
                                }
                                task.setfCoreRefIds(coreRefIds);
                            }
                            /* fCoreRefIdsCount (用 ID 用) 应该 */

                            healthMonitor.addCoreHMTask(task.getfTaskNameAsString(), task);
                        }
                    }
                }

                /* 解析 CoreHMTable */
                NodeList coreHMTableList = healthMonitorElem.getElementsByTagNameNS("*", "CoreHMTable");
                for (int i = 0; i < coreHMTableList.getLength(); i++) {
                    Element coreHMTableElem = (Element) coreHMTableList.item(i);
                    CoreHMTable coreHMTable = parseCoreHMTableXml(coreHMTableElem);
                    if (coreHMTable != null) {
                        healthMonitor.addCoreHMTable(coreHMTable.getNameAsString(), coreHMTable);
                    }
                }

                /* 解析 MultiPartitionHMTable */
                NodeList multiPartitionHMTableList = healthMonitorElem.getElementsByTagNameNS("*",
                        "MultiPartitionHMTable");
                for (int i = 0; i < multiPartitionHMTableList.getLength(); i++) {
                    Element multiPartitionHMTableElem = (Element) multiPartitionHMTableList.item(i);
                    MultiPartitionHMTable multiPartitionHMTable = parseMultiPartitionHMTableXml(
                            multiPartitionHMTableElem);
                    if (multiPartitionHMTable != null) {
                        healthMonitor.addMultiPartitionHMTable(multiPartitionHMTable.getNameAsString(),
                                multiPartitionHMTable);
                    }
                }

                /* 解析 PartitionHMTable */
                NodeList partitionHMTableList = healthMonitorElem.getElementsByTagNameNS("*", "PartitionHMTable");
                for (int i = 0; i < partitionHMTableList.getLength(); i++) {
                    Element partitionHMTableElem = (Element) partitionHMTableList.item(i);
                    PartitionHMTable partitionHMTable = parsePartitionHMTableXml(partitionHMTableElem);
                    if (partitionHMTable != null) {
                        healthMonitor.addPartitionHMTable(partitionHMTable.getTableNameAsString(), partitionHMTable);
                    }
                }

                /* 解析 CoreHmState */
                NodeList coreHmStateList = healthMonitorElem.getElementsByTagNameNS("*", "CoreHmState");
                for (int i = 0; i < coreHmStateList.getLength(); i++) {
                    Element stateElem = (Element) coreHmStateList.item(i);
                    CoreHMState coreHMState = new CoreHMState();

                    if (stateElem.hasAttribute("Description")) {
                        coreHMState.setDescription(stateElem.getAttribute("Description"));
                    }
                    if (stateElem.hasAttribute("StateIdentifier")) {
                        coreHMState.setStateIdentifier(parseInt(stateElem.getAttribute("StateIdentifier")));
                    }

                    /* 使用 StateIdentifier 作为 key */
                    String key = "State_" + coreHMState.getStateIdentifier();
                    healthMonitor.addCoreHMState(key, coreHMState);
                }

                module.setfHealthMonitor(healthMonitor); /* 解析分区? HealthMonitor 属性 Module ? */
            }

            /* Schedules */
            NodeList schedulesList = root.getElementsByTagNameNS("*", "Schedules");
            if (schedulesList.getLength() > 0) {
                Element schedulesElem = (Element) schedulesList.item(0);
                java.util.Map<String, Integer> scheduleCpuIdMap = new java.util.HashMap<>();
                java.util.Map<String, Integer> partitionCpuIdMap = new java.util.HashMap<>();
                NodeList bmpScheduleList = schedulesElem.getElementsByTagNameNS("*", "BmpSchedule");
                for (int b = 0; b < bmpScheduleList.getLength(); b++) {
                    Element bmpScheduleElem = (Element) bmpScheduleList.item(b);
                    NodeList coreNodes = bmpScheduleElem.getElementsByTagNameNS("*", "Core");
                    for (int c = 0; c < coreNodes.getLength(); c++) {
                        Element coreElem = (Element) coreNodes.item(c);
                        int cpuId = 0;
                        if (coreElem.hasAttribute("cpuId")) {
                            cpuId = parseInt(coreElem.getAttribute("cpuId"));
                        }
                        NodeList coreScheduleList = coreElem.getElementsByTagNameNS("*", "Schedule");
                        for (int s = 0; s < coreScheduleList.getLength(); s++) {
                            Element coreScheduleElem = (Element) coreScheduleList.item(s);
                            if (coreScheduleElem.hasAttribute("ScheduleName")) {
                                scheduleCpuIdMap.put(coreScheduleElem.getAttribute("ScheduleName"), cpuId);
                            }
                        }
                    }
                }
                NodeList scheduleList = schedulesElem.getElementsByTagNameNS("*", "Schedule");
                for (int i = 0; i < scheduleList.getLength(); i++) {
                    Element schElem = (Element) scheduleList.item(i);
                    String schName = schElem.getAttribute("ScheduleName");
                    /* 应该 */
                    Schedule schedule = parseScheduleXml(schElem);
                    if (schedule != null) {
                        schedule.setScheduleName(schName);
                        int cpuId = scheduleCpuIdMap.getOrDefault(schName, 0);
                        schedule.setCoreNumber(cpuId);
                        if (schedule.getPartitionWindows() != null) {
                            for (Schedule.PartitionWindow window : schedule.getPartitionWindows()) {
                                String partitionNameRef = window.getPartitionNameRefAsString();
                                if (partitionNameRef != null && !partitionNameRef.isEmpty()) {
                                    partitionCpuIdMap.put(partitionNameRef, cpuId);
                                }
                            }
                        }
                        module.addSchedule(schName, schedule);
                    }
                    /* parseScheduleXml应该 */
                }

                if (module.getfPartitions() != null && !module.getfPartitions().isEmpty()) {
                    for (java.util.Map.Entry<String, Partition> partitionEntry : module.getfPartitions().entrySet()) {
                        Partition partition = partitionEntry.getValue();
                        if (partition != null) {
                            partition.setfBoundProcessorCore(
                                    partitionCpuIdMap.getOrDefault(partition.getfNameAsString(), 0));
                        }
                    }
                }
            }

            /* Connections */
            NodeList connectionsList = root.getElementsByTagNameNS("*", "Connections");
            if (connectionsList.getLength() > 0) {
                Element connectionsElem = (Element) connectionsList.item(0);
                NodeList channelList = connectionsElem.getElementsByTagNameNS("*", "Channel");
                for (int i = 0; i < channelList.getLength(); i++) {
                    Element channelElem = (Element) channelList.item(i);
                    Channel channel = parseChannelXml(channelElem);
                    if (channel != null) {
                        module.addChannel(channel.getId(), channel);
                    }
                    /* parseChannelXml应该 */
                }
            }

            /* SharedDataRegions 解析 */
            NodeList sharedDataRegionsList = root.getElementsByTagNameNS("*", "SharedDataRegions");
            if (sharedDataRegionsList.getLength() > 0) {
                Element sharedDataRegionsElem = (Element) sharedDataRegionsList.item(0);
                /* 用SharedDataRegions?Size应该 */
                if (sharedDataRegionsElem.hasAttribute("Size")) {
                    module.setfSharedDataRegionsSize(sharedDataRegionsElem.getAttribute("Size"));
                }
                NodeList sharedDataList = sharedDataRegionsElem.getElementsByTagNameNS("*", "SharedData");
                for (int i = 0; i < sharedDataList.getLength(); i++) {
                    Element sharedDataElem = (Element) sharedDataList.item(i);
                    String sharedDataName = sharedDataElem.getAttribute("Name");

                    /* 应该SharedDataDescription */
                    SharedData sharedData = parseSharedDataXml(sharedDataElem, baseDir);
                    if (sharedData != null) {
                        sharedData.setfName(sharedDataName);
                        module.addSharedData(sharedDataName, sharedData);
                    }
                }
            }

            /* Payloads 解析 */
            NodeList payloadsList = root.getElementsByTagNameNS("*", "Payloads");
            if (payloadsList.getLength() > 0) {
                Element payloadsElem = (Element) payloadsList.item(0);
                Payload payload = new Payload();

                /* CoreOSPayload */
                NodeList coreOSPayloadList = payloadsElem.getElementsByTagNameNS("*", "CoreOSPayload");
                if (coreOSPayloadList.getLength() > 0) {
                    Element coreOSPayloadElem = (Element) coreOSPayloadList.item(0);
                    Payload.CoreOSPayload coreOSPayload = new Payload.CoreOSPayload();
                    /* 优先使用 Base_Address属性，兼容 BaseAddress */
                    if (coreOSPayloadElem.hasAttribute("Base_Address")) {
                        coreOSPayload.setBaseAddress(parseLong(coreOSPayloadElem.getAttribute("Base_Address")));
                    } else if (coreOSPayloadElem.hasAttribute("BaseAddress")) {
                        coreOSPayload.setBaseAddress(parseLong(coreOSPayloadElem.getAttribute("BaseAddress")));
                    }
                    if (coreOSPayloadElem.hasAttribute("Size")) {
                        coreOSPayload.setSize(parseLong(coreOSPayloadElem.getAttribute("Size")));
                    }
                    payload.setfCoreOSPayload(coreOSPayload);
                }

                /* SharedLibraryPayload */
                NodeList sharedLibraryPayloadList = payloadsElem.getElementsByTagNameNS("*", "SharedLibraryPayload");
                for (int i = 0; i < sharedLibraryPayloadList.getLength(); i++) {
                    Element slpElem = (Element) sharedLibraryPayloadList.item(i);
                    Payload.SharedLibraryPayload slp = new Payload.SharedLibraryPayload();
                    if (slpElem.hasAttribute("NameRef")) {
                        slp.setfNameRef(slpElem.getAttribute("NameRef"));
                    }
                    /* 优先使用 Base_Address属性，兼容 BaseAddress */
                    if (slpElem.hasAttribute("Base_Address")) {
                        slp.setBaseAddress(parseLong(slpElem.getAttribute("Base_Address")));
                    } else if (slpElem.hasAttribute("BaseAddress")) {
                        slp.setBaseAddress(parseLong(slpElem.getAttribute("BaseAddress")));
                    }
                    if (slpElem.hasAttribute("Size")) {
                        slp.setSize(parseLong(slpElem.getAttribute("Size")));
                    }
                    payload.addSharedLibraryPayload(slp);
                }

                /* ConfigRecordPayload */
                NodeList configRecordPayloadList = payloadsElem.getElementsByTagNameNS("*", "ConfigRecordPayload");
                for (int i = 0; i < configRecordPayloadList.getLength(); i++) {
                    Element crpElem = (Element) configRecordPayloadList.item(i);
                    Payload.ConfigRecordPayload crp = new Payload.ConfigRecordPayload();
                    if (crpElem.hasAttribute("NameRef")) {
                        crp.setfNameRef(crpElem.getAttribute("NameRef"));
                    }
                    /* 优先使用 Base_Address属性，兼容 BaseAddress */
                    if (crpElem.hasAttribute("Base_Address")) {
                        crp.setBaseAddress(parseLong(crpElem.getAttribute("Base_Address")));
                    } else if (crpElem.hasAttribute("BaseAddress")) {
                        crp.setBaseAddress(parseLong(crpElem.getAttribute("BaseAddress")));
                    }
                    if (crpElem.hasAttribute("Size")) {
                        crp.setSize(parseLong(crpElem.getAttribute("Size")));
                    }
                    payload.addConfigRecordPayload(crp);
                }

                /* PartitionPayload */
                NodeList partitionPayloadList = payloadsElem.getElementsByTagNameNS("*", "PartitionPayload");
                for (int i = 0; i < partitionPayloadList.getLength(); i++) {
                    Element ppElem = (Element) partitionPayloadList.item(i);
                    Payload.PartitionPayload pp = new Payload.PartitionPayload();
                    if (ppElem.hasAttribute("NameRef")) {
                        pp.setfNameRef(ppElem.getAttribute("NameRef"));
                    }
                    /* 优先使用 Base_Address属性，兼容 BaseAddress */
                    if (ppElem.hasAttribute("Base_Address")) {
                        pp.setBaseAddress(parseLong(ppElem.getAttribute("Base_Address")));
                    } else if (ppElem.hasAttribute("BaseAddress")) {
                        pp.setBaseAddress(parseLong(ppElem.getAttribute("BaseAddress")));
                    }
                    if (ppElem.hasAttribute("Size")) {
                        pp.setSize(parseLong(ppElem.getAttribute("Size")));
                    }
                    if (ppElem.hasAttribute("Online")) {
                        pp.setOnline(parseBool(ppElem.getAttribute("Online")));
                    }
                    payload.addPartitionPayload(pp);
                }

                module.setfPayload(payload);
            }

            /* PR_07: Module解析成功，read/parse日志在Application.java输出 */
            /* 应该 */
            return module;
        } catch (Throwable e) {
            /* 使用Throwable而非Exception以便捕获OutOfMemoryError等Error类型 */
            String reason = formatXmlErrorMessage(e);
            /* 检查是否为文件读取错误（如权限被拒绝） */
            if (isFileReadError(e)) {
                String fileName = extractFailedReadFileName(e, filePath);
                String errorMsg = "XML file " + fileName + " read failed.";
                Log.error(CodeConstant.ERROR_XML_READ_FAILED, errorMsg);
            } else {
                if (isDuplicateAttributeError(e)) {
                    int line = (e instanceof org.xml.sax.SAXParseException)
                            ? ((org.xml.sax.SAXParseException) e).getLineNumber()
                            : -1;
                    String[] elemAttr = extractElementAndAttributeFromDuplicateMsg(reason);
                    String fileName = new File(filePath).getName();
                    String msg = String.format("[File: %s, Line: %d] XML element %s attribute %s redundancy.",
                            fileName, line, elemAttr[0], elemAttr[1]);
                    Log.error(CodeConstant.ERROR_XML_REDUNDANCY_ATTRIBUTES, msg);                } else {
                    /* TR_PR_015会议决议(2026-08-25): 2001错误码删除，
                     * CPM模块XML文件解析失败与其余XML一致报1017并携带reason */
                    String errorMsg = buildParseFailedMessage(reason);
                    Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
                }
            }
            return null;
        }
    }

    /* Application */
    /**
     * parseApplicationXml
     *
     * @brief
     * 功能: 执行 parseApplicationXml 相关处理。
     *
     * @scope external
     * @errno:
     * N/A
     */
    public static ApplicationEntity parseApplicationXml(String filePath) {
        setCurrentParsingFile(filePath);
        try {
            File file = new File(filePath);
            ensureUtf8XmlEncoding(file);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(SILENT_ERROR_HANDLER);
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();
            ApplicationEntity application = new ApplicationEntity();
            /* MemorySize */
            NodeList memorySizeList = root.getElementsByTagNameNS("*", "MemorySize");
            if (memorySizeList.getLength() > 0) {
                Element e = (Element) memorySizeList.item(0);
                ApplicationEntity.MemorySize ms = new ApplicationEntity.MemorySize();
                ms.setfMemorySizeBss(parseLong(e.getAttribute("MemorySizeBss")));
                ms.setfMemorySizeData(parseLong(e.getAttribute("MemorySizeData")));
                ms.setfMemorySizeRoData(parseLong(e.getAttribute("MemorySizeRoData")));
                ms.setfMemorySizeText(parseLong(e.getAttribute("MemorySizeText")));

                /* MemorySizePersistentData - 应该 */
                if (e.hasAttribute("MemorySizePersistentData")) {
                    ms.setfMemorySizePersistentData(parseLong(e.getAttribute("MemorySizePersistentData")));
                    ms.setHasPersistentData(true);
                }

                /* MemorySizePersistentBss - 应该 */
                if (e.hasAttribute("MemorySizePersistentBss")) {
                    ms.setfMemorySizePersistentBss(parseLong(e.getAttribute("MemorySizePersistentBss")));
                    ms.setHasPersistentBss(true);
                }

                application.setfMemorySize(ms);
            }
            /* Ports */
            NodeList portsList = root.getElementsByTagNameNS("*", "Ports");
            if (portsList.getLength() > 0) {
                Element portsElem = (Element) portsList.item(0);
                /* QueuingPort */
                NodeList queuingPortList = portsElem.getElementsByTagNameNS("*", "QueuingPort");
                for (int i = 0; i < queuingPortList.getLength(); i++) {
                    Element qElem = (Element) queuingPortList.item(i);
                    QueuingPort qp = new QueuingPort();
                    qp.setfName(qElem.getAttribute("Name"));
                    qp.setfDirection(qElem.getAttribute("Direction"));
                    if (qElem.hasAttribute("Attribute")) {
                        qp.setfAttribute(qElem.getAttribute("Attribute"));
                    }
                    qp.setfMaxMessageSize(parseInt(qElem.getAttribute("MaxMessageSize")));
                    qp.setfExtendedPort(qElem.getAttribute("ExtendedPort"));
                    qp.setfMaxNbMessage(parseInt(qElem.getAttribute("MaxNbMessage")));
                    qp.setfProtocolType(qElem.getAttribute("ProtocolType"));
                    qp.setfDriverName(qElem.getAttribute("DriverName"));
                    application.addQueuingPort(qp);
                }
                /* SamplingPort */
                NodeList samplingPortList = portsElem.getElementsByTagNameNS("*", "SamplingPort");
                for (int i = 0; i < samplingPortList.getLength(); i++) {
                    Element sElem = (Element) samplingPortList.item(i);
                    SamplingPort sp = new SamplingPort();
                    sp.setfName(sElem.getAttribute("Name"));
                    sp.setfDirection(sElem.getAttribute("Direction"));
                    if (sElem.hasAttribute("Attribute")) {
                        sp.setfAttribute(sElem.getAttribute("Attribute"));
                    }
                    sp.setfMaxMessageSize(parseInt(sElem.getAttribute("MaxMessageSize")));
                    sp.setfRefreshRate(sElem.getAttribute("RefreshRate"));
                    sp.setfDriverName(sElem.getAttribute("DriverName"));
                    application.addSamplingPort(sp);
                }
            }

            /* PR_07: 打印解析XML文件成功 */
            Log.info("XML file parsed successfully. File Path: " + getCleanPath(file));
            return application;
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            /* 检查是否为文件读取错误（如权限被拒绝） */
            if (isFileReadError(e)) {
                String fileName = extractFailedReadFileName(e, filePath);
                String errorMsg = "XML file " + fileName + " read failed.";
                Log.error(CodeConstant.ERROR_XML_READ_FAILED, errorMsg);
            } else {
                String errorMsg = buildParseFailedMessage(reason);
                Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            }
            return null;
        }
    }

    /* Partition */
    /**
     * parsePartitionXml
     *
     * @brief
     * 功能: 执行 parsePartitionXml 相关处理。
     *
     * @scope external
     * @errno:
     * N/A
     */
    public static Partition parsePartitionXml(String filePath) {
        setCurrentParsingFile(filePath);
        try {
            File file = new File(filePath);
            ensureUtf8XmlEncoding(file);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(SILENT_ERROR_HANDLER);
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();
            Partition partition = new Partition();
            /* Application NameRef */
            NodeList applicationList = root.getElementsByTagNameNS("*", "Application");
            if (applicationList.getLength() > 0) {
                Element appElem = (Element) applicationList.item(0);
                partition.setfApplicationNameRef(appElem.getAttribute("NameRef"));
            }
            /* SharedLibraryRegion NameRef */
            NodeList sharedLibraryList = root.getElementsByTagNameNS("*", "SharedLibraryRegion");
            for (int i = 0; i < sharedLibraryList.getLength(); i++) {
                Element libElem = (Element) sharedLibraryList.item(i);
                partition.addSharedLibraryNameRef(libElem.getAttribute("NameRef"));
            }
            /* Settings */
            NodeList settingsList = root.getElementsByTagNameNS("*", "Settings");
            if (settingsList.getLength() > 0) {
                Element sElem = (Element) settingsList.item(0);
                Partition.Settings settings = new Partition.Settings();

                settings.setfRequiredMemorySize(parseLong(sElem.getAttribute("RequiredMemorySize")));
                /* 旧版XML用maxGlobalFDs属性默认20 */
                String maxGlobalFDsAttr = sElem.getAttribute("maxGlobalFDs");
                if (maxGlobalFDsAttr != null && !maxGlobalFDsAttr.isEmpty()) {
                    settings.setfMaxGlobalFDs(parseInt(maxGlobalFDsAttr));
                }
                settings.setfSystemPartition(parseBool(sElem.getAttribute("systemPartition")));
                /* 旧版XML用watchDogDuration应该1L */
                String watchDogDurationAttr = sElem.getAttribute("watchDogDuration");
                if (watchDogDurationAttr != null && !watchDogDurationAttr.isEmpty()) {
                    settings.setfWatchDogDuration(parseLong(watchDogDurationAttr));
                }
                /* 旧版XML用period应该2L */
                String periodAttr = sElem.getAttribute("period");
                if (periodAttr != null && !periodAttr.isEmpty()) {
                    settings.setfPeriod(parseLong(periodAttr));
                }
                /* 旧版XML用duration应该3L */
                String durationAttr = sElem.getAttribute("duration");
                if (durationAttr != null && !durationAttr.isEmpty()) {
                    settings.setfDuration(parseLong(durationAttr));
                }
                /* 旧版XML用numWorkerTasks应该0 */
                String numWorkerTasksAttr = sElem.getAttribute("numWorkerTasks");
                if (numWorkerTasksAttr != null && !numWorkerTasksAttr.isEmpty()) {
                    settings.setfNumWorkerTasks(parseInt(numWorkerTasksAttr));
                }
                /* 旧版XML用syscallPermissions应该0xFFFFFFFF */
                String syscallPermissionsAttr = sElem.getAttribute("syscallPermissions");
                if (syscallPermissionsAttr != null && !syscallPermissionsAttr.isEmpty()) {
                    settings.setfSyscallPermissions(parseLong(syscallPermissionsAttr));
                }
                /* 旧版XML用numFiles应该0 */
                String numFilesAttr = sElem.getAttribute("numFiles");
                if (numFilesAttr != null && !numFilesAttr.isEmpty()) {
                    settings.setfNumFiles(parseInt(numFilesAttr));
                }
                /* 旧版XML用numDrivers应该0 */
                String numDriversAttr = sElem.getAttribute("numDrivers");
                if (numDriversAttr != null && !numDriversAttr.isEmpty()) {
                    settings.setfNumDrivers(parseInt(numDriversAttr));
                }
                /* 旧版XML用numLogMsgs应该0 */
                String numLogMsgsAttr = sElem.getAttribute("numLogMsgs");
                if (numLogMsgsAttr != null && !numLogMsgsAttr.isEmpty()) {
                    settings.setfNumLogMsgs(parseInt(numLogMsgsAttr));
                }
                /* 旧版XML用numStackGuardPages应该0 */
                String numStackGuardPagesAttr = sElem.getAttribute("numStackGuardPages");
                if (numStackGuardPagesAttr != null && !numStackGuardPagesAttr.isEmpty()) {
                    settings.setfNumStackGuardPages(parseInt(numStackGuardPagesAttr));
                }
                /* 旧版XML用initProcStackSize应该0x4000 */
                String initProcStackSizeAttr = sElem.getAttribute("initProcStackSize");
                if (initProcStackSizeAttr != null && !initProcStackSizeAttr.isEmpty()) {
                    settings.setfInitProcStackSize(parseLong(initProcStackSizeAttr));
                }
                /* 旧版XML用isrStackSize应该0x2000 */
                String isrStackSizeAttr = sElem.getAttribute("isrStackSize");
                if (isrStackSizeAttr != null && !isrStackSizeAttr.isEmpty()) {
                    settings.setfIsrStackSize(parseLong(isrStackSizeAttr));
                }
                /* selSvrQSize解析0 */
                String selSvrQSizeAttr = sElem.getAttribute("selSvrQSize");
                if (selSvrQSizeAttr != null && !selSvrQSizeAttr.isEmpty()) {
                    settings.setfSelSvrQSize(parseLong(selSvrQSizeAttr));
                }
                /* allocDisable解析true解析? */
                String allocDisableAttr = sElem.getAttribute("allocDisable");
                if (allocDisableAttr != null && !allocDisableAttr.isEmpty()) {
                    settings.setfAllocDisable(parseBool(allocDisableAttr));
                }
                /* 旧版XML用maxEventQStallDuration应该-1 */
                String maxEventQStallDurationAttr = sElem.getAttribute("maxEventQStallDuration");
                if (maxEventQStallDurationAttr != null && !maxEventQStallDurationAttr.isEmpty()) {
                    settings.setfMaxEventQStallDuration(parseLong(maxEventQStallDurationAttr));
                }
                /* fpExcEnable解析true解析? */
                String fpExcEnableAttr = sElem.getAttribute("fpExcEnable");
                if (fpExcEnableAttr != null && !fpExcEnableAttr.isEmpty()) {
                    settings.setfFpExcEnable(parseBool(fpExcEnableAttr));
                }
                /* appsIdleRelinquishEnabled解析false应该 */
                String appsIdleRelinquishEnabledAttr = sElem.getAttribute("appsIdleRelinquishEnabled");
                if (appsIdleRelinquishEnabledAttr != null && !appsIdleRelinquishEnabledAttr.isEmpty()) {
                    settings.setfAppsIdleRelinquishEnabled(parseBool(appsIdleRelinquishEnabledAttr));
                }
                /* 旧版XML用appsPriority-1 */
                String appsPriorityAttr = sElem.getAttribute("appsPriority");
                if (appsPriorityAttr != null && !appsPriorityAttr.isEmpty()) {
                    settings.setfAppsPriority(parseInt(appsPriorityAttr));
                }
                /* setModuleSchedule解析false应该 */
                String setModuleScheduleAttr = sElem.getAttribute("setModuleSchedule");
                if (setModuleScheduleAttr != null && !setModuleScheduleAttr.isEmpty()) {
                    settings.setfSetModuleSchedule(parseBool(setModuleScheduleAttr));
                }
                /* scheduleChangeAction解析"IGNORE"解析? */
                String scheduleChangeActionAttr = sElem.getAttribute("scheduleChangeAction");
                if (scheduleChangeActionAttr != null && !scheduleChangeActionAttr.isEmpty()) {
                    settings.setfScheduleChangeAction(scheduleChangeActionAttr);
                }
                /* objResSpaceSize解析0应该 */
                String objResSpaceSizeAttr = sElem.getAttribute("objResSpaceSize");
                if (objResSpaceSizeAttr != null && !objResSpaceSizeAttr.isEmpty()) {
                    settings.setfObjResSpaceSize(parseLong(objResSpaceSizeAttr));
                }
                /* 旧版XML用taskExpStackSize应该0x2000 */
                String taskExpStackSizeAttr = sElem.getAttribute("taskExpStackSize");
                if (taskExpStackSizeAttr != null && !taskExpStackSizeAttr.isEmpty()) {
                    settings.setfTaskExpStackSize(parseLong(taskExpStackSizeAttr));
                }
                /* 旧版XML用idleTaskStackSize应该0x2000 */
                String idleTaskStackSizeAttr = sElem.getAttribute("idleTaskStackSize");
                if (idleTaskStackSizeAttr != null && !idleTaskStackSizeAttr.isEmpty()) {
                    settings.setfIdleTaskStackSize(parseLong(idleTaskStackSizeAttr));
                }
                /* 旧版XML用initTaskStackSize应该0x4000 */
                String initTaskStackSizeAttr = sElem.getAttribute("initTaskStackSize");
                if (initTaskStackSizeAttr != null && !initTaskStackSizeAttr.isEmpty()) {
                    settings.setfInitTaskStackSize(parseLong(initTaskStackSizeAttr));
                }
                /* 旧版XML用initTaskPriority应该10 */
                String initTaskPriorityAttr = sElem.getAttribute("initTaskPriority");
                if (initTaskPriorityAttr != null && !initTaskPriorityAttr.isEmpty()) {
                    settings.setfInitTaskPriority(parseInt(initTaskPriorityAttr));
                }
                /* defaultAppName应该 */
                String defaultAppNameAttr = sElem.getAttribute("defaultAppName");
                if (defaultAppNameAttr != null && !defaultAppNameAttr.isEmpty()) {
                    settings.setfDefaultAppName(defaultAppNameAttr);
                }
                /* user1解析0应该 */
                String user1Attr = sElem.getAttribute("user1");
                if (user1Attr != null && !user1Attr.isEmpty()) {
                    settings.setfUserExtension1(parseInt(user1Attr));
                }
                /* user2解析0应该 */
                String user2Attr = sElem.getAttribute("user2");
                if (user2Attr != null && !user2Attr.isEmpty()) {
                    settings.setfUserExtension2(parseInt(user2Attr));
                }

                partition.setfSettings(settings);
            }

            /* PseudoPartitionDescription ? Ports 用 */
            NodeList pseudoDescList = root.getElementsByTagNameNS("*", "PseudoPartitionDescription");
            if (pseudoDescList.getLength() > 0) {
                /* 解析分区用 */
                partition.setfIsPseudoPartition(true);
                Element pseudoDescElem = (Element) pseudoDescList.item(0);

                /* 应该 Ports */
                NodeList portsList = pseudoDescElem.getElementsByTagNameNS("*", "Ports");
                if (portsList.getLength() > 0) {
                    Element portsElem = (Element) portsList.item(0);
                    parsePartitionPorts(portsElem, partition);
                }
            } else {
                /* 应该 */
                NodeList portsList = root.getElementsByTagNameNS("*", "Ports");
                if (portsList.getLength() > 0) {
                    Element portsElem = (Element) portsList.item(0);
                    parsePartitionPorts(portsElem, partition);
                }
            }

            /* 用SharedDataRegion */
            NodeList sharedDataRegionList = root.getElementsByTagNameNS("*", "SharedDataRegion");
            for (int i = 0; i < sharedDataRegionList.getLength(); i++) {
                Element sdrElem = (Element) sharedDataRegionList.item(i);
                SharedDataRegion sdr = new SharedDataRegion();
                sdr.setfName(sdrElem.getAttribute("Name"));
                sdr.setfAccessRights(sdrElem.getAttribute("AccessRights"));
                sdr.setfAddress(parseLong(sdrElem.getAttribute("Address")));
                sdr.setfSize(parseLong(sdrElem.getAttribute("Size")));
                partition.addSharedDataRegion(sdr);
            }

            /* PR_07: 打印解析XML文件成功 */
            Log.info("XML file parsed successfully. File Path: " + getCleanPath(file));
            return partition;
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            /* 检查是否为文件读取错误（如权限被拒绝） */
            if (isFileReadError(e)) {
                String fileName = extractFailedReadFileName(e, filePath);
                String errorMsg = "XML file " + fileName + " read failed.";
                Log.error(CodeConstant.ERROR_XML_READ_FAILED, errorMsg);
            } else {
                String errorMsg = buildParseFailedMessage(reason);
                Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            }
            return null;
        }
    }

    /**
     * 应该兼容处理PseudoPartition?XML用
     * 应该 PseudoPartitionDescription ? Ports
     * 
     * @param pseudoPartElem PseudoPartition XML用
     * @return Partition应该
     */
    public static Partition parsePseudoPartitionXml(Element pseudoPartElem) {
        try {
            Partition partition = new Partition();
            partition.setfIsPseudoPartition(true);

            /* PseudoPartitionDescription */
            NodeList pseudoDescList = pseudoPartElem.getElementsByTagNameNS("*", "PseudoPartitionDescription");
            if (pseudoDescList.getLength() > 0) {
                Element pseudoDescElem = (Element) pseudoDescList.item(0);

                /* 应该 Ports */
                NodeList portsList = pseudoDescElem.getElementsByTagNameNS("*", "Ports");
                if (portsList.getLength() > 0) {
                    Element portsElem = (Element) portsList.item(0);
                    parsePartitionPorts(portsElem, partition);
                }
            }

            return partition;
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            String errorMsg = buildParseFailedMessage(reason);
            Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            return null;
        }
    }

    /* SharedLibrary */
    /**
     * parseSharedLibraryXml
     *
     * @brief
     * 功能: 执行 parseSharedLibraryXml 相关处理。
     *
     * @scope external
     * @errno:
     * N/A
     */
    public static SharedLibrary parseSharedLibraryXml(String filePath) {
        setCurrentParsingFile(filePath);
        try {
            File file = new File(filePath);
            ensureUtf8XmlEncoding(file);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(SILENT_ERROR_HANDLER);
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();
            SharedLibrary sharedLibrary = new SharedLibrary();
            if (root.hasAttribute("SystemSharedLibrary")) {
                sharedLibrary.setfSystemSharedLibrary(parseBool(root.getAttribute("SystemSharedLibrary")));
            }
            if (root.hasAttribute("VirtualAddress")) {
                sharedLibrary.setfVirtualAddress(parseLong(root.getAttribute("VirtualAddress")));
            }
            /* MemorySize */
            NodeList memorySizeList = root.getElementsByTagNameNS("*", "MemorySize");
            if (memorySizeList.getLength() > 0) {
                Element e = (Element) memorySizeList.item(0);
                SharedLibrary.MemorySize ms = new SharedLibrary.MemorySize();
                ms.setfMemorySizeText(e.getAttribute("MemorySizeText"));
                ms.setfMemorySizeRoData(e.getAttribute("MemorySizeRoData"));
                ms.setfMemorySizeData(e.getAttribute("MemorySizeData"));
                ms.setfMemorySizeBss(e.getAttribute("MemorySizeBss"));

                /* MemorySizePersistentData - 应该 */
                if (e.hasAttribute("MemorySizePersistentData")) {
                    ms.setfMemorySizePersistentData(e.getAttribute("MemorySizePersistentData"));
                }

                /* MemorySizePersistentBss - 应该 */
                if (e.hasAttribute("MemorySizePersistentBss")) {
                    ms.setfMemorySizePersistentBss(e.getAttribute("MemorySizePersistentBss"));
                }

                sharedLibrary.setfMemorySize(ms);
            }

            /* PR_07: 打印解析XML文件成功 */
            Log.info("shared lib cfg XML file parsed successfully. File Path: " + getCleanPath(file));
            return sharedLibrary;
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            /* 检查是否为文件读取错误（如权限被拒绝） */
            if (isFileReadError(e)) {
                String fileName = extractFailedReadFileName(e, filePath);
                String errorMsg = "XML file " + fileName + " read failed.";
                Log.error(CodeConstant.ERROR_XML_READ_FAILED, errorMsg);
            } else {
                String errorMsg = buildParseFailedMessage(reason);
                Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            }
            return null;
        }
    }

    /* Schedule */
    /**
     * 应该 Schedule
     */
    public static Schedule parseScheduleXml(String filePath) {
        setCurrentParsingFile(filePath);
        try {
            File file = new File(filePath);
            ensureUtf8XmlEncoding(file);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(SILENT_ERROR_HANDLER);
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();
            Element root = document.getDocumentElement();
            return parseScheduleXml(root);
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            /* 检查是否为文件读取错误（如权限被拒绝） */
            if (isFileReadError(e)) {
                String fileName = extractFailedReadFileName(e, filePath);
                String errorMsg = "XML file " + fileName + " read failed.";
                Log.error(CodeConstant.ERROR_XML_READ_FAILED, errorMsg);
            } else {
                String errorMsg = buildParseFailedMessage(reason);
                Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            }
            return null;
        }
    }

    /**
     * 解析? Schedule 解析
     */
    public static Schedule parseScheduleXml(Element root) {
        try {
            Schedule schedule = new Schedule();
            /* ScheduleIdentifier */
            if (root.hasAttribute("ScheduleIdentifier")) {
                schedule.setScheduleIdentifier(parseInt(root.getAttribute("ScheduleIdentifier")));
            }
            /* ScheduleName */
            if (root.hasAttribute("ScheduleName")) {
                schedule.setScheduleName(root.getAttribute("ScheduleName"));
            }
            /* InitialModuleSchedule */
            if (root.hasAttribute("InitialModuleSchedule")) {
                schedule.setInitialModuleSchedule(parseBool(root.getAttribute("InitialModuleSchedule")));
            }
            /* PartitionWindows */
            NodeList windowList = root.getElementsByTagNameNS("*", "PartitionWindow");
            for (int i = 0; i < windowList.getLength(); i++) {
                Element winElem = (Element) windowList.item(i);
                Schedule.PartitionWindow window = new Schedule.PartitionWindow();
                if (winElem.hasAttribute("PartitionNameRef")) {
                    window.setPartitionNameRef(winElem.getAttribute("PartitionNameRef"));
                }
                if (winElem.hasAttribute("Duration")) {
                    window.setDuration(parseLong(winElem.getAttribute("Duration")));
                }
                if (winElem.hasAttribute("PeriodicProcessingStart")) {
                    window.setPeriodicProcessingStart(parseBool(winElem.getAttribute("PeriodicProcessingStart")));
                }
                schedule.addPartitionWindow(window);
            }
            return schedule;
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            String errorMsg = buildParseFailedMessage(reason);
            Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            return null;
        }
    }

    /* Channel */
    /**
     * 用Channel XML用
     * 
     * @param channelElem Channel XML用
     * @return Channel用
     */
    public static Channel parseChannelXml(Element channelElem) {
        try {
            Channel channel = new Channel();

            /* Channel Id */
            if (channelElem.hasAttribute("Id")) {
                channel.setId(parseInt(channelElem.getAttribute("Id")));
            }

            /* 解析分区 Source 用 */
            NodeList sourceList = channelElem.getElementsByTagNameNS("*", "Source");
            for (int i = 0; i < sourceList.getLength(); i++) {
                Element sourceElem = (Element) sourceList.item(i);
                Channel.ChannelEndpoint source = new Channel.ChannelEndpoint();
                if (sourceElem.hasAttribute("PartitionNameRef")) {
                    source.setPartitionNameRef(sourceElem.getAttribute("PartitionNameRef"));
                }
                if (sourceElem.hasAttribute("PortNameRef")) {
                    source.setPortNameRef(sourceElem.getAttribute("PortNameRef"));
                }
                channel.addSource(source);
            }

            /* 解析分区 Destination 用 */
            NodeList destinationList = channelElem.getElementsByTagNameNS("*", "Destination");
            for (int i = 0; i < destinationList.getLength(); i++) {
                Element destinationElem = (Element) destinationList.item(i);
                Channel.ChannelEndpoint destination = new Channel.ChannelEndpoint();
                if (destinationElem.hasAttribute("PartitionNameRef")) {
                    destination.setPartitionNameRef(destinationElem.getAttribute("PartitionNameRef"));
                }
                if (destinationElem.hasAttribute("PortNameRef")) {
                    destination.setPortNameRef(destinationElem.getAttribute("PortNameRef"));
                }
                channel.addDestination(destination);
            }

            return channel;
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            String errorMsg = buildParseFailedMessage(reason);
            Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            return null;
        }
    }

    /* CoreHMTable */
    /**
     * 用CoreHMTable XML用
     * 
     * @param coreHMTableElem CoreHMTable XML用
     * @return CoreHMTable用
     */
    public static CoreHMTable parseCoreHMTableXml(Element coreHMTableElem) {
        try {
            CoreHMTable coreHMTable = new CoreHMTable();

            /* Name */
            if (coreHMTableElem.hasAttribute("Name")) {
                coreHMTable.setName(coreHMTableElem.getAttribute("Name"));
            }

            /* Description */
            if (coreHMTableElem.hasAttribute("Description")) {
                coreHMTable.setDescription(coreHMTableElem.getAttribute("Description"));
            }

            /* StateIdentifier */
            if (coreHMTableElem.hasAttribute("StateIdentifier")) {
                coreHMTable.setStateIdentifier(parseInt(coreHMTableElem.getAttribute("StateIdentifier")));
            }

            /* ErrorActions */
            NodeList errorActionList = coreHMTableElem.getElementsByTagNameNS("*", "ErrorAction");
            for (int i = 0; i < errorActionList.getLength(); i++) {
                Element actionElem = (Element) errorActionList.item(i);
                CoreHMTable.ErrorAction errorAction = new CoreHMTable.ErrorAction();

                if (actionElem.hasAttribute("ModuleRecoveryAction")) {
                    errorAction.setModuleRecoveryAction(actionElem.getAttribute("ModuleRecoveryAction"));
                }
                if (actionElem.hasAttribute("ErrorIdentifierRef")) {
                    errorAction.setErrorIdentifierRef(parseInt(actionElem.getAttribute("ErrorIdentifierRef")));
                }

                coreHMTable.addErrorAction(errorAction);
            }

            NodeList coreList = coreHMTableElem.getElementsByTagNameNS("*", "Core");
            for (int i = 0; i < coreList.getLength(); i++) {
                Element coreElem = (Element) coreList.item(i);
                if (coreElem.hasAttribute("cpuId")) {
                    coreHMTable.addCoreRefId(parseLong(coreElem.getAttribute("cpuId")));
                }
            }

            return coreHMTable;
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            String errorMsg = buildParseFailedMessage(reason);
            Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            return null;
        }
    }

    /* MultiPartitionHMTable */
    /**
     * 用MultiPartitionHMTable XML用
     * 
     * @param multiPartitionHMTableElem MultiPartitionHMTable XML用
     * @return MultiPartitionHMTable用
     */
    public static MultiPartitionHMTable parseMultiPartitionHMTableXml(Element multiPartitionHMTableElem) {
        try {
            MultiPartitionHMTable multiPartitionHMTable = new MultiPartitionHMTable();

            /* Name (用XML用TableName用) */
            if (multiPartitionHMTableElem.hasAttribute("TableName")) {
                multiPartitionHMTable.setName(multiPartitionHMTableElem.getAttribute("TableName"));
            }

            /* StateIdentifier (用XML用TableIdentifier用) */
            if (multiPartitionHMTableElem.hasAttribute("TableIdentifier")) {
                multiPartitionHMTable
                        .setStateIdentifier(parseInt(multiPartitionHMTableElem.getAttribute("TableIdentifier")));
            }

            /* Description (解析?) */
            if (multiPartitionHMTableElem.hasAttribute("Description")) {
                multiPartitionHMTable.setDescription(multiPartitionHMTableElem.getAttribute("Description"));
            }

            /* ErrorActions */
            NodeList errorActionList = multiPartitionHMTableElem.getElementsByTagNameNS("*", "ErrorAction");
            for (int i = 0; i < errorActionList.getLength(); i++) {
                Element actionElem = (Element) errorActionList.item(i);
                MultiPartitionHMTable.ErrorAction errorAction = new MultiPartitionHMTable.ErrorAction();

                if (actionElem.hasAttribute("ModuleRecoveryAction")) {
                    errorAction.setModuleRecoveryAction(actionElem.getAttribute("ModuleRecoveryAction"));
                }
                if (actionElem.hasAttribute("ErrorIdentifierRef")) {
                    errorAction.setErrorIdentifierRef(parseInt(actionElem.getAttribute("ErrorIdentifierRef")));
                }
                if (actionElem.hasAttribute("ErrorLevel")) {
                    errorAction.setErrorLevel(actionElem.getAttribute("ErrorLevel"));
                }

                multiPartitionHMTable.addErrorAction(errorAction);
            }

            return multiPartitionHMTable;
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            String errorMsg = buildParseFailedMessage(reason);
            Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            return null;
        }
    }

    /* PartitionHMTable */
    /**
     * 用PartitionHMTable XML用
     * 
     * @param partitionHMTableElem PartitionHMTable XML用
     * @return PartitionHMTable用
     */
    public static PartitionHMTable parsePartitionHMTableXml(Element partitionHMTableElem) {
        try {
            PartitionHMTable partitionHMTable = new PartitionHMTable();

            /* MultiPartitionHMTableNameRef */
            if (partitionHMTableElem.hasAttribute("MultiPartitionHMTableNameRef")) {
                partitionHMTable.setMultiPartitionHMTableNameRef(
                        partitionHMTableElem.getAttribute("MultiPartitionHMTableNameRef"));
            }

            /* TableIdentifier */
            if (partitionHMTableElem.hasAttribute("TableIdentifier")) {
                partitionHMTable.setTableIdentifier(parseInt(partitionHMTableElem.getAttribute("TableIdentifier")));
            }

            /* TableName */
            if (partitionHMTableElem.hasAttribute("TableName")) {
                partitionHMTable.setTableName(partitionHMTableElem.getAttribute("TableName"));
            }

            /* ErrorActions */
            NodeList errorActionList = partitionHMTableElem.getElementsByTagNameNS("*", "ErrorAction");
            for (int i = 0; i < errorActionList.getLength(); i++) {
                Element actionElem = (Element) errorActionList.item(i);
                PartitionHMTable.ErrorAction errorAction = new PartitionHMTable.ErrorAction();

                if (actionElem.hasAttribute("ErrorIdentifierRef")) {
                    errorAction.setErrorIdentifierRef(parseInt(actionElem.getAttribute("ErrorIdentifierRef")));
                }
                if (actionElem.hasAttribute("PartitionRecoveryAction")) {
                    errorAction.setPartitionRecoveryAction(actionElem.getAttribute("PartitionRecoveryAction"));
                }
                if (actionElem.hasAttribute("ErrorLevel")) {
                    errorAction.setErrorLevel(actionElem.getAttribute("ErrorLevel"));
                }

                partitionHMTable.addErrorAction(errorAction);
            }

            /* PartitionRefs */
            NodeList partitionRefList = partitionHMTableElem.getElementsByTagNameNS("*", "PartitionRef");
            for (int i = 0; i < partitionRefList.getLength(); i++) {
                Element refElem = (Element) partitionRefList.item(i);
                PartitionHMTable.PartitionRef partitionRef = new PartitionHMTable.PartitionRef();

                if (refElem.hasAttribute("Id")) {
                    partitionRef.setId(parseInt(refElem.getAttribute("Id")));
                }

                partitionHMTable.addPartitionRef(partitionRef);
            }

            return partitionHMTable;
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            String errorMsg = buildParseFailedMessage(reason);
            Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            return null;
        }
    }

    /* 解析 */
    /**
     * 
     * 
     * @param portsElem Ports XML用
     * @param partition 应该兼容处理
     */
    private static void parsePartitionPorts(Element portsElem, Partition partition) {
        try {
            /* QueuingPort */
            NodeList queuingPortList = portsElem.getElementsByTagNameNS("*", "QueuingPort");
            for (int i = 0; i < queuingPortList.getLength(); i++) {
                Element qElem = (Element) queuingPortList.item(i);
                QueuingPort qp = new QueuingPort();
                qp.setfName(qElem.getAttribute("Name"));
                qp.setfDirection(qElem.getAttribute("Direction"));
                qp.setfAttribute(qElem.hasAttribute("Attribute")
                        ? qElem.getAttribute("Attribute") : "PSEUDO_PORT");
                qp.setfMaxMessageSize(parseInt(qElem.getAttribute("MaxMessageSize")));
                qp.setfExtendedPort(qElem.getAttribute("ExtendedPort"));
                qp.setfMaxNbMessage(parseInt(qElem.getAttribute("MaxNbMessage")));
                qp.setfProtocolType(qElem.getAttribute("ProtocolType"));
                qp.setfDriverName(qElem.getAttribute("DriverName"));
                partition.addQueuingPort(qp);
            }

            /* SamplingPort */
            NodeList samplingPortList = portsElem.getElementsByTagNameNS("*", "SamplingPort");
            for (int i = 0; i < samplingPortList.getLength(); i++) {
                Element sElem = (Element) samplingPortList.item(i);
                SamplingPort sp = new SamplingPort();
                sp.setfName(sElem.getAttribute("Name"));
                sp.setfDirection(sElem.getAttribute("Direction"));
                sp.setfAttribute(sElem.hasAttribute("Attribute")
                        ? sElem.getAttribute("Attribute") : "PSEUDO_PORT");
                sp.setfMaxMessageSize(parseInt(sElem.getAttribute("MaxMessageSize")));
                sp.setfRefreshRate(sElem.getAttribute("RefreshRate"));
                sp.setfDriverName(sElem.getAttribute("DriverName"));
                partition.addSamplingPort(sp);
            }
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            String errorMsg = buildParseFailedMessage(reason);
            Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
        }
    }

    /**
     * 用SharedData XML
     * 用SharedDataDescription?SharedIODescription解析?
     */
    public static SharedData parseSharedDataXml(Element sharedDataElem, String baseDir) {
        try {
            SharedData sharedData = new SharedData();

            /* 解析分区?Include用 */
            String includeFile = sharedDataElem.getAttribute("Include");

            /* 解析分区? xi:include 属性 */
            NodeList xiIncludeList = sharedDataElem.getElementsByTagNameNS("http://www.w3.org/2001/XInclude",
                    "include");

            if (!includeFile.isEmpty()) {
                /* 用1属性Include应该 */
                String sharedDataFilePath = baseDir + "\\" + includeFile;
                File sharedDataFile = new File(sharedDataFilePath);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(SILENT_ERROR_HANDLER);
                Document doc = builder.parse(sharedDataFile);
                Element root = doc.getDocumentElement();

                /* 解析分区SharedDataDescription */
                NodeList dataDescList = root.getElementsByTagNameNS("*", "SharedDataDescription");
                if (dataDescList.getLength() > 0) {
                    Element descElem = (Element) dataDescList.item(0);
                    parseSharedDataDescription(descElem, sharedData);
                }

                /* 用SharedIODescription应该兼容处理 */
                NodeList ioDescList = root.getElementsByTagNameNS("*", "SharedIODescription");
                if (ioDescList.getLength() > 0) {
                    Element ioDescElem = (Element) ioDescList.item(0);
                    parseSharedIODescription(ioDescElem, sharedData);
                }

                /* PR_02: 打印SharedData XML文件解析成功（问题报告单2） */
                Log.info("shared data cfg XML file parsed successfully. File Path: " + getCleanPath(sharedDataFile));
            } else if (xiIncludeList.getLength() > 0) {
                /* 用2属性 xi:include 应该 */
                Element xiIncludeElem = (Element) xiIncludeList.item(0);
                String href = xiIncludeElem.getAttribute("href");
                if (!href.isEmpty()) {
                    /* 解析环境变量 */
                    String resolvedHref = resolveEnvVariables(href);
                    File sharedDataFile;
                    /* 如果解析后的路径是绝对路径，直接使用；否则相对于baseDir */
                    if (new File(resolvedHref).isAbsolute()) {
                        sharedDataFile = new File(resolvedHref);
                    } else {
                        sharedDataFile = new File(baseDir, resolvedHref);
                    }
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    builder.setErrorHandler(SILENT_ERROR_HANDLER);
                    Document doc = builder.parse(sharedDataFile);
                    Element root = doc.getDocumentElement();

                    /* 应该 SharedDataDescription ? SharedIODescription */
                    if (root.getLocalName().equals("SharedDataDescription")) {
                        parseSharedDataDescription(root, sharedData);
                    } else if (root.getLocalName().equals("SharedIODescription")) {
                        parseSharedIODescription(root, sharedData);
                    } else {
                        /* 应该 */
                        NodeList dataDescList = root.getElementsByTagNameNS("*", "SharedDataDescription");
                        if (dataDescList.getLength() > 0) {
                            Element descElem = (Element) dataDescList.item(0);
                            parseSharedDataDescription(descElem, sharedData);
                        }

                        NodeList ioDescList = root.getElementsByTagNameNS("*", "SharedIODescription");
                        if (ioDescList.getLength() > 0) {
                            Element ioDescElem = (Element) ioDescList.item(0);
                            parseSharedIODescription(ioDescElem, sharedData);
                        }
                    }

                    /* PR_02: 打印SharedData XML文件解析成功（问题报告单2） */
                    Log.info(
                            "shared data cfg XML file parsed successfully. File Path: " + getCleanPath(sharedDataFile));
                }
            } else {
                /* 用3应该兼容处理SharedDataDescription */
                NodeList dataDescList = sharedDataElem.getElementsByTagNameNS("*", "SharedDataDescription");
                if (dataDescList.getLength() > 0) {
                    Element descElem = (Element) dataDescList.item(0);
                    parseSharedDataDescription(descElem, sharedData);
                }

                /* 解析分区?SharedIODescription应该兼容处理 */
                NodeList ioDescList = sharedDataElem.getElementsByTagNameNS("*", "SharedIODescription");
                if (ioDescList.getLength() > 0) {
                    Element ioDescElem = (Element) ioDescList.item(0);
                    parseSharedIODescription(ioDescElem, sharedData);
                }
            }

            return sharedData;
        } catch (Exception e) {
            String reason = formatXmlErrorMessage(e);
            String errorMsg = buildParseFailedMessage(reason);
            Log.error(CodeConstant.ERROR_XML_PARSE_FAILED, errorMsg);
            return null;
        }
    }

    /**
     * 用SharedDataDescription用
     * 应该兼容处理SharedDataDescription用
     */
    private static void parseSharedDataDescription(Element descElem, SharedData sharedData) {
        SharedData.SharedDataDescription description = new SharedData.SharedDataDescription();

        /* 用CachePolicy应该兼容处理 */
        if (descElem.hasAttribute("CachePolicy")) {
            description.setfCachePolicy(descElem.getAttribute("CachePolicy"));
        }

        /* 用Size应该兼容处理 */
        if (descElem.hasAttribute("Size")) {
            description.setfSize(descElem.getAttribute("Size"));
        }

        /* 用DataType应该兼容处理 */
        if (descElem.hasAttribute("DataType")) {
            description.setfDataType(descElem.getAttribute("DataType"));
        }

        /* 用VirtualAddress应该兼容处理 */
        if (descElem.hasAttribute("VirtualAddress")) {
            description.setfVirtualAddress(descElem.getAttribute("VirtualAddress"));
        }

        /* 用SystemAccess应该 */
        if (descElem.hasAttribute("SystemAccess")) {
            description.setfSystemAccess(descElem.getAttribute("SystemAccess"));
        }

        sharedData.setfSharedDataDescription(description);
    }

    /**
     * 用SharedIODescription用
     * 解析?17用SharedIODescription用
     * 
     * 解析?
     * - CachePolicy: IO?OFF+GUARDED?DEFAULT解析?
     * - SystemAccess: READ_ONLY?READ_WRITE解析?
     * - VirtualAddress: 应该addressSpaceRegionAllocationUnit应该
     */
    private static void parseSharedIODescription(Element ioDescElem, SharedData sharedData) {
        SharedData.SharedIODescription ioDescription = new SharedData.SharedIODescription();

        /* 用CachePolicy属性IO应该 */
        /* 解析分区IO?OFF+GUARDED?DEFAULT */
        if (ioDescElem.hasAttribute("CachePolicy")) {
            String cachePolicy = ioDescElem.getAttribute("CachePolicy");
            ioDescription.setfCachePolicy(cachePolicy);
        }

        /* 用SystemAccess?CoreOS解析IO应该 */
        /* 解析分区READ_ONLY?READ_WRITE */
        if (ioDescElem.hasAttribute("SystemAccess")) {
            String systemAccess = ioDescElem.getAttribute("SystemAccess");
            ioDescription.setfSystemAccess(systemAccess);
        }

        /* 用VirtualAddress属性IO应该 */
        /* 解析分区用addressSpaceRegionAllocationUnit应该兼容处理 */
        if (ioDescElem.hasAttribute("VirtualAddress")) {
            String virtualAddress = ioDescElem.getAttribute("VirtualAddress");
            ioDescription.setfVirtualAddress(virtualAddress);
        }

        /* 用BaseAddress应该 */
        if (ioDescElem.hasAttribute("BaseAddress")) {
            String baseAddress = ioDescElem.getAttribute("BaseAddress");
            ioDescription.setfBaseAddress(baseAddress);
        }

        /* 用Size应该 */
        if (ioDescElem.hasAttribute("Size")) {
            String size = ioDescElem.getAttribute("Size");
            ioDescription.setfSize(size);
        }

        sharedData.setfSharedIODescription(ioDescription);
        /* 清空SharedDataDescription字段，因为SharedData构造函数会自动创建一个空的SharedDataDescription */
        /* 这样可以确保SharedIODescription类型的SharedData只有fSharedIODescription字段有值 */
        sharedData.setfSharedDataDescription(null);
    }
}
