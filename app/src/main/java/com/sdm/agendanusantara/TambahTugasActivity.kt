package com.sdm.agendanusantara

import android.app.DatePickerDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sdm.agendanusantara.databinding.ActivityTambahTugasBinding
import com.sdm.agendanusantara.db.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Halaman Tambah Tugas — dipakai bersama untuk kategori PENTING dan BIASA.
 * Tema warna (merah/hijau), judul, dan badge disesuaikan otomatis
 * berdasarkan EXTRA_CATEGORY yang dikirim dari BerandaActivity.
 */
class TambahTugasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahTugasBinding
    private lateinit var db: DatabaseHelper

    private var selectedDateDb   = ""   // "yyyy-MM-dd" → disimpan ke SQLite
    private var selectedDateShow = ""   // "06 Mei 2026" → ditampilkan ke user

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahTugasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        val category  = intent.getStringExtra(EXTRA_CATEGORY) ?: DatabaseHelper.CATEGORY_BIASA
        val isPenting = (category == DatabaseHelper.CATEGORY_PENTING)

        // Terapkan tema warna sesuai kategori
        applyTheme(isPenting)

        // Tanggal default = hari ini
        setDate(Calendar.getInstance())

        // Listeners
        binding.etTanggal.setOnClickListener { showDatePicker() }
        binding.ivCalendar.setOnClickListener { showDatePicker() }
        binding.btnBack.setOnClickListener   { finish() }
        binding.btnSimpan.setOnClickListener { simpanTugas(category) }
    }

    // ── Tema warna: Merah (Penting) vs Hijau (Biasa) ─────────────────
    private fun applyTheme(isPenting: Boolean) {
        val colorRes = if (isPenting) R.color.color_penting else R.color.color_biasa_green
        val color    = ContextCompat.getColor(this, colorRes)

        // Header bar
        binding.headerBar.setBackgroundColor(color)

        // Judul
        binding.tvTitle.text = if (isPenting) "Tambah Tugas Penting" else "Tambah Tugas Biasa"

        // Badge teks & warna
        binding.tvBadge.text = if (isPenting) "PENTING" else "BIASA"
        binding.tvBadge.setTextColor(color)
        val badgeAlphaColor = if (isPenting) 0x26EF4444.toInt() else 0x264DA04D.toInt()
        (binding.tvBadge.background as? GradientDrawable)?.setColor(badgeAlphaColor)

        // Tombol Simpan — pertahankan corner radius, ganti warna
        val density = resources.displayMetrics.density
        val btnBg = GradientDrawable().apply {
            shape         = GradientDrawable.RECTANGLE
            cornerRadius  = 30f * density
            setColor(color)
        }
        binding.btnSimpan.background = btnBg
    }

    // ── DatePickerDialog ─────────────────────────────────────────────
    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day)
                setDate(picked)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setDate(cal: Calendar) {
        selectedDateDb   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        selectedDateShow = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            .format(cal.time).replaceFirstChar { it.uppercase() }
        binding.etTanggal.setText(selectedDateShow)
    }

    // ── Validasi & simpan ke SQLite ───────────────────────────────────
    private fun simpanTugas(category: String) {
        val judul     = binding.etJudul.text?.toString()?.trim() ?: ""
        val deskripsi = binding.etDeskripsi.text?.toString()?.trim() ?: ""
        var hasError  = false

        if (selectedDateDb.isEmpty()) {
            Toast.makeText(this, "Pilih tanggal jatuh tempo", Toast.LENGTH_SHORT).show()
            hasError = true
        }
        if (judul.isEmpty()) {
            binding.tilJudul.error = "Judul tidak boleh kosong"
            hasError = true
        } else {
            binding.tilJudul.error = null
        }
        if (hasError) return

        val prefs    = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val username = prefs.getString(LoginActivity.KEY_USERNAME, "user") ?: "user"
        val userId   = db.getUserId(username)

        val result = db.insertTask(
            userId      = userId,
            title       = judul,
            description = deskripsi,
            dueDate     = selectedDateDb,
            category    = category
        )

        if (result > 0) {
            val label = if (category == DatabaseHelper.CATEGORY_PENTING) "penting" else "biasa"
            Toast.makeText(this, "Tugas $label berhasil disimpan!", Toast.LENGTH_SHORT).show()
            finish()    // kembali ke Beranda → onResume() refresh stats & chart
        } else {
            Toast.makeText(this, "Gagal menyimpan. Coba lagi.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
    }
}
