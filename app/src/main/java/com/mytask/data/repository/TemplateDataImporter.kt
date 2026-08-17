package com.mytask.data.repository

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backward-compatible entry point used by the first-login flow.
 * The real template application is handled by TemplateApplyRepository so every
 * template follows the same additive, multi-range-safe path used by Backup & Data.
 */
@Singleton
class TemplateDataImporter @Inject constructor(
    private val templateApplyRepository: TemplateApplyRepository
) {
    suspend fun importTemplate() {
        val template = TemplateSelectionStore.consume()
            ?: error("Belum ada template yang dipilih.")
        templateApplyRepository.apply(template)
    }
}
