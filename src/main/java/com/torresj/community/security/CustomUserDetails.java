package com.torresj.community.security;

import com.torresj.community.entities.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.torresj.community.enums.UserRole.ROLE_SUPERADMIN;

public class CustomUserDetails implements UserDetails {

    private final UserEntity userEntity;
    private final boolean hasAdminMembership;

    public CustomUserDetails(UserEntity userEntity, boolean hasAdminMembership) {
        this.userEntity = userEntity;
        this.hasAdminMembership = hasAdminMembership;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // SUPERADMIN is system-wide. Otherwise the coarse authority is derived from
        // per-community memberships: ROLE_ADMIN if the user administers any community,
        // plus ROLE_USER. Fine-grained per-community scoping is enforced in the service layer.
        if (userEntity.getRole() == ROLE_SUPERADMIN) {
            return List.of(new SimpleGrantedAuthority(ROLE_SUPERADMIN.name()));
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (hasAdminMembership) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return userEntity.getPassword();
    }

    @Override
    public String getUsername() {
        return userEntity.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
