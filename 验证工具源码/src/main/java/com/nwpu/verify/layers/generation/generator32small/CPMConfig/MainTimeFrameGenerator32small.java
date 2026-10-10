/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: MainTimeFrameGenerator32small.java
 * 
 * 描述    : 生成MainTimeFrame相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-25  开发人员  屏蔽未被需求规定的分区名称查找警告，保留默认分区ID处理。
 */
package com.nwpu.verify.layers.generation.generator32small.CPMConfig;


import com.nwpu.verify.entity.Schedule;
import com.nwpu.verify.entity.Schedule.PartitionWindow;
import com.nwpu.verify.entity.Partition;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: 主时间框架配置数据存储结构生成器 (32位大端)根据表6规范生成主时间框架配置数据存储结构。
 */
public class MainTimeFrameGenerator32small {

    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算主时间框架配置数据存储结构的总大小。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 包含所有配置信息的Module对象
     * @return 主时间框架配置结构在二进制文件中的总大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(com.nwpu.verify.entity.Module module) {
        int totalSize = 0;

        // 遍历所有调度表中的分区窗口
        Map<String, Schedule> schedules = module.getfSchedules();
        if (schedules != null) {
            for (Schedule schedule : schedules.values()) {
                if (schedule.getPartitionWindows() != null) {
                    int partitionWindowCount = schedule.getPartitionWindows().size();
                    // 每个PartitionWindow结构8字节(Duration) + 4字节(PartitionId) +
                    // 4字节(PeriodProcessingStart) = 16字节
                    totalSize += partitionWindowCount * 16;
                }
            }
        }

        return totalSize;
    }

    /**
     * generate
     *
     * @brief
     * 功能: 生成主时间框架配置数据存储结构的二进制内容。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 包含所有配置信息的Module对象
     * @param[in]  fieldMap:                      用于记录每个字段名及其对应二进制内容的映射
     * @return 生成的主时间框架配置结构二进制字节数组
     * @throws IOException 如果写入失败
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generate(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int windowIndex = 0; // 用于标识窗口索引

        // 按照PartitionWindow元素在Module/Schedules/Schedule中的先后顺序依次存储
        Map<String, Schedule> schedules = module.getfSchedules();
        if (schedules != null) {
            for (Schedule schedule : schedules.values()) {
                if (schedule.getPartitionWindows() != null) {
                    for (PartitionWindow partitionWindow : schedule.getPartitionWindows()) {

                        // 1. 时间窗口持续时间 (signed long long, 8字节)
                        // 映射的XML: /Module/Schedules/Schedule/PartitionWindow/@Duration
                        long duration = partitionWindow.getDuration();
                        byte[] durationBytes = BinaryUtils.longTo8BytesLittleEndian(duration);
                        baos.write(durationBytes);
                        fieldMap.put("MainTimeFrame_Window" + windowIndex + "_Duration", durationBytes);

                        // 2. 时间窗口执行的分区号 (int, 4字节)
                        // 映射的XML: /Module/Schedules/Schedule/PartitionWindow/@PartitionNameRef
                        // 计算方式: PartitionNameRef在分区配置中所对应的分区ID属性值Identifier
                        String partitionNameRef = partitionWindow.getPartitionNameRefAsString();
                        int partitionId = getPartitionIdByName(module, partitionNameRef);
                        byte[] partitionIdBytes = BinaryUtils.intToBytesLittleEndian(partitionId);
                        baos.write(partitionIdBytes);
                        fieldMap.put("MainTimeFrame_Window" + windowIndex + "_PartitionId", partitionIdBytes);

                        // 3. 周期进程释放点(unsigned int, 4字节)
                        // 映射的XML: /Module/Schedules/Schedule/PartitionWindow/@PeriodProcessingStart
                        // 计算方式: false时取0，true时取1
                        boolean periodProcessingStart = partitionWindow.isPeriodicProcessingStart();
                        int periodProcessingValue = periodProcessingStart ? 1 : 0;
                        byte[] periodProcessingBytes = BinaryUtils.intToBytesLittleEndian(periodProcessingValue);
                        baos.write(periodProcessingBytes);
                        fieldMap.put("MainTimeFrame_Window" + windowIndex + "_PeriodProcessingStart",
                                periodProcessingBytes);

                        windowIndex++;
                    }
                }
            }
        }

        return baos.toByteArray();
    }

    /**
     * getPartitionIdByName
     *
     * @brief
     * 功能: 根据分区名称获取分区ID。
     *
     * @param[in]  com:.nwpu.verify.entity.Module Module对象
     * @param[in]  partitionName:                 分区名称
     * @return 分区ID，如果找不到则返回0xFFFFFFFF
     * @scope internal
     * @errno:
     * N/A
     */
    private static int getPartitionIdByName(com.nwpu.verify.entity.Module module, String partitionName) {
        if (partitionName == null || partitionName.isEmpty()) {
            return 0xFFFFFFFF;
        }

        // 在分区配置中查找对应的分区ID
        Map<String, Partition> partitions = module.getfPartitions();
        if (partitions != null && partitions.containsKey(partitionName)) {
            Partition partition = partitions.get(partitionName);
            return partition.getfIdentifier();
        }

        // 如果找不到对应的分区，返回0xFFFFFFFF作为默认值
        return 0xFFFFFFFF;
    }

    /**
     * getTotalPartitionWindowCount
     *
     * @brief
     * 功能: 获取主时间框架配置中的总分区窗口数。
     *
     * @param[in]  com:.nwpu.verify.entity.Module Module对象
     * @return 总分区窗口数
     * @scope external
     * @errno:
     * N/A
     */
    public static int getTotalPartitionWindowCount(com.nwpu.verify.entity.Module module) {
        int totalCount = 0;

        Map<String, Schedule> schedules = module.getfSchedules();
        if (schedules != null) {
            for (Schedule schedule : schedules.values()) {
                if (schedule.getPartitionWindows() != null) {
                    totalCount += schedule.getPartitionWindows().size();
                }
            }
        }

        return totalCount;
    }

    /**
     * validateConfiguration
     *
     * @brief
     * 功能: 验证主时间框架配置的完整性。
     *
     * @param[in]  com:.nwpu.verify.entity.Module Module对象
     * @return 验证结果，true表示配置完整，false表示存在问题
     * @scope external
     * @errno:
     * N/A
     */
    public static boolean validateConfiguration(com.nwpu.verify.entity.Module module) {
        boolean isValid = true;

        Map<String, Schedule> schedules = module.getfSchedules();
        if (schedules != null) {
            for (Schedule schedule : schedules.values()) {
                if (schedule.getPartitionWindows() != null) {
                    for (PartitionWindow partitionWindow : schedule.getPartitionWindows()) {
                        // 检查Duration是否有效
                        if (partitionWindow.getDuration() <= 0) {
                            System.err.println("错误: 分区窗口Duration必须大于0，当前值: " + partitionWindow.getDuration());
                            isValid = false;
                        }

                        // 检查PartitionNameRef是否存在对应的分区
                        String partitionNameRef = partitionWindow.getPartitionNameRefAsString();
                        if (partitionNameRef == null || partitionNameRef.isEmpty()) {
                            System.err.println("错误: 分区窗口PartitionNameRef不能为空");
                            isValid = false;
                        } else {
                            Map<String, Partition> partitions = module.getfPartitions();
                            if (partitions == null || !partitions.containsKey(partitionNameRef)) {
                                System.err.println("错误: 分区窗口引用的'" + partitionNameRef + "' 不存在");
                                isValid = false;
                            }
                        }
                    }
                }
            }
        }

        return isValid;
    }
}
