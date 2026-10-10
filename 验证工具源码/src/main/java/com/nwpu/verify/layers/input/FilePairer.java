/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: FilePairer.java
 * 
 * 描述    : 根据文件基础名称建立XML配置文件与BIN二进制文件的一一对应关系。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.input;

/* 头文件 */
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @brief
 * 功能: 提供XML文件与BIN文件的配对能力。
 */
public class FilePairer {

    /**
     * pairFiles
     *
     * @brief
     * 功能: 依据文件名去除扩展名后的基础名称，将XML文件与同名BIN文件配对。
     *       配对规则如下：
     *       1. 先构建BIN文件基础名称到BIN文件对象的映射；
     *       2. 当BIN文件基础名称重复时保留第一个文件；
     *       3. 遍历XML文件，仅将存在同名BIN文件的XML加入结果。
     * 
     * @param[in]  xmls: 待配对的XML文件列表。
     * @param[in]  bins: 待配对的BIN文件列表。
     * @return
     * Map<File, File>: XML文件到BIN文件的配对结果。
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Map<File, File> pairFiles(List<File> xmls, List<File> bins){
        /* 构建文件名（无扩展名）到bin文件的映射 */
        Map<String, File> binMap = bins.stream()
                .collect(Collectors.toMap(
                        file -> removeExtension(file.getName()),
                        file -> file,
                        (existing, replacement) -> existing /* 如果重名，保留第一个 */
                ));

        /* 遍历xml文件，查找匹配的bin文件 */
        Map<File, File> result = new HashMap<>();
        for (File xmlFile : xmls) {
            String baseName = removeExtension(xmlFile.getName());
            File matchedBin = binMap.get(baseName);
            if (matchedBin != null) {
                result.put(xmlFile, matchedBin);
            }
        }

        return result;
    }

    /**
     * removeExtension
     *
     * @brief
     * 功能: 移除文件名中的最后一个扩展名，返回文件基础名称。
     *       示例映射如下：
     *       file.xml -> file
     *       file.bin -> file
     *       file -> file
     * 
     * @param[in]  fileName: 待处理的文件名称。
     * @return
     * String: 去除扩展名后的文件基础名称。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}
