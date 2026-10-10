1 中国航空无线电电子研究所中国上海市桂平路432号2CAGE Code： SCFW53 电话：（86）021-33298000

# 操作系统ARINC653配置生成工具

# 接口控制文档

日期：2025.09.30

文件编号：182-ACPS-0323-OSICD

版本：1.7

## Notice

This document and the information contained herein are the property of CARERI. Any reproduction, disclosure or use thereof is prohibited except as authorized in writing by CARERI. Recipient accepts the responsibility for maintaining the confidentiality of the contents of this document. 

此文档和所涵盖的信息均属中国航空无线电电子研究所所有。除经中国航空无线电电子研究所书面授权外，禁止任何复制、披露或使用。接收者具有维护本文档内容的保密责任。


签署页


<table><tr><td>文档签署</td><td>姓名</td><td>角色</td><td>签字</td><td>日期</td></tr><tr><td>拟制</td><td></td><td></td><td></td><td></td></tr><tr><td>校对</td><td></td><td></td><td></td><td></td></tr><tr><td>审核</td><td></td><td></td><td></td><td></td></tr><tr><td>会签</td><td></td><td></td><td></td><td></td></tr><tr><td>会签</td><td></td><td></td><td></td><td></td></tr><tr><td>会签</td><td></td><td></td><td></td><td></td></tr><tr><td>会签</td><td></td><td></td><td></td><td></td></tr><tr><td>会签</td><td></td><td></td><td></td><td></td></tr><tr><td>会签</td><td></td><td></td><td></td><td></td></tr><tr><td>批准</td><td></td><td></td><td></td><td></td></tr></table>

## 版本记录

<table><tr><td>版本</td><td>更改日期</td><td>更改者</td><td>描述/变更号</td></tr><tr><td>1.0</td><td>2024.5.8</td><td>王超然</td><td>初始版本</td></tr><tr><td>1.1</td><td>2024.6.18</td><td>王超然</td><td>同行评审修订/CXFACPS-808</td></tr><tr><td>1.2</td><td>2024.6.28</td><td>王超然</td><td>虚端口修订/CXFACPS-1215</td></tr><tr><td>1.3</td><td>2024.8.21</td><td>王超然</td><td>修复 182-ACPS-0323-OSICD 的 SRR 评审问题/CXFACPS-1621</td></tr><tr><td>1.4</td><td>2025.1.22</td><td>王超然</td><td>2025 年 1 月 PRA、GPT 工具的累积更新/CXFACPS-3401</td></tr><tr><td>1.5</td><td>2025.4.18</td><td>王超然</td><td>修复截止 1.2.4 版本 GPT、PRA 工具以及 OSICD 中存在的问题/CXFACPS-3590</td></tr><tr><td>1.6</td><td>2025.5.12</td><td>王超然</td><td>修复截止 1.3 版本 GPT、PRA 工具以及 OSICD、FATICD 中存在的问题/CXFACPS-4160</td></tr><tr><td>1.7</td><td>2025.9.30</td><td>王超然</td><td>根据 CXFACPS-3029 问题修改 OSICD 与 FATICD/CXFACPS-6329</td></tr><tr><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td><td></td></tr></table>

## 目 次

25 1 范围....1
26 2 术语和缩略语....1
27 3 引用文件....1
28 4 概述....1
29 5 输入文件....1
30 5.1 CoreOS-核心操作系统....3
31 5.2 Applications-应用列表....3
32 5.2.1 Application-应用....4
33 5.3 SharedDataRegions-共享数据区域....8
34 5.3.1 SharedData-共享数据....8
35 5.4 SharedLibraryRegions-共享库区域....12
36 5.4.1 SharedLibrary-共享库....12
37 5.5 Partitions-分区列表....14
38 5.5.1 Partition-分区....15
39 5.5.2 PseudoPartition-虚分区....21
40 5.6 Schedules-调度表....25
41 5.6.1 Schedule-调度....25
42 5.7 Connections-通信链接....27
43 5.7.1 Channel-通信通道....27
44 5.8 LogBookList-日志清单....29
45 5.8.1 LogBook-日志....29
46 5.9 HealthMonitor-健康监控表....30
47 5.9.1 SystemErrors-系统故障列表....错误!未定义书签。
48 5.9.2 CoreHmStatelist-系统状态列表....错误!未定义书签。
49 5.9.3 HMTable-健康监控表....错误!未定义书签。
50 5.9.4 HMTask-健康监控任务表....错误!未定义书签。
51 5.10 Payloads-载荷....30
52 5.10.1 ConfigRecordPayload-配置记录载荷....31
53 5.10.2 CoreOSPayload-核心操作系统载荷....31
54 5.10.3 PartitionPayload-分区载荷....32
55 5.10.4 SharedLibraryPayload-共享库载荷....33

## 图清单

图 1 CPM 配置文件命名规则.....2
图 2 Module 根元素包含的子元素结构.....3

## 表清单

表 1 操作系统 ARINC653 配置文件使用的版本编码信息.....2
表 2 操作系统 ARINC653 配置文件使用的命名空间信息.....2
表 3 Module 的属性及约束条件.....3
表 4 Application 元素包含的子元素及约束条件.....3
表 5 Application 元素的属性及约束条件.....4
表 6 Application 元素的子元素及约束条件.....4
表 7 ApplicationDescription 元素的属性及约束条件.....4
表 8 ApplicationDescription 元素包含的子元素及约束条件.....5
表 9 MemorySize 元素的属性及约束条件.....5
表 10 Ports 元素包含的子元素及约束条件.....6
表 11 QueuingPort 元素包含的子元素及约束条件.....6
表 12 SamplingPort 元素的属性及约束条件.....7
表 13 SharedDataRegions 的子元素及约束条件.....8
表 14 SharedData 元素的属性及约束条件.....8
表 15 SharedData 元素包含的子元素及约束条件.....9
表 16 SharedDataDescription 元素的属性及约束条件.....9
表 17 SharedIODescription 元素的属性及约束条件.....11
表 18 SharedLibraryRegions 元素包含的子元素及约束条件.....12
表 19 SharedLibrary 元素的属性及约束条件.....12
表 20 SharedLibrary 元素包含的子元素及约束条件.....13
表 21 SharedLibraryDescription 元素的属性及约束条件.....13
表 22 SharedLibraryDescription 的子元素及约束条件.....14
表 23 MemorySize 元素的属性及约束条件.....14
表 24 Partitions 元素包含的子元素及约束条件.....14
表 25 Partition 的属性及约束条件.....15
表 26 Partition 的子元素及约束条件.....15
表 27 PartitionDescription 元素的属性及约束条件.....16
表 28 PartitionDescription 元素包含的子元素及约束条件.....16
表 29 Application 元素的属性及约束条件.....17
表 30 SharedDataRegion 元素的属性及约束条件.....17
表 31 SharedLibraryRegion 元素的属性及约束条件.....18
表 32 Settings 元素的属性及约束条件.....18
表 33 PseudoPartition 的属性及约束条件.....21
表 34 PseudoPartition 元素包含的子元素及约束条件.....22

