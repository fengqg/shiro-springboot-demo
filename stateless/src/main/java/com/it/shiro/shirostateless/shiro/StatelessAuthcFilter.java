package com.it.shiro.shirostateless.shiro;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.it.shiro.shirostateless.shiro.token.manager.TokenManager;
import com.it.shiro.shirostateless.shiro.token.token.StatelessToken;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * @author fengqigui
 * @description 无状态授权过滤器
 * @date 2018/03/07 16:48
 */
public class StatelessAuthcFilter extends AccessControlFilter {


    private final Logger logger = LoggerFactory.getLogger(StatelessAuthcFilter.class);

    @Autowired
    private TokenManager tokenManager;

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request,
                                      ServletResponse response, Object mappedValue) throws Exception {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        logger.info("拦截到的url:" + httpRequest.getRequestURL().toString());
        // 前段token授权信息放在请求头中传入
        String authorization = getReqHeader(
                (HttpServletRequest) request, "authorization");

        if (StringUtils.isEmpty(authorization)) {
            onLoginFail(response, "请求头不包含认证信息authorization");
            return false;
        }
        // 获取无状态Token
        StatelessToken accessToken = tokenManager.getToken(authorization);
        try {
            // 委托给Realm进行登录
            getSubject(request, response).login(accessToken);
        } catch (Exception e) {
            logger.error("auth error:" + e.getMessage(), e);
            onLoginFail(response, "auth error:" + e.getMessage()); // 6、登录失败
            return false;
        }
        // 通过isPermitted 才能调用doGetAuthorizationInfo方法获取权限信息
        getSubject(request, response).isPermitted(httpRequest.getRequestURI());
        return true;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request,
                                     ServletResponse response) throws Exception {
        return false;
    }

    //登录失败时默认返回401状态码
    private void onLoginFail(ServletResponse response,String errorMsg) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.setContentType("text/html");
        httpResponse.setCharacterEncoding("utf-8");
        httpResponse.getWriter().write(errorMsg);
        httpResponse.getWriter().close();
    }

    private String getReqHeader(HttpServletRequest req, String header){

        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()){
            String nextElement = headerNames.nextElement();
            if(header.equals(nextElement)){
                return req.getHeader(nextElement);
            }
        }
        return null;
    }



}
