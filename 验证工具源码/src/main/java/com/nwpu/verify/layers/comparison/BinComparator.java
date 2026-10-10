/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: BinComparator.java
 * 
 * 描述    : 对二进制字段期望值与实际值进行逐项比对，并生成字段级比对结果。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-27  开发人员  按001155需求删除Padding字段过滤，保留位参与校验并加入比对结果。
 *     2026-08-01  开发人员  修正CSV报告Expected Value与Actual Value的数据来源。
 */

package com.nwpu.verify.layers.comparison;

/* 头文件 */
import com.nwpu.verify.entity.ComparisonResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @brief
 * 功能: 提供二进制字段比对能力。
 */
public class BinComparator {

    /**
     * compare
     *
     * @brief
     * 功能: 按字段顺序比较期望十六进制字符串与实际二进制字段值，生成比对结果列表。
     *       处理规则如下：
     *       1. expected为空或null时返回空结果列表；
     *       2. 保留位（Padding）字段参与校验并加入比对结果列表；
     *       3. 实际字段值由byte[]转换为十六进制字符串后参与比较；
     *       4. 比较结果为Success或Fail，并记录字段偏移量。
     * 
     * @param[in]  actualBinHex: 从输入BIN文件读取的实际十六进制字符串。
     * @param[in]  fieldNameMap: 字段名称与根据XML计算出的期望字段值的映射。
     * @return
     * List<ComparisonResult>: 字段级比对结果列表。
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<ComparisonResult> compare(String actualBinHex, Map<String,byte[]> fieldNameMap){
        int currentIndex = 0;
        List<ComparisonResult> comparisonResults = new ArrayList<>();
        
        /* 防止空字符串或null的情况 */
        if (actualBinHex == null || actualBinHex.isEmpty()) {
            System.err.println("Warning: input binary data is empty");
            return comparisonResults;
        }
        
        for (Map.Entry<String, byte[]> entry: fieldNameMap.entrySet()){
            String hexName = entry.getKey();
            /* 将每个byte[]转为十六进制字符串并去掉空格 */
            String hexString = bytesToHex(entry.getValue()).replace(" ", "");
            
            /* 添加严格的边界检查 */
            if (currentIndex >= actualBinHex.length()) {
                System.err.println("Warning: current index " + currentIndex + " exceeds actual BIN string length " + actualBinHex.length() + ", field: " + hexName);
                
                /* 为剩余字段创建空的比对结果 */
                ComparisonResult comparisonResult = new ComparisonResult();
                comparisonResult.setfBinaryFieldName(hexName);
                comparisonResult.setfBinaryFieldValue(hexString);
                comparisonResult.setfActualValue(""); /* BIN中缺少对应实际值 */
                comparisonResult.setfVerificationResult("Fail");
                /* 设置offset（十六进制字符串索引除以2得到字节偏移量） */
                comparisonResult.setfOffset(String.format("0x%04X", currentIndex / 2));

                /* 001155: 保留位（Padding）也参与校验，加入比对结果 */
                comparisonResults.add(comparisonResult);
                continue; /* 继续处理下一个字段 */
            }
            
            /* 确保不会超出字符串边界，并且结束索引不小于开始索引 */
            int endIndex = Math.min(currentIndex + hexString.length(), actualBinHex.length());
            if (endIndex <= currentIndex) {
                System.err.println("Warning: end index " + endIndex + " is not greater than start index " + currentIndex + ", field: " + hexName);
                endIndex = currentIndex; /* 设置为相等，创建空字符串 */
            }
            
            String actualSub = actualBinHex.substring(currentIndex, endIndex);
            
            ComparisonResult comparisonResult = new ComparisonResult();
            comparisonResult.setfBinaryFieldName(hexName);
            comparisonResult.setfBinaryFieldValue(hexString);
            comparisonResult.setfActualValue(actualSub);
            /* 设置offset（十六进制字符串索引除以2得到字节偏移量，格式化为0x开头的十六进制） */
            comparisonResult.setfOffset(String.format("0x%04X", currentIndex / 2));
            
            /* 检查是否匹配（考虑大小写不敏感和长度不足的情况） */
            boolean isMatch = false;
            if (actualSub.length() == hexString.length()) {
                isMatch = actualSub.equalsIgnoreCase(hexString);
            }
            
            comparisonResult.setfVerificationResult(isMatch ? "Success" : "Fail");

            currentIndex += hexString.length();

            /* 001155: 保留位（Padding）也参与校验，加入比对结果 */
            comparisonResults.add(comparisonResult);
        }
        
        return comparisonResults;
    }

    /**
     * bytesToHex
     *
     * @brief
     * 功能: 将byte数组转换为以空格分隔的两位大写十六进制字符串。
     * 
     * @param[in]  bytes: 待转换的byte数组。
     * @return
     * String: 去除末尾空格后的十六进制字符串。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b)); /* %02X 表示2位大写十六进制 */
        }
        return sb.toString().trim(); /* 去掉末尾空格 */
    }
}

