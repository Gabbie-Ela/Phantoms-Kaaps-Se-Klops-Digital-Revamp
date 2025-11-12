package com.example.phantoms.presentation.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.phantoms.R
import com.example.phantoms.data.local.room.Card.CardViewModel
import com.example.phantoms.data.model.User
import com.example.phantoms.presentation.activity.PaymentMethodActivity
import com.example.phantoms.presentation.activity.SettingsActivity
import com.example.phantoms.presentation.activity.ShipingAddressActivity
import com.example.phantoms.presentation.activity.SplashScreenActivity
import com.example.phantoms.utils.FirebaseUtils.storageReference
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException
import java.util.UUID

class ProfileFragment : Fragment() {

    private lateinit var animationView: LottieAnimationView
    private lateinit var profileImageIv: CircleImageView
    private lateinit var uploadBtn: Button
    private lateinit var nameTv: TextView
    private lateinit var emailTv: TextView
    private lateinit var cardsInfoTv: TextView
    // NEW: Orders info TextView
    private lateinit var ordersInfoTv: TextView

    private lateinit var row2: LinearLayout
    private lateinit var row3: LinearLayout
    private lateinit var row4: LinearLayout

    private val PICK_IMAGE_REQUEST = 71
    private var filePath: Uri? = null

    private lateinit var cardViewModel: CardViewModel
    private val auth = FirebaseAuth.getInstance()
    private val users = Firebase.firestore.collection("Users")

    private var snapReg: ListenerRegistration? = null
    // NEW: orders snapshot registration
    private var ordersReg: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImageIv = v.findViewById(R.id.profileImage_profileFrag)
        uploadBtn = v.findViewById(R.id.uploadImage_profileFrag)
        nameTv = v.findViewById(R.id.profileName_profileFrag)
        emailTv = v.findViewById(R.id.profileEmail_profileFrag)
        animationView = v.findViewById(R.id.animationView)
        row2 = v.findViewById(R.id.linearLayout2)
        row3 = v.findViewById(R.id.linearLayout3)
        row4 = v.findViewById(R.id.linearLayout4)

        val settingsCard = v.findViewById<CardView>(R.id.settingCd_profileFrag)
        val shippingCard = v.findViewById<CardView>(R.id.shippingAddressCard_ProfilePage)
        val paymentCard = v.findViewById<CardView>(R.id.paymentMethod_ProfilePage)
        val logoutCard = v.findViewById<CardView>(R.id.logoutCard_ProfilePage)
        cardsInfoTv = v.findViewById(R.id.cardsNumber_profileFrag)
        // NEW: bind orders info label (exists in your XML as @id/ordersInfo_profileFrag)
        ordersInfoTv = v.findViewById(R.id.ordersInfo_profileFrag)

        logoutCard.setOnClickListener { confirmAndLogout() }

        cardViewModel = ViewModelProvider(this)[CardViewModel::class.java]
        cardViewModel.allCards.observe(viewLifecycleOwner) {
            val count = it.size
            cardsInfoTv.text = if (count == 0) "You have no cards." else "You have $count card${if (count == 1) "" else "s"}."
        }

        // Default state: show loader, hide content areas and upload button
        hideLayout()
        uploadBtn.visible(false)

        settingsCard.setOnClickListener { startActivity(Intent(context, SettingsActivity::class.java)) }
        shippingCard.setOnClickListener { startActivity(Intent(context, ShipingAddressActivity::class.java)) }
        paymentCard.setOnClickListener { startActivity(Intent(context, PaymentMethodActivity::class.java)) }

        profileImageIv.setOnClickListener { showPhotoPickerMenu(it) }
        uploadBtn.setOnClickListener { uploadImage() }