表 35 PseudoPartitionDescription 元素包含的子元素及约束条件....22
表 36 Ports 元素包含的子元素及约束条件....22
表 37 QueuingPort 元素的属性及约束条件....23
表 38 SamplingPort 元素的属性及约束条件....24
表 39 Schedules 元素包含的子元素及约束条件....25
表 40 Schedule 元素的属性及约束条件....25
表 41 Schedule 元素包含的子元素及约束条件....26
表 42 PartitionWindow 的属性及约束条件....26
表 43 Connections 元素包含的子元素及约束条件....27
表 44 Channel 元素的属性及约束条件....27
表 45 Channel 元素包含的子元素及约束条件....27
表 46 Source 元素的属性及约束条件....27
表 47 Destination 元素的的属性及约束条件....28
表 48 LogBookList 元素包含的子元素及约束条件....29
表 49 LogBook 元素的属性及约束条件如....29
表 50 HealthMonitor 元素包含的子元素及约束条件....错误!未定义书签。
表 51 SystemErrors 元素包含的子元素及约束条件....错误!未定义书签。
表 52 SystemError 元素的的属性及约束条件....错误!未定义书签。
表 53 CoreHmStatelist 元素包含的子元素及约束条件....错误!未定义书签。
表 54 CoreHmState 元素的的属性及约束条件....错误!未定义书签。
表 55 HMTable 元素包含的子元素及约束条件....错误!未定义书签。
表 56 CoreHMTableList 元素包含的子元素及约束条件....错误!未定义书签。
表 57 CoreHMTable 元素的的属性及约束条件....错误!未定义书签。
表 58 CoreHMTable 元素包含的子元素及约束条件....错误!未定义书签。
表 59 ErrorAction 元素的的属性及约束条件....错误!未定义书签。
表 60 CoreHMTableList 元素包含的子元素及约束条件....错误!未定义书签。
表 61 MultiPartitionHMTable 元素的的属性及约束条件...错误!未定义书签。
表 62 MultiPartitionHMTable 元素包含的子元素及约束条件.错误!未定义书签。
表 63 ErrorAction 元素的的属性及约束条件....错误!未定义书签。
表 64 PartitionHMTableList 元素包含的子元素及约束条件..错误!未定义书签。
表 65 PartitionHMTable 元素的的属性及约束条件....错误!未定义书签。
表 66 PartitionHMTable 元素包含的子元素及约束条件....错误!未定义书签。
表 67 ErrorAction 元素的的属性及约束条件....错误!未定义书签。
表 68 PartitionRef 元素的的属性及约束条件....错误!未定义书签。
表 69 PartitionRef 元素的的属性及约束条件....30
表 70 Payloads 元素所含子元素及约束条件....30
表 71 ConfigRecordPayload 元素所含的属性及约束条件....31

表 72 CoreOSPayload 元素所含的属性及约束条件....31
表 73 PartitionPayload 元素所含的属性及约束条件....32
表 74 SharedLibraryPayload 元素所含的属性及约束条件....33

## 1 范围

本文件是 CXF 项目操作系统 ARINC653 配置文件说明，包含了 ARINC653操作系统所有配置数据的定义。本文件由中国航空无线电电子研究所编制，适用于CXF项目中接入航电核心处理系统的所有CPM模块。

## 2 术语和缩略语

以下缩略语适用于本文件。

<table><tr><td>缩略语</td><td>英文全称</td><td>中文</td></tr><tr><td>ARINC</td><td>Aeronautical Radio Inc</td><td>航空无线电通信公司</td></tr><tr><td>APR</td><td>Avionics Processing Resource</td><td>航电处理资源</td></tr><tr><td>CPM</td><td>Common Processing Module</td><td>通用处理模块</td></tr></table>

## 145 3 引用文件

146 下列文件对于本文件的应用是必不可少的。凡是注日期的引用文件，仅注147 日期的版本适用于本文件。凡是不注日期的引用文件，其最新版本（包括所有<sub>148</sub> 的修改单）适用于本文件。

<table><tr><td>文件编号</td><td>文件名称</td></tr><tr><td>ARINC 653 P1-3</td><td>航空电子应用软件标准接口-要求服务</td></tr><tr><td>ARINC 653 P2-2</td><td>航空电子应用软件标准接口-扩展服务</td></tr></table>

## 4 概述

ARINC 653 标准中定义了一种通过指定操作系统配置定义操作系统行为与特性的机制。CXF项目 APR中所有的 CPM模块均使用了符合 ARINC 653标准的操作系统，本文件描述了操作系统 ARINC653配置文件内容。

## 5 输入文件

操作系统 ARINC653配置文件为 XML格式，其命名规则如下：

![image](https://cdn-mineru.openxlab.org.cn/result/2026-08-26/a01489b9-9bdb-41aa-bf91-3bed34e70f1e/96369629038cd7f3c42e790fc18b83efd1065fd4cd88ab7d631f1879bda8c9d0.jpg)



图 1 CPM配置文件命名规则



操作系统 ARINC653配置文件使用的版本编码信息为：



表 1操作系统ARINC653 配置文件使用的版本编码信息


<table><tr><td>属性</td><td>值</td></tr><tr><td>xml version</td><td>1.0</td></tr><tr><td>encoding</td><td>UTF-8</td></tr><tr><td>standalone</td><td>no</td></tr></table>


操作系统 ARINC653配置文件使用的命名空间信息为：



表 2 操作系统ARINC653 配置文件使用的命名空间信息


<table><tr><td>前缀</td><td>URI</td></tr><tr><td>默认</td><td>http://www.actri.avic/ConfigRecord</td></tr><tr><td>xi</td><td>http://www.w3.org/2001/XInclude</td></tr><tr><td>xsi</td><td>http://www.w3.org/2001/XMLSchema-instance</td></tr></table>

操作系统 ARINC653配置文件引用的文件至少包括：

1) CoreOS.xml（引入核心操作系统配置）

