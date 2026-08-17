package com.mytask.data.repository

/**
 * In-memory bridge used only by the first-login template catalog dialog.
 * Selected templates are consumed immediately by TemplateDataImporter.
 */
object TemplateSelectionStore {
    @Volatile
    private var selectedTemplates: List<AppTemplate> = emptyList()

    fun selectAll(templates: List<AppTemplate>) {
        selectedTemplates = templates.distinctBy { "${it.id}@${it.version}" }
    }

    fun consumeAll(): List<AppTemplate> {
        val templates = selectedTemplates
        selectedTemplates = emptyList()
        return templates
    }
}
