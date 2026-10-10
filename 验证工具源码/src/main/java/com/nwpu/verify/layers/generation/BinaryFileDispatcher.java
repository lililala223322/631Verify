/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: BinaryFileDispatcher.java
 * 
 * 描述    : 根据二进制文件类型和生成器类型分发二进制数据生成任务。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-08-01  开发人员  删除通用生成器分支的非需求提示信息。
 */

package com.nwpu.verify.layers.generation;

/* 头文件 */
import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.generation.generator64big.CPMConfig.OsConfigGenerator64big;
import com.nwpu.verify.layers.generation.generator64big.CPMDeploy.CpmDeployInfoGenerator64big;
import com.nwpu.verify.layers.generation.generator64small.CPMConfig.OsConfigGenerator64small;
import com.nwpu.verify.layers.generation.generator64small.CPMDeploy.CpmDeployInfoGenerator64small;
import com.nwpu.verify.layers.generation.generator32big.CPMConfig.OsConfigGenerator32big;
import com.nwpu.verify.layers.generation.generator32big.CPMDeploy.CpmDeployInfoGenerator32big;
import com.nwpu.verify.layers.generation.generator32small.CPMConfig.OsConfigGenerator32small;
import com.nwpu.verify.layers.generation.generator32small.CPMDeploy.CpmDeployInfoGenerator32small;
import com.nwpu.verify.layers.output.Log;

import java.io.File;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * @brief
 * 功能: 按二进制文件名称识别文件类型，并调用对应位宽和端序的生成器生成待比对数据。
 */
public class BinaryFileDispatcher {

    /* 全局变量 */
    /* 当前使用的生成器类型（64big, 64little, 32big, 32little） */
    private static String generatorType = "64big"; /* 默认64big */

    /**
     * setGeneratorType
     *
     * @brief
     * 功能: 设置后续二进制生成任务使用的生成器类型。
     *       支持类型如下：
     *       64big    64位大端生成器；
     *       64little 64位小端生成器；
     *       32big    32位大端生成器；
     *       32little 32位小端生成器。
     * 
     * @param[in]  type: 生成器类型。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void setGeneratorType(String type) {
        generatorType = type;
    }

    /**
     * @brief
     * 功能: 定义可识别的二进制文件类型及其文件名和描述信息。
     */
    public enum BinaryFileType {
        CFG_RD("cfgrd.bin", "CPM Configuration Information Loadable File"),
        PAYLOAD_MAP_RAM("payloadMap_ram.bin", "CPM Deployment Information Loadable File (RAM)"),
        PAYLOAD_MAP_ROM("payloadMap_rom.bin", "CPM Deployment Information Loadable File (ROM)"),
        UNKNOWN("", "Unknown File Type");

        private final String fileName;
        private final String description;

        BinaryFileType(String fileName, String description) {
            this.fileName = fileName;
            this.description = description;
        }

        /**
         * getFileName
         *
         * @brief
         * 功能: 获取二进制文件类型对应的标准文件名。
         * 
         * @return
         * String: 标准文件名。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * getDescription
         *
         * @brief
         * 功能: 获取二进制文件类型对应的中文描述。
         * 
         * @return
         * String: 文件类型描述。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getDescription() {
            return description;
        }

        /**
         * identifyFileType
         *
         * @brief
         * 功能: 根据文件名识别二进制文件类型。
         *       识别规则如下：
         *       cfgrd.bin           对应CFG_RD；
         *       payloadmap_ram.bin  对应PAYLOAD_MAP_RAM；
         *       payloadmap_rom.bin  对应PAYLOAD_MAP_ROM；
         *       其他文件名或空文件名对应UNKNOWN。
         * 
         * @param[in]  fileName: 待识别的文件名。
         * @return
         * BinaryFileType: 识别得到的二进制文件类型。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public static BinaryFileType identifyFileType(String fileName) {
            if (fileName == null) {
                return UNKNOWN;
            }

            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.equals("cfgrd.bin")) {
                return CFG_RD;
            } else if (lowerFileName.equals("payloadmap_ram.bin")) {
                return PAYLOAD_MAP_RAM;
            } else if (lowerFileName.equals("payloadmap_rom.bin")) {
                return PAYLOAD_MAP_ROM;
            } else {
                return UNKNOWN;
            }
        }
    }

    /**
     * @brief
     * 功能: 保存二进制生成结果、字段映射、成功标志和错误信息。
     */
    public static class GenerationResult {
        private final byte[] generatedData;
        private final Map<String, byte[]> fieldMap;
        private final boolean success;
        private final String errorMessage;

