package org.truenewx.tnxjeex.cas.server.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.truenewx.tnxjee.model.spec.user.UserIdentity;
import org.truenewx.tnxjee.service.exception.BusinessException;
import org.truenewx.tnxjeex.cas.server.service.CasServerExceptionCodes;
import org.truenewx.tnxjeex.cas.server.service.CasServiceManager;

/**
 * CAS用户标识授权提供者
 */
@Component
public class CasUserIdentityAuthenticationProvider implements AuthenticationProvider {
    @Autowired
    private CasServiceManager serviceManager;
    @Autowired
    private CasLoginValidator loginValidator;

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
        String username = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();
        Object details = token.getDetails();
        if (details instanceof CasServiceAuthenticationDetails) {
            String service = ((CasServiceAuthenticationDetails) details).getService();
            try {
                String userType = this.serviceManager.resolveUserType(service);
                UserIdentity<?> userIdentity = this.loginValidator.validateLogin(userType, username, password);
                if (userIdentity == null) {
                    throw new BusinessException(CasServerExceptionCodes.UNSUPPORTED_USER_TYPE, userType);
                }
                return new CasUserIdentityAuthenticationToken(service, userIdentity);
            } catch (BusinessException e) {
                throw new BadCredentialsException(e.getLocalizedMessage(), e);
            }
        }
        return null;
    }

}