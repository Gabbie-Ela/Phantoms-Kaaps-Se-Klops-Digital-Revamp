package com.example.phantoms.presentation.activity

import android.content.Intent
import android.graphics.Typeface // NEW
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.local.room.Card.CardEntity
import com.example.phantoms.data.local.room.Card.CardViewModel
import com.example.phantoms.data.local.room.CartViewModel
import com.example.phantoms.data.local.room.ProductEntity
import com.example.phantoms.data.model.Address
import com.example.phantoms.presentation.adapter.CarDItemClickAdapter
import com.example.phantoms.presentation.adapter.CardAdapter
import com.example.phantoms.utils.CardType
import com.example.phantoms.utils.CardValidator
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import android.app.NotificationChannel
import android.app.NotificationManager

class PaymentMethodActivity : AppCompatActivity(), CarDItemClickAdapter {

    private lateinit var cardRec: RecyclerView
    private lateinit var cardAdapter: CardAdapter
    private lateinit var cardViewModel: CardViewModel
    private lateinit var cartVm: CartViewModel

    private var shippingAddress: Address? = null
    private var selectedCard: CardEntity? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private val ORDER_CHANNEL_ID = "orders_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_method)

        // Read address
        shippingAddress = intent.getParcelableExtra("shipping_address")
        val addressSummary = findViewById<TextView>(R.id.shippingAddressSummaryTv)

        // Make it receipt-like
        addressSummary?.typeface = Typeface.MONOSPACE // NEW: mono for clean columns
        addressSummary?.setLineSpacing(4f, 1.0f)     // NEW: a little extra spacing

        cardRec = findViewById(R.id.cardRecView_paymentMethodPage)
        val backIv = findViewById<ImageView>(R.id.backIv_PaymentMethodsPage)
        val addFab = findViewById<FloatingActionButton>(R.id.addCard_PaymentMethodPage)
        val payBtn = findViewById<Button>(R.id.payBtn)

        cardViewModel = ViewModelProvider(this)[CardViewModel::class.java]
        cartVm = ViewModelProvider(this)[CartViewModel::class.java]

        createOrderChannel()

        cardRec.layoutManager = LinearLayoutManager(this)
        cardAdapter = CardAdapter(this, this)
        cardRec.adapter = cardAdapter

        // Cards list
        cardViewModel.allCards.observe(this, Observer { list ->
            val safe = list ?: emptyList()
            cardAdapter.updateList(safe)
            if (selectedCard == null && safe.isNotEmpty()) onCardChosen(safe.first())
        })

        // 🎟️ Receipt builder: observe cart and render into the SAME TextView
        cartVm.allproducts.observe(this) { list ->
            val items = list ?: emptyList()
            addressSummary?.text = buildReceiptBlock(shippingAddress, items)
        }

        backIv.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        addFab.setOnClickListener { showAddOrEditCardSheet(modeEdit = false, existing = null) }

