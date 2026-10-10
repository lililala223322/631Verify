/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: SharedDataRegionNameGenerator32big.java
 * 
 * 描述    : 生成SharedDataRegionName相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.generation.generator32big.CPMConfig;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.Partition; // 导入 Partition 实体，用于访问其内的 SharedDataRegion 列表
import com.nwpu.verify.entity.SharedDataRegion; // 导入 SharedDataRegion 实体

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

// 共享数据区名配置存储结构生成器(32位大端)

public class SharedDataRegionNameGenerator32big {

    // Helper method to convert long to 4 bytes big-endian
    /**
     * longTo4BytesBigEndian
     *
     * @brief
     * 功能: 执行 longTo4BytesBigEndian 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private byte[] longTo4BytesBigEndian(long value) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt((int) value).array();
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
            byte[] sBytes = s.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(sBytes, 0, bytes, 0, Math.min(sBytes.length, 32));
        }
        return bytes;
    }

    /**
     * calculateSharedDataRegionNmaeStructSize
     *
     * @brief
     * 功能: 计算共享数据区名配置结构的总字节大小每个 SharedDataRegion 结构大小为：- fName: 32 字节- fMmuAttribute: 4 字节总计: 32 + 4 = 36 字节每个结构单独进行8字节对齐后为 40 字节。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 包含所有配置信息的Module对象
     * @return 共享数据区配置结构在二进制文件中的总大小（每个SharedDataRegion都进行8字节对齐）
     * @scope external
     * @errno:
     * N/A
     */
    public int calculateSharedDataRegionNmaeStructSize(com.nwpu.verify.entity.Module module) {
        int totalSize = 0;
        int singleStructSize = 36; // fName(32) + fMmuAttribute(4)
        int alignedSingleStructSize = alignTo8Bytes(singleStructSize); // 36对齐后为40字节

        // 遍历所有 Partition 中的 SharedDataRegion
        for (Map.Entry<String, Partition> partitionEntry : module.getfPartitions().entrySet()) {
            Partition partition = partitionEntry.getValue();
            // 每个 SharedDataRegion 都要单独对齐，所以是数量 × 对齐后的大小
            totalSize += partition.getfSharedDataRegions().size() * alignedSingleStructSize;
        }

        return totalSize;
    }

    /**
     * generateSharedDataRegionNamesBin
     *
     * @brief
     * 功能: 生成共享数据区配置结构的二进制内容。
     *
     * @param[in]  com:.nwpu.verify.entity.Module 包含所有配置信息的Module对象
     * @param[in]  fieldMap:                      用于记录每个字段名及其对应二进制内容的映射
     * @return 生成 SharedDataRegion 结构二进制字节数组
     * @throws IOException 如果写入失败
     * @scope external
     * @errno:
     * N/A
     */
    public byte[] generateSharedDataRegionNamesBin(com.nwpu.verify.entity.Module module, Map<String, byte[]> fieldMap)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 遍历所有 Partition 中的 SharedDataRegion
        for (Map.Entry<String, Partition> partitionEntry : module.getfPartitions().entrySet()) {
            String partitionName = partitionEntry.getKey();
            Partition partition = partitionEntry.getValue();

            for (SharedDataRegion sdr : partition.getfSharedDataRegions()) {
                String sdrNameKey = partitionName + "_" + sdr.getfNameAsString(); // 组合名称作为键

                // 1. 共享数据区名 (fName) - 32字节，不足\0填充
                byte[] nameBytes = stringTo32BytesPadded(sdr.getfNameAsString());
                baos.write(nameBytes);
                fieldMap.put("SharedDataRegion_" + sdrNameKey + "_Name", nameBytes);

                // 2. 共享数据区MMU 属性(fMmuAttribute) - 4字节
                // 根据AccessRights属性值设置MMU属性
                // NONE -> 0, READ_ONLY -> 4, READ_WRITE -> 12, 默认 -> 12
                long mmuAttributeValue;
                String accessRights = sdr.getfAccessRights();
                if ("NONE".equals(accessRights)) {
                    mmuAttributeValue = 0L;
                } else if ("READ_ONLY".equals(accessRights)) {
                    mmuAttributeValue = 4L;
                } else {
                    // READ_WRITE 或其他情况默认为 12
                    mmuAttributeValue = 12L;
                }
                byte[] mmuAttributeBytes = longTo4BytesBigEndian(mmuAttributeValue);
                baos.write(mmuAttributeBytes);
                fieldMap.put("SharedDataRegion_" + sdrNameKey + "_MmuAttribute", mmuAttributeBytes);

                // 3. 8字节对齐填充
                int currentSize = baos.size();
                int alignedSize = alignTo8Bytes(currentSize);
                int paddingSize = alignedSize - currentSize;

                if (paddingSize > 0) {
                    byte[] paddingBytes = new byte[paddingSize];
                    baos.write(paddingBytes);
                    fieldMap.put("SharedDataRegion_" + sdrNameKey + "_AlignmentPadding", paddingBytes);
                }
            }
        }

        return baos.toByteArray();
    }

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
    private int alignTo8Bytes(int size) {
        return (size + 7) & ~7;
    }
}
