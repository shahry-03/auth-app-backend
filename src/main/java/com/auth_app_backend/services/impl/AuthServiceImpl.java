package com.auth_app_backend.services.impl;

import org.springframework.stereotype.Service;
import com.auth_app_backend.dtos.UserDto;
import com.auth_app_backend.services.AuthService;
import com.auth_app_backend.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
    private final UserService userService;

    @Override
    public UserDto registerUser(UserDto userDto) {
        // Here you can add additional logic for registration,
        //  such as checking if the user already exists, hashing the password, etc.  
        UserDto userdto = userService.createUser(userDto);
        return userdto;
    }


}
