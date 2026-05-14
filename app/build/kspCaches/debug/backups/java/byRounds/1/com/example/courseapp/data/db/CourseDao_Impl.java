package com.example.courseapp.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.courseapp.data.model.Course;
import com.example.courseapp.data.model.CourseType;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CourseDao_Impl implements CourseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Course> __insertionAdapterOfCourse;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<Course> __deletionAdapterOfCourse;

  private final EntityDeletionOrUpdateAdapter<Course> __updateAdapterOfCourse;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllBySemester;

  public CourseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCourse = new EntityInsertionAdapter<Course>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `courses` (`id`,`name`,`teacher`,`location`,`dayOfWeek`,`startSlot`,`slotCount`,`type`,`weekRange`,`colorIndex`,`semester`,`credits`,`notes`,`examDate`,`customColor`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Course entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getTeacher());
        statement.bindString(4, entity.getLocation());
        statement.bindLong(5, entity.getDayOfWeek());
        statement.bindLong(6, entity.getStartSlot());
        statement.bindLong(7, entity.getSlotCount());
        final String _tmp = __converters.fromCourseType(entity.getType());
        statement.bindString(8, _tmp);
        statement.bindString(9, entity.getWeekRange());
        statement.bindLong(10, entity.getColorIndex());
        statement.bindString(11, entity.getSemester());
        statement.bindDouble(12, entity.getCredits());
        statement.bindString(13, entity.getNotes());
        statement.bindString(14, entity.getExamDate());
        statement.bindString(15, entity.getCustomColor());
      }
    };
    this.__deletionAdapterOfCourse = new EntityDeletionOrUpdateAdapter<Course>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `courses` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Course entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfCourse = new EntityDeletionOrUpdateAdapter<Course>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `courses` SET `id` = ?,`name` = ?,`teacher` = ?,`location` = ?,`dayOfWeek` = ?,`startSlot` = ?,`slotCount` = ?,`type` = ?,`weekRange` = ?,`colorIndex` = ?,`semester` = ?,`credits` = ?,`notes` = ?,`examDate` = ?,`customColor` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Course entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getTeacher());
        statement.bindString(4, entity.getLocation());
        statement.bindLong(5, entity.getDayOfWeek());
        statement.bindLong(6, entity.getStartSlot());
        statement.bindLong(7, entity.getSlotCount());
        final String _tmp = __converters.fromCourseType(entity.getType());
        statement.bindString(8, _tmp);
        statement.bindString(9, entity.getWeekRange());
        statement.bindLong(10, entity.getColorIndex());
        statement.bindString(11, entity.getSemester());
        statement.bindDouble(12, entity.getCredits());
        statement.bindString(13, entity.getNotes());
        statement.bindString(14, entity.getExamDate());
        statement.bindString(15, entity.getCustomColor());
        statement.bindLong(16, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAllBySemester = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM courses WHERE semester = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertCourse(final Course course, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCourse.insertAndReturnId(course);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCourses(final List<Course> courses,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCourse.insert(courses);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCourse(final Course course, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCourse.handle(course);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateCourse(final Course course, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCourse.handle(course);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllBySemester(final String semester,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllBySemester.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, semester);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllBySemester.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Course>> getCoursesBySemester(final String semester) {
    final String _sql = "SELECT * FROM courses WHERE semester = ? ORDER BY dayOfWeek, startSlot";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, semester);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"courses"}, new Callable<List<Course>>() {
      @Override
      @NonNull
      public List<Course> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTeacher = CursorUtil.getColumnIndexOrThrow(_cursor, "teacher");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfStartSlot = CursorUtil.getColumnIndexOrThrow(_cursor, "startSlot");
          final int _cursorIndexOfSlotCount = CursorUtil.getColumnIndexOrThrow(_cursor, "slotCount");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfWeekRange = CursorUtil.getColumnIndexOrThrow(_cursor, "weekRange");
          final int _cursorIndexOfColorIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorIndex");
          final int _cursorIndexOfSemester = CursorUtil.getColumnIndexOrThrow(_cursor, "semester");
          final int _cursorIndexOfCredits = CursorUtil.getColumnIndexOrThrow(_cursor, "credits");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfExamDate = CursorUtil.getColumnIndexOrThrow(_cursor, "examDate");
          final int _cursorIndexOfCustomColor = CursorUtil.getColumnIndexOrThrow(_cursor, "customColor");
          final List<Course> _result = new ArrayList<Course>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Course _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTeacher;
            _tmpTeacher = _cursor.getString(_cursorIndexOfTeacher);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpStartSlot;
            _tmpStartSlot = _cursor.getInt(_cursorIndexOfStartSlot);
            final int _tmpSlotCount;
            _tmpSlotCount = _cursor.getInt(_cursorIndexOfSlotCount);
            final CourseType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toCourseType(_tmp);
            final String _tmpWeekRange;
            _tmpWeekRange = _cursor.getString(_cursorIndexOfWeekRange);
            final int _tmpColorIndex;
            _tmpColorIndex = _cursor.getInt(_cursorIndexOfColorIndex);
            final String _tmpSemester;
            _tmpSemester = _cursor.getString(_cursorIndexOfSemester);
            final float _tmpCredits;
            _tmpCredits = _cursor.getFloat(_cursorIndexOfCredits);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpExamDate;
            _tmpExamDate = _cursor.getString(_cursorIndexOfExamDate);
            final String _tmpCustomColor;
            _tmpCustomColor = _cursor.getString(_cursorIndexOfCustomColor);
            _item = new Course(_tmpId,_tmpName,_tmpTeacher,_tmpLocation,_tmpDayOfWeek,_tmpStartSlot,_tmpSlotCount,_tmpType,_tmpWeekRange,_tmpColorIndex,_tmpSemester,_tmpCredits,_tmpNotes,_tmpExamDate,_tmpCustomColor);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Course>> getCoursesByDay(final String semester, final int day) {
    final String _sql = "SELECT * FROM courses WHERE semester = ? AND dayOfWeek = ? ORDER BY startSlot";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, semester);
    _argIndex = 2;
    _statement.bindLong(_argIndex, day);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"courses"}, new Callable<List<Course>>() {
      @Override
      @NonNull
      public List<Course> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTeacher = CursorUtil.getColumnIndexOrThrow(_cursor, "teacher");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfStartSlot = CursorUtil.getColumnIndexOrThrow(_cursor, "startSlot");
          final int _cursorIndexOfSlotCount = CursorUtil.getColumnIndexOrThrow(_cursor, "slotCount");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfWeekRange = CursorUtil.getColumnIndexOrThrow(_cursor, "weekRange");
          final int _cursorIndexOfColorIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorIndex");
          final int _cursorIndexOfSemester = CursorUtil.getColumnIndexOrThrow(_cursor, "semester");
          final int _cursorIndexOfCredits = CursorUtil.getColumnIndexOrThrow(_cursor, "credits");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfExamDate = CursorUtil.getColumnIndexOrThrow(_cursor, "examDate");
          final int _cursorIndexOfCustomColor = CursorUtil.getColumnIndexOrThrow(_cursor, "customColor");
          final List<Course> _result = new ArrayList<Course>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Course _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTeacher;
            _tmpTeacher = _cursor.getString(_cursorIndexOfTeacher);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpStartSlot;
            _tmpStartSlot = _cursor.getInt(_cursorIndexOfStartSlot);
            final int _tmpSlotCount;
            _tmpSlotCount = _cursor.getInt(_cursorIndexOfSlotCount);
            final CourseType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toCourseType(_tmp);
            final String _tmpWeekRange;
            _tmpWeekRange = _cursor.getString(_cursorIndexOfWeekRange);
            final int _tmpColorIndex;
            _tmpColorIndex = _cursor.getInt(_cursorIndexOfColorIndex);
            final String _tmpSemester;
            _tmpSemester = _cursor.getString(_cursorIndexOfSemester);
            final float _tmpCredits;
            _tmpCredits = _cursor.getFloat(_cursorIndexOfCredits);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpExamDate;
            _tmpExamDate = _cursor.getString(_cursorIndexOfExamDate);
            final String _tmpCustomColor;
            _tmpCustomColor = _cursor.getString(_cursorIndexOfCustomColor);
            _item = new Course(_tmpId,_tmpName,_tmpTeacher,_tmpLocation,_tmpDayOfWeek,_tmpStartSlot,_tmpSlotCount,_tmpType,_tmpWeekRange,_tmpColorIndex,_tmpSemester,_tmpCredits,_tmpNotes,_tmpExamDate,_tmpCustomColor);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Course>> getAllCourses() {
    final String _sql = "SELECT * FROM courses ORDER BY dayOfWeek, startSlot";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"courses"}, new Callable<List<Course>>() {
      @Override
      @NonNull
      public List<Course> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTeacher = CursorUtil.getColumnIndexOrThrow(_cursor, "teacher");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfStartSlot = CursorUtil.getColumnIndexOrThrow(_cursor, "startSlot");
          final int _cursorIndexOfSlotCount = CursorUtil.getColumnIndexOrThrow(_cursor, "slotCount");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfWeekRange = CursorUtil.getColumnIndexOrThrow(_cursor, "weekRange");
          final int _cursorIndexOfColorIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorIndex");
          final int _cursorIndexOfSemester = CursorUtil.getColumnIndexOrThrow(_cursor, "semester");
          final int _cursorIndexOfCredits = CursorUtil.getColumnIndexOrThrow(_cursor, "credits");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfExamDate = CursorUtil.getColumnIndexOrThrow(_cursor, "examDate");
          final int _cursorIndexOfCustomColor = CursorUtil.getColumnIndexOrThrow(_cursor, "customColor");
          final List<Course> _result = new ArrayList<Course>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Course _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTeacher;
            _tmpTeacher = _cursor.getString(_cursorIndexOfTeacher);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpStartSlot;
            _tmpStartSlot = _cursor.getInt(_cursorIndexOfStartSlot);
            final int _tmpSlotCount;
            _tmpSlotCount = _cursor.getInt(_cursorIndexOfSlotCount);
            final CourseType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toCourseType(_tmp);
            final String _tmpWeekRange;
            _tmpWeekRange = _cursor.getString(_cursorIndexOfWeekRange);
            final int _tmpColorIndex;
            _tmpColorIndex = _cursor.getInt(_cursorIndexOfColorIndex);
            final String _tmpSemester;
            _tmpSemester = _cursor.getString(_cursorIndexOfSemester);
            final float _tmpCredits;
            _tmpCredits = _cursor.getFloat(_cursorIndexOfCredits);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpExamDate;
            _tmpExamDate = _cursor.getString(_cursorIndexOfExamDate);
            final String _tmpCustomColor;
            _tmpCustomColor = _cursor.getString(_cursorIndexOfCustomColor);
            _item = new Course(_tmpId,_tmpName,_tmpTeacher,_tmpLocation,_tmpDayOfWeek,_tmpStartSlot,_tmpSlotCount,_tmpType,_tmpWeekRange,_tmpColorIndex,_tmpSemester,_tmpCredits,_tmpNotes,_tmpExamDate,_tmpCustomColor);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getCourseById(final long id, final Continuation<? super Course> $completion) {
    final String _sql = "SELECT * FROM courses WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Course>() {
      @Override
      @Nullable
      public Course call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTeacher = CursorUtil.getColumnIndexOrThrow(_cursor, "teacher");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfStartSlot = CursorUtil.getColumnIndexOrThrow(_cursor, "startSlot");
          final int _cursorIndexOfSlotCount = CursorUtil.getColumnIndexOrThrow(_cursor, "slotCount");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfWeekRange = CursorUtil.getColumnIndexOrThrow(_cursor, "weekRange");
          final int _cursorIndexOfColorIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorIndex");
          final int _cursorIndexOfSemester = CursorUtil.getColumnIndexOrThrow(_cursor, "semester");
          final int _cursorIndexOfCredits = CursorUtil.getColumnIndexOrThrow(_cursor, "credits");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfExamDate = CursorUtil.getColumnIndexOrThrow(_cursor, "examDate");
          final int _cursorIndexOfCustomColor = CursorUtil.getColumnIndexOrThrow(_cursor, "customColor");
          final Course _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTeacher;
            _tmpTeacher = _cursor.getString(_cursorIndexOfTeacher);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpStartSlot;
            _tmpStartSlot = _cursor.getInt(_cursorIndexOfStartSlot);
            final int _tmpSlotCount;
            _tmpSlotCount = _cursor.getInt(_cursorIndexOfSlotCount);
            final CourseType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toCourseType(_tmp);
            final String _tmpWeekRange;
            _tmpWeekRange = _cursor.getString(_cursorIndexOfWeekRange);
            final int _tmpColorIndex;
            _tmpColorIndex = _cursor.getInt(_cursorIndexOfColorIndex);
            final String _tmpSemester;
            _tmpSemester = _cursor.getString(_cursorIndexOfSemester);
            final float _tmpCredits;
            _tmpCredits = _cursor.getFloat(_cursorIndexOfCredits);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpExamDate;
            _tmpExamDate = _cursor.getString(_cursorIndexOfExamDate);
            final String _tmpCustomColor;
            _tmpCustomColor = _cursor.getString(_cursorIndexOfCustomColor);
            _result = new Course(_tmpId,_tmpName,_tmpTeacher,_tmpLocation,_tmpDayOfWeek,_tmpStartSlot,_tmpSlotCount,_tmpType,_tmpWeekRange,_tmpColorIndex,_tmpSemester,_tmpCredits,_tmpNotes,_tmpExamDate,_tmpCustomColor);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getCourseCount(final String semester,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM courses WHERE semester = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, semester);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getConflictingCourses(final String semester, final int day, final int startSlot,
      final int endSlot, final Continuation<? super List<Course>> $completion) {
    final String _sql = "SELECT * FROM courses WHERE semester = ? AND dayOfWeek = ? AND startSlot < ? AND (startSlot + slotCount) > ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 4);
    int _argIndex = 1;
    _statement.bindString(_argIndex, semester);
    _argIndex = 2;
    _statement.bindLong(_argIndex, day);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endSlot);
    _argIndex = 4;
    _statement.bindLong(_argIndex, startSlot);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Course>>() {
      @Override
      @NonNull
      public List<Course> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTeacher = CursorUtil.getColumnIndexOrThrow(_cursor, "teacher");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfStartSlot = CursorUtil.getColumnIndexOrThrow(_cursor, "startSlot");
          final int _cursorIndexOfSlotCount = CursorUtil.getColumnIndexOrThrow(_cursor, "slotCount");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfWeekRange = CursorUtil.getColumnIndexOrThrow(_cursor, "weekRange");
          final int _cursorIndexOfColorIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorIndex");
          final int _cursorIndexOfSemester = CursorUtil.getColumnIndexOrThrow(_cursor, "semester");
          final int _cursorIndexOfCredits = CursorUtil.getColumnIndexOrThrow(_cursor, "credits");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfExamDate = CursorUtil.getColumnIndexOrThrow(_cursor, "examDate");
          final int _cursorIndexOfCustomColor = CursorUtil.getColumnIndexOrThrow(_cursor, "customColor");
          final List<Course> _result = new ArrayList<Course>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Course _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTeacher;
            _tmpTeacher = _cursor.getString(_cursorIndexOfTeacher);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpStartSlot;
            _tmpStartSlot = _cursor.getInt(_cursorIndexOfStartSlot);
            final int _tmpSlotCount;
            _tmpSlotCount = _cursor.getInt(_cursorIndexOfSlotCount);
            final CourseType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toCourseType(_tmp);
            final String _tmpWeekRange;
            _tmpWeekRange = _cursor.getString(_cursorIndexOfWeekRange);
            final int _tmpColorIndex;
            _tmpColorIndex = _cursor.getInt(_cursorIndexOfColorIndex);
            final String _tmpSemester;
            _tmpSemester = _cursor.getString(_cursorIndexOfSemester);
            final float _tmpCredits;
            _tmpCredits = _cursor.getFloat(_cursorIndexOfCredits);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpExamDate;
            _tmpExamDate = _cursor.getString(_cursorIndexOfExamDate);
            final String _tmpCustomColor;
            _tmpCustomColor = _cursor.getString(_cursorIndexOfCustomColor);
            _item = new Course(_tmpId,_tmpName,_tmpTeacher,_tmpLocation,_tmpDayOfWeek,_tmpStartSlot,_tmpSlotCount,_tmpType,_tmpWeekRange,_tmpColorIndex,_tmpSemester,_tmpCredits,_tmpNotes,_tmpExamDate,_tmpCustomColor);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCoursesByIds(final List<Long> ids,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM courses WHERE id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : ids) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
