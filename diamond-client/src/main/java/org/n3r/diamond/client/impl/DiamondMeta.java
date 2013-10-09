package org.n3r.diamond.client.impl;

import org.n3r.diamond.client.DiamondStone;

import java.util.concurrent.atomic.AtomicLong;

class DiamondMeta {
    private DiamondStone.DiamondAxis diamondAxis;

    private volatile String lastModifiedHeader = Constants.NULL;
    private volatile String md5 = Constants.NULL;

    private volatile String localFile = null;
    private volatile long localVersion;

    private volatile boolean useLocal = false;

    private AtomicLong succCounter = new AtomicLong(0);

    public DiamondMeta(DiamondStone.DiamondAxis diamondAxis) {
        this.diamondAxis = diamondAxis;
    }

    public DiamondStone.DiamondAxis getDiamondAxis() {
        return diamondAxis;
    }

    public long getFetchCount() {
        return succCounter.get();
    }

    public long incSuccCounterAndGet() {
        return succCounter.incrementAndGet();
    }


    public String getLocalFile() {
        return localFile;
    }

    public void setLocalFile(String localFile) {
        this.localFile = localFile;
    }

    public long getLocalVersion() {
        return localVersion;
    }

    public void setLocalVersion(long localVersion) {
        this.localVersion = localVersion;
    }

    public boolean isUseLocal() {
        return useLocal;
    }

    public void setUseLocal(boolean useLocal) {
        this.useLocal = useLocal;
    }


    public String getLastModifiedHeader() {
        return lastModifiedHeader;
    }

    public void setLastModifiedHeader(String lastModifiedHeader) {
        this.lastModifiedHeader = lastModifiedHeader;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiamondMeta diamondMeta = (DiamondMeta) o;

        if (diamondAxis != null ? !diamondAxis.equals(diamondMeta.diamondAxis) : diamondMeta.diamondAxis != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return diamondAxis != null ? diamondAxis.hashCode() : 0;
    }

    public void clear() {
        setLastModifiedHeader(Constants.NULL);
        setMd5(Constants.NULL);
        setLocalFile(null);
        setLocalVersion(0L);
        setUseLocal(false);
    }
}
