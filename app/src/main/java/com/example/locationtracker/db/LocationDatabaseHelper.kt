package com.example.locationtracker.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.example.locationtracker.model.LocationEntryModel

class LocationDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    //region Override Methods for SQLiteOpenHelper

    override fun onCreate(db: SQLiteDatabase) {
        val SQL_CREATE_ENTRIES = """
            CREATE TABLE ${LocationDatabaseContract.LocationEntry.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY,
                ${LocationDatabaseContract.LocationEntry.COLUMN_DEVICE_ID} TEXT,
                ${LocationDatabaseContract.LocationEntry.COLUMN_LATITUDE} REAL,
                ${LocationDatabaseContract.LocationEntry.COLUMN_LONGITUDE} REAL
            )
        """.trimIndent()

        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Upgrade logic goes here
    }

    //endregion Override Methods for SQLiteOpenHelper

    // Insert a location entry into the database
    fun insertLocation(deviceId: String, latitude: Double, longitude: Double) {
        val values = ContentValues().apply {
            put(LocationDatabaseContract.LocationEntry.COLUMN_DEVICE_ID, deviceId)
            put(LocationDatabaseContract.LocationEntry.COLUMN_LATITUDE, latitude)
            put(LocationDatabaseContract.LocationEntry.COLUMN_LONGITUDE, longitude)
        }

        writableDatabase.insert(LocationDatabaseContract.LocationEntry.TABLE_NAME, null, values)
    }

    // Inside your LocationDatabaseHelper class

    // Get all location entries from the database
    fun getLocationEntries(): List<LocationEntryModel> {
        val locationList = mutableListOf<LocationEntryModel>()

        val db = readableDatabase

        val projection = arrayOf(
            LocationDatabaseContract.LocationEntry.COLUMN_DEVICE_ID,
            LocationDatabaseContract.LocationEntry.COLUMN_LATITUDE,
            LocationDatabaseContract.LocationEntry.COLUMN_LONGITUDE
        )

        val cursor = db.query(
            LocationDatabaseContract.LocationEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            null
        )

        with(cursor) {
            while (moveToNext()) {
                val deviceId =
                    getString(getColumnIndexOrThrow(LocationDatabaseContract.LocationEntry.COLUMN_DEVICE_ID))
                val latitude =
                    getDouble(getColumnIndexOrThrow(LocationDatabaseContract.LocationEntry.COLUMN_LATITUDE))
                val longitude =
                    getDouble(getColumnIndexOrThrow(LocationDatabaseContract.LocationEntry.COLUMN_LONGITUDE))
                locationList.add(LocationEntryModel(deviceId, latitude, longitude))
            }
        }

        cursor.close()
        return locationList
    }

    // Companion object for database constants
    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "LocationTracker.db"
    }
}
