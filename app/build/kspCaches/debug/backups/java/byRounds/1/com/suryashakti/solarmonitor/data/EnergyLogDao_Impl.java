package com.suryashakti.solarmonitor.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class EnergyLogDao_Impl implements EnergyLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EnergyLog> __insertionAdapterOfEnergyLog;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<EnergyLog> __deletionAdapterOfEnergyLog;

  private final EntityDeletionOrUpdateAdapter<EnergyLog> __updateAdapterOfEnergyLog;

  public EnergyLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEnergyLog = new EntityInsertionAdapter<EnergyLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `energy_logs` (`id`,`dateMillis`,`generatedKwh`,`consumedKwh`,`weatherCondition`,`perUnitRate`,`exportRate`,`panelCapacityKw`,`notes`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EnergyLog entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getDateMillis());
        statement.bindDouble(3, entity.getGeneratedKwh());
        statement.bindDouble(4, entity.getConsumedKwh());
        final String _tmp = __converters.toWeather(entity.getWeatherCondition());
        statement.bindString(5, _tmp);
        statement.bindDouble(6, entity.getPerUnitRate());
        statement.bindDouble(7, entity.getExportRate());
        statement.bindDouble(8, entity.getPanelCapacityKw());
        statement.bindString(9, entity.getNotes());
      }
    };
    this.__deletionAdapterOfEnergyLog = new EntityDeletionOrUpdateAdapter<EnergyLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `energy_logs` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EnergyLog entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfEnergyLog = new EntityDeletionOrUpdateAdapter<EnergyLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `energy_logs` SET `id` = ?,`dateMillis` = ?,`generatedKwh` = ?,`consumedKwh` = ?,`weatherCondition` = ?,`perUnitRate` = ?,`exportRate` = ?,`panelCapacityKw` = ?,`notes` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EnergyLog entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getDateMillis());
        statement.bindDouble(3, entity.getGeneratedKwh());
        statement.bindDouble(4, entity.getConsumedKwh());
        final String _tmp = __converters.toWeather(entity.getWeatherCondition());
        statement.bindString(5, _tmp);
        statement.bindDouble(6, entity.getPerUnitRate());
        statement.bindDouble(7, entity.getExportRate());
        statement.bindDouble(8, entity.getPanelCapacityKw());
        statement.bindString(9, entity.getNotes());
        statement.bindLong(10, entity.getId());
      }
    };
  }

  @Override
  public Object insertLog(final EnergyLog log, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfEnergyLog.insertAndReturnId(log);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteLog(final EnergyLog log, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfEnergyLog.handle(log);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLog(final EnergyLog log, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfEnergyLog.handle(log);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public LiveData<List<EnergyLog>> getAllLogs() {
    final String _sql = "SELECT * FROM energy_logs ORDER BY dateMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"energy_logs"}, false, new Callable<List<EnergyLog>>() {
      @Override
      @Nullable
      public List<EnergyLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "dateMillis");
          final int _cursorIndexOfGeneratedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "generatedKwh");
          final int _cursorIndexOfConsumedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "consumedKwh");
          final int _cursorIndexOfWeatherCondition = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCondition");
          final int _cursorIndexOfPerUnitRate = CursorUtil.getColumnIndexOrThrow(_cursor, "perUnitRate");
          final int _cursorIndexOfExportRate = CursorUtil.getColumnIndexOrThrow(_cursor, "exportRate");
          final int _cursorIndexOfPanelCapacityKw = CursorUtil.getColumnIndexOrThrow(_cursor, "panelCapacityKw");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<EnergyLog> _result = new ArrayList<EnergyLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EnergyLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDateMillis;
            _tmpDateMillis = _cursor.getLong(_cursorIndexOfDateMillis);
            final double _tmpGeneratedKwh;
            _tmpGeneratedKwh = _cursor.getDouble(_cursorIndexOfGeneratedKwh);
            final double _tmpConsumedKwh;
            _tmpConsumedKwh = _cursor.getDouble(_cursorIndexOfConsumedKwh);
            final WeatherCondition _tmpWeatherCondition;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeatherCondition);
            _tmpWeatherCondition = __converters.fromWeather(_tmp);
            final double _tmpPerUnitRate;
            _tmpPerUnitRate = _cursor.getDouble(_cursorIndexOfPerUnitRate);
            final double _tmpExportRate;
            _tmpExportRate = _cursor.getDouble(_cursorIndexOfExportRate);
            final double _tmpPanelCapacityKw;
            _tmpPanelCapacityKw = _cursor.getDouble(_cursorIndexOfPanelCapacityKw);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _item = new EnergyLog(_tmpId,_tmpDateMillis,_tmpGeneratedKwh,_tmpConsumedKwh,_tmpWeatherCondition,_tmpPerUnitRate,_tmpExportRate,_tmpPanelCapacityKw,_tmpNotes);
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
  public LiveData<List<EnergyLog>> getLast30Logs() {
    final String _sql = "SELECT * FROM energy_logs ORDER BY dateMillis DESC LIMIT 30";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"energy_logs"}, false, new Callable<List<EnergyLog>>() {
      @Override
      @Nullable
      public List<EnergyLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "dateMillis");
          final int _cursorIndexOfGeneratedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "generatedKwh");
          final int _cursorIndexOfConsumedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "consumedKwh");
          final int _cursorIndexOfWeatherCondition = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCondition");
          final int _cursorIndexOfPerUnitRate = CursorUtil.getColumnIndexOrThrow(_cursor, "perUnitRate");
          final int _cursorIndexOfExportRate = CursorUtil.getColumnIndexOrThrow(_cursor, "exportRate");
          final int _cursorIndexOfPanelCapacityKw = CursorUtil.getColumnIndexOrThrow(_cursor, "panelCapacityKw");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<EnergyLog> _result = new ArrayList<EnergyLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EnergyLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDateMillis;
            _tmpDateMillis = _cursor.getLong(_cursorIndexOfDateMillis);
            final double _tmpGeneratedKwh;
            _tmpGeneratedKwh = _cursor.getDouble(_cursorIndexOfGeneratedKwh);
            final double _tmpConsumedKwh;
            _tmpConsumedKwh = _cursor.getDouble(_cursorIndexOfConsumedKwh);
            final WeatherCondition _tmpWeatherCondition;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeatherCondition);
            _tmpWeatherCondition = __converters.fromWeather(_tmp);
            final double _tmpPerUnitRate;
            _tmpPerUnitRate = _cursor.getDouble(_cursorIndexOfPerUnitRate);
            final double _tmpExportRate;
            _tmpExportRate = _cursor.getDouble(_cursorIndexOfExportRate);
            final double _tmpPanelCapacityKw;
            _tmpPanelCapacityKw = _cursor.getDouble(_cursorIndexOfPanelCapacityKw);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _item = new EnergyLog(_tmpId,_tmpDateMillis,_tmpGeneratedKwh,_tmpConsumedKwh,_tmpWeatherCondition,_tmpPerUnitRate,_tmpExportRate,_tmpPanelCapacityKw,_tmpNotes);
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
  public Object getLogByDate(final long dateMillis,
      final Continuation<? super EnergyLog> $completion) {
    final String _sql = "SELECT * FROM energy_logs WHERE dateMillis = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dateMillis);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<EnergyLog>() {
      @Override
      @Nullable
      public EnergyLog call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "dateMillis");
          final int _cursorIndexOfGeneratedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "generatedKwh");
          final int _cursorIndexOfConsumedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "consumedKwh");
          final int _cursorIndexOfWeatherCondition = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCondition");
          final int _cursorIndexOfPerUnitRate = CursorUtil.getColumnIndexOrThrow(_cursor, "perUnitRate");
          final int _cursorIndexOfExportRate = CursorUtil.getColumnIndexOrThrow(_cursor, "exportRate");
          final int _cursorIndexOfPanelCapacityKw = CursorUtil.getColumnIndexOrThrow(_cursor, "panelCapacityKw");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final EnergyLog _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDateMillis;
            _tmpDateMillis = _cursor.getLong(_cursorIndexOfDateMillis);
            final double _tmpGeneratedKwh;
            _tmpGeneratedKwh = _cursor.getDouble(_cursorIndexOfGeneratedKwh);
            final double _tmpConsumedKwh;
            _tmpConsumedKwh = _cursor.getDouble(_cursorIndexOfConsumedKwh);
            final WeatherCondition _tmpWeatherCondition;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeatherCondition);
            _tmpWeatherCondition = __converters.fromWeather(_tmp);
            final double _tmpPerUnitRate;
            _tmpPerUnitRate = _cursor.getDouble(_cursorIndexOfPerUnitRate);
            final double _tmpExportRate;
            _tmpExportRate = _cursor.getDouble(_cursorIndexOfExportRate);
            final double _tmpPanelCapacityKw;
            _tmpPanelCapacityKw = _cursor.getDouble(_cursorIndexOfPanelCapacityKw);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _result = new EnergyLog(_tmpId,_tmpDateMillis,_tmpGeneratedKwh,_tmpConsumedKwh,_tmpWeatherCondition,_tmpPerUnitRate,_tmpExportRate,_tmpPanelCapacityKw,_tmpNotes);
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
  public LiveData<List<EnergyLog>> getLogsFrom(final long fromMillis) {
    final String _sql = "SELECT * FROM energy_logs WHERE dateMillis >= ? ORDER BY dateMillis ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, fromMillis);
    return __db.getInvalidationTracker().createLiveData(new String[] {"energy_logs"}, false, new Callable<List<EnergyLog>>() {
      @Override
      @Nullable
      public List<EnergyLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "dateMillis");
          final int _cursorIndexOfGeneratedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "generatedKwh");
          final int _cursorIndexOfConsumedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "consumedKwh");
          final int _cursorIndexOfWeatherCondition = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCondition");
          final int _cursorIndexOfPerUnitRate = CursorUtil.getColumnIndexOrThrow(_cursor, "perUnitRate");
          final int _cursorIndexOfExportRate = CursorUtil.getColumnIndexOrThrow(_cursor, "exportRate");
          final int _cursorIndexOfPanelCapacityKw = CursorUtil.getColumnIndexOrThrow(_cursor, "panelCapacityKw");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<EnergyLog> _result = new ArrayList<EnergyLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EnergyLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDateMillis;
            _tmpDateMillis = _cursor.getLong(_cursorIndexOfDateMillis);
            final double _tmpGeneratedKwh;
            _tmpGeneratedKwh = _cursor.getDouble(_cursorIndexOfGeneratedKwh);
            final double _tmpConsumedKwh;
            _tmpConsumedKwh = _cursor.getDouble(_cursorIndexOfConsumedKwh);
            final WeatherCondition _tmpWeatherCondition;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeatherCondition);
            _tmpWeatherCondition = __converters.fromWeather(_tmp);
            final double _tmpPerUnitRate;
            _tmpPerUnitRate = _cursor.getDouble(_cursorIndexOfPerUnitRate);
            final double _tmpExportRate;
            _tmpExportRate = _cursor.getDouble(_cursorIndexOfExportRate);
            final double _tmpPanelCapacityKw;
            _tmpPanelCapacityKw = _cursor.getDouble(_cursorIndexOfPanelCapacityKw);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _item = new EnergyLog(_tmpId,_tmpDateMillis,_tmpGeneratedKwh,_tmpConsumedKwh,_tmpWeatherCondition,_tmpPerUnitRate,_tmpExportRate,_tmpPanelCapacityKw,_tmpNotes);
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
  public LiveData<EnergyLog> getLatestLog() {
    final String _sql = "SELECT * FROM energy_logs ORDER BY dateMillis DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"energy_logs"}, false, new Callable<EnergyLog>() {
      @Override
      @Nullable
      public EnergyLog call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "dateMillis");
          final int _cursorIndexOfGeneratedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "generatedKwh");
          final int _cursorIndexOfConsumedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "consumedKwh");
          final int _cursorIndexOfWeatherCondition = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCondition");
          final int _cursorIndexOfPerUnitRate = CursorUtil.getColumnIndexOrThrow(_cursor, "perUnitRate");
          final int _cursorIndexOfExportRate = CursorUtil.getColumnIndexOrThrow(_cursor, "exportRate");
          final int _cursorIndexOfPanelCapacityKw = CursorUtil.getColumnIndexOrThrow(_cursor, "panelCapacityKw");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final EnergyLog _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDateMillis;
            _tmpDateMillis = _cursor.getLong(_cursorIndexOfDateMillis);
            final double _tmpGeneratedKwh;
            _tmpGeneratedKwh = _cursor.getDouble(_cursorIndexOfGeneratedKwh);
            final double _tmpConsumedKwh;
            _tmpConsumedKwh = _cursor.getDouble(_cursorIndexOfConsumedKwh);
            final WeatherCondition _tmpWeatherCondition;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeatherCondition);
            _tmpWeatherCondition = __converters.fromWeather(_tmp);
            final double _tmpPerUnitRate;
            _tmpPerUnitRate = _cursor.getDouble(_cursorIndexOfPerUnitRate);
            final double _tmpExportRate;
            _tmpExportRate = _cursor.getDouble(_cursorIndexOfExportRate);
            final double _tmpPanelCapacityKw;
            _tmpPanelCapacityKw = _cursor.getDouble(_cursorIndexOfPanelCapacityKw);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _result = new EnergyLog(_tmpId,_tmpDateMillis,_tmpGeneratedKwh,_tmpConsumedKwh,_tmpWeatherCondition,_tmpPerUnitRate,_tmpExportRate,_tmpPanelCapacityKw,_tmpNotes);
          } else {
            _result = null;
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
  public Object getLatestLogSync(final Continuation<? super EnergyLog> $completion) {
    final String _sql = "SELECT * FROM energy_logs ORDER BY dateMillis DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<EnergyLog>() {
      @Override
      @Nullable
      public EnergyLog call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "dateMillis");
          final int _cursorIndexOfGeneratedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "generatedKwh");
          final int _cursorIndexOfConsumedKwh = CursorUtil.getColumnIndexOrThrow(_cursor, "consumedKwh");
          final int _cursorIndexOfWeatherCondition = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCondition");
          final int _cursorIndexOfPerUnitRate = CursorUtil.getColumnIndexOrThrow(_cursor, "perUnitRate");
          final int _cursorIndexOfExportRate = CursorUtil.getColumnIndexOrThrow(_cursor, "exportRate");
          final int _cursorIndexOfPanelCapacityKw = CursorUtil.getColumnIndexOrThrow(_cursor, "panelCapacityKw");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final EnergyLog _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDateMillis;
            _tmpDateMillis = _cursor.getLong(_cursorIndexOfDateMillis);
            final double _tmpGeneratedKwh;
            _tmpGeneratedKwh = _cursor.getDouble(_cursorIndexOfGeneratedKwh);
            final double _tmpConsumedKwh;
            _tmpConsumedKwh = _cursor.getDouble(_cursorIndexOfConsumedKwh);
            final WeatherCondition _tmpWeatherCondition;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeatherCondition);
            _tmpWeatherCondition = __converters.fromWeather(_tmp);
            final double _tmpPerUnitRate;
            _tmpPerUnitRate = _cursor.getDouble(_cursorIndexOfPerUnitRate);
            final double _tmpExportRate;
            _tmpExportRate = _cursor.getDouble(_cursorIndexOfExportRate);
            final double _tmpPanelCapacityKw;
            _tmpPanelCapacityKw = _cursor.getDouble(_cursorIndexOfPanelCapacityKw);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _result = new EnergyLog(_tmpId,_tmpDateMillis,_tmpGeneratedKwh,_tmpConsumedKwh,_tmpWeatherCondition,_tmpPerUnitRate,_tmpExportRate,_tmpPanelCapacityKw,_tmpNotes);
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
  public Object getSummaryFrom(final long fromMillis,
      final Continuation<? super EnergySummary> $completion) {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            SUM(generatedKwh) as totalGenerated,\n"
            + "            SUM(consumedKwh) as totalConsumed\n"
            + "        FROM energy_logs \n"
            + "        WHERE dateMillis >= ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, fromMillis);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<EnergySummary>() {
      @Override
      @Nullable
      public EnergySummary call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTotalGenerated = 0;
          final int _cursorIndexOfTotalConsumed = 1;
          final EnergySummary _result;
          if (_cursor.moveToFirst()) {
            final double _tmpTotalGenerated;
            _tmpTotalGenerated = _cursor.getDouble(_cursorIndexOfTotalGenerated);
            final double _tmpTotalConsumed;
            _tmpTotalConsumed = _cursor.getDouble(_cursorIndexOfTotalConsumed);
            _result = new EnergySummary(_tmpTotalGenerated,_tmpTotalConsumed);
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
  public Object getLogCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM energy_logs";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
