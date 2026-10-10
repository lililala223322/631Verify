/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SharedDataRegionGenerator64small.java
 * 
 * 描述    : 生成Shared Data Region相关64位小端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */
package com.nwpu.verify.layers.generation.generator64small.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.SharedData;
import com.nwpu.verify.entity.CoreOS;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * @brief
 * 功能: Shared Data Region配置二进制生成器（64位小端）。
 */
public class SharedDataRegionGenerator64small {
    /**
     * alignTo8Bytes
     *
     * @brief
     * 功能: 执行alignTo8Bytes处理逻辑。
     *
     * @param[in]  size: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static int alignTo8Bytes(int size) {
        return (size + 7) & ~7;
    }
    /**
     * addAlignmentPadding
     *
     * @brief
     * 功能: 执行addAlignmentPadding处理逻辑。
     *
     * @param[in]  result: 参数
     * @param[in]  fieldMap: 参数
     * @param[in]  fieldName: 参数
     * @return
     * void: N/A
     * @scope internal
     * @errno:
     * N/A
     */
    private static void addAlignmentPadding(List<Byte> result, Map<String, byte[]> fieldMap, String fieldName) {
        int currentSize = result.size();
        int alignedSize = alignTo8Bytes(currentSize);
        int paddingSize = alignedSize - currentSize;
        if (paddingSize > 0) {
            byte[] paddingBytes = new byte[paddingSize];
            for (byte b : paddingBytes) {
                result.add(b);
            }
            if (fieldMap != null) {
                fieldMap.put(fieldName, paddingBytes);
            }
        }
    }

    // 配置类型固定值
    private static final int CONFIG_TYPE = 1129468935;

