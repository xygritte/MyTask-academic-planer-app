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
        val selected = TemplateSelectionStore.consumeAll()
        if (selected.isEmpty()) {
            error("Belum ada template yang dipilih.")
        }
        selected.forEach { templateApplyRepository.apply(it) }
    }
}
