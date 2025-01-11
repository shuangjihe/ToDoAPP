package com.example.todoapp.utils

import android.content.Context
import android.util.Log
import com.baidu.speech.EventListener
import com.baidu.speech.EventManager
import com.baidu.speech.EventManagerFactory
import com.baidu.speech.asr.SpeechConstant
import org.json.JSONObject

class BaiduASR(private val context: Context) {
    private var asr: EventManager? = null
    private var eventListener: EventListener = EventListener { name, params, _, _, _ ->
        Log.d("BaiduASR", "收到事件: $name, 参数: $params")
        
        when (name) {
            SpeechConstant.CALLBACK_EVENT_ASR_READY -> {
                Log.d("BaiduASR", "引擎就绪，可以开始说话")
            }
            SpeechConstant.CALLBACK_EVENT_ASR_BEGIN -> {
                Log.d("BaiduASR", "检测到用户开始说话")
            }
            SpeechConstant.CALLBACK_EVENT_ASR_END -> {
                Log.d("BaiduASR", "检测到用户停止说话")
            }
            SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL -> {
                try {
                    Log.d("BaiduASR", "收到部分结果：$params")
                    val json = JSONObject(params)
                    val results = json.optJSONArray("results_recognition")
                    if (results != null && results.length() > 0) {
                        val result = results.getString(0)
                        Log.d("BaiduASR", "部分识别结果: $result")
                        onResultListener?.invoke(result)
                    }
                } catch (e: Exception) {
                    Log.e("BaiduASR", "处理部分结果失败", e)
                }
            }
            SpeechConstant.CALLBACK_EVENT_ASR_FINISH -> {
                Log.d("BaiduASR", "识别结束，参数：$params")
                try {
                    val json = JSONObject(params)
                    val errorCode = json.optInt("error", 0)
                    if (errorCode != 0) {
                        Log.e("BaiduASR", "识别错误，错误码：$errorCode")
                    }
                } catch (e: Exception) {
                    Log.e("BaiduASR", "处理结束事件失败", e)
                }
            }
            SpeechConstant.CALLBACK_EVENT_ASR_ERROR -> {
                Log.e("BaiduASR", "识别错误: $params")
            }
        }
    }
    
    var onResultListener: ((String) -> Unit)? = null

    companion object {
        // 替换成你的百度语音 APP_ID、API_KEY 和 SECRET_KEY
        private const val APP_ID = "6258298"
        private const val API_KEY = "ejD0qEmebJb8D72jUlz1Tt3B"
        private const val SECRET_KEY = "7GJRL7ovIYNJhn1QxTUE1hIhzjoSMSgd"
    }

    init {
        initASR()
    }

    private fun initASR() {
        try {
            // 设置鉴权相关参数
            val map = HashMap<String, String>().apply {
                put("appid", APP_ID)
                put("api_key", API_KEY)
                put("secret_key", SECRET_KEY)
            }
            
            // 初始化前先进行授权
            val authManager = EventManagerFactory.create(context, "asr.auth")
            authManager?.send("asr.auth", JSONObject(map).toString(), null, 0, 0)
            
            // 初始化语音识别
            asr = EventManagerFactory.create(context, "asr")
            asr?.registerListener(eventListener)
            Log.d("BaiduASR", "语音识别初始化成功")
        } catch (e: Exception) {
            Log.e("BaiduASR", "语音识别初始化失败", e)
        }
    }

    fun startListening() {
        try {
            val params = JSONObject().apply {
                put("pid", 1537)
                put("accept-audio-volume", false)
                put("disable-punctuation", true)
                put("accept-audio-data", true)
                put("vad.endpoint-timeout", 0)
                put("vad", "touch")
            }
            
            Log.d("BaiduASR", "开始语音识别，参数：${params}")
            asr?.send(SpeechConstant.ASR_START, params.toString(), null, 0, 0)
            Log.d("BaiduASR", "已发送开始语音识别命令")
        } catch (e: Exception) {
            Log.e("BaiduASR", "开始语音识别失败", e)
        }
    }

    fun stopListening() {
        try {
            Log.d("BaiduASR", "准备停止语音识别")
            asr?.send(SpeechConstant.ASR_STOP, null, null, 0, 0)
            Log.d("BaiduASR", "已发送停止语音识别命令")
        } catch (e: Exception) {
            Log.e("BaiduASR", "停止语音识别失败", e)
        }
    }

    fun release() {
        try {
            Log.d("BaiduASR", "准备释放语音识别资源")
            asr?.unregisterListener(eventListener)
            asr = null
            Log.d("BaiduASR", "已释放语音识别资源")
        } catch (e: Exception) {
            Log.e("BaiduASR", "释放语音识别资源失败", e)
        }
    }
} 