package com.example.locationtracker.db

import android.provider.BaseColumns

// Location Database Contract
object LocationDatabaseContract {

    // Table contents are grouped together in an anonymous object.
    object LocationEntry : BaseColumns {
        const val TABLE_NAME        = "location"
        const val COLUMN_DEVICE_ID  = "device_id"
        const val COLUMN_LATITUDE   = "latitude"
        const val COLUMN_LONGITUDE  = "longitude"
    }
}