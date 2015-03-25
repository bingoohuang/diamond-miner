package org.n3r.diamond.server.service;

import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.n3r.diamond.server.domain.DiamondStone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static java.io.File.separator;
import static org.n3r.diamond.server.utils.Constants.*;


@Service
public class DiskService {
    private Logger log = LoggerFactory.getLogger(DiskService.class);
    private String filePath;
    // 修改标记缓存
    private ConcurrentHashMap<String/* dataId + group */, Boolean/* 是否正在修改 */> modifyMarkCache =
            new ConcurrentHashMap<String, Boolean>();

    private String getOrCreateFilePath() {
        if (StringUtils.isNotEmpty(filePath)) return filePath;

        filePath = System.getProperty("user.home") + separator + ".diamond-server";
        log.info("create dump home dir {}", filePath);

        File dir = new File(filePath);
        dir.mkdirs();
        if (!dir.exists()) throw new RuntimeException("fail to create diamond-miner root" + filePath);

        return filePath;
    }

    public File getDiamondFile(String dataId, String group) {
        File diamondFile = new File(getOrCreateFilePath() + separator + BASE_DIR
                + separator + group + separator + dataId + DIAMOND_STONE_EXT);

        return diamondFile;
    }

    public void updateToDisk(DiamondStone diamondStone) {
        if (!diamondStone.isValid()) {
            removeConfigInfo(diamondStone.getDataId(), diamondStone.getGroup());
            return;
        }

        String group = diamondStone.getGroup();
        String dataId = diamondStone.getDataId();
        String content = diamondStone.getContent();
        File tempFile = null;

        String cacheKey = generateCacheKey(group, dataId);
        // 标记正在写磁盘
        if (modifyMarkCache.putIfAbsent(cacheKey, true) != null)
            throw new RuntimeException("config info is being modified, dataId=" + dataId + ",group=" + group);

        try {
            String groupPath = getFilePath(BASE_DIR + separator + group);
            new File(groupPath).mkdirs();
            File targetFile = createFileIfNecessary(groupPath, dataId + DIAMOND_STONE_EXT);
            tempFile = createTempFile(dataId, group);
            FileUtils.writeStringToFile(tempFile, content, ENCODING);
            FileUtils.copyFile(tempFile, targetFile);
        } catch (Exception e) {
            log.error("save disk error, dataId={},group={}, error={}", dataId, group, e.getMessage());
            throw Throwables.propagate(e);
        } finally {
            FileUtils.deleteQuietly(tempFile);
            modifyMarkCache.remove(cacheKey); // 清除标记
        }
    }

    public boolean isUnderModifying(String dataId, String group) {
        return modifyMarkCache.get(generateCacheKey(group, dataId)) != null;
    }

    public final String generateCacheKey(String group, String dataId) {
        return group + "/" + dataId;
    }

    public void removeConfigInfo(String dataId, String group) {
        String cacheKey = generateCacheKey(group, dataId);
        // 标记正在写磁盘
        if (modifyMarkCache.putIfAbsent(cacheKey, true) != null)
            throw new RuntimeException("config info is being modified, dataId=" + dataId + ",group=" + group);

        try {
            String basePath = getFilePath(BASE_DIR);
            new File(basePath).mkdirs();

            String groupPath = getFilePath(BASE_DIR + separator + group);
            File groupDir = new File(groupPath);
            if (!groupDir.exists()) return;

            String dataPath = getFilePath(BASE_DIR + separator
                    + group + separator + dataId + DIAMOND_STONE_EXT);
            File dataFile = new File(dataPath);
            if (!dataFile.exists()) return;

            FileUtils.deleteQuietly(dataFile);
        } catch (Exception e) {
            log.error("delete config info error, dataId={},group={}, error={}", dataId, group, e.getMessage());
            throw Throwables.propagate(e);
        } finally {
            modifyMarkCache.remove(cacheKey); // 清除标记
        }
    }

    private String getFilePath(String dir) throws FileNotFoundException {
        return getOrCreateFilePath() + separator + dir; // // WebUtils.getRealPath(servletContext,
    }

    private File createFileIfNecessary(String parent, String child) throws IOException {
        final File file = new File(parent, child);
        if (!file.exists()) {
            file.createNewFile();
            changeFilePermission(file);
        }
        return file;
    }

    private void changeFilePermission(File file) {
        // 600
        file.setExecutable(false, false);
        file.setWritable(false, false);
        file.setReadable(false, false);
        file.setExecutable(false, true);
        file.setWritable(true, true);
        file.setReadable(true, true);
    }

    private File createTempFile(String dataId, String group) throws IOException {
        return File.createTempFile(group + "-" + dataId, ".tmp");
    }
}
