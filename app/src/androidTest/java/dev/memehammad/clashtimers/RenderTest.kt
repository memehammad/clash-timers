package dev.memehammad.clashtimers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Draws the Smartspacer card for a sample village (pulled by the "renders" workflow). */
@RunWith(AndroidJUnit4::class)
class RenderTest {
    @Test
    fun renderSmartspace() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val ctx = inst.targetContext
        val out = File(ctx.getExternalFilesDir(null), "renders").apply { mkdirs() }
        val now = System.currentTimeMillis() / 1000
        // Sample village: 6 builders (2 busy), lab busy, a potion running.
        val json = """{"tag":"#TEST","timestamp":$now,
            "helpers":[{"data":93000000,"lvl":8,"helper_cooldown":66076}],
            "buildings":[{"data":1000015,"lvl":8,"cnt":5},{"data":1000064,"lvl":1,"cnt":1},{"data":1000007,"lvl":16,"cnt":1},
              {"data":1000000,"lvl":14,"cnt":4},{"data":1000003,"lvl":18,"timer":8100},{"data":1000005,"lvl":18,"timer":30000},
              {"data":1000026,"lvl":13,"cnt":1}],
            "units":[{"data":4000000,"lvl":12,"timer":5000}],
            "boosts":{"clocktower_cooldown":68388}}"""
        Store.save(ctx, "#TEST", json)
        val cases = listOf(
            "builders" to SmartOptions(what = setOf("builder")),
            "builders_lab" to SmartOptions(what = setOf("builder", "lab")),
            "builders_lab_pets" to SmartOptions(what = setOf("builder", "lab", "pet")),
        )
        inst.runOnMainSync {
            for ((name, o) in cases) {
                try {
                    val x = Smartspace.content(ctx, o.copy(village = "#TEST"), System.currentTimeMillis())
                    if (x == null) {
                        File(out, "$name.txt").writeText("hidden")
                        continue
                    }
                    val rv = Smartspace.remoteViews(ctx, x, x.subtitle)
                    for ((bg, label) in listOf(Color.rgb(30, 40, 60) to "dark", Color.rgb(200, 210, 220) to "light")) {
                        val parent = FrameLayout(ctx)
                        val v = rv.apply(ctx, parent)
                        val d = ctx.resources.displayMetrics.density
                        val w = (380 * d).toInt()
                        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                        v.layout(0, 0, w, v.measuredHeight)
                        val pad = (16 * d).toInt()
                        val bmp = Bitmap.createBitmap(w + 2 * pad, v.measuredHeight + 2 * pad, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        canvas.drawColor(bg)
                        canvas.translate(pad.toFloat(), pad.toFloat())
                        v.draw(canvas)
                        File(out, "${name}_$label.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                } catch (e: Throwable) {
                    File(out, "$name.txt").writeText(e.stackTraceToString())
                }
            }
        }
    }
}
