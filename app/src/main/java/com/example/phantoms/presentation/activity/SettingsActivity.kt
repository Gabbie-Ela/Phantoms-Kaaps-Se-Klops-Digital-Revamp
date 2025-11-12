package com.example.phantoms.presentation.activity

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.phantoms.R
import com.example.phantoms.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var nameEt: EditText
    private lateinit var emailEt: EditText
    private lateinit var saveBtn: Button
    private lateinit var backIv: ImageView

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val users = Firebase.firestore.collection("Users")

    private var snapReg: ListenerRegistration? = null
    private var nameJob: Job? = null
    private var emailJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        nameEt = findViewById(R.id.nameEt_SettingsPage)
        emailEt = findViewById(R.id.EmailEt_SettingsPage)
        saveBtn = findViewById(R.id.saveSetting_SettingsBtn)
        backIv = findViewById(R.id.backIv_ProfileFrag)

        backIv.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Optional manual save
        saveBtn.setOnClickListener { manualSave() }

        wireAutoSaves()
    }

    override fun onStart() {
        super.onStart()
        startUserSnapshot()
    }

    override fun onStop() {
        snapReg?.remove()
        snapReg = null
        super.onStop()
    }

    private fun startUserSnapshot() {
        val uid = auth.uid ?: return
        snapReg = users.document(uid).addSnapshotListener(this) { snap, err ->
            if (err != null) return@addSnapshotListener
            if (snap == null || !snap.exists()) {
                // Create a minimal doc so writes succeed
                val seed = User(
                    userUid = uid,
                    userName = auth.currentUser?.displayName ?: "",
                    userEmail = auth.currentUser?.email ?: ""
                )
                users.document(uid).set(seed, SetOptions.merge())
                return@addSnapshotListener
            }
            val user = snap.toObject(User::class.java) ?: return@addSnapshotListener

            // Avoid cursor jumps
            if (nameEt.text?.toString() != user.userName) nameEt.setText(user.userName)
            if (emailEt.text?.toString() != user.userEmail) emailEt.setText(user.userEmail)
        }
    }

    private fun wireAutoSaves() {
        val uid = auth.uid ?: return
        val doc = users.document(uid)

        nameEt.doOnTextChanged { text, _, _, _ ->
            nameJob?.cancel()
            nameJob = lifecycleScope.launch {
                delay(500) // debounce
                val value = text?.toString()?.trim().orEmpty()
                if (value.isNotEmpty()) {
                    doc.update("userName", value)
                }
            }
            saveBtn.visible(true) // still allow manual save
        }

        emailEt.doOnTextChanged { text, _, _, _ ->
            emailJob?.cancel()
            emailJob = lifecycleScope.launch {
                delay(600)
                val value = text?.toString()?.trim().orEmpty()
                if (value.isNotEmpty()) {
                    doc.update("userEmail", value)
                }
            }
            saveBtn.visible(true)
        }
    }

    private fun manualSave() {
        val uid = auth.uid ?: return
        val doc = users.document(uid)
        val name = nameEt.text?.toString()?.trim().orEmpty()
        val email = emailEt.text?.toString()?.trim().orEmpty()
        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required.", Toast.LENGTH_SHORT).show()
            return
        }
        doc.set(mapOf("userName" to name, "userEmail" to email), SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                saveBtn.visible(false)
            }
    }
}

private fun View.visible(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.GONE
}
