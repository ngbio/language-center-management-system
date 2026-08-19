/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ntt.language_center_management.entity;

import com.ntt.language_center_management.enums.AttendanceStatus;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author THUAN
 */
@Entity
@Table(name = "attendance")
@NamedQueries({
    @NamedQuery(name = "Attendance.findAll", query = "SELECT a FROM Attendance a")})
public class Attendance implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "Id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.PRESENT;
    @Size(max = 500)
    @Column(name = "Note")
    private String note;
    @Basic(optional = false)
    @NotNull
    @Column(name = "AttendanceTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date attendanceTime;
    @JoinColumn(name = "EnrollmentId", referencedColumnName = "Id")
    @ManyToOne(optional = false)
    private Enrollment enrollmentId;
    @JoinColumn(name = "LessonId", referencedColumnName = "Id")
    @ManyToOne(optional = false)
    private Lesson lessonId;

    public Attendance() {
    }

    public Attendance(Integer id) {
        this.id = id;
    }

    public Attendance(Integer id, AttendanceStatus status, Date attendanceTime) {
        this.id = id;
        this.status = status;
        this.attendanceTime = attendanceTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getAttendanceTime() {
        return attendanceTime;
    }

    public void setAttendanceTime(Date attendanceTime) {
        this.attendanceTime = attendanceTime;
    }

    public Enrollment getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Enrollment enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Lesson getLessonId() {
        return lessonId;
    }

    public void setLessonId(Lesson lessonId) {
        this.lessonId = lessonId;
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
        if (!(object instanceof Attendance)) {
            return false;
        }
        Attendance other = (Attendance) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.ntt.language_center_management.entity.Attendance[ id=" + id + " ]";
    }
    
}
