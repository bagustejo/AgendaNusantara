package com.sdm.agendanusantara

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sdm.agendanusantara.databinding.ActivityBerandaBinding
import com.sdm.agendanusantara.db.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class BerandaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBerandaBinding
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBerandaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        val prefs    = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val username = prefs.getString(LoginActivity.KEY_USERNAME, "User") ?: "User"
        val userId   = db.getUserId(username)

        // Greeting
        binding.tvGreeting.text = "Halo, $username!"
        binding.tvDate.text     = getTodayLabel()

        // Stats & chart
        refreshStats(userId)
        loadWeeklyChart(userId)

        // Navigation
        binding.btnTambahPenting.setOnClickListener {
            startActivity(Intent(this, TambahTugasActivity::class.java).apply {
                putExtra(TambahTugasActivity.EXTRA_CATEGORY, DatabaseHelper.CATEGORY_PENTING)
            })
        }
        binding.btnTambahBiasa.setOnClickListener {
            startActivity(Intent(this, TambahTugasActivity::class.java).apply {
                putExtra(TambahTugasActivity.EXTRA_CATEGORY, DatabaseHelper.CATEGORY_BIASA)
            })
        }
        binding.btnDaftarTugas.setOnClickListener {
            startActivity(Intent(this, DaftarTugasActivity::class.java))
        }
        binding.btnPengaturan.setOnClickListener {
            startActivity(Intent(this, PengaturanActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs    = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val username = prefs.getString(LoginActivity.KEY_USERNAME, "User") ?: "User"
        val userId   = db.getUserId(username)
        refreshStats(userId)
        loadWeeklyChart(userId)
    }

    private fun refreshStats(userId: Int) {
        binding.tvSelesai.text = db.countTasks(userId, isDone = true).toString()
        binding.tvBelum.text   = db.countTasks(userId, isDone = false).toString()
    }

    private fun loadWeeklyChart(userId: Int) {
        val sdf    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())  // format tanggal untuk query DB, contoh: "2026-05-19"
        val dayFmt = SimpleDateFormat("EEE", Locale("id", "ID"))          // format nama hari pendek bahasa Indonesia, contoh: "Sen"
        val cal    = Calendar.getInstance()                                // kalender berisi waktu sekarang
        cal.firstDayOfWeek = Calendar.MONDAY                              // tetapkan Senin sebagai hari pertama dalam seminggu
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)                    // mundurkan kalender ke Senin minggu berjalan

        val entries = mutableListOf<Pair<String, Int>>()                  // list pasangan (label hari, jumlah tugas selesai)
        repeat(7) {                                                        // iterasi 7 kali: Senin s.d. Minggu
            val dayLabel = dayFmt.format(cal.time).replaceFirstChar { it.uppercase() }.take(3) // nama hari 3 huruf, huruf pertama kapital
            val count    = db.countTasksDoneOnDate(userId, sdf.format(cal.time))               // ambil jumlah tugas selesai pada hari ini dari DB
            entries.add(Pair(dayLabel, count))                            // tambahkan data hari ini ke list
            cal.add(Calendar.DAY_OF_YEAR, 1)                              // maju ke hari berikutnya
        }
        binding.barChart.setData(entries)                                 // kirim data ke custom view untuk dirender sebagai grafik batang
    }

    private fun getTodayLabel(): String {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")) // format tanggal panjang bahasa Indonesia
        return sdf.format(Date()).replaceFirstChar { it.uppercase() }        // format hari ini, pastikan huruf pertama kapital
    }
}
