# Разработка Nothing Glyph Toy

Руководство по созданию приложений для Glyph Matrix дисплея Nothing Phone (2a).

## Содержание
1. [Введение](#1-введение)
2. [Настройка проекта](#2-настройка-проекта)
3. [Архитектура Glyph Toy](#3-архитектура-glyph-toy)
4. [Ключевые компоненты SDK](#4-ключевые-компоненты-sdk)
5. [Паттерны реализации](#5-паттерны-реализации)
6. [Пример: Минимальный Glyph Toy](#6-пример-минимальный-glyph-toy)
7. [Пример: Игра со входом от сенсоров](#7-пример-игра-со-входом-от-сенсоров)
8. [Референс: Flappy Box](#8-референс-flappy-box)

---

## 1. Введение

### Что такое Glyph Toy

Glyph Toy — это мини-приложение, которое отображается на LED-матрице задней панели Nothing Phone (2a). Toys работают как фоновые сервисы и управляются системным Glyph Composer.

### Характеристики дисплея

- **Размер**: 25x25 пикселей (625 LED)
- **Яркость**: от 0 до 4095 (12-bit)
- **Частота обновления**: рекомендуется 20 FPS (50ms на кадр)
- **Адресация**: линейный массив `IntArray(625)`, где `index = y * 25 + x`

### Официальный SDK

Документация и примеры:
https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit

---

## 2. Настройка проекта

### Подключение SDK

SDK поставляется как AAR-файл. Добавьте его в проект:

```
libs/glyph-matrix-sdk-2.0.aar
```

### Зависимости в build.gradle.kts

```kotlin
dependencies {
    implementation(files("../libs/glyph-matrix-sdk-2.0.aar"))
    // Для OrientationProvider (опционально)
    implementation(libs.play.services.location)
}
```

### Permissions в AndroidManifest.xml

```xml
<uses-permission android:name="com.nothing.ketchum.permission.ENABLE" />
```

---

## 3. Архитектура Glyph Toy

### Service-based архитектура

Glyph Toy реализуется как Android Service с привязкой через Messenger:

```kotlin
class GlyphToyService : Service() {

    private val serviceHandler = Handler(Looper.getMainLooper()) {
        when (it.what) {
            GlyphToy.MSG_GLYPH_TOY -> {
                val event = it.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                when (event) {
                    GlyphToy.EVENT_CHANGE -> onLongPress()
                    GlyphToy.EVENT_AOD -> onAodEvent()
                }
                true
            }
            else -> false
        }
    }
    private val serviceMessenger = Messenger(serviceHandler)

    override fun onBind(intent: Intent?): IBinder {
        init()
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clear()
        return false
    }
}
```

### Регистрация Toy через meta-data

В `AndroidManifest.xml` сервис должен быть зарегистрирован с необходимыми meta-data:

```xml
<service
    android:name=".GlyphToyService"
    android:exported="true">

    <!-- Intent-filter для обнаружения системой -->
    <intent-filter>
        <action android:name="com.nothing.glyph.TOY"/>
    </intent-filter>

    <!-- Название toy (отображается в Glyph Composer) -->
    <meta-data
        android:name="com.nothing.glyph.toy.name"
        android:resource="@string/toy_name"/>

    <!-- Иконка toy (GIF или PNG) -->
    <meta-data
        android:name="com.nothing.glyph.toy.image"
        android:resource="@drawable/icon"/>

    <!-- Краткое описание -->
    <meta-data
        android:name="com.nothing.glyph.toy.summary"
        android:resource="@string/toy_summary"/>

    <!-- Activity для подробной информации (опционально) -->
    <meta-data
        android:name="com.nothing.glyph.toy.introduction"
        android:value="com.example.ToyIntroductionActivity"/>

    <!-- Поддержка long press: 1 = включено -->
    <meta-data
        android:name="com.nothing.glyph.toy.longpress"
        android:value="1"/>

    <!-- Поддержка AOD: 1 = включено -->
    <meta-data
        android:name="com.nothing.glyph.toy.aod_support"
        android:value="1"/>
</service>
```

### Жизненный цикл

```
onBind() → init() → [render loop] → onUnbind() → clear()
```

1. **init()** — инициализация GlyphMatrixManager, запуск game loop
2. **render loop** — обновление состояния и отрисовка кадров
3. **clear()** — остановка loop, освобождение ресурсов

---

## 4. Ключевые компоненты SDK

### GlyphMatrixManager

Центральный класс для управления дисплеем:

```kotlin
val manager = GlyphMatrixManager.getInstance(context)

// Инициализация с callback
manager.init(object : GlyphMatrixManager.Callback {
    override fun onServiceConnected(name: ComponentName?) {
        // Дисплей готов к использованию
        manager.setGlyphMatrixTimeout(false) // Отключить автовыключение
    }
    override fun onServiceDisconnected(name: ComponentName?) {
        // Соединение потеряно
    }
})

// Отображение кадра
manager.setMatrixFrame(frame: GlyphMatrixFrame)
manager.setMatrixFrame(matrix: IntArray) // прямой массив 25x25

// Очистка
manager.turnOff()
manager.closeAppMatrix()
manager.unInit()
```

### GlyphMatrixFrame

Фрейм с поддержкой слоёв. Слои рендерятся в порядке: Low → Mid → Top

```kotlin
val frame = GlyphMatrixFrame.Builder()
    .addLow(backgroundMatrix)   // Нижний слой (фон)
    .addMid(gameObjectsMatrix)  // Средний слой (игровые объекты)
    .addTop(textObject)         // Верхний слой (UI, текст)
    .build(context)
```

### GlyphMatrixObject

Объекты для добавления на слои:

```kotlin
// Текст
val scoreText = GlyphMatrixObject.Builder()
    .setText("42")
    .setPosition(10, 19)     // x, y
    .setScale(1)             // масштаб шрифта
    .setBrightness(50)       // яркость 0-4095
    .build()

// Изображение
val sprite = GlyphMatrixObject.Builder()
    .setImageSource(bitmap)
    .setPosition(5, 5)
    .setOrientation(90)      // поворот: 0, 90, 180, 270
    .build()

// Сырой массив
val customMatrix = GlyphMatrixObject.Builder()
    .setRawArray(intArrayOf(...))
    .build()
```

---

## 5. Паттерны реализации

### Game Loop (Handler + postDelayed)

Основной паттерн для игрового цикла:

```kotlin
class GameLoop(
    private val onUpdate: (deltaTime: Float) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val frameDelayMs = 50L  // 20 FPS
    private val deltaTime = frameDelayMs / 1000f

    private val updateRunnable = object : Runnable {
        override fun run() {
            onUpdate(deltaTime)
            handler.postDelayed(this, frameDelayMs)
        }
    }

    fun start() = handler.post(updateRunnable)
    fun stop() = handler.removeCallbacks(updateRunnable)
}
```

Использование:

```kotlin
private val gameLoop = GameLoop { deltaTime ->
    updateGameState(deltaTime)
    renderFrame()
}

// В onServiceConnected:
gameLoop.start()

// В clear():
gameLoop.stop()
```

### Sensor Input (Gyroscope через Flow)

Получение данных с гироскопа через Kotlin Flow:

```kotlin
data class SensorData(val x: Float, val y: Float, val z: Float)

class GyroSensorController(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val sensorFlow: Flow<SensorData> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    trySend(SensorData(it.values[0], it.values[1], it.values[2]))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
```

Обработка с фильтрацией и сглаживанием:

```kotlin
val SMOOTHING_FACTOR = 0.3f
val DEAD_ZONE_THRESHOLD = 0.01f

gyroController.sensorFlow
    .map { sensor -> -sensor.x }  // Инвертируем для интуитивного управления
    .scan(0f) { prevSmoothed, current ->
        prevSmoothed * (1 - SMOOTHING_FACTOR) + current * SMOOTHING_FACTOR
    }
    .filter { abs(it) >= DEAD_ZONE_THRESHOLD }  // Мёртвая зона
    .onEach { delta ->
        gameState.updatePosition(delta)
    }
    .launchIn(scope)
```

### Rendering Pipeline

Паттерн: State → Matrix → Frame

```kotlin
class GameRenderer {
    private val matrix = IntArray(625)

    fun render(context: Context, state: GameState): GlyphMatrixFrame {
        // 1. Очистка матрицы
        matrix.fill(0)

        // 2. Отрисовка игровых объектов в матрицу
        renderPlayer(state.playerX, state.playerY, brightness = 100)
        renderObstacles(state.obstacles)

        // 3. Создание UI элементов
        val scoreObject = GlyphMatrixObject.Builder()
            .setText(state.score.toString())
            .setPosition(10, 19)
            .setBrightness(10)
            .build()

        // 4. Сборка фрейма
        return GlyphMatrixFrame.Builder()
            .addMid(matrix)
            .addTop(scoreObject)
            .build(context)
    }

    private fun renderPlayer(x: Int, y: Int, brightness: Int) {
        if (x in 0..24 && y in 0..24) {
            matrix[y * 25 + x] = brightness
        }
    }
}
```

---

## 6. Пример: Минимальный Glyph Toy

Полный пример простого Toy, который отображает пиксель в центре экрана:

### MinimalGlyphToyService.kt

```kotlin
package com.example.minimaltoy

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

class MinimalGlyphToyService : Service(), GlyphMatrixManager.Callback {

    private val manager by lazy { GlyphMatrixManager.getInstance(this) }
    private val handler = Handler(Looper.getMainLooper())

    private var brightness = 50
    private var increasing = true

    private val serviceHandler = Handler(Looper.getMainLooper()) {
        when (it.what) {
            GlyphToy.MSG_GLYPH_TOY -> {
                val event = it.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                if (event == GlyphToy.EVENT_CHANGE) {
                    onLongPress()
                }
                true
            }
            else -> false
        }
    }
    private val messenger = Messenger(serviceHandler)

    private val renderRunnable = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 50L)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        manager.init(this)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacks(renderRunnable)
        manager.turnOff()
        manager.unInit()
        return false
    }

    override fun onServiceConnected(name: ComponentName?) {
        manager.setGlyphMatrixTimeout(false)
        handler.post(renderRunnable)
    }

    override fun onServiceDisconnected(name: ComponentName?) {}

    private fun render() {
        // Пульсирующий пиксель в центре
        if (increasing) {
            brightness += 10
            if (brightness >= 100) increasing = false
        } else {
            brightness -= 10
            if (brightness <= 10) increasing = true
        }

        val matrix = IntArray(625)
        matrix[12 * 25 + 12] = brightness  // Центр матрицы

        val frame = GlyphMatrixFrame.Builder()
            .addMid(matrix)
            .build(this)

        manager.setMatrixFrame(frame)
    }

    private fun onLongPress() {
        // Сброс яркости при длинном нажатии
        brightness = 50
        increasing = true
    }
}
```

---

## 7. Пример: Игра со входом от сенсоров

### OrientationProvider (FOP API)

Для более точного определения ориентации устройства используйте Fused Orientation Provider из Google Play Services:

```kotlin
package n7.test

import android.content.Context
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.LocationServices
import java.util.concurrent.Executors

class OrientationProvider(context: Context) {

    private val client = LocationServices.getFusedOrientationProviderClient(context)
    private val executor = Executors.newSingleThreadExecutor()

    var headingDegrees: Float = 0f
        private set
    var headingAccuracy: Float = 0f
        private set
    var quaternion: FloatArray = FloatArray(4)
        private set

    var onOrientationChanged: ((DeviceOrientation) -> Unit)? = null

    private val listener = DeviceOrientationListener { orientation ->
        headingDegrees = orientation.headingDegrees
        headingAccuracy = orientation.headingErrorDegrees
        quaternion = orientation.attitude
        onOrientationChanged?.invoke(orientation)
    }

    fun start() {
        val request = DeviceOrientationRequest.Builder(
            DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT
        ).build()

        client.requestOrientationUpdates(request, executor, listener)
    }

    fun stop() {
        client.removeOrientationUpdates(listener)
    }
}
```

### Извлечение pitch/roll из кватерниона

Кватернион `[x, y, z, w]` можно преобразовать в углы Эйлера:

```kotlin
fun quaternionToPitchRoll(q: FloatArray): Pair<Float, Float> {
    val x = q[0]
    val y = q[1]
    val z = q[2]
    val w = q[3]

    // Pitch (наклон вперёд/назад)
    val sinPitch = 2.0f * (w * x + y * z)
    val cosPitch = 1.0f - 2.0f * (x * x + y * y)
    val pitch = Math.atan2(sinPitch.toDouble(), cosPitch.toDouble()).toFloat()

    // Roll (наклон влево/вправо)
    val sinRoll = 2.0f * (w * y - z * x)
    val roll = Math.asin(sinRoll.toDouble().coerceIn(-1.0, 1.0)).toFloat()

    return Pair(
        Math.toDegrees(pitch.toDouble()).toFloat(),
        Math.toDegrees(roll.toDouble()).toFloat()
    )
}
```

### Маппинг наклона на 4 направления

Для игр типа змейка или Pac-Man:

```kotlin
enum class Direction { UP, DOWN, LEFT, RIGHT, NONE }

fun getDirectionFromTilt(pitch: Float, roll: Float, threshold: Float = 15f): Direction {
    return when {
        pitch > threshold -> Direction.DOWN    // Телефон наклонён от себя
        pitch < -threshold -> Direction.UP     // Телефон наклонён к себе
        roll > threshold -> Direction.RIGHT    // Телефон наклонён вправо
        roll < -threshold -> Direction.LEFT    // Телефон наклонён влево
        else -> Direction.NONE                 // В пределах мёртвой зоны
    }
}
```

### Сравнение: Гироскоп vs OrientationProvider

| Характеристика | Гироскоп (SensorManager) | OrientationProvider (FOP) |
|----------------|--------------------------|---------------------------|
| Данные | Угловая скорость (рад/с) | Абсолютная ориентация |
| Drift | Накапливается со временем | Нет (fusion с акселерометром и магнитометром) |
| Отклик | Мгновенный | Небольшая задержка |
| Калибровка | Не требуется | Автоматическая |
| Применение | Быстрые движения (Flappy Box) | Точное позиционирование (Snake) |

### Полный пример с OrientationProvider

```kotlin
class TiltGameController(context: Context) : GlyphMatrixManager.Callback {

    private val glyphManager = GlyphMatrixManager.getInstance(context)
    private val orientationProvider = OrientationProvider(context)
    private val gameLoop = GameLoop { render() }

    private var playerX = 12
    private var playerY = 12

    fun init() {
        glyphManager.init(this)

        orientationProvider.onOrientationChanged = { orientation ->
            val (pitch, roll) = quaternionToPitchRoll(orientation.attitude)
            val direction = getDirectionFromTilt(pitch, roll)

            when (direction) {
                Direction.UP -> if (playerY > 0) playerY--
                Direction.DOWN -> if (playerY < 24) playerY++
                Direction.LEFT -> if (playerX > 0) playerX--
                Direction.RIGHT -> if (playerX < 24) playerX++
                Direction.NONE -> { /* не двигаемся */ }
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?) {
        glyphManager.setGlyphMatrixTimeout(false)
        orientationProvider.start()
        gameLoop.start()
    }

    fun clear() {
        gameLoop.stop()
        orientationProvider.stop()
        glyphManager.turnOff()
        glyphManager.unInit()
    }

    private fun render() {
        val matrix = IntArray(625)
        matrix[playerY * 25 + playerX] = 100  // Игрок

        val frame = GlyphMatrixFrame.Builder()
            .addMid(matrix)
            .build(context)

        glyphManager.setMatrixFrame(frame)
    }

    override fun onServiceDisconnected(name: ComponentName?) {}
}
```

### Зависимость для OrientationProvider

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
```

---

## 8. Референс: Flappy Box

### Путь к проекту

```
../Glyph_Icons
```

### Описание ключевых файлов

| Файл | Назначение |
|------|------------|
| `GlyphToyService.kt` | Entry point — сервис с Messenger для событий от системы |
| `GlyphController.kt` | Координирует компоненты: manager, game loop, gyro, renderer |
| `GameLoop.kt` | Handler-based game loop с deltaTime |
| `GyroSensorController.kt` | Flow-based обёртка над SensorManager для гироскопа |
| `GameState.kt` | Состояние игры: позиция, очки, game over |
| `GameRenderer.kt` | Преобразование состояния в GlyphMatrixFrame |
| `GameConfig.kt` | Константы: скорость, размеры, яркость |
| `ObstacleManager.kt` | Генерация и движение препятствий |

### Что можно переиспользовать

1. **GameLoop** — универсальный game loop, копируется без изменений
2. **GyroSensorController** — Flow-based сенсор, легко адаптируется для других типов сенсоров
3. **Паттерн GlyphController** — разделение на init/clear, callback-based подключение
4. **Структура meta-data** в манифесте — шаблон регистрации toy

### Архитектурные решения

```
┌─────────────────┐
│ GlyphToyService │  ← Entry point, handles system events
└────────┬────────┘
         │ delegates to
         ▼
┌─────────────────┐
│ GlyphController │  ← Coordinator
└────────┬────────┘
         │ uses
    ┌────┴────┬──────────┬──────────┐
    ▼         ▼          ▼          ▼
┌───────┐ ┌───────┐ ┌─────────┐ ┌──────────────────┐
│GameLoop│ │GyroCtl│ │GameState│ │GlyphMatrixManager│
└───────┘ └───────┘ └─────────┘ └──────────────────┘
    │         │          │
    │         │          ▼
    │         │    ┌───────────┐
    │         └───►│GameRenderer│
    │              └───────────┘
    │                    │
    └────────────────────┘
              │
              ▼
        setMatrixFrame()
```

---

## Дополнительные ресурсы

- [GlyphMatrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
- [Nothing Developer Programme](https://nothing.tech/developer)
- Локальная документация SDK: `../GLYPH_SDK.md`