2) POS.xml（引入分区操作系统共享库配置）

操作系统 ARINC653配置文件引用的文件可以选择包括：

1) 应用配置（引入应用描述）

2) 分区配置（引入分区描述）

3) 系统共享库配置（引入共享库描述）

操作系统 ARINC653 配置文件中除 Module/Schedules/Schedule 元素的若干个PartitionWindow子元素存在排列上的先后顺序外，其他所有同级元素在配置文件中出现的位置无先后顺序要求；每个元素的所有属性在配置文件中出现的位置无先后顺序要求。

操作系统 ARINC653配置文件包含一个根元素 Module，其属性及约束关系如表 3：


表 3 Module的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>CRC32</td><td>文件CRC校验值</td><td>十六进制</td><td>N/A</td><td>校验范围仅为当前 xml 文件,不包含当前 xml 文件引用的其他文件。CRC 计算前将该属性置为空值,计算后再将 CRC 值填入该属性。所使用的 CRC32 多项式为:<eq>G[x] = x^{32} + x^{26} + x^{23} + x^{22} + x^{16} + x^{12} + x^{11} + x^{10} + x^{8} + x^{7} + x^{5} + x^{4} + x^{2} + x + 1</eq>。该属性值取值范围为 0x00000000 至 0xFFFFFFF,共 8 位,不足 8 位的在数值前补 0,数值前需要有 0x 标记。</td></tr></table>


Module根元素包含的子元素结构为：


<table><tr><td>CoreOS</td><td>Applications</td><td>SharedData Regions</td><td>SharedLibrary Regions</td><td>Partitions</td><td>Schedules</td><td>Connections</td><td>LogBookList</td><td>HealthMonitor</td><td>Payloads</td></tr></table>


图 2 Module 根元素包含的子元素结构


## 5.1 CoreOS-核心操作系统

CoreOS子元素内容通过CoreOS.xml引入。该文件由平台供应商提供，并由系统集成商审核。

## 5.2 Applications-应用列表

Applications元素包含的子元素及约束条件如表 4：

## 表 4 Application 元素包含的子元素及约束条件

<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>Application</td><td>255</td><td>1</td></tr></table>

## 5.2.1 Application-应用

Application 元素的属性及约束关系如表 5：


表 5 Application 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>Name</td><td>分区名称</td><td>字符串</td><td>N/A</td><td>1至30个字符,在根元素内必须唯一</td></tr></table>


Application 元素包含的子元素及约束条如表 6：



表 6 Application 元素的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>ApplicationDescription</td><td>1</td><td>1,内容可由其他 xml 引入</td></tr></table>

## 5.2.1.1 ApplicationDescription-应用描述

ApplicationDescription 元素的属性及约束关系如表 7：


表 7 ApplicationDescription 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>CRC32</td><td>文件 CRC校验值</td><td>十六进制</td><td>N/A</td><td>非必须属性,仅当该元素为独立文件时有效。校验范围仅为当前 xml 文件,不包含当前 xml 文件引用的其他文件。CRC 计算前将该属性置为空值,计算后再将 CRC 值填入该属性。所使用的 CRC32 多项式为:<eq>G[x] = x^{32} + x^{26} + x^{23} + x^{22} + x^{16} + x^{12} + x^{11} + x^{10} + x^{8} + x^{7} + x^{5} + x^{4} + x^{2} +</eq></td></tr><tr><td></td><td></td><td></td><td></td><td>x+1。该属性值取值范围为0x00000000至0xFFFFFFF,共8位,不足8位的在数值前补0,数值前需要有0x标记。</td></tr></table>

ApplicationDescription 元素包含的子元素及约束条件如表 8：


表 8 ApplicationDescription 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>MemorySize</td><td>1</td><td>1</td></tr><tr><td>Ports</td><td>1</td><td>1</td></tr></table>

## 5.2.1.1.1 MemorySize-应用段大小

MemorySize 元素的属性及约束条件如表 9：


表 9 MemorySize 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>MemorySizeBss</td><td>应用程序 ELF 文件的 BSS 段的最大大小</td><td rowspan="5">十进制或十六进制</td><td rowspan="5">Byte</td><td rowspan="5">其大小必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit.的非负整数倍</td></tr><tr><td>MemorySizeData</td><td>应用程序 ELF 文件的数据段的最大大小</td></tr><tr><td>MemorySizePersistentBss</td><td>应用程序 ELF 文件的持续 BSS 段最大大小</td></tr><tr><td>MemorySizePersistentData</td><td>应用程序 ELF 文件的持续数据段最大大小</td></tr><tr><td>MemorySizeRoData</td><td>应用程序 ELF 文件的只读数据段最大大小</td></tr><tr><td>MemorySizeText</td><td>应用程序 ELF 文件的代码段最大大小</td><td></td><td></td><td></td></tr></table>

## 5.2.1.1.2 Ports-应用端口

Ports 元素包含的子元素及约束条件如表 10：


表 10 Ports 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>QueuingPort</td><td rowspan="2">QueuingPort 和 SamplingPort 之和不超过 5120</td><td>0</td></tr><tr><td>SamplingPort</td><td>0</td></tr></table>

## 5.2.1.1.2.1 QueuingPort- 队列应用端口

QueuingPort 元素包含的子元素及约束条件如表 11：


表 11 QueuingPort元素包含的子元素及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>Name</td><td>端口名称</td><td>字符串</td><td>N/A</td><td>1至30个字符,根元素内必须唯一</td></tr><tr><td>MaxMessageSize</td><td>端口发送的最大消息长度</td><td>十进制或十六进制</td><td>Byte</td><td>1至0x100000</td></tr><tr><td>Direction</td><td>端口是输入还是输出数据</td><td>枚举</td><td>N/A</td><td>SOURCE(源端口、发送端口)或DESTINATION(目的端口、接收端口)</td></tr><tr><td>MaxNbMessage</td><td>一个端口可以同时处理的最大消息数量,最大队列深度</td><td>十进制或十六进制</td><td>个</td><td>1至512</td></tr><tr><td>ExtendedPort</td><td>是否使用SAP扩展服务,是否为SAP端口</td><td>布尔</td><td>N/A</td><td>非必须属性,若未填写,则CPM_GEN工具认为其值为false</td></tr><tr><td>ProtocolType</td><td>协议类型</td><td>十进制或十六进制</td><td>N/A</td><td>2(NOT_APPLICABLE,不适用)或1(RECEIVER_DISCARD,接收方丢弃)或0(SENDER_BLOCK,发送方阻塞)</td></tr><tr><td>Attribute</td><td>端口类型</td><td>枚举</td><td>N/A</td><td>NULL_PORT(空)或LOCAL_PORT(本地端口)或DIRECT_ACCESS_PORT(直接访问端口),非必须属性,若未填写,则CPM_GEN工具认为其值为LOCAL_PORT</td></tr></table>

