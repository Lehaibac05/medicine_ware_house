package com.pharmacy.warehouse.config;

import com.pharmacy.warehouse.model.Role;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.RoleRepository;
import com.pharmacy.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        System.out.println(">>> DATA INITIALIZER RUNNING <<<");

        if (roleRepository.count() > 0) {
            System.out.println(">>> DATA ALREADY EXISTS, SKIP INIT <<<");
            return;
        }

        Role admin = new Role();
        admin.setRoleName("ADMIN");
        admin.setDescription("System administrator");

        Role manager = new Role();
        manager.setRoleName("WAREHOUSE_MANAGER");
        manager.setDescription("Warehouse manager");

        Role staff = new Role();
        staff.setRoleName("WAREHOUSE_STAFF");
        staff.setDescription("Warehouse staff");

        Role accountant = new Role();
        accountant.setRoleName("ACCOUNTANT");
        accountant.setDescription("Accountant");

        Role requester = new Role();
        requester.setRoleName("REQUESTER");
        requester.setDescription("Issue requester");

        admin = roleRepository.save(admin);
        roleRepository.save(manager);
        roleRepository.save(staff);
        roleRepository.save(accountant);
        roleRepository.save(requester);

        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPasswordHash(passwordEncoder.encode("password123"));
        adminUser.setFullName("Administrator");
        adminUser.setEmail("admin@example.com");
        adminUser.setStatus("ACTIVE");
        adminUser.setRole(admin);

        userRepository.save(adminUser);

        System.out.println(">>> INIT DATA SUCCESS <<<");
    }
}
