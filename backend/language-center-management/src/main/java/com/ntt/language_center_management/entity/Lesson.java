/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ntt.language_center_management.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 * @author THUAN
 */
@Entity
@Table(name = "lesson")
@NamedQueries({
    @NamedQuery(name = "Lesson.findAll", query = "SELECT l FROM Lesson l")})
public class Lesson implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Size(max = 255)
    @Column(name = "topic")
    private String topic;
    @Basic(optional = false)
    @NotNull
    @Column(name = "lesson_date")
    @Temporal(TemporalType.DATE)
    private Date lessonDate;
    @Size(max = 500)
    @Column(name = "meeting_url")
    private String meetingUrl;
    @Column(name = "original_lesson_date")
    @Temporal(TemporalType.DATE)
    private Date originalLessonDate;
    @Size(max = 500)
    @Column(name = "reschedule_reason")
    private String rescheduleReason;
    @Column(name = "rescheduled_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date rescheduledAt;
    @JoinColumn(name = "rescheduled_by", referencedColumnName = "id")
    @ManyToOne
    private User rescheduledBy;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "status")
    private String status;
    @JoinColumn(name = "class_schedule_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Classschedule classScheduleId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "lessonId")
    private List<Attendance> attendanceList;

    public Lesson() {
    }

    public Lesson(Integer id) {
        this.id = id;
    }

    public Lesson(Integer id, Date lessonDate, String status) {
        this.id = id;
        this.lessonDate = lessonDate;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Date getLessonDate() {
        return lessonDate;
    }

    public void setLessonDate(Date lessonDate) {
        this.lessonDate = lessonDate;
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public void setMeetingUrl(String meetingUrl) {
        this.meetingUrl = meetingUrl;
    }

    public Date getOriginalLessonDate() {
        return originalLessonDate;
    }

    public void setOriginalLessonDate(Date originalLessonDate) {
        this.originalLessonDate = originalLessonDate;
    }

    public String getRescheduleReason() {
        return rescheduleReason;
    }

    public void setRescheduleReason(String rescheduleReason) {
        this.rescheduleReason = rescheduleReason;
    }

    public Date getRescheduledAt() {
        return rescheduledAt;
    }

    public void setRescheduledAt(Date rescheduledAt) {
        this.rescheduledAt = rescheduledAt;
    }

    public User getRescheduledBy() {
        return rescheduledBy;
    }

    public void setRescheduledBy(User rescheduledBy) {
        this.rescheduledBy = rescheduledBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Classschedule getClassScheduleId() {
        return classScheduleId;
    }

    public void setClassScheduleId(Classschedule classScheduleId) {
        this.classScheduleId = classScheduleId;
    }

    public List<Attendance> getAttendanceList() {
        return attendanceList;
    }

    public void setAttendanceList(List<Attendance> attendanceList) {
        this.attendanceList = attendanceList;
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
        if (!(object instanceof Lesson)) {
            return false;
        }
        Lesson other = (Lesson) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.ntt.language_center_management.entity.Lesson[ id=" + id + " ]";
    }
    
}
