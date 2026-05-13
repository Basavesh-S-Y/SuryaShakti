# SuryaShakti Project Architecture Diagrams

This document contains report-ready Mermaid diagrams for the SuryaShakti Android project.

## 1. Frontend Architecture Diagram

```mermaid
flowchart TB
    User["User"]

    subgraph AndroidUI["Android Frontend Layer"]
        Splash["SplashActivity"]
        AuthHost["AuthActivity + Auth NavHost"]
        MainHost["MainActivity + BottomNavigationView"]
        LocationPicker["PanelLocationActivity"]

        subgraph AuthScreens["Authentication Screens"]
            Login["LoginFragment"]
            Register["RegisterFragment"]
            Forgot["ForgotPasswordFragment"]
        end

        subgraph MainScreens["Main App Screens"]
            Dashboard["DashboardFragment"]
            LogEntry["LogEntryFragment"]
            Report["ReportFragment"]
            Sync["SyncFragment"]
            Settings["SettingsFragment"]
        end

        subgraph UIComponents["Reusable UI Components"]
            StatusOverlay["StatusOverlayView"]
            DonutChart["SolarDonutChart"]
            BarChart["EnergyBarChart"]
            SunPulse["SunPulseView"]
            Adapters["RecyclerView Adapters"]
            ThemeUI["ThemeManager UI Palette"]
        end
    end

    User --> Splash
    Splash --> AuthHost
    Splash --> MainHost
    Splash --> LocationPicker

    AuthHost --> Login
    Login --> Register
    Login --> Forgot
    Register --> LocationPicker
    Login --> LocationPicker
    LocationPicker --> MainHost

    MainHost --> Dashboard
    MainHost --> LogEntry
    MainHost --> Report
    MainHost --> Sync
    MainHost --> Settings

    Dashboard --> DonutChart
    Report --> BarChart
    LogEntry --> Adapters
    Report --> Adapters
    Sync --> StatusOverlay
    AuthScreens --> StatusOverlay
    MainScreens --> ThemeUI
```

## 2. Backend Architecture Diagram

```mermaid
flowchart TB
    subgraph AppBackend["In-App Backend / Business Layer"]
        AuthVM["AuthViewModel"]
        EnergyVM["EnergyViewModel"]
        ForecastVM["ForecastViewModel"]
        SyncVM["SyncViewModel"]
        PanelVM["PanelLocationViewModel"]

        AuthManager["AuthManager"]
        EnergyRepo["EnergyRepository"]
        ForecastRepo["ForecastRepository"]
        CloudSync["CloudSyncManager"]
        LocationSearch["LocationSearchService"]
        Geocoder["GeocoderService"]
        Prefs["PreferenceManager + PanelLocationManager"]
        Worker["PeakSunWorker"]
    end

    subgraph LocalStorage["Local Storage"]
        Room["Room AppDatabase"]
        Dao["EnergyLogDao"]
        SharedPrefs["SharedPreferences"]
    end

    subgraph CloudServices["External / Cloud Services"]
        FirebaseAuth["Firebase Authentication"]
        Firestore["Firebase Firestore"]
        GoogleSignIn["Google Sign-In"]
        GoogleMaps["Google Maps SDK"]
        OpenMeteo["Open-Meteo Forecast API"]
        Nominatim["Location Search API"]
    end

    AuthVM --> AuthManager
    EnergyVM --> EnergyRepo
    ForecastVM --> ForecastRepo
    SyncVM --> CloudSync
    PanelVM --> LocationSearch
    PanelVM --> Geocoder

    AuthManager --> FirebaseAuth
    AuthManager --> GoogleSignIn
    CloudSync --> Firestore
    EnergyRepo --> Dao
    Dao --> Room
    Prefs --> SharedPrefs
    ForecastRepo --> Prefs
    ForecastRepo --> OpenMeteo
    PanelVM --> Prefs
    LocationSearch --> Nominatim
    Geocoder --> Nominatim
    Worker --> ForecastRepo
    LocationPicker["PanelLocationActivity"] --> GoogleMaps
```

## 3. Whole Architecture Diagram

