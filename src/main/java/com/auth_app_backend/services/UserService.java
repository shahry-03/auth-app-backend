package com.auth_app_backend.services;

import com.auth_app_backend.dtos.UserDto;

public interface UserService {
    // Create User
    public UserDto createUser(UserDto userDto);

    // Update User
    public UserDto updateUser(UserDto userDto, String userId);

    // Get User by Id
    public UserDto getUserById(String userId);

    // Get User by Email
    public UserDto getUserByEmail(String email);

    // Get All Users
    public Iterable<UserDto> getAllUsers();

    // Delete User
    public void deleteUser(String userId);
}