    // 对齐填充位固定值
    private static final short ALIGNMENT_PADDING = 0;
    /**
     * calculateTotalSize
     *
     * @brief
     * 功能: 执行calculateTotalSize处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public static int calculateTotalSize(Module module) {
        if (module == null || module.getfSharedDatas() == null || module.getfSharedDatas().isEmpty()) {
            return 0;
        }

        int totalSize = 0;
        int sharedDataCount = module.getfSharedDatas().size();
        totalSize = sharedDataCount * calculateSingleConfigSize();

        return totalSize;
    }
    /**
     * calculateSingleConfigSize
     *
     * @brief
     * 功能: 执行calculateSingleConfigSize处理逻辑。
     *
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateSingleConfigSize() {
        // 根据64位规范计算64位系统下的字段大小
        return 4 // 配置类型 (unsigned int)
                + 4 // 配置数据大小 (unsigned int)
                + 32 // 共享数据区名 (字符数组)
                + 38 // 共享数据区池(字符数组)
                + 2 // 对齐填充(unsigned short)
                + 8 // 共享数据区物理地址 (unsigned long long)
                + 8 // 共享数据区逻辑地址 (unsigned long long)
                + 8 // 共享数据区大小 (unsigned long long)
                + 4 // 共享数据区MMU属性(int)
                + 4; // 8字节对齐填充
    }
    /**
     * generateSharedDataRegionConfigs
     *
     * @brief
     * 功能: 执行generateSharedDataRegionConfigs处理逻辑。
     *
     * @param[in]  module: 参数
     * @param[in]  fieldMap: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    public static List<Byte> generateSharedDataRegionConfigs(Module module, Map<String, byte[]> fieldMap) {
        List<Byte> result = new ArrayList<>();

        if (module == null || module.getfSharedDatas() == null || module.getfSharedDatas().isEmpty()) {
            return result;
        }

        // 获取userMemoryRegion的基地址
        long baseAddress = getUserMemoryRegionBaseAddress(module);
        long currentPhysicalAddress = baseAddress;

        // 获取CoreOS中的SharedIO配置（用于SharedIODescription类型的SharedData）
        CoreOS.HardwareConfiguration.SharedIO coreOSSharedIO = null;
        if (module.getfCoreOS() != null &&
                module.getfCoreOS().getfHardwareConfiguration() != null &&
                module.getfCoreOS().getfHardwareConfiguration().getfSharedIO() != null) {
            coreOSSharedIO = module.getfCoreOS().getfHardwareConfiguration().getfSharedIO();
        }

        // 按照SharedData在Module/SharedDataRegions中的顺序处理
        for (Map.Entry<String, SharedData> entry : module.getfSharedDatas().entrySet()) {
            String sharedDataName = entry.getKey();
            SharedData sharedData = entry.getValue();

            // 判断是否为SharedIO类型
            boolean isSharedIO = sharedData.getfSharedIODescription() != null;

            // 对于SharedIODescription，使用CoreOS中的物理地址、虚拟地址和大小
            long physicalAddressForThisSharedData;
            long virtualAddressForThisSharedData;
            long sizeForThisSharedData;

            if (isSharedIO && coreOSSharedIO != null) {
                // SharedIO从CoreOS获取
                physicalAddressForThisSharedData = coreOSSharedIO.getfBaseAddress();
                virtualAddressForThisSharedData = sharedData.getfSharedIODescription().getVirtualAddressAsLong();
                if (virtualAddressForThisSharedData == 0L) {
                    virtualAddressForThisSharedData = coreOSSharedIO.getfVirtualAddress();
                }
                sizeForThisSharedData = coreOSSharedIO.getfSize();
            } else {
                // 普通SharedData使用链式地址
                physicalAddressForThisSharedData = currentPhysicalAddress;
                virtualAddressForThisSharedData = getVirtualAddress(sharedData);
                sizeForThisSharedData = getSharedDataSize(sharedData);
            }

            // 生成单个共享数据区配置并记录fieldMap
            List<Byte> configData = generateSingleSharedDataRegionConfig(
                    sharedDataName, sharedData, physicalAddressForThisSharedData,
                    virtualAddressForThisSharedData, sizeForThisSharedData, fieldMap);
            result.addAll(configData);

            // 更新下一个共享数据的物理地址（仅对非SharedIO类型）
            if (!isSharedIO) {
                currentPhysicalAddress += sizeForThisSharedData;
            }
        }

        return result;
    }

    /**
     * 生成单个共享数据区配置的二进制数据，并记录每个字段到fieldMap
     * 
     * @param sharedDataName  共享数据名称
     * @param sharedData      共享数据对象
     * @param physicalAddress 物理地址
     * @param virtualAddress  虚拟地址
     * @param size            大小
     * @param fieldMap        用于记录每个字段名及其对应二进制内容的映射
     * @return 二进制数据列表
     */
    private static List<Byte> generateSingleSharedDataRegionConfig(
            String sharedDataName, SharedData sharedData, long physicalAddress,
            long virtualAddress, long size, Map<String, byte[]> fieldMap) {
        List<Byte> result = new ArrayList<>();

        // 1. 配置类型 (4字节)
        byte[] configTypeBytes = BinaryUtils.intToBytesLittleEndian(CONFIG_TYPE);
        result.addAll(bytesToList(configTypeBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_ConfigType", configTypeBytes);

        // 2. 配置数据大小 (4字节)
        int configSize = calculateSingleConfigSize();
        byte[] configSizeBytes = BinaryUtils.intToBytesLittleEndian(configSize);
        result.addAll(bytesToList(configSizeBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_ConfigDataSize", configSizeBytes);

        // 3. 共享数据区名 (32字节)
        byte[] nameBytes = new byte[32];
        byte[] srcName = sharedDataName != null ? sharedDataName.getBytes(StandardCharsets.US_ASCII) : new byte[0];
        System.arraycopy(srcName, 0, nameBytes, 0, Math.min(srcName.length, 32));
        result.addAll(bytesToList(nameBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_Name", nameBytes);

        // 4. 共享数据区池名 (38字节)
        String poolName = sharedDataName + "Pool";
        byte[] poolNameBytes = new byte[38];
        byte[] srcPoolName = poolName.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(srcPoolName, 0, poolNameBytes, 0, Math.min(srcPoolName.length, 38));
        result.addAll(bytesToList(poolNameBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_PoolName", poolNameBytes);

        // 5. 对齐填充位 (2字节)
        byte[] alignmentBytes = shortToBytesLittleEndian(ALIGNMENT_PADDING);
        result.addAll(bytesToList(alignmentBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_StructPadding", alignmentBytes);

        // 6. 共享数据区物理地址 (8字节)
        byte[] physicalAddrBytes = BinaryUtils.longTo8BytesLittleEndian(physicalAddress);
        result.addAll(bytesToList(physicalAddrBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_PhysicalAddress", physicalAddrBytes);

        // 7. 共享数据区逻辑地址 (8字节，64位系统) - 使用传入的参数
        byte[] virtualAddrBytes = BinaryUtils.longTo8BytesLittleEndian(virtualAddress);
        result.addAll(bytesToList(virtualAddrBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_VirtualAddress", virtualAddrBytes);

        // 8. 共享数据区大小 (8字节，64位系统) - 使用传入的参数
        byte[] sizeBytes = BinaryUtils.longTo8BytesLittleEndian(size);
        result.addAll(bytesToList(sizeBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_Size", sizeBytes);

        // 9. 共享数据区MMU属性 (4字节)
        int mmuAttributes = calculateMMUAttributes(sharedData);
        byte[] mmuAttrBytes = BinaryUtils.intToBytesLittleEndian(mmuAttributes);
        result.addAll(bytesToList(mmuAttrBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_MMUAttributes", mmuAttrBytes);

        // 添加8字节对齐填充
        addAlignmentPadding(result, fieldMap, "SharedData_" + sharedDataName + "_AlignmentPadding");

        return result;
    }
    /**
     * getUserMemoryRegionBaseAddress
     *
     * @brief
     * 功能: 执行getUserMemoryRegionBaseAddress处理逻辑。
     *
     * @param[in]  module: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static long getUserMemoryRegionBaseAddress(Module module) {
        try {
            if (module.getfCoreOS() != null &&
                    module.getfCoreOS().getfHardwareConfiguration() != null &&
                    module.getfCoreOS().getfHardwareConfiguration().getfUserMemoryRegion() != null) {

                // 获取userMemoryRegion的基地址
                CoreOS.HardwareConfiguration.UserMemoryRegion userMemoryRegion = module.getfCoreOS()
                        .getfHardwareConfiguration().getfUserMemoryRegion();
                return userMemoryRegion.getfBaseAddress();
            }
        } catch (Exception e) {
            System.err.println("警告：获取userMemoryRegion基地址失败 - " + e.getMessage());
        }

        return 0L; // 默认基地址
    }
    /**
     * getVirtualAddress
     *
     * @brief
     * 功能: 执行getVirtualAddress处理逻辑。
     *
     * @param[in]  sharedData: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static long getVirtualAddress(SharedData sharedData) {
        try {
            if (sharedData.getfSharedDataDescription() != null) {
                return sharedData.getfSharedDataDescription().getVirtualAddressAsLong();
            }
        } catch (Exception e) {
            System.err.println("警告：获取虚拟地址失败 - " + e.getMessage());
        }

        return 0L; // 默认值
    }
    /**
     * getSharedDataSize
     *
     * @brief
     * 功能: 执行getSharedDataSize处理逻辑。
     *
     * @param[in]  sharedData: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static long getSharedDataSize(SharedData sharedData) {
        try {
            if (sharedData.getfSharedDataDescription() != null) {
                return sharedData.getfSharedDataDescription().getSizeAsLong();
            }
        } catch (Exception e) {
            System.err.println("警告：获取共享数据大小失败 - " + e.getMessage());
        }

        return 0L; // 默认值
    }
    /**
     * calculateMMUAttributes
     *
     * @brief
     * 功能: 执行calculateMMUAttributes处理逻辑。
     *
     * @param[in]  sharedData: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateMMUAttributes(SharedData sharedData) {
        int cachePolicy = getCachePolicyValue(sharedData);
        int systemAccess = getSystemAccessValue(sharedData);

        return cachePolicy | systemAccess;
    }
    /**
     * getCachePolicyValue
     *
     * @brief
     * 功能: 执行getCachePolicyValue处理逻辑。
     *
     * @param[in]  sharedData: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static int getCachePolicyValue(SharedData sharedData) {
        String cachePolicy = null;

        try {
            // 优先从BharedIODescription读取
            if (sharedData.getfSharedIODescription() != null) {
                cachePolicy = sharedData.getfSharedIODescription().getfCachePolicy();
            } else if (sharedData.getfSharedDataDescription() != null) {
                cachePolicy = sharedData.getfSharedDataDescription().getfCachePolicy();
            }
        } catch (Exception e) {
            System.err.println("警告：获取CachePolicy失败 - " + e.getMessage());
        }

        if (cachePolicy == null || cachePolicy.isEmpty()) {
            return 0x100; // DEFAULT
        }

        switch (cachePolicy.toUpperCase()) {
            case "OFF":
                return 0x10;
            case "COPY_BACK":
                return 0x20;
            case "WRITE_THRU":
                return 0x30;
            case "OFF+COHERENCY":
                return 0x50;
            case "COPY_BACK+COHERENCY":
                return 0x60;
            case "WRITE_THRU+COHERENCY":
                return 0x70;
            case "OFF+GUARDED":
                return 0x90;
            case "COPY_BACK+GUARDED":
                return 0xa0;
            case "WRITE_THRU+GUARDED":
                return 0xb0;
            case "OFF+GUARDED+COHERENCY":
                return 0xd0;
            case "COPY_BACK+GUARDED+COHERENCY":
                return 0xe0;
            case "WRITE_THRU+GUARDED+COHERENCY":
                return 0xf0;
            case "IO":
                return 0x90;
            case "DEFAULT":
                return 0x100;
            default:
                System.err.println("警告：未知的CachePolicy值: " + cachePolicy + "，使用DEFAULT");
                return 0x100;
        }
    }
    /**
     * getSystemAccessValue
     *
     * @brief
     * 功能: 执行getSystemAccessValue处理逻辑。
     *
     * @param[in]  sharedData: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static int getSystemAccessValue(SharedData sharedData) {
        String systemAccess = null;

        try {
            // 优先从 SharedIODescription读取
            if (sharedData.getfSharedIODescription() != null) {
                systemAccess = sharedData.getfSharedIODescription().getfSystemAccess();
            } else if (sharedData.getfSharedDataDescription() != null) {
                systemAccess = sharedData.getfSharedDataDescription().getfSystemAccess();
            }
        } catch (Exception e) {
            System.err.println("警告：获取SystemAccess失败 - " + e.getMessage());
        }

        if (systemAccess == null || systemAccess.isEmpty()) {
            return 3; // 默认值为READ_WRITE
        }

        switch (systemAccess.toUpperCase()) {
            case "READ_WRITE":
                return 3;
            case "READ_ONLY":
                return 1;
            default:
                System.err.println("警告：未知的SystemAccess值: " + systemAccess + "，使用READ_WRITE");
                return 3;
        }
    }
    /**
     * bytesToList
     *
     * @brief
     * 功能: 执行bytesToList处理逻辑。
     *
     * @param[in]  bytes: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static List<Byte> bytesToList(byte[] bytes) {
        List<Byte> result = new ArrayList<>();
        for (byte b : bytes) {
            result.add(b);
        }
        return result;
    }
    /**
     * shortToBytesLittleEndian
     *
     * @brief
     * 功能: 执行shortToBytesLittleEndian处理逻辑。
     *
     * @param[in]  value: 参数
     * @return 处理结果
     * @scope internal
     * @errno:
     * N/A
     */
    private static byte[] shortToBytesLittleEndian(short value) {
        return new byte[] {
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }
}
