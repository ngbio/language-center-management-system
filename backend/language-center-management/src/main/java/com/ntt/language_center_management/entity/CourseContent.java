/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ntt.language_center_management.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 *
 * @author THUAN
 */
@Entity
@Table(name = "course_content")
@NamedQueries({
    @NamedQuery(name = "CourseContent.findAll", query = "SELECT c FROM CourseContent c")})
public class CourseContent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "title")
    private String title;
    @Lob
    @Size(max = 65535)
    @Column(name = "summary")
    private String summary;
    @Lob
    @Size(max = 2147483647)
    @Column(name = "content_html")
    private String contentHtml;
    @Size(max = 500)
    @Column(name = "audio_url")
    private String audioUrl;
    @Size(max = 500)
    @Column(name = "video_url")
    private String videoUrl;
    @Size(max = 500)
    @Column(name = "document_url")
    private String documentUrl;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "content_type")
    private String contentType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "display_order")
    private int displayOrder;
    @Basic(optional = false)
    @NotNull
    @Column(name = "is_preview")
    private boolean isPreview;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "publication_status")
    private String publicationStatus;
    @JoinColumn(name = "section_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private CourseSection sectionId;

    public CourseContent() {
    }

    public CourseContent(Integer id) {
        this.id = id;
    }

    public CourseContent(Integer id, String title, String contentType, int displayOrder, boolean isPreview, String publicationStatus) {
        this.id = id;
        this.title = title;
        this.contentType = contentType;
        this.displayOrder = displayOrder;
        this.isPreview = isPreview;
        this.publicationStatus = publicationStatus;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean getIsPreview() {
        return isPreview;
    }

    public void setIsPreview(boolean isPreview) {
        this.isPreview = isPreview;
    }

    public String getPublicationStatus() {
        return publicationStatus;
    }

    public void setPublicationStatus(String publicationStatus) {
        this.publicationStatus = publicationStatus;
    }

    public CourseSection getSectionId() {
        return sectionId;
    }

    public void setSectionId(CourseSection sectionId) {
        this.sectionId = sectionId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CourseContent)) {
            return false;
        }
        CourseContent other = (CourseContent) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.ntt.language_center_management.entity.CourseContent[ id=" + id + " ]";
    }
    
}