        /**
         * GenerationResult
         *
         * @brief
         * 功能: 构造生成结果对象。
         * 
         * @param[in]  generatedData: 生成的二进制数据。
         * @param[in]  fieldMap: 字段名称到字段二进制数据的映射。
         * @param[in]  success: 生成是否成功。
         * @param[in]  errorMessage: 生成失败时的错误信息。
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public GenerationResult(byte[] generatedData, Map<String, byte[]> fieldMap, boolean success,
                String errorMessage) {
            this.generatedData = generatedData;
            this.fieldMap = fieldMap;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        /**
         * getGeneratedData
         *
         * @brief
         * 功能: 获取生成的二进制数据。
         * 
         * @return
         * byte[]: 生成的二进制数据。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public byte[] getGeneratedData() {
            return generatedData;
        }

        /**
         * getFieldMap
         *
         * @brief
         * 功能: 获取字段名称到字段二进制数据的映射。
         * 
         * @return
         * Map<String, byte[]>: 字段映射结果。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public Map<String, byte[]> getFieldMap() {
            return fieldMap;
        }

        /**
         * isSuccess
         *
         * @brief
         * 功能: 获取二进制数据生成是否成功的标志。
         * 
         * @return
         * boolean: 生成成功返回true，生成失败返回false。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * getErrorMessage
         *
         * @brief
         * 功能: 获取二进制数据生成失败时的错误信息。
         * 
         * @return
         * String: 错误信息，生成成功时可为空。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * dispatchGeneration
     *
     * @brief
     * 功能: 识别待验证二进制文件类型，并分发到对应生成方法。
     *       分发规则如下：
     *       CFG_RD          调用CPM配置信息可加载文件生成流程；
     *       PAYLOAD_MAP_RAM 调用CPM部署信息可加载文件RAM生成流程；
     *       PAYLOAD_MAP_ROM 调用CPM部署信息可加载文件ROM生成流程；
     *       UNKNOWN         记录不支持的文件类型并返回失败结果。
     * 
     * @param[in]  binFile: 要验证的二进制文件。
     * @param[in]  module: XML解析得到的模块对象。
     * @return
     * GenerationResult: 二进制生成结果。
     * @implements 
     * @scope external
     * @errno:
     * Exception: 二进制数据生成过程中发生异常。
     */
    public static GenerationResult dispatchGeneration(File binFile, Module module) {
        BinaryFileType fileType = BinaryFileType.identifyFileType(binFile.getName());
        Map<String, byte[]> fieldMap = new LinkedHashMap<>();

        Log.info("Identified binary file type: " + fileType.getDescription() + " (" + binFile.getName() + ")");

        try {
            switch (fileType) {
                case CFG_RD:
                    return generateCfgRdBinary(module, fieldMap);

                case PAYLOAD_MAP_RAM:
                    return generatePayloadMapRamBinary(module, fieldMap);

                case PAYLOAD_MAP_ROM:
                    return generatePayloadMapRomBinary(module, fieldMap);

                case UNKNOWN:
                default:
                    String errorMsg = "Unsupported binary file type: " + binFile.getName();
                    Log.warning(CodeConstant.ERROR_BIN_FILE_EXTENSION_INVALID, errorMsg);
                    return new GenerationResult(null, fieldMap, false, errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Exception occurred while generating binary data: " + e.getMessage();
            return new GenerationResult(null, fieldMap, false, errorMsg);
        }
    }

    /**
     * generateCfgRdBinary
     *
     * @brief
     * 功能: 根据当前生成器类型生成CPM配置信息可加载文件cfgrd.bin的二进制数据。
     *       生成器映射如下：
     *       64big    OsConfigGenerator64big；
     *       64little OsConfigGenerator64small；
     *       32big    OsConfigGenerator32big；
     *       32little OsConfigGenerator32small。
     * 
     * @param[in]  module: XML解析得到的模块对象。
     * @param[out] fieldMap: 字段名称到字段二进制数据的映射。
     * @return
     * GenerationResult: cfgrd.bin生成结果。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static GenerationResult generateCfgRdBinary(Module module, Map<String, byte[]> fieldMap) {
        Log.info("Start generating CPM config loadable file (cfgrd.bin) - generator: " + generatorType);

        byte[] generatedBin = null;

        switch (generatorType) {
            case "64big":
                generatedBin = new OsConfigGenerator64big().generateBin(module, fieldMap);
                break;
            case "64little":
                generatedBin = new OsConfigGenerator64small().generateBin(module, fieldMap);
                break;
            case "32big":
                generatedBin = new OsConfigGenerator32big().generateBin(module, fieldMap);
                break;
            case "32little":
                generatedBin = new OsConfigGenerator32small().generateBin(module, fieldMap);
                break;
            default:
                String errorMsg = "Unsupported generator type: " + generatorType;
                Log.error(CodeConstant.ERROR_INVALID_PARAMETERS, errorMsg);
                return new GenerationResult(null, fieldMap, false, errorMsg);
        }

        if (generatedBin != null) {
            Log.info("CPM config loadable file generated successfully, size: " + generatedBin.length + " bytes");
            return new GenerationResult(generatedBin, fieldMap, true, null);
        } else {
            String errorMsg = "Failed to generate CPM Configuration Information Loadable File";
            return new GenerationResult(null, fieldMap, false, errorMsg);
        }
    }

    /**
     * generatePayloadMapRamBinary
     *
     * @brief
     * 功能: 根据当前生成器类型生成CPM部署信息可加载文件payloadMap_ram.bin的二进制数据。
     *       生成器映射如下：
     *       64big    CpmDeployInfoGenerator64big；
     *       64little CpmDeployInfoGenerator64small；
     *       32big    CpmDeployInfoGenerator32big；
     *       32little CpmDeployInfoGenerator32small。
     * 
     * @param[in]  module: XML解析得到的模块对象。
     * @param[out] fieldMap: 字段名称到字段二进制数据的映射。
     * @return
     * GenerationResult: payloadMap_ram.bin生成结果。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static GenerationResult generatePayloadMapRamBinary(Module module, Map<String, byte[]> fieldMap) {
        Log.info("Start generating CPM deploy loadable file (payloadMap_ram.bin) - generator: " + generatorType);

        byte[] generatedBin = null;

        switch (generatorType) {
            case "64big":
                generatedBin = new CpmDeployInfoGenerator64big().generateBin(module, fieldMap);
                break;
            case "64little":
                generatedBin = new CpmDeployInfoGenerator64small().generateBin(module, fieldMap);
                break;
            case "32big":
                generatedBin = new CpmDeployInfoGenerator32big().generateBin(module, fieldMap);
                break;
            case "32little":
                generatedBin = new CpmDeployInfoGenerator32small().generateBin(module, fieldMap);
                break;
            default:
                String errorMsg = "Unsupported generator type: " + generatorType;
                Log.error(CodeConstant.ERROR_INVALID_PARAMETERS, errorMsg);
                return new GenerationResult(null, fieldMap, false, errorMsg);
        }

        if (generatedBin != null) {
            Log.info("CPM deploy loadable file (RAM) generated successfully, size: " + generatedBin.length + " bytes");
            return new GenerationResult(generatedBin, fieldMap, true, null);
        } else {
            String errorMsg = "Failed to generate CPM Deployment Information Loadable File (RAM)";
            return new GenerationResult(null, fieldMap, false, errorMsg);
        }
    }

    /**
     * generatePayloadMapRomBinary
     *
     * @brief
     * 功能: 根据当前生成器类型生成CPM部署信息可加载文件payloadMap_rom.bin的二进制数据。
     *       当前ROM生成逻辑暂用部署信息通用生成器，映射如下：
     *       64big    CpmDeployInfoGenerator64big；
     *       64little CpmDeployInfoGenerator64small；
     *       32big    CpmDeployInfoGenerator32big；
     *       32little CpmDeployInfoGenerator32small。
     * 
     * @param[in]  module: XML解析得到的模块对象。
     * @param[out] fieldMap: 字段名称到字段二进制数据的映射。
     * @return
     * GenerationResult: payloadMap_rom.bin生成结果。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static GenerationResult generatePayloadMapRomBinary(Module module, Map<String, byte[]> fieldMap) {
        Log.info("Start generating CPM deploy loadable file (payloadMap_rom.bin) - generator: " + generatorType);

        /* 注意: 这里需要根据实际需求实现ROM版本的生成器 */
        /* 目前先使用相同的生成器，后续可以创建专门的ROM生成器 */
        byte[] generatedBin = null;

        switch (generatorType) {
            case "64big":
                generatedBin = new CpmDeployInfoGenerator64big().generateBin(module, fieldMap);
                break;
            case "64little":
                generatedBin = new CpmDeployInfoGenerator64small().generateBin(module, fieldMap);
                break;
            case "32big":
                generatedBin = new CpmDeployInfoGenerator32big().generateBin(module, fieldMap);
                break;
            case "32little":
                generatedBin = new CpmDeployInfoGenerator32small().generateBin(module, fieldMap);
                break;
            default:
                String errorMsg = "Unsupported generator type: " + generatorType;
                Log.error(CodeConstant.ERROR_INVALID_PARAMETERS, errorMsg);
                return new GenerationResult(null, fieldMap, false, errorMsg);
        }

        if (generatedBin != null) {
            Log.info("CPM deploy loadable file (ROM) generated successfully, size: " + generatedBin.length + " bytes");
            return new GenerationResult(generatedBin, fieldMap, true, null);
        } else {
            String errorMsg = "Failed to generate CPM Deployment Information Loadable File (ROM)";
            return new GenerationResult(null, fieldMap, false, errorMsg);
        }
    }
}
