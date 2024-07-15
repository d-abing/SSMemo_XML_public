package com.aube.ssgmemo.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import com.aube.ssgmemo.SpinnerModel
import com.aube.ssgmemo.databinding.ItemCategorySpinnerBinding
import com.aube.ssgmemo.etc.MyApplication

class SpinnerAdapter(
    context: Context,
    @LayoutRes private val resId: Int,
    private val values: MutableList<SpinnerModel>,
) : ArrayAdapter<SpinnerModel>(context, resId, values) {

    override fun getCount() = values.size
    override fun getItem(position: Int) = values[position]

    private val darkmode = MyApplication.prefs.getInt("darkmode", 16)

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding =
            ItemCategorySpinnerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val model = values[position]

        if (darkmode == 32) {
            binding.txtName.setTextColor(Color.WHITE)
        } else {
            binding.txtName.setTextColor(Color.BLACK)
        }

        try {
            binding.imgSpinner.setImageResource(model.image)
            binding.txtName.text = model.name
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return binding.root
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding =
            ItemCategorySpinnerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val model = values[position]
        try {
            binding.imgSpinner.setImageResource(model.image)
            binding.txtName.text = model.name

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return binding.root
    }
}