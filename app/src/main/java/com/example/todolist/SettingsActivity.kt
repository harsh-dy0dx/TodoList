package com.example.todolist

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        // --- FIX: Load new profile details ---
        binding.editTextName.setText(sharedPref.getString("user_name", ""))
        binding.editTextEmail.setText(sharedPref.getString("user_email", ""))
        binding.editTextBio.setText(sharedPref.getString("user_bio", ""))


        binding.buttonSave.setOnClickListener {
            val newName = binding.editTextName.text.toString().trim()
            val newEmail = binding.editTextEmail.text.toString().trim()
            val newBio = binding.editTextBio.text.toString().trim()

            if (newName.isNotEmpty()) {
                // --- FIX: Save all profile details ---
                with(sharedPref.edit()) {
                    putString("user_name", newName)
                    putString("user_email", newEmail)
                    putString("user_bio", newBio)
                    apply()
                }
                Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
