package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditHabitDialog(
    habitToEdit: Habit?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String?,
        category: String,
        iconName: String,
        colorHex: String,
        frequency: String,
        targetDaysPerWeek: Long,
        targetValue: Long,
        unit: String
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(habitToEdit?.title ?: "") }
    var description by remember { mutableStateOf(habitToEdit?.description ?: "") }
    var category by remember { mutableStateOf(habitToEdit?.category ?: "routine") }
    var selectedIcon by remember { mutableStateOf(habitToEdit?.iconName ?: "meditation") }
    var selectedColor by remember { mutableStateOf(habitToEdit?.colorHex ?: "#10B981") }
    var frequency by remember { mutableStateOf(habitToEdit?.frequency ?: "daily") }
    var isNumericTarget by remember { mutableStateOf((habitToEdit?.targetValue ?: 1L) > 1L) }
    var targetValueText by remember { mutableStateOf((habitToEdit?.targetValue ?: 1L).toString()) }
    var unit by remember { mutableStateOf(habitToEdit?.unit ?: "times") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (habitToEdit == null) "New Habit" else "Edit Habit",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Habit Name Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Habit Title") },
                placeholder = { Text("e.g. Read 20 pages, Hydrate, Meditation") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description (Optional)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Motivation or Details (Optional)") },
                placeholder = { Text("e.g. Right after waking up") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_description_input"),
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Category Selector
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitIconHelper.availableCategories.forEach { (catKey, catLabel) ->
                    val isSelected = category == catKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { category = catKey },
                        label = { Text(catLabel) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Color Palette
            Text(
                text = "Accent Color",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HabitIconHelper.availableColors.forEach { (hex, color) ->
                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColor = hex }
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Icon Picker
            Text(
                text = "Icon Symbol",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HabitIconHelper.availableIcons.forEach { (iconKey, vector) ->
                    val isSelected = selectedIcon == iconKey
                    val tintColor = HabitIconHelper.parseColor(selectedColor)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) tintColor.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) tintColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedIcon = iconKey },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = vector,
                            contentDescription = iconKey,
                            tint = if (isSelected) tintColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Target Type (Checkmark vs Numeric)
            Text(
                text = "Target Goal",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = !isNumericTarget,
                    onClick = {
                        isNumericTarget = false
                        targetValueText = "1"
                        unit = "times"
                    },
                    label = { Text("Simple Done / Not Done") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = isNumericTarget,
                    onClick = {
                        isNumericTarget = true
                        if (targetValueText == "1") targetValueText = "20"
                        if (unit == "times") unit = "pages"
                    },
                    label = { Text("Numeric Quantity") },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            AnimatedVisibility(visible = isNumericTarget) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = targetValueText,
                            onValueChange = { targetValueText = it.filter { char -> char.isDigit() } },
                            label = { Text("Target Amount") },
                            placeholder = { Text("e.g. 20") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("target_value_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
                            placeholder = { Text("e.g. pages, ml, mins") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("target_unit_input"),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Frequency
            Text(
                text = "Frequency",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "daily" to "Every Day",
                    "weekdays" to "Weekdays",
                    "weekends" to "Weekends"
                ).forEach { (freqKey, freqLabel) ->
                    FilterChip(
                        selected = frequency == freqKey,
                        onClick = { frequency = freqKey },
                        label = { Text(freqLabel) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (habitToEdit != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(habitToEdit.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Habit")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        val parsedVal = targetValueText.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
                        val daysPerWeek = when (frequency) {
                            "weekdays" -> 5L
                            "weekends" -> 2L
                            else -> 7L
                        }
                        onSave(
                            title,
                            description.ifBlank { null },
                            category,
                            selectedIcon,
                            selectedColor,
                            frequency,
                            daysPerWeek,
                            parsedVal,
                            unit.ifBlank { "times" }
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_habit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HabitIconHelper.parseColor(selectedColor),
                        contentColor = Color.White
                    ),
                    enabled = title.isNotBlank()
                ) {
                    Text(
                        text = if (habitToEdit == null) "Create Habit" else "Save Changes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
