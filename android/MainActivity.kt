package com.example.grocerylist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.grocerylist.ui.theme.GroceryListTheme
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GroceryListTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SmartCartApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SmartCartApp(modifier: Modifier = Modifier) {
    var itemsList by remember { mutableStateOf(listOf<JSONObject>()) }
    var statusText by remember { mutableStateOf("Загрузка...") }
    var budgetText by remember { mutableStateOf("") }
    var newBudget by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("") }
    var shopList by remember { mutableStateOf(listOf<String>()) }
    var selectedShop by remember { mutableStateOf("Не выбран") }
    var catList by remember { mutableStateOf(listOf<String>()) }
    var selectedCat by remember { mutableStateOf("Без категории") }
    var catExpanded by remember { mutableStateOf(false) }
    var filterShop by remember { mutableStateOf("") }
    var filterShopExpanded by remember { mutableStateOf(false) }
    var filterCat by remember { mutableStateOf("") }
    var filterCatExpanded by remember { mutableStateOf(false) }
    var showCatManager by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var showListManager by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var renameListId by remember { mutableStateOf(0) }
    var renameListName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var allLists by remember { mutableStateOf(listOf<JSONObject>()) }
    var activeListName by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("") }
    var sortExpanded by remember { mutableStateOf(false) }
    var shopExpanded by remember { mutableStateOf(false) }

    fun loadItems() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://app.cartplan.ru/items?shop=$filterShop&category=$filterCat&sort=$sortMode")
                    .build()
                val response = client.newCall(request).execute()
                val json = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(json)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until jsonArray.length()) {
                    list.add(jsonArray.getJSONObject(i))
                }
                val budgetRequest = Request.Builder()
                    .url("https://app.cartplan.ru/budget")
                    .build()
                val budgetResponse = client.newCall(budgetRequest).execute()
                val budgetJson = JSONObject(budgetResponse.body?.string() ?: "{}")
                val budgetVal = budgetJson.getDouble("budget")
                val spentVal = budgetJson.getDouble("spent")
                val remainingVal = budgetJson.getDouble("remaining")
                val shopsRequest = Request.Builder()
                    .url("https://app.cartplan.ru/shops/list")
                    .build()
                val shopsResponse = client.newCall(shopsRequest).execute()
                val shopsJson = JSONArray(shopsResponse.body?.string() ?: "[]")
                val shopsTemp = mutableListOf<String>()
                for (i in 0 until shopsJson.length()) {
                    shopsTemp.add(shopsJson.getString(i))
                }
                val catsRequest = Request.Builder()
                    .url("https://app.cartplan.ru/categories/list")
                    .build()
                val catsResponse = client.newCall(catsRequest).execute()
                val catsJson = JSONArray(catsResponse.body?.string() ?: "[]")
                val catsTemp = mutableListOf<String>()
                                for (i in 0 until catsJson.length()) {
                    catsTemp.add(catsJson.getString(i))
                }
                val listsRequest = Request.Builder()
                    .url("https://app.cartplan.ru/lists")
                    .build()
                val listsResponse = client.newCall(listsRequest).execute()
                val listsJson = JSONArray(listsResponse.body?.string() ?: "[]")
                val listsTemp = mutableListOf<JSONObject>()
                var activeName = ""
                for (i in 0 until listsJson.length()) {
                    val obj = listsJson.getJSONObject(i)
                    listsTemp.add(obj)
                    if (obj.getInt("is_active") == 1) {
                        activeName = obj.getString("name")
                    }
                }
                withContext(Dispatchers.Main) {
                    itemsList = list
                    statusText = "Готово (${list.size} товаров)"
                    budgetText = "💰 Бюджет: $budgetVal | Потрачено: $spentVal | Остаток: $remainingVal"
                    shopList = shopsTemp
                    catList = catsTemp
                    allLists = listsTemp
                    activeListName = activeName
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText = "Ошибка: ${e.message}"
                }
            }
        }
    }

    LaunchedEffect(Unit) { loadItems() }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "SmartCart", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = statusText)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = budgetText, modifier = Modifier.weight(1f))
            IconButton(onClick = { showBudgetDialog = true }) {
                Text("💰", style = MaterialTheme.typography.titleLarge)
            }
        }
        if (showBudgetDialog) {
            AlertDialog(
                onDismissRequest = { showBudgetDialog = false },
                title = { Text("Изменить бюджет") },
                text = {
                    OutlinedTextField(
                        value = newBudget,
                        onValueChange = { newBudget = it },
                        label = { Text("Новый бюджет") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newBudget.isNotBlank()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val client = OkHttpClient()
                                    val form = FormBody.Builder()
                                        .add("budget", newBudget)
                                        .build()
                                    val request = Request.Builder()
                                        .url("https://app.cartplan.ru/set_budget")
                                        .post(form)
                                        .build()
                                    client.newCall(request).execute()
                                    withContext(Dispatchers.Main) {
                                        newBudget = ""
                                        showBudgetDialog = false
                                        loadItems()
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                    }) {
                        Text("Сохранить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBudgetDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { filterShopExpanded = true }) {
                    Text("🏪", style = MaterialTheme.typography.titleLarge)
                }
                DropdownMenu(
                    expanded = filterShopExpanded,
                    onDismissRequest = { filterShopExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Все магазины") },
                        onClick = { filterShop = ""; filterShopExpanded = false; loadItems() }
                    )
                    shopList.forEach { shop ->
                        DropdownMenuItem(
                            text = { Text(shop) },
                            onClick = { filterShop = shop; filterShopExpanded = false; loadItems() }
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { filterCatExpanded = true }) {
                    Text("📂", style = MaterialTheme.typography.titleLarge)
                }
                DropdownMenu(
                    expanded = filterCatExpanded,
                    onDismissRequest = { filterCatExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Все категории") },
                        onClick = { filterCat = ""; filterCatExpanded = false; loadItems() }
                    )
                    catList.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { filterCat = cat; filterCatExpanded = false; loadItems() }
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { sortExpanded = true }) {
                    Text("⇅", style = MaterialTheme.typography.titleLarge)
                }
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Без сортировки") },
                        onClick = { sortMode = ""; sortExpanded = false; loadItems() }
                    )
                    DropdownMenuItem(
                        text = { Text("Сначала дорогие") },
                        onClick = { sortMode = "cost_desc"; sortExpanded = false; loadItems() }
                    )
                    DropdownMenuItem(
                        text = { Text("Сначала дешёвые") },
                        onClick = { sortMode = "cost_asc"; sortExpanded = false; loadItems() }
                    )
                }
            }
            Box {
                IconButton(onClick = { showCatManager = true }) {
                    Text("⚙️", style = MaterialTheme.typography.titleLarge)
                }
            }
            Box {
                IconButton(onClick = { showListManager = true }) {
                    Text("📋", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { showAddForm = !showAddForm }) {
            Text(if (showAddForm) "➖ Скрыть форму" else "➕ Добавить товар")
        }
        Spacer(modifier = Modifier.height(4.dp))

        if (showAddForm) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newName, onValueChange = { newName = it },
                    label = { Text("Название") }, modifier = Modifier.weight(1.5f))
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedTextField(value = newPrice, onValueChange = { newPrice = it },
                    label = { Text("Цена") }, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedTextField(value = newQty, onValueChange = { newQty = it },
                    label = { Text("Кол-во") }, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                OutlinedButton(onClick = { shopExpanded = true }) {
                    Text("🏪 $selectedShop")
                }
                DropdownMenu(
                    expanded = shopExpanded,
                    onDismissRequest = { shopExpanded = false }
                ) {
                    shopList.forEach { shop ->
                        DropdownMenuItem(
                            text = { Text(shop) },
                            onClick = {
                                selectedShop = shop
                                shopExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box {
                OutlinedButton(onClick = { catExpanded = true }) {
                    Text("📂 $selectedCat")
                }
                DropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false }
                ) {
                    catList.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCat = cat
                                catExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = {
                if (newName.isNotBlank() && newPrice.isNotBlank() && newQty.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val client = OkHttpClient()
                            val form = FormBody.Builder()
                                .add("name", newName)
                                .add("price", newPrice)
                                .add("quantity", newQty)
                                .add("shop", selectedShop)
                                .add("category", selectedCat)
                                .build()
                            val request = Request.Builder()
                                .url("https://app.cartplan.ru/add")
                                .post(form)
                                .build()
                            client.newCall(request).execute()
                            withContext(Dispatchers.Main) {
                                newName = ""
                                newPrice = ""
                                newQty = ""
                                loadItems()
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }) {
                Text("Добавить")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(itemsList) { item ->
                var inputText by remember { mutableStateOf("") }
                val isPurchased = item.getInt("is_purchased") == 1
                val itemId = item.getInt("id")

                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = item.getString("name"), style = MaterialTheme.typography.titleMedium)
                        Text(text = "План: ${item.getDouble("price")} × ${item.getDouble("quantity")} = ${item.getDouble("planned_cost")} руб")
                        Text(text = "🏪 ${item.optString("shop", "Не выбран")}")
                        Text(text = "📂 ${item.optString("category", "Без категории")}")

                        if (!isPurchased) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    label = { Text("Факт. цена") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    val actual = inputText.toDoubleOrNull() ?: 0.0
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val client = OkHttpClient()
                                            val json = JSONObject().put("actual_cost", actual)
                                            val body = RequestBody.create("application/json".toMediaType(), json.toString())
                                            val request = Request.Builder()
                                                .url("https://app.cartplan.ru/purchase/$itemId")
                                                .post(body)
                                                .build()
                                            client.newCall(request).execute()
                                            loadItems()
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                }) { Text("Куплено") }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val client = OkHttpClient()
                                            val request = Request.Builder()
                                                .url("https://app.cartplan.ru/delete/$itemId")
                                                .post(RequestBody.create(null, ByteArray(0)))
                                                .build()
                                            client.newCall(request).execute()
                                            loadItems()
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                }) { Text("🗑") }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "✅ Куплено за ${item.getDouble("actual_cost")} руб",
                                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                Button(onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val client = OkHttpClient()
                                            val request = Request.Builder()
                                                .url("https://app.cartplan.ru/delete/$itemId")
                                                .post(RequestBody.create(null, ByteArray(0)))
                                                .build()
                                            client.newCall(request).execute()
                                            loadItems()
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                }) { Text("🗑") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCatManager) {
        AlertDialog(
            onDismissRequest = { showCatManager = false },
            title = { Text("Управление категориями") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCatName,
                            onValueChange = { newCatName = it },
                            label = { Text("Новая категория") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (newCatName.isNotBlank()) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val client = OkHttpClient()
                                        val form = FormBody.Builder()
                                            .add("name", newCatName)
                                            .build()
                                        val request = Request.Builder()
                                            .url("https://app.cartplan.ru/categories/add")
                                            .post(form)
                                            .build()
                                        client.newCall(request).execute()
                                        withContext(Dispatchers.Main) {
                                            newCatName = ""
                                            loadItems()
                                        }
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            }
                        }) {
                            Text("Добавить")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    catList.forEach { cat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cat, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val client = OkHttpClient()
                                        val form = FormBody.Builder()
                                            .add("name", cat)
                                            .build()
                                        val request = Request.Builder()
                                            .url("https://app.cartplan.ru/categories/delete_by_name")
                                            .post(form)
                                            .build()
                                        client.newCall(request).execute()
                                        withContext(Dispatchers.Main) {
                                            loadItems()
                                        }
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            }) {
                                Text("🗑")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCatManager = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
        if (showListManager) {
            AlertDialog(
                onDismissRequest = { showListManager = false },
                title = { Text("Списки покупок") },
                text = {
                    Column {
                        Text("Активный: $activeListName")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = newListName, onValueChange = { newListName = it },
                                label = { Text("Новый список") }, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                if (newListName.isNotBlank()) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val client = OkHttpClient()
                                            val form = FormBody.Builder().add("name", newListName).build()
                                            val request = Request.Builder()
                                                .url("https://app.cartplan.ru/lists/create")
                                                .post(form).build()
                                            client.newCall(request).execute()
                                            withContext(Dispatchers.Main) { newListName = ""; loadItems() }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                }
                            }) { Text("Создать") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        allLists.forEach { lst ->
                            val isActive = lst.getInt("is_active") == 1
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isActive) {
                                    Text("✅ ${lst.getString("name")}", modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.primary)
                                } else {
                                    TextButton(onClick = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val client = OkHttpClient()
                                                val request = Request.Builder()
                                                    .url("https://app.cartplan.ru/lists/set_active/${lst.getInt("id")}")
                                                    .post(RequestBody.create(null, ByteArray(0))).build()
                                                client.newCall(request).execute()
                                                withContext(Dispatchers.Main) { showListManager = false; loadItems() }
                                            } catch (e: Exception) { e.printStackTrace() }
                                        }
                                    }) { Text(lst.getString("name")) }
                                    IconButton(onClick = {
                                        renameListId = lst.getInt("id")
                                        renameListName = lst.getString("name")
                                        showRenameDialog = true
                                    }) {
                                        Text("✏️")
                                    }
                                    IconButton(onClick = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val client = OkHttpClient()
                                                val request = Request.Builder()
                                                    .url("https://app.cartplan.ru/lists/delete/${lst.getInt("id")}")
                                                    .post(RequestBody.create(null, ByteArray(0))).build()
                                                client.newCall(request).execute()
                                                withContext(Dispatchers.Main) { loadItems() }
                                            } catch (e: Exception) { e.printStackTrace() }
                                        }
                                    }) { Text("🗑") }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showListManager = false }) { Text("Закрыть") }
                }
            )
        }
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Переименовать список") },
            text = {
                OutlinedTextField(
                    value = renameListName,
                    onValueChange = { renameListName = it },
                    label = { Text("Новое имя") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameListName.isNotBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val client = OkHttpClient()
                                val form = FormBody.Builder()
                                    .add("id", renameListId.toString())
                                    .add("name", renameListName)
                                    .build()
                                val request = Request.Builder()
                                    .url("https://app.cartplan.ru/lists/rename")
                                    .post(form)
                                    .build()
                                client.newCall(request).execute()
                                withContext(Dispatchers.Main) {
                                    showRenameDialog = false
                                    loadItems()
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
    }