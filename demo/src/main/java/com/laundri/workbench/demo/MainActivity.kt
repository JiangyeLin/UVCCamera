package com.laundri.workbench.demo

import android.Manifest
import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Button
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usbcameracommon.AbstractUVCCameraHandler
import com.serenegiant.usbcameracommon.UVCCameraHandler
import com.serenegiant.widget.CameraViewInterface
import java.io.File

class MainActivity : AppCompatActivity(), USBMonitor.OnDeviceConnectListener {

    private val TAG = "MainActivity"

    /**
     * set true if you want to record movie using MediaSurfaceEncoder
     * (writing frame data into Surface camera from MediaCodec
     * by almost same way as USBCameratest2)
     * set false if you want to record movie using MediaVideoEncoder
     */
    private val USE_SURFACE_ENCODER = false

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * [UVCCamera.setPreviewSize] throw exception
     */
    private val PREVIEW_WIDTH = 1280

    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * [UVCCamera.setPreviewSize] throw exception
     */
    private val PREVIEW_HEIGHT = 960

    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * [UVCCamera.setPreviewSize] throw exception
     * 0:YUYV, other:MJPEG
     */
    private val PREVIEW_MODE = 1

    /**
     * for accessing USB
     */
    private var mUSBMonitor: USBMonitor? = null

    /**
     * Handler to execute camera related methods sequentially on private thread
     */
    private var mCameraHandler: UVCCameraHandler? = null

    /**
     * for camera preview display
     */
    private var mUVCCameraView: CameraViewInterface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnCapture).setOnClickListener {
            //拍照
            mCameraHandler?.run {
                if (this.isOpened) {
                    Log.d(TAG, "onCreate: 拍照")
                    val path = "$externalCacheDir${File.separator}${System.currentTimeMillis()}.jpg"
                    //val file = File(externalCacheDir, "/images/${System.currentTimeMillis()}")
                    this.captureStill(
                        path, object : AbstractUVCCameraHandler.OnCaptureListener {
                            override fun onCaptureResult(picPath: String?) {
                                Log.d(TAG, "照片路径: $picPath")
                            }
                        })
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        }

        /*PermissionUtil.requestPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            object : PermissionUtil.OnPermissionCallback {
                override fun onGranted() {
                    Log.d(TAG, "onGranted: 权限申请成功")
                }

                override fun onDenied() {
                    Log.d(TAG, "onDenied: 权限申请失败")
                }
            })*/

        initCamera()
    }

    private fun initCamera() {
        val view = findViewById<View>(R.id.camera_view)
        mUVCCameraView = view as CameraViewInterface
        mUVCCameraView?.aspectRatio = (PREVIEW_WIDTH / PREVIEW_HEIGHT.toDouble())

        mUSBMonitor = USBMonitor(this, this)
        mCameraHandler = UVCCameraHandler.createHandler(
            this,
            mUVCCameraView,
            1,
            PREVIEW_WIDTH,
            PREVIEW_HEIGHT,
            PREVIEW_MODE
        )
    }

    override fun onStart() {
        super.onStart()
        mUSBMonitor?.register()
    }

    override fun onResume() {
        super.onResume()
        //mUVCCameraView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mUVCCameraView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mCameraHandler?.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraHandler?.release()
        mUSBMonitor?.destroy()
    }

    override fun onAttach(p0: UsbDevice?) {
        Log.d(TAG, "onAttach: 检测到设备回调")

        if (p0?.getInterface(0)?.interfaceClass == 14) {
            Log.d(TAG, "onAttach: 申请权限")
            mUSBMonitor?.requestPermission(p0)
        }
    }

    override fun onDettach(p0: UsbDevice?) {
        Log.d(TAG, "onDettach: 断开回调")
    }

    override fun onConnect(
        device: UsbDevice?,
        ctrlBlock: USBMonitor.UsbControlBlock?,
        createNew: Boolean
    ) {
        Log.d(TAG, "onConnect: 连接")
        mCameraHandler?.open(ctrlBlock)

        mUVCCameraView?.run {
            val surfaceTexture = this.surfaceTexture
            mCameraHandler?.startPreview(Surface(surfaceTexture))
        }
    }

    override fun onDisconnect(p0: UsbDevice?, p1: USBMonitor.UsbControlBlock?) {
        Log.d(TAG, "onDisconnect: 断开连接")
    }

    override fun onCancel(p0: UsbDevice?) {
        Log.d(TAG, "onCancel: ")
    }

}