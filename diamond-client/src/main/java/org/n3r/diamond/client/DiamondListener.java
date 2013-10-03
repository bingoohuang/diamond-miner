package org.n3r.diamond.client;

import java.util.concurrent.Executor;


/**
 * 客户如果想接收DataID对应的配置信息，需要自己实现一个监听器
 */
public interface DiamondListener {

    Executor getExecutor();


    void accept(DiamondStone diamondStone);
}
