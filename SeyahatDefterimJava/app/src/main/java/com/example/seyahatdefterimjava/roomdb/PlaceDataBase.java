package com.example.seyahatdefterimjava.roomdb;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.seyahatdefterimjava.model.Place;

@Database(entities = {Place.class},version = 1)
public abstract class PlaceDataBase extends RoomDatabase {
    public abstract  PlaceDao placeDao();
}
