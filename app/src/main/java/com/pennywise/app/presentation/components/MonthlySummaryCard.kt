package com.pennywise.app.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywise.app.R
import com.pennywise.app.presentation.theme.expense_red
import com.pennywise.app.presentation.theme.income_green
import com.pennywise.app.presentation.theme.neutral_gray
import kotlin.math.abs

/**
 * Monthly summary card component that displays income, expenses, and net balance
 */
@Composable
fun MonthlySummaryCard(
    totalIncome: Double,
    totalExpenses: Double,
    modifier: Modifier = Modifier
) {
    val netBalance = totalIncome - totalExpenses
    val isPositive = netBalance >= 0
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Net Balance Section
            AnimatedContent(
                targetState = netBalance,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = slideInVertically(
                            animationSpec = tween(300)
                        ) { height -> height } + fadeIn(
                            animationSpec = tween(300)
                        ),
                        initialContentExit = slideOutVertically(
                            animationSpec = tween(300)
                        ) { height -> -height } + fadeOut(
                            animationSpec = tween(300)
                        )
                    )
                }
            ) { balance ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.net_balance),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(balance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) income_green else expense_red
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Income and Expenses Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Income Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.income),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = totalIncome,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = slideInVertically(
                                    animationSpec = tween(300)
                                ) { height -> height } + fadeIn(
                                    animationSpec = tween(300)
                                ),
                                initialContentExit = slideOutVertically(
                                    animationSpec = tween(300)
                                ) { height -> -height } + fadeOut(
                                    animationSpec = tween(300)
                                )
                            )
                        }
                    ) { income ->
                        Text(
                            text = formatCurrency(income),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = income_green
                        )
                    }
                }
                
                // Divider
                Text(
                    text = "•",
                    style = MaterialTheme.typography.titleLarge,
                    color = neutral_gray
                )
                
                // Expenses Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.expenses),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = totalExpenses,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = slideInVertically(
                                    animationSpec = tween(300)
                                ) { height -> height } + fadeIn(
                                    animationSpec = tween(300)
                                ),
                                initialContentExit = slideOutVertically(
                                    animationSpec = tween(300)
                                ) { height -> -height } + fadeOut(
                                    animationSpec = tween(300)
                                )
                            )
                        }
                    ) { expenses ->
                        Text(
                            text = formatCurrency(expenses),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = expense_red
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format currency value with proper decimal places and currency symbol
 */
private fun formatCurrency(amount: Double): String {
    return if (amount >= 0) {
        "$${String.format("%.2f", amount)}"
    } else {
        "-$${String.format("%.2f", abs(amount))}"
    }
}
