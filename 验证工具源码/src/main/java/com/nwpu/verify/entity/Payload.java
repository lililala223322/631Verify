/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: Payload.java
 * 
 * 描述    : 负载配置实体类
 *           对应XML中的 /Module/Payload 节点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Payload {

    /* 成员变量 */
    /* CoreOS负载 */
    private CoreOSPayload fCoreOSPayload;
    /* 共享库负载列表 */
    private List<SharedLibraryPayload> fSharedLibraryPayloads;
    /* 配置记录负载列表 */
    private List<ConfigRecordPayload> fConfigRecordPayloads;
    /* 分区负载列表 */
    private List<PartitionPayload> fPartitionPayloads;

    /**
     * Payload
     *
     * @brief
     * 功能: 默认构造函数，初始化各负载列表
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Payload() {
        this.fSharedLibraryPayloads = new ArrayList<>();
        this.fConfigRecordPayloads = new ArrayList<>();
        this.fPartitionPayloads = new ArrayList<>();
    }

    /**
     * getfCoreOSPayload
     *
     * @brief
     * 功能: 获取CoreOS负载配置
     * 
     * @return
     * CoreOSPayload: CoreOS负载配置
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public CoreOSPayload getfCoreOSPayload() {
        return fCoreOSPayload;
    }

    /**
     * setfCoreOSPayload
     *
     * @brief
     * 功能: 设置CoreOS负载配置
     * 
     * @param[in]  fCoreOSPayload: CoreOS负载配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setfCoreOSPayload(CoreOSPayload fCoreOSPayload) {
        this.fCoreOSPayload = fCoreOSPayload;
    }

    /**
     * getfSharedLibraryPayloads
     *
     * @brief
     * 功能: 获取共享库负载列表
     * 
     * @return
     * List<SharedLibraryPayload>: 共享库负载列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<SharedLibraryPayload> getfSharedLibraryPayloads() {
        return fSharedLibraryPayloads;
    }

    /**
     * addSharedLibraryPayload
     *
     * @brief
     * 功能: 向共享库负载列表添加一项
     * 
     * @param[in]  payload: 共享库负载配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addSharedLibraryPayload(SharedLibraryPayload payload) {
        this.fSharedLibraryPayloads.add(payload);
    }

    /**
     * getfConfigRecordPayloads
     *
     * @brief
     * 功能: 获取配置记录负载列表
     * 
     * @return
     * List<ConfigRecordPayload>: 配置记录负载列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<ConfigRecordPayload> getfConfigRecordPayloads() {
        return fConfigRecordPayloads;
    }

    /**
     * addConfigRecordPayload
     *
     * @brief
     * 功能: 向配置记录负载列表添加一项
     * 
     * @param[in]  payload: 配置记录负载配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addConfigRecordPayload(ConfigRecordPayload payload) {
        this.fConfigRecordPayloads.add(payload);
    }

    /**
     * getfPartitionPayloads
     *
     * @brief
     * 功能: 获取分区负载列表
     * 
     * @return
     * List<PartitionPayload>: 分区负载列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<PartitionPayload> getfPartitionPayloads() {
        return fPartitionPayloads;
    }

    /**
     * addPartitionPayload
     *
     * @brief
     * 功能: 向分区负载列表添加一项
     * 
     * @param[in]  payload: 分区负载配置
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addPartitionPayload(PartitionPayload payload) {
        this.fPartitionPayloads.add(payload);
    }

    /**
     * CoreOSPayload
     *
     * @brief
     * 功能: 内部类：CoreOSPayload，CoreOS负载配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class CoreOSPayload {
        private long fBaseAddress;
        private long fSize;

        /**
         * CoreOSPayload
         *
         * @brief
         * 功能: 默认构造函数
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public CoreOSPayload() {
        }

        /**
         * getBaseAddress
         *
         * @brief
         * 功能: 获取基地址
         * 
         * @return
         * long: 基地址
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getBaseAddress() {
            return fBaseAddress;
        }

        /**
         * setBaseAddress
         *
         * @brief
         * 功能: 设置基地址
         * 
         * @param[in]  fBaseAddress: 基地址
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setBaseAddress(long fBaseAddress) {
            this.fBaseAddress = fBaseAddress;
        }

        /**
         * getSize
         *
         * @brief
         * 功能: 获取大小
         * 
         * @return
         * long: 大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getSize() {
            return fSize;
        }

        /**
         * setSize
         *
         * @brief
         * 功能: 设置大小
         * 
         * @param[in]  fSize: 大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setSize(long fSize) {
            this.fSize = fSize;
        }
    }

    /**
     * SharedLibraryPayload
     *
     * @brief
     * 功能: 内部类：SharedLibraryPayload，共享库负载配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class SharedLibraryPayload {
        private char[] fNameRef = new char[32];
        private long fBaseAddress;
        private long fSize;

        /**
         * SharedLibraryPayload
         *
         * @brief
         * 功能: 默认构造函数，将名称引用字符数组填充为0
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public SharedLibraryPayload() {
            Arrays.fill(fNameRef, (char) 0);
        }

        /**
         * getfNameRef
         *
         * @brief
         * 功能: 获取名称引用字符数组
         * 
         * @return
         * char[]: 名称引用字符数组
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public char[] getfNameRef() {
            return fNameRef;
        }

        /**
         * setfNameRef
         *
         * @brief
         * 功能: 以字符数组设置名称引用，超出长度部分截断，不足部分以0填充
         * 
         * @param[in]  fNameRef: 名称引用字符数组
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfNameRef(char[] fNameRef) {
            int length = Math.min(fNameRef.length, this.fNameRef.length);
            for (int i = 0; i < length; i++) {
                this.fNameRef[i] = fNameRef[i];
            }
            for (int i = length; i < this.fNameRef.length; i++) {
                this.fNameRef[i] = 0;
            }
        }

        /**
         * setfNameRef
         *
         * @brief
         * 功能: 以字符串设置名称引用，按US_ASCII编码写入字符数组
         * 
         * @param[in]  fNameRef: 名称引用字符串
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfNameRef(String fNameRef) {
            if (fNameRef == null) {
                Arrays.fill(this.fNameRef, (char) 0);
                return;
            }
            byte[] bytes = fNameRef.getBytes(StandardCharsets.US_ASCII);
            int length = Math.min(bytes.length, this.fNameRef.length);
            for (int i = 0; i < length; i++) {
                this.fNameRef[i] = (char) bytes[i];
            }
            for (int i = length; i < this.fNameRef.length; i++) {
                this.fNameRef[i] = 0;
            }
        }

        /**
         * getfNameRefAsString
         *
         * @brief
         * 功能: 将名称引用字符数组转换为字符串，遇0终止符截断
         * 
         * @return
         * String: 名称引用字符串
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfNameRefAsString() {
            int endIndex = 0;
            while (endIndex < fNameRef.length && fNameRef[endIndex] != 0) {
                endIndex++;
            }
            byte[] bytes = new byte[endIndex];
            for (int i = 0; i < endIndex; i++) {
                bytes[i] = (byte) fNameRef[i];
            }
            return new String(bytes, StandardCharsets.US_ASCII);
        }

        /**
         * getBaseAddress
         *
         * @brief
         * 功能: 获取基地址
         * 
         * @return
         * long: 基地址
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getBaseAddress() {
            return fBaseAddress;
        }

        /**
         * setBaseAddress
         *
         * @brief
         * 功能: 设置基地址
         * 
         * @param[in]  fBaseAddress: 基地址
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setBaseAddress(long fBaseAddress) {
            this.fBaseAddress = fBaseAddress;
        }

        /**
         * getSize
         *
         * @brief
         * 功能: 获取大小
         * 
         * @return
         * long: 大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getSize() {
            return fSize;
        }

        /**
         * setSize
         *
         * @brief
         * 功能: 设置大小
         * 
         * @param[in]  fSize: 大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setSize(long fSize) {
            this.fSize = fSize;
        }
    }

    /**
     * ConfigRecordPayload
     *
     * @brief
     * 功能: 内部类：ConfigRecordPayload，配置记录负载配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class ConfigRecordPayload {
        private char[] fNameRef = new char[32];
        private long fBaseAddress;
        private long fSize;

        /**
         * ConfigRecordPayload
         *
         * @brief
         * 功能: 默认构造函数，将名称引用字符数组填充为0
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public ConfigRecordPayload() {
            Arrays.fill(fNameRef, (char) 0);
        }

        /**
         * getfNameRef
         *
         * @brief
         * 功能: 获取名称引用字符数组
         * 
         * @return
         * char[]: 名称引用字符数组
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public char[] getfNameRef() {
            return fNameRef;
        }

        /**
         * setfNameRef
         *
         * @brief
         * 功能: 以字符数组设置名称引用，超出长度部分截断，不足部分以0填充
         * 
         * @param[in]  fNameRef: 名称引用字符数组
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfNameRef(char[] fNameRef) {
            int length = Math.min(fNameRef.length, this.fNameRef.length);
            for (int i = 0; i < length; i++) {
                this.fNameRef[i] = fNameRef[i];
            }
            for (int i = length; i < this.fNameRef.length; i++) {
                this.fNameRef[i] = 0;
            }
        }

        /**
         * setfNameRef
         *
         * @brief
         * 功能: 以字符串设置名称引用，按US_ASCII编码写入字符数组
         * 
         * @param[in]  fNameRef: 名称引用字符串
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfNameRef(String fNameRef) {
            if (fNameRef == null) {
                Arrays.fill(this.fNameRef, (char) 0);
                return;
            }
            byte[] bytes = fNameRef.getBytes(StandardCharsets.US_ASCII);
            int length = Math.min(bytes.length, this.fNameRef.length);
            for (int i = 0; i < length; i++) {
                this.fNameRef[i] = (char) bytes[i];
            }
            for (int i = length; i < this.fNameRef.length; i++) {
                this.fNameRef[i] = 0;
            }
        }

        /**
         * getfNameRefAsString
         *
         * @brief
         * 功能: 将名称引用字符数组转换为字符串，遇0终止符截断
         * 
         * @return
         * String: 名称引用字符串
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfNameRefAsString() {
            int endIndex = 0;
            while (endIndex < fNameRef.length && fNameRef[endIndex] != 0) {
                endIndex++;
            }
            byte[] bytes = new byte[endIndex];
            for (int i = 0; i < endIndex; i++) {
                bytes[i] = (byte) fNameRef[i];
            }
            return new String(bytes, StandardCharsets.US_ASCII);
        }

        /**
         * getBaseAddress
         *
         * @brief
         * 功能: 获取基地址
         * 
         * @return
         * long: 基地址
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getBaseAddress() {
            return fBaseAddress;
        }

        /**
         * setBaseAddress
         *
         * @brief
         * 功能: 设置基地址
         * 
         * @param[in]  fBaseAddress: 基地址
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setBaseAddress(long fBaseAddress) {
            this.fBaseAddress = fBaseAddress;
        }

        /**
         * getSize
         *
         * @brief
         * 功能: 获取大小
         * 
         * @return
         * long: 大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getSize() {
            return fSize;
        }

        /**
         * setSize
         *
         * @brief
         * 功能: 设置大小
         * 
         * @param[in]  fSize: 大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setSize(long fSize) {
            this.fSize = fSize;
        }
    }

    /**
     * PartitionPayload
     *
     * @brief
     * 功能: 内部类：PartitionPayload，分区负载配置信息
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class PartitionPayload {
        private char[] fNameRef = new char[32];
        private long fBaseAddress;
        private long fSize;
        private boolean fOnline;

        /**
         * PartitionPayload
         *
         * @brief
         * 功能: 默认构造函数，将名称引用字符数组填充为0
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public PartitionPayload() {
            Arrays.fill(fNameRef, (char) 0);
        }

        /**
         * getfNameRef
         *
         * @brief
         * 功能: 获取名称引用字符数组
         * 
         * @return
         * char[]: 名称引用字符数组
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public char[] getfNameRef() {
            return fNameRef;
        }

        /**
         * setfNameRef
         *
         * @brief
         * 功能: 以字符数组设置名称引用，超出长度部分截断，不足部分以0填充
         * 
         * @param[in]  fNameRef: 名称引用字符数组
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfNameRef(char[] fNameRef) {
            int length = Math.min(fNameRef.length, this.fNameRef.length);
            for (int i = 0; i < length; i++) {
                this.fNameRef[i] = fNameRef[i];
            }
            for (int i = length; i < this.fNameRef.length; i++) {
                this.fNameRef[i] = 0;
            }
        }

        /**
         * setfNameRef
         *
         * @brief
         * 功能: 以字符串设置名称引用，按US_ASCII编码写入字符数组
         * 
         * @param[in]  fNameRef: 名称引用字符串
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setfNameRef(String fNameRef) {
            if (fNameRef == null) {
                Arrays.fill(this.fNameRef, (char) 0);
                return;
            }
            byte[] bytes = fNameRef.getBytes(StandardCharsets.US_ASCII);
            int length = Math.min(bytes.length, this.fNameRef.length);
            for (int i = 0; i < length; i++) {
                this.fNameRef[i] = (char) bytes[i];
            }
            for (int i = length; i < this.fNameRef.length; i++) {
                this.fNameRef[i] = 0;
            }
        }

        /**
         * getfNameRefAsString
         *
         * @brief
         * 功能: 将名称引用字符数组转换为字符串，遇0终止符截断
         * 
         * @return
         * String: 名称引用字符串
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getfNameRefAsString() {
            int endIndex = 0;
            while (endIndex < fNameRef.length && fNameRef[endIndex] != 0) {
                endIndex++;
            }
            byte[] bytes = new byte[endIndex];
            for (int i = 0; i < endIndex; i++) {
                bytes[i] = (byte) fNameRef[i];
            }
            return new String(bytes, StandardCharsets.US_ASCII);
        }

        /**
         * getBaseAddress
         *
         * @brief
         * 功能: 获取基地址
         * 
         * @return
         * long: 基地址
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getBaseAddress() {
            return fBaseAddress;
        }

        /**
         * setBaseAddress
         *
         * @brief
         * 功能: 设置基地址
         * 
         * @param[in]  fBaseAddress: 基地址
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setBaseAddress(long fBaseAddress) {
            this.fBaseAddress = fBaseAddress;
        }

        /**
         * getSize
         *
         * @brief
         * 功能: 获取大小
         * 
         * @return
         * long: 大小
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public long getSize() {
            return fSize;
        }

        /**
         * setSize
         *
         * @brief
         * 功能: 设置大小
         * 
         * @param[in]  fSize: 大小
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setSize(long fSize) {
            this.fSize = fSize;
        }

        /**
         * isOnline
         *
         * @brief
         * 功能: 获取分区是否为在线加载分区
         *
         * @return
         * boolean: Online属性值，未配置时为false
         * @scope external
         * @errno:
         * N/A
         */
        public boolean isOnline() {
            return fOnline;
        }

        /**
         * setOnline
         *
         * @brief
         * 功能: 设置分区是否为在线加载分区
         *
         * @param[in] fOnline: Online属性值
         * @scope external
         * @errno:
         * N/A
         */
        public void setOnline(boolean fOnline) {
            this.fOnline = fOnline;
        }
    }
}
