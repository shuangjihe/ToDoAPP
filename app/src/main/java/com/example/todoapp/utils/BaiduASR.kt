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
        try {
            // 初始化EventManager对象
            asr = EventManagerFactory.create(context, "asr")
            
            // 注册事件监听器
            listener = EventListener { name, params, _, _, _ ->
                when (name) {
                    SpeechConstant.CALLBACK_EVENT_ASR_READY -> {
                        println("引擎就绪，可以开始说话")
                    }
                    SpeechConstant.CALLBACK_EVENT_ASR_BEGIN -> {
                        println("检测到用户的已经开始说话")
                    }
                    SpeechConstant.CALLBACK_EVENT_ASR_END -> {
                        println("检测到用户的已经停止说话")
                    }
                    SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL -> {
                        // 识别结果
                        val json = JSONObject(params)
                        json.optJSONArray("results_recognition")?.let {
                            if (it.length() > 0) {
                                val result = it.getString(0)
                                println("识别结果: $result")
                                onResultListener?.invoke(result)
                            }
                        }
                    }
                    SpeechConstant.CALLBACK_EVENT_ASR_FINISH -> {
                        println("识别结束")
                    }
                    SpeechConstant.CALLBACK_EVENT_ASR_ERROR -> {
                        println("识别错误: $params")
                    }
                }
            }
            
            asr?.registerListener(listener)
            println("语音识别初始化成功")
        } catch (e: Exception) {
            println("语音识别初始化失败: ${e.message}")
            e.printStackTrace()
        }
    }

    fun startListening() {
        try {
            val params = JSONObject()
            params.put("pid", 1537) // 普通话搜索模型
            params.put("accept-audio-volume", false)
            params.put("disable-punctuation", true)
            params.put("accept-audio-data", true)
            
            asr?.send(SpeechConstant.ASR_START, params.toString(), null, 0, 0)
            println("开始语音识别")
        } catch (e: Exception) {
            println("开始语音识别失败: ${e.message}")
            e.printStackTrace()
        }
    }

    fun stopListening() {
        try {
            asr?.send(SpeechConstant.ASR_STOP, null, null, 0, 0)
            println("停止语音识别")
        } catch (e: Exception) {
            println("停止语音识别失败: ${e.message}")
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            listener?.let { asr?.unregisterListener(it) }
            asr = null
            listener = null
            println("释放语音识别资源")
        } catch (e: Exception) {
            println("释放语音识别资源失败: ${e.message}")
            e.printStackTrace()
        }
    }
} 