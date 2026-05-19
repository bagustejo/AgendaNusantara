package com.sdm.agendanusantara

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sdm.agendanusantara.adapter.TaskAdapter
import com.sdm.agendanusantara.databinding.ActivityDaftarTugasBinding
import com.sdm.agendanusantara.db.DatabaseHelper

class DaftarTugasActivity : AppCompatActivity() {

    private lateinit var binding : ActivityDaftarTugasBinding
    private lateinit var db      : DatabaseHelper
    private lateinit var adapter : TaskAdapter
    private var userId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarTugasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        val prefs    = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val username = prefs.getString(LoginActivity.KEY_USERNAME, "user") ?: "user"
        userId = db.getUserId(username)

        // RecyclerView setup
        adapter = TaskAdapter(
            tasks   = mutableListOf(),
            db      = db,
            userId  = userId,
            onToggle = { refreshEmptyState() }
        )
        binding.rvTugas.layoutManager = LinearLayoutManager(this)
        binding.rvTugas.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun loadTasks() {
        val tasks = db.getAllTasksAsList(userId)
        adapter.updateData(tasks)
        refreshEmptyState()
    }

    private fun refreshEmptyState() {
        val isEmpty = adapter.itemCount == 0
        binding.tvEmpty.visibility  = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvTugas.visibility  = if (isEmpty) View.GONE    else View.VISIBLE
    }
}
