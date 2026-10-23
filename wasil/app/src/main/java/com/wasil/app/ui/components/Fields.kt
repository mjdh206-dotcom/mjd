package com.wasil.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.wasil.app.ui.theme.Gold
import com.wasil.app.ui.theme.Hairline
import com.wasil.app.ui.theme.SurfaceTwo
import com.wasil.app.ui.theme.TextMuted
import com.wasil.app.ui.theme.TextPrimary

@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: Painter,
    keyboardType: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Text(
        label,
        style = MaterialTheme.typography.titleSmall,
        color = TextMuted,
        modifier = Modifier.padding(bottom = 6.dp),
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        placeholder = {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextMuted.copy(alpha = 0.7f))
        },
        leadingIcon = {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                colorFilter = ColorFilter.tint(Gold),
            )
        },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceTwo,
            unfocusedContainerColor = SurfaceTwo,
            disabledContainerColor = SurfaceTwo,
            focusedBorderColor = Gold,
            unfocusedBorderColor = Hairline,
            cursorColor = Gold,
            focusedLeadingIconColor = Gold,
            unfocusedLeadingIconColor = Gold,
        ),
    )
}

@Composable
fun InlineLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted,
        modifier = modifier,
    )
}
