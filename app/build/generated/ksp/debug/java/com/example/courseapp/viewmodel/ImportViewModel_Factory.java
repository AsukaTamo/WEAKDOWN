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
public final class ImportViewModel_Factory implements Factory<ImportViewModel> {
  private final Provider<CourseRepository> repositoryProvider;

  public ImportViewModel_Factory(Provider<CourseRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ImportViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static ImportViewModel_Factory create(Provider<CourseRepository> repositoryProvider) {
    return new ImportViewModel_Factory(repositoryProvider);
  }

  public static ImportViewModel newInstance(CourseRepository repository) {
    return new ImportViewModel(repository);
  }
}
