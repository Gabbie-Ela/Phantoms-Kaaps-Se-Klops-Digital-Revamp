package com.example.phantoms.presentation.activity

import android.app.Activity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.phantoms.R
import com.example.phantoms.data.model.Address
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddAddressActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private var editAddressId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_address)

        val title = findViewById<TextView>(R.id.titleTv)
        val fullNameEt = findViewById<EditText>(R.id.fullNameEt)
        val phoneEt = findViewById<EditText>(R.id.phoneEt)
        val line1Et = findViewById<EditText>(R.id.line1Et)
        val line2Et = findViewById<EditText>(R.id.line2Et)
        val cityEt = findViewById<EditText>(R.id.cityEt)
        val regionEt = findViewById<EditText>(R.id.regionEt)
        val postalEt = findViewById<EditText>(R.id.postalEt)
        val defaultCb = findViewById<CheckBox>(R.id.defaultCb)
        val saveBtn = findViewById<Button>(R.id.saveBtn)

        editAddressId = intent.getStringExtra("addressId")
        val uid = auth.uid ?: return finish()
        val col = db.collection("Users").document(uid).collection("addresses")

        if (editAddressId != null) {
            title.text = "Edit Address"
            col.document(editAddressId!!).get().addOnSuccessListener { snap ->
                val a = snap.toObject(Address::class.java) ?: return@addOnSuccessListener
                fullNameEt.setText(a.fullName)
                phoneEt.setText(a.phone)
                line1Et.setText(a.line1)
                line2Et.setText(a.line2)
                cityEt.setText(a.city)
                regionEt.setText(a.region)
                postalEt.setText(a.postalCode)
                defaultCb.isChecked = a.isDefault
            }
        } else {
            // If this is the FIRST address, pre-check "default"
            col.limit(1).get().addOnSuccessListener { qs ->
                if (qs.isEmpty) defaultCb.isChecked = true
            }
        }

        saveBtn.setOnClickListener {
            val fullName = fullNameEt.text.toString().trim()
            val phone = phoneEt.text.toString().trim()
            val line1 = line1Et.text.toString().trim()
            val line2 = line2Et.text.toString().trim()
            val city = cityEt.text.toString().trim()
            val region = regionEt.text.toString().trim()
            val postal = postalEt.text.toString().trim()
            val wantsDefault = defaultCb.isChecked

            if (fullName.isEmpty() || phone.isEmpty() || line1.isEmpty() ||
                city.isEmpty() || region.isEmpty() || postal.isEmpty()
            ) {
                Toast.makeText(this, "Please fill the required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val docRef = if (editAddressId != null) col.document(editAddressId!!) else col.document()
            val address = Address(
                id = docRef.id,
                userId = uid,
                fullName = fullName,
                phone = phone,
                line1 = line1,
                line2 = line2,
                city = city,
                region = region,
                postalCode = postal,
                country = "South Africa",
                isDefault = wantsDefault // may be overridden in batch if necessary
            )

            // Atomic default logic:
            // - If wantsDefault: unset all others, set this one true
            // - Else: if collection empty (first address), force true; otherwise save as-is
            col.get().addOnSuccessListener { qs ->
                val batch = db.batch()
                val isFirstAddress = qs.isEmpty

                if (wantsDefault || isFirstAddress) {
                    // Unset all others
                    qs.documents.forEach { d -> batch.update(d.reference, "isDefault", false) }
                    // Upsert this one with isDefault = true
                    val data = address.copy(isDefault = true)
                    batch.set(docRef, data, SetOptions.merge())
                } else {
                    // Normal save, keep isDefault as provided (usually false)
                    batch.set(docRef, address, SetOptions.merge())
                }

                batch.commit()
                    .addOnSuccessListener {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, e.message ?: "Save failed", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Save failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
