/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: Channel.java
 * 
 * 描述    : 通道实体类，映射XML配置中的Channel节点及其源端点与目的端点
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.entity;

/* 头文件 */
import java.util.ArrayList;
import java.util.List;

public class Channel {

    /* 成员变量 */
    private int id;
    private List<ChannelEndpoint> sources;
    private List<ChannelEndpoint> destinations;
    
    /**
     * Channel
     *
     * @brief
     * 功能: 默认构造函数，初始化源端点列表和目的端点列表
     * 
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public Channel() {
        this.sources = new ArrayList<>();
        this.destinations = new ArrayList<>();
    }
    
    /**
     * getId
     *
     * @brief
     * 功能: 获取通道ID
     * 
     * @return
     * int: 通道ID
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public int getId() {
        return id;
    }
    
    /**
     * setId
     *
     * @brief
     * 功能: 设置通道ID
     * 
     * @param[in]  id: 通道ID
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * getSources
     *
     * @brief
     * 功能: 获取通道源端点列表
     * 
     * @return
     * List<ChannelEndpoint>: 源端点列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<ChannelEndpoint> getSources() {
        return sources;
    }
    
    /**
     * setSources
     *
     * @brief
     * 功能: 设置通道源端点列表
     * 
     * @param[in]  sources: 源端点列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setSources(List<ChannelEndpoint> sources) {
        this.sources = sources;
    }
    
    /**
     * addSource
     *
     * @brief
     * 功能: 向通道添加一个源端点
     * 
     * @param[in]  source: 待添加的源端点
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addSource(ChannelEndpoint source) {
        this.sources.add(source);
    }
    
    /**
     * getDestinations
     *
     * @brief
     * 功能: 获取通道目的端点列表
     * 
     * @return
     * List<ChannelEndpoint>: 目的端点列表
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public List<ChannelEndpoint> getDestinations() {
        return destinations;
    }
    
    /**
     * setDestinations
     *
     * @brief
     * 功能: 设置通道目的端点列表
     * 
     * @param[in]  destinations: 目的端点列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void setDestinations(List<ChannelEndpoint> destinations) {
        this.destinations = destinations;
    }
    
    /**
     * addDestination
     *
     * @brief
     * 功能: 向通道添加一个目的端点
     * 
     * @param[in]  destination: 待添加的目的端点
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public void addDestination(ChannelEndpoint destination) {
        this.destinations.add(destination);
    }
    
    /**
     * getSource
     *
     * @brief
     * 功能: 获取第一个源端点，保留向后兼容
     * 
     * @return
     * ChannelEndpoint: 第一个源端点，列表为空时返回null
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    @Deprecated
    public ChannelEndpoint getSource() {
        return sources.isEmpty() ? null : sources.get(0);
    }
    
    /**
     * setSource
     *
     * @brief
     * 功能: 以单个源端点替换源端点列表，保留向后兼容
     * 
     * @param[in]  source: 源端点，为null时清空列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    @Deprecated
    public void setSource(ChannelEndpoint source) {
        this.sources.clear();
        if (source != null) {
            this.sources.add(source);
        }
    }
    
    /**
     * getDestination
     *
     * @brief
     * 功能: 获取第一个目的端点，保留向后兼容
     * 
     * @return
     * ChannelEndpoint: 第一个目的端点，列表为空时返回null
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    @Deprecated
    public ChannelEndpoint getDestination() {
        return destinations.isEmpty() ? null : destinations.get(0);
    }
    
    /**
     * setDestination
     *
     * @brief
     * 功能: 以单个目的端点替换目的端点列表，保留向后兼容
     * 
     * @param[in]  destination: 目的端点，为null时清空列表
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    @Deprecated
    public void setDestination(ChannelEndpoint destination) {
        this.destinations.clear();
        if (destination != null) {
            this.destinations.add(destination);
        }
    }
    
    /**
     * ChannelEndpoint
     *
     * @brief
     * 功能: 通道端点内部类，表示Source或Destination端点的分区与端口引用
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class ChannelEndpoint {
        private String partitionNameRef;
        private String portNameRef;
        
        /**
         * ChannelEndpoint
         *
         * @brief
         * 功能: 默认构造函数
         * 
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public ChannelEndpoint() {}
        
        /**
         * ChannelEndpoint
         *
         * @brief
         * 功能: 带参构造函数，初始化分区名称引用和端口名称引用
         * 
         * @param[in]  partitionNameRef: 分区名称引用
         * @param[in]  portNameRef: 端口名称引用
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public ChannelEndpoint(String partitionNameRef, String portNameRef) {
            this.partitionNameRef = partitionNameRef;
            this.portNameRef = portNameRef;
        }
        
        /**
         * getPartitionNameRef
         *
         * @brief
         * 功能: 获取分区名称引用
         * 
         * @return
         * String: 分区名称引用
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getPartitionNameRef() {
            return partitionNameRef;
        }
        
        /**
         * setPartitionNameRef
         *
         * @brief
         * 功能: 设置分区名称引用
         * 
         * @param[in]  partitionNameRef: 分区名称引用
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setPartitionNameRef(String partitionNameRef) {
            this.partitionNameRef = partitionNameRef;
        }
        
        /**
         * getPortNameRef
         *
         * @brief
         * 功能: 获取端口名称引用
         * 
         * @return
         * String: 端口名称引用
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getPortNameRef() {
            return portNameRef;
        }
        
        /**
         * setPortNameRef
         *
         * @brief
         * 功能: 设置端口名称引用
         * 
         * @param[in]  portNameRef: 端口名称引用
         * @return
         * N/A
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public void setPortNameRef(String portNameRef) {
            this.portNameRef = portNameRef;
        }
        
        /**
         * toString
         *
         * @brief
         * 功能: 返回端点对象的字符串表示
         * 
         * @return
         * String: 端点信息字符串
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        @Override
        public String toString() {
            return "ChannelEndpoint{" +
                    "partitionNameRef='" + partitionNameRef + '\'' +
                    ", portNameRef='" + portNameRef + '\'' +
                    '}';
        }
    }
    
    /**
     * toString
     *
     * @brief
     * 功能: 返回通道对象的字符串表示
     * 
     * @return
     * String: 通道信息字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    @Override
    public String toString() {
        return "Channel{" +
                "id=" + id +
                ", sources=" + sources +
                ", destinations=" + destinations +
                '}';
    }
}
