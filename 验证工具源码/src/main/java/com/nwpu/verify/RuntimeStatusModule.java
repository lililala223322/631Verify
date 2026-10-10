/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: RuntimeStatusModule.java
 * 描述    : 运行状态记录文件相关接口。
 */
package com.nwpu.verify;

import com.nwpu.verify.layers.output.Log;

/**
 * 封装运行状态记录、阶段进度和日志生命周期操作。
 */
final class RuntimeStatusModule {
    /**
     * 打印用户输入参数。
     *
     * @param args 命令行参数
     */
    void printUserInputParameters(String[] args) {
        Application.printUserInputParameters(args);
    }

    /**
     * 重置阶段统计。
     */
    void resetStage() {
        Log.resetStage();
    }

    /**
     * 开始验证阶段。
     *
     * @param stageName 阶段名称
     */
    void startStage(String stageName) {
        Log.startStage(stageName);
    }

    /**
     * 开始验证阶段并按要求输出控制台提示。
     *
     * @param stageName 阶段名称
     * @param outputGeneratorToConsole 是否输出通用开始提示
     */
    void startStage(String stageName, boolean outputGeneratorToConsole) {
        Log.startStage(stageName, outputGeneratorToConsole);
    }

    /**
     * 记录当前阶段开始信息。
     *
     * @param stageName 阶段名称
     */
    void recordCurrentStageStartInLog(String stageName) {
        Log.recordCurrentStageStartInLog(stageName);
    }

    /**
     * 报告阶段进度。
     *
     * @param progress 进度百分比
     */
    void reportProgress(int progress) {
        Log.reportProgress(progress);
    }

    /**
     * 记录阶段完成信息。
     *
     * @param stageName 阶段名称
     * @param errorCount 阶段错误数
     */
    void completeStage(String stageName, int errorCount) {
        Log.completeStage(stageName, errorCount);
    }

    /**
     * 记录阶段完成信息，并控制是否输出通用完成提示。
     *
     * @param stageName 阶段名称
     * @param errorCount 阶段错误数
     * @param outputGenericCompletionToConsole 是否输出通用完成提示
     */
    void completeStage(String stageName, int errorCount, boolean outputGenericCompletionToConsole) {
        Log.completeStage(stageName, errorCount, outputGenericCompletionToConsole);
    }

    /**
     * 关闭运行状态记录文件。
     */
    void close() {
        Log.close();
    }
}
