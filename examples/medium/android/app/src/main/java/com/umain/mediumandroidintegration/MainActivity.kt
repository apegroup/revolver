package com.umain.mediumandroidintegration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import com.umain.mediumandroidintegration.auth.AuthScreen
import com.umain.mediumandroidintegration.auth.AuthViewModel
import com.umain.mediumandroidintegration.cart.CartScreen
import com.umain.mediumandroidintegration.cart.CartViewModel
import com.umain.mediumandroidintegration.fake.FakeAuthRepository
import com.umain.mediumandroidintegration.fake.FakeCheckoutService
import com.umain.mediumandroidintegration.fake.FakeSearchRepository
import com.umain.mediumandroidintegration.search.SearchScreen
import com.umain.mediumandroidintegration.search.SearchViewModel

/**
 * Single activity that hosts three independent Revolver-powered screens via a
 * Material 3 [NavigationBar].
 *
 * Each ViewModel is created once and survives tab switches because they are held
 * as activity-scoped properties rather than inside composables.
 */
class MainActivity : ComponentActivity() {

    // ViewModels are activity-scoped so state survives tab navigation.
    private val authViewModel = AuthViewModel(FakeAuthRepository())
    private val searchViewModel = SearchViewModel(FakeSearchRepository())
    private val cartViewModel = CartViewModel(FakeCheckoutService())

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                var selectedTab by remember { mutableIntStateOf(0) }

                val tabs = listOf(
                    Tab("Auth", Icons.Default.Person),
                    Tab("Search", Icons.Default.Search),
                    Tab("Cart", Icons.Default.ShoppingCart),
                )

                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Revolver — Medium Example") })
                    },
                    bottomBar = {
                        NavigationBar {
                            tabs.forEachIndexed { idx, tab ->
                                NavigationBarItem(
                                    selected = selectedTab == idx,
                                    onClick = { selectedTab = idx },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    when (selectedTab) {
                        0 -> AuthScreen(viewModel = authViewModel, modifier = Modifier.padding(padding))
                        1 -> SearchScreen(viewModel = searchViewModel, modifier = Modifier.padding(padding))
                        2 -> CartScreen(viewModel = cartViewModel, modifier = Modifier.padding(padding))
                    }
                }
            }
        }
    }
}

private data class Tab(val label: String, val icon: ImageVector)
