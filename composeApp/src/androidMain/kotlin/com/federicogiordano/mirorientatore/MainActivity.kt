package com.federicogiordano.mirorientatore

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.federicogiordano.mirorientatore.api.QuizService
import com.federicogiordano.mirorientatore.data.FileSystem
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val quizService = QuizService.getInstance(applicationContext)

        setContent {
            val scope = rememberCoroutineScope()

            val importQuizLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        val jsonContent = FileSystem.readTextFromUri(applicationContext, uri)
                        if (jsonContent != null) {
                            val success = quizService.importQuizzesFromJson(jsonContent)
                            if (success) {
                                Toast.makeText(applicationContext, "Quiz importati con successo", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(applicationContext, "Errore durante l'importazione dei quiz", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(applicationContext, "Impossibile leggere il file selezionato", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(applicationContext, "Nessun file selezionato per l'importazione", Toast.LENGTH_SHORT).show()
                }
            }

            val exportQuizLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        val jsonContent = quizService.exportQuizzesToJson()
                        if (jsonContent != null) {
                            val success = FileSystem.writeTextToUri(applicationContext, uri, jsonContent)
                            if (success) {
                                Toast.makeText(applicationContext, "Quiz esportati con successo", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(applicationContext, "Errore durante l'esportazione dei quiz", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(applicationContext, "Nessun quiz da esportare o errore nella generazione del file", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(applicationContext, "Nessuna destinazione selezionata per l'esportazione", Toast.LENGTH_SHORT).show()
                }
            }

            App(
                triggerImport = {
                    importQuizLauncher.launch("application/json")
                },
                triggerExport = {
                    val defaultFileName = "mirorientatore_quizzes.json"
                    exportQuizLauncher.launch(defaultFileName)
                }
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        triggerImport = {},
        triggerExport = {}
    )
}