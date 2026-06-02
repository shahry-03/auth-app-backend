package com.auth_app_backend.services.impl;

import com.auth_app_backend.dtos.UserDto;
import com.auth_app_backend.entity.Provider;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.helper.UserHelper;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        // Check if email is null or empty
        if (userDto.getEmail() == null || userDto.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        // Check if email already exists
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Convert UserDto to User entity and save it to the database
        User user = modelMapper.map(userDto, User.class);
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);
        //ToDo: Assign role to the new user for authorization
        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDto.class);
        

    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        // convert String userId to UUID userId
        UUID uId = UserHelper.parseId(userId);
        // find existing user by id and update it
        User existingUser = userRepository
                .findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        // for this project, we will not allow updating the user's email'
        if(userDto.getName() != null) existingUser.setName(userDto.getName());
        if(userDto.getImage() != null) existingUser.setImage(userDto.getImage());
        if (userDto.getProvider() != null) existingUser.setProvider(userDto.getProvider());
        //ToDo: change password update logic
        if (userDto.getPassword() != null) existingUser.setPassword(userDto.getPassword());
        existingUser.setEnable(userDto.isEnable());
        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);


    }

    @Override
    public UserDto getUserById(String userId) {
        // convert String userId to UUID userId
        UUID uId = UserHelper.parseId(userId);
        User user = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public Iterable<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }

    @Override
    public void deleteUser(String userId) {
        // convert String userId to UUID userId
        UUID uId = UserHelper.parseId(userId);
        // find user by id and delete it
        userRepository.findById(uId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        userRepository.deleteById(uId);

    }
}
