/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: ComparisonResult.java
 * 
 * 描述    : 配置数据比对结果实体类，用于记录二进制字段验证的期望值与实际值
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 *     2026-08-01  开发人员  增加CSV报告Line列的XML来源信息。
 */
package com.nwpu.verify.entity;

public class ComparisonResult {

    /* 成员变量 */
    private String fBinaryFieldName;
    private String fBinaryFieldValue;
    private String fActualValue;
    private String fVerificationResult;
    private String fBinaryFileName;
    private String fOffset;
    private String fLine;

    /**
     * ComparisonResult
     *
     * @brief
     * 功能: 默认构造函数
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public ComparisonResult() {
    }

    /**
     * getfBinaryFieldName
     *
     * @brief
     * 功能: 获取二进制字段名称
     * 
     * @return
     * String: 二进制字段名称
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfBinaryFieldName() {
        return fBinaryFieldName;
    }

    /**
     * setfBinaryFieldName
     *
     * @brief
     * 功能: 设置二进制字段名称
     * 
     * @param[in]  fBinaryFieldName: 二进制字段名称
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfBinaryFieldName(String fBinaryFieldName) {
        this.fBinaryFieldName = fBinaryFieldName;
    }

    /**
     * getfBinaryFieldValue
     *
     * @brief
     * 功能: 获取二进制字段期望值
     * 
     * @return
     * String: 二进制字段期望值
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfBinaryFieldValue() {
        return fBinaryFieldValue;
    }

    /**
     * setfBinaryFieldValue
     *
     * @brief
     * 功能: 设置二进制字段期望值
     * 
     * @param[in]  fBinaryFieldValue: 二进制字段期望值
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfBinaryFieldValue(String fBinaryFieldValue) {
        this.fBinaryFieldValue = fBinaryFieldValue;
    }

    /**
     * getfActualValue
     *
     * @brief
     * 功能: 获取实际读取值
     * 
     * @return
     * String: 实际读取值
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfActualValue() {
        return fActualValue;
    }

    /**
     * setfActualValue
     *
     * @brief
     * 功能: 设置实际读取值
     * 
     * @param[in]  fActualValue: 实际读取值
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfActualValue(String fActualValue) {
        this.fActualValue = fActualValue;
    }

    /**
     * getfVerificationResult
     *
     * @brief
     * 功能: 获取验证结果
     * 
     * @return
     * String: 验证结果
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfVerificationResult() {
        return fVerificationResult;
    }

    /**
     * setfVerificationResult
     *
     * @brief
     * 功能: 设置验证结果
     * 
     * @param[in]  fVerificationResult: 验证结果
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfVerificationResult(String fVerificationResult) {
        this.fVerificationResult = fVerificationResult;
    }

    /**
     * getfBinaryFileName
     *
     * @brief
     * 功能: 获取二进制文件名
     * 
     * @return
     * String: 二进制文件名
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfBinaryFileName() {
        return fBinaryFileName;
    }

    /**
     * setfBinaryFileName
     *
     * @brief
     * 功能: 设置二进制文件名
     * 
     * @param[in]  fBinaryFileName: 二进制文件名
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfBinaryFileName(String fBinaryFileName) {
        this.fBinaryFileName = fBinaryFileName;
    }

    /**
     * getfOffset
     *
     * @brief
     * 功能: 获取字段偏移量
     * 
     * @return
     * String: 字段偏移量
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public String getfOffset() {
        return fOffset;
    }

    /**
     * setfOffset
     *
     * @brief
     * 功能: 设置字段偏移量
     * 
     * @param[in]  fOffset: 字段偏移量
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfOffset(String fOffset) {
        this.fOffset = fOffset;
    }

    /**
     * @brief 获取字段失败时对应的XML来源位置。
     * @return String: FileName Line{lineNumber}，多个来源以分号分隔。
     */
    public String getfLine() {
        return fLine;
    }

    /**
     * @brief 设置字段失败时对应的XML来源位置。
     * @param[in] fLine: XML来源位置；非XML属性来源为0。
     */
    public void setfLine(String fLine) {
        this.fLine = fLine;
    }
}
