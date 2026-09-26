package com.contactmanagement.auth.service;

import com.contactmanagement.auth.client.UserClient;
import com.contactmanagement.auth.security.AuthUserPrincipal;
import com.contactmanagement.common.dto.UserAuthResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RemoteUserDetailsService implements UserDetailsService {
    private final UserClient userClient;

    @Override
    public UserDetails loadUserByUsername(String email) {
        try {
            var userResponse = userClient.byEmail(email.trim().toLowerCase());
            UserAuthResponseDto user = Objects.isNull(userResponse) ? null : userResponse.getData();
            if (Objects.isNull(user) || user.isStatus())
                throw new UsernameNotFoundException("Invalid email or password");
            return new AuthUserPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
        } catch (UsernameNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InternalAuthenticationServiceException("User service is unavailable", exception);
        }
    }
}