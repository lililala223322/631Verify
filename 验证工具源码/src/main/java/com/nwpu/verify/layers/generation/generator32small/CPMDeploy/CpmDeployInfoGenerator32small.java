/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: CpmDeployInfoGenerator32small.java
 * 
 * 描述    : 生成CpmDeployInfo相关32位大端二进制配置数据。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-08-01  开发人员  补充空部署信息配置存储结构生成逻辑。
 */
package com.nwpu.verify.layers.generation.generator32small.CPMDeploy;

import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.CoreOS.KernelConfiguration;
import com.nwpu.verify.layers.generation.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @brief
 * 功能: CPM部署信息可加载文件生成器 (32位大端)。
 */
public class CpmDeployInfoGenerator32small {
    
    // 默认的CPM部署信息可加载文件大小
    private static final long DEFAULT_PAYLOAD_MAP_SIZE = 0x20000L; // 128KB
    
    /**
     * generateBin
     *
     * @brief
     * 功能: 生成CPM部署信息可加载文件的二进制内容。
     *
     * @param[in]  module: 从XML解析层传入的顶层Module对象
     * @param[in]  fieldMap: 用于记录每个字段名及其对应二进制内容的映射，供对比层使用
     * @return 生成的完整二进制字节数组
     * @scope external
     * @errno:
     * N/A
     */
    public byte[] generateBin(Module module, Map<String, byte[]> fieldMap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // 计算CPM部署信息可加载文件大小
            long payloadMapSize = calculatePayloadMapSize(module);
            
            // 记录文件大小信息（4字节值 + 4字节对齐填充 = 8字节）
            byte[] payloadMapSizeBytes = BinaryUtils.longTo4BytesLittleEndian(payloadMapSize);
            byte[] payloadMapSizePadding = new byte[4]; // 4字节对齐填充
            fieldMap.put("CPMDeployInfo_PayloadMapSize", payloadMapSizeBytes);
            fieldMap.put("CPMDeployInfo_PayloadMapSize_Padding", payloadMapSizePadding);
            
            System.out.println("CPM deployment info loadable file size 0x" + Long.toHexString(payloadMapSize).toUpperCase() +
                             " (" + payloadMapSize + " bytes) - 4-byte value + 4-byte alignment padding");
            
            // 2. 写入CPM部署信息可加载文件大小字节（4字节）+ 4字节对齐填充 = 8字节
            baos.write(payloadMapSizeBytes);
            baos.write(payloadMapSizePadding);
            
            // =======================================================================
            // 后续数据结构写入区域
            // =======================================================================
            // 核心 OS 部署信息配置存储结构
            CoreOSDeployInfoConfigGenerator32small coreOSGenerator = new CoreOSDeployInfoConfigGenerator32small();
            byte[] coreOSDeployInfoBytes = coreOSGenerator.generateBin(module, fieldMap);
            baos.write(coreOSDeployInfoBytes);
            
            System.out.println("CoreOS deployment info config storage structure size: " + coreOSDeployInfoBytes.length + " bytes");
            
            // 检查8字节对齐
            int currentSize = baos.size();
            int alignmentRequired = 8 - (currentSize % 8);
            if (alignmentRequired != 8 && alignmentRequired != 0) {
                byte[] alignmentBytes = new byte[alignmentRequired];
                baos.write(alignmentBytes);
                fieldMap.put("CoreOSDeployInfo_AlignmentPadding", alignmentBytes);
                System.out.println("CoreOS deployment info alignment padding: " + alignmentRequired + " bytes");
            }
            
            // 分区操作系统共享库部署信息配置存储结构
            try {
                byte[] partitionOsSharedBytes = PartitionOsSharedGenerator32small.generate(module, fieldMap);
                baos.write(partitionOsSharedBytes);
                
                System.out.println("Partition OS shared library deployment info config storage structure size: " + partitionOsSharedBytes.length + " bytes");
                
                // 检查8字节对齐
                currentSize = baos.size();
                alignmentRequired = 8 - (currentSize % 8);
                if (alignmentRequired != 8 && alignmentRequired != 0) {
                    byte[] alignmentBytes = new byte[alignmentRequired];
                    baos.write(alignmentBytes);
                    fieldMap.put("PartitionOsShared_AlignmentPadding", alignmentBytes);
                    System.out.println("Partition OS shared library deployment info alignment padding: " + alignmentRequired + " bytes");
                }
            } catch (IOException e) {
                System.err.println("Error occurred while generating partition OS shared library deployment info: " + e.getMessage());
                e.printStackTrace();
            }

            // 分区部署信息配置存储结构
            try {
                PartitionDeploymentGenerator32small partitionDeploymentGenerator = new PartitionDeploymentGenerator32small();
                byte[] partitionDeploymentBytes = partitionDeploymentGenerator.generateAllPartitionDeployments(module, fieldMap);
                baos.write(partitionDeploymentBytes);
                
                System.out.println("Partition deployment info config storage structure size: " + partitionDeploymentBytes.length + " bytes");
                
                // 检查8字节对齐
                currentSize = baos.size();
                alignmentRequired = 8 - (currentSize % 8);
                if (alignmentRequired != 8 && alignmentRequired != 0) {
                    byte[] alignmentBytes = new byte[alignmentRequired];
                    baos.write(alignmentBytes);
                    fieldMap.put("PartitionDeployment_AlignmentPadding", alignmentBytes);
                    System.out.println("Partition deployment info alignment padding: " + alignmentRequired + " bytes");
                }
            } catch (IOException e) {
                System.err.println("Error occurred while generating partition deployment info: " + e.getMessage());
                e.printStackTrace();
            }

            // 空部署信息配置存储结构（数据词典表71）
            byte[] emptyDomainName = new byte[32];
            byte[] emptyDomainType = BinaryUtils.longTo4BytesLittleEndian(0xFFFFFFFFL);
            byte[] emptyPlaceholder = new byte[4];
            byte[] emptySegmentType = new byte[4];
            byte[] emptyDeploymentAddress = new byte[4];
            byte[] emptyLogicalAddress = new byte[4];
            byte[] emptyPhysicalAddress = new byte[8];
            byte[] emptySegmentSize = new byte[4];
            byte[] emptyChecksum = new byte[4];

            baos.write(emptyDomainName);
            baos.write(emptyDomainType);
            baos.write(emptyPlaceholder);
            baos.write(emptySegmentType);
            baos.write(emptyDeploymentAddress);
            baos.write(emptyLogicalAddress);
            baos.write(emptyPhysicalAddress);
            baos.write(emptySegmentSize);
            baos.write(emptyChecksum);

            fieldMap.put("EmptyDeployment_DomainName", emptyDomainName);
            fieldMap.put("EmptyDeployment_DomainType", emptyDomainType);
            fieldMap.put("EmptyDeployment_Placeholder", emptyPlaceholder);
            fieldMap.put("EmptyDeployment_SegmentType", emptySegmentType);
            fieldMap.put("EmptyDeployment_DeploymentAddress", emptyDeploymentAddress);
            fieldMap.put("EmptyDeployment_LogicalAddress", emptyLogicalAddress);
            fieldMap.put("EmptyDeployment_PhysicalAddress", emptyPhysicalAddress);
            fieldMap.put("EmptyDeployment_SegmentSize", emptySegmentSize);
            fieldMap.put("EmptyDeployment_Checksum", emptyChecksum);

            // =======================================================================
            // 填充处理
            // =======================================================================
            // 填充0，直至整个CPM部署信息可加载文件的大小与payloadMapSize属性值保持一致
            byte[] dataBeforePadding = baos.toByteArray();
            int finalSize = dataBeforePadding.length;
            
            // 计算需要填充的字节数（payloadMapSize - 4字节CRC32 - 当前大小）
            long targetSizeWithoutCRC = payloadMapSize - 4; // 减去CRC32的4字节
            long paddingSize = targetSizeWithoutCRC - finalSize;
            
            if (paddingSize > 0) {
                // 添加填充字节
                byte[] paddingBytes = new byte[(int)paddingSize];
                baos.write(paddingBytes);
                fieldMap.put("CPMDeployInfo_PaddingBytes", paddingBytes);
                
                System.out.println("CPM deployment info padding: " + paddingSize + " bytes");
            } else if (paddingSize < 0) {
                System.err.println("Warning: CPM deployment info current data size (" + finalSize +
                                 ") exceeds target size " + targetSizeWithoutCRC + ")");
            } else {
                System.out.println("CPM deployment info needs no padding, current size matches target size exactly");
            }
            
            // // =======================================================================
            // // CRC32校验
            // // =======================================================================
            // // 获取当前已生成的所有数据（不包括CRC32。
            // byte[] dataWithoutCRC = baos.toByteArray();
            
            // // 计算CRC32校验
            // long crc32Value = CRC32Utils.calculateCRC32(dataWithoutCRC);
            // byte[] crc32Bytes = CRC32Utils.crc32ToBytes(crc32Value);
            
            // // 写入CRC32校验
            // baos.write(crc32Bytes);
            // fieldMap.put("CPMDeployInfo_CRC32CheckSum", crc32Bytes);
            
            // System.out.println("CPM部署信息CRC32: 0x" + 
            //                  Long.toHexString(crc32Value).toUpperCase());
            
            return baos.toByteArray();
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * calculatePayloadMapSize
     *
     * @brief
     * 功能: 计算CPM部署信息可加载文件大小。
     *
     * @param[in]  module: 模块对象
     * @return CPM部署信息可加载文件大小（字节）
     * @scope internal
     * @errno:
     * N/A
     */
    private long calculatePayloadMapSize(Module module) {
        // 获取KernelConfiguration
        if (module == null || module.getfCoreOS() == null || 
            module.getfCoreOS().getfKernelConfiguration() == null) {
            System.out.println("KernelConfiguration does not exist, using default payloadMapSize: 0x" +
                             Long.toHexString(DEFAULT_PAYLOAD_MAP_SIZE).toUpperCase());
            return DEFAULT_PAYLOAD_MAP_SIZE;
        }
        
        KernelConfiguration kernelConfig = module.getfCoreOS().getfKernelConfiguration();
        
        // 从XML配置中读取payloadMapSize属性
        String payloadMapSizeStr = kernelConfig.getfPayloadMapSize();
        
        if (payloadMapSizeStr != null && !payloadMapSizeStr.trim().isEmpty()) {
            try {
                long payloadMapSize;
                // 支持十六进制和十进制格式
                if (payloadMapSizeStr.startsWith("0x") || payloadMapSizeStr.startsWith("0X")) {
                    // 十六进制格式0x20000
                    String hexStr = payloadMapSizeStr.substring(2);
                    payloadMapSize = Long.parseLong(hexStr, 16);
                } else {
                    // 十进制格式
                    payloadMapSize = Long.parseLong(payloadMapSizeStr);
                }
                
                System.out.println("Parse payloadMapSize: " + payloadMapSizeStr + " -> " +
                                 payloadMapSize + " bytes");
                return payloadMapSize;
                
            } catch (NumberFormatException e) {
                System.err.println("Warning: Cannot parse payloadMapSize: " + payloadMapSizeStr +
                                 ", using default value: 0x" + Long.toHexString(DEFAULT_PAYLOAD_MAP_SIZE).toUpperCase());
                return DEFAULT_PAYLOAD_MAP_SIZE;
            }
        } else {
            System.out.println("payloadMapSize attribute not configured, using default value: 0x" +
                             Long.toHexString(DEFAULT_PAYLOAD_MAP_SIZE).toUpperCase());
            return DEFAULT_PAYLOAD_MAP_SIZE;
        }
            }
        
    
    /**
     * calculateTotalSize
     *
     * @brief
     * 功能: 计算CPM部署信息可加载文件的总大小。
     *
     * @param[in]  module: 模块对象
     * @return 文件总大小（字节）
     * @scope external
     * @errno:
     * N/A
     */
    public static long calculateTotalSize(Module module) {
        CpmDeployInfoGenerator32small generator = new CpmDeployInfoGenerator32small();
        return generator.calculatePayloadMapSize(module);
    }
            }


