package org.n3r.diamond.client.impl;

import org.n3r.diamond.client.DiamondStone;
import org.n3r.diamond.client.DiamondListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;


class DiamondAllListener implements DiamondListener {
    private Logger log = LoggerFactory.getLogger(DiamondAllListener.class);

    private final ConcurrentMap<DiamondStone.DiamondAxis,
            CopyOnWriteArrayList<DiamondListener>> allListeners
            = new ConcurrentHashMap<DiamondStone.DiamondAxis, CopyOnWriteArrayList<DiamondListener>>();

    public void accept(final DiamondStone diamondStone) {
        CopyOnWriteArrayList<DiamondListener> listeners = allListeners.get(diamondStone.getDiamondAxis());
        if (listeners == null || listeners.isEmpty()) {
            log.warn("[notify-listener] no listener for {}", diamondStone.getDiamondAxis());
            return;
        }

        for (DiamondListener listener : listeners) {
            try {
                notifyListener(diamondStone, listener);
            } catch (Throwable t) {
                log.error("call listener error, {}", diamondStone.getDiamondAxis(), t);
            }
        }
    }


    private void notifyListener(final DiamondStone diamondStone, final DiamondListener listener) {
        if (listener == null) return;

        log.info("call listener {} for {}", listener, diamondStone.getDiamondAxis());

        Runnable job = new Runnable() {
            public void run() {
                try {
                    listener.accept(diamondStone);
                } catch (Throwable t) {
                    log.error("listener error {}", listener, t);
                }
            }
        };

        if (null != listener.getExecutor()) {
            listener.getExecutor().execute(job);
        } else {
            job.run();
        }
    }

    public void removeManagerListeners(DiamondStone.DiamondAxis diamondAxis) {
        allListeners.remove(diamondAxis);
    }

    public void addDiamondListener(DiamondStone.DiamondAxis diamondAxis, DiamondListener addListener) {
        if (null == addListener) return;

        CopyOnWriteArrayList<DiamondListener> listenerList = allListeners.get(diamondAxis);
        if (listenerList == null) {
            listenerList = new CopyOnWriteArrayList<DiamondListener>();
            CopyOnWriteArrayList<DiamondListener> oldList = allListeners.putIfAbsent(diamondAxis, listenerList);
            if (oldList != null) listenerList = oldList;
        }

        listenerList.add(addListener);
    }

    public void removeDiamondListener(DiamondStone.DiamondAxis diamondAxis, DiamondListener addListener) {
        if (null == addListener) return;

        CopyOnWriteArrayList<DiamondListener> listenerList = allListeners.get(diamondAxis);
        if (listenerList != null) listenerList.remove(addListener);
    }

    public Executor getExecutor() {
        return null;
    }
}
