package com.mytask.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytask.data.repository.AppTemplate
import com.mytask.data.repository.BackupRepository
import com.mytask.data.repository.TemplateApplyRepository
import com.mytask.data.repository.TemplateApplyResult
import com.mytask.data.repository.TemplateCatalog
import com.mytask.data.repository.TemplatePreview
import com.mytask.data.repository.UserDataFile
import com.mytask.data.repository.UserDataFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplateCardState(
    val template: AppTemplate,
    val courses: Int,
    val schedules: Int
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val templateCatalog: TemplateCatalog,
    private val templateApplyRepository: TemplateApplyRepository,
    private val userDataFileRepository: UserDataFileRepository
) : ViewModel() {

    val files: StateFlow<List<UserDataFile>> =
        userDataFileRepository.files.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val templates: List<TemplateCardState> = templateCatalog.templates.map { template ->
        val preview = runCatching { templateCatalog.preview(template) }
            .getOrDefault(TemplatePreview(0, 0))
        TemplateCardState(template, preview.courses, preview.schedules)
    }

    fun exportData(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { backupRepository.exportData() }
                .onSuccess(onSuccess)
                .onFailure { onError(it.message ?: "Gagal membuat backup.") }
        }
    }

    fun importData(json: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { backupRepository.importData(json) }
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Gagal mengimpor backup.") }
        }
    }

    fun rememberFile(uri: Uri) {
        viewModelScope.launch { runCatching { userDataFileRepository.remember(uri) } }
    }

    fun removeFile(uri: String) {
        viewModelScope.launch { userDataFileRepository.remove(uri) }
    }

    fun applyTemplate(
        template: AppTemplate,
        onSuccess: (TemplateApplyResult) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching { templateApplyRepository.apply(template) }
                .onSuccess(onSuccess)
                .onFailure { onError(it.message ?: "Template gagal diterapkan.") }
        }
    }
}
