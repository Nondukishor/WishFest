package com.nipu.wishfest.ui.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.platform.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProjectsUiState(
    val projects: List<ProjectModel> = emptyList(),
    val isLoading: Boolean = false
)

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    fun loadProjects() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val list = repository.listAllProjects()
        _uiState.value = ProjectsUiState(projects = list, isLoading = false)
    }

    fun deleteProject(projectId: String) {
        repository.deleteProject(projectId)
        loadProjects()
    }

    fun duplicateProject(projectId: String) {
        repository.duplicateProject(projectId)
        loadProjects()
    }
}
