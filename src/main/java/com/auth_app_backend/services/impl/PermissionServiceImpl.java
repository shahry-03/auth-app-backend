package com.auth_app_backend.services.impl;

import com.auth_app_backend.dto.response.PermissionResponse;
import com.auth_app_backend.entity.Permission;
import com.auth_app_backend.exception.ResourceNotFoundException;
import com.auth_app_backend.mapper.PermissionMapper;
import com.auth_app_backend.repositories.PermissionRepository;
import com.auth_app_backend.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(UUID permissionId) {
        Permission p = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));
        return PermissionMapper.toResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll()
            .stream()
            .map(PermissionMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public PermissionResponse createPermission(String name, String description) {
        if (permissionRepository.existsByName(name)) {
            throw new IllegalArgumentException("Permission already exists: " + name);
        }
        Permission p = Permission.builder()
            .name(name)
            .description(description)
            .build();
        return PermissionMapper.toResponse(permissionRepository.save(p));
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(UUID permissionId, String description) {
        Permission p = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));
        if (description != null) p.setDescription(description);
        return PermissionMapper.toResponse(permissionRepository.save(p));
    }

    @Override
    @Transactional
    public void deletePermission(UUID permissionId) {
        if (!permissionRepository.existsById(permissionId)) {
            throw new ResourceNotFoundException("Permission not found: " + permissionId);
        }
        permissionRepository.deleteById(permissionId);
    }
}