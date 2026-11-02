package com.example.expensetracker.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.R
import com.example.expensetracker.data.CategoryBudget
import com.example.expensetracker.data.Expense
import com.example.expensetracker.domain.BudgetMath
import com.example.expensetracker.domain.BudgetStatus
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.viewmodel.ExpenseViewModel
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val categories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Health", "Other")
private val categoryColors = listOf(Color(0xFFEF8354), Color(0xFF4D96FF), Color(0xFF6C63FF),
    Color(0xFFE6A23C), Color(0xFFB66DCE), Color(0xFF48A999), Color(0xFF8793A5))

@Composable
fun ExpenseApp(vm: ExpenseViewModel) {
    val language by vm.language.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val localizedConfig = remember(configuration, language) {
        configuration.copy().apply { setLocale(Locale(if (language == "bn") "bn" else "en")) }
    }
    ExpenseTrackerTheme(theme) {
        CompositionLocalProvider(LocalConfiguration provides localizedConfig) {
            MainShell(vm)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)\nprivate fun MainShell(vm: ExpenseViewModel) {
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val budgets by vm.categoryBudgets.collectAsStateWithLifecycle()
    val totalBudget by vm.totalBudget.collectAsStateWithLifecycle()
    val month by vm.selectedMonth.collectAsStateWithLifecycle()
    var page by remember { mutableIntStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Expense?>(null) }
    var deleteTarget by remember { mutableStateOf<Expense?>(null) }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@runCatching
            val obj = JSONObject(json)
            val expArray = obj.optJSONArray("expenses")
            val imported = buildList {
                if (expArray != null) for (i in 0 until expArray.length()) {
                    val item = expArray.getJSONObject(i)
                    add(Expense(item.optLong("id"), item.optDouble("amount"), item.optString("date"),
                        item.optString("category", "Other"), item.optString("note", "")))
                }
            }
            val bArray = obj.optJSONArray("categoryBudgets")
            val importedBudgets = buildList {
                if (bArray != null) for (i in 0 until bArray.length()) {
                    val item = bArray.getJSONObject(i)
                    add(CategoryBudget(item.optString("category"), item.optDouble("monthlyLimit")))
                }
            }
            vm.importBackup(imported, importedBudgets, obj.optDouble("totalBudget", 0.0))
        }
    }

    val monthExpenses = expenses.filter { runCatching { YearMonth.from(LocalDate.parse(it.date)) }.getOrNull() == month }
    val thisMonthTotal = monthExpenses.sumOf { it.amount }
    val previousTotal = expenses.filter {
        runCatching { YearMonth.from(LocalDate.parse(it.date)) }.getOrNull() == month.minusMonths(1)
    }.sumOf { it.amount }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Expense Tracker", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = { page = 3 }) { Icon(Icons.Default.Settings, contentDescription = null) } }
            )
        },
        floatingActionButton = {
            if (page == 0 || page == 1) FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_expense))
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = page == 0, onClick = { page = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.dashboard)) })
                NavigationBarItem(selected = page == 1, onClick = { page = 1 }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text(stringResource(R.string.expenses)) })
                NavigationBarItem(selected = page == 2, onClick = { page = 2 }, icon = { Icon(Icons.Default.PieChart, null) }, label = { Text(stringResource(R.string.summary)) })
                NavigationBarItem(selected = page == 3, onClick = { page = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.settings)) })
            }
        }
    ) { padding ->
        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 12 })) {
            when (page) {
                0 -> DashboardScreen(monthExpenses, expenses, thisMonthTotal, totalBudget, month, { page = 1 }, { editing = it; showEditor = true },
                    budgets, onMonthBack = { vm.setMonth(month.minusMonths(1)) }, onMonthForward = { vm.setMonth(month.plusMonths(1)) }, modifier = Modifier.padding(padding))
                1 -> ExpensesScreen(expenses, { editing = it; showEditor = true }, { deleteTarget = it }, modifier = Modifier.padding(padding))
                2 -> SummaryScreen(expenses, month, thisMonthTotal, previousTotal, budgets, totalBudget,
                    { vm.setMonth(month.minusMonths(1)) }, { vm.setMonth(month.plusMonths(1)) }, modifier = Modifier.padding(padding))
                else -> SettingsScreen(vm, totalBudget, budgets, { exportLauncher.launch("expense-tracker-backup.json") },
                    { importLauncher.launch(arrayOf("application/json", "text/json")) }, modifier = Modifier.padding(padding))
            }
        }
    }

    if (showEditor) ExpenseEditorDialog(editing, onDismiss = { showEditor = false }, onSave = {
        vm.save(it); showEditor = false
    })
    deleteTarget?.let { target ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = { TextButton(onClick = { vm.delete(target); deleteTarget = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun DashboardScreen(
    monthExpenses: List<Expense>, allExpenses: List<Expense>, total: Double, budget: Double, month: YearMonth,
    onSeeAll: () -> Unit, onEdit: (Expense) -> Unit, budgets: List<CategoryBudget>,
    onMonthBack: () -> Unit, onMonthForward: () -> Unit, modifier: Modifier = Modifier
) {
    val recent = allExpenses.sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.id }).take(5)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { MonthHeader(month, onMonthBack, onMonthForward) }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Text(stringResource(R.string.monthly_total), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f))
                    Text(money(total), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.budget_usage), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f))
                    val ratio = BudgetMath.usage(total, budget).coerceIn(0.0, 1.0).toFloat()
                    LinearProgressIndicator(progress = ratio, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color(0xFFE8BC62), trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = .22f))
                    if (budget > 0) Text("${(ratio * 100).toInt()}% · ${money(budget)}", color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(top = 8.dp))
                    BudgetNotice(total, budget)
                }
            }
        }
        item { SectionTitle(stringResource(R.string.recent_expenses), trailing = stringResource(R.string.expenses), onClick = onSeeAll) }
        if (recent.isEmpty()) item { EmptyState() }
        items(recent, key = { it.id }) { ExpenseRow(it, onClick = { onEdit(it) }) }
    }
}

