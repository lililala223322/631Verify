/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: ScheduleConfigGenerator32big.java
 * 
 * 描述    : 生成ScheduleConfig相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.generation.generator32big.CPMConfig;

import com.nwpu.verify.entity.*;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: 调度表配置生成器负责生成调度表配置数据结构的二进制内容。
 */
public class ScheduleConfigGenerator32big {

    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算调度表配置结构的大小。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 包含配置信息的Module对象
     * @return 调度表配置结构的总大小
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(com.nwpu.verify.entity.Module module) {
        Map<String, Schedule> schedules = module.getfSchedules();
        if (schedules == null || schedules.isEmpty()) {
            return 0;
        }

        // 计算单个调度表结构的基础大小 (32位系统)：4 + 4 + 32 + 4 + 4 + 4 + 4 = 56 bytes
        final int BASE_SCHEDULE_STRUCT_SIZE = 4 + 4 + 32 + 4 + 4 + 4 + 4;
        // 8字节对齐后的大小
        final int SINGLE_SCHEDULE_STRUCT_SIZE = alignTo8Bytes(BASE_SCHEDULE_STRUCT_SIZE);

        // 计算有效调度表的数量
        int validScheduleCount = 0;
        for (Schedule schedule : schedules.values()) {
            if (schedule != null) {
                validScheduleCount++;
            }
        }

        return validScheduleCount * SINGLE_SCHEDULE_STRUCT_SIZE;
    }

