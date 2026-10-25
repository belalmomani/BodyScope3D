package com.bodyscope3d.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** كل عمليات القاعدة خارج خيط الواجهة */
fun CoroutineScope.launchIO(block: suspend () -> Unit) = launch(Dispatchers.IO) { block() }

@Composable fun isArabic(): Boolean = LocalLayoutDirection.current == LayoutDirection.Rtl
