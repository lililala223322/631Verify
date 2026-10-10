/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: ConsistencyValidationResult.java
 * 描述    : 配置数据符合性校验阶段的结果。
 */
package com.nwpu.verify;

import com.nwpu.verify.entity.ComparisonResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存配置数据符合性校验阶段的统计结果。
 */
final class ConsistencyValidationResult {
    /** 所有 BIN 文件的字段比对结果。 */
    final List<ComparisonResult> comparisonResults = new ArrayList<>();

    /** 实际参与报告生成的 BIN 文件名。 */
    final List<String> processedBinFileNames = new ArrayList<>();

    /** 统计信息。 */
    int totalSuccessCount;
    int totalFailCount;
    int stage7ErrorCount;

    /** 是否存在可供比对的 BIN 文件。 */
    boolean hasBinFiles;

    /** 本阶段最终验证状态。 */
    boolean validationPassed = true;
}
