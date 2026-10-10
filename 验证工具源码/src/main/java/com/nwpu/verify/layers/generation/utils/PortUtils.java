/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: PortUtils.java
 * 
 * 描述    : 端口处理工具类，提供端口收集、查找和属性映射的静态方法
 * 
 * 修改历史:
 *     2026-06-07  系统  按注释规范翻新文件注释
 */
package com.nwpu.verify.layers.generation.utils;

/* 头文件 */
import com.nwpu.verify.entity.*;
import com.nwpu.verify.entity.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PortUtils {

    /**
     * UnifiedPort
     *
     * @brief
     * 功能: 统一包装Port的辅助类，用于添加分区信息和其他元数据
     * 
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    public static class UnifiedPort {
        private Port port;
        private int parentPartitionId;
        private int parentModuleId;
        private int channelId;
        
        public UnifiedPort(Port port, int parentPartitionId, int parentModuleId, int channelId) {
            this.port = port;
            this.parentPartitionId = parentPartitionId;
            this.parentModuleId = parentModuleId;
            this.channelId = channelId;
        }
        
        /**
         * getPort
         *
         * @brief
         * 功能: 执行 getPort 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        public Port getPort() { return port; }
        /**
         * getParentPartitionId
         *
         * @brief
         * 功能: 执行 getParentPartitionId 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        public int getParentPartitionId() { return parentPartitionId; }
        /**
         * getParentModuleId
         *
         * @brief
         * 功能: 执行 getParentModuleId 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        public int getParentModuleId() { return parentModuleId; }
        /**
         * getChannelId
         *
         * @brief
         * 功能: 执行 getChannelId 相关处理。
         *
         * @scope external
         * @errno:
         * N/A
         */
        public int getChannelId() { return channelId; }
    }

    /**
     * gatherAllPorts
     *
     * @brief
     * 功能: 从Module中收集所有端口（QueuingPort和SamplingPort），
     *       严格按照XML解析的顺序来收集，确保生成的二进制结构顺序正确。
     *       收集来源:
     *       Application  通过分区引用关联的Application端口
     *       Partition    分区中的端口（包括PseudoPartition中的端口）
     * 
     * @param[in]  module: 包含所有配置信息的Module对象
     * @return
     * List<UnifiedPort>: 统一封装后的端口列表，按原始XML顺序排列
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static List<UnifiedPort> gatherAllPorts(Module module) {
        List<UnifiedPort> allPorts = new ArrayList<>();
        
        if (module == null) {
            return allPorts;
        }
        
        /* 获取Module/@Id，未配置时保持Module默认值0 */
        int moduleId = module.getfId();
        
        /* 端口按所属分区顺序存储；每个分区内先队列端口、后采样端口 */
        Map<String, ApplicationEntity> applications = module.getfApplications();
        Map<String, Partition> partitions = module.getfPartitions();
        if (partitions != null && !partitions.isEmpty()) {
            for (Map.Entry<String, Partition> partEntry : partitions.entrySet()) {
                Partition partition = partEntry.getValue();
                if (partition == null) continue;
                
                int partitionId = partition.getfIdentifier();
                String partitionName = partition.getfNameAsString();

                ApplicationEntity application = null;
                if (applications != null && partition.getfApplicationNameRef() != null) {
                    application = applications.get(partition.getfApplicationNameRef());
                }

                /* 同一分区的所有队列端口必须排在采样端口之前 */
                if (application != null) {
                    appendPorts(allPorts, application.getfQueuingPorts(), partitionId, moduleId,
                            partitionName, module);
                }
                appendPorts(allPorts, partition.getfQueuingPorts(), partitionId, moduleId,
                        partitionName, module);

                if (application != null) {
                    appendPorts(allPorts, application.getfSamplingPorts(), partitionId, moduleId,
                            partitionName, module);
                }
                appendPorts(allPorts, partition.getfSamplingPorts(), partitionId, moduleId,
                        partitionName, module);
            }
        }
        
        /* System.out.println("总共收集到端口数量: " + allPorts.size()); */
        return allPorts;
    }

    /**
     * appendPorts
     *
     * @brief
     * 功能: 执行 appendPorts 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static void appendPorts(List<UnifiedPort> allPorts, List<? extends Port> ports,
            int partitionId, int moduleId, String partitionName, Module module) {
        if (ports == null) {
            return;
        }
        for (Port port : ports) {
            if (port == null || isNullPort(port)) {
                continue;
            }
            int channelId = findChannelIdForPort(module, port.getfNameAsString(),
                    port.getfDirection(), partitionName);
            allPorts.add(new UnifiedPort(port, partitionId, moduleId, channelId));
        }
    }
    
    /**
     * isNullPort
     *
     * @brief
     * 功能: 检查端口是否为NULL_PORT
     * 
     * @param[in]  port: 要检查的端口
     * @return
     * boolean: 如果是NULL_PORT返回true，否则返回false
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean isNullPort(Port port) {
        if (port == null) {
            return true;
        }
        /* 只有显式配置为NULL_PORT时才跳过；缺省属性由端口类型采用规范默认值 */
        return "NULL_PORT".equalsIgnoreCase(port.getfAttribute());
    }
    
    /**
     * findPartitionIdForApplication
     *
     * @brief
     * 功能: 根据Application名称查找对应的Partition ID
     * 
     * @param[in]  module: Module对象
     * @param[in]  applicationName: Application名称
     * @return
     * int: Partition ID，如果找不到则返回0
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static int findPartitionIdForApplication(Module module, String applicationName) {
        Map<String, Partition> partitions = module.getfPartitions();
        if (partitions == null) return 0;
        
        for (Map.Entry<String, Partition> partEntry : partitions.entrySet()) {
            Partition partition = partEntry.getValue();
            if (partition != null && applicationName.equals(partition.getfApplicationNameRef())) {
                return partition.getfIdentifier();
            }
        }
        /* 如果找不到对应的分区，返回默认值0 */
        return 0;
    }
    
    /**
     * findPartitionNameForApplication
     *
     * @brief
     * 功能: 根据Application名称查找对应的Partition名称
     * 
     * @param[in]  module: Module对象
     * @param[in]  applicationName: Application名称
     * @return
     * String: Partition名称，如果找不到则返回null
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static String findPartitionNameForApplication(Module module, String applicationName) {
        Map<String, Partition> partitions = module.getfPartitions();
        if (partitions == null) return null;
        
        for (Map.Entry<String, Partition> partEntry : partitions.entrySet()) {
            Partition partition = partEntry.getValue();
            if (partition != null && applicationName.equals(partition.getfApplicationNameRef())) {
                return partition.getfNameAsString();
            }
        }
        /* 如果找不到对应的分区，返回null */
        return null;
    }
    
    /**
     * findChannelIdForPort
     *
     * @brief
     * 功能: 根据端口名称和分区名称查找对应的Channel ID，查找逻辑如下：
     *       获取port的name和partition
     *       遍历所有Channel
     *       在Channel的所有Source和Destination端点中查找
     *       同时匹配端口名和分区名，找到则返回Channel ID
     * 
     * @param[in]  module: Module对象
     * @param[in]  portName: 端口名称
     * @param[in]  direction: 端口方向（"Source" 或 "Destination"），保留参数以保持接口兼容，但不再使用
     * @param[in]  partitionName: 端口所属的分区名称
     * @return
     * int: Channel ID，如果找不到则返回0
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static int findChannelIdForPort(Module module, String portName, String direction, String partitionName) {
        if (module == null || portName == null || portName.isEmpty()) {
            return 0;
        }
        
        Map<Integer, Channel> channels = module.getfChannels();
        if (channels == null || channels.isEmpty()) {
            return 0;
        }
        
        for (Map.Entry<Integer, Channel> channelEntry : channels.entrySet()) {
            Channel channel = channelEntry.getValue();
            if (channel == null) continue;

            /* 遍历所有Source端点查找匹配的PortNameRef和PartitionNameRef */
            if (channel.getSources() != null) {
                for (Channel.ChannelEndpoint source : channel.getSources()) {
                    if (source != null && 
                        portName.equals(source.getPortNameRef()) &&
                        (partitionName == null || partitionName.equals(source.getPartitionNameRef()))) {
                        return channel.getId();
                    }
                }
            }
            
            /* 遍历所有Destination端点查找匹配的PortNameRef和PartitionNameRef */
            if (channel.getDestinations() != null) {
                for (Channel.ChannelEndpoint destination : channel.getDestinations()) {
                    if (destination != null && 
                        portName.equals(destination.getPortNameRef()) &&
                        (partitionName == null || partitionName.equals(destination.getPartitionNameRef()))) {
                        return channel.getId();
                    }
                }
            }
        }
        
        /* 如果找不到对应的Channel，返回0 */
        return 0;
    }
    
    /**
     * getPortName
     *
     * @brief
     * 功能: 获取端口名称
     * 
     * @param[in]  port: 端口对象
     * @return
     * String: 端口名称字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static String getPortName(Port port) {
        if (port == null) {
            return "UnknownPort";
        }
        
        if (port instanceof QueuingPort) {
            QueuingPort qp = (QueuingPort) port;
            return qp.getfName() != null ? new String(qp.getfName()) : "UnknownQueuingPort";
        } else if (port instanceof SamplingPort) {
            SamplingPort sp = (SamplingPort) port;
            return sp.getfName() != null ? new String(sp.getfName()) : "UnknownSamplingPort";
        }
        
        return "UnknownPort";
    }

    /**
     * getDriverName
     *
     * @brief
     * 功能: 获取端口驱动名称
     * 
     * @param[in]  port: 端口对象
     * @return
     * String: 驱动名称字符串
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static String getDriverName(Port port) {
        if (port == null) {
            return "";
        }
        
        /* 从端口对象中获取驱动名称 */
        String driverName = port.getfDriverName();
        
        /* 如果驱动名称为空或null，则根据端口类型提供默认驱动名称 */
        if (driverName == null || driverName.trim().isEmpty()) {
                return "";
        }
        
        /* 返回去除前后空格的驱动名称 */
        return driverName.trim();
    }

    /**
     * getMaxNbMessage
     *
     * @brief
     * 功能: 获取端口最大消息数
     * 
     * @param[in]  port: 端口对象
     * @return
     * int: 最大消息数
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static int getMaxNbMessage(Port port) {
        if (port instanceof QueuingPort) {
            QueuingPort qp = (QueuingPort) port;
            return (int) qp.getfMaxNbMessage();
        } else if (port instanceof SamplingPort) {
            /* 采样端口通常只有一个消息 */
            return 0;
        }
        /* 默认值 */
        return 0;
    }

    /**
     * getProtocolType
     *
     * @brief
     * 功能: 获取端口协议类型
     * 
     * @param[in]  port: 端口对象
     * @return
     * int: 协议类型数值
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static int getProtocolType(Port port) {
        if (port == null) {
            return 0;
        }
        
        /* 对于QueuingPort，尝试获取协议类型 */
        if (port instanceof QueuingPort) {
            QueuingPort qp = (QueuingPort) port;
            String protocolType = qp.getfProtocolType();
            
            if (protocolType != null && !protocolType.trim().isEmpty()) {
                /* 尝试将协议类型字符串映射为数值 */
                return mapProtocolTypeString(protocolType.trim());
            }
        }
        
        /* 对于SamplingPort或其他端口类型，可以根据端口类型返回默认协议类型 */
        if (port instanceof SamplingPort) {
            /* 采样端口默认协议类型为2 */
            return 2;
        }
        
        /* 默认协议类型 */
        return 2;
    }
    
    /**
     * mapProtocolTypeString
     *
     * @brief
     * 功能: 将协议类型字符串映射为数值
     * 
     * @param[in]  protocolType: 协议类型字符串
     * @return
     * int: 协议类型数值
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static int mapProtocolTypeString(String protocolType) {
        if (protocolType == null || protocolType.isEmpty()) {
            return 0;
        }
        
        /* 直接解析为数字 */
        try {
            return Integer.parseInt(protocolType);
        } catch (NumberFormatException e) {
            System.err.println("Warning: unknown protocol type '" + protocolType + "', using default value 0");
            return 0;
        }
    }

    /**
     * mapDirection
     *
     * @brief
     * 功能: 将Direction字符串映射为数值，对应关系如下：
     *       Source       0
     *       Destination  1
     * 
     * @param[in]  direction: 方向字符串（"Source" 或 "Destination"）
     * @return
     * int: 0表示Source，1表示Destination，默认为Source
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static int mapDirection(String direction) {
        if ("Source".equalsIgnoreCase(direction)) {
            return 0;
        } else if ("Destination".equalsIgnoreCase(direction)) {
            return 1;
        }
        /* 默认为Source */
        return 0;
    }

    /**
     * mapRefreshRate
     *
     * @brief
     * 功能: 将RefreshRate字符串转换为long值，对应关系如下：
     *       INFINITE_TIME  -1
     *       十进制/十六进制 对应的long值
     *       解析失败        0
     * 
     * @param[in]  refreshRate: 刷新率字符串
     * @return
     * long: long值，INFINITE_TIME返回-1，解析失败返回0
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static long mapRefreshRate(String refreshRate) {
        if (refreshRate == null || refreshRate.isEmpty()) {
            return 0;
        }
        
        /* 处理INFINITE_TIME特殊情况 */
        if ("INFINITE_TIME".equalsIgnoreCase(refreshRate.trim())) {
            /* INFINITE_TIME对应的值是-1 */
            return -1;
        }
        
        String value = refreshRate.trim();
        try {
            if (value.startsWith("+")) {
                value = value.substring(1);
            }
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseLong(value.substring(2), 16);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            System.err.println("Warning: cannot parse RefreshRate '" + refreshRate + "', returning 0");
            return 0;
        }
    }

    /**
     * mapPortAttribute
     *
     * @brief
     * 功能: 根据Port属性映射端口类型，对应关系如下：
     *       NULL_PORT            0
     *       LOCAL_PORT           1
     *       PSEUDO_PORT          2
     *       DIRECT_ACCESS_PORT   3
     * 
     * @param[in]  port: Port对象
     * @param[in]  module: Module对象，用于查找分区信息
     * @param[in]  partitionName: 端口所属的分区名称
     * @return
     * int: 端口类型数值
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static int mapPortAttribute(Port port, Module module, String partitionName) {
        if (port == null) {
            /* 默认为LOCAL_PORT */
            return 1;
        }
        
        /* 首先检查端口自身是否设置了Attribute属性 */
        String attribute = port.getfAttribute();
        if (attribute != null && !attribute.trim().isEmpty()) {
            /* 端口有明确的Attribute值，使用Port类的映射方法 */
            return port.getAttributeAsInt();
        }
        
        /* 端口没有设置Attribute，根据端口所属的分区类型设置默认值 */
        if (module != null && partitionName != null && !partitionName.isEmpty()) {
            Map<String, Partition> partitions = module.getfPartitions();
            if (partitions != null && partitions.containsKey(partitionName)) {
                Partition partition = partitions.get(partitionName);
                if (partition != null) {
                    /* 检查是否为虚分区 */
                    if (partition.isFIsPseudoPartition()) {
                        /* PSEUDO_PORT */
                        return 2;
                    } else {
                        /* LOCAL_PORT（分区端口） */
                        return 1;
                    }
                }
            }
        }
        
        /* 如果无法确定分区信息，默认为LOCAL_PORT */
        return 1;
    }

    /**
     * mapPortAttribute
     *
     * @brief
     * 功能: 根据Port属性映射端口类型，当无法提供分区信息时使用此方法
     * 
     * @param[in]  port: Port对象
     * @return
     * int: 端口类型数值
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static int mapPortAttribute(Port port) {
        if (port == null) {
            /* 默认为LOCAL_PORT */
            return 1;
        }
        
        /* 检查端口自身是否设置了Attribute属性 */
        String attribute = port.getfAttribute();
        if (attribute != null && !attribute.trim().isEmpty()) {
            /* 端口有明确的Attribute值，使用Port类的映射方法 */
            return port.getAttributeAsInt();
        }
        
        /* 端口没有设置Attribute，返回默认值LOCAL_PORT */
        return 1;
    }

    /**
     * mapPortAttribute
     *
     * @brief
     * 功能: 基于UnifiedPort映射端口属性类型，该方法可以自动获取分区信息进行智能映射
     * 
     * @param[in]  unifiedPort: UnifiedPort对象
     * @param[in]  module: Module对象
     * @return
     * int: 端口类型数值
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static int mapPortAttribute(UnifiedPort unifiedPort, Module module) {
        if (unifiedPort == null || unifiedPort.getPort() == null) {
            /* 默认为LOCAL_PORT */
            return 1;
        }
        
        Port port = unifiedPort.getPort();
        
        /* 首先检查端口自身是否设置了Attribute属性 */
        String attribute = port.getfAttribute();
        if (attribute != null && !attribute.trim().isEmpty()) {
            /* 端口有明确的Attribute值，使用Port类的映射方法 */
            return port.getAttributeAsInt();
        }
        
        /* 端口没有设置Attribute，根据端口所属的分区类型设置默认值 */
        if (module != null) {
            Map<String, Partition> partitions = module.getfPartitions();
            if (partitions != null) {
                /* 通过分区ID查找分区 */
                int partitionId = unifiedPort.getParentPartitionId();
                for (Partition partition : partitions.values()) {
                    if (partition != null && partition.getfIdentifier() == partitionId) {
                        /* 检查是否为虚分区 */
                        if (partition.isFIsPseudoPartition()) {
                            /* PSEUDO_PORT */
                            return 2;
                        } else {
                            /* LOCAL_PORT（分区端口） */
                            return 1;
                        }
                    }
                }
            }
        }
        
        /* 如果无法确定分区信息，默认为LOCAL_PORT */
        return 1;
    }
}
