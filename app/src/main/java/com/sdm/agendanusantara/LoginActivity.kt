package com.sdm.agendanusantara

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sdm.agendanusantara.databinding.ActivityLoginBinding
import com.sdm.agendanusantara.db.DatabaseHelper

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        // If already logged in, skip directly to Beranda
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        if (prefs.getString(KEY_USERNAME, null) != null) {
            goToBeranda()
            return
        }

        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        val username = binding.etUsername.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString()?.trim() ?: ""

        var hasError = false

        // Validate username
        if (username.isEmpty()) {
            binding.tilUsername.error = "Username tidak boleh kosong"
            hasError = true
        } else {
            binding.tilUsername.error = null
        }

        // Validate password
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password tidak boleh kosong"
            hasError = true
        } else {
            binding.tilPassword.error = null
        }

        if (hasError) return

        // Show loading indicator
        binding.btnLogin.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        // Check credentials against SQLite
        if (db.validateUser(username, password)) {
            // Persist session
            getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                .putString(KEY_USERNAME, username)
                .apply()
            goToBeranda()
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            Toast.makeText(this, "Username atau password salah", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToBeranda() {
        startActivity(Intent(this, BerandaActivity::class.java))
        finish()
    }

    companion object {
        const val PREF_NAME    = "agenda_session"
        const val KEY_USERNAME = "username"
    }
}
