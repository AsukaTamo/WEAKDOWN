package com.example.courseapp.di;

import com.example.courseapp.data.db.AppDatabase;
import com.example.courseapp.data.db.TimeSlotTemplateDao;
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
public final class AppModule_ProvideTimeSlotTemplateDaoFactory implements Factory<TimeSlotTemplateDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideTimeSlotTemplateDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public TimeSlotTemplateDao get() {
    return provideTimeSlotTemplateDao(dbProvider.get());
  }

  public static AppModule_ProvideTimeSlotTemplateDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideTimeSlotTemplateDaoFactory(dbProvider);
  }

  public static TimeSlotTemplateDao provideTimeSlotTemplateDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTimeSlotTemplateDao(db));
  }
}
