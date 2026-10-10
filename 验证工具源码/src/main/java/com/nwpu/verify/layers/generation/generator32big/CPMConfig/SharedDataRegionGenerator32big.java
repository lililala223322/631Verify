/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SharedDataRegionGenerator32big.java
 * 
 * 描述    : 生成SharedDataRegion相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.generation.generator32big.CPMConfig;

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
 * 功能: 共享数据区配置生成器（32位大端序）根据32位规范生成共享数据区配置的二进制数据。
 */
public class SharedDataRegionGenerator32big {
    
    /**
     * alignTo8Bytes
     *
     * @brief
     * 功能: 计算8字节对齐后的大小。
     * @return N/A
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
     * 功能: 添加8字节对齐填充。
     * @return N/A
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
     * 功能: 计算所有共享数据区配置的总大小。
     *
     * @param[in]  module: 模块对象
     * @return 总大小（字节）
     * @scope external
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
     * 功能: 计算单个共享数据区配置的大小。
     *
     * @return 单个配置的大小（字节）
     * @scope internal
     * @errno:
     * N/A
     */
    private static int calculateSingleConfigSize() {
        // 根据32位规范计算32位系统下的字段大小（参考数据词典表21）
        // 32位系统中 unsigned long 占用4字节
        int baseSize = 4     // 配置类型 (unsigned int)
                     + 4     // 配置数据大小 (unsigned int)
                     + 32    // 共享数据区名 (字符数组)
                     + 38    // 共享数据区池(字符数组)
                     + 2     // 对齐填充(unsigned short)
                     + 8     // 共享数据区物理地址 (unsigned long long) - 保持8字节
                     + 4     // 共享数据区逻辑地址 (unsigned long) - 32位改为4字节
                     + 4     // 共享数据区大小 (unsigned long) - 32位改为4字节
                     + 4;    // 共享数据区MMU属性(int)
        // baseSize = 100字节, 8字节对齐后为104字节
        return alignTo8Bytes(baseSize);
    }
    
