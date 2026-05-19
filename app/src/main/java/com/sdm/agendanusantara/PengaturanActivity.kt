package com.sdm.agendanusantara

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sdm.agendanusantara.databinding.ActivityPengaturanBinding
import com.sdm.agendanusantara.db.DatabaseHelper

class PengaturanActivity : AppCompatActivity() {

    private lateinit var binding : ActivityPengaturanBinding
    private lateinit var db      : DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPengaturanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSimpanPassword.setOnClickListener { gantiPassword() }
    }

    private fun gantiPassword() {
        val passwordLama = binding.etPasswordLama.text?.toString() ?: ""
        val passwordBaru = binding.etPasswordBaru.text?.toString() ?: ""

        // Validasi tidak kosong
        var hasError = false
        if (passwordLama.isEmpty()) {
            binding.tilPasswordLama.error = "Password saat ini tidak boleh kosong"
            hasError = true
        } else {
            binding.tilPasswordLama.error = null
        }
        if (passwordBaru.isEmpty()) {
            binding.tilPasswordBaru.error = "Password baru tidak boleh kosong"
            hasError = true
        } else {
            binding.tilPasswordBaru.error = null
        }
        if (hasError) return

        // Validasi minimum panjang
        if (passwordBaru.length < 4) {
            binding.tilPasswordBaru.error = "Password baru minimal 4 karakter"
            return
        }

        // Ambil username dari session
        val prefs    = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val username = prefs.getString(LoginActivity.KEY_USERNAME, "user") ?: "user"

        // Cek password lama
        if (!db.validateUser(username, passwordLama)) {
            binding.tilPasswordLama.error = "Password saat ini salah"
            binding.etPasswordLama.text?.clear()
            return
        }

        // Simpan password baru ke SQLite
        val berhasil = db.updatePassword(username, passwordBaru)
        if (berhasil) {
            binding.etPasswordLama.text?.clear()
            binding.etPasswordBaru.text?.clear()
            binding.tilPasswordLama.error = null
            binding.tilPasswordBaru.error = null
            Toast.makeText(this, "Password berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Gagal memperbarui password. Coba lagi.", Toast.LENGTH_SHORT).show()
        }
    }
}
