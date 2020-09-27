package org.truenewx.tnxjeex.cas.server.authentication.logout;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.truenewx.tnxjee.core.Strings;
import org.truenewx.tnxjee.core.util.NetUtil;
import org.truenewx.tnxjee.webmvc.util.WebMvcUtil;
import org.truenewx.tnxjeex.cas.server.util.CasServerConstants;

/**
 * CAS服务端登出成功处理器
 */
public class CasServerLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    @Override
    @Autowired // 覆写以自动注入
    public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
        super.setRedirectStrategy(redirectStrategy);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String targetUrl = super.determineTargetUrl(request, response);
        if (targetUrl.startsWith(Strings.SLASH)) { // 以/开头为相对当前应用的路径，需转换为绝对路径
            String prefix = WebMvcUtil.getProtocolAndHost(request) + request.getContextPath();
            if (prefix.endsWith(Strings.SLASH)) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
            targetUrl = prefix + targetUrl;
        }
        String service = request.getParameter(CasServerConstants.PARAMETER_SERVICE);
        targetUrl = NetUtil.mergeParam(targetUrl, CasServerConstants.PARAMETER_SERVICE, service);
        return targetUrl;
    }

}
