# PartitionDescription 元素顺序校验修改说明

## 1. 修改背景

本次修改针对 PartitionDescription 下同级元素顺序校验错误。

《操作系统 ARINC653 配置生成工具接口控制文档》明确规定：

> 除 Module/Schedules/Schedule 元素的若干个 PartitionWindow 子元素存在排列上的先后顺序外，其他所有同级元素在配置文件中出现的位置无先后顺序要求。

原始文档位置：

- [操作系统ARINC653配置生成工具接口控制文档.md:212](C:/Users/xie/Desktop/631/md文档/操作系统ARINC653配置生成工具接口控制文档.md:212)
- [表28 PartitionDescription元素包含的子元素及约束条件](C:/Users/xie/Desktop/631/md文档/操作系统ARINC653配置生成工具接口控制文档.md:487)

PartitionDescription 的子元素数量约束为：

| 子元素 | 最小出现次数 | 最大出现次数 |
|---|---:|---:|
| Application | 1 | N/A |
| SharedDataRegion | 0 | N/A |
| SharedLibraryRegion | 1 | N/A |
| Settings | 1 | 1 |

因此，以下顺序均应允许：

~~~xml
<Application/>
<SharedDataRegion/>
<SharedLibraryRegion/>
<Settings/>
~~~

~~~xml
<Settings/>
<SharedLibraryRegion/>
<Application/>
<SharedDataRegion/>
~~~

## 2. 修改前存在的问题

修改前的 [Partition.xsd](C:/Users/xie/Desktop/631/cpm-gen-verify/test/XSD/Partition.xsd) 使用 xs:sequence，强制要求：

~~~text
SharedDataRegion → Application → SharedLibraryRegion → Settings
~~~

原始文件 PAA_L1Instance_part.xml 和 DLC_L1_part.xml 的实际顺序为：

~~~xml
<Application .../>
<SharedDataRegion .../>
<SharedLibraryRegion .../>
<Settings .../>
~~~

这符合接口控制文档的“同级元素无先后顺序要求”，但被原 XSD 错误拒绝，并报告为 2010 元素出现次数超过最大值。

## 3. 修改文件

本次修改涉及两个源码文件：

1. [test/XSD/Partition.xsd](C:/Users/xie/Desktop/631/cpm-gen-verify/test/XSD/Partition.xsd)
2. [ARINC653ConfigValidator.java](C:/Users/xie/Desktop/631/cpm-gen-verify/src/main/java/com/nwpu/verify/layers/validation/ARINC653ConfigValidator.java)

未修改原始 XML 文件，未运行或修改现有 javatest。

## 4. Partition.xsd 代码变更

### 4.1 变更前

原 PartitionDescriptionType 使用顺序模型：

~~~xml
<xs:complexType name="PartitionDescriptionType">
    <xs:sequence>
        <xs:element name="SharedDataRegion"
                    type="A653_MemoryRegionType"
                    minOccurs="0"
                    maxOccurs="unbounded">
        </xs:element>

        <xs:element name="Application"
                    minOccurs="1"
                    maxOccurs="unbounded">
            <xs:complexType>
                <xs:attribute name="NameRef"
                              type="NameType"
                              use="required"/>
            </xs:complexType>
        </xs:element>

        <xs:element name="SharedLibraryRegion"
                    minOccurs="1"
                    maxOccurs="unbounded">
            <xs:complexType>
                <xs:attribute name="NameRef"
                              type="NameType"
                              use="required"/>
            </xs:complexType>
        </xs:element>

        <xs:element name="Settings"
                    minOccurs="1"
                    maxOccurs="1">
            <!-- Settings 原有属性定义 -->
        </xs:element>
    </xs:sequence>
</xs:complexType>
~~~

xs:sequence 会按照声明顺序校验子元素，因此不符合接口控制文档的顺序规则。

### 4.2 变更后

改为循环选择模型：

