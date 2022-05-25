package com.jeanwest.mobile.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorSnackBar(state: SnackbarHostState) {

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {

        SnackbarHost(hostState = state, snackbar = {
            Snackbar(
                shape = MaterialTheme.shapes.large,
                action = {
                    Text(
                        text = "باشه",
                        color = JeanswestButtonDisabled,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable {
                                state.currentSnackbarData?.dismiss()
                            }
                    )
                }
            ) {
                Text(
                    text = state.currentSnackbarData?.message ?: "",
                    color = errorColor,
                    style = MaterialTheme.typography.body1,
                )
            }
        })
    }
}