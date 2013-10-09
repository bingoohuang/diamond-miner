package org.n3r.diamond.client.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Futures;
import org.joor.Reflect;
import org.n3r.diamond.client.DiamondStone;
import org.n3r.diamond.client.impl.DiamondSubstituter;
import org.n3r.diamond.client.impl.SnapshotMiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class DiamondCache {
    private final SnapshotMiner snapshotMiner;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Logger log = LoggerFactory.getLogger(DiamondCache.class);
    private Cache<DiamondStone.DiamondAxis, Future<Object>> cache = CacheBuilder.newBuilder().build();

    public DiamondCache(SnapshotMiner snapshotMiner) {
        this.snapshotMiner = snapshotMiner;
    }

    public Object getCache(final DiamondStone.DiamondAxis diamondAxis, final String diamondContent) {
        Callable<Future<Object>> callable = new Callable<Future<Object>>() {
            @Override
            public Future<Object> call() throws Exception {
                return executorService.submit(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return updateCache(diamondAxis, diamondContent);
                    }
                });
            }
        };

        Future<Object> cachedObject;
        try {
            cachedObject = cache.get(diamondAxis, callable);
        } catch (ExecutionException e) {
            log.error("get cache {} failed", diamondContent, e);
            return null;
        }

        return futureGet(diamondAxis, diamondContent, cachedObject);
    }

    private Object futureGet(DiamondStone.DiamondAxis diamondAxis,
                             String diamondContent, Future<Object> future) {
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {   // 有限时间内不返回，尝试读取snapshot版本
            log.error("update cache {} timeout, try to use snapshot", diamondContent);
            Object object = snapshotMiner.getCache(diamondAxis);
            if (object != null) return object;
        } catch (Exception e) {
            log.error("update cache {} failed", diamondContent, e);
        }

        try {
            return future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            log.error("update cache {} failed", diamondContent, e);
        }

        return null;
    }

    private Object updateCache(DiamondStone.DiamondAxis diamondAxis, String diamondContent) {
        log.info("start to update cache {}", diamondAxis);
        if (isEmpty(diamondContent)) return null;

        diamondContent = DiamondSubstituter.substitute(diamondContent, true);

        Spec spec;
        try {
            spec = SpecParser.parseSpecLeniently(diamondContent);
        } catch (Exception e) {
            log.error("parse {} failed", diamondContent, e);
            removeCacheSnapshot(diamondAxis);
            return null;
        }

        Object object = Reflect.on(spec.getName()).create().get();
        if (object instanceof ParamsAppliable)
            ((ParamsAppliable) object).applyParams(spec.getParams());

        if (!(object instanceof Callable)) {
            log.error("{} cannot be parsed as DiamondCachable", diamondContent);
            removeCacheSnapshot(diamondAxis);
        }

        Callable diamondCachable = (Callable) object;
        Object diamondCache = null;
        try {
            diamondCache = diamondCachable.call();
        } catch (Exception e) {
            log.error("{} called with exception", diamondContent, e);
            removeCacheSnapshot(diamondAxis);
        }

        snapshotMiner.saveCache(diamondAxis, diamondCache);
        log.info("end to update cache {}", diamondAxis);

        return diamondCache;
    }

    public void close() {
        executorService.shutdownNow();
    }

    public void saveDiamondCache(final DiamondStone.DiamondAxis diamondAxis,
                                 final String diamondContent) {
        Future<Object> cacheOptional = cache.getIfPresent(diamondAxis);
        if (cacheOptional == null) return;

        Callable<Object> task = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final Object updated = updateCache(diamondAxis, diamondContent);
                cache.put(diamondAxis, Futures.immediateFuture(updated));

                return updated;
            }
        };

        executorService.submit(task);
    }

    public void removeCacheSnapshot(DiamondStone.DiamondAxis diamondAxis) {
        cache.invalidate(diamondAxis);
        snapshotMiner.removeCache(diamondAxis);
    }


}
