package com.mytask.data.repository

/**
 * Small in-memory bridge used only by the first-login template catalog dialog.
 * The selected template is consumed immediately by TemplateDataImporter.
 */
object TemplateSelectionStore {
    @Volatile
    private var selectedTemplate: AppTemplate? = null

    fun select(template: AppTemplate) {
        selectedTemplate = template
    }

    fun consume(): AppTemplate? {
        val template = selectedTemplate
        selectedTemplate = null
        return template
    }
}
