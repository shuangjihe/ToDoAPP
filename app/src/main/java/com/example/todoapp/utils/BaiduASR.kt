package com.example.todoapp.utils

import android.content.Context
import com.baidu.speech.EventListener
import com.baidu.speech.EventManager
import com.baidu.speech.EventManagerFactory
import com.baidu.speech.asr.SpeechConstant
import org.json.JSONObject

class BaiduASR(private val context: Context) {
    private var asr: EventManager? = null
    private var eventListener: EventListener = EventListener { name, params, _, _, _ ->
        when (name) {
            SpeechConstant.CALLBACK_EVENT_ASR_READY -> {
                println("引擎就绪，可以开始说话")
            }
            SpeechConstant.CALLBACK_EVENT_ASR_BEGIN -> {
                println("检测到用户开始说话")
            }
            SpeechConstant.CALLBACK_EVENT_ASR_END -> {
                println("检测到用户停止说话")
            }
            SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL -> {
                try {
                    val json = JSONObject(params)
                    val results = json.optJSONArray("results_recognition")
                    if (results != null && results.length() > 0) {
                        val result = results.getString(0)
                        println("部分识别结果: $result")
                        if (json.optInt("result_type", -1) == 5) { // 最终结果
                            onResultListener?.invoke(result)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
    
    var onResultListener: ((String) -> Unit)? = null

    init {
        initASR()
    }

    private fun initASR() {
        try {
            asr = EventManagerFactory.create(context, "asr")
            asr?.registerListener(eventListener)
            println("语音识别初始化成功")
        } catch (e: Exception) {
            println("语音识别初始化失败: ${e.message}")
            e.printStackTrace()
        }
    }

    fun startListening() {
        try {
            val params = JSONObject().apply {
                put("pid", 1537) // 普通话搜索模型
                put("accept-audio-volume", false)
                put("disable-punctuation", true)
                put("accept-audio-data", true)
                put("vad.endpoint-timeout", 800) // 静音超时时间
                put("vad", "touch") // 点触模式
            }
            
            println("准备开始语音识别")
            asr?.send(SpeechConstant.ASR_START, params.toString(), null, 0, 0)
            println("已发送开始语音识别命令")
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
            asr?.unregisterListener(eventListener)
            asr = null
            println("释放语音识别资源")
        } catch (e: Exception) {
            println("释放语音识别资源失败: ${e.message}")
            e.printStackTrace()
        }
    }
} 