~~~xml
<xs:complexType name="PartitionDescriptionType">
    <!-- The ICD allows sibling elements in any order. The Java validator
         enforces the individual min/max occurrence constraints. -->
    <xs:choice minOccurs="1" maxOccurs="unbounded">
        <xs:element name="SharedDataRegion"
                    type="A653_MemoryRegionType"
                    minOccurs="0"
                    maxOccurs="unbounded">
        </xs:element>

        <xs:element name="Application"
                    minOccurs="1"
                    maxOccurs="unbounded">
            <xs:complexType>
                <xs:attribute name="NameRef"
                              type="NameType"
                              use="required"/>
            </xs:complexType>
        </xs:element>

        <xs:element name="SharedLibraryRegion"
                    minOccurs="1"
                    maxOccurs="unbounded">
            <xs:complexType>
                <xs:attribute name="NameRef"
                              type="NameType"
                              use="required"/>
            </xs:complexType>
        </xs:element>

        <xs:element name="Settings"
                    minOccurs="1"
                    maxOccurs="1">
            <!-- Settings 原有属性定义保持不变 -->
        </xs:element>
    </xs:choice>
</xs:complexType>
~~~

xs:choice 允许四类元素任意顺序出现，同时仍然只允许这四类元素。

由于项目使用 XML Schema 1.0，无法直接在“任意顺序 + 可重复元素”的模型中可靠表达每个元素独立的最小/最大出现次数，所以由 Java 校验器补充数量校验。

## 5. Java 校验代码变更

### 5.1 变更前

validateSingleFile 在 XSD 校验没有错误时直接返回成功：

~~~java
validator.validate(source);

if (errorHandler.hasErrors()) {
    Log.error(
            CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED,
            "Correctness verification for XML file Failed, File Path: ["
                    + xmlFile.getAbsolutePath() + "]。");
    return false;
}

return true;
~~~

### 5.2 变更后

XSD 校验通过后，增加 PartitionDescription 子元素数量校验：

~~~java
validator.validate(source);

if (errorHandler.hasErrors()) {
    Log.error(
            CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED,
            "Correctness verification for XML file Failed, File Path: ["
                    + xmlFile.getAbsolutePath() + "]。");
    return false;
}

// XML Schema 1.0 cannot express the unordered repeated
// PartitionDescription content model while retaining the
// individual min/max occurrence constraints. Validate those
// constraints after the order-independent XSD check.
return validatePartitionDescriptionOccurrences(xmlFile, errorMessages);
~~~

### 5.3 新增数量校验代码

~~~java
private static boolean validatePartitionDescriptionOccurrences(
        File xmlFile, List<String> errorMessages) {
    try {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        try {
            factory.setFeature(
                    XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Exception ignored) {
            // Keep compatibility with parsers that do not expose this feature.
        }

        Document document =
                factory.newDocumentBuilder().parse(xmlFile);
        Element root = document.getDocumentElement();
        String rootName = root.getLocalName() != null
                ? root.getLocalName()
                : root.getNodeName();

        if (!"PartitionDescription".equals(rootName)) {
            return true;
        }

        int applicationCount = 0;
        int sharedLibraryRegionCount = 0;
        int settingsCount = 0;

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) {
                continue;
            }

            Element child = (Element) children.item(i);
            String childName = child.getLocalName() != null
                    ? child.getLocalName()
                    : child.getNodeName();

            if ("Application".equals(childName)) {
                applicationCount++;
            } else if ("SharedLibraryRegion".equals(childName)) {
                sharedLibraryRegionCount++;
            } else if ("Settings".equals(childName)) {
                settingsCount++;
            }
        }

        boolean valid = true;
        int rootLine =
                getElementLineNumber(xmlFile, "PartitionDescription");

        if (applicationCount < 1) {
            String message = String.format(
                    "[File: %s, Line: %d] "
                            + "element Application occurs %d time(s) "
                            + "less than minimum 1 time(s).",
                    xmlFile.getName(),
                    rootLine,
                    applicationCount);

            addStructuralOccurrenceError(
                    errorMessages,
                    CodeConstant.ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET,
                    message);
            valid = false;
        }

        if (sharedLibraryRegionCount < 1) {
            String message = String.format(
                    "[File: %s, Line: %d] "
                            + "element SharedLibraryRegion occurs %d time(s) "
                            + "less than minimum 1 time(s).",
                    xmlFile.getName(),
                    rootLine,
                    sharedLibraryRegionCount);

            addStructuralOccurrenceError(
                    errorMessages,
                    CodeConstant.ERROR_XML_ELEMENT_MIN_OCCURS_NOT_MET,
                    message);
            valid = false;
        }

        if (settingsCount == 0) {
            String message = String.format(
                    "[File: %s, Line: %d] "
                            + "Required element 'Settings' is missing "
                            + "in parent element 'PartitionDescription'.",
                    xmlFile.getName(),
                    rootLine);

            addStructuralOccurrenceError(
                    errorMessages,
                    CodeConstant.ERROR_XML_REQUIRED_ELEMENT_MISSING,
                    message);
            valid = false;
        } else if (settingsCount > 1) {
            int settingsLine =
                    getElementLineNumber(xmlFile, "Settings");
            String message = String.format(
                    "[File: %s, Line: %d] "
                            + "element Settings occurs %d time(s) "
                            + "greater than 1 time(s).",
                    xmlFile.getName(),
                    settingsLine,
                    settingsCount);

            addStructuralOccurrenceError(
                    errorMessages,
                    CodeConstant.ERROR_XML_ELEMENT_MAX_OCCURS_EXCEEDED,
                    message);
            valid = false;
        }

        return valid;
    } catch (Exception e) {
        // The schema validator already reports malformed XML.
        // Do not add a second structural occurrence error for that case.
        return true;
    }
}
~~~

