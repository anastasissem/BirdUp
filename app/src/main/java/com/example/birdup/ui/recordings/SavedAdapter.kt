package com.example.birdup.ui.recordings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.birdup.MainActivity
import com.example.birdup.R
import com.example.birdup.model.Recording
import com.example.birdup.utils.FileHelper
import java.io.File


@SuppressLint("NotifyDataSetChanged")
class SavedAdapter(liveDataToObserve: LiveData<ArrayList<Recording>>, lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<ItemViewHolder>() {

        private lateinit var recordingList: ArrayList<Recording>
        private var player: MediaPlayer? = null

    // Storage Permissions
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )



    init {
            liveDataToObserve.observe(lifecycleOwner) {
                recordingList = it
                notifyDataSetChanged()
            }
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.saved_items_layout, parent, false)
            return ItemViewHolder(v)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val recording = recordingList[position]
            val context = holder.context ?: return

            //holder.storedIndex.text = recording.bird.id.toString()
            //holder.storedIndex.text = itemCount.toString()
            holder.storedName.text = recording.bird.name
            holder.storedLatin.text = recording.bird.latinName
            holder.storedPercent.text = recording.probability.toString()
            holder.storedDate.text = recording.dateTime


            fun verifyStoragePermissions(context: Context) {
                // Check if we have write permission
                val permission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // We don't have permission so prompt the user
                    requestPermissions(
                        context as Activity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                    )
                }
            }


            //YOKO
            holder.deleteButton.setOnClickListener {
                Log.d("REC", "ADD DELETE METHOD")
                Log.d("POSITION", position.toString())
                // use this instead of removeAt(position) to avoid OutofBounds error
                recordingList.remove(recording)
                //recordingList.removeAt(position)
                notifyItemRemoved(position)
                Log.d("``````````````````````````REMOVING RECORDING", recording.path)
                FileHelper(context).deleteRecording(recording.path)
                // add dialog delete window

            }

            holder.shareButton.setOnClickListener {

                verifyStoragePermissions(context)
                //TODO MAYBE I NEED JUST THE FILENAME, NOT ABSOLUTE PATH

                try{
                    val requestFile = File( FileHelper.getFilesDirPath( context, recording.path+".3gp"))
                    val uri = FileProvider.getUriForFile( context, "com.example.birdup.provider", requestFile)

                    val sharingIntent = Intent(Intent.ACTION_SEND).apply {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                    }

//                    val resInfoList: List<ResolveInfo> = context.packageManager.queryIntentContentProviders(
//                        sharingIntent,
//                        PackageManager.MATCH_DEFAULT_ONLY
//                        )
//                    for (resolveInfo in resInfoList) {
//                        val packageName = resolveInfo.activityInfo.packageName
//                        context.grantUriPermission(
//                            packageName,
//                            uri,
//                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
//                        )
//                    }

                    context.startActivity(Intent.createChooser(sharingIntent, "Share BirdUp file using"))
                } catch (e: ActivityNotFoundException){
                    Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
                }
            }

            holder.playButton.setOnClickListener {
                Log.d("REC", "PLAY RECORDING")
                player = MediaPlayer().apply {
                    try {
                        setDataSource(recording.path+".3gp")
                        prepare()
                        start()
                        //disable playback if already playing
                        holder.playButton.isEnabled = false
                        Log.d("MEDIA PLAYER", "PLAY ON")
                        val icon = ContextCompat.getDrawable(
                            context,
                            R.drawable.pause_saved
                        )
                        holder.playButton.setImageDrawable(icon)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    setOnCompletionListener {
                        val icon = ContextCompat.getDrawable(context, R.drawable.play_saved)
                        holder.playButton.setImageDrawable(icon)
                        reset()
                        //enable playback for future clicks
                        holder.playButton.isEnabled = true
                        Log.d("MEDIA PLAYER", "PLAY END")
                    }
                }
            }

        }

        override fun getItemCount(): Int {
            return recordingList.size
        }
    }

class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
    val context: Context? = itemView.context

    var deleteButton: ImageButton
    var playButton: ImageButton
    var shareButton: ImageButton
    //val storedIndex: TextView
    val storedName: TextView
    val storedLatin: TextView
    val storedPercent: TextView
    val storedDate: TextView


    init {
        super.itemView
        //storedIndex = itemView.findViewById(R.id.saved_index)
        storedName = itemView.findViewById(R.id.saved_name)
        storedLatin  =itemView.findViewById(R.id.saved_latin)
        storedPercent = itemView.findViewById(R.id.saved_confidence)
        storedDate = itemView.findViewById(R.id.saved_datetime)
        deleteButton = itemView.findViewById(R.id.saved_trashButton)
        playButton = itemView.findViewById(R.id.saved_playButton)
        shareButton = itemView.findViewById(R.id.saved_shareButton)
    }
}