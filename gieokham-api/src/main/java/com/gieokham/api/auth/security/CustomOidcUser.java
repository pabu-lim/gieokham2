package com.gieokham.api.auth.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;

@Getter
public class CustomOidcUser extends DefaultOidcUser {

    private final Long memberId;

    public CustomOidcUser(OidcUser oidcUser, Collection<? extends GrantedAuthority> authorities, Long memberId) {
        super(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "sub");

        this.memberId = memberId;
    }
}