### 5.4 新增行号获取代码

为了让新增的数量错误包含 XML 行号，增加以下辅助方法：

~~~java
private static int getElementLineNumber(
        File xmlFile, String elementName) {
    final int[] lineNumber = {1};

    try {
        javax.xml.parsers.SAXParserFactory factory =
                javax.xml.parsers.SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        org.xml.sax.XMLReader reader =
                factory.newSAXParser().getXMLReader();

        reader.setContentHandler(new DefaultHandler() {
            private org.xml.sax.Locator locator;

            @Override
            public void setDocumentLocator(
                    org.xml.sax.Locator locator) {
                this.locator = locator;
            }

            @Override
            public void startElement(
                    String uri,
                    String localName,
                    String qName,
                    org.xml.sax.Attributes attributes) {
                String currentName =
                        localName == null || localName.isEmpty()
                                ? qName
                                : localName;

                if (elementName.equals(currentName)
                        && locator != null
                        && lineNumber[0] == 1) {
                    lineNumber[0] = locator.getLineNumber();
                }
            }
        });

        reader.parse(xmlFile.toURI().toString());
    } catch (Exception ignored) {
        // A fallback line is sufficient when the XML is not readable.
    }

    return lineNumber[0];
}
~~~

### 5.5 错误记录辅助方法

~~~java
private static void addStructuralOccurrenceError(
        List<String> errorMessages,
        Integer errorCode,
        String message) {
    if (errorMessages != null
            && !errorMessages.contains(message)) {
        errorMessages.add(message);
    }

    Log.error(errorCode, message);
}
~~~

## 6. 验证结果

### 6.1 构建验证

执行：

~~~text
mvn -q -DskipTests package
~~~

结果：构建成功。

### 6.2 原始 XML 回归

使用 CPM_L1(原始数据) 中的原始 XML 运行：

- XML 文件读取解析：通过
- XML 正确性校验：通过，0 errors
- XML 一致性校验：通过
- XML CRC 阶段：通过
- BIN CRC 阶段：通过
- 后续 BIN 配置数据比对仍有原有的 3011/3012，不属于本次 XML 顺序修改范围

日志：

[回归测试日志](<C:/Users/xie/Desktop/631/_xml_req_run/logs/after_order_fix_20260903_111010/CPM_Gen_Verify_Log_20260903_111011.log>)

### 6.3 数量约束反例验证

| 测试场景 | 结果 |
|---|---|
| Application、SharedDataRegion、SharedLibraryRegion、Settings 任意顺序 | XSD 校验通过 |
| 缺少 Settings | 报 2015 |
| 重复 Settings | 报 2010 |

## 7. 修改总结

本次修改将 PartitionDescription 的元素校验从“固定顺序”改为“任意顺序”，与接口控制文档保持一致；同时由 Java 校验器继续保证必填元素和 Settings 唯一性，避免放宽顺序后丢失数量校验。
