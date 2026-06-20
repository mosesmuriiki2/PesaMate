package com.musafinance.pesamate.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoanAnalyticsCard(status: Map<String, Double>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Loan Portfolio Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            LoanRow("Total Borrowed", status["Total Borrowed"] ?: 0.0, MaterialTheme.colorScheme.primary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.3f))
            LoanRow("Total Repaid", status["Total Repaid"] ?: 0.0, Color(0xFF2E7D32))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.3f))
            LoanRow("Outstanding Balance", status["Outstanding"] ?: 0.0, Color(0xFFC62828))
            
            val repaid = status["Total Repaid"] ?: 0.0
            val total = status["Total Borrowed"] ?: 1.0
            val progress = (repaid / total).toFloat().coerceIn(0f, 1f)
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Repayment Progress (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                color = if (progress > 0.8f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LoanRow(label: String, amount: Double, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            "KSh ${String.format("%,.2f", amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                "KSh ${String.format("%,.0f", amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RecipientCard(
    rank: Int,
    name: String,
    amount: Double,
    isTopThree: Boolean,
    isIncoming: Boolean = false
) {
    val medalIcon = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTopThree) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank or Medal
                if (medalIcon != null) {
                    Text(
                        medalIcon,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                rank.toString(),
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Column {
                    Text(
                        name.take(30),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isTopThree) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        if (isIncoming) "Received from" else "Sent to",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "KSh ${String.format("%,.0f", amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncoming) 
                        MaterialTheme.colorScheme.secondary 
                    else 
                        MaterialTheme.colorScheme.error
                )
                Icon(
                    if (isIncoming) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isIncoming) 
                        MaterialTheme.colorScheme.secondary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CategoryListCard(categoryMap: Map<String, Double>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Top Spending Categories",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val sortedCategories = categoryMap.toList().sortedByDescending { it.second }.take(5)
            
            if (sortedCategories.isEmpty()) {
                EmptyState("No categories found")
            } else {
                sortedCategories.forEachIndexed { index, (cat, amt) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            "KSh ${String.format("%,.0f", amt)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (index < sortedCategories.size - 1) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun SmartInsightsCard(
    categoryMap: Map<String, Double>,
    incomeVsExpense: Pair<Double, Double>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "PesaMate Insight",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            val topCat = categoryMap.maxByOrNull { it.value }?.key
            val savingsRate = if (incomeVsExpense.first > 0) 
                ((incomeVsExpense.first - incomeVsExpense.second) / incomeVsExpense.first * 100)
            else 0.0
            
            Text(
                when {
                    topCat != null && savingsRate < 20 -> 
                        "You're spending ${String.format("%.1f%%", (categoryMap[topCat]!! / incomeVsExpense.second * 100))} of expenses on $topCat. Consider setting a budget to save more."
                    savingsRate >= 20 -> 
                        "Great job! You're saving ${String.format("%.1f%%", savingsRate)} of your income. Keep it up!"
                    incomeVsExpense.second > incomeVsExpense.first && incomeVsExpense.first > 0 ->
                        "⚠️ Your expenses exceed your income. Review your spending habits."
                    else -> 
                        "Start tracking more transactions to see personalized insights."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
