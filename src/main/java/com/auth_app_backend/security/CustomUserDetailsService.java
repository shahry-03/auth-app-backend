package com.auth_app_backend.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.auth_app_backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Implement your logic to load user details from the database or any other
        // source
        // For example, you can use a UserRepository to fetch user details based on the
        // username
        // and return a UserDetails object containing the user's information and
        // authorities.
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
    }

}