## 5.2.1.1.2.2 SamplingPort- 采样应用端口

SamplingPort 元素的属性及约束条件如表 12：


表 12 SamplingPort 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>Name</td><td>端口名称</td><td>字符串</td><td>N/A</td><td>1至30个字符,根元素内必须唯一</td></tr><tr><td>MaxMessageSize</td><td>端口发送的最大消息长度</td><td>十进制或十六进制</td><td>N/A</td><td>1至0x100000</td></tr><tr><td>Direction</td><td>端口是输入还是输出数据</td><td>枚举</td><td>N/A</td><td>SOURCE(源端口、发送端口)或DESTINATION(目的端口、接收端</td></tr><tr><td></td><td></td><td></td><td></td><td>口)</td></tr><tr><td>Attribute</td><td>端口类型</td><td>枚举</td><td>N/A</td><td>NULL_PORT(空)或LOCAL_PORT(本地端口)或DIRECT_ACCESS_PORT(直接访问端口),非必须属性,若未填写,则CPM_GEN工具认为其值为LOCAL_PORT</td></tr><tr><td>RefreshRate</td><td>消息在端口的更新频率,刷新率</td><td>十进制或十六进制或枚举值(仅INFINITE_TIME)</td><td>nS</td><td>1至9223372036854775807或INFINITE_TIME(-1)</td></tr></table>

## 5.3 SharedDataRegions-共享数据区域

SharedDataRegions 元素包含的子元素及约束条件如表 13：


表 13 SharedDataRegions 的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>SharedData</td><td>N/A</td><td>0</td></tr></table>

## 5.3.1 SharedData-共享数据

SharedData 元素的属性及约束条件如表 14：


表 14 SharedData 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>Name</td><td>共享数据的名称</td><td>字符串</td><td>N/A</td><td>1至30个字符,根元素内必须唯一。对于共享IO,其名称必须与Module/CoreOS/CoreOSDescriptio</td></tr><tr><td></td><td></td><td></td><td></td><td>n/HardwareConfiguration/sharedIO/@PoolName 相匹配</td></tr></table>


SharedData 元素包含的子元素及约束条件如表 15：



表 15 SharedData 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>SharedDataDescription</td><td>1</td><td>0</td></tr><tr><td>SharedIODescription</td><td>1</td><td>0</td></tr></table>


5.3.1.1 SharedDataDescription-共享数据描述



SharedDataDescription 元素包含的属性及约束关系如表 16：



表 16 SharedDataDescription 元素的属性及约束条件


<table><tr><td>属性项</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束条件</td></tr><tr><td>Size</td><td>共享数据区域的大小</td><td>十进制或十六进制</td><td>Byte</td><td>其大小必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit.的非负整数倍</td></tr><tr><td>CachePolicy</td><td>共享数据区域的缓存策略</td><td>枚举</td><td>N/A</td><td>从以下字段中取值:OFF、COPY_BACK、WRITE_THRU、OFF+COHERENCY、COPY_BACK+COHERENCY、WRITE_THRU+COHERENCY、OFF+GUARDED、COPY_BACK+GUARDED、WRITE_THRU+GUARDED、OFF+GUARDED+COHERENCY、COPY_BACK+GUARDED+COHERENCY、WRITE_THRU+GUARDED+COHERENCY、IO、DEFAULT,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>DataType</td><td>储存于共享数据区域中的数据的类型</td><td>枚举</td><td>N/A</td><td>CONFIG_FILE或DATABASE或VIRTUAL,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>SystemAccess</td><td>决定CoreOS对该共享数据区域的访问权限</td><td>枚举</td><td>N/A</td><td>READ_ONLY或READ_WRITE,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>VirtualAddress</td><td>共享数据区域的虚拟地址</td><td>十进制或十六进制</td><td>Byte</td><td>其大小必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit.的非负整数倍,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr></table>


SharedIODescription 元素的属性及约束条件如表 17：


5.3.1.2 SharedIODescription-共享 IO 描述


表 17 SharedIODescription 元素的属性及约束条件


<table><tr><td>属性项</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束条件</td></tr><tr><td>CachePolicy</td><td>共享IO区域的缓存策略</td><td>枚举</td><td>N/A</td><td>IO或OFF+GUARDED或DEFAULT,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>SystemAccess</td><td>决定CoreOS对该共享IO区域的访问权限</td><td>枚举</td><td>N/A</td><td>READ_ONLY或READ_WRITE,非必须属性,</td></tr><tr><td></td><td></td><td></td><td></td><td>若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>VirtualAddress</td><td>共享IO区域的虚拟地址</td><td>十进制或十六进制</td><td>Byte</td><td>其大小必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit.的非负整数倍,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr></table>

## 5.4 SharedLibraryRegions-共享库区域

SharedLibraryRegions 元素包含的子元素及约束条件如表 18：


表 18 SharedLibraryRegions 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>SharedLibrary</td><td>N/A</td><td>1,至少有一个元素的内容由POS.xml引入</td></tr></table>


引入的 POS.xml由平台供应商提供，并由系统集成商审核。


## 5.4.1 SharedLibrary-共享库

SharedLibrary 元素包含的属性及约束关系如表 19：


表 19 SharedLibrary 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>Name</td><td>共享数据的名称</td><td>字符串</td><td>N/A</td><td>1至30个字符,根元素内必须唯一</td></tr></table>

SharedLibrary 元素包含的子元素及约束条件如表 20：


表 20 SharedLibrary 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>SharedLibraryDescription</td><td>1</td><td>1</td></tr></table>

## 5.4.1.1 SharedLibraryDescription-共享库描述

SharedLibraryDescription 元素的属性及约束条件如表 21：


表 21 SharedLibraryDescription 元素的属性及约束条件


