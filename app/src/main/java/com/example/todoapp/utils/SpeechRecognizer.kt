package com.example.todoapp.utils

import android.content.Context
import com.baidu.speech.EventListener
import com.baidu.speech.EventManager
import com.baidu.speech.EventManagerFactory
import com.baidu.speech.asr.SpeechConstant
import org.json.JSONObject

class SpeechRecognizer(context: Context) {
    private var asr: EventManager? = null
    
    init {
        // 初始化EventManager对象
        asr = EventManagerFactory.create(context, "asr")
        // 注册事件监听器
        asr?.registerListener(eventListener)
        
        // 初始化百度语音识别
        val params = JSONObject()
        params.put("pid", 1537) // 1537表示普通话搜索模型
        params.put("appid", "6258298")
        params.put("apikey", "ejD0qEmebJb8D72jUlz1Tt3B")
        params.put("secretkey", "7GJRL7ovIYNJhn1QxTUE1hIhzjoSMSgd")
        
        asr?.send(SpeechConstant.ASR_START, params.toString(), null, 0, 0)
    }
    
    var onResultListener: ((String) -> Unit)? = null
    
    private val eventListener = EventListener { name, params, data, offset, length ->
        when (name) {
            SpeechConstant.CALLBACK_EVENT_ASR_READY -> {
                // 引擎就绪
            }
            SpeechConstant.CALLBACK_EVENT_ASR_BEGIN -> {
                // 检测到说话开始
            }
            SpeechConstant.CALLBACK_EVENT_ASR_END -> {
                // 检测到说话结束
            }
            SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL -> {
                // 识别结果
                try {
                    val json = JSONObject(params)
                    val result = json.optString("results_recognition", "")
                    onResultListener?.invoke(result)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            SpeechConstant.CALLBACK_EVENT_ASR_FINISH -> {
                // 识别结束
            }
            SpeechConstant.CALLBACK_EVENT_ASR_ERROR -> {
                // 识别错误
            }
        }
    }
    
    fun startListening() {
        asr?.send(SpeechConstant.ASR_START, null, null, 0, 0)
    }
    
    fun stopListening() {
        asr?.send(SpeechConstant.ASR_STOP, null, null, 0, 0)
    }
    
    fun release() {
        asr?.unregisterListener(eventListener)
        asr = null
    }
} 