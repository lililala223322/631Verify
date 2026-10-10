/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PPSConfigGenerator64big.java
 * 
 * 描述    : 生成PPSConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64big.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.Partition;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: PPS调度配置二进制生成器 ( 32位大端)。
 */
public class PPSConfigGenerator64big {

    /**
     * generateSinglePPSConfig
     *
     * @brief
     * 功能: 生成单个PPS调度配置数据存储结构的二进制数据。
     *
     * @param[in]  partition: 分区对象
     * @return 单个PPS调度配置数据存储结构的二进制数据
     * @throws IOException IO异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generateSinglePPSConfig(Partition partition) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 获取Settings配置
        Partition.Settings settings = partition.getfSettings();
        if (settings == null) {
            // 如果没有Settings配置，使用默认值
            settings = new Partition.Settings();
        }

        // 字段 1: 是否允许空闲时占用标记(4字节)
        // 当appsIdleRelinquishEnabled属性值为false时，取值为0；当属性值为true时，取值为1
        boolean appsIdleRelinquishEnabled = settings.isfAppsIdleRelinquishEnabled();
        int idleRelinquishFlag = appsIdleRelinquishEnabled ? 1 : 0;
        baos.write(BinaryUtils.intToBytesBigEndian(idleRelinquishFlag));

        // 字段 2: PPS优先级(4字节)
        // appsPriority属性值
        int appsPriority = settings.getfAppsPriority();
        baos.write(BinaryUtils.intToBytesBigEndian(appsPriority));

        return baos.toByteArray();
    }

    /**
     * generatePPSConfigs
     *
     * @brief
     * 功能: 生成所有PPS调度配置数据存储结构的二进制数据。
     *
     * @param[in]  module: 模块对象
     * @param[in]  fieldMap: 字段映射，用于记录生成的二进制内容
     * @return 所有PPS调度配置数据存储结构的二进制数据
     * @throws IOException IO异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generatePPSConfigs(Module module, Map<String, byte[]> fieldMap) throws IOException {
        if (module.getfPartitions() == null || 
            module.getfPartitions().isEmpty()) {
            return new byte[0]; // 如果没有分区，返回空数组
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Map<String, Partition> partitions = module.getfPartitions();

        int configIndex = 0;
        // 按照分区顺序依次生成PPS调度配置
        for (Map.Entry<String, Partition> partitionEntry : partitions.entrySet()) {
            String partitionName = partitionEntry.getKey();
            Partition partition = partitionEntry.getValue();

            if (partition.isFIsPseudoPartition()) {
                continue;
            }

            // 生成单个PPS调度配置数据存储结构
            byte[] singleConfigBytes = generateSinglePPSConfig(partition);
            baos.write(singleConfigBytes);

            // 记录到fieldMap中，便于后续对比
            String fieldKey = String.format("PPSConfig_%s_%d", partitionName, configIndex);
            fieldMap.put(fieldKey, singleConfigBytes);

            configIndex++;
        }

        byte[] allConfigsBytes = baos.toByteArray();
        
        return allConfigsBytes;
    }

    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算PPS调度配置数据存储结构的总大小。
     *
     * @param[in]  module: 模块对象
     * @return 配置大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(Module module) {
        if (module.getfPartitions() == null || 
            module.getfPartitions().isEmpty()) {
            return 0;
        }

        // 每个PPS调度配置数据存储结构的固定大小：
        // 是否允许空闲时占用标记(4字节) + PPS优先级(4字节) = 8字节
        int singleConfigSize = 4 + 4;
        
        // 计算非伪分区的数量
        int partitionCount = 0;
        for (Partition partition : module.getfPartitions().values()) {
            if (!partition.isFIsPseudoPartition()) {
                partitionCount++;
            }
        }
        
        return partitionCount * singleConfigSize;
    }

    /**
     * calculateSingleSize
     *
     * @brief
     * 功能: 计算单个PPS调度配置数据存储结构的大小。
     *
     * @return 单个结构的大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSingleSize() {
        // 是否允许空闲时占用标记(4字节) + PPS优先级(4字节) = 8字节
        return 4 + 4;
    }
}

