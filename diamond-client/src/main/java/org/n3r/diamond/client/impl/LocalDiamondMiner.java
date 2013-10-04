package org.n3r.diamond.client.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.n3r.diamond.client.DiamondStone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.io.File.separator;
import static org.n3r.diamond.client.impl.Constants.*;

class LocalDiamondMiner {
    private Logger log = LoggerFactory.getLogger(LocalDiamondMiner.class);

    private Map<String/* filePath */, Long/* timestamp */> existFilesTimestamp = new HashMap<String, Long>();

    private volatile boolean running;
    private String rootPath = null;
    private FileAlterationMonitor monitor;


    /**
     * 获取本地配置
     *
     * @param force 强制获取，在没有变更的时候不返回null
     */
    public String getLocal(DiamondMeta diamondMeta, boolean force) {
        String filePath = getFilePath(diamondMeta.getDiamondAxis());
        if (!existFilesTimestamp.containsKey(filePath)) {
            if (diamondMeta.isUseLocal()) diamondMeta.clear();

            return null;
        }

        if (force) {
            log.info("主动从本地获取配置数据, {}", diamondMeta.getDiamondAxis());

            return readFileContent(filePath);
        }

        // 判断是否变更，没有变更，返回null
        if (!filePath.equals(diamondMeta.getLocalFile())
                || existFilesTimestamp.get(filePath) != diamondMeta.getLocalVersion()) {
            diamondMeta.setLocalFile(filePath);
            diamondMeta.setLocalVersion(existFilesTimestamp.get(filePath));
            diamondMeta.setUseLocal(true);
            log.info("本地配置数据发生变化, {}", diamondMeta.getDiamondAxis());

            return readFileContent(filePath);
        } else {
            diamondMeta.setUseLocal(true);
            log.debug("本地配置数据没有发生变化,{}", diamondMeta.getDiamondAxis());

            return null;
        }
    }

    String readFileContent(String filePath) {
        try {
            return FileUtils.readFileToString(new File(filePath), ENCODING);
        } catch (IOException e) {
            log.error("readfile content fail {}", e.getMessage());
            return null;
        }
    }


    String getFilePath(DiamondStone.DiamondAxis diamondAxis) {
        File file = new File(rootPath + separator
                + diamondAxis.getGroup() + separator
                + diamondAxis.getDataId() + DIAMOND_STONE_EXT);
        return file.getAbsolutePath();
    }


    public synchronized void start(DiamondManagerConf managerConfig) {
        if (running) return;

        running = true;
        rootPath = managerConfig.getFilePath() + separator + DATA_DIR;

        initDataDir();
        startCheckLocalDir();
    }

    private void watchRoot() {
        FileAlterationObserver observer = new FileAlterationObserver(rootPath);
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                String realPath = file.getAbsolutePath();
                String grandpaDir = getGrandpaDir(realPath);
                if (!rootPath.equals(grandpaDir)
                        || !DIAMOND_STONE_EXT.equals("." + FilenameUtils.getExtension(realPath))) {
                    log.error("无效的文件进入监控目录{} ", file);
                    return;
                }

                existFilesTimestamp.put(realPath, System.currentTimeMillis());
                log.info("File {} Created", realPath);
            }

            @Override
            public void onFileDelete(File file) {
                String realPath = file.getAbsolutePath();
                String grandpaDir = getGrandpaDir(realPath);
                if (rootPath.equals(grandpaDir)
                        && DIAMOND_STONE_EXT.equals("." + FilenameUtils.getExtension(realPath))) {
                    existFilesTimestamp.remove(realPath);
                    log.info("File {} Delete", realPath);
                }
            }

        });
        monitor = new FileAlterationMonitor(5000);
        monitor.addObserver(observer);
        try {
            monitor.start();
        } catch (Exception e) {
            log.error("start monitor fail", e);
        }
    }


    private void initDataDir() {
        try {
            new File(rootPath).mkdir();
        } catch (Exception e) {
        }
    }


    public synchronized void stop() {
        if (!running) return;
        running = false;

        if (monitor != null) try {
            monitor.stop();
        } catch (Exception e) {
            log.error("stop monitor fail", e);
        }
    }


    private void index(File file) {
        if (!file.isDirectory()) return;

        File[] subFiles = file.listFiles();
        for (File subFile : subFiles) {
            if (subFile.isDirectory()) {
                index(subFile);
            } else {
                String realPath = subFile.getAbsolutePath();
                String grandpaDir = getGrandpaDir(realPath);
                if (!rootPath.equals(grandpaDir)
                        || !DIAMOND_STONE_EXT.equals("." + FilenameUtils.getExtension(realPath))) {
                    log.error("无效的文件进入监控目录{} ", subFile);
                    continue;
                }

                existFilesTimestamp.put(realPath, System.currentTimeMillis());
                log.info("{}文件被初始添加", realPath);
            }
        }
    }

    private void startCheckLocalDir() {
        index(new File(rootPath));
        watchRoot();
    }


    public String getGrandpaDir(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            log.error("此路径表达的不是文件{}", path);
            return null;
        }

        File parent = file.getParentFile();
        if (parent != null) {
            File grandpa = parent.getParentFile();
            if (grandpa != null) {
                return grandpa.getAbsolutePath();
            }
        }
        log.error("取得祖父目录失败{}", path);
        return null;
    }
}
