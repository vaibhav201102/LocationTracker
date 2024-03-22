package com.example.locationtracker.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.locationtracker.db.LocationDatabaseHelper
import com.example.locationtracker.model.LocationEntryModel
import com.example.locationtracker.service.LocationBackgroundService
import com.example.locationtracker.databinding.ActivityMainBinding
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileOutputStream

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    //region Variables

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: LocationDatabaseHelper

    private val stopServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.locationtracker.STOP_SERVICE") {
                stopLocationService()
            }
        }
    }

    //endregion Variables

    //region Override Methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        databaseHelper = LocationDatabaseHelper(this)

        // ============== INIT METHODS ================
        init()
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        unregisterReceiver(stopServiceReceiver)
//    }

    //endregion Override

    //region Private Methods

    private fun init(){
        binding.trackingEnabledText.visibility = GONE
        checkpermissionRequired()

        startLocationButtonClick()
        stopLocationButtonClick()
        downloadExcelButtonClick()
    }

    //Start location button click
    private fun startLocationButtonClick(){
        binding.btnStart.setOnClickListener {
            startLocationService()
            binding.trackingEnabledText.visibility = VISIBLE
            binding.buttonClickText.text = "Press stop button to disable location track service"
            binding.trackingEnabledText.text = "Location tracking is enabled"
            binding.btnStart.isEnabled = false
            binding.btnStop.isEnabled = true
        }
    }


    //Stop location button click
    private fun stopLocationButtonClick(){
        binding.btnStop.setOnClickListener {
            sendStopServiceBroadcast()
            stopLocationService()
            binding.buttonClickText.text = "Press start button to enable location track service"
            binding.trackingEnabledText.text = "Location tracking is disabled"
            binding.btnStart.isEnabled = true
            binding.btnStop.isEnabled = false

            // Inside your LocationForegroundService class, wherever you want to check the inserted entries
            val entries = databaseHelper.getLocationEntries()
            for (entry in entries) {
                Log.d("DatabaseEntry", "Device ID: ${entry.deviceId}, Latitude: ${entry.latitude}, Longitude: ${entry.longitude}")
            }

        }
    }

    //Download excel button click
    private fun downloadExcelButtonClick(){
        binding.btnDownloadExcel.setOnClickListener {
            val entries = databaseHelper.getLocationEntries()
            val file = File(getExternalFilesDir(null), "location_data.xlsx")
            val filePath = file.absolutePath
            createExcelFile(databaseHelper.getLocationEntries(), filePath)
            downloadExcelFile(entries)

        }
    }

    //start location service
    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationBackgroundService::class.java)
        startService(serviceIntent)
    }

    //stop location service
    private fun sendStopServiceBroadcast() {
        val intent = Intent("com.example.locationtracker.STOP_SERVICE")
        sendBroadcast(intent)
    }

    //stop location service
    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationBackgroundService::class.java)
        stopService(serviceIntent)
    }

    private fun checkpermissionRequired() {
        this.let {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (hasPermission(it as Context, permission_s)) {
                    //startFragment()
                } else {
                    permReqLauncher.launch(permission_s)
                }
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                if (hasPermission(it as Context, permission_r)) {
                    //startFragment()
                } else {
                    permReqLauncher.launch(permission_r)
                }
            }
        }
    }

    val permReqLauncher =  registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all {it.value} // Checks the permission is granted or not

        if (granted) {
            // navigate to respective screen
            //startFragment()
        } else {
            // show custom alert
            //Previously Permission Request was cancelled with 'Dont Ask Again',
            // Redirect to Settings after showing Information about why you need the permission
        }
    }

    private fun hasPermission(context: Context, permissions: Array<String>): Boolean =
        permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    //endregion Private Methods

    //companion object
    companion object {

        @SuppressLint("InlinedApi")
        var permission_s   = arrayOf(

            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,

            )

        @SuppressLint("InlinedApi")
        var permission_r                    =               arrayOf(

            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,

            )
    }
    //create excel file
    private fun createExcelFile(entries: List<LocationEntryModel>, filePath: String) {
        val workbook = WorkbookFactory.create(true)
        val sheet = workbook.createSheet("Location Data")

        // Create headers
        val headers = arrayOf("Device ID", "Latitude", "Longitude")
        val headerRow = sheet.createRow(0)
        for ((index, header) in headers.withIndex()) {
            headerRow.createCell(index).setCellValue(header)
        }

        // Fill data
        for ((rowIndex, entry) in entries.withIndex()) {
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(entry.deviceId)
            row.createCell(1).setCellValue(entry.latitude)
            row.createCell(2).setCellValue(entry.longitude)
        }

        // Write workbook to file
        val fileOut = FileOutputStream(filePath)
        workbook.write(fileOut)
        fileOut.close()

        // Close workbook
        workbook.close()
    }

    //download excel file
    private fun downloadExcelFile(entries: List<LocationEntryModel>) {
        val fileName = "location_data.xlsx"
        val file = File(getExternalFilesDir(null), fileName) // Using getExternalFilesDir() to get a directory where the app can write files
        val filePath = file.absolutePath

        createExcelFile(entries, filePath)

        val fileUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(fileUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        startActivity(intent)
    }

}