        return v
    }

    override fun onStart() {
        super.onStart()

        // Guard: if not authenticated, prompt and leave
        val user = auth.currentUser
        if (user == null) {
            // Make sure we’re not stuck on the loader
            animationView.pauseAnimation()
            animationView.visible(false)
            row2.visible(false); row3.visible(false); row4.visible(false)

            AlertDialog.Builder(requireContext())
                .setTitle("Sign in required")
                .setMessage("Please sign in to view your profile.")
                .setCancelable(false)
                .setPositiveButton("Go to sign in") { _, _ -> goToAuthGate() }
                .show()
            return
        }

        startUserSnapshot()
        // NEW: start orders snapshot
        startOrdersSnapshot()
    }

    override fun onStop() {
        snapReg?.remove()
        snapReg = null
        // NEW: clean up orders snapshot
        ordersReg?.remove()
        ordersReg = null
        super.onStop()
    }

    private fun startUserSnapshot() {
        // Show loader every time we (re)attach the listener
        hideLayout()

        val uid = auth.uid
        if (uid == null) {
            // Shouldn’t happen if onStart() check passes, but be safe
            Toast.makeText(requireContext(), "Not signed in.", Toast.LENGTH_SHORT).show()
            goToAuthGate()
            return
        }

        // Clean any previous listener to avoid duplicate updates
        snapReg?.remove()
        snapReg = users.document(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                // Stop loader and keep a friendly UI
                animationView.pauseAnimation()
                animationView.visible(false)
                row2.visible(false); row3.visible(false); row4.visible(false)
                Toast.makeText(requireContext(), "Unable to load profile. Check your connection or permissions.", Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }

            if (snap == null || !snap.exists()) {
                // Create a skeleton document so future loads work
                users.document(uid).set(User(userUid = uid), SetOptions.merge())
                // Keep loader briefly then we’ll get a second callback with data
                return@addSnapshotListener
            }

            val user = snap.toObject(User::class.java)

            // Defensive UI defaults
            nameTv.text = user?.userName?.takeIf { it.isNotBlank() } ?: "Your name"
            emailTv.text = user?.userEmail?.takeIf { it.isNotBlank() } ?: auth.currentUser?.email ?: "Email not set"

            val imageUrl = user?.userImage
            Glide.with(this@ProfileFragment)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(profileImageIv)

            // Show content
            showLayout()
        }
    }

    // NEW: Live orders counter
    private fun startOrdersSnapshot() {
        val uid = auth.uid ?: return
        ordersReg?.remove()
        ordersReg = Firebase.firestore.collection("Users")
            .document(uid)
            .collection("orders")
            .addSnapshotListener { qs, err ->
                if (err != null) {
                    ordersInfoTv.text = "unable to fetch orders"
                    return@addSnapshotListener
                }
                val count = qs?.size() ?: 0
                ordersInfoTv.text = if (count == 0) {
                    "You have no orders"
                } else {
                    "You have $count order${if (count == 1) "" else "s"}"
                }
            }
    }

    private fun confirmAndLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Log out") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        snapReg?.remove()
        snapReg = null
        ordersReg?.remove()
        ordersReg = null
        auth.signOut()
        goToAuthGate()
    }

    private fun goToAuthGate() {
        val i = Intent(requireContext(), SplashScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(i)
        requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showPhotoPickerMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.profile_photo_storage, popup.menu)
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.galleryMenu -> launchGallery()
                R.id.cameraMenu  -> launchGallery() // TODO: implement camera flow
            }
            true
        }
        popup.show()
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    private fun uploadImage() {
        val uri = filePath ?: run {
            Toast.makeText(context, "Please choose an image.", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.uid
        if (uid == null) {
            Toast.makeText(context, "Sign in again to upload a photo.", Toast.LENGTH_SHORT).show()
            goToAuthGate()
            return
        }

        // Optional: a little feedback
        uploadBtn.isEnabled = false
        uploadBtn.text = getString(R.string.uploading_ellipsis)

        val ref = storageReference.child("profile_Image/${UUID.randomUUID()}")
        val uploadTask = ref.putFile(uri)
        uploadTask
            .continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) throw task.exception ?: RuntimeException("Upload failed")
                ref.downloadUrl
            })
            .addOnCompleteListener { t ->
                uploadBtn.isEnabled = true
                uploadBtn.text = getString(R.string.profile_upload)
                if (t.isSuccessful) {
                    val download = t.result.toString()
                    users.document(uid).set(mapOf("userImage" to download), SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(context, "Photo updated", Toast.LENGTH_SHORT).show()
                            uploadBtn.visible(false)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Saved to storage but failed to update profile.", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                uploadBtn.isEnabled = true
                uploadBtn.text = getString(R.string.profile_upload)
                Toast.makeText(context, it.message ?: "Upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val chosen = data?.data ?: return
            filePath = chosen
            try {
                val bmp = MediaStore.Images.Media.getBitmap(context?.contentResolver, chosen)
                profileImageIv.setImageBitmap(bmp)
                uploadBtn.visible(true)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Unable to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideLayout() {
        animationView.playAnimation()
        animationView.loop(true)
        row2.visible(false); row3.visible(false); row4.visible(false)
        animationView.visible(true)
    }

    private fun showLayout() {
        animationView.pauseAnimation()
        animationView.visible(false)
        row2.visible(true); row3.visible(true); row4.visible(true)
    }

    private fun View.visible(show: Boolean) {
        visibility = if (show) View.VISIBLE else View.GONE
    }
}