@Composable
private fun ExpensesScreen(expenses: List<Expense>, onEdit: (Expense) -> Unit, onDelete: (Expense) -> Unit, modifier: Modifier = Modifier) {
    if (expenses.isEmpty()) EmptyState(modifier.fillMaxSize())
    else LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(expenses, key = { it.id }) { expense ->
            ExpenseRow(expense, onClick = { onEdit(expense) }, trailing = {
                IconButton(onClick = { onDelete(expense) }) { Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) }
            })
        }
    }
}

@Composable
private fun SummaryScreen(expenses: List<Expense>, month: YearMonth, total: Double, previous: Double,
                         budgets: List<CategoryBudget>, totalBudget: Double, back: () -> Unit, forward: () -> Unit,
                         modifier: Modifier = Modifier) {
    val current = expenses.filter { runCatching { YearMonth.from(LocalDate.parse(it.date)) }.getOrNull() == month }
    val grouped = categories.map { category -> category to current.filter { it.category == category }.sumOf { it.amount } }.filter { it.second > 0 }
    val change = BudgetMath.monthChange(total, previous)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { MonthHeader(month, back, forward) }
        item { Card(shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.monthly_total), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(money(total), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(when {
                change == null -> "—"
                change > 0 -> "${stringResource(R.string.increase)} ${change.toInt()}%"
                change < 0 -> "${stringResource(R.string.decrease)} ${kotlin.math.abs(change).toInt()}%"
                else -> stringResource(R.string.no_change)
            }, color = if ((change ?: 0.0) > 0) MaterialTheme.colorScheme.error else Color(0xFF24946D))
        } } }
        item { if (grouped.isNotEmpty()) DonutChart(grouped) else EmptyState() }
        items(grouped) { (category, amount) ->
            val idx = categories.indexOf(category).coerceAtLeast(0)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(categoryColors[idx]))
                Text(categoryLabel(category), Modifier.weight(1f).padding(start = 10.dp))
                Text(money(amount), fontWeight = FontWeight.SemiBold)
            }
            val limit = budgets.firstOrNull { it.category == category }?.monthlyLimit ?: 0.0
            if (limit > 0) BudgetNotice(amount, limit)
        }
        item { if (totalBudget > 0) BudgetNotice(total, totalBudget) }
    }
}

