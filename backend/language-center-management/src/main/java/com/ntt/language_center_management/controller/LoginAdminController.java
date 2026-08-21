package com.ntt.language_center_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginAdminController {

	@GetMapping("/login")
    public String loginRedirect() {
        return "redirect:/admin/login";
    }

	@GetMapping("/admin/login")
	public String loginView() {
		return "login";
	}
}
