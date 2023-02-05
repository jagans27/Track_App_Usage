package com.jagan.trackappusage

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.os.Process.myUid
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.jagan.trackappusage.LocationUtil.LocationService
import com.jagan.trackappusage.Util.SharedViewModel
import com.jagan.trackappusage.ui.theme.TrackAppUsageTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ), 0
        )

        setContent {
            TrackAppUsageTheme {
                Scaffold(scaffoldState = rememberScaffoldState(),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(if (isSystemInDarkTheme()) Color(0x12121212) else Color.Black),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "  App Usage Tracker.",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    },
                    bottomBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth() ,
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Transparent)
                                    .height(60.dp),
                                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color.Black),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
                                        onClick = {
                                            Intent(
                                                applicationContext,
                                                LocationService::class.java
                                            ).apply {
                                                action = LocationService.ACTION_START
                                                startService(this)
                                            }
                                            Toast.makeText(
                                                applicationContext,
                                                "Tracking started",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }) {
                                        Text(text = "Start", color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
                                        onClick = {
                                            Intent(
                                                applicationContext,
                                                LocationService::class.java
                                            ).apply {
                                                action = LocationService.ACTION_STOP
                                                startService(this)
                                            }
                                            Toast.makeText(
                                                applicationContext,
                                                "Tracking stopped",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }) {
                                        Text(text = "Stop", color = Color.White)
                                    }
                                }
                            }
                        }
                    }) {

                    Spacer(modifier = Modifier.height(30.dp))

                    val list = rememberSaveable {
                        LinkedHashMap<String, AppData>()
                    }
                    var appData = rememberSaveable {
                        ArrayList<String>()
                    }

                    if (checkUsageStatePermission()) {
                        Toast.makeText(
                            applicationContext, "Loading Please Wait....", Toast.LENGTH_SHORT
                        ).show()
                        showUsageStats(list)
                        Log.d("DEBUG",list.toString())
                        val sharedViewModel = SharedViewModel()
                        sharedViewModel.getAllLocations(applicationContext, Locations = {
                                Log.d("DEBUG",it.toString())

                                appData = sharedViewModel.interlaceData(location = it, appData = list)!!

                                Log.d("DEBUG1",appData.toString())

                        })

                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .background(Color.Black)
                        ) {

                            for (i in list) {
                                var area: String? =null
                                Log.d("DEBUG123","Going in $appData" )

                                for(j in appData) {

                                    Log.d("DEBUG123",j+" "+i+" "+j.indexOf(i.value.packageName))
                                    if(j.indexOf(i.value.packageName)!=-1){
                                        val sp = j.split(" ")
                                        area = sp[1]
                                        break;
                                    }
                                }

                                Card(
                                    elevation = 10.dp,
                                    modifier = Modifier
                                        .padding(
                                            start = 10.dp, end = 10.dp, top = 6.dp
                                        )
                                        .fillMaxWidth()
                                        .background(Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.background(Color.DarkGray)) {
                                        Text(
                                            text = i.value.packageName +(if(area.isNullOrBlank()) "" else " ( $area )"),
                                            modifier = Modifier.padding(10.dp),
                                            fontSize = 20.sp,
                                            color = Color.White
                                        )
                                        Row() {
                                            Text(
                                                text = i.value.lastTimeUsed,
                                                modifier = Modifier.padding(10.dp),
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = i.value.lastTimeStamp,
                                                modifier = Modifier.padding(10.dp),
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                        }
                                    }

                                }

                            }
                        }

                    } else {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }

                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun showUsageStats(appDataList: LinkedHashMap<String, AppData>) {

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val queryUsageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis()
        )

        val sortedUsageStats =
            queryUsageStats.sortedWith(compareByDescending<UsageStats> { it.lastTimeStamp }.thenBy { it.packageName })

        val packageManager = packageManager
        for (usageStats in sortedUsageStats) {
            try {
                val packageInfo = packageManager.getPackageInfo(usageStats.packageName, 0)
                val appName = packageInfo.applicationInfo.loadLabel(packageManager)
                //Log.d("DEBUG", appName.toString())
                if (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_INSTALLED != 0) {
                    if (!appDataList.containsKey(usageStats.packageName)) {
                        appDataList[usageStats.packageName] = AppData(
                            usageStats.packageName,
                            convertTime(usageStats.lastTimeUsed),
                            usageStats.describeContents().toString(),
                            convertTime(usageStats.firstTimeStamp),
                            convertTime(usageStats.lastTimeStamp),
                            convertTime2(usageStats.totalTimeInForeground)
                        )
                    }
                }
            } catch (e: Exception) {
                //Log.e("Debug", e.toString())
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun getAppNameFromPackage(packageName: String, context: Context): String? {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pkgAppsList = context.packageManager.queryIntentActivities(mainIntent, 0)
        for (app in pkgAppsList) {
            if (app.activityInfo.packageName == packageName) {
                return app.activityInfo.loadLabel(context.packageManager).toString()
            }
        }
        return null
    }

    private fun convertTime(lastTimeUsed: Long): String {

        val date = Date(lastTimeUsed)
        val format = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.ENGLISH)
        return format.format(date)
    }

    private fun convertTime2(lastTimeUsed: Long): String {

        val date = Date(lastTimeUsed)
        val format = SimpleDateFormat("hh:mm", Locale.ENGLISH)
        return format.format(date)
    }

    private fun checkUsageStatePermission(): Boolean {
        val appOpsManager: AppOpsManager?

        appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        val mode = appOpsManager.checkOpNoThrow(
            OPSTR_GET_USAGE_STATS, myUid(), packageName
        )

        return mode == MODE_ALLOWED

    }

}


data class AppData(
    val packageName: String,
    val lastTimeUsed: String,
    val describeContents: String,
    val firstTimeStamp: String,
    val lastTimeStamp: String,
    val totalTimeInForeground: String
)
