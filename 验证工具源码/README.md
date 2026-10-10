# CPM 配置验证工具

本目录是 Maven 项目根目录，业务源码位于 `src/main/java`。

## 构建和运行

构建需要 JDK 和 Maven，并配置 `JAVA_HOME` 与 `PATH`。源码编译目标为 Java 8；首次构建需要下载 Maven 插件和依赖。

在本目录（`pom.xml` 所在目录）执行任务要求的命令：

```powershell
mvn install clean
```

命令成功后，交付文件位于：

```text
dist/
├── CPM_Gen_Verify.jar
└── XSD/
    ├── Application.xsd
    ├── CoreOS.xsd
    ├── Module.xsd
    ├── Partition.xsd
    ├── TypeLib.xsd
    └── XInclude.xsd
```

JAR 包含依赖和 `com.nwpu.verify.Application` 启动入口。分发时应复制整个 `dist` 目录，保留 JAR 旁的 `XSD` 子目录。

```powershell
cd dist
java -jar CPM_Gen_Verify.jar -v
java -jar CPM_Gen_Verify.jar -h
```

执行配置验证时，传入四个路径。XML 和 BIN 应来自同一次配置生成；输出目录应事先存在且可写：

```powershell
java -jar CPM_Gen_Verify.jar "主XML文件路径" "BIN目录" "报告目录" "日志目录"
```

默认按 64 位大端格式验证；其他格式可使用 `-32`、`-64`、`-big`、`-little` 参数。XML CRC 默认启用。

`install clean` 先编译、测试、打包并安装到 Maven 本地仓库，再清理 `target`。为满足任务要求，可执行 JAR 直接生成在 `dist`，XSD 同步复制到 `dist/XSD`，不受末尾 `clean` 影响。没有跳过或禁用 Maven 的清理操作。`mvn clean install` 和 `mvn package` 也使用同一交付目录。

`dist` 是保留的交付目录；若后续构建失败，其中可能仍有上次成功构建的文件，应以当前构建显示 `BUILD SUCCESS` 为准。

## 功能接口编号

按照 2026-09-16 任务的 CPM 前缀要求和需求中的功能顺序，在已有功能入口的方法注释中增加以下编号。任务原文已归档至工作区的 `文档/任务归档/2026-09-16_接口编号与构建打包.md`。未改变方法签名或业务逻辑。任务未附完整设计编号表，下表记录本次采用的对应关系。

下列 Java 文件均位于 `src/main/java/com/nwpu/verify/`。

| 设计编号 | 功能 | 入口方法 |
|---|---|---|
| TDD-CPMGenVerify-001 | 输入参数校验 | `ParameterValidationModule.validateParameters(String[])` |
| TDD-CPMGenVerify-002 | XML 读取与解析 | `XmlValidationModule.parseModule(String)` |
| TDD-CPMGenVerify-003 | XML 正确性校验 | `XmlValidationModule.validateXsdOnly(File, List)` |
| TDD-CPMGenVerify-004 | XML 一致性校验 | `XmlValidationModule.validateConsistencyRules(File, List)` |
| TDD-CPMGenVerify-005 | XML CRC 校验 | `XmlValidationModule.validateXmlCrc(File, List, boolean)` |
| TDD-CPMGenVerify-006 | BIN CRC 校验 | `BinValidationModule.validateBinCrc(List)` |
| TDD-CPMGenVerify-007 | 配置数据符合性校验 | `ConfigurationConsistencyModule.checkBinConfigConsistency(Module, List, File, String, String, String)` |
| TDD-CPMGenVerify-008 | CPM 配置验证报告生成 | `ReportGenerationModule.generatorReport(...)` |
| TDD-CPMGenVerify-009 | 运行状态记录文件生成 | `ReportGenerationModule.initialize(String, String)` |

九个编号同时标注在接口定义和 `Application.main()` 的对应调用处，便于从主流程追溯功能。日志初始化通过 `validateInputFiles()` 间接调用，因此主函数注明该调用关系，并在 `ParameterValidationModule.validateInputFiles()` 中调用 `ReportGenerationModule.initialize()` 的实际位置补上 `TDD-CPMGenVerify-009`。

日志初始化入口委托给 `layers/output/Log.initialize()`，后续写入和关闭仍由已有日志流程负责。此次补充仅涉及注释，不改变调用顺序、方法签名或业务逻辑。
