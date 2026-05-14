package com.example.courseapp.di;

import com.example.courseapp.data.db.AppDatabase;
import com.example.courseapp.data.db.CourseDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideCourseDaoFactory implements Factory<CourseDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideCourseDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CourseDao get() {
    return provideCourseDao(dbProvider.get());
  }

  public static AppModule_ProvideCourseDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideCourseDaoFactory(dbProvider);
  }

  public static CourseDao provideCourseDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCourseDao(db));
  }
}
