package org.n3r.diamond.server.controller;

import org.n3r.diamond.server.service.DiamondService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/notify.do")
public class NotifyController {

    @Autowired
    private DiamondService diamondService;

    @RequestMapping(method = RequestMethod.GET, params = "method=notifyConfigInfo")
    public String notifyConfigInfo(@RequestParam("dataId") String dataId,
                                   @RequestParam("group") String group) {
        diamondService.loadConfigInfoToDisk(dataId.trim(), group.trim());
        return "200";
    }

}