        payBtn?.setOnClickListener {
            val addr = shippingAddress
            val card = selectedCard
            if (addr == null) {
                Toast.makeText(this, "Please select a shipping address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (card == null) {
                Toast.makeText(this, "Please select a payment card", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val uid = auth.uid ?: run {
                Toast.makeText(this, "You must be signed in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // If you don’t have a synchronous getter, read the latest value from LiveData:
                val items = cartVm.allproducts.value ?: emptyList()

                val order = hashMapOf(
                    "userId" to uid,
                    "status" to "placed",
                    "createdAt" to Timestamp.now(),
                    "shipping" to mapOf(
                        "fullName" to addr.fullName,
                        "line1" to addr.line1,
                        "line2" to addr.line2,
                        "city" to addr.city,
                        "region" to addr.region,
                        "postalCode" to addr.postalCode,
                        "country" to addr.country,
                        "phone" to addr.phone
                    ),
                    "payment" to mapOf(
                        "brand" to card.brandC,
                        "last4" to card.number.takeLast(4),
                        "exp" to card.exp
                    ),
                    "items" to items.map {
                        mapOf(
                            "productId" to it.id,
                            "name" to it.name,
                            "price" to it.price,
                            "quantity" to (it.qua ?: 1)
                        )
                    },
                    "subtotal" to items.sumOf { (it.price ?: 0) * (it.qua ?: 1) }
                )

                db.collection("Users").document(uid)
                    .collection("orders")
                    .add(order)
                    .addOnSuccessListener { ref ->
                        // Clear local cart (if you don’t have a DAO method yet, remove one by one)
                        cartVm.allproducts.value?.forEach { p ->
                            cartVm.deleteCart(p)
                        }

                        showOrderPlacedNotification(ref.id)
                        Toast.makeText(this@PaymentMethodActivity, "Order placed!", Toast.LENGTH_SHORT).show()

                        val i = Intent(
                            this@PaymentMethodActivity,
                            HomeActivity::class.java
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("open_tab", "home")
                        }
                        startActivity(i)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@PaymentMethodActivity, e.message ?: "Failed to place order.", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    // Build a neat, mono-spaced “receipt” block inside the TextView
    private fun buildReceiptBlock(addr: Address?, items: List<ProductEntity>): String {
        val sb = StringBuilder()

        // Address block
        if (addr != null) {
            sb.appendLine(addr.fullName)
            sb.appendLine("${addr.line1} ${addr.line2.orEmpty()}".trim())
            sb.appendLine("${addr.city}, ${addr.region} ${addr.postalCode}")
            sb.appendLine(addr.country)
        } else {
            sb.appendLine("No address selected")
        }

        sb.appendLine()
        sb.appendLine("ORDER SUMMARY")
        sb.appendLine("─────────────")

        // Items
        if (items.isEmpty()) {
            sb.appendLine("No items in cart")
        } else {
            items.forEach {
                val name = it.name ?: "Item"
                val qty = it.qua ?: 1
                val price = it.price ?: 0
                val lineTotal = price * qty
                // Keep it simple: Name xQTY  R<lineTotal>
                sb.appendLine("• $name x$qty  R$lineTotal")
            }
        }

        // Subtotal
        val subtotal = items.sumOf { (it.price ?: 0) * (it.qua ?: 1) }
        sb.appendLine()
        sb.appendLine("Subtotal:      R$subtotal")

        // If you add shipping/tax later, append similarly:
        // sb.appendLine("Shipping:      R$shipping")
        // sb.appendLine("Tax:           R$tax")
        // sb.appendLine("─────────────")
        // sb.appendLine("Total:         R${subtotal + shipping + tax}")

        return sb.toString()
    }

    // Adapter selects card
    fun onCardChosen(cardEntity: CardEntity) { selectedCard = cardEntity }

    private fun showAddOrEditCardSheet(modeEdit: Boolean, existing: CardEntity?) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val sheet = LayoutInflater.from(this).inflate(R.layout.card_add_bottom_sheet, null, false)

        val titleTv = sheet.findViewById<TextView>(R.id.title_cardAddBottomSheet)
        val nameEt = sheet.findViewById<EditText>(R.id.nameEt_cardAddBottomSheet)
        val numberEt = sheet.findViewById<EditText>(R.id.cardNumber_cardAddBottomSheet)
        val expEt = sheet.findViewById<EditText>(R.id.exp_cardAddBottomSheet)
        val cvvEt = sheet.findViewById<EditText>(R.id.cvv_cardAddBottomSheet)
        val actionBtn = sheet.findViewById<Button>(R.id.addCardBtn_cardAddBottomSheet)

        titleTv.text = if (modeEdit) "Edit card" else "Add card"
        actionBtn.text = if (modeEdit) "Save changes" else "Add card"
        nameEt.setText(existing?.nameCH ?: "")
        numberEt.setText(existing?.number ?: "")
        expEt.setText(existing?.exp ?: "")
        cvvEt.setText(existing?.cvv ?: "")

        actionBtn.setOnClickListener {
            val holder = nameEt.text.toString().trim()
            val number = numberEt.text.toString().trim().replace(" ", "")
            val exp = expEt.text.toString().trim()
            val cvv = cvvEt.text.toString().trim()

            if (holder.isEmpty() || number.isEmpty() || exp.isEmpty() || cvv.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val digitsOnly = number.all { ch -> ch.isDigit() }
            val passesLuhn = if (digitsOnly && number.isNotEmpty()) {
                runCatching { CardValidator.isValid(number.toLong()) }.getOrDefault(false)
            } else false

            if (!passesLuhn) {
                Toast.makeText(this, "Enter a valid card number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val brand = CardType.detect(number).toString()

            if (modeEdit && existing != null) {
                existing.nameCH = holder
                existing.number = number
                existing.exp = exp
                existing.cvv = cvv
                existing.brandC = brand
                cardViewModel.updateCart(existing)
                Toast.makeText(this, "Card updated", Toast.LENGTH_SHORT).show()
            } else {
                val entity = CardEntity(holder, number, exp, cvv, brand)
                cardViewModel.insert(entity)
                onCardChosen(entity)
                Toast.makeText(this, "New card added", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.setContentView(sheet as android.view.View)
        dialog.show()
    }

    private fun createOrderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                ORDER_CHANNEL_ID,
                "Orders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Order status updates" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun showOrderPlacedNotification(orderId: String) {
        val notif = NotificationCompat.Builder(this, ORDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bag)
            .setContentTitle("Order placed")
            .setContentText("Your order #$orderId was placed successfully.")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(1001, notif)
    }

    override fun onItemDeleteClick(cardEntity: CardEntity) {
        cardViewModel.deleteCart(cardEntity)
        if (selectedCard?.id == cardEntity.id) selectedCard = null
        Toast.makeText(this, "Card removed", Toast.LENGTH_SHORT).show()
    }

    override fun onItemUpdateClick(cardEntity: CardEntity) {
        showAddOrEditCardSheet(modeEdit = true, existing = cardEntity)
    }
}
