package com.noonyuu.prc

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.noonyuu.prc.ui.user.UserScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        UserScreen()
    }
}
