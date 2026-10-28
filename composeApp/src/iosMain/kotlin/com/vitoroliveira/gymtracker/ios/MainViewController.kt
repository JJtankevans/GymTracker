package com.vitoroliveira.gymtracker.ios

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Chamado pelo Swift (iosApp/iosApp/iOSApp.swift): a tela inteira é Compose. */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
