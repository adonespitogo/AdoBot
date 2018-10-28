package com.android.adobot.database;

import android.arch.persistence.room.Dao;
//import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface SmsDao {

//    @Query("SELECT * FROM Sms")
//    List<Sms> getAll();
//
//    @Query("SELECT * FROM Sms WHERE id IN (:userIds)")
//    List<Sms> loadAllByIds(int[] userIds);
//
    @Query("SELECT * FROM Sms WHERE id  = :id AND thread_id = :thread_id")
    Sms findByIdAndThreadId(int id, String thread_id);

    @Query("SELECT * FROM Sms WHERE phone  = :phone AND body = :body AND date = :date AND type = :type")
    Sms findByAttributes(String phone, String body, String date, int type);

    @Query("SELECT * FROM Sms LIMIT 1")
    Sms first();

    @Insert
    void insert(Sms sms);

//    @Update
//    void update(Sms sms);

    @Delete()
    void delete(Sms sms);

}
