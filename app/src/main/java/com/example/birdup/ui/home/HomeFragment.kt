package com.example.birdup.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.ffmpegkit.FFmpegKit
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.birdup.R
import com.example.birdup.databinding.FragmentHomeBinding
import com.example.birdup.ml.FinalizedTest
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileFilter
import java.io.IOException

private const val LOG_TAG = "AudioRecordTest"

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    //RecyclerAdapter Vars
//    private var titlesList = mutableListOf<String>()
//    private var detailsList = mutableListOf<String>()
//    private var percentsList = mutableListOf<String>()
//    private var imageList = mutableListOf<Int>()

    // LATEINIT FOR DIRECT IMPLEMENTATION
    private lateinit var titlesList: MutableList<String>
    private lateinit var detailsList: MutableList<String>
    private lateinit var percentsList: MutableList<String>
    private lateinit var imageList: MutableList<Int>

    // HomeFragment exclusive vars
    private var fileName: String? = null
    private var modelInput: String? = null
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    private var timeWhenStopped: Long = 0

    private var isRecording = false
    private var isPaused = false

    // POPUP WINDOW VARS
    private lateinit var savePopup: AlertDialog.Builder
    private lateinit var dialog: AlertDialog
    private lateinit var saveItem: Button
    private lateinit var discardItem: Button

    // Requesting permission to use device microphone
    private fun recordPermissionSetup() {
        val permission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.RECORD_AUDIO)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            permissionsResultCallback.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            //onRecord()
        }
    }

    // Requesting permission to write to device storage
    private fun writePermissionSetup(){
        val permission = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            permissionsResultCallback.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            //onRecord()
        }
    }

    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestPermission()){
        when (it) {
            true -> {
                //onRecord()
                println("Permission has been granted by user")
            }
            false -> {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        recordPermissionSetup()
        writePermissionSetup()
    }

    @SuppressLint("SdCardPath")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        titlesList = mutableListOf()
        detailsList = mutableListOf()
        percentsList = mutableListOf()
        imageList = mutableListOf()

        // instantiate RecyclerVIew
        binding.predictionsView.layoutManager = LinearLayoutManager(context)

        val meter: Chronometer = root.findViewById(R.id.chronometer)

        fun postToList(
            Names: MutableList<String>,
            Commons: MutableList<String>,
            Scores: MutableList<String>,
            Id: MutableList<Int>
        ) {
            for(i in 0..2){
                Log.d("NAME $i", Names[i])
                Log.d("COMMON $i", Commons[i])
                Log.d("SCORE $i", Scores[i])
                Log.d("ID $i", Id[i].toString())
            }
        }

        // DISPLAY PREDICTION RESULTS
        val predictionList: RecyclerView = root.findViewById(R.id.predictionsView)
        predictionList.visibility = View.INVISIBLE
//        postToList()
        fun timer() {

            if (!isPaused) {
                meter.base = SystemClock.elapsedRealtime() + timeWhenStopped
                meter.start()
            } else {
                timeWhenStopped = meter.base - SystemClock.elapsedRealtime()
                meter.stop()
            }

            if (timeWhenStopped == 0L) {
                if (!isPaused){
                    Toast.makeText(requireContext(), "RECORDING", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (isPaused){
                    Toast.makeText(requireContext(), "PAUSED", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "RESUMED", Toast.LENGTH_SHORT).show()
                }
            }
        }

        /* START/STOP */
        val recordButton: ImageButton = root.findViewById(R.id.RecordButton)
        recordButton.setOnClickListener {
            //recordPermissionSetup()
            onRecord()
            timer()

            /* Change bird icon between start/pause */
            if (isPaused) {
                val icon = getDrawable(requireContext(), R.drawable.still_stork)
                recordButton.setImageDrawable(icon)
            } else {
                val icon = getDrawable(requireContext(), R.drawable.stork_medium)
                recordButton.setImageDrawable(icon)
            }
        }

        /* PLAYER FUNCTION*/
        val playButton: ImageButton = root.findViewById(R.id.playButton)
        playButton.setOnClickListener  {
            // Not interactive when recording is in progress
            if ((!isRecording && !isPaused) || (isRecording && isPaused)) {
                // move save permissions to save button
                //writePermissionSetup()
                if (!hasAudio()){
                    Toast.makeText(requireContext(), "Nothing to Play!", Toast.LENGTH_SHORT).show()
                } else {
                    releaseRecorder()
                    player = MediaPlayer().apply {
                        try {
                            setDataSource(fileName.toString())
                            prepare()
                            start()
                            //disable playback if already playing
                            playButton.isEnabled = false
                            Log.d(LOG_TAG, "PLAY ON")
                            val icon = getDrawable(requireContext(), R.drawable.pause_black_small)
                            playButton.setImageDrawable(icon)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        setOnCompletionListener {
                            val icon = getDrawable(requireContext(), R.drawable.play_black_small)
                            playButton.setImageDrawable(icon)
                            reset()
                            //enable playback for future clicks
                            playButton.isEnabled = true
                            Log.d(LOG_TAG, "PLAY END")
                        }
                    }
                }
            }
        }

        /* RESET & PREDICT */
        val analyzeButton: Button = root.findViewById(R.id.analyzeButton)
        analyzeButton.setOnClickListener {
            // Not interactive when recording is in progress
            if ((!isRecording && !isPaused) || (isRecording && isPaused)) {
                if (!hasAudio()){
                    Toast.makeText(requireContext(), "Nothing to Predict!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Log.d(LOG_TAG, "RESET")
                    /* reset meter after completing recording*/
                    finish()
                    Toast.makeText(requireContext(), "STOPPED", Toast.LENGTH_SHORT).show()
                    meter.base = SystemClock.elapsedRealtime()

                    // Init python
                    if (!Python.isStarted()){
                        Python.start(AndroidPlatform(requireContext()))
                    }

                    // Init scripts
                    val python = Python.getInstance()

                    //convert the .3gp to .wav for python scripts to use with AudioSegment
                    // TODO (REMOVE HARDCODED METHOD)
                    FFmpegKit.execute("-i /data/user/0/com.example.birdup/files/audiorecordtest.3gp " +
                            "/data/user/0/com.example.birdup/files/audiorecordtest.wav")

                    //get input audio path
                    for (file in requireContext().filesDir.listFiles()!!)
                        if (file.name.endsWith("wav"))
                            modelInput = file.toString()

                    Log.d("INPUT", modelInput.toString())
                    Log.d("FILE", fileName.toString())

                    //create new folder to store chunks and images separately from audio
                    val sampleDir = File(requireContext().filesDir?.path+"/samples")
                    if (!sampleDir.isDirectory)
                        Log.d("FOLDER CREATED", sampleDir.toString())
                        sampleDir.mkdirs()

                    // Split audio in 5sec chunks, remove noisy/empty ones
                    val splitFile = python.getModule("split_wav")
                    splitFile.callAttr("split", modelInput!!.toString(), sampleDir.toString())

                    // CHECK SAMPLEDIR CONTENTS AFTER SPLIT
                    for (f in sampleDir.listFiles()!!) {
                        Log.d("DIRECTORY AFTER SPLIT", f.absolutePath)
                        //f.delete()
                    }

                    // Convert chunks into spectrograms with STFT
                    val preprocessFile = python.getModule("preprocessing")
                    preprocessFile.callAttr("make_specs", sampleDir.toString())

                    // CHECK SAMPLEDIR CONTENTS AFTER PREPROCESSING
                    for (f in sampleDir.listFiles()!!) {
                        Log.d("DIRECTORY AFTER PREPROCESSING", f.absolutePath)
                        //f.delete()
                    }

                    //Resize image dims, compress, convert to lighter .jpg format
                    val convertPng = python.getModule("png2jpg")
                    convertPng.callAttr("compress", sampleDir.toString())

                    // CHECK SAMPLEDIR CONTENTS AFTER COMPRESSING
                    for (f in sampleDir.listFiles()!!) {
                        Log.d("DIRECTORY AFTER COMPRESSING", f.absolutePath)
                        //f.delete()
                    }

                    //If sample directory is empty, let user know
                    if(sampleDir.listFiles()!!.isEmpty()) {
                        Toast.makeText(
                            requireContext(), "No valid audio remained. Please try again",
                            Toast.LENGTH_LONG).show()
                        //remove previous recording
                        for (f in requireContext().filesDir?.listFiles()!!) {
                            if(f.name.endsWith("wav") or f.name.endsWith("3gp")) {
                                Log.d(LOG_TAG, f.absolutePath)
                                f.delete()
                            }
                        }
                    }
                    // pass the valid chunks through the model to make predictions
                    else {
                        //LOAD MODEL
                        val birdModel = context?.let { it1 -> FinalizedTest.newInstance(it1) }

                        //LOAD LABELS
                        val labels = context?.let { it1 -> FileUtil.loadLabels(it1, "labels.txt") }

                        // INIT IMAGEPROCESSOR - NORMALIZING, RESIZING
                        val imageProcessor = ImageProcessor.Builder()
                            .add(NormalizeOp(0F, 1/255.0F))
                            .build()
                        for (sample in sampleDir.listFiles()!!){

                            val bitmap = BitmapFactory.decodeFile(sample.absolutePath)

                            //RESIZING WITHOUT IMAGEPROCESSOR
                            val resized: Bitmap = Bitmap.createScaledBitmap(bitmap, 224,
                                168, true)

                            val inputShape = intArrayOf(1, 168, 224, 3)
                            // Only FLOAT32 and UINT-8 supported
                            val inputFeature0 = TensorBuffer.createFixedSize(inputShape,
                            DataType.FLOAT32)

                            //// IMAGE PROCESSOR CODE
                            var tImage = TensorImage(DataType.FLOAT32)
                            tImage.load(resized)
                            tImage = imageProcessor.process(tImage)

                            try {
                                inputFeature0.loadBuffer(tImage.buffer)
                            } catch (e: java.lang.IllegalArgumentException) {
                                Log.e(LOG_TAG, "INPUT TO MODEL FAIL")
                                e.printStackTrace()
                            }

                            val outputs = birdModel?.process(inputFeature0)
                            val outputFeature0 = outputs?.outputFeature0AsTensorBuffer


                            var inputNan = 0
                            for (i in inputFeature0.floatArray){
                                if (i.isNaN()){
                                    inputNan++
                                }
                            }
                            var outputNan = 0
                            for (i in outputFeature0?.floatArray!!){
                                if (i.isNaN()){
                                    outputNan++
                                }
                            }
                            Log.d("Nan inputs", inputNan.toString())
                            Log.d("Nan outputs", outputNan.toString())

                            val max = getMax(outputFeature0.floatArray)

                            Log.d("MAX INDEX", max.toString())
                            Log.d("MAX LABEL 1", (labels?.get(max) ?: max).toString())
                            Log.d("LABEL 1 SCORE", outputFeature0.floatArray[max].toString())
                            Log.d("MAX LABEL 2", (labels?.get(max-1) ?: max-1).toString())
                            Log.d("LABEL 2 SCORE", outputFeature0.floatArray[max-1].toString())

                            // instantiateRecycler Adapter
                            binding.predictionsView.adapter = RecyclerAdapter(titlesList, detailsList, percentsList, imageList)

                            // POPULATE RECYCLERVIEW WITH TOP 3 PREDICTION OUTPUTS
                            for(i in 0..2){
                                titlesList.add((labels?.get(max-i) ?: max-i).toString())
                                detailsList.add("YO MAMA $i")
                                percentsList.add(outputFeature0.floatArray[max-i].toString())
                                imageList.add(R.mipmap.ic_launcher_round)
                            }
                            // SHOW RESULTS TO USER
                            predictionList.visibility = View.VISIBLE

                        }
                        // close model when predictions are completed
                        birdModel?.close()
                    }
                    for (f in sampleDir.listFiles()!!) {
                        Log.d("DIRECTORY AFTER PREPROCESSING", f.absolutePath)
                        f.delete()
                    }
                }
            }
        }

        /* DISCARD & RESET */
        val trashButton: ImageButton = root.findViewById((R.id.trashButton))
        trashButton.setOnClickListener {
            // Not interactive when recording is in progress
            if ((!isRecording && !isPaused) || (isRecording && isPaused)) {
                Log.d(LOG_TAG, "DISCARD")
                finish()
                meter.base = SystemClock.elapsedRealtime()
                if(hasAudio()){
                    for (f in requireContext().filesDir?.listFiles()!!) {
                        if(f.name.endsWith("wav") or f.name.endsWith("3gp")) {
                            Log.d(LOG_TAG, f.absolutePath)
                            f.delete()
                        }
                    }
                    // REMOVE PREDICTIONS FROM LIST/VIEW
                    predictionList.visibility = View.INVISIBLE
                    titlesList.clear()
                    detailsList.clear()
                    percentsList.clear()
                    imageList.clear()

                    Toast.makeText(requireContext(), "Trash emptied!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Log.d(LOG_TAG, "No Files.\n")
                    Toast.makeText(requireContext(), "Nothing in trash", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }


        return root
    }

    // DECLARE FUNCTIONS TO BE USED

    // Check for existing recordings in internal storage
    private fun hasAudio(): Boolean{
        val f: File = requireContext().filesDir
        val filter = FileFilter { f -> f.name.endsWith("3gp") or f.name.endsWith("wav") }
        val files = f.listFiles(filter) ?: throw IllegalArgumentException("non-null value expected")
        Log.d("3GP/WAV FILES", files.size.toString())
        return(files.isNotEmpty())
    }

    // Reset recorder
    private fun finish() {
        Log.d(LOG_TAG, "STOP")
        releaseRecorder()
        timeWhenStopped = 0
        isPaused = false
        isRecording = false
    }

    private fun getMax(arr: FloatArray) : Int{

        var index = 0
        var min = 0.0F

        for(i in 0..49){
            if(arr[i]>min){
                index = i
                min = arr[i]
                Log.d("getMax index", index.toString())
                Log.d("getMax value", min.toString())
            }
        }
        return index
    }

    /* RECORDER FUNCTIONS START*/
    private fun onRecord() {
        when{
            isPaused -> resumeRecording()
            isRecording -> pauseRecording()
            else -> startRecording()
        }
    }

    private fun releaseRecorder() {
        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    private fun startRecording() {
//        val simpleDateFormat = SimpleDateFormat("d MM yyyy", Locale.getDefault())
//        val date : String = simpleDateFormat.format(Date())
//        fileName = requireContext().filesDir?.path+"/TEST_$date.3gp"
        fileName = requireContext().filesDir?.path+"/audiorecordtest.3gp"
        File(fileName?:"").createNewFile()

        /* Branch for if MediaRecorder() is not deprecated */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            recorder = MediaRecorder(requireContext()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // FOR MP3,
                //Use AAC_ELD(optimized for higher quality) for medium/higher bitrate(1411kbps)
                // instead of HE_AAC(low bandwidth for livestreaming), for low bitrate(705kbps)
                //TODO("TRY LOWER SAMPLE RATES FOR TESTING")
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(fileName)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD)
                setAudioChannels(1)
                setAudioEncodingBitRate(16*22050)
                setAudioSamplingRate(22050)

                Log.d(LOG_TAG, "UPDATED")
                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "prepare() failed")
                    e.printStackTrace()
                }

                try {
                    Log.d(LOG_TAG, "START")
                    start()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "start() failed")
                    e.printStackTrace()
                }
            }
            isRecording = true
            isPaused = false
        }
        else {
            /* Branch for if MediaRecorder() is deprecated */
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // FOR MP3,
                //Use AAC_ELD(optimized for higher quality) for medium/higher bitrate(1411kbps)
                // instead of HE_AAC(low bandwidth for livestreaming), for low bitrate(705kbps)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(fileName)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD)
                //TODO("TRY LOWER SAMPLE RATES FOR TESTING")
                setAudioChannels(1)
                setAudioEncodingBitRate(16*22050)
                setAudioSamplingRate(22050)

                Log.d(LOG_TAG, "DEPRECATED")
                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "prepare() failed")
                    e.printStackTrace()
                }

                try {
                    Log.d(LOG_TAG, "START")
                    start()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "start() failed")
                    e.printStackTrace()
                }
            }
        }
        isRecording = true
        isPaused = false
    }

    private fun pauseRecording(){
        /* pause() requires Android 7 or higher(API 24) */
        Log.d(LOG_TAG, "PAUSE")
        try {
            recorder?.pause()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "pause() failed")
            e.printStackTrace()
        }
        isPaused = true
    }

    private fun resumeRecording(){
        /* resume() requires Android 7 or higher(API 24) */
        Log.d(LOG_TAG, "RESUME")
        try {
            recorder?.resume()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "resume() failed")
            e.printStackTrace()
        }
        isPaused = false
    }
    /* RECORDER FUNCTIONS END */

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
        player?.release()
        player = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // remove any recordings if the app is closed
        if(hasAudio()) {
            for (f in requireContext().filesDir?.listFiles()!!) {
                if (f.name.endsWith("wav") or f.name.endsWith("3gp")) {
                    Log.d(LOG_TAG, f.absolutePath)
                    f.delete()
                }
            }
        }
    }
}