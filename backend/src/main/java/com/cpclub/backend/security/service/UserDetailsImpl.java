package com.cpclub.backend.security.service;

import com.cpclub.backend.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security adapter for a CP Club {@link User}.
 *
 * <p>This separates the persisted JPA entity from the security framework and marks the
 * password as ignored for JSON serialization, preventing hash leakage in diagnostics.</p>
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String email;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Creates security user details and a single authority from a persisted user role.
     *
     * @param user persisted member whose credentials are being authenticated
     * @return immutable security principal used by Spring Security
     */
    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().name())
        );

        return new UserDetailsImpl(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /** {@inheritDoc} */
    @Override
    public String getPassword() {
        return password;
    }

    /** {@inheritDoc} */
    @Override
    public String getUsername() {
        return email;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