```mermaid
flowchart TB
    User["User"]

    subgraph Device["Android Device"]
        subgraph Presentation["Presentation Layer"]
            Activities["SplashActivity, AuthActivity, MainActivity, PanelLocationActivity"]
            Fragments["Login, Register, Dashboard, Log, Report, Sync, Settings"]
            CustomViews["Charts, Status Overlay, Sun Animation"]
        end

        subgraph StateLogic["State and Business Logic"]
            ViewModels["Android ViewModels + LiveData"]
            Repositories["EnergyRepository + ForecastRepository"]
            Managers["AuthManager, CloudSyncManager, PreferenceManager, ThemeManager"]
            WorkManager["PeakSunWorker"]
        end

        subgraph Persistence["Local Persistence"]
            RoomDB["Room SQLite: energy_logs"]
            SharedPrefs["SharedPreferences: settings, theme, panel location"]
        end
    end

    subgraph External["External Services"]
        FirebaseAuth["Firebase Auth"]
        Firestore["Cloud Firestore"]
        GoogleMaps["Google Maps + Sign-In"]
        OpenMeteo["Open-Meteo Solar Irradiance"]
        LocationAPI["Location Search / Reverse Geocoding"]
    end

    User --> Activities
    Activities --> Fragments
    Fragments --> CustomViews
    Fragments --> ViewModels
    ViewModels --> Repositories
    ViewModels --> Managers
    ViewModels --> WorkManager
    Repositories --> RoomDB
    Managers --> SharedPrefs
    Managers --> FirebaseAuth
    Managers --> Firestore
    Activities --> GoogleMaps
    Repositories --> OpenMeteo
    Managers --> LocationAPI
    WorkManager --> OpenMeteo
```

## 4. Database Schema

### Local Room Database Schema

```mermaid
erDiagram
    ENERGY_LOGS {
        long id PK "Auto generated"
        long dateMillis "Midnight epoch millis"
        double generatedKwh "Solar generation"
        double consumedKwh "Energy consumption"
        string weatherCondition "SUNNY, PARTLY_CLOUDY, CLOUDY, RAINY"
        double perUnitRate "Grid rate per kWh"
        double exportRate "Export rate per kWh"
        double panelCapacityKw "Installed solar capacity"
        string notes "Optional user notes"
    }
```

### Cloud Firestore Logical Schema

```mermaid
erDiagram
    USERS {
        string uid PK
        string displayName
        string email
        long updatedAt
        long lastSyncAt
    }

    CLOUD_ENERGY_LOGS {
        string documentId PK "dateMillis"
        long id
        long dateMillis
        double generatedKwh
        double consumedKwh
        string weatherCondition
        double perUnitRate
        double exportRate
        double panelCapacityKw
        string notes
        long syncedAt
    }

    USERS ||--o{ CLOUD_ENERGY_LOGS : owns
```

### SharedPreferences Storage

```mermaid
classDiagram
    class PreferenceManager {
        gridRatePerUnit
        exportRatePerUnit
        panelCapacityKw
        notificationsEnabled
        userName
    }

    class PanelLocationManager {
        panel_lat
        panel_lon
        panel_name
        panel_short
        panel_country
        panel_location_set
    }

    class ThemeManager {
        selected_theme
        dark
        light
    }
```

## 5. User Flow Diagram

```mermaid
flowchart TD
    Start["Open SuryaShakti App"] --> Splash["Splash Screen"]
    Splash --> AuthCheck{"User logged in?"}

    AuthCheck -- "No" --> Login["Login Screen"]
    Login --> LoginChoice{"Choose action"}
    LoginChoice -- "Email login" --> EmailLogin["Enter email and password"]
    LoginChoice -- "Google login" --> GoogleLogin["Google Sign-In"]
    LoginChoice -- "Create account" --> Register["Register Screen"]
    LoginChoice -- "Forgot password" --> Forgot["Password Reset"]

    Register --> AuthSuccess["Authentication Success"]
    EmailLogin --> AuthSuccess
    GoogleLogin --> AuthSuccess
    Forgot --> Login

    AuthCheck -- "Yes" --> LocationCheck{"Panel location set?"}
    AuthSuccess --> LocationCheck
    LocationCheck -- "No" --> PickLocation["Pick panel location by search, map, or current location"]
    PickLocation --> MainDashboard["Dashboard"]
    LocationCheck -- "Yes" --> MainDashboard

    MainDashboard --> LogEnergy["Log daily generation and consumption"]
    MainDashboard --> ViewReports["View reports and analytics"]
    MainDashboard --> SyncCloud["Sync local logs with cloud"]
    MainDashboard --> Settings["Manage rates, capacity, theme, notifications, account"]

    LogEnergy --> MainDashboard
    ViewReports --> MainDashboard
    SyncCloud --> MainDashboard
    Settings --> MainDashboard
```

