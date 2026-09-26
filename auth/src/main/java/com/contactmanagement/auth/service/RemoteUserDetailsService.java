package com.contactmanagement.auth.service;

import com.contactmanagement.auth.client.UserClient;
import com.contactmanagement.auth.dto.UserAuthResponseDto;
import com.contactmanagement.auth.response.ApiResponse;
import com.contactmanagement.auth.security.AuthUserPrincipal;
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
        ApiResponse<UserAuthResponseDto> response = fetchUser(email);
        UserAuthResponseDto user = Objects.nonNull(response) ? response.getData() : null;
        if (Objects.isNull(user) || user.isStatus()) throw new UsernameNotFoundException("Invalid email or password");
        return new AuthUserPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
    }

    private ApiResponse<UserAuthResponseDto> fetchUser(String email) {
        try {
            return userClient.byEmail(email.trim().toLowerCase());
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException("User service is unavailable", ex);
        }
    }
}


//eyJraWQiOiJNTDRETjFsYVV3M19OSUdvSHk5V0ZMNmFUM0lLSTZwZWJLY0s5a3o1STRVIiwiYWxnIjoiSFMyNTYifQ.eyJpc3MiOiJjb250YWN0LW1hbmFnZW1lbnQtYXV0aCIsInN1YiI6ImFkbWluQGRyZWFtc29sLmNvbSIsImV4cCI6MTc4OTU3MTk2MiwiaWF0IjoxNzg5NTY4MzYyLCJ1c2VySWQiOjEsInJvbGVzIjpbIkFETUlOIiwiRkFDVE9SX1BBU1NXT1JEIl19.GyzP_FlKE2fHgAM9aL0xDSceV9HAw-aCeVgV_PaJN98

//


