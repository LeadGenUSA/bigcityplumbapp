package com.bigcityplumbing.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bigcityplumbing.config.AppConfig
import com.bigcityplumbing.ui.theme.BrandBlue
import com.bigcityplumbing.ui.theme.BrandOrange

private val SERVICE_TYPES = listOf(
    "Emergency leak / burst pipe",
    "Clogged drain or toilet",
    "Water heater problem",
    "Heating system service",
    "Bathroom or kitchen remodel",
    "Inspection / quote",
    "Other",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRequestScreen() {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf(SERVICE_TYPES.first()) }
    var details by remember { mutableStateOf("") }
    var typeMenuOpen by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Request Service", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Fill this out and we'll send it to ${AppConfig.EMAIL}. For emergencies please call us.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        // Quick call button
        Button(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(AppConfig.telUri())))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Phone, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Call now: ${AppConfig.PHONE_NUMBER_DISPLAY}", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Service address") },
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )

        ExposedDropdownMenuBox(
            expanded = typeMenuOpen,
            onExpandedChange = { typeMenuOpen = !typeMenuOpen },
        ) {
            OutlinedTextField(
                value = serviceType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Type of service") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = typeMenuOpen,
                onDismissRequest = { typeMenuOpen = false },
            ) {
                SERVICE_TYPES.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { serviceType = option; typeMenuOpen = false },
                    )
                }
            }
        }

        OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text("Describe the problem") },
            singleLine = false,
            minLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        )

        Button(
            onClick = {
                when {
                    name.isBlank() -> error = "Please enter your name."
                    phone.isBlank() -> error = "Please enter a phone number we can reach you at."
                    address.isBlank() -> error = "Please enter the service address."
                    else -> {
                        // Build a mailto: that opens the device's email app pre-filled.
                        val subject = "Service request: $serviceType"
                        val body = buildString {
                            appendLine("Name: $name")
                            appendLine("Phone: $phone")
                            if (email.isNotBlank()) appendLine("Email: $email")
                            appendLine("Address: $address")
                            appendLine("Service: $serviceType")
                            appendLine()
                            appendLine("Details:")
                            appendLine(details.ifBlank { "(none)" })
                        }
                        val uri = Uri.parse(
                            "mailto:${Uri.encode(AppConfig.EMAIL)}" +
                                "?subject=${Uri.encode(subject)}" +
                                "&body=${Uri.encode(body)}"
                        )
                        val intent = Intent(Intent.ACTION_SENDTO, uri)
                        try {
                            context.startActivity(intent)
                            showConfirm = true
                        } catch (e: Exception) {
                            error = "No email app found. Please call ${AppConfig.PHONE_NUMBER_DISPLAY}."
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Mail, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Submit Request", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Request prepared") },
            text = { Text("We've opened your email app. Send the message to finish your request.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false }) { Text("OK") }
            },
        )
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Missing info") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }
}
