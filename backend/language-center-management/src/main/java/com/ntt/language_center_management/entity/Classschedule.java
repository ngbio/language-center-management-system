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
@Table(name = "classschedule")
@NamedQueries({
    @NamedQuery(name = "Classschedule.findAll", query = "SELECT c FROM Classschedule c")})
public class Classschedule implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "Id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "DayOfWeek")
    private short dayOfWeek;
    @Basic(optional = false)
    @NotNull
    @Column(name = "StartTime")
    @Temporal(TemporalType.TIME)
    private Date startTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EndTime")
    @Temporal(TemporalType.TIME)
    private Date endTime;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "DeliveryMode")
    private String deliveryMode;
    @Size(max = 500)
    @Column(name = "MeetingUrl")
    private String meetingUrl;
    @JoinColumn(name = "CourseClassId", referencedColumnName = "Id")
    @ManyToOne(optional = false)
    private Courseclass courseClassId;
    @JoinColumn(name = "RoomId", referencedColumnName = "Id")
    @ManyToOne
    private Room roomId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "classScheduleId")
    private List<Lesson> lessonList;

    public Classschedule() {
    }

    public Classschedule(Integer id) {
        this.id = id;
    }

    public Classschedule(Integer id, short dayOfWeek, Date startTime, Date endTime, String deliveryMode) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.deliveryMode = deliveryMode;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public short getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(short dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public void setMeetingUrl(String meetingUrl) {
        this.meetingUrl = meetingUrl;
    }

    public Courseclass getCourseClassId() {
        return courseClassId;
    }

    public void setCourseClassId(Courseclass courseClassId) {
        this.courseClassId = courseClassId;
    }

    public Room getRoomId() {
        return roomId;
    }

    public void setRoomId(Room roomId) {
        this.roomId = roomId;
    }

    public List<Lesson> getLessonList() {
        return lessonList;
    }

    public void setLessonList(List<Lesson> lessonList) {
        this.lessonList = lessonList;
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
        if (!(object instanceof Classschedule)) {
            return false;
        }
        Classschedule other = (Classschedule) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.ntt.language_center_management.entity.Classschedule[ id=" + id + " ]";
    }
    
}