<table><tr><td>属性项</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束条件</td></tr><tr><td>SystemSharedLibrary</td><td>是否为系统共享库</td><td>布尔</td><td>N/A</td><td>N/A</td></tr><tr><td>VirtualAddress</td><td>共享库的虚拟地址</td><td>十进制或十六进制</td><td>Byte</td><td>其大小必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit.的非负整数倍</td></tr><tr><td>CRC32</td><td>文件CRC校验值</td><td>十六进制</td><td>N/A</td><td>非必须属性,仅当该元素为独立文件时有效。校验范围仅为当前xml文件,不包含当前xml文件引用的其他文件。CRC计算前将该属性置为空值,计算后再将CRC值填入该属性。所使用的CRC32多项式为:<eq>G[x] = x^{32} + x^{26} + x^{23} + x^{22} + x^{16} + x^{12} + x^{11} + x^{10} + x^{8} + x^{7} + x^{5} + x^{4} + x^{2} + x + 1</eq>。该属性值取值范围为0x00000000至0xFFFFFFFF,共8位,不足8位的在数值前补0,数值前需要有0x标记。</td></tr></table>


SharedLibraryDescription 元素包含的子元素及约束条件如表 22：



表 22 SharedLibraryDescription 的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>MemorySize</td><td>1</td><td>1</td></tr></table>

## 5.4.1.1.1 MemorySize-共享库段大小

MemorySize 元素的属性及约束条件如表 23：


表 23 MemorySize 元素的属性及约束条件


<table><tr><td>属性项</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束条件</td></tr><tr><td>MemorySizeBss</td><td>共享库的 BSS 段的最大大小</td><td rowspan="6">十进制或十六进制</td><td rowspan="6">Byte</td><td rowspan="6">其大小必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit.的非负整数倍</td></tr><tr><td>MemorySizeData</td><td>共享库的数据段的最大大小</td></tr><tr><td>MemorySizePersistentBss</td><td>共享库的持续BSS 段最大大小</td></tr><tr><td>MemorySizePersistentData</td><td>共享库的持续数据段最大大小</td></tr><tr><td>MemorySizeRoData</td><td>共享库的只读数据段最大大小</td></tr><tr><td>MemorySizeText</td><td>共享库的代码段最大大小</td></tr></table>

## 5.5 Partitions-分区列表

Partitions 元素包含的子元素及约束条件如表 24：

表 24 Partitions 元素包含的子元素及约束条件

<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>Partition</td><td>255</td><td>1</td></tr><tr><td>PseudoPartition</td><td>无限制</td><td>0</td></tr></table>

## 5.5.1 Partition-分区

Partition 元素的属性及约束条件如表 25：


表 25 Partition 的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>Identifier</td><td>分区号</td><td>十进制</td><td>N/A</td><td>1至255,根元素内必须唯一,其值与Module/Partitions/PseudoPartition/@Identifier.定义不能相同</td></tr><tr><td>Name</td><td>分区名称</td><td>字符串</td><td>N/A</td><td>1至30个字符,根元素内必须唯一</td></tr><tr><td>Type</td><td>分区类型</td><td>枚举</td><td>N/A</td><td>PARTITION_APEX、PARTITION_POSIX、PARTITION_GOS,非必须属性,若未填写,则CPM_GEN工具认为其值为PARTITION_APEX</td></tr></table>


Partition元素包含的子元素及约束条件如表26：



表 26 Partition 的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>PartitionDescription</td><td>1</td><td>1,内容可由其他 xml 引入</td></tr></table>

## 5.5.1.1 PartitionDescription-分区描述

PartitionDescription 元素可参考如下分区配置模板进行配置：


PartitionDescription 元素的属性及约束关系如表 27：



表 27 PartitionDescription 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>CRC32</td><td>文件CRC校验值</td><td>十六进制</td><td>N/A</td><td>非必须属性,仅当该元素为独立文件时有效。校验范围仅为当前xml文件,不包含当前xml文件引用的其他文件。CRC计算前将该属性置为空值,计算后再将CRC值填入该属性。所使用的CRC32多项式为:<eq>G[x] = x^{32} + x^{26} + x^{23} + x^{22} + x^{16} + x^{12} + x^{11} + x^{10} + x^{8} + x^{7} + x^{5} + x^{4} + x^{2} + x + 1</eq>。该属性值取值范围为0x00000000至0xFFFFFFF,共8位,不足8位的在数值前补0,数值前需要有0x标记。</td></tr></table>


PartitionDescription 元素包含的子元素及约束条件如表 28：



表 28 PartitionDescription 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>Application</td><td>N/A</td><td>1</td></tr><tr><td>SharedDataRegion</td><td>N/A</td><td>0</td></tr><tr><td>SharedLibraryRegion</td><td>N/A</td><td>1</td></tr><tr><td>Settings</td><td>1</td><td>1</td></tr></table>

## 5.5.1.1.1 Application-分区应用

Application 元素的的属性及约束条件如表 29：


表 29 Application 元素的的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>NameRef</td><td>分区内的应用名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/Applications/Application/@Name.的定义对应</td></tr></table>

## 5.5.1.1.2 SharedDataRegion- 分区共享数据区域

SharedDataRegion 元素的的属性及约束条件如表 30：


表 30 SharedDataRegion 元素的的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>Name</td><td>分区需要访问的共享数据区域名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/SharedDataRegions/SharedData/@Name.的定义对应</td></tr><tr><td>Type</td><td>分区需要访问的共享数据区域类型</td><td>枚举</td><td>N/A</td><td>CONFIG_FILE或DATABASE或VIRTUAL,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>Size</td><td>分区需要访问的共享数据区域大小</td><td>十进制或十六进制</td><td>Byte</td><td>其大小必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit.的非负整数倍</td></tr><tr><td>Address</td><td>分区需要访问的共享数据区域地址</td><td>十进制或十六进制</td><td>N/A</td><td>其大小必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit.的非负整数倍,非必须属性,</td></tr><tr><td></td><td></td><td></td><td></td><td>若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>AccessRights</td><td>分区对该共享数据区域的访问权限</td><td>枚举</td><td>Byte</td><td>NONE或READ_ONLY或READ_WRITE</td></tr></table>

## 5.5.1.1.3 SharedLibraryRegion-分区共享库

SharedLibraryRegion 元素的属性及约束条件如表 31：


表 31 SharedLibraryRegion 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>NameRef</td><td>分区使用的共享库名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/SharedLibraryRegions/SharedLibrary/@Name.定义相匹配</td></tr></table>

## 5.5.1.1.4 Settings-分区设置

Settings 元素的属性及约束条件如表 32：


表 32 Settings 元素的属性及约束条件


