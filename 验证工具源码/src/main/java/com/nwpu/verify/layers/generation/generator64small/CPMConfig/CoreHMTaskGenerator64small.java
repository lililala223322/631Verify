/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CoreHMTaskGenerator64small.java
 * 
 * 描述    : 生成CoreHMTask相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

import com.nwpu.verify.entity.CoreHMTask;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: 核心健康监控任务配置二进制生成器 32位大端。
 */
public class CoreHMTaskGenerator64small {

    // 固定值常量
    private static final int CONFIG_TYPE = 1129468932; // 配置类型固定值
    private static final int CORE_ID_ARRAY_SIZE = 255; // 核心或分区ID数组大小
    private static final int CORE_ID_COUNT = 1; // 核心或分区ID个数固定值

    // 默认值常量
    private static final int DEFAULT_MAX_QUEUE_DEPTH = 0;
    private static final int DEFAULT_QUEUE_THRESHOLD = 0;
    private static final int DEFAULT_STACK_SIZE = 0x1800;
    private static final int DEFAULT_MAX_LOG_ENTRIES = 0;
    private static final int DEFAULT_LOG_ENTRIES_THRESHOLD = 0;
    private static final int DEFAULT_ATTRIBUTE_MASK = 0;
    private static final int DEFAULT_EVENT_FILTER_MASK = 0;
    private static final int DEFAULT_MAX_ERROR_HANDLER_QUEUE_DEPTH = 0;
    private static final int DEFAULT_ERROR_HANDLER_QUEUE_THRESHOLD = 0;

