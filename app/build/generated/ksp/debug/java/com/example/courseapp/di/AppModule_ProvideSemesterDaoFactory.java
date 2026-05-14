package com.example.courseapp.di;

import com.example.courseapp.data.db.AppDatabase;
import com.example.courseapp.data.db.SemesterDao;
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
public final class AppModule_ProvideSemesterDaoFactory implements Factory<SemesterDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideSemesterDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SemesterDao get() {
    return provideSemesterDao(dbProvider.get());
  }

  public static AppModule_ProvideSemesterDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideSemesterDaoFactory(dbProvider);
  }

  public static SemesterDao provideSemesterDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSemesterDao(db));
  }
}
