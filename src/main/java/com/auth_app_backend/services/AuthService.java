package com.auth_app_backend.services;

import com.auth_app_backend.dtos.UserDto;


public interface AuthService {

    // register user
    UserDto registerUser(UserDto userDto);
    
} 
