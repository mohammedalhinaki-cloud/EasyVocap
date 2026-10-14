package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    
    object ListDetail : Screen("list_detail/{listId}") {
        fun createRoute(listId: Long) = "list_detail/$listId"
    }

    object Quiz : Screen("quiz/{listId}/{filterType}") {
        fun createRoute(listId: Long, filterType: String) = "quiz/$listId/$filterType"
    }

    object QuizResults : Screen("quiz_results/{listId}/{tested}/{knew}/{forgot}/{durationSec}") {
        fun createRoute(listId: Long, tested: Int, knew: Int, forgot: Int, durationSec: Long) =
            "quiz_results/$listId/$tested/$knew/$forgot/$durationSec"
    }

    object QuickReview : Screen("quick_review/{listId}/{filterType}") {
        fun createRoute(listId: Long, filterType: String) = "quick_review/$listId/$filterType"
    }

    object WordBox : Screen("word_box/{listId}/{filterType}") {
        fun createRoute(listId: Long, filterType: String) = "word_box/$listId/$filterType"
    }

    object AddEditWord : Screen("add_edit_word/{listId}?wordId={wordId}") {
        fun createRoute(listId: Long, wordId: Long? = null): String {
            return if (wordId != null && wordId > 0) {
                "add_edit_word/$listId?wordId=$wordId"
            } else {
                "add_edit_word/$listId"
            }
        }
    }

    object Import : Screen("import?listId={listId}") {
        fun createRoute(listId: Long? = null): String {
            return if (listId != null && listId > 0) "import?listId=$listId" else "import"
        }
    }

    object Settings : Screen("settings")
}
