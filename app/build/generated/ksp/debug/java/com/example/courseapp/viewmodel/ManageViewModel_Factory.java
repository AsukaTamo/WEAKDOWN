package com.example.courseapp.viewmodel;

import com.example.courseapp.data.repository.CourseRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ManageViewModel_Factory implements Factory<ManageViewModel> {
  private final Provider<CourseRepository> repositoryProvider;

  public ManageViewModel_Factory(Provider<CourseRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ManageViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static ManageViewModel_Factory create(Provider<CourseRepository> repositoryProvider) {
    return new ManageViewModel_Factory(repositoryProvider);
  }

  public static ManageViewModel newInstance(CourseRepository repository) {
    return new ManageViewModel(repository);
  }
}
