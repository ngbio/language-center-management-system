package com.ntt.language_center_management.dto.request;

import com.ntt.language_center_management.enums.CatalogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LanguageRequest {

    private Integer id;
    @NotBlank
    @Size(max = 20)
    private String languageCode;
    @NotBlank
    @Size(max = 100)
    private String languageName;
    @Size(max = 500)
    private String description;
    @NotNull
    private CatalogStatus status = CatalogStatus.ACTIVE;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CatalogStatus getStatus() {
        return status;
    }

    public void setStatus(CatalogStatus status) {
        this.status = status;
    }
}
