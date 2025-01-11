package com.example.todoapp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todoapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var todoAdapter: TodoAdapter
    private val todoList = mutableListOf<TodoItem>()
    private val filteredList = mutableListOf<TodoItem>()
    private var currentSearchQuery = ""

    companion object {
        private const val SPEECH_REQUEST_CODE = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            startVoiceRecognition()
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
}