<table><tr><td>属性项</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束条件</td></tr><tr><td>RequiredMemorySize</td><td>分区需要的内存大小,包括分区的各段,分区的堆和栈空间</td><td>十进制或十六进制</td><td>N/A</td><td>最小值应不小于所引用应用各段之和+pos 各段(不包括代码段)之和+0x1000。所有分区的@RequiredMemorySize 之和+共享库各段(包括附加段)之和+所有共享数据大小必须小于等于Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/userMemoryRegion/@Size</td></tr><tr><td>allocDisable</td><td>是否禁止分区 NORMAL后的动态内存分配,ARINC653 规范建议分区在 NORMAL 后要禁止动态内存分配;</td><td>布尔</td><td>N/A</td><td>非必须属性,若未填写,则 CPM_GEN认为其值为 true</td></tr><tr><td>fpExcEnable</td><td>浮点异常是否使能</td><td>布尔</td><td>N/A</td><td>非必须属性,若未填写,则由CPM_GEN 工具根据模块默认值定义</td></tr><tr><td>isrStackSize</td><td>分区虚中断堆栈大小</td><td>十进制或十六进制</td><td>N/A</td><td>非必须属性,若未填写,则由CPM_GEN 工具根据模块默认值定义</td></tr><tr><td>maxGlobalFDs</td><td>分区能够打开的全局文件描述符的最大数量</td><td>十进制或十六进制</td><td>N/A</td><td>非必须属性,若未填写,则由CPM_GEN 工具根据模块默认值定义</td></tr><tr><td>numLogMsgs</td><td>在分区日志队列中允许缓存的消息的最大数量</td><td>十进制或十六进制</td><td>N/A</td><td>非必须属性,若未填写,则由CPM_GEN 工具根据模块默认值定义</td></tr><tr><td>numStackGuardPages</td><td>分区内进程的栈保护页个数。当该配置不为 0时,操作系统可检测并报告栈溢出异常,栈保护页个数的多少决定了操作系统可检测的栈溢出量的大小;当该配置为 0 时,操作系统无法检测栈溢出异常。</td><td>十进制或十六进制</td><td>N/A</td><td>非必须属性,若未填写,则由CPM_GEN 工具根据模块默认值定义</td></tr><tr><td>numWorkerTasks</td><td>分区允许的工作任务的最大数量</td><td>十进制或十六进制</td><td>N/A</td><td>非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>watchDogDuration</td><td>分区热启动过程的最长等待时间,超过等待时间则分区冷启动;</td><td>十进制或十六进制</td><td>nS</td><td>1至9223372036854775807,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>systemPartition</td><td>设置分区是否为系统分区</td><td>布尔</td><td>N/A</td><td>非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>scheduleChangeAction</td><td>调度切换时的操作</td><td>枚举</td><td>N/A</td><td>COLD_RESTARTWARM_RESTARTIDLEIGNORE,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>initProcStackSize</td><td>初始化进程栈大小</td><td>十进制或十六进制</td><td>N/A</td><td>非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>setModuleSchedule</td><td>是否允许分区执行调度表切换,配置为true时才能在该分区中调用切换调度表接口</td><td>布尔</td><td>N/A</td><td>非必须属性,若未填写,则CPM_GEN认为其值为false</td></tr><tr><td>duration</td><td>分区窗口长度,不使用</td><td>十进制或十六进制</td><td>ns</td><td>非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>period</td><td>分区周期,不使用</td><td>十进制或十六进制</td><td>ns</td><td>非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>partitionVirtualAddress</td><td>分区虚拟起始地址,不使用</td><td>十进制或十六进制</td><td>N/A</td><td>非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>partitionVirtualMemorySize</td><td>分区虚拟地址大小,不使用</td><td>十进制或十六进制</td><td>N/A</td><td>非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr></table>

## 5.5.2 PseudoPartition-虚分区

PseudoPartition 元素的属性及约束条件如表 33：


表 33 PseudoPartition 的属性及约束条件


<table><tr><td>属性项</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束条件</td></tr><tr><td>Identifier</td><td>定义虚分区的标识符</td><td>十进制或十六进制</td><td>N/A</td><td>1至255,根元素内必须唯一,其值与Module/Partitions/Partition/@ Identifier.定义不能相同</td></tr><tr><td>Name</td><td>虚分区的名称</td><td>字符串</td><td>N/A</td><td>1至30个字符,根元素内必须唯一。CXF项目ACPS中约定为固定值:a664_driver</td></tr><tr><td>Type</td><td>虚分区的类型</td><td>枚举</td><td>N/A</td><td>APP_PARTITIONSYS_PARTITIONIO_PARTITION</td></tr></table>


PseudoPartition 元素包含的子元素及约束条件如表 34：



表 34 PseudoPartition 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>PseudoPartitionDescription</td><td>1</td><td>1</td></tr></table>

## 5.5.2.1 PseudoPartitionDescription-虚分区描述

PseudoPartitionDescription 元素包含的子元素及约束条件如表 35：


表 35 PseudoPartitionDescription 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>Ports</td><td>N/A</td><td>1</td></tr></table>

## 5.5.2.1.1 Ports-虚端口

Ports 元素包含的子元素及约束条件如表 36：


表 36 Ports 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>QueuingPort</td><td rowspan="2">QueuingPort 和 SamplingPort之和不超过 5120</td><td>0</td></tr><tr><td>SamplingPort</td><td>0</td></tr></table>

## 5.5.2.1.1.1 QueuingPort-队列虚端口

QueuingPort 元素的属性及约束条件如表 37：


表 37 QueuingPort 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>Name</td><td>端口名称</td><td>字符串</td><td>N/A</td><td>1至30个字符,根元素内必须唯一CXF项目ACPS中约定:发送端口:pseudoport_tx_【端口号】_【端口序号】接收端口:pseudoport_rx_【端口号】_【端口序号】其中【端口号】为6位以内的数字,【端口序号】为2位以内的数字。</td></tr><tr><td>MaxMessageSize</td><td>端口发送的最大消息长度</td><td>十进制或十六进制</td><td>Byte</td><td>1至0x100000</td></tr><tr><td>Direction</td><td>端口是输入还是输出数据</td><td>枚举</td><td>N/A</td><td>SOURCE(源端口、发送端口)或DESTINATION(目的端口、接收端口)</td></tr><tr><td>MaxNbMessage</td><td>一个端口可以同时处理的最大消息数量,最大队列深度</td><td>十进制或十六进制</td><td>个</td><td>1至512</td></tr><tr><td>ExtendedPort</td><td>是否使用SAP扩展服务,是否为SAP端口</td><td>布尔</td><td>N/A</td><td>非必须属性,若未填写,则CPM_GEN工具认为其值为false</td></tr><tr><td>ProtocolType</td><td>协议类型</td><td>十进制或十六进制</td><td>N/A</td><td>2(NOT_APPLICABLE,不适用)或1(RECEIVER_DISCARD,接收方丢弃)或0(SENDER_BLOCK,发送方阻塞)</td></tr><tr><td>Attribute</td><td>端口类型</td><td>枚举</td><td>N/A</td><td>NULL_PORT(空)或PSEUDO_PORT(虚端口)非必须属性,若未填写,则CPM_GEN工具认为其值为PSEUDO_PORT</td></tr><tr><td>DriverName</td><td>端口驱动名称</td><td>字符串</td><td>N/A</td><td>1~255字符</td></tr></table>

