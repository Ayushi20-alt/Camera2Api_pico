package com.example.camerapicosoft

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import com.google.android.material.imageview.ShapeableImageView
import java.io.File

class MainActivity : AppCompatActivity() {

    lateinit var capReq : CaptureRequest.Builder
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    lateinit var imageReader: ImageReader
    private lateinit var button : ShapeableImageView
    private lateinit var captureRequestBuilder : CaptureRequest.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        get_permissions()

        textureView = findViewById(R.id.textureview)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
        captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.3048f)

        textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }

        }

        imageReader = ImageReader.newInstance(1080,1920,ImageFormat.JPEG,1)
        imageReader.setOnImageAvailableListener(object: ImageReader.OnImageAvailableListener{
            override fun onImageAvailable(reader: ImageReader?) {
                val image = reader?.acquireLatestImage()
                val buffer = image!!.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                 buffer.get(bytes)

                // Create a File object to store the image.
                val file = File(getExternalFilesDir(null), "image.jpg")
//                var opStream = FileOutputStream(file)
//                opStream.write(bytes)

                // Create a ContentValues object to store the image's metadata.
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "Image")
                values.put(MediaStore.Images.Media.DESCRIPTION, "A photo taken with the camera.")
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

                // Use the MediaStore.Images.Media.insertImage() method to insert the image into the gallery.
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                // Use the Intent.ACTION_MEDIA_SCANNER_SCAN_FILE intent to scan the gallery for new images.
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.setData(uri)
                sendBroadcast(intent)

//                opStream.close()
                // Close the ImageReader.
                    image.close()



//                var image = reader?.acquireLatestImage()
//                var buffer = image!!.planes[0].buffer
//                var bytes = ByteArray(buffer.remaining())
//                buffer.get(bytes)
//
//                var file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "img.jpeg")
//                var opStream = FileOutputStream(file)
//                opStream.write(bytes)
//                opStream.close()
//                image.close()
                Toast.makeText(this@MainActivity,"image clicked", Toast.LENGTH_LONG).show()

            }

        }, handler)

        button = findViewById(R.id.Capture)
        button.apply {
            setOnClickListener {
                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                capReq.addTarget(imageReader.surface)
                cameraCaptureSession.capture(capReq.build(),null,null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun open_camera() {
        // with the help of camera manager we are going to open the camera
        cameraManager.openCamera(cameraManager.cameraIdList[0], object: CameraDevice.StateCallback(){
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                // now we have the camera device we need to capture request
                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                // now we need to add a target to our capture request
                var surface = Surface(textureView.surfaceTexture)
                capReq.addTarget(surface)

                cameraCaptureSession.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        // Do something after the focus is locked.

                    }
                }, null)

                // now we have a capture request we have to attach it to the session
                cameraDevice.createCaptureSession(listOf(surface, imageReader.surface), object:
                    CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        cameraCaptureSession.setRepeatingRequest(capReq.build(),null,null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        TODO("Not yet implemented")
                    }

                },handler)


            }

            override fun onDisconnected(camera: CameraDevice) {

            }

            override fun onError(camera: CameraDevice, error: Int) {

            }

        }, handler)
    }

    // getting the permissions
    private fun get_permissions() {
        val permissionLst = mutableListOf<String>()

        if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)permissionLst.add(android.Manifest.permission.CAMERA)
        if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)permissionLst.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)permissionLst.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if(permissionLst.size > 0)
        {
            requestPermissions(permissionLst.toTypedArray(), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if(it != PackageManager.PERMISSION_GRANTED)
            {
                get_permissions()
            }
        }
    }

    // getting the camera

}