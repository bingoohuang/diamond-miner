package org.n3r.diamond.server.utils;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;


public class Json {
    private static Logger log = LoggerFactory.getLogger(Json.class);

    public static String processJson(HttpServletRequest request, ModelMap modelMap, Object page) {
        String accept = request.getHeader("Accept");
        if (accept == null || accept.indexOf("application/json") < 0) return null;

        try {
            modelMap.addAttribute("pageJson", JSON.toJSONString(page));
        } catch (Exception e) {
            log.error("Json serialize page error", e);
        }

        return "/admin/config/list_json";
    }

}
