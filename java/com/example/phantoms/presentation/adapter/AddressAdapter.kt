package com.example.phantoms.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.model.Address

class AddressAdapter(
    private val onEdit: (Address) -> Unit,
    private val onRemove: (Address) -> Unit,
    private val onSetDefault: (Address, Boolean) -> Unit,
    private val onSelect: (Address) -> Unit                 // NEW
) : RecyclerView.Adapter<AddressAdapter.H>() {

    private val data = mutableListOf<Address>()
    private var selectedId: String? = null                  // NEW

    fun submit(list: List<Address>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    /** Call this from the Activity to visually mark a selected address */
    fun setSelected(addr: Address?) {                       // NEW
        selectedId = addr?.id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): H {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_address, parent, false)
        return H(v)
    }

    override fun onBindViewHolder(h: H, pos: Int) {
        val a = data[pos]
        h.name.text = a.fullName
        h.line.text = listOf(a.line1, a.line2.takeIf { it.isNotBlank() })
            .filterNotNull()
            .joinToString(", ")
        h.city.text = "${a.city}, ${a.region}, ${a.postalCode}"
        h.phone.text = a.phone

        // default checkbox
        h.defaultCb.setOnCheckedChangeListener(null)
        h.defaultCb.isChecked = a.isDefault
        h.defaultCb.setOnCheckedChangeListener { _, checked -> onSetDefault(a, checked) }

        // actions
        h.edit.setOnClickListener { onEdit(a) }
        h.remove.setOnClickListener { onRemove(a) }

        // selection
        h.itemView.setOnClickListener {
            selectedId = a.id
            notifyDataSetChanged()
            onSelect(a)
        }

        // simple visual cue (no extra drawable needed)
        val selected = a.id == selectedId
        h.itemView.alpha = if (selected) 1.0f else 0.92f
        // If you add a selector background later, you can use:
        // h.itemView.isSelected = selected
    }

    override fun getItemCount() = data.size

    class H(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.nameTv)
        val line: TextView = v.findViewById(R.id.lineTv)
        val city: TextView = v.findViewById(R.id.cityTv)
        val phone: TextView = v.findViewById(R.id.phoneTv)
        val edit: Button = v.findViewById(R.id.editBtn)
        val remove: Button = v.findViewById(R.id.removeBtn)
        val defaultCb: CheckBox = v.findViewById(R.id.defaultCb)
    }
}
