package com.vitoroliveira.gymtracker.ui.profile

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Salva o CSV no cache do app e abre o "compartilhar" do Android (Drive, WhatsApp, e-mail...). */
fun Context.shareCsv(csv: String, fileName: String = "gymtracker_treinos.csv") {
    val dir = File(cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, fileName)
    // BOM: faz o Excel reconhecer UTF-8 (acentos) ao abrir direto
    file.writeText("﻿" + csv, Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Meus treinos (GymTracker)")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, "Exportar treinos").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
