package org.n3r.diamond.server.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import org.apache.commons.lang3.StringUtils;
import org.n3r.diamond.server.domain.DiamondStone;
import org.n3r.diamond.server.domain.Page;
import org.n3r.diamond.server.service.AdminService;
import org.n3r.diamond.server.service.DiamondService;
import org.n3r.diamond.server.service.PersistService;
import org.n3r.diamond.server.utils.Constants;
import org.n3r.diamond.server.utils.Encrypt;
import org.n3r.diamond.server.utils.Json;
import org.n3r.diamond.server.utils.GlobalCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/admin.do")
public class AdminController {
    private Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;
    @Autowired
    private DiamondService diamondService;
    @Autowired
    private PersistService persistService;

    @RequestMapping(params = "method=postConfig", method = RequestMethod.POST)
    public String postConfig(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam("dataId") String dataId,
                             @RequestParam("group") String group,
                             @RequestParam("content") String content,
                             @RequestParam(value = "description", required = false) String description,
                             @RequestParam(value = "valid", required = false) boolean valid,
                             @RequestParam(value = "encrypt", required = false) boolean encrypt,
                             ModelMap modelMap) {
        response.setCharacterEncoding("UTF-8");
        dataId = dataId.trim();
        group = group.trim();

        String errorMessage = checkDataIdGroupAndContent(dataId, group, content);

        if (errorMessage == null && persistService.findConfigInfo(dataId, group) != null)
            errorMessage = "dataId=" + dataId + ", group=" + group + " exists already!";

        if (errorMessage != null) {
            modelMap.addAttribute("message", errorMessage);
            return "/admin/config/new";
        }

        content = Encrypt.tryToEncryptContent(encrypt, dataId, content);

        diamondService.addConfigInfo(dataId, group, content, description, valid);

        modelMap.addAttribute("message", "Submit Successfully!");
        return listConfig(request, response, dataId, group, 1, 20, modelMap);
    }

    private String checkDataIdGroupAndContent(String dataId, String group, String content) {
        String errorMessage = null;
        if (StringUtils.isBlank(dataId) || hasInvalidChar(dataId.trim())) {
            errorMessage = "Invalid DataId";
        }
        if (StringUtils.isBlank(group) || hasInvalidChar(group.trim())) {
            errorMessage = "Invalid Group";
        }
        if (StringUtils.isBlank(content)) {
            errorMessage = "Invalid Content";
        }
        return errorMessage;
    }


    @RequestMapping(params = "method=deleteConfig", method = RequestMethod.GET)
    public String deleteConfig(HttpServletRequest request, HttpServletResponse response,
                               @RequestParam("id") long id,
                               ModelMap modelMap) {
        diamondService.removeConfigInfo(id);
        String result = Json.processJson(request, modelMap, "Delete successfully!");
        if (result != null) return result;

        modelMap.addAttribute("message", "Delete successfully!");
        return "/admin/config/list";
    }


    @RequestMapping(params = "method=upload", method = RequestMethod.POST)
    public String upload(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam("dataId") String dataId,
                         @RequestParam("group") String group,
                         @RequestParam(value = "description", required = false) String description,
                         @RequestParam(value = "valid", required = false) boolean valid,
                         @RequestParam(value = "encrypt", required = false) boolean encrypt,
                         @RequestParam("contentFile") MultipartFile contentFile,
                         ModelMap modelMap) {
        response.setCharacterEncoding("UTF-8");

        String content = getContentFromFile(contentFile);
        String errorMessage = checkDataIdGroupAndContent(dataId, group, content);
        if (errorMessage != null) {
            modelMap.addAttribute("message", errorMessage);
            return "/admin/config/upload";
        }

        content = Encrypt.tryToEncryptContent(encrypt, dataId, content);

        diamondService.addConfigInfo(dataId, group, content, description, valid);
        modelMap.addAttribute("message", "Submit Successfully!");
        return listConfig(request, response, dataId, group, 1, 20, modelMap);
    }

    private boolean hasInvalidChar(String str) {
        return !CharMatcher.JAVA_LETTER_OR_DIGIT.or(CharMatcher.anyOf("._")).matchesAllOf(str);
    }

