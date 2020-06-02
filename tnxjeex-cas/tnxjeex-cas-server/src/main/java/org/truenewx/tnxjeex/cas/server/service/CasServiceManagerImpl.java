package org.truenewx.tnxjeex.cas.server.service;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.truenewx.tnxjee.core.Strings;
import org.truenewx.tnxjee.service.exception.BusinessException;
import org.truenewx.tnxjeex.cas.server.config.CasServerProperties;
import org.truenewx.tnxjeex.cas.server.ticket.TicketManager;

/**
 * CAS服务管理器实现
 *
 * @author jianglei
 */
@Component
public class CasServiceManagerImpl implements CasServiceManager {

    @Autowired
    private CasServerProperties serverProperties;
    @Autowired
    private TicketManager ticketManager;

    @Override
    public String getUserType(String service) {
        return getProperties(service).getUserType();
    }

    @Override
    public String getHost(String service) {
        return getProperties(service).getHost();
    }

    private CasService getProperties(String service) {
        CasService casService = this.serverProperties.getServices().get(service);
        if (casService == null) {
            throw new BusinessException(CasServerExceptionCodes.INVALID_SERVICE);
        }
        return casService;
    }

    @Override
    public String getLoginUrl(HttpServletRequest request, String service) {
        String loginUrl = getProperties(service).getFullLoginUrl();
        int index = loginUrl.indexOf(Strings.QUESTION);
        if (index < 0) {
            loginUrl += Strings.QUESTION;
        } else {
            loginUrl += Strings.AND;
        }
        loginUrl += "ticket=" + this.ticketManager.getServiceTicket(request, service);
        return loginUrl;
    }

    @Override
    public Map<String, String> getLogoutUrls(String service) {
        Map<String, String> logoutUrls = new HashMap<>();
        Map<String, CasService> serviceMap = this.serverProperties.getServices();
        CasService currentServiceObj = serviceMap.get(service);
        if (currentServiceObj != null) {
            String userType = currentServiceObj.getUserType();
            serviceMap.forEach((serviceName, serviceObj) -> {
                if (StringUtils.isBlank(userType) || StringUtils.isBlank(serviceObj.getUserType())
                        || userType.equals(serviceObj.getUserType())) {
                    logoutUrls.put(serviceName, serviceObj.getFullLogoutUrl());
                }
            });
        }
        return logoutUrls;
    }

}
