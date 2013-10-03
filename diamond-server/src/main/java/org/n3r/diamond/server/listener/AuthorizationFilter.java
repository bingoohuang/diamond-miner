package org.n3r.diamond.server.listener;


import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;


public class AuthorizationFilter implements Filter {

    public void destroy() {

    }


    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession();
        // 判断是否登录，没有就跳转到登录页面
        if (session.getAttribute("user") == null)
            ((HttpServletResponse) response).sendRedirect(
                    httpRequest.getContextPath() + "/jsp/login.jsp");
        else
            chain.doFilter(httpRequest, response);
    }


    public void init(FilterConfig filterConfig) throws ServletException {

    }

}
