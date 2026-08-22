package com.ntt.language_center_management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.service.LanguageService;

@Controller
@RequestMapping("/admin/languages")
public class LanguageController {

	@Autowired
	private LanguageService languageService;

	@GetMapping
	public String listLanguage(Model model) {
		model.addAttribute("languages", this.languageService.getLanguages());
		model.addAttribute("activePage", "languages");
		return "languages";
	}

	@PostMapping("/delete")
	public String deleteLanguage(@RequestParam("languageId") int id, RedirectAttributes redirectAttributes) {
		try {
			boolean isDeleted = this.languageService.deleteLanguage(id);
			if (isDeleted) {
				redirectAttributes.addFlashAttribute("success", "Đã xóa ngôn ngữ thành công!");
			} else {
				redirectAttributes.addFlashAttribute("error", "Không thể xóa! Ngôn ngữ này đang có cấp độ liên kết.");
			}
		} catch (Exception exception) {
			redirectAttributes.addFlashAttribute("error", "Xóa thất bại! Không tìm thấy ngôn ngữ.");
		}

		return "redirect:/admin/languages";
	}

	@GetMapping("/update/{id}")
	public String editView(Model model, @PathVariable("id") int id) {
		model.addAttribute("language", this.languageService.getLanguageById(id));
		return "language-form";
	}

	@PostMapping("/save")
	public String save(@ModelAttribute("language") LanguageRequest languageRequest) {
		this.languageService.addOrUpdateLanguage(languageRequest);
		return "redirect:/admin/languages";
	}

	@GetMapping("/add")
	public String addLanguage(Model model) {
		model.addAttribute("language", new LanguageRequest());
		return "language-form";
	}
}
