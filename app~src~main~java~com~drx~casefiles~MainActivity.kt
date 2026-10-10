package com.drx.casefiles

import android.app.Application
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.drx.casefiles.data.AppDb
import com.drx.casefiles.data.Repo
import com.drx.casefiles.ui.CameraScreen
import com.drx.casefiles.ui.CompareScreen
import com.drx.casefiles.ui.HomeScreen
import com.drx.casefiles.ui.Nav
import com.drx.casefiles.ui.PatientScreen
import com.drx.casefiles.ui.Screen
import com.drx.casefiles.ui.SearchScreen
import com.drx.casefiles.ui.ViewerScreen
import com.drx.casefiles.ui.VisitScreen

class App : Application() {
    lateinit var repo: Repo

    override fun onCreate() {
        super.onCreate()
        repo = Repo(this, AppDb.get(this).dao())
    }
}

/** Lets the camera screen use the volume keys as a shutter. */
object KeyBridge {
    var onVolume: (() -> Unit)? = null
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as App).repo
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface { AppRoot(repo) }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val handler = KeyBridge.onVolume
        if (handler != null && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            if (event?.repeatCount == 0) handler()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun AppRoot(repo: Repo) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val nav = remember { Nav(stack) }
    BackHandler(enabled = stack.size > 1) { nav.pop() }
    val s = stack.last()
    key(stack.size, s) {
        when (s) {
            is Screen.Home -> HomeScreen(repo, nav)
            is Screen.Search -> SearchScreen(repo, nav)
            is Screen.PatientDetail -> PatientScreen(s.id, repo, nav)
            is Screen.VisitDetail -> VisitScreen(s.id, repo, nav)
            is Screen.Camera -> CameraScreen(s.visitId, s.category, repo, nav)
            is Screen.Viewer -> ViewerScreen(s.items, s.index, repo, nav)
            is Screen.Compare -> CompareScreen(s.a, s.b, s.la, s.lb, repo, nav)
        }
    }
}
