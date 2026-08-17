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
    val tasks: Int,
    val schedules: Int
)

@Singleton
class TemplateCatalog @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val templates: List<AppTemplate> = listOf(
        AppTemplate("universal_academic_starter", 1, "Academic Starter", "Data awal perkuliahan untuk memulai semester.", "Akademik", "📚", "template_academic.json"),
        AppTemplate("healthy_daily_routine", 1, "Healthy Daily Routine", "Rutinitas sederhana untuk makan, olahraga, dan minum air.", "Kesehatan", "🏃", "template_healthy_routine.json"),
        AppTemplate("daily_prayer_schedule", 1, "Waktu Sholat Harian", "Rutinitas sholat lima waktu sebagai jadwal harian.", "Ibadah", "🕌", "template_prayer_schedule.json"),
        AppTemplate("productive_day", 1, "Productive Day", "Rutinitas fokus, membaca, olahraga, dan refleksi harian.", "Produktivitas", "🎯", "template_productive_day.json")
    )

    fun preview(template: AppTemplate): TemplatePreview {
        val root = readJson(template)
        return TemplatePreview(
            courses = root.optJSONArray("courses")?.length() ?: 0,
            tasks = root.optJSONArray("tasks")?.length() ?: 0,
            schedules = root.optJSONArray("schedules")?.length() ?: 0
        )
    }

    fun readJson(template: AppTemplate): JSONObject =
        context.assets.open("templates/${template.assetName}").use { input ->
            InputStreamReader(input, Charsets.UTF_8).use { JSONObject(it.readText()) }
        }
}
