package com.jagan.trackappusage.Util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.jagan.trackappusage.AppData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class SharedViewModel  {
    fun getLastLocation(
        context: Context,
        loc : LastLocation,
        userLastLocation: (String) -> Unit
    ) =
        CoroutineScope(Dispatchers.IO).launch {
            val firestoreRef = Firebase.firestore
                .collection("LastLocation")
                .document("LocationLast")

            try {
                firestoreRef.get().addOnSuccessListener {
                    if (it.exists()) {
                        val lastLocation = it.toObject<LastLocation>()!!
                        if(lastLocation.location!=loc.location) {
                            userLastLocation(lastLocation.location.toString())
                        }else if(lastLocation.location?.isEmpty() == true){
                            addLastLocation(context, lastLocation)
                        }
                    } else {
                        Log.d("DEBUG","error message")
                    }
                }.addOnCanceledListener {
                    Log.d("DEBUG","Some error")
                }
            } catch (e: Exception) {
                Log.d("DEBUG",e.message.toString())
            }
        }

    @SuppressLint("SimpleDateFormat")
    fun addLastLocation(
        context: Context,
        location: LastLocation
    ) = CoroutineScope(Dispatchers.IO).launch {
        val firestoreRef = Firebase.firestore
            .collection("LastLocation").document("LocationLast")
        try {
            firestoreRef.set(location).addOnSuccessListener {

                Toast.makeText(
                    context,
                    "Last Location Updated + $location" ,
                    Toast.LENGTH_SHORT
                ).show()


                // to update the list of times
                val sdf = SimpleDateFormat("dd/MM/yy hh:mm a")
                val currentDate = sdf.format(Date()).toString()
                checkTheLocation(context,location.location.toString(),currentDate)

            }

        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }
    }


    fun checkTheLocation(
        context: Context,
        loc : String,
        time : String
    ) =
        CoroutineScope(Dispatchers.IO).launch {
            val firestoreRef = Firebase.firestore
                .collection("Location")
                .document(loc)

            try {
                firestoreRef.get().addOnSuccessListener {
                    if(it.exists()){
                        val lastLocation = it.toObject<LocationTime>()!!

                        val str = LocationTime(lastLocation.locTime+","+time)
                        addLocationTime(context,str,loc)
                    }else{
                        val str = LocationTime(time)
                        addLocation(loc,str,context)
                    }
                }

            }catch (e:Exception){
                Log.d("DEBUG",e.toString())
            }
        }


    fun addLocation(
        loc : String,
        location : LocationTime,
        context: Context
    ) = CoroutineScope(Dispatchers.IO).launch {


        val firestoreRef = Firebase.firestore
            .collection("Location").document(loc)

        try {
            firestoreRef.set(location).addOnSuccessListener {
                    Toast.makeText(
                        context,
                        "Location Added Successfully",
                        Toast.LENGTH_SHORT
                    ).show()

            }

        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }
    }


    fun addLocationTime(
        context: Context,
        locationTime : LocationTime,
        document : String
    ) = CoroutineScope(Dispatchers.IO).launch {
        val firestoreRef = Firebase.firestore
            .collection("Location").document(document)
        try {
            firestoreRef.set(locationTime).addOnSuccessListener {

                Toast.makeText(
                    context,
                    "Location Added + $locationTime" ,
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    /* here to check the app */


    private val locationData = HashMap<String, String>()

    fun getAllLocations(
        context: Context,
        Locations: (HashMap<String, String>) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {

            val db = FirebaseFirestore.getInstance()
            val collectionReference = db.collection("Location")
            collectionReference.get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        locationData[document.id] = document.data.toString()
                    }
                    Locations(locationData)
                }
                .addOnFailureListener { exception ->
                    Log.w("DEBUG", "Error getting documents.", exception)
                }

        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun interlaceData(
        location: HashMap<String, String>,
        appData: LinkedHashMap<String, AppData>
    ): ArrayList<String>? {
        val result: ArrayList<String> = ArrayList()
        for (appDataKey in appData.keys) {
            for (locationKey in location.keys) {
                if (location[locationKey]!!.contains(appData[appDataKey]!!.lastTimeUsed)) {
                    result.add(appDataKey + " " + locationKey + " " + appData[appDataKey]!!.lastTimeUsed)
                }
            }
        }
        result.sort()
        return result
    }


}








data class LastLocation(

    var location: String? = null
)

data class LocationTime(
    var locTime: String? = null
)


