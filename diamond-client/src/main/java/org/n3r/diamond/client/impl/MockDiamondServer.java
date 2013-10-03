package org.n3r.diamond.client.impl;

import org.n3r.diamond.client.DiamondStone;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class MockDiamondServer {
    private static ConcurrentHashMap<DiamondStone.DiamondAxis, String>
            mocks = new ConcurrentHashMap<DiamondStone.DiamondAxis, String>();
    private static volatile boolean testMode = false;

    public static void setUpMockServer() {
        testMode = true;
    }

    public static void tearDownMockServer() {
        mocks.clear();
        DiamondSubscriber.getInstance().close();
        testMode = false;
    }


    public static String getDiamond(DiamondStone.DiamondAxis diamondAxis) {
        return mocks.get(diamondAxis);
    }


    public static void setConfigInfos(Map<String, String> configInfos) {
        if (null == configInfos) return;

        for (Map.Entry<String, String> entry : configInfos.entrySet()) {
            setConfigInfo(entry.getKey(), entry.getValue());
        }
    }

    public static void setConfigInfo(String dataId, String configInfo) {
        setConfigInfo(DiamondStone.DiamondAxis.makeAxis(dataId), configInfo);
    }

    public static void setConfigInfo(String group, String dataId, String configInfo) {
        setConfigInfo(DiamondStone.DiamondAxis.makeAxis(group, dataId), configInfo);
    }

    private static void setConfigInfo(DiamondStone.DiamondAxis diamondAxis, String configInfo) {
        mocks.putIfAbsent(diamondAxis, configInfo);
    }

    public static boolean isTestMode() {
        return testMode;
    }
}