    @RequestMapping(params = "method=reupload", method = RequestMethod.POST)
    public String reupload(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam("dataId") String dataId,
                           @RequestParam("group") String group,
                           @RequestParam(value = "description", required = false) String description,
                           @RequestParam(value = "valid", required = false) boolean valid,
                           @RequestParam(value = "encrypt", required = false) boolean encrypt,
                           @RequestParam("contentFile") MultipartFile contentFile,
                           ModelMap modelMap) {
        response.setCharacterEncoding("UTF-8");

        String content = getContentFromFile(contentFile);
        DiamondStone diamondStone = new DiamondStone(dataId, group, content, description, valid);

        String errorMessage = checkDataIdGroupAndContent(dataId, group, content);
        if (errorMessage != null) {
            modelMap.addAttribute("message", errorMessage);
            modelMap.addAttribute("configInfo", diamondStone);
            return "/admin/config/edit";
        }

        content = Encrypt.tryToEncryptContent(encrypt, dataId, content);
        diamondService.updateConfigInfo(dataId, group, content, description, valid);

        modelMap.addAttribute("message", "Update Successfully!");
        return listConfig(request, response, dataId, group, 1, 20, modelMap);
    }


    private String getContentFromFile(MultipartFile contentFile) {
        try {
            return new String(contentFile.getBytes(), Charsets.UTF_8);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }


    @RequestMapping(params = "method=updateConfig", method = RequestMethod.POST)
    public String updateConfig(HttpServletRequest request, HttpServletResponse response,
                               @RequestParam("dataId") String dataId,
                               @RequestParam("group") String group,
                               @RequestParam("content") String content,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam(value = "valid", required = false) boolean valid,
                               @RequestParam(value = "encrypt", required = false) boolean encrypt,
                               ModelMap modelMap) {
        response.setCharacterEncoding("UTF-8");

        DiamondStone diamondStone = new DiamondStone(dataId, group, content, description, valid);
        String errorMessage = checkDataIdGroupAndContent(dataId, group, content);
        if (errorMessage != null) {
            modelMap.addAttribute("message", errorMessage);
            modelMap.addAttribute("diamondStone", diamondStone);
            return "/admin/config/edit";
        }

        content = Encrypt.tryToEncryptContent(encrypt, dataId, content);

        diamondService.updateConfigInfo(dataId, group, content, description, valid);

        modelMap.addAttribute("message", "Submit Successfully!");
        return listConfig(request, response, dataId, group, 1, 20, modelMap);
    }


    @RequestMapping(params = "method=listConfig", method = RequestMethod.GET)
    public String listConfig(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam("dataId") String dataId,
                             @RequestParam("group") String group,
                             @RequestParam("pageNo") int pageNo,
                             @RequestParam("pageSize") int pageSize,
                             ModelMap modelMap) {
        Page<DiamondStone> page = diamondService.findConfigInfo(pageNo, pageSize, group, dataId);

        String result = Json.processJson(request, modelMap, page);
        if (result != null) return result;

        modelMap.addAttribute("dataId", dataId);
        modelMap.addAttribute("group", group);
        modelMap.addAttribute("page", page);
        return "/admin/config/list";
    }


    @RequestMapping(params = "method=listConfigLike", method = RequestMethod.GET)
    public String listConfigLike(HttpServletRequest request, HttpServletResponse response,
                                 @RequestParam("dataId") String dataId,
                                 @RequestParam("group") String group,
                                 @RequestParam("pageNo") int pageNo,
                                 @RequestParam("pageSize") int pageSize,
                                 ModelMap modelMap) {
        if (StringUtils.isBlank(dataId) && StringUtils.isBlank(group)) {
            modelMap.addAttribute("message", "Fuzzy query should has at least one param");
            return "/admin/config/list";
        }

        Page<DiamondStone> page = diamondService.findConfigInfoLike(pageNo, pageSize, group, dataId);

        String result = Json.processJson(request, modelMap, page);
        if (result != null) return result;

        modelMap.addAttribute("page", page);
        modelMap.addAttribute("dataId", dataId);
        modelMap.addAttribute("group", group);
        modelMap.addAttribute("method", "listConfigLike");
        return "/admin/config/list";
    }


    @RequestMapping(params = "method=detailConfig", method = RequestMethod.GET)
    public String getConfigInfo(HttpServletRequest request, HttpServletResponse response,
                                @RequestParam("dataId") String dataId,
                                @RequestParam("group") String group,
                                ModelMap modelMap) {
        dataId = dataId.trim();
        group = group.trim();
        DiamondStone diamondStone = diamondService.findConfigInfo(dataId, group);
        modelMap.addAttribute("diamondStone", diamondStone);
        return "/admin/config/edit";
    }


    @RequestMapping(params = "method=batchQuery", method = RequestMethod.POST)
    public String batchQuery(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam("dataIds") String dataIds,
                             @RequestParam("group") String group,
                             ModelMap modelMap) {

        response.setCharacterEncoding("UTF-8");

        if (StringUtils.isBlank(dataIds)) {
            throw new IllegalArgumentException("Batch Query, dataIds should not be empty");
        }
        if (StringUtils.isBlank(group)) {
            throw new IllegalArgumentException("Batch Query, group should not be empty");
        }

        String[] dataIdArray = dataIds.split(Constants.WORD_SEPARATOR);
        group = group.trim();

        List<DiamondStone> configInfoExList = new ArrayList<DiamondStone>();
        for (String dataId : dataIdArray) {
            DiamondStone configInfoEx = new DiamondStone();
            configInfoEx.setDataId(dataId);
            configInfoEx.setGroup(group);
            configInfoExList.add(configInfoEx);
            try {
                if (StringUtils.isBlank(dataId)) {
                    configInfoEx.setStatus(Constants.BATCH_QUERY_NONEXISTS);
                    configInfoEx.setMessage("dataId is blank");
                    continue;
                }

                DiamondStone diamondStone = diamondService.findConfigInfo(dataId, group);
                if (diamondStone == null) {
                    configInfoEx.setStatus(Constants.BATCH_QUERY_NONEXISTS);
                    configInfoEx.setMessage("query data does not exist");
                } else {
                    String content = diamondStone.getContent();
                    configInfoEx.setContent(content);
                    configInfoEx.setStatus(Constants.BATCH_QUERY_EXISTS);
                    configInfoEx.setMessage("query success");
                }
            } catch (Exception e) {
                log.error("Batch query error on dataId={}, group=", dataId, group, e);
                configInfoEx.setStatus(Constants.BATCH_OP_ERROR);
                configInfoEx.setMessage("query error: " + e.getMessage());
            }
        }

        String json = null;
        try {
            json = JSON.toJSONString(configInfoExList);
        } catch (Exception e) {
            log.error("Batch query Json serialize error", e);
        }
        modelMap.addAttribute("json", json);

        return "/admin/config/batch_result";
    }


    @RequestMapping(params = "method=batchAddOrUpdate", method = RequestMethod.POST)
    public String batchAddOrUpdate(HttpServletRequest request, HttpServletResponse response,
                                   @RequestParam("allDataIdAndContent") String allDataIdAndContent,
                                   @RequestParam("group") String group,
                                   ModelMap modelMap) {

        response.setCharacterEncoding("UTF-8");

        if (StringUtils.isBlank(allDataIdAndContent)) {
            throw new IllegalArgumentException("Batch Update, allDataIdAndContent should not be empty");
        }
        if (StringUtils.isBlank(group) || hasInvalidChar(group)) {
            throw new IllegalArgumentException("Batch Update, group should not be empty or with invalid letters");
        }

        String[] dataIdAndContentArray = allDataIdAndContent.split(Constants.LINE_SEPARATOR);
        group = group.trim();

        List<DiamondStone> configInfoExList = new ArrayList<>();
        for (String dataIdAndContent : dataIdAndContentArray) {
            String[] split = dataIdAndContent.split(Constants.WORD_SEPARATOR);
            String dataId = split[0];
            String content = split[1];
            String description = split[2];
            boolean valid = Boolean.parseBoolean(split[3]);
            DiamondStone configInfoEx = new DiamondStone();
            configInfoEx.setDataId(dataId);
            configInfoEx.setGroup(group);
            configInfoEx.setContent(content);
            configInfoEx.setDescription(description);
            configInfoEx.setValid(valid);

            try {
                if (StringUtils.isBlank(dataId) || hasInvalidChar(dataId)) {
                    throw new IllegalArgumentException("Batch Update, dataId should not has invlid letters");
                }
                if (StringUtils.isBlank(content)) {
                    throw new IllegalArgumentException("Batch Update, content should not be blank");
                }

                DiamondStone diamondStone = diamondService.findConfigInfo(dataId, group);
                if (diamondStone == null) {
                    diamondService.addConfigInfo(dataId, group, content, description, valid);
                    configInfoEx.setStatus(Constants.BATCH_ADD_SUCCESS);
                    configInfoEx.setMessage("add success");
                } else {
                    diamondService.updateConfigInfo(dataId, group, content, description, valid);
                    configInfoEx.setStatus(Constants.BATCH_UPDATE_SUCCESS);
                    configInfoEx.setMessage("update success");
                }
            } catch (Exception e) {
                log.error("Batch Update error on dataId={},group={}", dataId, group + ",content=" + content, e);
                configInfoEx.setStatus(Constants.BATCH_OP_ERROR);
                configInfoEx.setMessage("batch write error: " + e.getMessage());
            }
            configInfoExList.add(configInfoEx);
        }

        String json = null;
        try {
            json = JSON.toJSONString(configInfoExList);
        } catch (Exception e) {
            log.error("Batch update Json serialize error", e);
        }
        modelMap.addAttribute("json", json);

        return "/admin/config/batch_result";
    }


    @RequestMapping(params = "method=listUser", method = RequestMethod.GET)
    public String listUser(HttpServletRequest request, HttpServletResponse response,
                           ModelMap modelMap) {
        Map<String, String> userMap = adminService.getAllUsers();
        modelMap.addAttribute("userMap", userMap);
        return "/admin/user/list";
    }


    @RequestMapping(params = "method=addUser", method = RequestMethod.POST)
    public String addUser(HttpServletRequest request, HttpServletResponse response,
                          @RequestParam("userName") String userName,
                          @RequestParam("password") String password, ModelMap modelMap) {
        if (StringUtils.isBlank(userName) || hasInvalidChar(userName.trim())) {
            modelMap.addAttribute("message", "Invalid username");
            return listUser(request, response, modelMap);
        }
        if (StringUtils.isBlank(password) || hasInvalidChar(password.trim())) {
            modelMap.addAttribute("message", "Invalid password");
            return "/admin/user/new";
        }
        if (adminService.addUser(userName, password))
            modelMap.addAttribute("message", "Add successfully!");
        else
            modelMap.addAttribute("message", "Add failed!");

        return listUser(request, response, modelMap);
    }


    @RequestMapping(params = "method=deleteUser", method = RequestMethod.GET)
    public String deleteUser(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam("userName") String userName, ModelMap modelMap) {
        if (StringUtils.isBlank(userName) || hasInvalidChar(userName.trim())) {
            modelMap.addAttribute("message", "Invalid username");
            return listUser(request, response, modelMap);
        }
        if (adminService.removeUser(userName)) {
            modelMap.addAttribute("message", "Delete successfully!");
        } else {
            modelMap.addAttribute("message", "Delete failed!");
        }
        return listUser(request, response, modelMap);
    }


    @RequestMapping(params = "method=changePassword", method = RequestMethod.GET)
    public String changePassword(HttpServletRequest request, HttpServletResponse response,
                                 @RequestParam("userName") String userName,
                                 @RequestParam("password") String password, ModelMap modelMap) {

        userName = userName.trim();
        password = password.trim();

        if (StringUtils.isBlank(userName) || hasInvalidChar(userName.trim())) {
            modelMap.addAttribute("message", "Invalid username");
            return listUser(request, response, modelMap);
        }
        if (StringUtils.isBlank(password) || hasInvalidChar(password.trim())) {
            modelMap.addAttribute("message", "Invalid new password");
            return listUser(request, response, modelMap);
        }
        if (adminService.updatePassword(userName, password)) {
            modelMap.addAttribute("message", "Change password successfully.");
        } else {
            modelMap.addAttribute("message", "Change password failed");
        }

        return listUser(request, response, modelMap);
    }


    @RequestMapping(params = "method=setRefuseRequestCount", method = RequestMethod.POST)
    public String setRefuseRequestCount(@RequestParam("count") long count, ModelMap modelMap) {
        if (count <= 0) {
            modelMap.addAttribute("message", "Invalid count " + count);
            return "/admin/count";
        }
        GlobalCounter.getCounter().set(count);
        modelMap.addAttribute("message", "Set successfully!");
        return getRefuseRequestCount(modelMap);
    }


    @RequestMapping(params = "method=getRefuseRequestCount", method = RequestMethod.GET)
    public String getRefuseRequestCount(ModelMap modelMap) {
        modelMap.addAttribute("count", GlobalCounter.getCounter().get());
        return "/admin/count";
    }


    @RequestMapping(params = "method=reloadUser", method = RequestMethod.GET)
    public String reloadUser(HttpServletRequest request, HttpServletResponse response,
                             ModelMap modelMap) {
        adminService.loadUsers();
        modelMap.addAttribute("message", "Reload successfully!");
        return listUser(request, response, modelMap);
    }
}