## 5.5.2.1.1.2 SamplingPort-采样虚端口

SamplingPort 元素的属性及约束条件如表 38：


表 38 SamplingPort 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>Name</td><td>端口名称</td><td>字符串</td><td>N/A</td><td>1至30个字符,根元素内必须唯一CXF项目ACPS中约定:发送端口:pseudoport_tx_【端口号】_【端口序号】接收端口:pseudoport_rx_【端口号】_【端口序号】其中【端口号】为6位以内的数字,【端口序号】为2位以内的数字。</td></tr><tr><td>MaxMessageSi</td><td>端口发送的最</td><td>十进制或</td><td>N/A</td><td>1至0x100000</td></tr><tr><td>ze</td><td>大消息长度</td><td>十六进制</td><td></td><td></td></tr><tr><td>Direction</td><td>端口是输入还是输出数据</td><td>枚举</td><td>N/A</td><td>SOURCE(源端口、发送端口)或DESTINATION(目的端口、接收端口)</td></tr><tr><td>Attribute</td><td>端口类型</td><td>枚举</td><td>N/A</td><td>NULL_PORT(空)或PSEUDO_PORT(虚端口)非必须属性,若未填写,则CPM_GEN工具认为其值为PSEUDO_PORT</td></tr><tr><td>RefreshRate</td><td>消息在端口的更新频率,刷新率</td><td>十进制或十六进制或枚举值(仅INFINITE_TIME)</td><td>nS</td><td>1至9223372036854775807或INFINITE_TIME(-1)</td></tr><tr><td>DriverName</td><td>端口驱动名称</td><td>字符串</td><td>N/A</td><td>1~255字符</td></tr></table>

## 5.6 Schedules-调度表

Schedules 元素包含的子元素及约束条件如表 39：


表 39 Schedules 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>Schedule</td><td>256</td><td>1</td></tr></table>

## 5.6.1 Schedule-调度

Schedule 元素的属性及约束条件如表 40：


表 40 Schedule 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>ScheduleName</td><td>调度名称</td><td>字符串</td><td>N/A</td><td>1至30个字符,根元素内必须唯一</td></tr><tr><td>ScheduleIdentifier</td><td>调度号</td><td>十进制或十六进制</td><td>N/A</td><td>0至255,根元素内必须唯一</td></tr><tr><td>InitialModuleSchedule</td><td>是否为初始化调度</td><td>布尔</td><td>N/A</td><td>N/A</td></tr></table>


Schedule元素包含的子元素及约束条件如表<sub>41</sub>：



表 41 Schedule 元素包含的子元素及约束条件


<table><tr><td>属性名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>PartitionWindow</td><td>N/A</td><td>1</td></tr></table>


5.6.1.1 PartitionWindow-分区窗口



PartitionWindow 元素包含的属性及约束关系如表 42：



表 42 PartitionWindow 的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>PartitionNameRef</td><td>该时间片内运行的分区名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/Partitions/Partition/@Name的定义匹配,或为SPARE.</td></tr><tr><td>Duration</td><td>分区窗口持续时间</td><td>十进制或十六进制</td><td>nS</td><td>1至9223372036854775807</td></tr><tr><td>Offset</td><td>分区窗口起始偏移</td><td>十进制或十六进制</td><td>nS</td><td>1至9223372036854775807</td></tr><tr><td>PeriodicProcessingStart</td><td>决定分区窗口的开始是否为一个周</td><td>布尔</td><td>N/A</td><td>N/A</td></tr><tr><td></td><td>期进程释放点。</td><td></td><td></td><td></td></tr></table>

## 5.7 Connections-通信链接

Connections 元素包含的子元素及约束条件如表 43：


表 43 Connections 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>Channel</td><td>N/A</td><td>0</td></tr></table>

## 5.7.1 Channel-通信通道

Channel 元素的属性及约束条件如表 44：


表 44 Channel 元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>Id</td><td>通信通道号</td><td>十进制或十六进制</td><td>N/A</td><td>根元素内必须唯一</td></tr></table>

298 Channel元素包含的子元素及约束条件如表<sub>45</sub>：


表 45 Channel 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>Source</td><td>1</td><td>1</td></tr><tr><td>Destination</td><td>N/A</td><td>1</td></tr></table>

## 5.7.1.1 Source-源端

Source 元素的的属性及约束条件如表 46：


表 46 Source元素的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>PartitionNameRef</td><td>所使用端口的应用所在的分区或CoreOS的名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/Partitions/Partition/@Name, /ConfigRecord/Module/Partitions/PseudoPartition/@Name或Module/CoreOS/CoreOSDescription/@KernelName.的定义匹配</td></tr><tr><td>PortNameRef</td><td>端口的名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/Applications/Application/ApplicationDescription/Ports/QueuingPort/@Name,或Module/Applications/Application/ApplicationDescription/Ports/SamplingPort/@Name或Module/Partitions/PseudoPartition/PseudoPartitionDescription/Ports/QueuingPort/@Name,或Module/Partitions/PseudoPartition/PseudoPartitionDescription/Ports/SamplingPort/@Name.的定义匹配</td></tr></table>

## 5.7.1.2 Destination-目的端

Destination 元素的的属性及约束条件如表 47：


表 47 Destination 元素的的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>PartitionNameRef</td><td>所使用端口的应用所在的分区或CoreOS的名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/Partitions/Partition/@Name或Module/Partitions/PseudoPartition/@Name或Module/CoreOS/CoreOSDescription/@KernelName.的定义匹配</td></tr><tr><td>PortNameRef</td><td>端口的名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/Applications/Application/ApplicationDescription/Ports/QueuingPort/@Name,或</td></tr><tr><td></td><td></td><td></td><td></td><td>Module/Applications/Application/ApplicationDescription/Ports/SamplingPort/@Name或Module/Partitions/PseudoPartition/PseudoPartitionDescription/Ports/QueuingPort/@Name,或Module/Partitions/PseudoPartition/PseudoPartitionDescription/Ports/SamplingPort/@Name.的定义匹配</td></tr></table>

