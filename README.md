# 🔍 Lost and Found - Lost Item Search Application

<div align="center">
  <img width="200" alt="image" src="https://github.com/user-attachments/assets/94125fdd-78ad-4232-b4ad-3e1faba7af99" />

  
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
  [![Java](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
  [![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow.svg)](https://firebase.google.com/)
  [![Academic](https://img.shields.io/badge/Project-Academic-blue.svg)]()
</div>

## 📋 Table of Contents
- [Introduction](#-introduction)
- [Features](#-features)
- [Technologies](#-technologies)
- [Installation](#-installation)
- [Usage Guide](#-usage-guide)
- [System Architecture](#-system-architecture)
- [Screenshots](#-screenshots)
- [Contributing](#-contributing)
- [Authors](#-authors)
- [License](#-license)

## 📖 Introduction

**Lost and Found** is an Android mobile application that helps users post, search, and share information about lost or found items within schools or small areas. The app creates a supportive community, increasing the chances of recovering lost belongings quickly and efficiently.

### 🎯 Objectives
- Reduce time spent manually searching for lost items through technology
- Create a community connection channel within schools/small areas
- Provide intuitive experience with maps and images
- Support real-time data synchronization

### 📚 Project Information
- **Course:** Mobile Application Development (NT118.Q14)
- **University:** University of Information Technology - VNU-HCM
- **Advisor:** MSc. Tran Manh Hung
- **Academic Year:** 2024-2025

## ✨ Features

### Core Features
- 🔐 **User Authentication:** Secure registration/login with Firebase Authentication
- 📝 **Post Creation:** Create posts about lost or found items with images, descriptions, and location
- 🔍 **Smart Search:** Search posts by keywords with ML Kit support
- 🗺️ **Interactive Map:** View item locations on Vietmap
- 📍 **Location Services:** Automatically get current location and filter "nearby" posts
- 💬 **Direct Chat:** 1-on-1 messaging between item owners and finders
- 💭 **Comments:** Public discussion under posts
- 🔔 **Notifications:** Receive notifications for interactions (comments, messages)
- 📊 **History:** Manage personal posts

### Highlighted Features
- 🤖 **AI Image Labeling:** Automatically suggest item tags from images (Google ML Kit)
- ⚡ **Real-time Sync:** Instant data updates with Firebase Realtime Database
- 📱 **User-Friendly Interface:** Modern, easy-to-use design
- 🎨 **Color Differentiation:** Orange for "Lost", Green for "Found"

## 🛠️ Technologies

### Platform & Language
- **Platform:** Android (API 21+)
- **Language:** Java
- **IDE:** Android Studio

### Backend & Database
- **Firebase Authentication:** User authentication
- **Firebase Realtime Database:** Real-time data storage and synchronization

### Libraries & SDK
| Technology | Purpose |
|-----------|----------|
| Vietmap SDK | Display maps and markers |
| Google Location Services | Get current location |
| Google ML Kit | Image Labeling - Object recognition |
| Glide | Load and display images |
| OkHttp & Retrofit | Network communication |
| Gson | JSON parsing |

### Architecture
- **Model:** Client-Server
- **Design Pattern:** MVVM (Model-View-ViewModel)
- **Navigation:** Bottom Navigation + Fragment

## 📥 Installation

### System Requirements
- Android Studio 4.0 or higher
- JDK 8 or higher
- Android SDK (API Level 21+)
- Android device or Emulator

### Installation Steps

1. **Clone repository**
```bash
git clone https://github.com/BooBoo0531/Lost-And-Found---Mobile-App.git
cd Lost-And-Found---Mobile-App
```

2. **Open project in Android Studio**
- Open Android Studio
- Select `File` → `Open`
- Choose the cloned project folder

3. **Configure Firebase**
- Download `google-services.json` from Firebase Console
- Place the file in `app/` folder
- Configure Firebase Realtime Database (region: asia-southeast1)

4. **Sync Gradle**
```bash
./gradlew sync
```

5. **Build & Run**
- Connect Android device or start Emulator
- Click `Run` or press `Shift + F10`

## 📱 Usage Guide

### Register & Login
1. Open the app for the first time
2. Select "Sign Up" to create a new account
3. Enter email and password
4. Or login if you already have an account

### Post Lost/Found Items
1. Tap **Floating Action Button** (FAB) - red (Lost) or green (Found)
2. Select item image from gallery
3. System automatically suggests tags (AI)
4. Fill in detailed description
5. Select location on map
6. Enter contact information
7. Tap "Post"

### Search for Items
1. Use search bar in **HomeActivity**
2. Enter keywords or capture/upload image for AI search
3. View matching results

### View on Map
1. Switch to **Map** tab
2. View red (Lost) and green (Found) markers
3. Tap marker to view information
4. Enable location to see "Nearby Posts"

### Contact & Interaction
1. **Comment:** Tap on post → Write public comment
2. **Chat:** Tap message icon → Private 1-on-1 chat
3. **Notifications:** Check **Notify** tab to see new interactions

## 🏗️ System Architecture
<img width="971" height="1077" alt="image" src="https://github.com/user-attachments/assets/c9fb6e11-b560-4505-8537-aaf3f9f89f04" />
<img width="1919" height="849" alt="image" src="https://github.com/user-attachments/assets/25f39af9-9129-462a-acf9-55eb1fadcf89" />

### Data Flow
```
User → Firebase Auth → Authenticated
  ↓
HomeActivity → Load Posts from RTDB → Display in RecyclerView
  ↓
PostActivity → Upload Image + Data → RTDB → Real-time Update
  ↓
MapFragment → Load Markers → Vietmap → Display on Map
  ↓
ChatActivity → Send Message → RTDB → Real-time Sync
```

## 📸 Screenshots

### Authentication & Home
<table>
  <tr>
    <td align="center">
      <img width="250" alt="Login Screen" src="https://github.com/user-attachments/assets/ea6377e8-d9df-4ed4-8d15-9a06d03eb7f9" />
      <br />
      <b>Login Screen</b>
      <br />
      <sub>User authentication with Firebase</sub>
    </td>
    <td align="center">
      <img width="250" alt="Home Activity" src="https://github.com/user-attachments/assets/ae069db4-cfde-4c19-b399-59f09c4ec92c" />
      <br />
      <b>Home Feed</b>
      <br />
      <sub>Browse lost & found posts</sub>
    </td>
  </tr>
</table>

### Post Creation & Map
<table>
  <tr>
    <td align="center">
      <img width="250" alt="Create Post" src="https://github.com/user-attachments/assets/28851bf3-e782-4eb1-8f40-d6184138e6b0" />
      <br />
      <b>Create Post</b>
      <br />
      <sub>Post Lost items with details</sub>
    </td>
    <td align="center">
      <img width="250" alt="Create Post" src="https://github.com/user-attachments/assets/299fe25b-7373-4a3c-965c-5c0f9df74040" />
      <br />
      <b>Create Post</b>
      <br />
      <sub>Post Found items with details</sub>
    </td>
    <td align="center">
      <img width="250" alt="Map View" src="https://github.com/user-attachments/assets/e81bb8bb-b1bc-4dc7-bba1-07f9eeda15e4"/>
      <br />
      <b>Map View</b>
      <br />
      <sub>Interactive map with Vietmap SDK</sub>
    </td>
    <td align="center">
      <img width="250" alt="Location Picker" src="https://github.com/user-attachments/assets/726c42a1-9d67-49c2-85a4-a02d472ca229" />
      <br />
      <b>Location Picker</b>
      <br />
      <sub>Select exact location on map</sub>
    </td>
  </tr>
</table>

### Communication & History
<table>
  <tr>
    <td align="center">
      <img width="250" alt="Notification" src="https://github.com/user-attachments/assets/bf9e06f9-ea7d-4858-8038-82e259d59a33" />
      <br />
      <b>Notification</b>
      <br />
      <sub>User notifications</sub>
    </td>
    <td align="center">
      <img width="250" alt="Chat List" src="https://github.com/user-attachments/assets/d843f4c8-a595-46c2-aa39-64cfcb955aa6" />
      <br />
      <b>Chat List Activityt</b>
      <br />
      <sub>Conversation List</sub>
    </td>
    <td align="center">
      <img width="250" alt="Chat" src="https://github.com/user-attachments/assets/46c9f8f6-f449-4b08-b36d-c355fca8196e" />
      <br />
      <b>Chat Activityt</b>
      <br />
      <sub>Real-time messaging</sub>
    </td>
    <td align="center">
      <img width="250" alt="History" src="https://github.com/user-attachments/assets/0f542552-e6d2-4c10-9acc-20e609d8ed91" />
      <br />
      <b>Post History</b>
      <br />
      <sub>Manage your posts</sub>
    </td>
  </tr>
</table>

## 🧪 Testing

### Test Cases
| Test ID | Feature | Result |
|---------|---------|--------|
| TC01 | Register/Login | ✅ Pass |
| TC02 | Post Lost/Found | ✅ Pass |
| TC03 | ML Kit Tag Suggestion | ✅ Pass |
| TC04 | Map & Markers | ✅ Pass |
| TC05 | Comments & Notifications | ✅ Pass |
| TC06 | Real-time Chat | ✅ Pass |

### Testing Environment
- Android Emulator (Android 12/13)
- Physical Device: Android 9+
- Network: Wi-Fi / 4G

## 🚀 Future Development

- [ ] Migrate to Firebase Storage for images (replace Base64)
- [ ] Local Caching & Offline Mode
- [ ] Content moderation and report system
- [ ] Push Notifications with FCM
- [ ] Advanced filters (radius, time, status)
- [ ] Multi-language support
- [ ] Dark Mode
- [ ] iOS/Web platform expansion

## 🤝 Contributing

We welcome all contributions! To contribute:

1. Fork the repository
2. Create a new branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 👥 Authors

**Group 24 - Class NT118.Q14**

| Member | Student ID | Email |
|--------|-----------|-------|
| Do Hong Ngan | 23520994 | 23520994@gm.uit.edu.vn |
| Vo Thi Hong Phuc | 23521226 | 23521226@gm.uit.edu.vn |

**Advisor:** MSc. Tran Manh Hung

## 📞 Contact

- **GitHub:** [BooBoo0531/Lost-And-Found---Mobile-App](https://github.com/BooBoo0531/Lost-And-Found---Mobile-App)
- **Email:** 23520994@gm.uit.edu.vn
- **Email:** 23521226@gm.uit.edu.vn

## 📄 License

This project is an academic assignment developed for the Mobile Application Development course (NT118) at the University of Information Technology - VNU-HCM. 

**For Educational Purposes Only**

This application was created as part of coursework and is intended solely for:
- Academic evaluation and grading
- Learning and educational demonstration
- Portfolio and skill showcase

All rights reserved by the authors. If you wish to use any part of this project, please contact the authors for permission.

---

<div align="center">
  <p>Made with ❤️ by Group 24 - NT118.Q14</p>
  <p>© 2025 Lost and Found App. All rights reserved.</p>
</div>
