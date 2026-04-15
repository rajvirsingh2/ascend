package com.ascend.app.domain.model

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val message: String, val code: Int = 0) : Result<Nothing>
    data object Loading : Result<Nothing>
}