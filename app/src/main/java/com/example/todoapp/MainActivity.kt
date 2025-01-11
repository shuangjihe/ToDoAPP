package com.example.todoapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.todoapp.utils.BaiduASR
// 添加这些导入
import com.volcengine.speech.*
import com.volcengine.speech.recognition.*
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todoapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import android.view.MotionEvent
import android.widget.FloatingActionButton
import androidx.lifecycle.ViewModelProvider
import android.Manifest
import android.content.pm.PackageManager
import com.example.todoapp.utils.BaiduASR

class MainActivity : AppCompatActivity() {
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var baiduASR: BaiduASR
    
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

        todoAdapter = TodoAdapter(mutableListOf())

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
            // 开始语音识别
            baiduASR.startListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        baiduASR.release()
    }
}


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var todoAdapter: TodoAdapter
    private val todoList = mutableListOf<TodoItem>()
    private val filteredList = mutableListOf<TodoItem>()
    private var currentSearchQuery = ""
    private var mIat: SpeechRecognizer? = null
    private lateinit var speechRecognizer: SpeechRecognizer
    private var recognizedText = ""
    private lateinit var todoViewModel: TodoViewModel
    private val PERMISSION_REQUEST_CODE = 123
    private lateinit var baiduASR: BaiduASR

    companion object {
        private const val SPEECH_REQUEST_CODE = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        todoViewModel = ViewModelProvider(this)[TodoViewModel::class.java]
        
        // 添加科大讯飞初始化代码
        SpeechUtility.createUtility(this, "appid=0bfd7d23")

        // 添加讯飞初始化
        initXunfei()
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            loadTodos()
            setupRecyclerView()
            setupClickListeners()
            setupSwipeRefresh()
            setupSearch()
            updateEmptyView()

        } catch (e: Exception) {
            Log.e("TodoAPP-Test", "Error in onCreate: ${e.message}")
        }

        initXunfei()

        // 请求录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
        
        // 初始化语音识别
        initSpeechRecognizer()

        // 初始化语音识别器
        speechRecognizer = SpeechRecognizer(this)
        
        // 设置语音识别按钮的触摸事件
        findViewById<FloatingActionButton>(R.id.btnVoiceRecognition).apply {
            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 按下按钮时开始识别
                        baiduASR.startListening()
                        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(200).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // 松开按钮时停止识别
                        baiduASR.stopListening()
                        view.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                    }
                }
                true
            }
        }

        speechRecognizer.onResultListener = { result ->
            recognizedText = result
        }

        baiduASR = BaiduASR(this)
        baiduASR.onResultListener = { result ->
            // 处理识别结果
            createTodoItem(result)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTodos(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterTodos(binding.etSearch.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun filterTodos(query: String) {
        currentSearchQuery = query.trim().lowercase()

        filteredList.clear()
        if (currentSearchQuery.isEmpty()) {
            filteredList.addAll(todoList)
        } else {
            filteredList.addAll(todoList.filter {
                it.text.lowercase().contains(currentSearchQuery)
            })
        }

        binding.rvTodos.alpha = 1f
        binding.rvTodos.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                todoAdapter.notifyDataSetChanged()
                binding.rvTodos.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
            }
            .start()

        updateEmptyView()
    }

    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(
            filteredList,
            { position -> // onDeleteClick
                val deletedItem = filteredList[position]
                val originalPosition = todoList.indexOf(deletedItem)

                todoList.removeAt(originalPosition)
                filteredList.removeAt(position)

                todoAdapter.notifyItemRemoved(position)
                saveTodos()
                updateEmptyView()

                Snackbar.make(binding.root, "已删除：${deletedItem.text}", Snackbar.LENGTH_LONG)
                    .setAction("撤销") {
                        todoList.add(originalPosition, deletedItem)
                        filterTodos(currentSearchQuery)
                        saveTodos()
                        updateEmptyView()
                    }
                    .show()
            },
            { position -> // onItemClick
                val item = filteredList[position]
                val originalPosition = todoList.indexOf(item)
                todoList[originalPosition].isCompleted = !item.isCompleted
                todoAdapter.notifyItemChanged(position)
                saveTodos()
            }
        )

        binding.rvTodos.apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)

            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
                moveDuration = 300
                changeDuration = 300
            }
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition

                val fromItem = filteredList[fromPos]
                val toItem = filteredList[toPos]
                val originalFromPos = todoList.indexOf(fromItem)
                val originalToPos = todoList.indexOf(toItem)

                todoList.add(originalToPos, todoList.removeAt(originalFromPos))
                filteredList.add(toPos, filteredList.removeAt(fromPos))

                todoAdapter.notifyItemMoved(fromPos, toPos)
                saveTodos()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val deletedItem = filteredList[position]
                val originalPosition = todoList.indexOf(deletedItem)

                todoList.removeAt(originalPosition)
                filteredList.removeAt(position)

                todoAdapter.notifyItemRemoved(position)
                saveTodos()
                updateEmptyView()

                Snackbar.make(binding.root, "已删除：${deletedItem.text}", Snackbar.LENGTH_LONG)
                    .setAction("撤销") {
                        todoList.add(originalPosition, deletedItem)
                        filterTodos(currentSearchQuery)
                        saveTodos()
                        updateEmptyView()
                    }
                    .show()
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.apply {
                        animate()
                            .scaleX(1.05f)
                            .scaleY(1.05f)
                            .alpha(0.9f)
                            .setDuration(100)
                            .start()
                    }
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.apply {
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(100)
                        .start()
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.rvTodos)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            )

            setOnRefreshListener {
                postDelayed({
                    todoList.shuffle()
                    filterTodos(currentSearchQuery)
                    isRefreshing = false
                }, 1000)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnAdd.setOnClickListener {
            val text = binding.etTodo.text.toString().trim()
            if (text.isNotEmpty()) {
                addTodo(text)
                binding.etTodo.text.clear()
            }
        }

        binding.btnVoiceInput.setOnClickListener {
            // 检查权限
            if (checkPermission()) {
                startListening()
            } else {
                requestPermission()
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出要添加的待办事项")
        }

        try {
            // 显示波形动画
            binding.voiceWaveView.visibility = View.VISIBLE
            binding.voiceWaveView.startAnimation()
            binding.btnVoiceInput.visibility = View.INVISIBLE

            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            binding.voiceWaveView.visibility = View.GONE
            binding.btnVoiceInput.visibility = View.VISIBLE
            Snackbar.make(binding.root, "您的设备不支持语音识别", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 停止波形动画
        binding.voiceWaveView.stopAnimation()
        binding.voiceWaveView.visibility = View.GONE
        binding.btnVoiceInput.visibility = View.VISIBLE

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.get(0)?.let { spokenText ->
                if (spokenText.isNotEmpty()) {
                    binding.btnVoiceInput.animate()
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(100)
                        .withEndAction {
                            binding.btnVoiceInput.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                        }
                        .start()

                    addTodo(spokenText)
                    Snackbar.make(binding.root, "已添加：$spokenText", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addTodo(text: String) {
        val newTodo = TodoItem(text)
        todoList.add(0, newTodo)

        if (text.lowercase().contains(currentSearchQuery)) {
            filteredList.add(0, newTodo)
            todoAdapter.notifyItemInserted(0)
            binding.rvTodos.smoothScrollToPosition(0)

            binding.rvTodos.findViewHolderForAdapterPosition(0)?.itemView?.apply {
                scaleX = 0.5f
                scaleY = 0.5f
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start()
            }
        }

        saveTodos()
        updateEmptyView()
    }

    private fun saveTodos() {
        val prefs = getSharedPreferences("TodoApp", MODE_PRIVATE)
        val todoTexts = todoList.map { it.text }
        prefs.edit()
            .putStringSet("todos", todoTexts.toSet())
            .apply()
    }

    private fun loadTodos() {
        val prefs = getSharedPreferences("TodoApp", MODE_PRIVATE)
        val savedTodos = prefs.getStringSet("todos", setOf()) ?: setOf()
        todoList.clear()
        todoList.addAll(savedTodos.map { TodoItem(it) })
        filteredList.addAll(todoList)
    }

    private fun updateEmptyView() {
        if (filteredList.isEmpty()) {
            binding.tvEmpty.apply {
                text = if (currentSearchQuery.isEmpty()) {
                    "还没有待办事项\n点击添加按钮创建新的待办"
                } else {
                    "没有找到匹配的待办事项"
                }
                visibility = View.VISIBLE
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    private fun initXunfei() {
        // 初始化识别对象
        mIat = SpeechRecognizer.createRecognizer(this) { code ->
            if (code != ErrorCode.SUCCESS) {
                Toast.makeText(this, "初始化失败", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 设置语音识别参数
        mIat?.setParameter(SpeechConstant.DOMAIN, "iat")
        mIat?.setParameter(SpeechConstant.LANGUAGE, "zh_cn")
        mIat?.setParameter(SpeechConstant.ACCENT, "mandarin")
    }

    // 添加权限检查方法
    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 添加权限请求方法
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            1
        )
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
                Toast.makeText(this, "录音权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要录音权限才能使用语音识别功能", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }
    }
    
    private fun initSpeechRecognizer() {
        val config = SpeechConfig().apply {
            // 设置你的 AppID
            appId = "你的AppID"
            // 设置你的 AccessKey
            accessKey = "你的AccessKey"
            // 设置你的 SecretKey
            secretKey = "你的SecretKey"
        }
        
        // 创建识别器
        speechRecognizer = SpeechRecognizer(this, config)
        
        // 设置识别回调
        speechRecognizer.setListener(object : RecognitionListener {
            override fun onReadyForSpeech() {
                // 准备就绪，可以开始说话
                binding.voiceWaveView.visibility = View.VISIBLE
                binding.voiceWaveView.startAnimation()
                binding.btnVoiceInput.visibility = View.GONE
            }
            
            override fun onResults(results: String) {
                // 识别结果
                addTodo(results)
                binding.voiceWaveView.stopAnimation()
                binding.voiceWaveView.visibility = View.GONE
                binding.btnVoiceInput.visibility = View.VISIBLE
            }
            
            override fun onError(errorCode: Int, errorMessage: String) {
                // 发生错误
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
                binding.voiceWaveView.stopAnimation()
                binding.voiceWaveView.visibility = View.GONE
                binding.btnVoiceInput.visibility = View.VISIBLE
            }
        })
    }
    
    private fun startListening() {
        speechRecognizer.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.release()
        baiduASR.release()
    }

    // 创建待办事项的方法
    private fun createTodoItem(text: String) {
        // 创建一个新的 Todo 对象
        val todo = Todo(
            title = text,
            description = "",  // 可以根据需要设置描述
            priority = Priority.LOW,  // 默认优先级
            timestamp = System.currentTimeMillis()
        )
        
        // 使用 ViewModel 插入新的待办事项
        todoViewModel.insert(todo)
        
        // 可以添加一个提示
        Toast.makeText(this, "已添加：$text", Toast.LENGTH_SHORT).show()
    }
}