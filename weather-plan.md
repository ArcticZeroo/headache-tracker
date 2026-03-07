# Plan: Decouple Weather from Entries, Adjacent-Day Correlations, Fahrenheit, Exercise Chart

## Problem Statement
1. Weather data is tied to headache entries via FK — should be stored independently by date so we can correlate adjacent-day weather with pain
2. Weather is fetched inline during save, slowing it down — should be background-only with on-demand fallback
3. Temperatures display in Celsius instead of Fahrenheit
4. Exercise minutes don't have their own chart tab

## Approach

### 1. New `DailyWeather` entity (date-keyed, no FK to entries)
- Create `DailyWeather` entity keyed by date string (unique)
- Room migration v2→v3: create new table, migrate existing data, drop old `weather_data` table
- New `DailyWeatherDao` with queries: `getByDate`, `getByDateRange`, `getMissingDates`
- Remove old `WeatherData` entity and `WeatherDao`

**Files:** New DailyWeather.kt, new DailyWeatherDao.kt, HeadacheDatabase.kt (migration + entity swap), DatabaseModule.kt

### 2. Restructure WeatherRepository
- Remove `fetchAndStoreWeatherForEntry()` (entry-coupled)
- Add `syncWeatherAroundEntries()`: for each entry with location, fetch weather for D-1, D, D+1 using that entry's coords. Open-Meteo supports date ranges so we batch per location.
- Add `ensureWeatherForDateRange(start, end, lat, lng)`: on-demand fetch for missing dates (used by analysis/correlations when data isn't synced yet)
- `getWeatherForDateRange(startDate, endDate)`: query local DB by date

**Files:** WeatherRepository.kt

### 3. Remove inline weather from save flow
- EntryViewModel.saveEntry(): remove weather fetch, enqueue one-shot WorkManager sync instead
- WidgetClickAction: remove weather fetch block entirely (background sync picks it up)

**Files:** EntryViewModel.kt, WidgetClickAction.kt

### 4. Update background sync
- WeatherSyncWorker calls `syncWeatherAroundEntries()` which fetches D-1/D/D+1 for all entries with location but missing weather on those dates

**Files:** WeatherSyncWorker.kt (minimal change — just calls new repo method)

### 5. Adjacent-day weather correlations
- CorrelationRepository: build weather-by-date maps, then correlate pain[D] with weather[D-1] and weather[D+1]
- Add "Prev. Day Rain", "Prev. Day Pressure", "Next Day Rain", "Next Day Pressure" correlations
- Before computing, call `ensureWeatherForDateRange()` on-demand if needed

**Files:** CorrelationRepository.kt

### 6. Update all weather consumers to use date-keyed data
- WeatherDataSource: query by date range (no entry join needed)
- HistoryViewModel: query weather by date string instead of entry IDs
- CorrelationRepository: query by date (already partially does this)

**Files:** WeatherDataSource.kt, HistoryViewModel.kt, CorrelationRepository.kt

### 7. Temperature in Fahrenheit
- Convert C→F at display time (F = C × 9/5 + 32)
- Keep DB in Celsius for consistency with Open-Meteo API

**Files:** WeatherDataSource.kt, HistoryScreen.kt (DayContextRow)

### 8. Exercise minutes chart tab
- New `ExerciseDataSource` implementing `AnalysisDataSource`
- Register in Hilt AppModule

**Files:** New ExerciseDataSource.kt, AppModule.kt
