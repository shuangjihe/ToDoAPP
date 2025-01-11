package com.example.todoapp.utils

import android.content.Context
import com.baidu.speech.EventListener
import com.baidu.speech.EventManager
import com.baidu.speech.EventManagerFactory
import com.baidu.speech.asr.SpeechConstant
import org.json.JSONObject

class BaiduASR(private val context: Context) {
    private var asr: EventManager? = null
    private var listener: EventListener? = null
    var onResultListener: ((String) -> Unit)? = null

    init {
        initASR()
    }

    private fun initASR() {
        // 初始化EventManager对象
        asr = EventManagerFactory.create(context, "asr")
        
        // 注册事件监听器
        listener = EventListener { name, params, _, _, _ ->
            when (name) {
                SpeechConstant.CALLBACK_EVENT_ASR_READY -> {
                    // 引擎就绪，可以开始说话
                }
                SpeechConstant.CALLBACK_EVENT_ASR_BEGIN -> {
                    // 检测到用户的已经开始说话
                }
                SpeechConstant.CALLBACK_EVENT_ASR_END -> {
                    // 检测到用户的已经停止说话
                }
                SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL -> {
                    // 识别结果
                    val json = JSONObject(params)
                    json.optJSONArray("results_recognition")?.let {
                        if (it.length() > 0) {
                            val result = it.getString(0)
                            onResultListener?.invoke(result)
                        }
                    }
                }
                SpeechConstant.CALLBACK_EVENT_ASR_FINISH -> {
                    // 识别结束（可能是错误导致）
                }
                SpeechConstant.CALLBACK_EVENT_ASR_ERROR -> {
                    // 识别错误
                }
            }
        }
        
        asr?.registerListener(listener)
    }

    fun startListening() {
        val params = JSONObject()
        params.put("pid", 1537) // 普通话搜索模型
        params.put("appid", "6258298")
        params.put("appkey", "ejD0qEmebJb8D72jUlz1Tt3B")
        params.put("secret", "7GJRL7ovIYNJhn1QxTUE1hIhzjoSMSgd")
        
        asr?.send(SpeechConstant.ASR_START, params.toString(), null, 0, 0)
    }

    fun stopListening() {
        asr?.send(SpeechConstant.ASR_STOP, null, null, 0, 0)
    }

    fun release() {
        listener?.let { asr?.unregisterListener(it) }
        asr = null
        listener = null
    }
} 