package com.celehub.backend.user.dto;

/** GET /api/users/me/profile — thông tin tài khoản + số liệu theo role (role không liên quan thì null). */
public record ProfileResponse(UserDto user, StudentStats student, TeacherStats teacher) {

    public record StudentStats(long activeCourses, int attended, int held, Double attendanceRate) {}

    public record TeacherStats(long activeCourses, long totalStudents, long lessonsThisMonth) {}
}
