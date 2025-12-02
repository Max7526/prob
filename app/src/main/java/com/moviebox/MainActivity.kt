package com.moviebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.moviebox.ui.theme.MovieBoxTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A lightweight prototype for the MovieBox application described in the project brief.
 * The screen demonstrates the main use cases: popular movies, search results, favorites,
 * personal notes, ratings and watched flags. Networking and persistence layers are
 * represented by [MovieRepository] so they can be swapped for Retrofit/Room later.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MovieBoxTheme {
                val repository = remember { FakeMovieRepository() }
                val viewModel: MovieViewModel = viewModel(factory = MovieViewModelFactory(repository))
                MovieBoxApp(viewModel = viewModel)
            }
        }
    }
}

// Data + repository layer

data class Movie(
    val id: String,
    val title: String,
    val posterUrl: String,
    val overview: String,
    val rating: Double,
    val year: Int,
    val genres: List<String>,
    val runtimeMinutes: Int,
    val releaseDate: String,
    val country: String,
    val isFavorite: Boolean = false,
    val personalNote: String = "",
    val personalRating: Int = 0,
    val watched: Boolean = false
)

interface MovieRepository {
    fun popularMovies(): Flow<List<Movie>>
    fun searchMovies(query: String): Flow<List<Movie>>
    fun toggleFavorite(movieId: String)
    fun updateNote(movieId: String, note: String)
    fun updatePersonalRating(movieId: String, rating: Int)
    fun toggleWatched(movieId: String)
}

class FakeMovieRepository : MovieRepository {
    private val movies = MutableStateFlow(sampleMovies())

    override fun popularMovies(): Flow<List<Movie>> = movies

    override fun searchMovies(query: String): Flow<List<Movie>> {
        if (query.isBlank()) return flowOf(emptyList())
        return movies.combine(flowOf(query)) { list, q ->
            list.filter { it.title.contains(q, ignoreCase = true) }
        }
    }

    override fun toggleFavorite(movieId: String) {
        movies.update { current ->
            current.map { movie ->
                if (movie.id == movieId) movie.copy(isFavorite = !movie.isFavorite) else movie
            }
        }
    }

    override fun updateNote(movieId: String, note: String) {
        movies.update { current ->
            current.map { movie -> if (movie.id == movieId) movie.copy(personalNote = note) else movie }
        }
    }

    override fun updatePersonalRating(movieId: String, rating: Int) {
        movies.update { current ->
            current.map { movie -> if (movie.id == movieId) movie.copy(personalRating = rating.coerceIn(1, 5)) else movie }
        }
    }

    override fun toggleWatched(movieId: String) {
        movies.update { current ->
            current.map { movie -> if (movie.id == movieId) movie.copy(watched = !movie.watched) else movie }
        }
    }

    private fun sampleMovies(): List<Movie> = listOf(
        Movie(
            id = "1",
            title = "Interstellar",
            posterUrl = "https://image.tmdb.org/t/p/w500/nBNZadXqJSdt05SHLqgT0HuC5Gm.jpg",
            overview = "A team travels through a wormhole in search of a new home for humanity.",
            rating = 8.6,
            year = 2014,
            genres = listOf("Sci-Fi", "Adventure"),
            runtimeMinutes = 169,
            releaseDate = "2014-11-05",
            country = "USA"
        ),
        Movie(
            id = "2",
            title = "The Batman",
            posterUrl = "https://image.tmdb.org/t/p/w500/74xTEgt7R36Fpooo50r9T25onhq.jpg",
            overview = "Batman investigates a series of murders in Gotham City.",
            rating = 7.8,
            year = 2022,
            genres = listOf("Action", "Crime"),
            runtimeMinutes = 176,
            releaseDate = "2022-03-02",
            country = "USA"
        ),
        Movie(
            id = "3",
            title = "Spirited Away",
            posterUrl = "https://image.tmdb.org/t/p/w500/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg",
            overview = "A young girl enters a mysterious spirit world.",
            rating = 8.5,
            year = 2001,
            genres = listOf("Animation", "Fantasy"),
            runtimeMinutes = 125,
            releaseDate = "2001-07-20",
            country = "Japan"
        )
    )
}

// ViewModel layer

class MovieViewModel(private val repository: MovieRepository) : ViewModel() {
    val popularMovies: Flow<List<Movie>> = repository.popularMovies()
    private val _searchResults = MutableStateFlow<List<Movie>>(emptyList())
    val searchResults: Flow<List<Movie>> get() = _searchResults

    fun search(query: String) {
        viewModelScope.launch {
            repository.searchMovies(query).collect { results ->
                _searchResults.value = results
            }
        }
    }

    fun toggleFavorite(movie: Movie) = repository.toggleFavorite(movie.id)

    fun updateNote(movie: Movie, note: String) = repository.updateNote(movie.id, note)

    fun updatePersonalRating(movie: Movie, rating: Int) = repository.updatePersonalRating(movie.id, rating)

