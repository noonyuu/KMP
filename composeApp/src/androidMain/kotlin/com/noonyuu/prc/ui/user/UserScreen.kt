package com.noonyuu.prc.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun UserScreen(
    viewModel: UserViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var input by remember { mutableStateOf("1") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("User ID") },
        )
        Button(onClick = { viewModel.load(input) }) {
            Text("Load")
        }

        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
            )
            state.user != null -> {
                val u = state.user!!
                Text("ID: ${u.id.value}")
                Text("Name: ${u.name}")
                Text("Email: ${u.email}")
            }
        }
    }
}