## 6. Wireflow Diagram

```mermaid
flowchart LR
    SplashUI["Splash\nLogo + loading"] --> LoginUI["Login\nEmail, password, Google, register, forgot"]
    LoginUI --> RegisterUI["Register\nName, email, password"]
    LoginUI --> ForgotUI["Forgot Password\nEmail reset"]
    RegisterUI --> LocationUI["Panel Location\nSearch tab + Map tab"]
    LoginUI --> LocationUI
    LocationUI --> DashboardUI["Dashboard\nIrradiance, generation, consumption, charts, savings"]

    DashboardUI --> LogUI["Log Entry\nDate, weather chips, generation, consumption, rates, notes"]
    LogUI --> DashboardUI

    DashboardUI --> ReportUI["Report\n30-day summary, bar chart, expandable log list, CSV export"]
    ReportUI --> DashboardUI

    DashboardUI --> SyncUI["Sync\nCloud sync status, upload/download result"]
    SyncUI --> DashboardUI

    DashboardUI --> SettingsUI["Settings\nPanel location, profile, themes, rates, capacity, notifications, sign out"]
    SettingsUI --> LocationUI
    SettingsUI --> LoginUI
```

## 7. Flowchart Diagram

### Daily Energy Log Flow

```mermaid
flowchart TD
    Start["User opens Log Entry"] --> LoadPrefs["Load saved rates and panel capacity"]
    LoadPrefs --> SelectDate["Select date"]
    SelectDate --> SelectWeather["Choose weather condition"]
    SelectWeather --> Simulate{"Use simulation?"}
    Simulate -- "Yes" --> Generate["Estimate generated kWh from weather and capacity"]
    Simulate -- "No" --> ManualGen["Enter generated kWh manually"]
    Generate --> EnterConsumption["Enter consumed kWh"]
    ManualGen --> EnterConsumption
    EnterConsumption --> EnterOptional["Edit rates, capacity, notes"]
    EnterOptional --> Validate{"Inputs valid?"}
    Validate -- "No" --> ShowError["Show field error"]
    ShowError --> EnterOptional
    Validate -- "Yes" --> CheckExisting{"Log exists for date?"}
    CheckExisting -- "Yes" --> UpdateRoom["Update existing Room record"]
    CheckExisting -- "No" --> InsertRoom["Insert new Room record"]
    UpdateRoom --> Recompute["Recompute dashboard and report LiveData"]
    InsertRoom --> Recompute
    Recompute --> Success["Show saved/updated confirmation"]
```

### Forecast and Peak Sun Flow

```mermaid
flowchart TD
    OpenDashboard["Open Dashboard"] --> HasLocation{"Panel location saved?"}
    HasLocation -- "No" --> ShowLocationNeeded["Show location needed message"]
    HasLocation -- "Yes" --> FetchForecast["ForecastRepository requests Open-Meteo"]
    FetchForecast --> ParseJSON["Parse hourly direct radiation"]
    ParseJSON --> BuildForecast["Create SolarForecast"]
    BuildForecast --> PeakCheck{"Current irradiance > 500 W/m2?"}
    PeakCheck -- "Yes" --> PeakUI["Show peak sun card and high status"]
    PeakCheck -- "No" --> NormalUI["Show normal irradiance status"]
    PeakUI --> DashboardCharts["Update dashboard UI"]
    NormalUI --> DashboardCharts
```

## 8. Methodology Diagram

```mermaid
flowchart LR
    Requirement["Requirement Analysis\nSolar monitoring, logging, reports, sync"] --> Design["System Design\nMVVM, Room, Firebase, APIs"]
    Design --> UIUX["UI/UX Design\nDashboard, log form, reports, settings"]
    UIUX --> Implementation["Implementation\nKotlin, XML layouts, ViewModels, repositories"]
    Implementation --> Integration["Integration\nFirebase, Google Maps, Open-Meteo, WorkManager"]
    Integration --> Testing["Testing\nValidation, navigation, persistence, sync checks"]
    Testing --> Evaluation["Evaluation\nEnergy savings, independence score, report outputs"]
    Evaluation --> Enhancement["Enhancement\nThemes, notifications, cloud backup, report export"]
    Enhancement --> Requirement
```

