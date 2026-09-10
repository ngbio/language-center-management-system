package com.ntt.language_center_management.unit.fixture;

import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Lesson;
import com.ntt.language_center_management.entity.Payment;

public final class TestFixtures {
  private TestFixtures() {}

  public static Course course(int id) { return new Course(id); }
  public static Courseclass courseClass(int id) { return new Courseclass(id); }
  public static Enrollment enrollment(int id) { return new Enrollment(id); }
  public static Payment payment(int id) { return new Payment(id); }
  public static Lesson lesson(int id) { return new Lesson(id); }
}
