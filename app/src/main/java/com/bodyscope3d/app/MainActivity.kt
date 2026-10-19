package com.bodyscope3d.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.bodyscope3d.app.data.AppGraph
import com.bodyscope3d.app.ui.AppRoot

class MainActivity : AppCompatActivity() {
    private lateinit var graph: AppGraph
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        graph = AppGraph(this)
        setContent { AppRoot(graph) }
    }
}
