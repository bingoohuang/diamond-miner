package org.n3r.diamond.server.controller;

import org.apache.commons.lang3.StringUtils;
import org.n3r.diamond.server.utils.Constants;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class DiamondServlet extends HttpServlet {
    private static final long serialVersionUID = 4339468526746635388L;

    private DiamondController diamondController;

    @Override
    public void init() throws ServletException {
        super.init();
        WebApplicationContext webApplicationContext =
                WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        diamondController = (DiamondController) webApplicationContext.getBean("diamondController");
    }

    public void forward(HttpServletRequest request, HttpServletResponse response, String page, String basePath,
                        String postfix) throws IOException, ServletException {
        RequestDispatcher requestDispatcher = request.getRequestDispatcher(basePath + page + postfix);
        requestDispatcher.forward(request, response);
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        String probeModify = request.getParameter(Constants.PROBE_MODIFY_REQUEST);

        if (StringUtils.isEmpty(probeModify)) throw new IOException("无效的probeModify");

        String page = diamondController.getProbeModifyResult(request, response, probeModify);
        forward(request, response, page, "/jsp/", ".jsp");
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String group = request.getParameter("group");
        String dataId = request.getParameter("dataId");

        if (StringUtils.isEmpty(dataId)) throw new IOException("无效的dataId");

        String page = diamondController.getConfig(request, response, dataId, group);
        if (!"OK".equals(page)) forward(request, response, page, "/jsp/", ".jsp");
    }
}
