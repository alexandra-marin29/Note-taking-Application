package com.example.noteapp.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import com.example.noteapp.R

fun createColorBorderDrawable(context: Context, colorHex: String): Drawable {
    val chosenColor = Color.parseColor(colorHex)
    val colorDrawable = GradientDrawable().apply {
        cornerRadius = 10f
        setColor(chosenColor)
    }

    val borderDrawable = ContextCompat.getDrawable(context, R.drawable.pink_border)!!

    val layers = arrayOf<Drawable>(colorDrawable, borderDrawable)
    return LayerDrawable(layers)
}
