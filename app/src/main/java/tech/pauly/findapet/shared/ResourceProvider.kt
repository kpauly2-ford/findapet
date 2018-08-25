package tech.pauly.findapet.shared

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.annotation.PluralsRes
import android.support.annotation.StringRes
import com.squareup.picasso.Picasso
import tech.pauly.findapet.dependencyinjection.ForApplication
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A Singleton class to handle things that need context.
 *
 * Not an ideal solution for sure, but I continue to run into situations where
 * there's no way around using context in a ViewModel. Usually when concatenating
 * local resources and Strings from server data. This is a god object that holds one
 * reference to the Application. Not an ideal solution, but everyone can inject
 * this class and use the limited set of context APIs implemented in this class.
 */
@Singleton
open class ResourceProvider @Inject
internal constructor(@ForApplication private val context: Context) {

    open fun getString(@StringRes stringId: Int, vararg formatArgs: Any): String {
        return context.getString(stringId, *formatArgs)
    }

    open fun getQuantityString(@PluralsRes stringId: Int, quantity: Int): String {
        return context.resources.getQuantityString(stringId, quantity, quantity)
    }

    open fun getBitmapFromUrl(url: String): Bitmap {
        return Picasso.get().load(url).get()
    }

    open fun saveBitmapToFile(filename: String, bitmap: Bitmap): String {
        context.openFileOutput(filename, Context.MODE_PRIVATE).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.close()
        }
        return context.getFileStreamPath(filename).absolutePath
    }

    open fun openBitmapFromFile(filename: String): Bitmap {
        context.openFileInput(filename).use {
            return BitmapFactory.decodeStream(it)
        }
    }

    open fun deleteFile(path: String) {
        val filename = path.substring(path.lastIndexOf('/') + 1)
        context.deleteFile(filename)
    }
}