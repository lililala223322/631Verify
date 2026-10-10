/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: InputFileModule.java
 * 描述    : 负责扫描 CPM 可加载文件。
 */
package com.nwpu.verify;

import com.nwpu.verify.layers.input.InputScanner;

import java.nio.file.Paths;
import java.io.File;
import java.util.List;

/**
 * 输入文件处理功能。
 */
final class InputFileModule {
    /**
     * 扫描输入目录中的 BIN 文件，并过滤非必需文件。
     *
     * @param inputPath BIN 文件目录路径
     * @return 扫描到的必需 BIN 文件列表
     */
    List<File> scanBinFiles(String inputPath) {
        InputScanner scanner = new InputScanner();
        List<File> binFiles = scanner.scanBinFiles(Paths.get(inputPath));
        binFiles.removeIf(file -> !Application.isRequiredBinFile(file));

        if (binFiles.isEmpty()) {
            com.nwpu.verify.layers.output.Log.warning(
                    com.nwpu.verify.entity.CodeConstant.ERROR_BIN_FILE_PATH_INVALID,
                    "No BIN files found in path: "
                            + Application.getCleanPath(Paths.get(inputPath).toFile()));
        }
        return binFiles;
    }
}
