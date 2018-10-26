package com.android.adobot.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Sms.class, CallLog.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SmsDao smsDao();
    public abstract CallLogDao callLogDao();
}