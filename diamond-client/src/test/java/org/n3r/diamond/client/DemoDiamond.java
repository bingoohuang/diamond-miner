package org.n3r.diamond.client;

import java.util.concurrent.Executor;

public class DemoDiamond {
    public static void main(String[] args) {
        DiamondManager diamondManager = new DiamondManager("foo");
        diamondManager.addDiamondListener(new DiamondListener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void accept(DiamondStone diamondStone) {
                System.out.println("DiamondListener:" + diamondStone.getContent());
            }
        });

        long start = System.currentTimeMillis();
        String diamond = diamondManager.getDiamond();
        System.out.println((System.currentTimeMillis() - start) + ", diamond:" + diamond);

        start = System.currentTimeMillis();
        DemoCacheBean cached = (DemoCacheBean) diamondManager.getCache();
        System.out.println((System.currentTimeMillis() - start) + ", cached:" + cached);


        while (true) {
            sleepMillis(10000);
            start = System.currentTimeMillis();
            diamond = DiamondMiner.getString("foo");
            System.out.println((System.currentTimeMillis() - start) + ", diamond:" + diamond);

            start = System.currentTimeMillis();
            cached = DiamondMiner.getCache("foo");
            System.out.println((System.currentTimeMillis() - start) + ", cached:" + cached);
        }

//
//        int time = 10;
//        while (time-- > 0) {
//            String diamond = diamondManager.getDiamond();
//            System.out.println("diamond:" + diamond);
//            DiamondMiner.sleepMillis(10000);
//        }

//        DiamondSubscriber.getInstance().close();
    }


    public static void sleepMillis(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
