package org.n3r.diamond.client.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.io.FileUtils;
import org.n3r.diamond.client.DiamondStone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.n3r.diamond.client.impl.Constants.*;
import static java.io.File.separator;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public class SnapshotMiner {
    private Logger log = LoggerFactory.getLogger(SnapshotMiner.class);
    private final String dir;

    public SnapshotMiner(DiamondManagerConf managerConfig) {
        dir = managerConfig.getFilePath() + separator + SNAPSHOT_DIR;
        File file = new File(dir);
        file.mkdirs();
    }

    public String getSnapshot(DiamondStone.DiamondAxis diamondAxis) throws IOException {
        return getFileContent(diamondAxis, DIAMOND_STONE_EXT);
    }

    private String getFileContent(DiamondStone.DiamondAxis diamondAxis, String extension) throws IOException {
        File file = new File(dir + separator + diamondAxis.getGroup()
                + separator + diamondAxis.getDataId() + extension);
        if (!file.exists()) return null;

        return FileUtils.readFileToString(file, ENCODING);
    }

    public void saveSnaptshot(DiamondStone.DiamondAxis diamondAxis, String content) {
        try {
            File file = getOrCreateDiamondFile(diamondAxis, DIAMOND_STONE_EXT);
            FileUtils.writeStringToFile(file, defaultIfEmpty(content, ""), ENCODING);


        } catch (IOException e) {
            log.error("save snapshot error {} by {}", diamondAxis, content, e);
        }
    }

    public void removeSnapshot(DiamondStone.DiamondAxis diamondAxis) {
        removeSnapshot(diamondAxis, DIAMOND_STONE_EXT);
    }

    public void removeSnapshot(DiamondStone.DiamondAxis diamondAxis, String extension) {
        String path = dir + separator + diamondAxis.getGroup();
        File dir = new File(path);
        if (!dir.exists()) return;

        File file = new File(path + separator + diamondAxis.getDataId() + extension);
        if (!file.exists()) return;

        file.delete();

        if (dir.list().length == 0) dir.delete();
    }

    private File getOrCreateDiamondFile(DiamondStone.DiamondAxis diamondAxis, String extension) throws IOException {
        String path = dir + separator + diamondAxis.getGroup();
        File dir = new File(path);
        if (!dir.exists()) dir.mkdir();

        File file = new File(path + separator + diamondAxis.getDataId() + extension);
        if (!file.exists()) file.createNewFile();

        return file;
    }

    public void saveCache(DiamondStone.DiamondAxis diamondAxis, Object diamondCache) {
        String json = JSON.toJSONString(diamondCache, SerializerFeature.WriteClassName);
        try {
            File file = getOrCreateDiamondFile(diamondAxis, DIAMOND_CACHE_EXT);
            FileUtils.writeStringToFile(file, json, ENCODING);
        } catch (IOException e) {
            log.error("save {} cache snaptshot error", diamondAxis, e);
        }
    }

    public Object getCache(DiamondStone.DiamondAxis diamondAxis) {
        try {
            String fileContent = getFileContent(diamondAxis, DIAMOND_CACHE_EXT);
            if (fileContent == null) return null;

            return JSON.parse(fileContent);
        } catch (IOException e) {
            log.error("read cache snapshot {} failed {}", e.getMessage());
        }

        return null;
    }

    public void removeCache(DiamondStone.DiamondAxis diamondAxis) {
        removeSnapshot(diamondAxis, DIAMOND_CACHE_EXT);
    }
}
