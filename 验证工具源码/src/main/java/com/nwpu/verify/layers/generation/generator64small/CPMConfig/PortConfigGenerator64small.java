/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PortConfigGenerator64small.java
 * 
 * 描述    : 生成PortConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

import com.nwpu.verify.entity.*;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;
import com.nwpu.verify.layers.generation.utils.PortUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @brief
 * 功能: 端口配置生成器负责生成端口配置数据结构的二进制内容。
 */
public class PortConfigGenerator64small {

    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算端口配置结构的大小。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 包含配置信息的Module对象
     * @return 端口配置结构的总大小
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(com.nwpu.verify.entity.Module module) {
        List<PortUtils.UnifiedPort> allPorts = PortUtils.gatherAllPorts(module);
        if (allPorts == null || allPorts.isEmpty()) {
            return 0; // 如果没有端口，则返回0
        }
        // 单个端口结构大小: 4*12 + 32 + 8 + 255 = 343
        final int SINGLE_PORT_STRUCT_SIZE = 343;
        // 8字节对齐，不足8字节补零
        final int ALIGNED_PORT_STRUCT_SIZE = BinaryUtils.alignTo8Bytes(SINGLE_PORT_STRUCT_SIZE);
        return allPorts.size() * ALIGNED_PORT_STRUCT_SIZE;
    }

    /**
     * generate
     *
     * @brief
     * 功能: 生成所有端口配置数据结构的二进制内容。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 包含所有配置信息的Module对象
     * @param[in]  fieldMap: 用于记录字段映射
     * @return 所有端口配置数据结构的完整二进制字节数组
     * @throws IOException 写入流时可能发生异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generate(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 步骤 1: 收集并排序所有有效端口
        List<PortUtils.UnifiedPort> allPorts = PortUtils.gatherAllPorts(module);

        // 步骤 2: 遍历统一后的端口列表，生成二进制
        final int SINGLE_PORT_STRUCT_SIZE = 343; // 4*12 + 32 + 8 + 255
        final int ALIGNED_PORT_STRUCT_SIZE = BinaryUtils.alignTo8Bytes(SINGLE_PORT_STRUCT_SIZE); // 8字节对齐

        for (int i = 0; i < allPorts.size(); i++) {
            PortUtils.UnifiedPort unifiedPort = allPorts.get(i);
            Port port = unifiedPort.getPort();
            String baseFieldName = "Port_" + i + "_";

            // 记录当前端口结构开始位置，用于计算填充
            int portStartPos = baos.size();

            // --- 开始生成15个字段---
            // 1. 配置类型
            baos.write(BinaryUtils.intToBytesLittleEndian(1129468929));
            fieldMap.put(baseFieldName + "ConfigType", BinaryUtils.intToBytesLittleEndian(1129468929));

            // 2. 配置数据大小
            baos.write(BinaryUtils.intToBytesLittleEndian(ALIGNED_PORT_STRUCT_SIZE));
            fieldMap.put(baseFieldName + "ConfigSize", BinaryUtils.intToBytesLittleEndian(ALIGNED_PORT_STRUCT_SIZE));

            // 3. 端口名(32字节, \0填充)
            byte[] portNameBytes = BinaryUtils.fixedLengthAscii(PortUtils.getPortName(port), 32, '\0');
            baos.write(portNameBytes);
            fieldMap.put(baseFieldName + "PortName", portNameBytes);

            // 4. 端口所属分区ID
            byte[] partitionIdBytes = BinaryUtils.intToBytesLittleEndian(unifiedPort.getParentPartitionId());
            baos.write(partitionIdBytes);
            fieldMap.put(baseFieldName + "PartitionId", partitionIdBytes);

            // 5. 端口模块ID
            byte[] moduleIdBytes = BinaryUtils.intToBytesLittleEndian(unifiedPort.getParentModuleId());
            baos.write(moduleIdBytes);
            fieldMap.put(baseFieldName + "ModuleId", moduleIdBytes);

            // 6. 端口所属通道ID
            byte[] channelIdBytes = BinaryUtils.intToBytesLittleEndian(unifiedPort.getChannelId());
            baos.write(channelIdBytes);
            fieldMap.put(baseFieldName + "ChannelId", channelIdBytes);

            // 7. 端口方向 (Source:0, Destination:1)
            byte[] directionBytes = BinaryUtils.intToBytesLittleEndian(PortUtils.mapDirection(port.getfDirection()));
            baos.write(directionBytes);
            fieldMap.put(baseFieldName + "Direction", directionBytes);

            // 8. 端口模式 (Queuing:0, Sampling:1)
            int portMode = (port instanceof QueuingPort) ? 0 : 1;
            byte[] portModeBytes = BinaryUtils.intToBytesLittleEndian(portMode);
            baos.write(portModeBytes);
            fieldMap.put(baseFieldName + "PortMode", portModeBytes);

            // 9. 端口协议类型
            byte[] protocolTypeBytes = BinaryUtils.intToBytesLittleEndian(PortUtils.getProtocolType(port));
            baos.write(protocolTypeBytes);
            fieldMap.put(baseFieldName + "ProtocolType", protocolTypeBytes);

            // 10. 消息大小
            byte[] messageSizeBytes = BinaryUtils.intToBytesLittleEndian((int) port.getfMaxMessageSize());
            baos.write(messageSizeBytes);
            fieldMap.put(baseFieldName + "MessageSize", messageSizeBytes);

            // 11. 消息数
            byte[] messageCountBytes = BinaryUtils.intToBytesLittleEndian(PortUtils.getMaxNbMessage(port));
            baos.write(messageCountBytes);
            fieldMap.put(baseFieldName + "MessageCount", messageCountBytes);

            // 12. 采样端口刷新频率 (8字节)
            long refreshRate = 0;
            if (port instanceof SamplingPort) {
                refreshRate = PortUtils.mapRefreshRate(((SamplingPort) port).getfRefreshRate());
            }
            byte[] refreshRateBytes = BinaryUtils.longTo8BytesLittleEndian(refreshRate);
            baos.write(refreshRateBytes);
            fieldMap.put(baseFieldName + "RefreshRate", refreshRateBytes);

            // 13. 保留字段 (固定值0)
            byte[] reservedBytes = BinaryUtils.intToBytesLittleEndian(0);
            baos.write(reservedBytes);
            fieldMap.put(baseFieldName + "Reserved", reservedBytes);

            // 14. 端口类型 (LOCAL, PSEUDO等)
            byte[] portAttributeBytes = BinaryUtils.intToBytesLittleEndian(PortUtils.mapPortAttribute(port));
            baos.write(portAttributeBytes);
            fieldMap.put(baseFieldName + "PortAttribute", portAttributeBytes);

            // 15. 虚端口设备名 (255字节, \0填充)
            byte[] driverNameBytes = BinaryUtils.fixedLengthAscii(PortUtils.getDriverName(port), 255, '\0');
            baos.write(driverNameBytes);
            fieldMap.put(baseFieldName + "DriverName", driverNameBytes);

            // 计算当前端口结构的实际大小
            int currentPortSize = baos.size() - portStartPos;

            // 计算需要填充的字节数以达到8字节对齐
            int paddingSize = ALIGNED_PORT_STRUCT_SIZE - currentPortSize;
            if (paddingSize > 0) {
                byte[] paddingBytes = new byte[paddingSize];
                // 填充字节全部为0
                baos.write(paddingBytes);
                fieldMap.put(baseFieldName + "Padding", paddingBytes);
            }
        }

        return baos.toByteArray();
    }
}