## 9. MVVM Package Diagram

```mermaid
classDiagram
    class UI {
        Activities
        Fragments
        XML Layouts
        Custom Views
    }

    class ViewModelLayer {
        AuthViewModel
        EnergyViewModel
        ForecastViewModel
        SyncViewModel
        PanelLocationViewModel
        TipViewModel
    }

    class RepositoryLayer {
        EnergyRepository
        ForecastRepository
    }

    class DataLayer {
        EnergyLog
        SolarForecast
        PanelLocation
        UserProfile
        RoomDatabase
        DAO
    }

    class UtilityLayer {
        AuthManager
        CloudSyncManager
        PreferenceManager
        ThemeManager
        OpenMeteoService
        LocationSearchService
        GeocoderService
    }

    UI --> ViewModelLayer : observes LiveData
    ViewModelLayer --> RepositoryLayer : requests data
    ViewModelLayer --> UtilityLayer : auth, sync, prefs, theme
    RepositoryLayer --> DataLayer : reads/writes
    UtilityLayer --> DataLayer : maps models
```

## 10. Authentication Sequence Diagram

```mermaid
sequenceDiagram
    actor User
    participant Login as Login/Register Fragment
    participant AuthVM as AuthViewModel
    participant AuthMgr as AuthManager
    participant Firebase as Firebase Auth
    participant Cloud as CloudSyncManager
    participant Firestore as Firestore
    participant Main as MainActivity

    User->>Login: Enter credentials or choose Google
    Login->>AuthVM: login/register/handleGoogleSignInResult()
    AuthVM->>AuthMgr: authenticate user
    AuthMgr->>Firebase: Firebase Auth request
    Firebase-->>AuthMgr: Firebase user
    AuthMgr-->>AuthVM: UserProfile
    AuthVM->>Cloud: saveUserProfile(uid, name, email)
    Cloud->>Firestore: Upsert users/{uid}
    Firestore-->>Cloud: Success
    AuthVM-->>Login: AuthState.LoggedIn
    Login-->>Main: Navigate after location check
```

## 11. Energy Log Sequence Diagram

```mermaid
sequenceDiagram
    actor User
    participant Log as LogEntryFragment
    participant VM as EnergyViewModel
    participant Repo as EnergyRepository
    participant DAO as EnergyLogDao
    participant DB as Room Database
    participant Dash as Dashboard/Report

    User->>Log: Fill energy log form
    Log->>VM: saveLog(...)
    VM->>Repo: getLogByDate(dateMillis)
    Repo->>DAO: Query existing log
    DAO->>DB: SELECT by dateMillis
    DB-->>DAO: Existing log or null
    DAO-->>Repo: Result
    Repo-->>VM: Result
    alt Existing log found
        VM->>Repo: updateLog(log)
        Repo->>DAO: UPDATE
    else New log
        VM->>Repo: insertLog(log)
        Repo->>DAO: INSERT
    end
    DAO->>DB: Persist record
    DB-->>DAO: Updated LiveData
    DAO-->>Dash: latestLog and last30Logs updates
    VM-->>Log: SaveStatus.Saved or Updated
```

## 12. Cloud Sync Sequence Diagram

```mermaid
sequenceDiagram
    actor User
    participant SyncUI as SyncFragment
    participant SyncVM as SyncViewModel
    participant Repo as EnergyRepository
    participant Cloud as CloudSyncManager
    participant Firestore as Firestore
    participant Room as Room Database

    User->>SyncUI: Tap sync
    SyncUI->>SyncVM: startSync()
    SyncVM->>Repo: Read local logs
    Repo->>Room: Query all energy_logs
    Room-->>Repo: Local logs
    Repo-->>SyncVM: Local logs
    SyncVM->>Cloud: sync(uid, localLogs)
    Cloud->>Firestore: Upload local logs to users/{uid}/energy_logs
    Firestore-->>Cloud: Upload complete
    Cloud->>Firestore: Download remote logs
    Firestore-->>Cloud: Remote documents
    loop New remote logs
        Cloud->>Repo: onSaveLocal(log)
        Repo->>Room: Insert downloaded log
    end
    Cloud->>Firestore: Update users/{uid}.lastSyncAt
    Cloud-->>SyncVM: SyncResult(uploaded, downloaded)
    SyncVM-->>SyncUI: SyncState.Success
```

