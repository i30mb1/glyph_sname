# GlyphMatrix SDK

## Подключение

AAR библиотека находится в `libs/glyph-matrix-sdk-1.0.aar`

## Настройка AndroidManifest

Добавить permission:
```xml
<uses-permission android:name="com.nothing.ketchum.permission.ENABLE" />
```

## Основные компоненты

- **GlyphMatrixManager** - управление подключением и отображением
- **GlyphMatrixFrame** - рендеринг матрицы со слоями
- **GlyphMatrixObject** - элементы для отображения на матрице

## API

### GlyphMatrixManager

**Инициализация:**
```kotlin
val manager = GlyphMatrixManager.getInstance(context)
manager.init(callback: Callback)
manager.register(packageName: String): Boolean
```

**Управление отображением:**
```kotlin
setMatrixFrame(frame: GlyphMatrixFrame)        // обновление toy дисплея
setMatrixFrame(matrix: IntArray)               // прямая матрица 25x25
setAppMatrixFrame(frame: GlyphMatrixFrame)     // отображение для приложения
setAppMatrixFrame(matrix: IntArray)
closeAppMatrix()                                // закрыть app matrix
setGlyphMatrixTimeout(enabled: Boolean)         // таймаут автоотключения
turnOff()                                       // выключить дисплей
unInit()                                        // отвязка от сервиса
```

### GlyphMatrixFrame.Builder

**Слои (приоритет сверху вниз):**
```kotlin
addTop(obj: GlyphMatrixObject)    // верхний слой
addTop(matrix: IntArray)
addMid(obj: GlyphMatrixObject)    // средний слой
addMid(matrix: IntArray)
addLow(obj: GlyphMatrixObject)    // нижний слой
addLow(matrix: IntArray)
build(context: Context): GlyphMatrixFrame
buildWithMarquee(context, handler, speed, direction, listener): GlyphMatrixFrameWithMarquee
```

### GlyphMatrixObject.Builder

```kotlin
setImageSource(bitmap: Bitmap)                 // изображение
setRawArray(matrix: IntArray)                  // сырой массив 25x25
setText(text: String)                          // текст
setText(text: String, brightness: Int)
setText(text: String, brightness: Int, range: Range<Int>)
setTextStyle(style: String)                    // стиль шрифта
setPosition(x: Int, y: Int)                    // позиция объекта
setOrientation(orientation: Int)               // ориентация (0/90/180/270)
setScale(scale: Int)                           // масштаб
setBrightness(brightness: Int)                 // яркость 0-100
setReverse(reverse: Boolean)                   // инверсия
build(): GlyphMatrixObject
```

### Пример: один пиксель в центре
```kotlin
val matrix = IntArray(25 * 25)
matrix[12 * 25 + 12] = 100  // центр, яркость 100

val frame = GlyphMatrixFrame.Builder()
    .addMid(matrix)
    .build(context)

manager.setMatrixFrame(frame)
```

## Взаимодействие

- Короткое нажатие: переключение между toys
- Длинное нажатие: кастомные действия
- Touch events для специфичной обработки

## Утилиты

### Добавление паддингов к GIF

Для обложки toy используйте ImageMagick (без потери качества):
```bash
magick input.gif -bordercolor transparent -border 5x5 output.gif
magick input.gif -bordercolor transparent -border 10x20 output.gif  # разные паддинги
```

## Форма дисплея

Матрица 25x25 вписана в круг. Углы не видны.

| Строка (y) | Видимых пикселей | Диапазон x   |
|------------|------------------|--------------|
| 0, 24      | 7                | 9-15         |
| 1, 23      | 11               | 7-17         |
| 2, 22      | 15               | 5-19         |
| 3, 21      | 17               | 4-20         |
| 4, 20      | 19               | 3-21         |
| 5-6, 18-19 | 21               | 2-22         |
| 7-8, 16-17 | 23               | 1-23         |
| 9-15       | 24               | 0-24         |

**Центр**: (12, 12)
**Физический радиус**: ~12.5 пикселей
**Безопасный радиус для контента**: 11 пикселей (dx² + dy² ≤ 121)

## Источник

https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
