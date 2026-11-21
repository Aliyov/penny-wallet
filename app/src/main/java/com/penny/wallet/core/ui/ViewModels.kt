package com.penny.wallet.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.penny.wallet.AppContainer
import com.penny.wallet.container

/** `val vm = pennyViewModel { HomeViewModel(it) }` */
@Composable
inline fun <reified VM : ViewModel> pennyViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val c = LocalContext.current.container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(c) } })
}