    /**
     * generateSharedDataRegionConfigs
     *
     * @brief
     * 功能: 生成所有共享数据区配置的二进制数据，并记录每个字段到fieldMap。
     *
     * @param[in]  module: 模块对象
     * @param[in]  fieldMap: 用于记录每个字段名及其对应二进制内容的映射
     * @return 二进制数据列表
     * @scope external
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
        
        // 获取CoreOS中sharedIO的信息（如果存在）
        CoreOS.HardwareConfiguration.SharedIO coreOSSharedIO = null;
        if (module.getfCoreOS() != null && 
            module.getfCoreOS().getfHardwareConfiguration() != null) {
            coreOSSharedIO = module.getfCoreOS().getfHardwareConfiguration().getfSharedIO();
        }
        
        // 按照SharedData在Module/SharedDataRegions中的顺序处理
        for (Map.Entry<String, SharedData> entry : module.getfSharedDatas().entrySet()) {
            String sharedDataName = entry.getKey();
            SharedData sharedData = entry.getValue();
            
            // 判断是否为sharedIO类型（有SharedIODescription）
            boolean isSharedIOType = sharedData.getfSharedIODescription() != null;
            
            // 对于sharedIO类型，使用CoreOS中sharedIO元素的Base_Address和Size
            long physicalAddress = currentPhysicalAddress;
            long sharedDataSize = getSharedDataSize(sharedData);
            
            if (isSharedIOType && coreOSSharedIO != null) {
                physicalAddress = coreOSSharedIO.getfBaseAddress();
                sharedDataSize = coreOSSharedIO.getfSize();
            }
            
            // 生成单个共享数据区配置并记录fieldMap
            List<Byte> configData = generateSingleSharedDataRegionConfig(
                sharedDataName, sharedData, physicalAddress, sharedDataSize, coreOSSharedIO, fieldMap);
            result.addAll(configData);
            
            // 只有非sharedIO类型才累加物理地址
            if (!isSharedIOType) {
                currentPhysicalAddress += sharedDataSize;
            }
        }
        
        return result;
    }
    
    /**
     * generateSingleSharedDataRegionConfig
     *
     * @brief
     * 功能: 生成单个共享数据区配置的二进制数据，并记录每个字段到fieldMap。
     *
     * @param[in]  sharedDataName: 共享数据名称
     * @param[in]  sharedData: 共享数据对象
     * @param[in]  physicalAddress: 物理地址
     * @param[in]  fieldMap: 用于记录每个字段名及其对应二进制内容的映射
     * @return 二进制数据列。。
     * @scope internal
     * @errno:
     * N/A
     */
    private static List<Byte> generateSingleSharedDataRegionConfig(
            String sharedDataName, SharedData sharedData, long physicalAddress, 
            long size, CoreOS.HardwareConfiguration.SharedIO coreOSSharedIO, Map<String, byte[]> fieldMap) {
        List<Byte> result = new ArrayList<>();

        // 1. 配置类型 (4字节)
        byte[] configTypeBytes = BinaryUtils.intToBytesBigEndian(CONFIG_TYPE);
        result.addAll(bytesToList(configTypeBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_ConfigType", configTypeBytes);

        // 2. 配置数据大小 (4字节)
        int configSize = calculateSingleConfigSize();
        byte[] configSizeBytes = BinaryUtils.intToBytesBigEndian(configSize);
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
        byte[] alignmentBytes = shortToBytesBigEndian(ALIGNMENT_PADDING);
        result.addAll(bytesToList(alignmentBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_StructPadding", alignmentBytes);

        // 6. 共享数据区物理地址 (8字节)
        byte[] physicalAddrBytes = BinaryUtils.longTo8BytesBigEndian(physicalAddress);
        result.addAll(bytesToList(physicalAddrBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_PhysicalAddress", physicalAddrBytes);

        // 7. 共享数据区逻辑地址 (4字节，32位系统 unsigned long)
        long virtualAddress = getVirtualAddress(sharedData);
        byte[] virtualAddrBytes = BinaryUtils.longTo4BytesBigEndian(virtualAddress);
        result.addAll(bytesToList(virtualAddrBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_VirtualAddress", virtualAddrBytes);

        // 8. 共享数据区大小 (4字节，32位系统 unsigned long) - 使用传入的size参数
        byte[] sizeBytes = BinaryUtils.longTo4BytesBigEndian(size);
        result.addAll(bytesToList(sizeBytes));
        fieldMap.put("SharedData_" + sharedDataName + "_Size", sizeBytes);

        // 9. 共享数据区MMU属性 (4字节)
        int mmuAttributes = calculateMMUAttributes(sharedData);
        byte[] mmuAttrBytes = BinaryUtils.intToBytesBigEndian(mmuAttributes);
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
     * 功能: 获取userMemoryRegion的基地址。
     *
     * @param[in]  module: 模块对象
     * @return 基地址
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
                CoreOS.HardwareConfiguration.UserMemoryRegion userMemoryRegion = 
                    module.getfCoreOS().getfHardwareConfiguration().getfUserMemoryRegion();
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
     * 功能: 获取共享数据的虚拟地址。
     *
     * @param[in]  sharedData: 共享数据对象
     * @return 虚拟地址
     * @scope internal
     * @errno:
     * N/A
     */
    private static long getVirtualAddress(SharedData sharedData) {
        try {
            // 优先从SharedIODescription获取（用于sharedIO类型）
            if (sharedData.getfSharedIODescription() != null) {
                return sharedData.getfSharedIODescription().getVirtualAddressAsLong();
            }
            // 否则从SharedDataDescription获取
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
     * 功能: 获取共享数据的大小。
     *
     * @param[in]  sharedData: 共享数据对象
     * @return 大小
     * @scope internal
     * @errno:
     * N/A
     */
    private static long getSharedDataSize(SharedData sharedData) {
        try {
            // 优先从SharedIODescription获取（用于sharedIO类型）
            if (sharedData.getfSharedIODescription() != null) {
                return sharedData.getfSharedIODescription().getSizeAsLong();
            }
            // 否则从SharedDataDescription获取
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
     * 功能: 计算MMU属性（CachePolicy和SystemAccess的位或）。
     *
     * @param[in]  sharedData: 共享数据对象
     * @return MMU属性值
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
     * 功能: 获取CachePolicy的数值。
     *
     * @param[in]  sharedData: 共享数据对象
     * @return CachePolicy数值
     * @scope internal
     * @errno:
     * N/A
     */
    private static int getCachePolicyValue(SharedData sharedData) {
        String cachePolicy = null;
        
        try {
            // 优先从SharedIODescription获取（用于sharedIO类型）
            if (sharedData.getfSharedIODescription() != null) {
                cachePolicy = sharedData.getfSharedIODescription().getfCachePolicy();
            }
            // 否则从SharedDataDescription获取
            else if (sharedData.getfSharedDataDescription() != null) {
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
     * 功能: 获取SystemAccess的数值。
     *
     * @param[in]  sharedData: 共享数据对象
     * @return SystemAccess数值
     * @scope internal
     * @errno:
     * N/A
     */
    private static int getSystemAccessValue(SharedData sharedData) {
        String systemAccess = null;
        
        try {
            // 优先从SharedIODescription获取（用于sharedIO类型）
            if (sharedData.getfSharedIODescription() != null) {
                systemAccess = sharedData.getfSharedIODescription().getfSystemAccess();
            }
            // 否则从SharedDataDescription获取
            else if (sharedData.getfSharedDataDescription() != null) {
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
     * 功能: 将字节数组转换为字节列表。
     *
     * @param[in]  bytes: 字节数组
     * @return 字节列表
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
     * shortToBytesBigEndian
     *
     * @brief
     * 功能: 将short转换为2字节大端字节数组。
     *
     * @param[in]  value: short值
     * @return 2字节大端字节数组
     * @scope internal
     * @errno:
     * N/A
     */
    private static byte[] shortToBytesBigEndian(short value) {
        return new byte[] {
            (byte) ((value >> 8) & 0xFF),
            (byte) (value & 0xFF)
        };
    }
}

