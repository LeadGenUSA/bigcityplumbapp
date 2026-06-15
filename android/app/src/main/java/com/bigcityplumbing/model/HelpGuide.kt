package com.bigcityplumbing.model

import android.content.Context
import com.bigcityplumbing.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class GuideSection(
    val heading: String,
    val body: String,
)

data class HelpGuide(
    val id: String,
    val title: String,
    val summary: String,
    val icon: String,
    val sections: List<GuideSection>,
)

object HelpGuideLoader {
    /**
     * Loads and parses the bundled help_guides.json from res/raw.
     * Returns an empty list if anything goes wrong (defensive).
     */
    fun load(context: Context): List<HelpGuide> {
        return try {
            val raw = context.resources.openRawResource(R.raw.help_guides).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            }
            val root = JSONObject(raw)
            val arr = root.getJSONArray("guides")
            (0 until arr.length()).map { i ->
                val g = arr.getJSONObject(i)
                val sectionsArr = g.getJSONArray("sections")
                val sections = (0 until sectionsArr.length()).map { j ->
                    val s = sectionsArr.getJSONObject(j)
                    GuideSection(
                        heading = s.getString("heading"),
                        body = s.getString("body"),
                    )
                }
                HelpGuide(
                    id = g.getString("id"),
                    title = g.getString("title"),
                    summary = g.getString("summary"),
                    icon = g.optString("icon", "info"),
                    sections = sections,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