## 13. Notification / Background Worker Diagram

```mermaid
flowchart TD
    AppStart["SuryaShaktiApp.onCreate"] --> Schedule["Schedule PeakSunWorker"]
    Schedule --> WorkManager["Android WorkManager"]
    WorkManager --> WorkerRun["PeakSunWorker runs periodically"]
    WorkerRun --> NotificationsEnabled{"Notifications enabled?"}
    NotificationsEnabled -- "No" --> Stop["Stop worker task"]
    NotificationsEnabled -- "Yes" --> HasLocation{"Panel location saved?"}
    HasLocation -- "No" --> Stop
    HasLocation -- "Yes" --> Fetch["Fetch latest irradiance"]
    Fetch --> Peak{"Peak sun threshold crossed?"}
    Peak -- "No" --> Complete["Complete silently"]
    Peak -- "Yes" --> Notify["Show peak sun notification"]
    Notify --> OpenApp["Tap notification opens MainActivity"]
```

## 14. Deployment / Runtime Context Diagram

```mermaid
flowchart TB
    subgraph Phone["Android Phone"]
        APK["SuryaShakti APK"]
        SQLite["Room SQLite DB"]
        Prefs["SharedPreferences"]
        Notification["Android Notification Manager"]
        WorkManager["WorkManager"]
    end

    subgraph GoogleFirebase["Google / Firebase Cloud"]
        Auth["Firebase Auth"]
        Firestore["Firestore Database"]
        SignIn["Google Sign-In"]
        Maps["Google Maps SDK"]
    end

    subgraph PublicAPIs["Public APIs"]
        Meteo["Open-Meteo API"]
        Geo["Geocoding / Location Search API"]
    end

    APK --> SQLite
    APK --> Prefs
    APK --> Notification
    APK --> WorkManager
    APK --> Auth
    APK --> Firestore
    APK --> SignIn
    APK --> Maps
    APK --> Meteo
    APK --> Geo
```

## 15. Data Flow Diagram

```mermaid
flowchart LR
    User["User Input"] --> UI["Fragments / Activities"]
    UI --> VM["ViewModels"]

    VM --> Validation["Validation and State Handling"]
    Validation --> LocalData["Room Database"]
    Validation --> PrefData["SharedPreferences"]
    Validation --> RemoteData["Firebase / APIs"]

    LocalData --> LiveData["LiveData Observables"]
    PrefData --> UI
    RemoteData --> VM
    LiveData --> VM
    VM --> UI

    UI --> Output["Dashboard Cards, Charts, Reports, Toasts, Notifications"]
```

## 16. Component Interaction Diagram

```mermaid
flowchart TB
    Dashboard["DashboardFragment"] --> ForecastVM["ForecastViewModel"]
    Dashboard --> EnergyVM["EnergyViewModel"]
    Dashboard --> TipVM["TipViewModel"]

    LogEntry["LogEntryFragment"] --> EnergyVM
    Report["ReportFragment"] --> EnergyVM
    Sync["SyncFragment"] --> SyncVM["SyncViewModel"]
    Settings["SettingsFragment"] --> AuthVM["AuthViewModel"]
    Settings --> Prefs["PreferenceManager"]
    Settings --> Theme["ThemeManager"]
    PanelLocation["PanelLocationActivity"] --> PanelVM["PanelLocationViewModel"]

    EnergyVM --> EnergyRepo["EnergyRepository"]
    EnergyRepo --> Room["Room / EnergyLogDao"]
    ForecastVM --> ForecastRepo["ForecastRepository"]
    ForecastRepo --> OpenMeteo["Open-Meteo"]
    SyncVM --> CloudSync["CloudSyncManager"]
    CloudSync --> Firestore["Firestore"]
    AuthVM --> AuthManager["AuthManager"]
    AuthManager --> FirebaseAuth["Firebase Auth"]
    PanelVM --> LocationSearch["Location Search"]
    PanelVM --> PanelPrefs["PanelLocationManager"]
```