## 5.8 LogBookList-日志清单

LogBookList 元素包含的子元素及约束条件如：


表 48 LogBookList 元素包含的子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>LogBook</td><td>N/A</td><td>1</td></tr></table>

## 5.8.1 LogBook-日志

LogBook元素的属性及约束条件如：


表 49 LogBook 元素的属性及约束条件如


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>LogbookName</td><td>该日志记录本的名称</td><td>字符串</td><td>N/A</td><td>1至30个字符,根元素内必须唯一</td></tr><tr><td>MaxMessageSize</td><td>该日志记录本单条日志的最大长度</td><td>十进制或十六进制</td><td>N/A</td><td>0至0x10000</td></tr><tr><td>MaxNBLoggedMessages</td><td>该日志记录本可以容纳的最大消息数</td><td>十进制或十六进制</td><td>N/A</td><td>1至2048</td></tr><tr><td>MaxNBinProgressMessage</td><td>该日志记录本对应RAM缓冲的</td><td>十进制或十六进制</td><td>N/A</td><td>1至2048</td></tr><tr><td></td><td>最大消息数</td><td></td><td></td><td></td></tr><tr><td>PartitionNameRef</td><td>使用日志记录本的分区名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/Partitions/Partition/@Name的定义相匹配</td></tr><tr><td>serialNumber</td><td>日志记录本序号</td><td>十进制或十六进制</td><td>N/A</td><td>根元素内必须唯一</td></tr></table>

## 5.9 HealthMonitor-健康监控表

HealthMonitor 元素包含内容，由平台供应商提供，输入文件名称为HealthMonitor.xml，该文件由系统集成商审核。

其中 HealthMonitor.xml 中不需要包含任何 PartitionRef子元素，PartitionRef子元素由系统集成商在 Module/HealthMonitor/HMTable/PartitionHMTableList/PartitionHMTable 元素和 Module/HealthMonitor /HMTask/PartitionHMTaskList/PartitionHMTask元素中添加并建立分区引用。

PartitionRef 元素的的属性及约束条件如表 50：


表 50 PartitionRef 元素的的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>Id</td><td>引用的分区 ID 号</td><td>十进制或十六进制</td><td>N/A</td><td>其 ID 号必须与Module/Partitions/Partition/@Identifier 相匹配</td></tr></table>

## 5.10 Payloads-载荷

Payloads元素所含子元素及约束条件见如表<sub>51</sub>：


表 51 Payloads 元素所含子元素及约束条件


<table><tr><td>子元素名称</td><td>最大出现次数</td><td>最小出现次数</td></tr><tr><td>ConfigRecordPayload</td><td>无限制</td><td>1</td></tr><tr><td>CoreOSPayload</td><td>1</td><td>1</td></tr><tr><td>PartitionPayload</td><td>N/A</td><td>0</td></tr><tr><td>SharedDataPayload</td><td>N/A</td><td>0</td></tr><tr><td>SharedLibraryPayload</td><td>N/A</td><td>1</td></tr></table>

## 5.10.1 ConfigRecordPayload-配置记录载荷

ConfigRecordPayload 元素所含的属性及约束条件如表 52：


表 52 ConfigRecordPayload 元素所含的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>NameRef</td><td>配置记录镜像的名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/CoreOS/CoreOSDescription/HardwareConfiguration/PhysicalMemory/userConfigRecordRegion/@Name定义相匹配或为 configRecord</td></tr><tr><td>Base_Address</td><td>配置记录镜像的起始物理地址</td><td>十进制或十六进制</td><td>Byte</td><td>其大小必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit.的非负整数倍,非必须属性,若未填写,则由 CPM_GEN 工具根据模块默认值定义</td></tr></table>

## 5.10.2 CoreOSPayload-核心操作系统载荷

CoreOSPayload 元素所含的属性及约束条件如表 53：


表 53 CoreOSPayload 元素所含的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>NameRef</td><td>CoreOS 镜像的名称</td><td>字符串</td><td>N/A</td><td>取值与 CoreOS.xml 内 CoreOS 镜像的名称相匹配,</td></tr><tr><td></td><td></td><td></td><td></td><td>非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>Base_Address</td><td>CoreOS镜像的起始物理地址</td><td>十进制或十六进制</td><td>Byte</td><td>其大小必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit.的非负整数倍,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr></table>

## <sub>330</sub> 5.10.3 PartitionPayload-分区载荷

PartitionPayload 元素所含的属性及约束条件如表 54：


表 54 PartitionPayload 元素所含的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>NameRef</td><td>分区镜像的名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/Partitions/Partition/@Name.定义相匹配</td></tr><tr><td>Base_Address</td><td>分区镜像的起始物理地址</td><td>十进制或十六进制</td><td>Byte</td><td>其大小必须是Module/CoreOS/CoreOSDescription/KernelConfiguration/@addressSpaceRegionAllocationUnit.的非负整数倍,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr><tr><td>Online</td><td>分区是否为在线加载分区</td><td>布尔</td><td>N/A</td><td>非必须属性,若未填写,则由CPM_GEN工</td></tr><tr><td></td><td></td><td></td><td></td><td>具根据模块默认值定义</td></tr></table>

## 5.10.4 SharedLibraryPayload-共享库载荷

SharedLibraryPayload 元素所含的属性及约束条件如表 55：


表 55 SharedLibraryPayload 元素所含的属性及约束条件


<table><tr><td>属性名称</td><td>属性描述</td><td>数据类型</td><td>单位</td><td>约束</td></tr><tr><td>NameRef</td><td>公共库镜像的名称</td><td>字符串</td><td>N/A</td><td>其名称必须与Module/SharedLibraryRegions/SharedLib rary/@Name.定义相匹配</td></tr><tr><td>Base_Addre ss</td><td>共享库镜像的起始物理地址</td><td>十进制或十六进制</td><td>Byte</td><td>其大小必须是Module/CoreOS/CoreOSDescription/Ke nelConfiguration/@addressSpaceRegion AllocationUnit.的非负整数倍,非必须属性,若未填写,则由CPM_GEN工具根据模块默认值定义</td></tr></table>