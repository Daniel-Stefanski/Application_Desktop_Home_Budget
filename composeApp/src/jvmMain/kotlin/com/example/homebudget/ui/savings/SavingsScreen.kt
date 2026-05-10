package com.example.homebudget.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.ui.common.feedback.EmptyState
import com.example.homebudget.ui.common.feedback.LoadingState
import com.example.homebudget.viewmodel.savings.SavingsViewModel

/**
 * Główny ekran celów oszczędnościowych.
 * Odpowiada za wyświetlenie listy celów i uruchamianie dialogów.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen() {
    val viewModel: SavingsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadGoals()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Row {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp)
            ) {

                Column(modifier = Modifier.fillMaxSize()) {
                    // Tytuł
                    Text(
                        "Cele oszczędnościowe",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    // Przycisk dodaj cel
                    Button(onClick = { viewModel.showAddGoalDialog() }) {
                        Text("➕ Dodaj cel")
                    }
                    Spacer(Modifier.height(16.dp))

                    if (state.isLoading) {
                        LoadingState(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            message = "Ładowanie danych..."
                        )
                    } else if (state.goals.isEmpty()) {
                        EmptyState(text = "📝 Brak celów oszczędnościowych.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.goals) { goal ->
                                SavingsGoalCard(
                                    goal = goal,
                                    onAddAmount = { viewModel.showAddContributionDialog(goal) },
                                    onWithdraw = { viewModel.showWithdrawDialog(goal) },
                                    onEdit = { viewModel.showEditGoalDialog(goal) },
                                    onDelete = { viewModel.showDeleteGoalDialog(goal) },
                                    onHistory = { viewModel.showHistoryDialog(goal) }
                                )
                            }
                        }
                    }
                }

                SavingsDialogs(viewModel = viewModel)
            }
        }
    }
}
