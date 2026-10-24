package com.relay.api;

/**
 * 早期初始化钩子。功能如果需要注册网络通道、编解码器之类"必须在进服前完成"的东西，
 * 实现本接口即可；核心在 onInitialize 阶段用 ServiceLoader 发现并调用。
 *
 * 与 {@link RelayFeature} 分开的原因：RelayFeature 在服务器启动后才加载，
 * 而网络通道必须在客户端初始化之前注册，否则客户端会崩。
 */
public interface RelayBootstrap {

    String id();

    void onInitialize();
}
