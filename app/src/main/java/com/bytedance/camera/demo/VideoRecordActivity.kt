package com.bytedance.camera.demo

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PictureCallback
import android.media.CamcorderProfile
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_take_picture.*
import kotlinx.android.synthetic.main.activity_video_record.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Stream

class VideoRecordActivity : AppCompatActivity() {
//    private var mTakePhoto: Button? = null
//    private var mRecordVideo: Button? = null
    private var mSwitch: Button? = null
    private var mTakeAndRecord: RadioButton? = null
    private var mSurfaceView: SurfaceView? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCamera: Camera? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var mIsRecording = false
    private var mCameraFacing = CameraInfo.CAMERA_FACING_BACK
    private var mImageView: ImageView? = null
    private var mEditText: EditText? = null
    private var mTextView: TextView? = null
    private var mVideoFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record)
        mSurfaceView = findViewById(R.id.surface_view)
       // mTakePhoto = findViewById(R.id.take_photo)
       // mRecordVideo = findViewById(R.id.record_video)
        mSwitch = findViewById(R.id.switch_button)
        mTakeAndRecord  = findViewById(R.id.record_take)
        mImageView = findViewById(R.id.last_pic)
        mEditText = findViewById(R.id.edit_text)
        mTextView = findViewById(R.id.text_view)
        startCamera()
      //  mTakePhoto?.setOnClickListener(View.OnClickListener { takePicture() })
      //  mRecordVideo?.setOnClickListener(View.OnClickListener { recordVideo() })
        mTakeAndRecord?.setOnClickListener(View.OnClickListener {
            if(mIsRecording) {
                recordVideo()
            } else {
                takePicture()
            }
           })
        mTakeAndRecord?.setOnLongClickListener(View.OnLongClickListener { recordVideo() })
        mSwitch?.setOnClickListener(View.OnClickListener { switchCamera() })
    }

    private fun switchCamera() {
        if(CameraInfo.CAMERA_FACING_BACK == mCameraFacing) {
            // TODO: 2021/12/1

            mCameraFacing = CameraInfo.CAMERA_FACING_FRONT
        } else {

            mCameraFacing = CameraInfo.CAMERA_FACING_BACK
        }

        mCamera!!.stopPreview()
        mCamera!!.release()
        mCamera = null

        mCamera = Camera.open(mCameraFacing)
        setCameraDisplayOrientation()
        mCamera!!.setPreviewDisplay(mSurfaceHolder)
        mCamera!!.startPreview()

    }


    private fun startCamera() {
        try {
            mCamera = Camera.open(mCameraFacing)
            setCameraDisplayOrientation()
        } catch (e: Exception) {
            // error
        }
        mSurfaceHolder = mSurfaceView!!.holder
        mSurfaceHolder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    /**
                     *   补充完整缺失代码 C1
                     */
                    mCamera!!.setPreviewDisplay(holder)
                    mCamera!!.startPreview()
                    loadLastPic()
                } catch (e: IOException) {
                    // error
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
                try {
                    mCamera!!.stopPreview()
                } catch (e: Exception) {
                    // error
                }
                try {
                    mCamera!!.setPreviewDisplay(holder)
                    mCamera!!.startPreview()
                } catch (e: Exception) {
                    //error
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mCamera!!.stopPreview()
                mCamera!!.release()
                mCamera = null
            }
        })
    }

    private fun setCameraDisplayOrientation() {
        val rotation = windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        val info = CameraInfo()
        Camera.getCameraInfo(mCameraFacing, info)

        var result = (info.orientation - degrees + 360) % 360
        if(mCameraFacing == CameraInfo.CAMERA_FACING_FRONT) {
            result = 360 - result
        }
        mCamera!!.setDisplayOrientation(result)
    }

    private fun takePicture() {
        mCamera!!.takePicture(null, null, PictureCallback { bytes, camera ->
            val pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, mEditText!!.text.toString()) ?: return@PictureCallback
            try {
                /**
                 *   补充完整缺失代码 C2
                 */
                val fos = FileOutputStream(pictureFile)
                fos.write(bytes)
                fos.close()
                showLastPic(pictureFile)
            } catch (e: FileNotFoundException) {
                //error
            } catch (e: IOException) {
                //error
            }
            mCamera!!.startPreview()
        })
    }

    private fun getOutputMediaFile(type: Int, name: String): File? {
        // Android/data/com.bytedance.camera.demo/files/Pictures
        val mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (!mediaStorageDir!!.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val mediaFile: File
        mediaFile = if (type == MEDIA_TYPE_IMAGE) {
            File(mediaStorageDir.path + File.separator + "IMG_" + name + ".jpg")
        } else if (type == MEDIA_TYPE_VIDEO) {
            File(mediaStorageDir.path + File.separator + "VID_" + name + ".mp4")
        } else {
            return null
        }
        return mediaFile
    }

    @SuppressLint("NewApi")
    private fun prepareVideoRecorder(): Boolean {
        mMediaRecorder = MediaRecorder()
        mCamera!!.unlock()
        mMediaRecorder!!.setCamera(mCamera)
        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        mMediaRecorder!!.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
        mVideoFile = getOutputMediaFile(MEDIA_TYPE_VIDEO, mEditText!!.text.toString())
        mMediaRecorder!!.setOutputFile(mVideoFile)

        mMediaRecorder!!.setPreviewDisplay(mSurfaceHolder!!.surface)
        try {
            mMediaRecorder!!.prepare()
        } catch (e: IllegalStateException) {
            releaseMediaRecorder()
            return false
        } catch (e: IOException) {
            releaseMediaRecorder()
            return false
        }
        return true
    }

    private fun recordVideo() :Boolean{
        if (mIsRecording) {
            mMediaRecorder!!.stop()
            releaseMediaRecorder()
            mCamera!!.lock()
            mIsRecording = false
            showLastPic(mVideoFile!!)
           // mRecordVideo!!.text = "Start Recording"
        } else {
            if (prepareVideoRecorder()) {
              //  mMediaRecorder!!.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO, mEditText!!.text.toString()).toString())
                mMediaRecorder!!.start()
                mIsRecording = true
             //   mRecordVideo!!.text = "Stop Recording"
            } else {
                releaseMediaRecorder()
            }
        }

        return true;
    }

    private fun loadLastPic() {
        var mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        var list = mediaStorageDir!!.listFiles()
        if(list.isEmpty()) {
            return
        } else {
            list.sortedBy { it.lastModified() }
            showLastPic(list[list.size - 1])
        }

    }


    @SuppressLint("NewApi")
    private fun showLastPic(file: File) {
        var matrix: Matrix = Matrix()
        var cameraInfo = CameraInfo()
        Camera.getCameraInfo(mCameraFacing, cameraInfo)
        matrix.setRotate(cameraInfo.orientation.toFloat())


        val targetWidth = mImageView!!.width
        val targetHeight = mImageView!!.height
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var mCurrentPhotoPath = file.absolutePath
        BitmapFactory.decodeFile(mCurrentPhotoPath, options)
        var photoH = options.outHeight
        var photoW = options.outWidth
        var inSampleSize = 1
        while(photoH > targetHeight || photoW > targetWidth) {
            photoH /= 2
            photoW /= 2
            inSampleSize *= 2
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        options.inPurgeable = true
        var bitmap: Bitmap? = null
        if(file!!.name.startsWith("IMG")) {
             bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, options)
        } else {
            var mmr = MediaMetadataRetriever()
            mmr.setDataSource(file.absolutePath)
             bitmap = mmr.getScaledFrameAtTime(-1,MediaMetadataRetriever.OPTION_CLOSEST, targetWidth, targetHeight)

        }

        bitmap = Bitmap.createBitmap(bitmap!!, 0,0,bitmap!!.width, bitmap!!.height, matrix ,true)
        mImageView!!.setImageBitmap(bitmap)
        mTextView!!.text = file.name
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaRecorder()
        releaseCamera()
    }

    private fun releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder!!.reset()
            mMediaRecorder!!.release()
            mMediaRecorder = null
            mCamera!!.lock()
        }
    }

    private fun releaseCamera() {
        if (mCamera != null) {
            mCamera!!.release()
            mCamera = null
        }

    }

    companion object {
        private const val MEDIA_TYPE_IMAGE = 1
        private const val MEDIA_TYPE_VIDEO = 2
    }
}