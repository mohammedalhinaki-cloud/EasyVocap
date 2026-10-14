package com.example.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.detail.ListDetailScreen
import com.example.ui.detail.ListDetailViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.importexport.ImportScreen
import com.example.ui.importexport.ImportViewModel
import com.example.ui.quiz.QuizResultsScreen
import com.example.ui.quiz.QuizScreen
import com.example.ui.quiz.QuizViewModel
import com.example.ui.review.QuickReviewScreen
import com.example.ui.review.QuickReviewViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.words.AddEditWordScreen
import com.example.ui.words.AddEditWordViewModel
import com.example.ui.words.WordBoxScreen
import com.example.ui.words.WordBoxViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Home
        composable(Screen.Home.route) {
            val homeVm: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = homeVm,
                onListClick = { listId ->
                    navController.navigate(Screen.ListDetail.createRoute(listId))
                },
                onImportClick = {
                    navController.navigate(Screen.Import.createRoute())
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // 2. List Detail
        composable(
            route = Screen.ListDetail.route,
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: 0L
            val detailVm = viewModel { ListDetailViewModel(app, listId) }
            ListDetailScreen(
                listId = listId,
                viewModel = detailVm,
                onBackClick = { navController.popBackStack() },
                onStartQuiz = { filterType ->
                    navController.navigate(Screen.Quiz.createRoute(listId, filterType))
                },
                onQuickReview = { filterType ->
                    navController.navigate(Screen.QuickReview.createRoute(listId, filterType))
                },
                onOpenBox = { filterType ->
                    navController.navigate(Screen.WordBox.createRoute(listId, filterType))
                },
                onAddWordClick = {
                    navController.navigate(Screen.AddEditWord.createRoute(listId))
                },
                onImportClick = {
                    navController.navigate(Screen.Import.createRoute(listId))
                }
            )
        }

        // 3. Quiz Screen
        composable(
            route = Screen.Quiz.route,
            arguments = listOf(
                navArgument("listId") { type = NavType.LongType },
                navArgument("filterType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: 0L
            val filterType = backStackEntry.arguments?.getString("filterType") ?: "ALL"
            val quizVm = viewModel { QuizViewModel(app, listId, filterType) }

            QuizScreen(
                viewModel = quizVm,
                onQuizFinished = { tested, knew, forgot, durationSec ->
                    navController.navigate(Screen.QuizResults.createRoute(listId, tested, knew, forgot, durationSec)) {
                        popUpTo(Screen.ListDetail.createRoute(listId))
                    }
                },
                onExitClick = { navController.popBackStack() }
            )
        }

        // 4. Quiz Results Screen
        composable(
            route = Screen.QuizResults.route,
            arguments = listOf(
                navArgument("listId") { type = NavType.LongType },
                navArgument("tested") { type = NavType.IntType },
                navArgument("knew") { type = NavType.IntType },
                navArgument("forgot") { type = NavType.IntType },
                navArgument("durationSec") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: 0L
            val tested = backStackEntry.arguments?.getInt("tested") ?: 0
            val knew = backStackEntry.arguments?.getInt("knew") ?: 0
            val forgot = backStackEntry.arguments?.getInt("forgot") ?: 0
            val durationSec = backStackEntry.arguments?.getLong("durationSec") ?: 0L

            QuizResultsScreen(
                listId = listId,
                tested = tested,
                knew = knew,
                forgot = forgot,
                durationSec = durationSec,
                onReviewForgottenClick = {
                    navController.navigate(Screen.Quiz.createRoute(listId, "FORGOTTEN")) {
                        popUpTo(Screen.ListDetail.createRoute(listId))
                    }
                },
                onBackToListClick = {
                    navController.popBackStack(Screen.ListDetail.createRoute(listId), inclusive = false)
                }
            )
        }

        // 5. Quick Review Screen
        composable(
            route = Screen.QuickReview.route,
            arguments = listOf(
                navArgument("listId") { type = NavType.LongType },
                navArgument("filterType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: 0L
            val filterType = backStackEntry.arguments?.getString("filterType") ?: "ALL"
            val reviewVm = viewModel { QuickReviewViewModel(app, listId, filterType) }

            QuickReviewScreen(
                viewModel = reviewVm,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 6. Word Box Screen
        composable(
            route = Screen.WordBox.route,
            arguments = listOf(
                navArgument("listId") { type = NavType.LongType },
                navArgument("filterType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: 0L
            val filterType = backStackEntry.arguments?.getString("filterType") ?: "ALL"
            val boxVm = viewModel { WordBoxViewModel(app, listId, filterType) }

            WordBoxScreen(
                listId = listId,
                filterType = filterType,
                viewModel = boxVm,
                onBackClick = { navController.popBackStack() },
                onAddWordClick = { navController.navigate(Screen.AddEditWord.createRoute(listId)) },
                onEditWordClick = { wordId -> navController.navigate(Screen.AddEditWord.createRoute(listId, wordId)) },
                onStartQuizClick = { filter -> navController.navigate(Screen.Quiz.createRoute(listId, filter)) }
            )
        }

        // 7. Add / Edit Word Screen
        composable(
            route = Screen.AddEditWord.route,
            arguments = listOf(
                navArgument("listId") { type = NavType.LongType },
                navArgument("wordId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: 0L
            val wordIdArg = backStackEntry.arguments?.getLong("wordId") ?: -1L
            val wordId = if (wordIdArg > 0) wordIdArg else null
            val addEditVm = viewModel { AddEditWordViewModel(app, listId, wordId) }

            AddEditWordScreen(
                viewModel = addEditVm,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 8. Import Screen
        composable(
            route = Screen.Import.route,
            arguments = listOf(
                navArgument("listId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val listIdArg = backStackEntry.arguments?.getLong("listId") ?: -1L
            val listId = if (listIdArg > 0) listIdArg else null
            val importVm = viewModel { ImportViewModel(app, listId) }

            ImportScreen(
                viewModel = importVm,
                onBackClick = { navController.popBackStack() },
                onImportFinished = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }

        // 9. Settings Screen
        composable(Screen.Settings.route) {
            val settingsVm: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = settingsVm,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
