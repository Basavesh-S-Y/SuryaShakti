# ☀️ SuryaShakti – Smart Solar Monitoring Android App

> Smart Solar Monitoring for a Sustainable Future ☀️

---

## 📌 Project Information

**Project Name:** SuryaShakti  
**Owner / Developer:** Basavesh SY  
**GitHub Repository:** https://github.com/Basavesh-S-Y/SuryaShakti  
**Version:** v1.0.4 
**Release Date:** *(May-13-2026)*  
**Institution / Organization:** *(Bapuji Institute of Engineering and Technology Davanagere ,MindMatrix)*  
**Project Guide / Mentor:** *(Gangamma Hediyalad)*  
**Department:** *(CSE)*  
**Academic Year:** *(2026)*  

---

## 📖 About the Project

SuryaShakti is a modern Android application designed for monitoring solar energy systems with a premium user interface, real-time weather forecasting, energy logging, cloud synchronization, and location-based solar analysis.

The application helps users:

- Monitor solar panel performance
- Track energy generation
- Analyze solar suitability based on location
- Store and manage energy history
- Sync data to cloud storage
- Receive smart solar notifications

---

## ✨ Features

### 🔐 Authentication System
- Email Login
- User Registration
- Forgot Password
- Firebase Authentication
- Secure User Session Management

---

### 📊 Solar Dashboard
Interactive dashboard with:

- Solar performance overview
- Energy production metrics
- Animated counters
- Donut charts
- Bar graphs
- System health indicators
- Smart solar insights

Custom Components:
- `SolarDonutChart`
- `EnergyBarChart`
- `AnimatedCounterView`
- `SunPulseView`

---

### 📝 Energy Log Management
Users can:

- Add energy generation logs
- View history
- Track performance trends
- Store records locally

Technology:
- Room Database
- DAO Architecture
- Repository Pattern

---

### 📍 Location-Based Solar Analysis
Features:

- Google Maps integration
- GPS location detection
- Reverse geocoding
- Installation site analysis
- Solar suitability estimation

Used Services:
- Google Maps SDK
- Google Play Location Services

---

### ☁️ Weather & Forecast
Provides:

- Weather updates
- Sunlight estimation
- Solar production prediction

API:
- Open-Meteo API

---

### 🔄 Cloud Synchronization
Includes:

- Firebase Firestore Sync
- Manual Sync
- Sync Status Monitoring

---

### ⚙️ Settings Module
User Preferences:

- Theme Switching
- Account Settings
- App Customization

Themes:
- Light Mode
- Dark Mode

---

### 🔔 Notifications
Smart alerts for:

- Peak sunlight hours
- Scheduled solar reminders
- Background monitoring

Powered by:
- WorkManager

---

## 🏗 Architecture

Project follows **MVVM Architecture Pattern**

```text
com.suryashakti.solarmonitor
│
├── adapter
├── custom
├── data
├── repository
├── ui
│   ├── auth
│   ├── dashboard
│   ├── location
│   ├── log
│   ├── main
│   ├── report
│   ├── settings
│   ├── splash
│   └── sync
├── util
├── viewmodel
└── worker
```

Architecture Components:

- MVVM
- Repository Pattern
- ViewModel
- LiveData
- Room Persistence
- Firebase Integration
- Navigation Component

---

## 🛠 Technology Stack

### Programming Language
- Kotlin

### UI Development
- XML Layouts
- Material Design
- Custom Android Views

### Architecture
- MVVM

### Database
- Room Database

### Authentication
- Firebase Authentication

### Cloud Storage
- Firebase Firestore

### Location Services
- Google Maps SDK
- Play Services Location

### APIs
- Open-Meteo API

### Background Tasks
- WorkManager

---

## 📦 Dependencies

```gradle
androidx.core:core-ktx
androidx.appcompat
material
constraintlayout
navigation-fragment-ktx
navigation-ui-ktx
room-runtime
room-ktx
lifecycle-viewmodel-ktx
lifecycle-livedata-ktx
work-runtime-ktx
kotlinx-coroutines
firebase-auth
firebase-firestore
firebase-analytics
play-services-auth
play-services-maps
play-services-location
```

---

## 📂 Main Modules

### Authentication
Files:
- `AuthActivity.kt`
- `LoginFragment.kt`
- `RegisterFragment.kt`
- `ForgotPasswordFragment.kt`

---

### Dashboard
Files:
- `DashboardFragment.kt`

---

### Energy Logs
Files:
- `LogEntryFragment.kt`
- `EnergyLog.kt`
- `EnergyLogDao.kt`

---

### Reports
Files:
- `ReportFragment.kt`

---

### Cloud Sync
Files:
- `SyncFragment.kt`
- `CloudSyncManager.kt`

---

### Location Module
Files:
- `PanelLocationActivity.kt`
- `LocationHelper.kt`
- `GeocoderService.kt`

---

## 🚀 Installation Guide

### Requirements
Install:

- Android Studio Hedgehog+
- JDK 17
- Android SDK 34
- Firebase Project
- Google Maps API Key

---

### Clone Repository

```bash
git clone https://github.com/Basavesh-S-Y/SuryaShakti.git
```

---

### Setup Steps

1. Open Android Studio
2. Click **Open Existing Project**
3. Select project folder
4. Wait for Gradle sync

---

## Firebase Setup

Enable:

- Authentication
- Firestore Database
- Analytics

Add:

```text
app/google-services.json
```

---

## Google Maps Setup

Add API key in:

```xml
AndroidManifest.xml
```

Example:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY"/>
```

---

## ▶ Run Application

Build:

```bash
./gradlew assembleDebug
```

Run from Android Studio:

```text
Run → Run 'app'
```

---

## 📸 Screenshots

*(Add screenshots manually)*

Suggested screenshots:

- Splash Screen
- Login Screen
- Dashboard
- Energy Logs
- Reports
- Sync Page
- Settings
- Maps Screen

---

## 🔮 Future Enhancements

- IoT Solar Inverter Integration
- Real-time Monitoring
- AI Prediction Engine
- PDF Report Export
- Push Notifications
- Admin Dashboard
- Multi-user Access
- Fault Detection

---


## 👨‍💻 Developer

**Basavesh SY**

GitHub: https://github.com/Basavesh-S-Y  
Email: *(basavesh47@gmail.com)*  
LinkedIn: *(https://www.linked.com/in/basavesh-sy-53662225b)*

---

## 🙌 Acknowledgements

Built using:

- Android Studio
- Kotlin
- Firebase
- Google Maps
- Open-Meteo API
- Material Design

---

# Thank You ☀️
