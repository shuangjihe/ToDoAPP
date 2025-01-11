package com.example.todoapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.todoapp.utils.BaiduASR

class MainActivity : AppCompatActivity() {
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var baiduASR: BaiduASR

    // 注册权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // 所有权限都已获取
            startVoiceRecognition()
        } else {
            Toast.makeText(this, "需要所有权限才能使用语音识别", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 初始化语音识别
        baiduASR = BaiduASR(this)
        baiduASR.onResultListener = { result ->
            // 处理语音识别结果
            val etTodoTitle = findViewById<EditText>(R.id.etTodoTitle)
            etTodoTitle.setText(result)
        }

        todoAdapter = TodoAdapter(
            todos = mutableListOf(),
            onDeleteClick = { position ->
                todoAdapter.deleteTodo(position)
            }
        )

        val rvTodoItems = findViewById<RecyclerView>(R.id.rvTodoItems)
        rvTodoItems.adapter = todoAdapter
        rvTodoItems.layoutManager = LinearLayoutManager(this)

        val btnAddTodo = findViewById<Button>(R.id.btnAddTodo)
        val btnDeleteDoneTodos = findViewById<Button>(R.id.btnDeleteDoneTodos)
        val etTodoTitle = findViewById<EditText>(R.id.etTodoTitle)
        val fabVoice = findViewById<FloatingActionButton>(R.id.btnVoiceRecognition)

        btnAddTodo.setOnClickListener {
            val todoTitle = etTodoTitle.text.toString()
            if (todoTitle.isNotEmpty()) {
                val todo = Todo(todoTitle)
                todoAdapter.addTodo(todo)
                etTodoTitle.text.clear()
            }
        }

        btnDeleteDoneTodos.setOnClickListener {
            todoAdapter.deleteDoneTodos()
        }

        fabVoice.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        if (REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }) {
            // 已经有所有权限，直接开始语音识别
            startVoiceRecognition()
        } else {
            // 请求权限
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun startVoiceRecognition() {
        baiduASR.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        baiduASR.release()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    }
}