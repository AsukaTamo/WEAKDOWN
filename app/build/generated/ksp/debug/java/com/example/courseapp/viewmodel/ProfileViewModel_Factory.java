package com.example.courseapp.viewmodel;

import android.content.Context;
import com.example.courseapp.data.repository.CourseRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<CourseRepository> repositoryProvider;

  private final Provider<Context> contextProvider;

  public ProfileViewModel_Factory(Provider<CourseRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(repositoryProvider.get(), contextProvider.get());
  }

  public static ProfileViewModel_Factory create(Provider<CourseRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    return new ProfileViewModel_Factory(repositoryProvider, contextProvider);
  }

  public static ProfileViewModel newInstance(CourseRepository repository, Context context) {
    return new ProfileViewModel(repository, context);
  }
}
