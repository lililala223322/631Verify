/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: XmlFieldSourceResolver.java
 * 描述    : 为CSV报告中的失败字段解析XML属性来源位置。
 *
 * 修改历史:
 *     2026-08-01  开发人员  按001050实现主XML及XInclude文件的属性来源定位。
 */
package com.nwpu.verify.layers.output;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @brief 建立XML属性来源索引，并根据二进制字段名称及期望值定位来源行。
 */
public final class XmlFieldSourceResolver {
    private static final int MIN_CONFIDENT_SCORE = 120;
    private final List<AttributeSource> sources = new ArrayList<>();
    private final Set<String> visitedFiles = new HashSet<>();
    private final Map<String, String> resolvedLocations = new HashMap<>();

    public XmlFieldSourceResolver(File moduleXmlFile) {
        scan(moduleXmlFile);
    }

    /**
     * @brief 解析字段对应的一个或多个XML属性位置。
     * @return String: FileName Line{lineNumber}；无法确认XML属性来源时返回0。
     */
    public String resolve(String fieldName, String expectedHex) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return "0";
        }

        String normalizedField = normalize(fieldName);
        String normalizedExpected = normalizeHex(expectedHex);
        String cacheKey = normalizedField + '\u0000' + normalizedExpected;
        String cachedLocation = resolvedLocations.get(cacheKey);
        if (cachedLocation != null) {
            return cachedLocation;
        }

        List<ScoredSource> matches = new ArrayList<>();
        int highestScore = 0;
        for (AttributeSource source : sources) {
            int sourceScore = score(normalizedField, normalizedExpected, source);
            if (sourceScore >= MIN_CONFIDENT_SCORE) {
                matches.add(new ScoredSource(source, sourceScore));
                highestScore = Math.max(highestScore, sourceScore);
            }
        }
        if (matches.isEmpty()) {
            resolvedLocations.put(cacheKey, "0");
            return "0";
        }

        final int best = highestScore;
        Collections.sort(matches, Comparator
                .comparing((ScoredSource item) -> item.source.fileName)
                .thenComparingInt(item -> item.source.lineNumber));

        LinkedHashSet<String> locations = new LinkedHashSet<>();
        for (ScoredSource match : matches) {
            if (match.score >= best - 20) {
                locations.add(match.source.fileName + " Line" + match.source.lineNumber);
            }
        }

        String location = locations.isEmpty() || locations.size() > 8 ? "0" : String.join(";", locations);
        resolvedLocations.put(cacheKey, location);
        return location;
    }
    /**
     * score
     *
     * @brief
     * 功能: 执行 score 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private int score(String fieldName, String normalizedExpected, AttributeSource source) {
        String attributeName = source.normalizedAttributeName;
        String elementName = source.normalizedElementName;
        int result = 0;

        if (attributeName.length() >= 4 && fieldName.contains(attributeName)) {
            result += attributeName.length() <= 4 ? 100 : 140;
        }
        if (elementName.length() >= 4 && fieldName.contains(elementName)) {
            result += 60;
        }
        String identity = source.normalizedIdentityValue;
        if (identity.length() >= 3 && fieldName.contains(identity)) {
            result += 80;
        }
        if (isSegmentSizeAlias(fieldName, attributeName)) {
            result += 180;
        }

        // A value match adds only 70 points. Below 50 name-based points the
        // candidate cannot reach MIN_CONFIDENT_SCORE, so conversion is skipped.
        if (result >= MIN_CONFIDENT_SCORE - 70
                && !normalizedExpected.isEmpty()
                && source.expectedHexValues.contains(normalizedExpected)) {
            result += 70;
        }
        return result;
    }

    /**
     * isSegmentSizeAlias
     *
     * @brief
     * 功能: 执行 isSegmentSizeAlias 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private boolean isSegmentSizeAlias(String fieldName, String attributeName) {
        if (!fieldName.contains("segmentsize") || !attributeName.startsWith("memorysize")) {
            return false;
        }
        String[] segments = { "persistentdata", "persistentbss", "rodata", "text", "data", "bss" };
        for (String segment : segments) {
            if (fieldName.contains(segment) && attributeName.equals("memorysize" + segment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * buildExpectedHexValues
     *
     * @brief
     * 功能: 执行 buildExpectedHexValues 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static Set<String> buildExpectedHexValues(String attributeValue) {
        Set<String> values = new HashSet<>();
        if (attributeValue == null) {
            return values;
        }

        String rawHex = toHex(attributeValue.getBytes(StandardCharsets.UTF_8));
        values.add(rawHex);
        values.add(rawHex + "00");

        String value = attributeValue.trim();
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            addNumericHexValues(values, BigInteger.valueOf(Boolean.parseBoolean(value) ? 1L : 0L));
            return values;
        }

        try {
            BigInteger number;
            if (value.matches("(?i)0x[0-9a-f]+")) {
                number = new BigInteger(value.substring(2), 16);
            } else if (value.matches("[+-]?\\d+")) {
                number = new BigInteger(value, 10);
            } else {
                return values;
            }
            addNumericHexValues(values, number);
        } catch (NumberFormatException ignored) {
            // Raw UTF-8 representations remain valid for non-numeric values.
        }
        return values;
    }

    /**
     * addNumericHexValues
     *
     * @brief
     * 功能: 执行 addNumericHexValues 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void addNumericHexValues(Set<String> values, BigInteger value) {
        for (int width : new int[] { 1, 2, 4, 8 }) {
            String bigEndian = toHex(toFixedWidth(value, width));
            values.add(bigEndian);
            values.add(reverseHexBytes(bigEndian));
        }
    }

    /**
     * toFixedWidth
     *
     * @brief
     * 功能: 执行 toFixedWidth 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static byte[] toFixedWidth(BigInteger value, int width) {
        byte[] result = new byte[width];
        byte[] raw = value.toByteArray();
        int copyLength = Math.min(raw.length, width);
        System.arraycopy(raw, raw.length - copyLength, result, width - copyLength, copyLength);
        return result;
    }
    /**
     * scan
     *
     * @brief
     * 功能: 执行 scan 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private void scan(File xmlFile) {
        if (xmlFile == null || !xmlFile.isFile()) {
            return;
        }
        try {
            if (!visitedFiles.add(xmlFile.getCanonicalPath())) {
                return;
            }

            final List<String> includes = new ArrayList<>();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            parser.parse(xmlFile, new DefaultHandler() {
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
                public void setDocumentLocator(Locator value) {
                    locator = value;
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
                    String element = localName == null || localName.isEmpty() ? qName : localName;
                    int line = locator == null ? 0 : locator.getLineNumber();
                    String identity = findIdentity(attributes);
                    for (int i = 0; i < attributes.getLength(); i++) {
                        String attribute = attributes.getLocalName(i);
                        if (attribute == null || attribute.isEmpty()) {
                            attribute = attributes.getQName(i);
                        }
                        if (!attribute.startsWith("xmlns")) {
                            sources.add(new AttributeSource(xmlFile.getName(), line, element,
                                    attribute, attributes.getValue(i), identity));
                        }
                    }
                    if ("include".equals(element)) {
                        String href = attributes.getValue("href");
                        if (href != null && !href.trim().isEmpty()) {
                            includes.add(href.trim());
                        }
                    }
                }
            });

            for (String href : includes) {
                scan(resolveInclude(xmlFile, href));
            }
        } catch (Exception ignored) {
            /*
             * 来源定位不改变主校验流程。解析或读取错误仍由既有1017、1019逻辑处理；
             * 无法建立来源索引时，Line列回退为0。
             */
        }
    }

    /**
     * findIdentity
     *
     * @brief
     * 功能: 执行 findIdentity 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private String findIdentity(Attributes attributes) {
        String[] names = { "Name", "NameRef", "Id", "ID", "Identifier", "PortName", "TaskName" };
        for (String name : names) {
            String value = attributes.getValue(name);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

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
    private File resolveInclude(File baseFile, String href) {
        String resolved = href;
        for (java.util.Map.Entry<String, String> variable : System.getenv().entrySet()) {
            resolved = resolved.replace("${" + variable.getKey() + "}", variable.getValue());
            resolved = resolved.replace("%" + variable.getKey() + "%", variable.getValue());
        }
        File included = new File(resolved);
        return included.isAbsolute() ? included : new File(baseFile.getParentFile(), resolved);
    }

    /**
     * normalize
     *
     * @brief
     * 功能: 执行 normalize 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    /**
     * normalizeHex
     *
     * @brief
     * 功能: 执行 normalizeHex 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String normalizeHex(String value) {
        return value == null ? "" : value.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
    }

    /**
     * toHex
     *
     * @brief
     * 功能: 执行 toHex 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String toHex(byte[] bytes) {
        final char[] hexDigits = "0123456789ABCDEF".toCharArray();
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            int unsigned = value & 0xFF;
            result.append(hexDigits[unsigned >>> 4]);
            result.append(hexDigits[unsigned & 0x0F]);
        }
        return result.toString();
    }

    /**
     * reverseHexBytes
     *
     * @brief
     * 功能: 执行 reverseHexBytes 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String reverseHexBytes(String hex) {
        StringBuilder result = new StringBuilder(hex.length());
        for (int i = hex.length(); i > 0; i -= 2) {
            result.append(hex, i - 2, i);
        }
        return result.toString();
    }
    private static final class AttributeSource {
        private final String fileName;
        private final int lineNumber;
        private final String normalizedElementName;
        private final String normalizedAttributeName;
        private final String normalizedIdentityValue;
        private final Set<String> expectedHexValues;

        private AttributeSource(String fileName, int lineNumber, String elementName,
                String attributeName, String attributeValue, String identityValue) {
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.normalizedElementName = normalize(elementName);
            this.normalizedAttributeName = normalize(attributeName);
            this.normalizedIdentityValue = normalize(identityValue);
            this.expectedHexValues = buildExpectedHexValues(attributeValue);
        }
    }
    private static final class ScoredSource {
        private final AttributeSource source;
        private final int score;

        private ScoredSource(AttributeSource source, int score) {
            this.source = source;
            this.score = score;
        }
    }
}
