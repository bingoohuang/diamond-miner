package org.n3r.diamond.server.service;

import com.google.common.base.Throwables;
import org.apache.commons.codec.digest.DigestUtils;
import org.n3r.diamond.server.domain.DiamondStone;
import org.n3r.diamond.server.domain.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.util.StringUtils.containsWhitespace;
import static org.springframework.util.StringUtils.hasLength;


@Service
public class DiamondService {
    private Logger log = LoggerFactory.getLogger(DiamondService.class);

    @Autowired
    private PersistService persistService;

    @Autowired
    private DiskService diskService;

    @Autowired
    private NotifyService notifyService;

    /**
     * content的MD5的缓存,key为group/dataId，value为md5值
     */
    private final ConcurrentHashMap<String, String> contentMD5Cache = new ConcurrentHashMap<String, String>();

    public void updateMD5Cache(DiamondStone diamondStone) {
        contentMD5Cache.put(createMD5CacheKey(diamondStone.getDataId(),
                diamondStone.getGroup()), DigestUtils.md5Hex(diamondStone.getContent()));
    }

    public String getContentMD5(String dataId, String group) {
        String key = createMD5CacheKey(dataId, group);
        String md5 = contentMD5Cache.get(key);
        if (md5 == null) {
            synchronized (this) {
                // 二重检查
                return contentMD5Cache.get(key);
            }
        } else {
            return md5;
        }
    }


    String createMD5CacheKey(String dataId, String group) {
        return group + "/" + dataId;
    }

    public void removeConfigInfo(long id) {
        try {
            DiamondStone diamondStone = persistService.findConfigInfo(id);
            diskService.removeConfigInfo(diamondStone.getDataId(), diamondStone.getGroup());
            contentMD5Cache.remove(createMD5CacheKey(diamondStone.getDataId(), diamondStone.getGroup()));
            persistService.removeConfigInfo(diamondStone);
            notifyOtherNodes(diamondStone.getDataId(), diamondStone.getGroup());

        } catch (Exception e) {
            log.error("删除配置信息错误", e);
            throw Throwables.propagate(e);
        }
    }

    public void addConfigInfo(String dataId, String group, String content, String description, boolean valid) {
        checkParameter(dataId, group, content);
        DiamondStone diamondStone = new DiamondStone(dataId, group, content, description, valid);
        // 保存顺序：先数据库，再磁盘
        try {
            persistService.addConfigInfo(diamondStone);
            contentMD5Cache.put(createMD5CacheKey(dataId, group), diamondStone.getMd5());
            diskService.saveToDisk(diamondStone);
            notifyOtherNodes(dataId, group);
        } catch (Exception e) {
            log.error("保存ConfigInfo失败", e);
            throw Throwables.propagate(e);
        }
    }

    public void updateConfigInfo(String dataId, String group, String content, String description, boolean valid) {
        checkParameter(dataId, group, content);
        DiamondStone diamondStone = new DiamondStone(dataId, group, content, description, valid);
        // 先更新数据库，再更新磁盘
        try {
            persistService.updateConfigInfo(diamondStone);

            // 切记更新缓存
            String key = createMD5CacheKey(dataId, group);
            if (valid) contentMD5Cache.put(key, diamondStone.getMd5());
            else contentMD5Cache.remove(key);

            diskService.saveToDisk(diamondStone);

            notifyOtherNodes(dataId, group);
        } catch (Exception e) {
            log.error("保存ConfigInfo失败", e);
            throw Throwables.propagate(e);
        }
    }

    public void loadConfigInfoToDisk(String dataId, String group) {
        try {
            DiamondStone diamondStone = persistService.findConfigInfo(dataId, group);
            if (diamondStone != null && diamondStone.isValid()) {
                contentMD5Cache.put(createMD5CacheKey(dataId, group), diamondStone.getMd5());
                diskService.saveToDisk(diamondStone);
            } else {
                contentMD5Cache.remove(createMD5CacheKey(dataId, group));
                diskService.removeConfigInfo(dataId, group);
            }
        } catch (Exception e) {
            log.error("保存ConfigInfo到磁盘失败", e);
            throw Throwables.propagate(e);
        }
    }


    public DiamondStone findConfigInfo(String dataId, String group) {
        return persistService.findConfigInfo(dataId, group);
    }


    public Page<DiamondStone> findConfigInfo(final int pageNo, final int pageSize, final String group, final String dataId) {
        if (hasLength(dataId) && hasLength(group)) {
            DiamondStone DiamondStone = persistService.findConfigInfo(dataId, group);
            if (DiamondStone != null) return null;

            Page<DiamondStone> page = new Page<DiamondStone>();
            page.setPageNo(1);
            page.setTotalPages(1);
            page.getPageItems().add(DiamondStone);

            return page;
        } else if (hasLength(dataId) && !hasLength(group)) {
            return persistService.findConfigInfoByDataId(pageNo, pageSize, dataId);
        } else if (!hasLength(dataId) && hasLength(group)) {
            return persistService.findConfigInfoByGroup(pageNo, pageSize, group);
        } else {
            return persistService.findAllConfigInfo(pageNo, pageSize);
        }
    }

    public Page<DiamondStone> findConfigInfoLike(final int pageNo, final int pageSize, final String group,
                                                 final String dataId) {
        return persistService.findConfigInfoLike(pageNo, pageSize, dataId, group);
    }


    private void checkParameter(String dataId, String group, String content) {
        if (!hasLength(dataId) || containsWhitespace(dataId))
            throw new RuntimeException("无效的dataId");

        if (!hasLength(group) || containsWhitespace(group))
            throw new RuntimeException("无效的group");

        if (!hasLength(content))
            throw new RuntimeException("无效的content");
    }

    private void notifyOtherNodes(String dataId, String group) {
        notifyService.notifyConfigInfoChange(dataId, group);
    }

}
