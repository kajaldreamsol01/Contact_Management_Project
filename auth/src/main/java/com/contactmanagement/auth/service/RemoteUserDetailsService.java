package com.contactmanagement.auth.service;

import com.contactmanagement.auth.client.UserClient;
import com.contactmanagement.auth.security.AuthUserPrincipal;
import com.contactmanagement.common.dto.UserAuthResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemoteUserDetailsService implements UserDetailsService {
    private final UserClient userClient;

    @Override
    public UserDetails loadUserByUsername(String email) {
        try {
            var r = userClient.byEmail(email.trim().toLowerCase());
            UserAuthResponseDto u = r == null ? null : r.getData();
            if (u == null || u.isStatus()) throw new UsernameNotFoundException("Invalid email or password");
            return new AuthUserPrincipal(u.getId(), u.getEmail(), u.getPassword(), u.getRole());
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalAuthenticationServiceException("User service is unavailable", e);
        }
    }
}
