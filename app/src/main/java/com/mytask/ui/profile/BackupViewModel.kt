package com.mytask.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytask.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository
) : ViewModel() {

    fun exportData(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch {

            try {

                val json =
                    repository.exportData()

                onSuccess(json)

            } catch (e: Exception) {

                onError(
                    e.message
                        ?: "Gagal membuat backup."
                )
            }
        }
    }

    fun importData(
        json: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch {

            try {

                repository.importData(json)

                onSuccess()

            } catch (e: Exception) {

                onError(
                    e.message
                        ?: "Gagal mengimpor backup."
                )
            }
        }
    }
}