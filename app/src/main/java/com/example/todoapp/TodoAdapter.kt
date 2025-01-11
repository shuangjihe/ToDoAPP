package com.example.todoapp

import android.animation.ValueAnimator
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.example.todoapp.databinding.ItemTodoBinding

class TodoAdapter(
    private val todoList: List<TodoItem>,
    private val onDeleteClick: (Int) -> Unit,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = todoList[position]
        holder.binding.apply {
            // 设置文本
            tvTodoText.text = todo.text

            // 设置复选框状态（不触发监听器）
            cbDone.setOnCheckedChangeListener(null)
            cbDone.isChecked = todo.isCompleted

            // 设置删除线和透明度
            updateTextAppearance(this, todo.isCompleted)

            // 设置点击事件
            root.setOnClickListener {
                onItemClick(position)
                // 切换复选框状态
                cbDone.isChecked = !cbDone.isChecked
            }

            // 设置复选框点击事件
            cbDone.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != todo.isCompleted) {
                    onItemClick(position)
                    animateCompletion(this, isChecked)
                }
            }

            // 设置删除按钮点击事件
            btnDelete.setOnClickListener {
                // 添加删除动画
                root.animate()
                    .translationX(root.width.toFloat())
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        onDeleteClick(position)
                    }
                    .start()
            }
        }
    }

    private fun updateTextAppearance(binding: ItemTodoBinding, isCompleted: Boolean) {
        binding.tvTodoText.apply {
            if (isCompleted) {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                alpha = 0.5f
            } else {
                paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                alpha = 1.0f
            }
        }
    }

    private fun animateCompletion(binding: ItemTodoBinding, isCompleted: Boolean) {
        // 创建透明度动画
        ValueAnimator.ofFloat(
            if (isCompleted) 1f else 0.5f,
            if (isCompleted) 0.5f else 1f
        ).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                binding.tvTodoText.alpha = animator.animatedValue as Float
            }
            start()
        }

        // 缩放动画
        binding.root.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(150)
            .withEndAction {
                binding.root.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    override fun getItemCount() = todoList.size
}