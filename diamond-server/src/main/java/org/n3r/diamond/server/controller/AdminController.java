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

        diamondService.addConfigInfo(dataId, group, content, description, valid);

        modelMap.addAttribute("message", "提交成功!");
        return listConfig(request, response, dataId, group, 1, 20, modelMap);
    }

    private String checkDataIdGroupAndContent(String dataId, String group, String content) {
        String errorMessage = null;
        if (StringUtils.isBlank(dataId) || hasInvalidChar(dataId.trim())) {
            errorMessage = "无效的DataId";
        }
        if (StringUtils.isBlank(group) || hasInvalidChar(group.trim())) {
            errorMessage = "无效的分组";
        }
        if (StringUtils.isBlank(content)) {
            errorMessage = "无效的内容";
        }
        return errorMessage;
    }


    @RequestMapping(params = "method=deleteConfig", method = RequestMethod.GET)
    public String deleteConfig(HttpServletRequest request, HttpServletResponse response,
                               @RequestParam("id") long id,
                               ModelMap modelMap) {
        diamondService.removeConfigInfo(id);
        modelMap.addAttribute("message", "删除成功!");
        return "/admin/config/list";
    }


    @RequestMapping(params = "method=upload", method = RequestMethod.POST)
    public String upload(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam("dataId") String dataId,
                         @RequestParam("group") String group,
                         @RequestParam(value = "description", required = false) String description,
                         @RequestParam(value = "valid", required = false) boolean valid,
                         @RequestParam("contentFile") MultipartFile contentFile,
                         ModelMap modelMap) {
        response.setCharacterEncoding("UTF-8");

        String content = getContentFromFile(contentFile);
        String errorMessage = checkDataIdGroupAndContent(dataId, group, content);
        if (errorMessage != null) {
            modelMap.addAttribute("message", errorMessage);
            return "/admin/config/upload";
        }

        diamondService.addConfigInfo(dataId, group, content, description, valid);
        modelMap.addAttribute("message", "提交成功!");
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

        diamondService.updateConfigInfo(dataId, group, content, description, valid);

        modelMap.addAttribute("message", "更新成功!");
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
                               @RequestParam(value = "description") String description,
                               @RequestParam(value = "valid", required = false) boolean valid,
                               ModelMap modelMap) {
        response.setCharacterEncoding("UTF-8");

        DiamondStone diamondStone = new DiamondStone(dataId, group, content, description, valid);
        String errorMessage = checkDataIdGroupAndContent(dataId, group, content);
        if (errorMessage != null) {
            modelMap.addAttribute("message", errorMessage);
            modelMap.addAttribute("diamondStone", diamondStone);
            return "/admin/config/edit";
        }

        diamondService.updateConfigInfo(dataId, group, content, description, valid);

        modelMap.addAttribute("message", "提交成功!");
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

        String accept = request.getHeader("Accept");
        if (accept != null && accept.indexOf("application/json") >= 0) {
            try {
                modelMap.addAttribute("pageJson", JSON.toJSONString(page));
            } catch (Exception e) {
                log.error("序列化page对象出错", e);
            }
            return "/admin/config/list_json";
        } else {
            modelMap.addAttribute("dataId", dataId);
            modelMap.addAttribute("group", group);
            modelMap.addAttribute("page", page);
            return "/admin/config/list";
        }
    }


    @RequestMapping(params = "method=listConfigLike", method = RequestMethod.GET)
    public String listConfigLike(HttpServletRequest request, HttpServletResponse response,
                                 @RequestParam("dataId") String dataId,
                                 @RequestParam("group") String group,
                                 @RequestParam("pageNo") int pageNo,
                                 @RequestParam("pageSize") int pageSize,
                                 ModelMap modelMap) {
        if (StringUtils.isBlank(dataId) && StringUtils.isBlank(group)) {
            modelMap.addAttribute("message", "模糊查询请至少设置一个查询参数");
            return "/admin/config/list";
        }

        Page<DiamondStone> page = diamondService.findConfigInfoLike(pageNo, pageSize, group, dataId);

        String accept = request.getHeader("Accept");
        if (accept != null && accept.indexOf("application/json") >= 0) {
            try {
                modelMap.addAttribute("pageJson", JSON.toJSONString(page));
            } catch (Exception e) {
                log.error("序列化page对象出错", e);
            }

            return "/admin/config/list_json";
        } else {
            modelMap.addAttribute("page", page);
            modelMap.addAttribute("dataId", dataId);
            modelMap.addAttribute("group", group);
            modelMap.addAttribute("method", "listConfigLike");
            return "/admin/config/list";
        }
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


    // =========================== 批量处理 ============================== //

    @RequestMapping(params = "method=batchQuery", method = RequestMethod.POST)
    public String batchQuery(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam("dataIds") String dataIds,
                             @RequestParam("group") String group,
                             ModelMap modelMap) {

        response.setCharacterEncoding("UTF-8");

        // 这里抛出的异常, 会产生一个500错误, 返回给sdk, sdk会将500错误记录到日志中
        if (StringUtils.isBlank(dataIds)) {
            throw new IllegalArgumentException("批量查询, dataIds不能为空");
        }
        // group对批量操作的每一条数据都相同, 不需要在for循环里面进行判断
        if (StringUtils.isBlank(group)) {
            throw new IllegalArgumentException("批量查询, group不能为空或者包含非法字符");
        }

        // 分解dataId
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

                // 查询数据库
                DiamondStone diamondStone = diamondService.findConfigInfo(dataId, group);
                if (diamondStone == null) {
                    // 没有异常, 说明查询成功, 但数据不存在, 设置不存在的状态码
                    configInfoEx.setStatus(Constants.BATCH_QUERY_NONEXISTS);
                    configInfoEx.setMessage("query data does not exist");
                } else {
                    // 没有异常, 说明查询成功, 而且数据存在, 设置存在的状态码
                    String content = diamondStone.getContent();
                    configInfoEx.setContent(content);
                    configInfoEx.setStatus(Constants.BATCH_QUERY_EXISTS);
                    configInfoEx.setMessage("query success");
                }
            } catch (Exception e) {
                log.error("批量查询, 在查询这个dataId时出错, dataId=" + dataId + ",group=" + group, e);
                // 出现异常, 设置异常状态码
                configInfoEx.setStatus(Constants.BATCH_OP_ERROR);
                configInfoEx.setMessage("query error: " + e.getMessage());
            }
        }

        String json = null;
        try {
            json = JSON.toJSONString(configInfoExList);
        } catch (Exception e) {
            log.error("批量查询结果序列化出错, json=" + json, e);
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

        // 这里抛出的异常, 会产生一个500错误, 返回给sdk, sdk会将500错误记录到日志中
        if (StringUtils.isBlank(allDataIdAndContent)) {
            throw new IllegalArgumentException("批量写, allDataIdAndContent不能为空");
        }
        // group对批量操作的每一条数据都相同, 不需要在for循环里面进行判断
        if (StringUtils.isBlank(group) || hasInvalidChar(group)) {
            throw new IllegalArgumentException("批量写, group不能为空或者包含非法字符");
        }

        String[] dataIdAndContentArray = allDataIdAndContent.split(Constants.LINE_SEPARATOR);
        group = group.trim();

        List<DiamondStone> configInfoExList = new ArrayList<DiamondStone>();
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
                    // 这里抛出的异常, 会在下面catch, 然后设置状态, 保证一个dataId的异常不会影响其他dataId
                    throw new IllegalArgumentException("批量写, dataId不能包含非法字符");
                }
                if (StringUtils.isBlank(content)) {
                    throw new IllegalArgumentException("批量写, 内容不能为空");
                }

                // 查询数据库
                DiamondStone diamondStone = diamondService.findConfigInfo(dataId, group);
                if (diamondStone == null) {
                    // 数据不存在, 新增
                    diamondService.addConfigInfo(dataId, group, content, description, valid);
                    // 新增成功, 设置状态码
                    configInfoEx.setStatus(Constants.BATCH_ADD_SUCCESS);
                    configInfoEx.setMessage("add success");
                } else {
                    // 数据存在, 更新
                    diamondService.updateConfigInfo(dataId, group, content, description, valid);
                    // 更新成功, 设置状态码
                    configInfoEx.setStatus(Constants.BATCH_UPDATE_SUCCESS);
                    configInfoEx.setMessage("update success");
                }
            } catch (Exception e) {
                log.error("批量写这条数据时出错, dataId=" + dataId + ",group=" + group + ",content=" + content, e);
                // 出现异常, 设置异常状态码
                configInfoEx.setStatus(Constants.BATCH_OP_ERROR);
                configInfoEx.setMessage("batch write error: " + e.getMessage());
            }
            configInfoExList.add(configInfoEx);
        }

        String json = null;
        try {
            json = JSON.toJSONString(configInfoExList);
        } catch (Exception e) {
            log.error("批量写, 结果序列化出错, json=" + json, e);
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
            modelMap.addAttribute("message", "无效的用户名");
            return listUser(request, response, modelMap);
        }
        if (StringUtils.isBlank(password) || hasInvalidChar(password.trim())) {
            modelMap.addAttribute("message", "无效的密码");
            return "/admin/user/new";
        }
        if (adminService.addUser(userName, password))
            modelMap.addAttribute("message", "添加成功!");
        else
            modelMap.addAttribute("message", "添加失败!");
        return listUser(request, response, modelMap);
    }


    @RequestMapping(params = "method=deleteUser", method = RequestMethod.GET)
    public String deleteUser(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam("userName") String userName, ModelMap modelMap) {
        if (StringUtils.isBlank(userName) || hasInvalidChar(userName.trim())) {
            modelMap.addAttribute("message", "无效的用户名");
            return listUser(request, response, modelMap);
        }
        if (adminService.removeUser(userName)) {
            modelMap.addAttribute("message", "删除成功!");
        } else {
            modelMap.addAttribute("message", "删除失败!");
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
            modelMap.addAttribute("message", "无效的用户名");
            return listUser(request, response, modelMap);
        }
        if (StringUtils.isBlank(password) || hasInvalidChar(password.trim())) {
            modelMap.addAttribute("message", "无效的新密码");
            return listUser(request, response, modelMap);
        }
        if (adminService.updatePassword(userName, password)) {
            modelMap.addAttribute("message", "更改成功,下次登录请用新密码！");
        } else {
            modelMap.addAttribute("message", "更改失败!");
        }
        return listUser(request, response, modelMap);
    }


    @RequestMapping(params = "method=setRefuseRequestCount", method = RequestMethod.POST)
    public String setRefuseRequestCount(@RequestParam("count") long count, ModelMap modelMap) {
        if (count <= 0) {
            modelMap.addAttribute("message", "非法的计数");
            return "/admin/count";
        }
        GlobalCounter.getCounter().set(count);
        modelMap.addAttribute("message", "设置成功!");
        return getRefuseRequestCount(modelMap);
    }


    @RequestMapping(params = "method=getRefuseRequestCount", method = RequestMethod.GET)
    public String getRefuseRequestCount(ModelMap modelMap) {
        modelMap.addAttribute("count", GlobalCounter.getCounter().get());
        return "/admin/count";
    }


    /**
     * 重新文件加载用户信息
     */
    @RequestMapping(params = "method=reloadUser", method = RequestMethod.GET)
    public String reloadUser(HttpServletRequest request, HttpServletResponse response,
                             ModelMap modelMap) {
        adminService.loadUsers();
        modelMap.addAttribute("message", "加载成功!");
        return listUser(request, response, modelMap);
    }
}
