package com.example.courseapp.data.repository;

import com.example.courseapp.data.db.CourseDao;
import com.example.courseapp.data.db.SemesterDao;
import com.example.courseapp.data.db.TimeSlotTemplateDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class CourseRepository_Factory implements Factory<CourseRepository> {
  private final Provider<CourseDao> courseDaoProvider;

  private final Provider<SemesterDao> semesterDaoProvider;

  private final Provider<TimeSlotTemplateDao> timeSlotTemplateDaoProvider;

  public CourseRepository_Factory(Provider<CourseDao> courseDaoProvider,
      Provider<SemesterDao> semesterDaoProvider,
      Provider<TimeSlotTemplateDao> timeSlotTemplateDaoProvider) {
    this.courseDaoProvider = courseDaoProvider;
    this.semesterDaoProvider = semesterDaoProvider;
    this.timeSlotTemplateDaoProvider = timeSlotTemplateDaoProvider;
  }

  @Override
  public CourseRepository get() {
    return newInstance(courseDaoProvider.get(), semesterDaoProvider.get(), timeSlotTemplateDaoProvider.get());
  }

  public static CourseRepository_Factory create(Provider<CourseDao> courseDaoProvider,
      Provider<SemesterDao> semesterDaoProvider,
      Provider<TimeSlotTemplateDao> timeSlotTemplateDaoProvider) {
    return new CourseRepository_Factory(courseDaoProvider, semesterDaoProvider, timeSlotTemplateDaoProvider);
  }

  public static CourseRepository newInstance(CourseDao courseDao, SemesterDao semesterDao,
      TimeSlotTemplateDao timeSlotTemplateDao) {
    return new CourseRepository(courseDao, semesterDao, timeSlotTemplateDao);
  }
}
