package pt.ipp.estg.moveon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import pt.ipp.estg.moveon.data.local.AppDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit, onHistoryItemClick: (Long) -> Unit) {

    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // Ler distância total para o Menu Lateral
    val localTotalDistance by db.activityDao().getTotalDistance().collectAsState(initial = 0f)
    var selectedTab by remember { mutableIntStateOf(1) }
    val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Utilizador"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // --- MENU LATERAL (O TEU PERFIL COMPLETO) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(80.dp).background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(50.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Conta de:", color = Color.White.copy(0.8f), fontSize = 14.sp)
                        Text(userEmail, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsRun, null, tint = Color.White)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Distância Total", color = Color.White.copy(0.8f), fontSize = 12.sp)
                                    Text("%.2f km".format((localTotalDistance ?: 0f) / 1000), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("Ver Histórico") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.EmojiEvents, null) },
                    label = { Text("Rankings") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.weight(1f))
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, null) },
                    label = { Text("Sair") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onLogout() },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(

            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("MoveOn", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(icon = { Icon(Icons.Default.History, null) }, label = { Text("Histórico") }, selected = selectedTab == 0, onClick = { selectedTab = 0 })
                    NavigationBarItem(icon = { Icon(Icons.Default.DirectionsRun, null) }, label = { Text("Gravar") }, selected = selectedTab == 1, onClick = { selectedTab = 1 })
                    NavigationBarItem(icon = { Icon(Icons.Default.EmojiEvents, null) }, label = { Text("Ranking") }, selected = selectedTab == 2, onClick = { selectedTab = 2 })
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> HistoryScreen(onItemClick = onHistoryItemClick)
                    1 -> Dashboard(
                        onLogout = onLogout,
                        onProfileClick = { scope.launch { drawerState.open() } }
                    )
                    2 -> ProfileScreen(onLogout = onLogout)
                }
            }
        }
    }
}