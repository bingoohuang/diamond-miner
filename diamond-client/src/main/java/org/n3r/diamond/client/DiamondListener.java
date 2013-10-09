package org.n3r.diamond.client;

import java.util.concurrent.Executor;

public interface DiamondListener {
    Executor getExecutor();

    void accept(DiamondStone diamondStone);
}
