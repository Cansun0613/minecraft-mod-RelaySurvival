package com.relay.api;

import java.util.Optional;

/**
 * 可选功能之间共享服务用的注册表：功能 A 注册接口实现，功能 B 按接口取用。
 * 两者互不引用实现类。
 */
public interface RelayServices {

    <T> void register(Class<T> type, T implementation);

    <T> Optional<T> find(Class<T> type);

    /** 某些功能需要每秒拿到一次全局状态（例如 HUD 同步），实现此接口并注册即可。 */
    interface SecondListener {
        void onSecond(RelayContext ctx);
    }
}
