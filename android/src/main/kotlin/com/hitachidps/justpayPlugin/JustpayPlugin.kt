package com.hitachidps.justpayPlugin

import android.content.*
import android.os.Build
import android.os.Handler
import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import com.lankaclear.justpay.LCTrustedSDK
import com.lankaclear.justpay.callbacks.CreateIdentityCallback
import io.flutter.plugin.common.BinaryMessenger
import com.lankaclear.justpay.callbacks.SignMessageCallback
import kotlin.collections.HashMap
import android.os.Looper




/** JustpayPlugin */
public class JustpayPlugin: FlutterPlugin, MethodCallHandler {

  private lateinit var channel : MethodChannel



  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "justpayPlugin")
    channel.setMethodCallHandler(this);
    buildEngine(flutterPluginBinding.applicationContext, flutterPluginBinding.binaryMessenger)
  }

  companion object {


    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "justpayPlugin")
      channel.setMethodCallHandler(JustpayPlugin())
      val instance = JustpayPlugin()
      instance.buildEngine(registrar.activeContext()!!, registrar.messenger()!!)



    }

    lateinit var applicationContext: Context
    lateinit var lcTrustedSDK: LCTrustedSDK

  }
  private class MethodResultWrapper internal constructor(private val methodResult: Result) : Result {
    private val handler: Handler

    init {
      handler = Handler(Looper.getMainLooper())
    }

    override fun success(result: Any?) {
      handler.post(
              Runnable { methodResult.success(result) })
    }

    override fun error(
            errorCode: String, errorMessage: String?, errorDetails: Any?) {
      handler.post(
              Runnable { methodResult.error(errorCode, errorMessage, errorDetails) })
    }

    override fun notImplemented() {
      handler.post(
              Runnable { methodResult.notImplemented() })
    }
  }


  override fun onMethodCall(@NonNull call: MethodCall, @NonNull rawResult: Result) {
    val result = MethodResultWrapper(rawResult)

    if (call.method == "getPlatformVersion") {
      result.success("Android ${Build.VERSION.RELEASE}")
    }
    else if (call.method == "getDeviceId") {
      val deviceID = lcTrustedSDK!!.getDeviceID()
      result.success(deviceID)
    }
    else if (call.method == "isIdentityExist") {
      isIdentityExist(result)
    }
    else if (call.method == "createIdentity") {
      var challenge = call.argument<String>("challengeKey")
      if (challenge != null) {
        createIdentity(challenge,result)
      }
    }
    else if (call.method == "signMessage") {
      var message = call.argument<String>("message")
      if (message != null) {
        signMessage(message, result)
      }
    }

    else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private fun isIdentityExist(@NonNull result: Result) {
    var isIdentity = lcTrustedSDK.isIdentityExist()
    result.success(isIdentity)
  }

  private fun createIdentity(challengeKey: String , @NonNull result: Result){

    lcTrustedSDK.createIdentity(challengeKey, object : CreateIdentityCallback {
      override fun onSuccess() {
        result.success("success");
      }

      override fun onFailed(errorCode: Int, errorMessage: String) {
        val errorMsg = errorMessage.ifEmpty { "Unknown error occurred" }
        result.error(errorCode.toString(),errorMessage, "Error in createIdentity")
      }
    })
//    result.success(true)
  }

  private fun signMessage(message : String, @NonNull result: Result){

    lcTrustedSDK.signMessage(message, object : SignMessageCallback {

      override fun onSuccess(signMessage: String, status: String) {
//        val response = HashMap<String,String>()
//        response.put("message", signMessage)
//        response.put("status", status)
//        val responseObj = response.toString()
        result.success(signMessage)
      }

      override fun onFailed(errorCode: Int, errorMessage: String) {
         val errorMsg = errorMessage.ifEmpty { "Unknown error occurred" }
            val response = hashMapOf(
                "errorCode" to errorCode.toString(),
                "errorMessage" to errorMsg
            )
      //  val response = HashMap<String, String>()
      //  response.put("errorCode", errorCode.toString())
     //   response.put("errorMessage", errorMessage)
     //   val responseObj = response.toString()
        result.error(errorCode.toString(),errorMessage,responseObj)
      }
    })
  }

  private fun buildEngine(context: Context, messenger: BinaryMessenger) {
    applicationContext = context!!
    lcTrustedSDK = LCTrustedSDK(context!!)
    lcTrustedSDK.init()

  }
}
