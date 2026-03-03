# MindCheck — AI-Powered Mental Health Companion for University Students

**A comprehensive mental health screening and wellness tracking Android application, powered by Machine Learning**

MindCheck is a mobile application designed to help university students in Indonesia detect early signs of depression, track their mental wellness, and build positive self-care habits through AI-driven insights and evidence-based interventions.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-14.0+-green.svg)](https://www.android.com)
[![Python](https://img.shields.io/badge/Python-3.9+-blue.svg)](https://www.python.org)
[![Flask](https://img.shields.io/badge/Flask-3.0-black.svg)](https://flask.palletsprojects.com)
[![Machine Learning](https://img.shields.io/badge/ML-Logistic_Regression-orange.svg)](https://scikit-learn.org)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Machine Learning Model](#machine-learning-model)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [Demo Video](#demo-video)
- [Team](#team)
- [License](#license)

---

## Overview

Mental health issues, particularly depression, are increasingly prevalent among university students due to academic pressure, financial stress, sleep deprivation, and social challenges. Many students are unaware of their mental health status or hesitate to seek professional help due to stigma.

**MindCheck** addresses this gap by providing:
- **Early detection** of depression risk through AI-powered screening (PHQ-9 based)
- **Daily wellness tracking** for mood, sleep, and gratitude journaling
- **Self-care tools** including goal setting, breathing exercises, and grounding techniques
- **Anonymous & accessible** platform for self-assessment without judgment

This project combines **Android mobile development** and **Machine Learning** to create a holistic mental health support system for students.

---

## Features

### AI Mental Health Screening
- **PHQ-9 Based Questionnaire** — scientifically validated depression assessment
- **Logistic Regression Model** — trained on mental health data with 98.26% accuracy (5-fold CV)
- **Instant Results** — risk level classification (Low/Medium/High) with personalized advice
- **Screening History** — track progress over time with visualized trends

### Daily Wellness Tracking
- **Mood Logging** — record daily emotions with trigger identification
- **Sleep Monitoring** — track sleep duration, quality, and patterns
- **Gratitude Journal** — practice positive reflection with daily entries
- **Analytics Dashboard** — visualize trends and wellness score

### Self-Care & Goal Management
- **Wellness Goals** — set personalized self-care objectives
- **Task Breakdown** — create actionable steps with daily/weekly tracking
- **Streak System** — gamified consistency rewards
- **Progress Visualization** — monitor goal completion rates

### Emergency Support
- **Breathing Exercises** — guided techniques to reduce anxiety
- **Grounding Method (5-4-3-2-1)** — sensory-based calming practice
- **Crisis Hotlines** — quick access to mental health support (Indonesia: 119 ext 8)
- **Positive Affirmations** — instant mood boosters

### Privacy & Security
- **Local-First Storage** — sensitive data stored on device using Room database
- **Firebase Backup** — optional cloud sync with encryption
- **Anonymous Screening** — no personal identifiers sent to ML API
- **Google OAuth** — secure authentication without password storage

---

## Tech Stack

### Android Application
| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| Architecture | MVVM (Model-View-ViewModel) |
| UI Framework | Material Design 3, ViewBinding |
| Local Database | Room (SQLite) |
| Cloud Services | Firebase (Auth, Firestore, Analytics) |
| Networking | Retrofit + OkHttp |
| Async Operations | Kotlin Coroutines + Flow |
| Logging | Timber |

### Machine Learning Backend
| Component | Technology |
|-----------|-----------|
| Framework | Flask 3.0 (Python) |
| ML Algorithm | Logistic Regression (scikit-learn) |
| Preprocessing | StandardScaler, Label Encoding |
| Model Persistence | Pickle |
| API Format | RESTful JSON |
| CORS | Enabled for Android client |

### Development Tools
- **Android Studio** Hedgehog (2023.1.1+)
- **Python** 3.9+
- **Gradle** 8.2+
- **JDK** 17

---

## System Architecture

```
┌────────────────────────────────────────────────────────────┐
│                  Android App (Kotlin MVVM)                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
│  │   Home   │  │   Mood   │  │  Goals   │  │  Profile  │  │
│  │ Fragment │  │ Fragment │  │ Fragment │  │ Fragment  │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └─────┬─────┘  │
│       │             │              │              │         │
│  ┌────┴──────────────┴──────────────┴──────────────┴─────┐ │
│  │              ViewModel Layer (LiveData + Flow)         │ │
│  └────┬──────────────┬──────────────┬──────────────┬──────┘ │
│       │              │              │              │         │
│  ┌────┴─────┐  ┌─────┴─────┐  ┌────┴────┐  ┌──────┴─────┐ │
│  │Repository│  │  Room DB  │  │Firebase │  │  Retrofit  │ │
│  │  Layer   │  │  (Local)  │  │(Cloud)  │  │ API Client │ │
│  └──────────┘  └───────────┘  └─────────┘  └──────┬─────┘ │
└────────────────────────────────────────────────────┼───────┘
                                                      │
                                            HTTP/JSON │
                                                      ▼
                     ┌────────────────────────────────────┐
                     │      Flask ML API (Python)         │
                     │  ┌──────────────────────────────┐  │
                     │  │   /predict Endpoint          │  │
                     │  └────────┬─────────────────────┘  │
                     │           ▼                         │
                     │  ┌──────────────────────────────┐  │
                     │  │ Logistic Regression Model    │  │
                     │  │ (model.pkl + scaler.pkl)     │  │
                     │  └────────┬─────────────────────┘  │
                     │           ▼                         │
                     │  ┌──────────────────────────────┐  │
                     │  │  Prediction + Advice         │  │
                     │  └──────────────────────────────┘  │
                     └────────────────────────────────────┘
```

### Data Flow

**1. Local-First Approach**
- User data saved to Room database (offline-first)
- Background sync to Firebase for backup
- Works completely offline for core features

**2. AI Screening Flow**
```
User Input (10 factors)
    → Retrofit API Call
    → Flask Server
    → StandardScaler Normalization
    → Logistic Regression Prediction
    → Risk Level + Advice
    → Saved to Room + Firestore
    → Displayed to User
```

---

## Machine Learning Model

### Training Notebook & Documentation
- **ML Training Notebook:** [`ml_training.ipynb`](ml_training.ipynb) — Complete model training pipeline
- **Full Technical Report:** [`docs/K1_Laporan_ML.pdf`](docs/K1_Laporan_ML.pdf) — Comprehensive ML documentation (Bahasa Indonesia)

### Dataset & Training
- **Source:** Student Mental Health Assessment Dataset (Kaggle)
- **Training Data:** 502 samples (401 train / 101 test)
- **Features:** 10 variables (academic, lifestyle, psychological, demographic)
- **Target:** Binary classification (Depression: Yes/No)

### Model Performance
| Metric | Score |
|--------|-------|
| **Accuracy** | 100% (test set) |
| **Cross-Validation Accuracy** | 98.26% ± 1.49% (5-fold) |
| **Precision** | 100% |
| **Recall** | 100% |
| **F1-Score** | 100% |
| **ROC AUC** | 1.000 |

**Note:** Perfect test scores indicate excellent model fit on current data. Model has been validated through cross-validation (98.26% avg) to ensure generalizability. Continuous monitoring recommended for real-world deployment.

### Input Features (10 Variables)
| Feature | Type | Range | Description |
|---------|------|-------|-------------|
| Gender | Binary | 0=Female, 1=Male | Biological sex |
| Age | Numeric | 18-34 | Student age |
| Academic Pressure | Ordinal | 1-5 | Level of academic stress |
| Study Satisfaction | Ordinal | 1-5 | Satisfaction with studies |
| Sleep Duration | Categorical | 4 options | Hours of sleep per night |
| Dietary Habits | Categorical | 0=Unhealthy, 1=Fair, 2=Healthy | Eating patterns |
| Suicidal Thoughts | Binary | 0=Never, 1=Yes | History of suicidal ideation |
| Study Hours | Numeric | 0-24 | Daily study duration |
| Financial Stress | Ordinal | 1-5 | Level of financial worry |
| Family History | Binary | 0=No, 1=Yes | Mental health history |

### Feature Importance (Logistic Regression Coefficients)
The following features show the strongest influence on depression prediction:

1. **Suicidal Thoughts** (3.30) - Strongest positive indicator
2. **Academic Pressure** (3.08) - High correlation with depression
3. **Age** (-2.04) - Protective factor (older students show lower risk)
4. **Study Satisfaction** (-1.88) - Protective factor
5. **Financial Stress** (1.68) - Moderate positive indicator
6. **Study Hours** (1.47) - Longer study hours increase risk
7. **Sleep Duration** (-1.12) - Protective factor (adequate sleep)
8. **Dietary Habits** (-1.14) - Protective factor (healthy eating)
9. **Family History** (0.85) - Mild positive indicator
10. **Gender** (0.32) - Minimal influence

### API Endpoint Example
```bash
POST http://YOUR_IP:5001/predict
Content-Type: application/json

{
  "gender": "Laki-laki",
  "age": 21,
  "academic_pressure": 4,
  "study_satisfaction": 3,
  "sleep_duration": "5-6 jam",
  "dietary_habits": "Cukup",
  "suicidal_thoughts": "Tidak Pernah",
  "study_hours": 8,
  "financial_stress": 3,
  "family_history": "Tidak"
}
```

**Response:**
```json
{
  "success": true,
  "prediction": 0,
  "prediction_label": "Tidak Ada Depresi",
  "confidence": 0.87,
  "risk_level": "Tidak",
  "advice": [
    "Pertahankan pola tidur sehat 7-8 jam per malam",
    "Kebiasaan makan seimbang kamu berkontribusi pada kesejahteraan mental",
    "Pertimbangkan olahraga teratur untuk menjaga kesehatan mental"
  ],
  "timestamp": "2025-01-10T10:26:11.478375"
}
```

---

## Getting Started

### Prerequisites

**For Android App:**
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Gradle 8.2+
- Android device/emulator running Android 7.0+ (API 24)

**For Flask ML API:**
- Python 3.9 or higher
- pip package manager
- Virtual environment (recommended)

---

### Installation Steps

#### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/mindcheck.git
cd mindcheck
```

---

#### 2. Set Up Flask ML Server

```bash
cd flask-api

# Create virtual environment
python3 -m venv venv

# Activate virtual environment
source venv/bin/activate  # macOS/Linux
# OR
venv\Scripts\activate     # Windows

# Install dependencies
pip install -r requirements.txt

# Run the Flask server
python app.py
```

The server will start on `http://0.0.0.0:5001`

**Verify server is running:**
```bash
curl http://localhost:5001/health
# Expected: {"status": "healthy", "model_loaded": true}
```

---

#### 3. Configure Android App

**A. Update API Endpoint**

Edit `app/src/main/java/com/example/mindcheck/data/remote/RetrofitClient.kt`:

```kotlin
// Replace with your machine's local IP address
private const val BASE_URL = "http://YOUR_IP_ADDRESS:5001/"
```

**Find your IP address:**
- **macOS/Linux:** `ifconfig | grep "inet " | grep -v 127.0.0.1`
- **Windows:** `ipconfig`

**Important:** Both Android device and Flask server must be on the **same WiFi network**.

**B. Set Up Firebase**

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project (or use existing)
3. Add Android app with package name: `com.mindcheck.app`
4. Download `google-services.json`
5. Place it in `app/` directory
6. Enable:
   - **Authentication** → Sign-in method → Google
   - **Firestore Database** → Create database (start in test mode)
   - **Analytics** (optional)

---

#### 4. Build & Run Android App

**Option A: Android Studio (Recommended)**
1. Open project in Android Studio
2. Wait for Gradle sync to complete
3. Connect Android device via USB (enable USB debugging) OR start emulator
4. Click **Run** button

**Option B: Command Line**
```bash
# Build APK
./gradlew assembleDebug

# Install to connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

#### 5. Alternative: USB Debugging with Port Forwarding

If WiFi connection fails, use ADB reverse:

```bash
# Forward port 5001 from device to computer
adb reverse tcp:5001 tcp:5001
```

Then update `RetrofitClient.kt`:
```kotlin
private const val BASE_URL = "http://localhost:5001/"
```

---

## Project Structure

```
mindcheck/
├── app/                                    # Android application
│   ├── src/main/
│   │   ├── java/com/mindcheck/app/
│   │   │   ├── data/
│   │   │   │   ├── local/                  # Room database
│   │   │   │   │   ├── dao/                # Data Access Objects
│   │   │   │   │   ├── entity/             # Database entities
│   │   │   │   │   └── database/           # AppDatabase
│   │   │   │   ├── remote/                 # Retrofit API
│   │   │   │   │   ├── api/                # API service interfaces
│   │   │   │   │   └── RetrofitClient.kt   # API configuration
│   │   │   │   ├── firebase/               # Firestore repositories
│   │   │   │   └── repository/             # Data repositories
│   │   │   ├── presentation/               # UI layer
│   │   │   │   ├── auth/                   # Login/Register
│   │   │   │   ├── home/                   # Dashboard
│   │   │   │   ├── screening/              # AI screening
│   │   │   │   ├── mood/                   # Mood tracking
│   │   │   │   ├── journal/                # Gratitude journal
│   │   │   │   ├── goals/                  # Self-care goals
│   │   │   │   ├── emergency/              # Crisis support
│   │   │   │   └── profile/                # User profile
│   │   │   ├── utils/                      # Utilities
│   │   │   └── MindCheckApplication.kt     # Application class
│   │   └── res/
│   │       ├── layout/                     # XML layouts
│   │       ├── drawable/                   # Icons & graphics
│   │       ├── values/                     # Colors, strings, themes
│   │       └── navigation/                 # Navigation graph
│   └── build.gradle.kts                    # App-level dependencies
│
├── flask-api/                              # Machine Learning backend
│   ├── app.py                              # Flask server
│   ├── model.pkl                           # Trained Logistic Regression
│   ├── scaler.pkl                          # StandardScaler
│   ├── requirements.txt                    # Python dependencies
│   └── SETUP_INSTRUCTIONS.md               # Setup guide
│
├── docs/                                   # Documentation
│   └── K1_Laporan_ML.pdf                   # ML technical report (Bahasa Indonesia)
│
├── video-demo/                             # Application demo video
│
├── ml_training.ipynb                       # Complete ML training pipeline
├── model_testing_colab.ipynb               # ML model testing notebook
├── .gitignore                              # Git ignore rules
├── build.gradle.kts                        # Project-level Gradle
├── settings.gradle.kts                     # Gradle settings
└── README.md                               # This file
```

---

## API Documentation

### Base URL
```
http://YOUR_IP_ADDRESS:5001
```

### Endpoints

#### 1. Health Check
```http
GET /health
```

**Response:**
```json
{
  "status": "healthy",
  "model_loaded": true,
  "timestamp": "2025-01-10T10:15:51.958073"
}
```

---

#### 2. Predict Depression Risk
```http
POST /predict
Content-Type: application/json
```

**Request Body:**
```json
{
  "gender": "Laki-laki",
  "age": 21,
  "academic_pressure": 4,
  "study_satisfaction": 3,
  "sleep_duration": "7-8 jam",
  "dietary_habits": "Sehat",
  "suicidal_thoughts": "Tidak Pernah",
  "study_hours": 6,
  "financial_stress": 2,
  "family_history": "Tidak"
}
```

**Response (No Depression):**
```json
{
  "success": true,
  "prediction": 0,
  "prediction_label": "Tidak Ada Depresi",
  "confidence": 0.92,
  "risk_level": "Tidak",
  "advice": [
    "Pertahankan pola tidur sehat 7-8 jam per malam",
    "Kebiasaan makan seimbang kamu berkontribusi pada kesejahteraan mental",
    "Pertimbangkan olahraga teratur untuk menjaga kesehatan mental",
    "Jaga hubungan sosial dengan teman dan keluarga"
  ],
  "timestamp": "2025-01-10T10:26:11.478375"
}
```

**Response (Depression Detected):**
```json
{
  "success": true,
  "prediction": 1,
  "prediction_label": "Risiko Depresi Terdeteksi",
  "confidence": 0.84,
  "risk_level": "Ya",
  "advice": [
    "Pertimbangkan berbicara dengan konselor kampus atau profesional kesehatan mental segera",
    "Prioritaskan tidur 7-8 jam setiap malam - kualitas tidur sangat penting untuk kesehatan mental",
    "Jika kamu memiliki pikiran menyakiti diri, hubungi hotline krisis: 119 ext 8 (24/7)",
    "Bicarakan beban akademik dengan dosen/advisor untuk mencari solusi",
    "Hubungi teman, keluarga, atau kelompok dukungan yang dipercaya",
    "Ingat: mencari bantuan adalah tanda kekuatan, bukan kelemahan"
  ],
  "timestamp": "2025-01-10T10:26:11.478375"
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Missing required fields: age, sleep_duration"
}
```

---

## Demo Video

**[Watch Demo Video on Google Drive](https://drive.google.com/file/d/1fApwuRyQ04DbhqrmDD108jNEBfLdfMn0/view)**

The demonstration video showcases:
- Complete user onboarding flow
- Google Sign-In authentication
- AI mental health screening process
- Daily mood and sleep tracking
- Gratitude journaling
- Self-care goal management
- Emergency support features (breathing exercises, grounding techniques)
- Analytics dashboard and progress visualization
- Profile management and settings

---

## Team

**Kelompok 10 — Teknik Informatika, Politeknik Caltex Riau (TA 2025/2026)**

| Name | Student ID | Role |
|------|-----------|------|
| **Mohamad Haziq Dafren** | 2355301119 | Backend Developer, ML Engineer (Data Cleaning, EDA, Model Preparation), System Architecture |
| **Andika Syuhada** | 2355301017 | Frontend Developer (Android UI/UX), ML Model Training, Documentation |

### Responsibilities Breakdown

**Mohamad Haziq Dafren:**
- Flask API development and deployment
- Machine learning pipeline (data cleaning, exploratory data analysis, preprocessing)
- Model preparation and feature engineering
- Retrofit integration in Android app
- System architecture design
- Backend logic and data flow implementation

**Andika Syuhada:**
- Android UI/UX design using Material Design 3
- Room database implementation
- Firebase integration (Authentication, Firestore)
- Machine learning model training and evaluation
- Project documentation and technical writing
- User testing and feedback collection

### Academic Supervisors
- **Supervisor:** Dr. Juni Nurma Sari, S.Kom., M.MT.
- **Lab Instructor:** Rezky Kurniawan, S.Tr.Kom.

---

## License

This project is developed as a final year capstone project for Politeknik Caltex Riau (TA 2025/2026).

**For academic and portfolio purposes only.** Not intended for commercial use.

---

## Acknowledgments

- **Politeknik Caltex Riau** — Academic support and resources
- **Dr. Juni Nurma Sari & Rezky Kurniawan** — Project supervision and guidance
- **Kaggle Community** — Student Mental Health Assessment dataset
- **Open Source Libraries:**
  - [Retrofit](https://square.github.io/retrofit/) — Type-safe HTTP client
  - [Room](https://developer.android.com/training/data-storage/room) — SQLite abstraction
  - [Firebase](https://firebase.google.com) — Cloud services
  - [scikit-learn](https://scikit-learn.org) — Machine learning framework
  - [Flask](https://flask.palletsprojects.com) — Lightweight web framework
  - [Material Design](https://m3.material.io/) — UI design system

---

## Mental Health Support Resources

**MindCheck is a wellness tool and NOT a substitute for professional mental health care.**

If you or someone you know is experiencing a mental health crisis:

- **Indonesia Crisis Hotline:** 119 ext 8 (24/7)
- **International Association for Suicide Prevention:** [iasp.info](https://www.iasp.info/resources/Crisis_Centres/)
- **Campus Counseling Services** — Contact your university's student affairs office

**Always consult qualified healthcare providers for diagnosis and treatment.**

---

## Future Roadmap

- iOS version development
- Advanced ML models comparison (Random Forest, Neural Networks)
- Real-time peer support chat (moderated)
- Wearable device integration (Fitbit, Apple Watch)
- Multi-language support (English, Bahasa Indonesia)
- Therapist connection feature
- Export wellness reports (PDF)
- Cloud-based model retraining pipeline

---

**Developed for mental health awareness by students, for students.**

**Politeknik Caltex Riau — TA 2025/2026**