@Composable
private fun DonutChart(items: List<Pair<String, Double>>) {
    val sum = items.sumOf { it.second }.coerceAtLeast(0.01)
    Card(shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(170.dp)) {
                var start = -90f
                items.forEach { (cat, value) ->
                    val idx = categories.indexOf(cat).coerceAtLeast(0)
                    val sweep = (value / sum * 360f).toFloat()
                    drawArc(categoryColors[idx], start, sweep, false, style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt))
                    start += sweep
                }
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text("৳${sum.toLong()}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.monthly_total), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: ExpenseViewModel, totalBudget: Double, budgets: List<CategoryBudget>,
                          onExport: () -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier) {
    val language by vm.language.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    var budgetText by remember(totalBudget) { mutableStateOf(if (totalBudget > 0) totalBudget.toString() else "") }
    val budgetMap = budgets.associate { it.category to it.monthlyLimit }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle(stringResource(R.string.settings)) }
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.monthly_budget), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = budgetText, onValueChange = { budgetText = it }, label = { Text(stringResource(R.string.budget_amount)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = { budgetText.toDoubleOrNull()?.let(vm::setTotalBudget) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_budget)) }
                }
            }
        }
        item { SectionTitle(stringResource(R.string.category_budgets)) }
        items(categories) { category ->
            var text by remember(budgetMap[category]) { mutableStateOf(budgetMap[category]?.takeIf { it > 0 }?.toString() ?: "") }
            Card(shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryDot(category)
                    Text(categoryLabel(category), Modifier.weight(1f).padding(start = 8.dp))
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("৳") },
                        modifier = Modifier.width(120.dp), singleLine = true)
                    IconButton(onClick = { text.toDoubleOrNull()?.let { vm.setCategoryBudget(category, it) } }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.language), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = language == "en", onClick = { vm.setLanguage("en") }, label = { Text(stringResource(R.string.english)) })
                        FilterChip(selected = language == "bn", onClick = { vm.setLanguage("bn") }, label = { Text(stringResource(R.string.bengali)) })
                    }
                    Text(stringResource(R.string.theme), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("system", "light", "dark").forEach { option ->
                            FilterChip(selected = theme == option, onClick = { vm.setTheme(option) },
                                label = { Text(when(option) { "system" -> stringResource(R.string.system); "light" -> stringResource(R.string.light); else -> stringResource(R.string.dark) }) })
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileUpload, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.export_backup))
                    }
                    OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.import_backup))
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)\nprivate fun ExpenseEditorDialog(expense: Expense?, onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    var amount by remember(expense) { mutableStateOf(expense?.amount?.toString() ?: "") }
    var date by remember(expense) { mutableStateOf(expense?.date ?: LocalDate.now().toString()) }
    var category by remember(expense) { mutableStateOf(expense?.category ?: "Food") }
    var note by remember(expense) { mutableStateOf(expense?.note ?: "") }
    var error by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (expense == null) stringResource(R.string.add_expense) else stringResource(R.string.edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text(stringResource(R.string.amount)) }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(date, { date = it }, label = { Text(stringResource(R.string.date)) }, modifier = Modifier.weight(1f), singleLine = true)
                    IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, null) }
                }
                Text(stringResource(R.string.category), fontWeight = FontWeight.SemiBold)
                CategorySelector(category, { category = it })
                OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.note)) }, singleLine = true)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = {
            val value = amount.toDoubleOrNull()
            val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
            if (value == null || value <= 0) error = "Enter a positive amount"
            else if (parsed == null) error = "Enter a valid date (YYYY-MM-DD)"
            else onSave(Expense(expense?.id ?: 0, value, parsed.toString(), category, note.trim()))
        }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = runCatching { LocalDate.parse(date).toEpochDay() * 86400000 }.getOrNull())
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { date = LocalDate.ofEpochDay(it / 86400000).toString() }
                showDatePicker = false
            }) { Text(stringResource(R.string.save)) }
        }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }) {
            DatePicker(state)
        }
    }
}

@Composable
private fun CategorySelector(selected: String, onSelected: (String) -> Unit) {
    Column {
        categories.chunked(4).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                line.forEach { cat ->
                    FilterChip(selected = selected == cat, onClick = { onSelected(cat) },
                        label = { Text(categoryLabel(cat), fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                }
                repeat(4 - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, back: () -> Unit, forward: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = back) { Icon(Icons.Default.ChevronLeft, null) }
        Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        IconButton(onClick = forward) { Icon(Icons.Default.ChevronRight, null) }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryDot(expense.category)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(categoryLabel(expense.category), fontWeight = FontWeight.SemiBold)
                Text(if (expense.note.isBlank()) expense.date else "${expense.date} · ${expense.note}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Text(money(expense.amount), fontWeight = FontWeight.Bold)
            trailing()
        }
    }
}

@Composable
private fun CategoryDot(category: String) {
    val index = categories.indexOf(category).coerceAtLeast(0)
    Box(Modifier.size(38.dp).clip(CircleShape).background(categoryColors[index].copy(alpha = .15f)), contentAlignment = Alignment.Center) {
        Icon(when(category) {
            "Food" -> Icons.Default.Restaurant
            "Transport" -> Icons.Default.DirectionsBus
            "Bills" -> Icons.Default.Receipt
            "Shopping" -> Icons.Default.ShoppingBag
            "Entertainment" -> Icons.Default.Movie
            "Health" -> Icons.Default.MedicalServices
            else -> Icons.Default.Category
        }, contentDescription = null, tint = categoryColors[index], modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BudgetNotice(spent: Double, budget: Double) {
    val status = BudgetMath.status(spent, budget)
    if (status == BudgetStatus.WARNING || status == BudgetStatus.EXCEEDED) {
        Text(if (status == BudgetStatus.EXCEEDED) stringResource(R.string.over_budget) else stringResource(R.string.budget_warning),
            color = if (status == BudgetStatus.EXCEEDED) MaterialTheme.colorScheme.error else Color(0xFFE3A02D),
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String? = null, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (trailing != null && onClick != null) TextButton(onClick = onClick) { Text(trailing) }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(52.dp))
        Text(stringResource(R.string.empty_expenses), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun categoryLabel(category: String): String = when(category) {
    "Food" -> "Food"; "Transport" -> "Transport"; "Bills" -> "Bills"; "Shopping" -> "Shopping"
    "Entertainment" -> "Entertainment"; "Health" -> "Health"; else -> "Other"
}
private fun money(value: Double): String = "৳" + String.format(Locale.US, "%,.2f", value)
