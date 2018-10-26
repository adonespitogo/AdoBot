package com.android.adobot.database;

import android.arch.persistence.room.Dao;
//import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface SmsModelDao {

    @Query("SELECT * FROM smsmodel")
    List<SmsModel> getAll();

    @Query("SELECT * FROM smsmodel WHERE id IN (:userIds)")
    List<SmsModel> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM smsmodel WHERE id  = :id AND thread_id = :thread_id")
    SmsModel findByIdAndThreadId(int id, String thread_id);

    @Query("SELECT * FROM smsmodel LIMIT 1")
    SmsModel first();

    @Insert
    void insert(SmsModel smsModel);

    @Update
    void update(SmsModel smsModel);

    @Query("DELETE FROM smsmodel WHERE id = :id AND thread_id = :thread_id")
    void delete(int id, String thread_id);

}
