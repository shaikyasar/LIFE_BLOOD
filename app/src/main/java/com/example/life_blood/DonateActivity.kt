package com.example.life_blood

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.*

class DonateActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 100
    private lateinit var btnFetchLocation: ImageView
    private lateinit var tvCity: TextView
    private lateinit var dbHelper: DatabaseHelper



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        btnFetchLocation = findViewById(R.id.locationIcon)
        tvCity = findViewById(R.id.click_location)





        dbHelper = DatabaseHelper(this)

        btnFetchLocation.setOnClickListener {
            Toast.makeText(this, "Fetching location...", Toast.LENGTH_SHORT).show()
            getLastLocation()
        }


        val btnDonate = findViewById<Button>(R.id.btnSignUp)
        btnDonate.setOnClickListener {

            if(saveDonorData())
            {
                finish()
            }
        }

    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    try {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val city = address.locality ?: "City not found"
                            tvCity.text = city

                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Error fetching address: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to get location: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            askPermission()
        }
    }

    private fun askPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                Toast.makeText(this, "Please provide the required permission", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun saveDonorData(): Boolean {
        val name = findViewById<EditText>(R.id.fullName).text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        val password = findViewById<EditText>(R.id.password).text.toString()
        if (password.isEmpty()) {
            Toast.makeText(this, "password cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }


        val ageStr = findViewById<EditText>(R.id.age).text.toString()
        val age = ageStr.toIntOrNull()
        if (age == null || age < 20 || age > 70) {
            Toast.makeText(this, "Age must be between 20 and 70", Toast.LENGTH_SHORT).show()
            return false
        }

        val genderSpinner = findViewById<Spinner>(R.id.spinnerGender)
        val gender = genderSpinner.selectedItem.toString()
        if (gender.isEmpty()) {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show()
            return false
        }

        val phone = findViewById<EditText>(R.id.Number).text.toString()
        if (phone.length != 10 || !phone.all { it.isDigit() }) {
            Toast.makeText(this, "Phone number must be 10 digits", Toast.LENGTH_SHORT).show()
            return false
        }

        val email = findViewById<EditText>(R.id.gmail).text.toString()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return false
        }


        val city = tvCity.text.toString()
        val bloodGroup = getSelectedBloodGroup()
        if (bloodGroup.isEmpty()) {
            Toast.makeText(this, "Please select a blood group", Toast.LENGTH_SHORT).show()
            return false
        }

        if (dbHelper.isDonorDuplicate(phone, email)) {
            Toast.makeText(this, "Phone number or email already exists in the database", Toast.LENGTH_SHORT).show()
            return false
        }

        val donor = Donor(name,password, age.toString(), gender, phone, email, city, bloodGroup)
        val result = dbHelper.addDonor(donor)
        return if (result != -1L) {
            dbHelper.setUserLoggedIn(name)
            Toast.makeText(this, "Donor information saved successfully!", Toast.LENGTH_SHORT).show()
            true
        } else {
            Toast.makeText(this, "Failed to save donor information", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun getSelectedBloodGroup(): String {
        return when {
            findViewById<RadioButton>(R.id.radio_a_positive).isChecked -> "A+"
            findViewById<RadioButton>(R.id.radio_a_negative).isChecked -> "A-"
            findViewById<RadioButton>(R.id.radio_b_positive).isChecked -> "B+"
            findViewById<RadioButton>(R.id.radio_b_negative).isChecked -> "B-"
            findViewById<RadioButton>(R.id.radio_ab_positive).isChecked -> "AB+"
            findViewById<RadioButton>(R.id.radio_ab_negative).isChecked -> "AB-"
            findViewById<RadioButton>(R.id.radio_o_positive).isChecked -> "O+"
            findViewById<RadioButton>(R.id.radio_o_negative).isChecked -> "O-"
            else -> ""
        }
    }
}
