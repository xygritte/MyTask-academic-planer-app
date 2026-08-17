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
            id = "belajar_pengembangan",
            version = 3,
            name = "Belajar & Pengembangan",
            description = "Preset untuk membaca, latihan bahasa, dan menulis jurnal.",
            category = "Pengembangan Diri",
            icon = "📚",
            assetName = "template_belajar_pengembangan.json"
        ),
        AppTemplate(
            id = "olahraga_kebugaran",
            version = 3,
            name = "Olahraga & Kebugaran",
            description = "Rutinitas lari, latihan kekuatan, dan stretching.",
            category = "Kesehatan",
            icon = "🏃",
            assetName = "template_olahraga_kebugaran.json"
        ),
        AppTemplate(
            id = "ibadah_harian",
            version = 3,
            name = "Ibadah Harian",
            description = "Preset ibadah harian dengan jadwal sholat, membaca Al-Quran, dan dzikir.",
            category = "Ibadah",
            icon = "🕌",
            assetName = "template_ibadah_harian.json"
        ),
        AppTemplate(
            id = "produktivitas_harian",
            version = 3,
            name = "Produktivitas Harian",
            description = "Rutinitas makan, olahraga ringan, dan waktu tidur.",
            category = "Kegiatan Harian",
            icon = "🎯",
            assetName = "template_produktivitas_harian.json"
        ),
        AppTemplate(
            id = "kegiatan_luar_sosial",
            version = 3,
            name = "Kegiatan Luar & Sosial",
            description = "Preset untuk jalan-jalan santai dan kegiatan komunitas.",
            category = "Sosial & Luar Ruang",
            icon = "🌳",
            assetName = "template_kegiatan_luar_sosial.json"
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