    fun toggleWatched(movie: Movie) = repository.toggleWatched(movie.id)
}

class MovieViewModelFactory(private val repository: MovieRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovieViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MovieViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// UI layer

private enum class Screen(val title: String) { Popular("Популярное"), Search("Поиск"), Favorites("Избранное") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieBoxApp(viewModel: MovieViewModel) {
    var screen by remember { mutableStateOf(Screen.Popular) }
    val topAppBarState = rememberTopAppBarState()
    val popular by viewModel.popularMovies.collectAsState(initial = emptyList())
    val searchResults by viewModel.searchResults.collectAsState(initial = emptyList())

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = screen.title) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 3.dp
            ) {
                Screen.values().forEach { target ->
                    val isSelected = target == screen
                    val label = if (isSelected) "• ${'$'}{target.title}" else target.title
                    Text(
                        text = label,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clickable { screen = target },
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.DarkGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    ) { innerPadding ->
        when (screen) {
            Screen.Popular -> PopularScreen(popular, innerPadding, viewModel::toggleFavorite)
            Screen.Search -> SearchScreen(innerPadding, searchResults, onSearch = viewModel::search, onToggleFavorite = viewModel::toggleFavorite)
            Screen.Favorites -> FavoritesScreen(popular.filter { it.isFavorite }, innerPadding, viewModel)
        }
    }
}

@Composable
private fun PopularScreen(movies: List<Movie>, padding: PaddingValues, onFavorite: (Movie) -> Unit) {
    MovieList(
        title = "Популярные фильмы",
        movies = movies,
        padding = padding,
        onFavorite = onFavorite
    )
}

@Composable
private fun SearchScreen(
    padding: PaddingValues,
    results: List<Movie>,
    onSearch: (String) -> Unit,
    onToggleFavorite: (Movie) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(padding).padding(16.dp)) {
        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Поиск фильма по названию") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onSearch(query) }) {
            Text("Найти")
        }
        Spacer(modifier = Modifier.height(8.dp))
        MovieList(title = "Результаты поиска", movies = results, padding = PaddingValues(), onFavorite = onToggleFavorite)
        if (query.isNotBlank() && results.isEmpty()) {
            Text(text = "Ничего не найдено", modifier = Modifier.padding(8.dp), color = Color.Red)
        }
    }
}

@Composable
private fun FavoritesScreen(favorites: List<Movie>, padding: PaddingValues, viewModel: MovieViewModel) {
    Column(modifier = Modifier.padding(padding)) {
        if (favorites.isEmpty()) {
            Text("Список избранного пуст", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favorites) { movie ->
                    FavoriteCard(
                        movie = movie,
                        onToggleFavorite = { viewModel.toggleFavorite(movie) },
                        onUpdateNote = { viewModel.updateNote(movie, it) },
                        onUpdateRating = { viewModel.updatePersonalRating(movie, it) },
                        onToggleWatched = { viewModel.toggleWatched(movie) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieList(title: String, movies: List<Movie>, padding: PaddingValues, onFavorite: (Movie) -> Unit) {
    Column(modifier = Modifier.padding(padding)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(movies) { movie ->
                MovieCard(movie = movie, onFavorite = { onFavorite(movie) })
            }
        }
    }
}

@Composable
private fun MovieCard(movie: Movie, onFavorite: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.height(140.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(movie.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Год: ${'$'}{movie.year} • Рейтинг TMDB: ${'$'}{movie.rating}")
                Text("Жанры: ${'$'}{movie.genres.joinToString()}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Длительность: ${'$'}{movie.runtimeMinutes} мин")
                Text(movie.overview, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = onFavorite) {
                        Text(if (movie.isFavorite) "Убрать из избранного" else "В избранное")
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    movie: Movie,
    onToggleFavorite: () -> Unit,
    onUpdateNote: (String) -> Unit,
    onUpdateRating: (Int) -> Unit,
    onToggleWatched: () -> Unit
) {
    var note by remember(movie.id) { mutableStateOf(movie.personalNote) }
    var ratingInput by remember(movie.id) { mutableStateOf(movie.personalRating.takeIf { it > 0 }?.toString() ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.height(120.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(movie.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Личная оценка: ${'$'}{if (movie.personalRating > 0) movie.personalRating else "нет"}")
                    Text("Просмотрено: ${'$'}{if (movie.watched) "да" else "нет"}")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = ratingInput,
                    onValueChange = { ratingInput = it.filter { ch -> ch.isDigit() }.take(1) },
                    label = { Text("Оценка 1-5") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    ratingInput.toIntOrNull()?.let(onUpdateRating)
                }) { Text("Сохранить оценку") }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onUpdateNote(note) }) { Text("Сохранить заметку") }
                Button(onClick = onToggleWatched) { Text(if (movie.watched) "Пометить как не просмотрено" else "Пометить как просмотрено") }
                Button(onClick = onToggleFavorite) { Text("Удалить из избранного") }
            }
        }
    }
}
