package com.mytask.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class AppTemplate(
    val id: String,
    val version: Int,
    val name: String,
    val description: String,
    val category: String,
    val icon: String,
    val assetName: String
)

data class TemplatePreview(
    val courses: Int,
    val schedules: Int
)

@Singleton
class TemplateCatalog @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val templates: List<AppTemplate> = listOf(
        AppTemplate(
            id = "universal_academic_starter",
            version = 1,
            name = "Academic Starter",
            description = "Data awal perkuliahan untuk memulai semester.",
            category = "Akademik",
            icon = "📚",
            assetName = "template_academic.json"
        ),
        AppTemplate(
            id = "daily_life_starter",
            version = 3,
            name = "Daily Life Starter",
            description = "Contoh kegiatan harian, ibadah, hubungan, produktivitas, dan musik dengan jadwal siap pakai.",
            category = "Kegiatan Harian",
            icon = "🌿",
            assetName = "template_healthy_routine.json"
        ),
        AppTemplate(
            id = "daily_prayer_schedule",
            version = 1,
            name = "Waktu Sholat Harian",
            description = "Rutinitas sholat lima waktu sebagai jadwal harian.",
            category = "Ibadah",
            icon = "🕌",
            assetName = "template_prayer_schedule.json"
        ),
        AppTemplate(
            id = "productive_day",
            version = 1,
            name = "Productive Day",
            description = "Rutinitas fokus, membaca, olahraga, dan refleksi harian.",
            category = "Produktivitas",
            icon = "🎯",
            assetName = "template_productive_day.json"
        )
    )

    fun preview(template: AppTemplate): TemplatePreview {
        val root = readJson(template)
        return TemplatePreview(
            courses = root.optJSONArray("courses")?.length() ?: 0,
            schedules = root.optJSONArray("schedules")?.length() ?: 0
        )
    }

    fun readJson(template: AppTemplate): JSONObject {
        return context.assets.open("templates/${template.assetName}").use { input ->
            InputStreamReader(input, Charsets.UTF_8).use { reader ->
                JSONObject(reader.readText())
            }
        }
    }
}
