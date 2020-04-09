package com.ifttt.groceryexpress

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target

class FeatureView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defAttrStyle: Int = 0
) : AppCompatTextView(context, attributeSet, defAttrStyle), Target {

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        setCompoundDrawables(placeHolderDrawable, null, null, null)
    }

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
        setCompoundDrawables(null, null, null, null)
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        if (bitmap != null) {
            val size = resources.getDimensionPixelSize(R.dimen.feature_icon_size)
            setCompoundDrawables(BitmapDrawable(resources, bitmap).apply {
                setBounds(0, 0, size, size)
            }, null, null, null)

            setCompoundDrawablePadding(resources.getDimension(R.dimen.feature_drawable_padding).toInt())
        } else {
            setCompoundDrawables(null, null, null, null)
        }
    }
}
