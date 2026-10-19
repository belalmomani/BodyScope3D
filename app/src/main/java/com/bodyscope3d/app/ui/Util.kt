package com.bodyscope3d.app.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** كل عمليات القاعدة خارج خيط الواجهة */
fun CoroutineScope.launchIO(block: suspend () -> Unit) = launch(Dispatchers.IO) { block() }
