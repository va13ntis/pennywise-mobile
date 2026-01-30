package com.pennywise.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pennywise.app.domain.model.BillingCycle

/**
 * Composable for selecting a billing cycle from available options.
 * 
 * Displays the currently selected billing cycle and allows users to select
 * from available cycles via a dropdown menu. Handles empty state gracefully
 * and ensures proper accessibility support.
 * 
 * @param selectedBillingCycle The currently selected billing cycle, or null if none selected
 * @param availableBillingCycles List of available billing cycles to choose from
 * @param onBillingCycleSelected Callback invoked when a billing cycle is selected
 * @param modifier Modifier to be applied to the component
 */
@Composable
fun BillingCycleSelector(
    selectedBillingCycle: BillingCycle?,
    availableBillingCycles: List<BillingCycle>,
    onBillingCycleSelected: (BillingCycle) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Handle empty state - don't render if no cycles available
    if (availableBillingCycles.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Billing Cycle",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "No billing cycles available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Billing Cycle",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { expanded = true })
                        .semantics {
                            contentDescription = "Select billing cycle. Currently selected: ${selectedBillingCycle?.displayName ?: "None"}"
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedBillingCycle?.displayName ?: "Select Billing Cycle",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedBillingCycle == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null, // Already described by parent semantics
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    availableBillingCycles.forEach { cycle ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = cycle.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                onBillingCycleSelected(cycle)
                                expanded = false
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Select ${cycle.displayName}"
                            }
                        )
                    }
                }
            }
        }
    }
}

