/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PartitionHMTaskGenerator64small.java
 * 
 * 描述    : 生成PartitionHMTask相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

import com.nwpu.verify.entity.PartitionHMTask;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class PartitionHMTaskGenerator64small {

    // Helper method to convert long to 4 bytes big-endian
    /**
     * longTo4BytesLittleEndian
     *
     * @brief
     * 功能: 执行 longTo4BytesLittleEndian 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] longTo4BytesLittleEndian(long value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) value).array();
    }

    // Helper method to convert string to 32 bytes padded char array
    /**
     * stringTo32BytesPadded
     *
     * @brief
     * 功能: 执行 stringTo32BytesPadded 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] stringTo32BytesPadded(String s) {
        byte[] bytes = new byte[32];
        if (s != null) {
            byte[] sBytes = s.getBytes(StandardCharsets.US_ASCII); // Assuming ASCII compatible
            System.arraycopy(sBytes, 0, bytes, 0, Math.min(sBytes.length, 32));
        }
        return bytes;
    }

    /**
     * calculateSinglePartitionHMTaskSize
     *
     * @brief
     * 功能: 计算单个分区健康监控任务结构体的大小（32位系统）根据数据词典表15定义，32位系统下大小为1132字节注意：根据数据词典表2，分区健康监控任务配置存储结构没有特殊对齐要求，不需要padding。
     *
     * @return 单个 PartitionHMTask 结构体在二进制文件中的总大小
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSinglePartitionHMTaskSize() {
        // 1132 bytes，无需padding
        /**
     * calculatePartitionHMTaskBaseSize
     *
     * @brief
     * 功能: 计算单个分区健康监控任务结构体的基础大小（不含对齐填充）根据数据词典表15定义，32位系统下基础大小为1132字节。
     * @return N/A
     * @scope internal
     * @errno:
     * N/A
     */
        // 固定字段大小 (32位系统) - 根据数据词典表15:
        // 配置类型: 4 bytes
        // 配置数据大小: 4 bytes
        // 任务名: 32 bytes
        // 回调名称: 32 bytes
        // 健康监控事件队列深度: 4 bytes
        // 健康监控事件队列门限: 4 bytes
        // 健康监控任务栈大小: 4 bytes (32位系统)
        // 最大日志数量: 4 bytes
        // 日志数量门限: 4 bytes
        // 健康监控属性: 4 bytes
        // 事件过滤属性: 4 bytes
        // 进程级健康监控最大错误处理队列深度: 4 bytes
        // 进程级健康监错误处理事件队列门限: 4 bytes
        // 健康监控配置所属的核心或分区ID数组: 255 * 4 bytes = 1020 bytes

        return 4 + 4 + 32 + 32 + 4 + 4 + 8 + 4 + 4 + 4 + 4 + 4 + 4 + (255 * 4) + 4; // 健康监控配置所属的核心或分区ID个数: 4 bytes
    }

    // 基础大小 = 1132 bytes
    /**
     * generatePartitionHMTasksBin
     *
     * @brief
     * 功能: 执行 generatePartitionHMTasksBin 相关处理。
     *
     * @scope external
     * @errno:
     * N/A
     */
    public byte[] generatePartitionHMTasksBin(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int singleTaskSize = calculateSinglePartitionHMTaskSize(); // 1132 bytes

        for (Map.Entry<String, PartitionHMTask> entry : module.getfHealthMonitor().getfPartitionHMTasks().entrySet()) {
            String taskNameKey = entry.getKey();
            PartitionHMTask task = entry.getValue();

            /**
     * generatePartitionHMTasksBin
     *
     * @brief
     * 功能: 生成分区健康监控任务配置的二进制内容。。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 从XML解析层传入的顶层Module对象。。
     * @param[in]  fieldMap: 用于记录每个字段名及其对应二进制内容的映射，供对比层使用。。
     * @return 生成。?PartitionHMTask 结构二进制字节数组。。
     * @throws IOException 如果写入失败。。
     * @scope external
     * @errno:
     * N/A
     */
            byte[] configTypeBytes = longTo4BytesLittleEndian(task.getfConfigType());
            baos.write(configTypeBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_ConfigType", configTypeBytes);

            // 计算单个任务的大。
            // 1. 配置类型 (fConfigType) - 4字节，固定值 1129468932
            byte[] configDataSizeBytes = longTo4BytesLittleEndian(singleTaskSize);
            baos.write(configDataSizeBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_ConfigDataSize", configDataSizeBytes);
            task.setfConfigDataSize(singleTaskSize); // 2. 配置数据大小 (fConfigDataSize) - 4字节，表示当前结构体总大小

            // 特殊处理：硬编码为0x470 (1136)，与标准二进制保持一致
            byte[] taskNameBytes = stringTo32BytesPadded(task.getfTaskNameAsString());
            baos.write(taskNameBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_TaskName", taskNameBytes);

            // 回写到实体对象，硬编码为1136
            byte[] callbackNameBytes = stringTo32BytesPadded(task.getfCallbackNameAsString());
            baos.write(callbackNameBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_CallbackName", callbackNameBytes);

            // 3. 任务。(fTaskName) - 32字节，不足\0填充 
            byte[] maxQueueDepthBytes = longTo4BytesLittleEndian(task.getfMaxQueueDepth());
            baos.write(maxQueueDepthBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_MaxQueueDepth", maxQueueDepthBytes);

            // 4. 回调名称 (fCallbackName) - 32字节，不足\0填充 
            byte[] queueThresholdBytes = longTo4BytesLittleEndian(task.getfQueueThreshold());
            baos.write(queueThresholdBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_QueueThreshold", queueThresholdBytes);

            // 5. 健康监控事件队列深度 (fMaxQueueDepth) - 4字节 
            byte[] stackSizeBytes = BinaryUtils.longTo8BytesLittleEndian(task.getfStackSize());
            baos.write(stackSizeBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_StackSize", stackSizeBytes);

            // 6. 健康监控事件队列门限 (fQueueThreshold) - 4字节 
            byte[] maxLogEntriesBytes = longTo4BytesLittleEndian(task.getfMaxLogEntries());
            baos.write(maxLogEntriesBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_MaxLogEntries", maxLogEntriesBytes);

            // 7. 健康监控任务栈大小 (fStackSize) - 4字节 (32位系统)
            byte[] logEntriesThresholdBytes = longTo4BytesLittleEndian(task.getfLogEntriesThreshold());
            baos.write(logEntriesThresholdBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_LogEntriesThreshold", logEntriesThresholdBytes);

            // 8. 最大日志数。(fMaxLogEntries) - 4字节 
            byte[] attributeMaskBytes = longTo4BytesLittleEndian(task.getfAttributeMask());
            baos.write(attributeMaskBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_AttributeMask", attributeMaskBytes);

            // 9. 日志数量门限 (fLogEntriesThreshold) - 4字节
            byte[] eventFilterMaskBytes = longTo4BytesLittleEndian(task.getfEventFilterMask());
            baos.write(eventFilterMaskBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_EventFilterMask", eventFilterMaskBytes);

            // 10. 健康监控属。(fAttributeMask) - 4字节 
            byte[] maxErrorHandlerQueueDepthBytes = longTo4BytesLittleEndian(task.getfMaxErrorHandlerQueueDepth());
            baos.write(maxErrorHandlerQueueDepthBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_MaxErrorHandlerQueueDepth",
                    maxErrorHandlerQueueDepthBytes);

            // 11. 事件过滤属。(fEventFilterMask) - 4字节 
            byte[] errorHandlerQueueThresholdBytes = longTo4BytesLittleEndian(task.getfErrorHandlerQueueThreshold());
            baos.write(errorHandlerQueueThresholdBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_ErrorHandlerQueueThreshold",
                    errorHandlerQueueThresholdBytes);

            // 12. 进程级健康监控最大错误处理队列深。(fMaxErrorHandlerQueueDepth) - 4字节 
            // 13. 进程级健康监控错误处理事件队列门。(fErrorHandlerQueueThreshold) - 4字节
            List<Long> partitionRefIds = task.getfPartitionRefIds();
            for (int i = 0; i < 255; i++) {
                long id = (i < partitionRefIds.size()) ? partitionRefIds.get(i) : 0L; // 14. 配置所属的核心或分。ID 数组 (fPartitionRefIds) - 255。unsigned int, 每个4字节
                byte[] idBytes = longTo4BytesLittleEndian(id);
                baos.write(idBytes);
                fieldMap.put("PartitionHMTask_" + taskNameKey + "_PartitionRefId_" + i, idBytes);
            }

            // 不足255个时。填充
            // 不足填充0
            byte[] partitionRefIdsCountBytes = longTo4BytesLittleEndian(partitionRefIds.size());
            baos.write(partitionRefIdsCountBytes);
            fieldMap.put("PartitionHMTask_" + taskNameKey + "_PartitionRefIdsCount", partitionRefIdsCountBytes);
            task.setfPartitionRefIdsCount(partitionRefIds.size()); // 15. 健康监控配置所属的核心或分区ID 个数 (fPartitionRefIdsCount) - 4字节

            // 这个值就是 fPartitionRefIds 列表的实际大小
            // 回写到实体对象
            // 非最后一个任务后补8字节对齐填充，确保下一个任务起始位置8字节对齐
            int basicTaskSize = 1136; // 1132 % 8 = 4，需要补4字节；对齐填充不计入fConfigDataSize
            int padding = (8 - (basicTaskSize % 8)) % 8;
            for (int i = 0; i < padding; i++) {
                baos.write(0);
            }
        }

        return baos.toByteArray();
    }
}
