package org.n3r.diamond.server.service;

import org.n3r.diamond.server.domain.DiamondStone;
import org.n3r.diamond.server.domain.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


@Service
public class TimerTaskService {
    private Logger log = LoggerFactory.getLogger(TimerTaskService.class);
    private static final int DUMP_INTERVAL = 600; // 10 min
    private static final int PAGE_SIZE = 1000;
    private static final String THREAD_NAME = "diamond dump config thread";

    @Autowired
    private PersistService persistService;
    @Autowired
    private DiskService diskService;
    @Autowired
    private DiamondService diamondService;

    private ScheduledExecutorService scheduler;


    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName(THREAD_NAME);
                t.setDaemon(true);
                return t;
            }
        });

        Runnable dumpTask = new DumpDiamondTask();
        dumpTask.run();

        scheduler.scheduleWithFixedDelay(dumpTask, DUMP_INTERVAL, DUMP_INTERVAL, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void dispose() {
        if (scheduler != null) scheduler.shutdown();
    }

    private class DumpDiamondTask implements Runnable {
        public void run() {
            try {
                Page<DiamondStone> page = persistService.findAllConfigInfo(1, PAGE_SIZE);
                if (page == null) return;

                updateConfigInfo(page);

                int totalPages = page.getTotalPages();
                for (int pageNo = 2; pageNo <= totalPages; pageNo++) {
                    page = persistService.findAllConfigInfo(pageNo, PAGE_SIZE);
                    if (page != null) updateConfigInfo(page);
                }
            } catch (Throwable t) {
                log.error("dump task run error", t);
            }
        }


        private void updateConfigInfo(Page<DiamondStone> page) throws IOException {
            for (DiamondStone diamondStone : page.getPageItems()) {
                if (diamondStone == null) continue;

                try {
                    if (diamondStone.isValid()) {
                        diamondService.updateMD5Cache(diamondStone);
                        diskService.saveToDisk(diamondStone);
                    } else {
                        diamondService.removeConfigInfo(diamondStone.getId());
                    }
                } catch (Throwable t) {
                    log.error("dump config info error, dataId="
                            + diamondStone.getDataId() + ", group=" + diamondStone.getGroup(), t);
                }

            }
        }

    }

}
