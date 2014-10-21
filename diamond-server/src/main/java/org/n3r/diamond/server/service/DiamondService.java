package org.n3r.diamond.server.service;

import com.google.common.base.Throwables;
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
     * content's MD5 cache,key is group/dataId，value is md5
     */
    private final ConcurrentHashMap<String, String> contentMD5Cache = new ConcurrentHashMap<String, String>();

    public void updateMD5Cache(DiamondStone diamondStone) {
        String md5CacheKey = createMD5CacheKey(diamondStone.getDataId(), diamondStone.getGroup());
        if (diamondStone.isValid()) {
            contentMD5Cache.put(md5CacheKey, diamondStone.getMd5());
        } else {
            contentMD5Cache.remove(md5CacheKey);
        }
    }

    public String getCacheContentMD5(String dataId, String group) {
        String key = createMD5CacheKey(dataId, group);
        return contentMD5Cache.get(key);
    }


    String createMD5CacheKey(String dataId, String group) {
        return group + "/" + dataId;
    }

    public void removeConfigInfo(String id) {
        try {
            DiamondStone diamondStone = persistService.findConfigInfo(id);
            diskService.removeConfigInfo(diamondStone.getDataId(), diamondStone.getGroup());
            contentMD5Cache.remove(createMD5CacheKey(diamondStone.getDataId(), diamondStone.getGroup()));
            persistService.removeConfigInfo(diamondStone);
            notifyOtherNodes(diamondStone.getDataId(), diamondStone.getGroup());

        } catch (Exception e) {
            log.error("remove config info error", e);
            throw Throwables.propagate(e);
        }
    }

    public void addConfigInfo(String dataId, String group, String content, String description, boolean valid) {
        checkParameter(dataId, group, content);
        DiamondStone diamondStone = new DiamondStone(dataId, group, content, description, valid);
        try {
            persistService.addConfigInfo(diamondStone);
            updateMD5Cache(diamondStone);
            diskService.updateToDisk(diamondStone);
            notifyOtherNodes(dataId, group);
        } catch (Exception e) {
            log.error("addConfigInfo error", e);
            throw Throwables.propagate(e);
        }
    }

    public void updateConfigInfo(String dataId, String group, String content, String description, boolean valid) {
        checkParameter(dataId, group, content);
        DiamondStone diamondStone = new DiamondStone(dataId, group, content, description, valid);
        try {
            persistService.updateConfigInfo(diamondStone);
            updateMD5Cache(diamondStone);
            diskService.updateToDisk(diamondStone);
            notifyOtherNodes(dataId, group);
        } catch (Exception e) {
            log.error("updateConfigInfo error", e);
            throw Throwables.propagate(e);
        }
    }

    public void loadConfigInfoToDisk(String dataId, String group) {
        try {
            DiamondStone diamondStone = persistService.findConfigInfo(dataId, group);
            if (diamondStone != null) {
                updateMD5Cache(diamondStone);
                diskService.updateToDisk(diamondStone);
            }
        } catch (Exception e) {
            log.error("loadConfigInfoToDisk error", e);
            throw Throwables.propagate(e);
        }
    }

    public DiamondStone findConfigInfo(String dataId, String group) {
        return persistService.findConfigInfo(dataId, group);
    }

    public Page<DiamondStone> findConfigInfo(final int pageNo, final int pageSize, final String group, final String dataId) {
        if (hasLength(dataId) && hasLength(group)) {
            DiamondStone DiamondStone = persistService.findConfigInfo(dataId, group);
            if (DiamondStone == null) return null;

            Page<DiamondStone> page = new Page<DiamondStone>();
            page.setPageNo(1);
            page.setTotalPages(1);
            page.getPageItems().add(DiamondStone);

            return page;
        }

        if (hasLength(dataId) && !hasLength(group)) {
            return persistService.findConfigInfoByDataId(pageNo, pageSize, dataId);
        }

        if (!hasLength(dataId) && hasLength(group)) {
            return persistService.findConfigInfoByGroup(pageNo, pageSize, group);
        }

        return persistService.findAllConfigInfo(pageNo, pageSize);
    }

    public Page<DiamondStone> findConfigInfoLike(final int pageNo, final int pageSize, final String group,
                                                 final String dataId) {
        return persistService.findConfigInfoLike(pageNo, pageSize, dataId, group);
    }


    private void checkParameter(String dataId, String group, String content) {
        if (!hasLength(dataId) || containsWhitespace(dataId))
            throw new RuntimeException("Invalid dataId");

        if (!hasLength(group) || containsWhitespace(group))
            throw new RuntimeException("Invalid group");

        if (!hasLength(content))
            throw new RuntimeException("Invalid content");
    }

    private void notifyOtherNodes(String dataId, String group) {
        notifyService.notifyConfigInfoChange(dataId, group);
    }

}
