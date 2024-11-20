package com.example.sqlproject

import FeedReaderContract
import FeedReaderDbHelper
import android.content.ContentValues
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sqlproject.ui.theme.SQLProjectTheme

class MainActivity : ComponentActivity() {

    private lateinit var dbHelper: FeedReaderDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        dbHelper = FeedReaderDbHelper(this)

        setContent {
            SQLProjectTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {
        var entries by remember { mutableStateOf(listOf<Triple<Long, String, String>>()) }
        var title by remember { mutableStateOf("") }
        var subtitle by remember { mutableStateOf("") }
        var selectedId by remember { mutableStateOf<Long?>(null) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            content = { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text("Database CRUD Application")
                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Sous-titre") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        if (title.isNotBlank() && subtitle.isNotBlank()) {
                            insertData(title, subtitle)
                            entries = loadData()
                            title = ""
                            subtitle = ""
                            selectedId = null
                        }
                    }) {
                        Text("Ajouter des données")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        selectedId?.let {
                            updateData(it, title, subtitle)
                            entries = loadData()
                            title = ""
                            subtitle = ""
                            selectedId = null
                        } ?: Log.d("MainActivity", "Aucun ID sélectionné pour la mise à jour")
                    }) {
                        Text("Mettre à jour les données")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bouton pour supprimer toutes les données
                    Button(onClick = {
                        deleteAllData()
                        entries = loadData()  // Recharge les données, maintenant vide
                        title = ""
                        subtitle = ""
                        selectedId = null
                    }) {
                        Text("Supprimer toutes les données")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        entries = loadData()
                    }) {
                        Text("Charger les données")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn {
                        items(entries) { (id, title, subtitle) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedId = id
                                        Log.d("MainActivity", "Sélectionné - ID: $id, Titre: $title, Sous-titre: $subtitle")
                                    }
                                    .padding(8.dp)
                            ) {
                                Text("ID: $id, Titre: $title, Sous-titre: $subtitle")
                            }
                        }
                    }
                }
            }
        )
    }

    private fun insertData(title: String, subtitle: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE, title)
            put(FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE, subtitle)
        }
        val newRowId = db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values)
        Log.d("MainActivity", "Ajout - Nouveau ID: $newRowId, Titre: $title, Sous-titre: $subtitle")
    }

    private fun loadData(): List<Triple<Long, String, String>> {
        val db = dbHelper.readableDatabase
        val projection = arrayOf(
            BaseColumns._ID,
            FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE,
            FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE
        )

        val cursor = db.query(
            FeedReaderContract.FeedEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            null
        )

        val entries = mutableListOf<Triple<Long, String, String>>()
        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(BaseColumns._ID))
                val title = getString(getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE))
                val subtitle = getString(getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE))
                entries.add(Triple(id, title, subtitle))
                Log.d("MainActivity", "Chargement - ID: $id, Titre: $title, Sous-titre: $subtitle")
            }
        }
        cursor.close()
        return entries
    }

    private fun updateData(id: Long, newTitle: String, newSubtitle: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE, newTitle)
            put(FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE, newSubtitle)
        }
        db.update(
            FeedReaderContract.FeedEntry.TABLE_NAME,
            values,
            "${BaseColumns._ID} = ?",
            arrayOf(id.toString())
        )
    }

    private fun deleteAllData() {
        val db = dbHelper.writableDatabase
        val rowsDeleted = db.delete(FeedReaderContract.FeedEntry.TABLE_NAME, null, null)
        Log.d("MainActivity", "Suppression de toutes les données - Lignes supprimées: $rowsDeleted")
    }
}
