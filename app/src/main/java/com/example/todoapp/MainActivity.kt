package com.example.todoapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
        )
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
            // 检查权限
            if (checkPermissions()) {
                // 开始语音识别
                baiduASR.startListening()
            } else {
                requestPermissions()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 对于每个权限，检查是否需要显示解释
            val shouldShowRationale = REQUIRED_PERMISSIONS.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }

            if (shouldShowRationale) {
                // 显示解释对话框
                AlertDialog.Builder(this)
                    .setTitle("需要权限")
                    .setMessage("语音识别功能需要麦克风和存储权限才能正常工作")
                    .setPositiveButton("确定") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            REQUIRED_PERMISSIONS,
                            PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else {
                // 直接请求权限
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // 所有权限都已获取，开始语音识别
                baiduASR.startListening()
            } else {
                // 显示设置对话框
                AlertDialog.Builder(this)
                    .setTitle("权限未授予")
                    .setMessage("请在设置中手动开启所需权限，否则语音识别功能将无法使用")
                    .setPositiveButton("去设置") { _, _ ->
                        // 打开应用设置页面
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        baiduASR.release()
    }
}