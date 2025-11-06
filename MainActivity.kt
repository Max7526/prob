package com.example.weather

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var temperatureText: TextView
    private lateinit var humidityText: TextView
    private lateinit var windSpeedText: TextView
    private lateinit var pressureText: TextView
    private lateinit var visibilityText: TextView
    private lateinit var feelsLikeText: TextView
    private lateinit var uvIndexText: TextView
    private lateinit var gpsButton: Button
    private lateinit var errorText: TextView
    private lateinit var weatherApiService: WeatherApiService
    private var isLocationRequestInProgress = false

    // Инициализация requestPermissionLauncher
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            // Если разрешение получено, получаем местоположение
            getWeatherForCurrentLocation()
        } else {
            Toast.makeText(this, "Разрешение на доступ к геолокации не получено", Toast.LENGTH_SHORT).show()
            errorText.text = "Ошибка: Разрешение на доступ к геолокации не получено"
            resetLocationRequestState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация элементов UI
        temperatureText = findViewById(R.id.temperatureText)
        humidityText = findViewById(R.id.humidityText)
        windSpeedText = findViewById(R.id.windSpeedText)
        pressureText = findViewById(R.id.pressureText)
        visibilityText = findViewById(R.id.visibilityText)
        feelsLikeText = findViewById(R.id.feelsLikeText)
        uvIndexText = findViewById(R.id.uvIndexText)
        gpsButton = findViewById(R.id.gpsButton)
        errorText = findViewById(R.id.errorText)

        // Инициализация Retrofit для получения данных о погоде
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.weatherapi.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        weatherApiService = retrofit.create(WeatherApiService::class.java)

        gpsButton.setOnClickListener {
            if (isLocationRequestInProgress) {
                Toast.makeText(this, "Запрос уже выполняется", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                isLocationRequestInProgress = true
                gpsButton.isEnabled = false
                checkLocationPermission()
            } catch (e: Exception) {
                resetLocationRequestState()
                Log.e("MainActivity", "Error in checkLocationPermission: ${e.message}")
                errorText.text = "Ошибка: ${e.message}"
            }
        }
    }

    private fun checkLocationPermission() {
        try {
            // Проверяем, есть ли разрешение на доступ к геолокации
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                    // Если разрешение есть, получаем местоположение
                    getWeatherForCurrentLocation()
                }
                else -> {
                    // Если разрешение нет, запрашиваем его
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in checkLocationPermission: ${e.message}")
            errorText.text = "Ошибка: ${e.message}"
            resetLocationRequestState()
        }
    }

    private fun getWeatherForCurrentLocation() {
        try {
            val location = getCurrentLocation()
            if (location != null) {
                fetchWeatherData(location)
            } else {
                Toast.makeText(this, "Не удалось определить местоположение", Toast.LENGTH_SHORT).show()
                errorText.text = "Ошибка: Не удалось определить местоположение"
                resetLocationRequestState()
            }
        } catch (e: SecurityException) {
            // Обрабатываем ошибку, если разрешение на геолокацию не было предоставлено
            Toast.makeText(this, "Ошибка доступа к геолокации: ${e.message}", Toast.LENGTH_LONG).show()
            errorText.text = "Ошибка: ${e.message}"
            Log.e("LocationError", "SecurityException: ${e.message}")
            resetLocationRequestState()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in getWeatherForCurrentLocation: ${e.message}")
            errorText.text = "Ошибка: ${e.message}"
            resetLocationRequestState()
        }
    }

    private fun getCurrentLocation(): String? {
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            // Проверяем разрешения на доступ к геолокации
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                Log.d("Location", "Location: $location")  // Логируем местоположение
                return location?.let { "${it.latitude},${it.longitude}" }
            } else {
                errorText.text = "Ошибка: Разрешение на геолокацию не предоставлено"
                resetLocationRequestState()
                return null
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in getCurrentLocation: ${e.message}")
            errorText.text = "Ошибка: ${e.message}"
            resetLocationRequestState()
            return null
        }
    }

    private fun fetchWeatherData(location: String) {
        try {
            // Получаем данные о погоде через API
            val call = weatherApiService.getWeather(location, "de7e08d5708e480bb1b141754252110")
            call.enqueue(object : Callback<Weather> {
                override fun onResponse(call: Call<Weather>, response: Response<Weather>) {
                    if (response.isSuccessful) {
                        val weather = response.body()
                        if (weather != null) {
                            // Отображаем данные о погоде
                            Log.d("WeatherAPI", "Weather Icon: ${weather.weatherIcon}")  // Логируем иконку погоды
                            temperatureText.text = "Температура: ${weather.temperature}°C"
                            humidityText.text = "Влажность: ${weather.humidity}%"
                            windSpeedText.text = "Скорость ветра: ${weather.windSpeed} км/ч"
                            pressureText.text = "Давление: ${weather.pressure} мбар"
                            visibilityText.text = "Видимость: ${weather.visibility} км"
                            feelsLikeText.text = "Ощущаемая температура: ${weather.feelsLike}°C"
                            uvIndexText.text = "Индекс ультрафиолетового излучения: ${weather.uvIndex}"

                            // Загружаем иконку погоды с помощью Glide
                            val iconUrl = if (weather.weatherIcon != null) {
                                "https://cdn.weatherapi.com/weather/64x64/day/${weather.weatherIcon}.png"
                            } else {
                                "https://cdn.weatherapi.com/weather/64x64/day/default.png" // Заглушка, если иконка отсутствует
                            }

                        }
                    } else {
                        errorText.text = "Ошибка: ${response.code()} - ${response.message()}"
                        Log.e("WeatherAPI", "Response error: ${response.message()}")
                    }
                    resetLocationRequestState()
                }

                override fun onFailure(call: Call<Weather>, t: Throwable) {
                    errorText.text = "Ошибка при загрузке данных: ${t.message}"
                    Log.e("WeatherAPI", "Failure: ${t.message}")
                    resetLocationRequestState()
                }
            })
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in fetchWeatherData: ${e.message}")
            errorText.text = "Ошибка: ${e.message}"
            resetLocationRequestState()
        }
    }

    private fun resetLocationRequestState() {
        isLocationRequestInProgress = false
        gpsButton.isEnabled = true
    }

    // Парсинг восхода и заката
    private fun formatSunTime(sunTime: String?): String {
        if (sunTime.isNullOrEmpty()) {
            return "Не доступно"
        }
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(sunTime)
        return outputFormat.format(date)
    }
}
