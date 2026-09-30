package com.android.adobot.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

@Dao
public interface CallLogDao {

    @Insert
    void insert(CallLog callLog);

    @Query("SELECT * FROM calllog LIMIT 1")
    CallLog first();

    @Query("DELETE FROM calllog WHERE id = :id")
    void delete(int id);

}
