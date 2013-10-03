package org.n3r.diamond.client;

import org.n3r.diamond.client.cache.ParamsAppliable;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Callable;

public class DemoUpdater implements Callable<DemoCacheBean>, ParamsAppliable {
    private String param;

    @Override
    public DemoCacheBean call() {
        sleepMillis(10 * 1000);
        String s = " at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        System.out.println(">>>DemoUpdater:" + s);
        return new DemoCacheBean(param + s);
    }

    @Override
    public void applyParams(String[] params) {
        this.param = Arrays.toString(params);
    }

    public static void sleepMillis(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}