    /**
     * generate
     *
     * @brief
     * 功能: 生成所有调度表配置结构的二进制内容。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 包含所有配置信息的Module对象
     * @param[in]  fieldMap:                      用于记录字段映射
     * @param[in]  cpmStartAddress:               CPM配置信息可加载文件的起始地址
     * @param[in]  mainFrameOffset:               主时间框架配置结构在文件中的偏移。。
     * @return 所有调度表配置结构的完整二进制字节数组
     * @throws IOException 写入流时可能发生异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generate(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap,
            long cpmStartAddress, long mainFrameOffset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Map<String, Schedule> schedules = module.getfSchedules();
        if (schedules == null || schedules.isEmpty()) {
            return baos.toByteArray(); // 如果没有调度表，则返回空字节数组
        }

        // 计算单个调度表结构的基础大小 (32位系统)：4 + 4 + 32 + 4 + 4 + 4 + 4 = 56 bytes
        final int BASE_SCHEDULE_STRUCT_SIZE = 4 + 4 + 32 + 4 + 4 + 4 + 4;
        // 8字节对齐后的大小
        final int SINGLE_SCHEDULE_STRUCT_SIZE = alignTo8Bytes(BASE_SCHEDULE_STRUCT_SIZE);

        // 遍历XML中所有的 Schedule 元素，按XML文件中的出现顺序生成数据结构
        // LinkedHashMap保持插入顺序，即XML解析顺序
        // 这与buildScheduleConfigOffsets中的顺序保持一致
        int idx = 0;
        long currentWindowOffset = 0; // 当前Schedule在主时间框架区域内的窗口偏移（累计前面所有Schedule的窗口大小）

        for (Schedule schedule : schedules.values()) {
            String baseFieldName = "Schedule_" + idx + "_"; // 用于fieldMap的前缀

            // 字段 1: 配置类型 (固定值)
            int configType = 1129468934;
            byte[] configTypeBytes = BinaryUtils.intToBytesBigEndian(configType);
            baos.write(configTypeBytes);
            fieldMap.put(baseFieldName + "ConfigType", configTypeBytes);

            // 字段 2: 配置数据大小 (当前结构的大小)
            int configSize = SINGLE_SCHEDULE_STRUCT_SIZE;
            byte[] configSizeBytes = BinaryUtils.intToBytesBigEndian(configSize);
            baos.write(configSizeBytes);
            fieldMap.put(baseFieldName + "ConfigSize", configSizeBytes);

            // 字段 3: 调度表名称(32字节定长)
            byte[] scheduleNameBytes = BinaryUtils.fixedLengthAscii(new String(schedule.getScheduleName()), 32, '\0');
            baos.write(scheduleNameBytes);
            fieldMap.put(baseFieldName + "ScheduleName", scheduleNameBytes);

            // 字段 4: 主时间框架指针 (4字节，32位系统)
            // 计算方式: CPM起始地址 + 主时间框架配置存储结构在文件中的偏移 + 当前Schedule的窗口在主时间框架区域内的偏移
            // MainTimeFrameGenerator按Schedule顺序连续生成所有窗口，每个窗口16字节
            long mainFramePtr = cpmStartAddress + mainFrameOffset + currentWindowOffset;
            byte[] mainFramePtrBytes = BinaryUtils.intToBytesBigEndian((int) mainFramePtr);
            baos.write(mainFramePtrBytes);
            fieldMap.put(baseFieldName + "MainFramePtr", mainFramePtrBytes);

            // 字段 5: 时间窗口数目
            // 计算方式: 当前调度表的主时间框架的个数（即PartitionWindow的数量）
            int windowCount = (schedule.getPartitionWindows() != null) ? schedule.getPartitionWindows().size() : 0;
            byte[] windowCountBytes = BinaryUtils.intToBytesBigEndian(windowCount);
            baos.write(windowCountBytes);
            fieldMap.put(baseFieldName + "WindowCount", windowCountBytes);

            // 字段 6: 调度表绑定的处理器核号
            // 当BmpSchedule元素存在时取值为分区所在Core元素的cpuId；不存在时取固定值0
            int coreNumber = schedule.getCoreNumber();
            byte[] coreNumberBytes = BinaryUtils.intToBytesBigEndian(coreNumber);
            baos.write(coreNumberBytes);
            fieldMap.put(baseFieldName + "CoreNumber", coreNumberBytes);

            // 字段 7: 缺省调度表标志
            // 根据 @InitialModuleSchedule 的布尔值决定
            int defaultFlag = schedule.isInitialModuleSchedule() ? 1 : 0;
            byte[] defaultFlagBytes = BinaryUtils.intToBytesBigEndian(defaultFlag);
            baos.write(defaultFlagBytes);
            fieldMap.put(baseFieldName + "DefaultFlag", defaultFlagBytes);

            // 字段 8: 对齐填充
            // 动态计算需要的对齐填充字节数
            addAlignmentPadding(baos, fieldMap, baseFieldName + "AlignmentPadding");

            // 更新下一个Schedule的窗口偏移
            // 每个PartitionWindow占16字节 (8字节Duration + 4字节PartitionId + 4字节PeriodProcessing)
            currentWindowOffset += windowCount * 16;

            idx++;
        }

        return baos.toByteArray();
    }

    /**
     * alignTo8Bytes
     *
     * @brief
     * 功能: 将大小对齐到8字节边界。
     *
     * @param[in]  size: 原始大小
     * @return 对齐后的大小
     * @scope internal
     * @errno:
     * N/A
     */
    private static int alignTo8Bytes(int size) {
        return (size + 7) & ~7; // 向上对齐到8字节边界
    }

    /**
     * addAlignmentPadding
     *
     * @brief
     * 功能: 添加8字节对齐填充。
     *
     * @param[in]  baos:      字节输出流
     * @param[in]  fieldMap:  字段映射
     * @param[in]  fieldName: 填充字段名称
     * @throws IOException IO异常
     * @scope internal
     * @errno:
     * N/A
     */
    private static void addAlignmentPadding(ByteArrayOutputStream baos, Map<String, byte[]> fieldMap, String fieldName)
            throws IOException {
        int currentSize = baos.size();
        int alignedSize = alignTo8Bytes(currentSize);
        int paddingSize = alignedSize - currentSize;

        if (paddingSize > 0) {
            byte[] paddingBytes = new byte[paddingSize];
            baos.write(paddingBytes);
            fieldMap.put(fieldName, paddingBytes);
        }
    }

}
