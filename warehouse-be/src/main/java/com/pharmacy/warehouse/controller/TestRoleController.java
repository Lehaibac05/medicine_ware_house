package com.pharmacy.warehouse.controller;

import org.springframework.web.bind.annotation.*;

@RestController
public class TestRoleController {

    @GetMapping("/admin/test")
    public String admin() {
        return "ADMIN OK";
    }

    @GetMapping("/warehouse/manager/test")
    public String manager() {
        return "WAREHOUSE MANAGER OK";
    }

    @GetMapping("/warehouse/staff/test")
    public String staff() {
        return "WAREHOUSE STAFF OK";
    }

    @GetMapping("/accountant/test")
    public String accountant() {
        return "ACCOUNTANT OK";
    }

    @GetMapping("/supplier/test")
    public String supplier() {
        return "SUPPLIER OK";
    }
}
