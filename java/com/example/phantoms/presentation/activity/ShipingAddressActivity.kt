package com.example.phantoms.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.model.Address
import com.example.phantoms.presentation.adapter.AddressAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ShipingAddressActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private lateinit var rec: RecyclerView
    private lateinit var adapter: AddressAdapter
    private val addresses = mutableListOf<Address>()

    private var selectedAddress: Address? = null

    private val addEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        loadAddresses()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shiping_address)

        rec = findViewById(R.id.addressRec)
        val fab = findViewById<FloatingActionButton>(R.id.addAddress_ShippingPage)
        val continueBtn = findViewById<MaterialButton>(R.id.continueBtn_ShippingPage)

        adapter = AddressAdapter(
            onEdit = { openEditor(it) },
            onRemove = { removeAddress(it) },
            onSetDefault = { addr, checked -> setDefault(addr, checked) },
            onSelect = { addr -> setSelected(addr) } // <-- add this callback in your adapter
        )
        rec.layoutManager = LinearLayoutManager(this)
        rec.adapter = adapter

        fab.setOnClickListener { openEditor(null) }

        continueBtn.setOnClickListener {
            val chosen = selectedAddress
            if (chosen == null) {
                Toast.makeText(this, "Please select an address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val i = Intent(this, PaymentMethodActivity::class.java)
            i.putExtra("shipping_address", chosen) // Address must be @Parcelize
            startActivity(i)
        }
    }

    override fun onStart() {
        super.onStart()
        loadAddresses()
    }

    private fun loadAddresses() {
        val uid = auth.uid ?: return
        db.collection("Users").document(uid)
            .collection("addresses")
            .orderBy("isDefault", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { qs ->
                addresses.clear()
                addresses.addAll(qs.documents.mapNotNull { it.toObject(Address::class.java) })
                adapter.submit(addresses)

                // Preselect default (or first)
                selectedAddress = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
                adapter.setSelected(selectedAddress) // implement to visually highlight
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Failed to load addresses", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setSelected(addr: Address) {
        selectedAddress = addr
        adapter.setSelected(addr) // update highlight in adapter
    }

    private fun openEditor(address: Address?) {
        val i = Intent(this, AddAddressActivity::class.java)
        if (address != null) i.putExtra("addressId", address.id)
        addEditLauncher.launch(i)
    }

    private fun removeAddress(address: Address) {
        val uid = auth.uid ?: return
        db.collection("Users").document(uid)
            .collection("addresses").document(address.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Address removed", Toast.LENGTH_SHORT).show()
                loadAddresses()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Delete failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setDefault(address: Address, checked: Boolean) {
        val uid = auth.uid ?: return
        val col = db.collection("Users").document(uid).collection("addresses")

        col.get().addOnSuccessListener { qs ->
            val batch = db.batch()
            qs.documents.forEach { d ->
                val isThis = d.id == address.id
                batch.update(d.reference, "isDefault", isThis && checked)
            }
            batch.commit()
                .addOnSuccessListener { loadAddresses() }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update default", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
