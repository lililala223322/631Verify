/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: InputScanner.java
 * 
 * 描述    : 扫描指定目录下的XML、BIN以及非BIN输入文件。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 */

package com.nwpu.verify.layers.input;

/* 头文件 */
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @brief
 * 功能: 提供输入目录文件扫描能力。
 */
public class InputScanner {

    /**
     * scanXmlFiles
     *
     * @brief
     * 功能: 递归扫描指定目录，返回所有扩展名为.xml的普通文件。
     *       处理规则如下：
     *       1. dir为空或不是目录时返回空列表；
     *       2. 扫描过程中忽略目录，仅保留普通文件；
     *       3. 文件扩展名匹配不区分大小写。
     * 
     * @param[in]  dir: 待扫描目录路径。
     * @return
     * List<File>: XML文件列表，未扫描到时返回空列表。
     * @implements 
     * @scope external
     * @errno:
     * IOException: 目录遍历失败时捕获并输出错误信息。
     */
    public List<File> scanXmlFiles(Path dir){
        List<File> xmlFiles = new ArrayList<>();

        if (dir == null || !Files.isDirectory(dir)) {
            return xmlFiles;
        }

        /* 使用 try-with-resources 确保 Stream 被关闭 */
        try (Stream<Path> pathStream = Files.walk(dir)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".xml"))
                    .forEach(path -> xmlFiles.add(path.toFile()));
        } catch (IOException e) {
            System.err.println("Error scanning directory: " + e.getMessage());
        }

        return xmlFiles;
    }

    /**
     * scanBinFiles
     *
     * @brief
     * 功能: 递归扫描指定目录，返回所有扩展名为.bin的普通文件。
     *       处理规则如下：
     *       1. dir为空或不是目录时返回空列表；
     *       2. 扫描过程中忽略目录，仅保留普通文件；
     *       3. 文件扩展名匹配不区分大小写。
     * 
     * @param[in]  dir: 待扫描目录路径。
     * @return
     * List<File>: BIN文件列表，未扫描到时返回空列表。
     * @implements 
     * @scope external
     * @errno:
     * IOException: 目录遍历失败时捕获并输出错误信息。
     */
    public List<File> scanBinFiles(Path dir){
        List<File> binFiles = new ArrayList<>();

        if (dir == null || !Files.isDirectory(dir)) {
            return binFiles;
        }

        /* 使用 try-with-resources 确保 Stream 被关闭 */
        try (Stream<Path> pathStream = Files.walk(dir)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".bin"))
                    .forEach(path -> binFiles.add(path.toFile()));
        } catch (IOException e) {
            System.err.println("Error scanning directory: " + e.getMessage());
        }

        return binFiles;
    }

    /**
     * findNonBinFiles
     *
     * @brief
     * 功能: 递归扫描指定目录，返回所有扩展名不是.bin的普通文件。
     *       处理规则如下：
     *       1. dir为空或不是目录时返回空列表；
     *       2. 扫描过程中忽略目录，仅保留普通文件；
     *       3. .bin扩展名判断不区分大小写。
     * 
     * @param[in]  dir: 待检查目录路径。
     * @return
     * List<File>: 非BIN文件列表，未扫描到时返回空列表。
     * @implements 
     * @scope external
     * @errno:
     * IOException: 目录遍历失败时捕获并输出错误信息。
     */
    public List<File> findNonBinFiles(Path dir) {
        List<File> nonBinFiles = new ArrayList<>();

        if (dir == null || !Files.isDirectory(dir)) {
            return nonBinFiles;
        }

        try (Stream<Path> pathStream = Files.walk(dir)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.toString().toLowerCase().endsWith(".bin"))
                    .forEach(path -> nonBinFiles.add(path.toFile()));
        } catch (IOException e) {
            System.err.println("Error scanning directory: " + e.getMessage());
        }

        return nonBinFiles;
    }
}
