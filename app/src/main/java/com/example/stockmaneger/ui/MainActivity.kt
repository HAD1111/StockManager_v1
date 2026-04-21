package com.example.stockmaneger

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.stockmaneger.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect to activity_main.xml
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Connect NavController to the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Connect sidebar navigation to NavController
        NavigationUI.setupWithNavController(binding.navigationView, navController)

        // Open/close sidebar when nav items are tapped
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard  -> navController.navigate(R.id.dashboardFragment)
                R.id.nav_sales      -> navController.navigate(R.id.salesFragment)
                R.id.nav_customers  -> navController.navigate(R.id.customersFragment)
                R.id.nav_stock      -> navController.navigate(R.id.stockFragment)
                R.id.nav_account    -> navController.navigate(R.id.accountFragment)
                R.id.nav_reports    -> navController.navigate(R.id.reportsFragment)
            }
            binding.drawerLayout.closeDrawers()
            true
        }
    }
}