    /**
     * generateBinary
     *
     * @brief
     * 功能: 生成单个核心健康监控任务的二进制数据。
     *
     * @param[in]  coreHMTask: 核心健康监控任务对象
     * @param[in]  fieldMap:   字段映射
     * @param[in]  taskName:   任务名称（用于字段命名）
     * @return 二进制字节数组
     * @throws IOException IO异常
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generateBinary(CoreHMTask coreHMTask, Map<String, byte[]> fieldMap, String taskName)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 创建一个临时的LinkedHashMap来保持字段顺序
        Map<String, byte[]> tempFieldMap = new java.util.LinkedHashMap<>();

        // 1. 配置类型 (4字节) - 固定值正整数"1129468932"
        byte[] configTypeBytes = BinaryUtils.intToBytesLittleEndian(CONFIG_TYPE);
        baos.write(configTypeBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_ConfigType", configTypeBytes);

        // 2. 配置数据大小 (4字节) - 先写0，后面会更新
        int configDataSizeOffset = baos.size();
        byte[] configDataSizePlaceholder = BinaryUtils.intToBytesLittleEndian(0); // 占位符
        baos.write(configDataSizePlaceholder);
        // 预留位置，稍后会用正确的值替换
        tempFieldMap.put("CoreHMTask_" + taskName + "_ConfigDataSize", configDataSizePlaceholder);

        // 3. 任务名(32字节) - Name属性值，不满32字节用0填充
        String taskNameValue = coreHMTask.getName() != null ? coreHMTask.getName() : "";
        byte[] taskNameBytes = BinaryUtils.fixedLengthAscii(taskNameValue, 32, '\0');
        baos.write(taskNameBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_TaskName", taskNameBytes);

        // 4. 回调名称 (32字节) - Callback属性值，不满32字节用0填充，默认值为空
        String callback = coreHMTask.getCallback() != null ? coreHMTask.getCallback() : "";
        byte[] callbackBytes = BinaryUtils.fixedLengthAscii(callback, 32, '\0');
        baos.write(callbackBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_Callback", callbackBytes);

        // 5. 健康监控事件队列深度 (4字节) - maxQueueDepth属性值，默认值为0
        int maxQueueDepth = coreHMTask.getMaxQueueDepth() != 0 ? coreHMTask.getMaxQueueDepth()
                : DEFAULT_MAX_QUEUE_DEPTH;
        byte[] maxQueueDepthBytes = BinaryUtils.intToBytesLittleEndian(maxQueueDepth);
        baos.write(maxQueueDepthBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_MaxQueueDepth", maxQueueDepthBytes);

        // 6. 健康监控事件队列门限 (4字节) - queueThreshold属性值，默认值为0
        int queueThreshold = coreHMTask.getQueueThreshold() != 0 ? coreHMTask.getQueueThreshold()
                : DEFAULT_QUEUE_THRESHOLD;
        byte[] queueThresholdBytes = BinaryUtils.intToBytesLittleEndian(queueThreshold);
        baos.write(queueThresholdBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_QueueThreshold", queueThresholdBytes);

        // 7. 健康监控任务栈大小 (4字节，32位系统) - stackSize属性值，默认值为0x1800
        int stackSize = coreHMTask.getStackSize() != 0 ? coreHMTask.getStackSize() : DEFAULT_STACK_SIZE;
        byte[] stackSizeBytes = BinaryUtils.intTo8BytesLittleEndian(stackSize);
        baos.write(stackSizeBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_StackSize", stackSizeBytes);

        // 8. 最大日志数量(4字节) - maxLogEntries属性值，默认值为0
        int maxLogEntries = coreHMTask.getMaxLogEntries() != 0 ? coreHMTask.getMaxLogEntries()
                : DEFAULT_MAX_LOG_ENTRIES;
        byte[] maxLogEntriesBytes = BinaryUtils.intToBytesLittleEndian(maxLogEntries);
        baos.write(maxLogEntriesBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_MaxLogEntries", maxLogEntriesBytes);

        // 9. 日志数量门限 (4字节) - LogEntriesThreshold属性值，默认值为0
        int logEntriesThreshold = coreHMTask.getLogEntriesThreshold() != 0 ? coreHMTask.getLogEntriesThreshold()
                : DEFAULT_LOG_ENTRIES_THRESHOLD;
        byte[] logEntriesThresholdBytes = BinaryUtils.intToBytesLittleEndian(logEntriesThreshold);
        baos.write(logEntriesThresholdBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_LogEntriesThreshold", logEntriesThresholdBytes);

        // 10. 健康监控属性(4字节) - attributeMask属性值，默认值为0
        int attributeMask = coreHMTask.getAttributeMask() != 0 ? coreHMTask.getAttributeMask() : DEFAULT_ATTRIBUTE_MASK;
        byte[] attributeMaskBytes = BinaryUtils.intToBytesLittleEndian(attributeMask);
        baos.write(attributeMaskBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_AttributeMask", attributeMaskBytes);

        // 11. 事件过滤属性(4字节) - eventFilterMask属性值，默认值为0
        int eventFilterMask = coreHMTask.getEventFilterMask() != 0 ? coreHMTask.getEventFilterMask()
                : DEFAULT_EVENT_FILTER_MASK;
        byte[] eventFilterMaskBytes = BinaryUtils.intToBytesLittleEndian(eventFilterMask);
        baos.write(eventFilterMaskBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_EventFilterMask", eventFilterMaskBytes);

        // 12. 进程级健康监控最大错误处理队列深度(4字节) - maxErrorHandlerQueueDepth属性值，默认值为0
        int maxErrorHandlerQueueDepth = coreHMTask.getMaxErrorHandlerQueueDepth() != 0
                ? coreHMTask.getMaxErrorHandlerQueueDepth()
                : DEFAULT_MAX_ERROR_HANDLER_QUEUE_DEPTH;
        byte[] maxErrorHandlerQueueDepthBytes = BinaryUtils.intToBytesLittleEndian(maxErrorHandlerQueueDepth);
        baos.write(maxErrorHandlerQueueDepthBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_MaxErrorHandlerQueueDepth", maxErrorHandlerQueueDepthBytes);

        // 13. 进程级健康监错误处理事件队列门限 (4字节) - errorHandlerQueueThreshold属性值，默认值为0
        int errorHandlerQueueThreshold = coreHMTask.getErrorHandlerQueueThreshold() != 0
                ? coreHMTask.getErrorHandlerQueueThreshold()
                : DEFAULT_ERROR_HANDLER_QUEUE_THRESHOLD;
        byte[] errorHandlerQueueThresholdBytes = BinaryUtils.intToBytesLittleEndian(errorHandlerQueueThreshold);
        baos.write(errorHandlerQueueThresholdBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_ErrorHandlerQueueThreshold", errorHandlerQueueThresholdBytes);

        // 14. 健康监控配置所属的核心或分区ID数组 (4*255字节)
        // 优先使用XML中配置的Core/cpuId（已解析到fCoreRefIds），其余补0
        byte[] coreIdArrayBytes = new byte[CORE_ID_ARRAY_SIZE * 4];
        java.util.List<Long> coreIds = coreHMTask.getfCoreRefIds();
        for (int i = 0; i < CORE_ID_ARRAY_SIZE; i++) {
            int id = 0;
            if (coreIds != null && i < coreIds.size()) {
                id = coreIds.get(i).intValue();
            }
            byte[] idBytes = BinaryUtils.intToBytesLittleEndian(id);
            System.arraycopy(idBytes, 0, coreIdArrayBytes, i * 4, 4);
        }
        baos.write(coreIdArrayBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_CoreIdArray", coreIdArrayBytes);

        // 15. 健康监控配置所属的核心或分区ID个数 (4字节) - 固定值1
        byte[] coreIdCountBytes = BinaryUtils.intToBytesLittleEndian(CORE_ID_COUNT);
        baos.write(coreIdCountBytes);
        tempFieldMap.put("CoreHMTask_" + taskName + "_CoreIdCount", coreIdCountBytes);

        // 8字节对齐
        int currentSize = baos.size();
        int padding = (8 - (currentSize % 8)) % 8;
        if (padding > 0) {
            byte[] paddingBytes = new byte[padding];
            baos.write(paddingBytes);
            tempFieldMap.put("CoreHMTask_" + taskName + "_Padding", paddingBytes);
        }

        // 更新配置数据大小字段
        byte[] result = baos.toByteArray();
        int configDataSize = result.length;
        byte[] configDataSizeBytes = BinaryUtils.intToBytesLittleEndian(configDataSize);
        System.arraycopy(configDataSizeBytes, 0, result, configDataSizeOffset, 4);
        // 更新 tempFieldMap 中的配置数据大小值
        tempFieldMap.put("CoreHMTask_" + taskName + "_ConfigDataSize", configDataSizeBytes);

        // 将所有字段按顺序添加到最终的 fieldMap 中
        fieldMap.putAll(tempFieldMap);

        return result;
    }

    /**
     * generateCoreHMTaskConfigs
     *
     * @brief
     * 功能: 生成所有核心健康监控任务配置的二进制内容（用于OsConfigGenerator64small集成）。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 模块对象
     * @param[in]  fieldMap:                      字段映射
     * @return 二进制字节数组
     * @scope external
     * @errno:
     * N/A
     */
    public static byte[] generateCoreHMTaskConfigs(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap)
            throws IOException {
        if (module.getfHealthMonitor() == null || module.getfHealthMonitor().getfCoreHMTasks() == null ||
                module.getfHealthMonitor().getfCoreHMTasks().isEmpty()) {
            return new byte[0]; // 如果没有核心健康监控任务，返回空数组
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Map<String, CoreHMTask> coreHMTasks = module.getfHealthMonitor().getfCoreHMTasks();

        // 按照CoreHMTask在CoreHMTaskList下的先后顺序依次生成
        for (Map.Entry<String, CoreHMTask> entry : coreHMTasks.entrySet()) {
            String taskName = entry.getKey();
            CoreHMTask coreHMTask = entry.getValue();

            byte[] taskData = generateBinary(coreHMTask, fieldMap, taskName);
            baos.write(taskData);
        }

        byte[] allTasksData = baos.toByteArray();
        // fieldMap.put("CoreHMTaskConfigs", allTasksData);

        return allTasksData;
    }

    /**
     * calculateSize
     *
     * @brief
     * 功能: 计算核心健康监控任务配置的总大小。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 模块对象
     * @return 配置大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSize(com.nwpu.verify.entity.Module module) {
        if (module.getfHealthMonitor() == null || module.getfHealthMonitor().getfCoreHMTasks() == null ||
                module.getfHealthMonitor().getfCoreHMTasks().isEmpty()) {
            return 0;
        }

        Map<String, CoreHMTask> coreHMTasks = module.getfHealthMonitor().getfCoreHMTasks();

        // 每个核心健康监控任务配置结构的固定大小
        int singleTaskSize = calculateSingleTaskSize();

        return coreHMTasks.size() * singleTaskSize;
    }

    /**
     * calculateSingleTaskSize
     *
     * @brief
     * 功能: 计算单个核心健康监控任务配置结构的大小。
     *
     * @return 单个任务配置大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static int calculateSingleTaskSize() {
        // 配置类型(4) + 配置数据大小(4) + 任务名(32) + 回调名称(32) +
        // 健康监控事件队列深度(4) + 健康监控事件队列门限(4) + 健康监控任务栈大小(4, 32位系统) +
        // 最大日志数量(4) + 日志数量门限(4) + 健康监控属性(4) + 事件过滤属性(4) +
        // 进程级健康监控最大错误处理队列深度(4) + 进程级健康监错误处理事件队列门限(4) +
        // 核心ID数组(4*255) + ID个数(4) = 1128字节
        int baseSize = 4 + 4 + 32 + 32 + 4 + 4 + 8 + 4 + 4 + 4 + 4 + 4 + 4 + (4 * CORE_ID_ARRAY_SIZE) + 4;

        // 8字节对齐（动态计算，与generateBinary保持一致）
        int padding = (8 - (baseSize % 8)) % 8;

        return baseSize + padding;
    }
}
