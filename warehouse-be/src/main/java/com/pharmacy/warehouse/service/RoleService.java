package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.Role;
import com.pharmacy.warehouse.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    // Lấy tất cả role
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    // Lấy theo ID
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    // Tạo role
    public Role createRole(Role role) {
        return roleRepository.save(role);
    }

    // Update role
    public Role updateRole(Long id, Role newRole) {
        Role role = getRoleById(id);
        role.setRoleName(newRole.getRoleName());
        role.setDescription(newRole.getDescription());
        return roleRepository.save(role);
    }

    // Xoá role
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }
}