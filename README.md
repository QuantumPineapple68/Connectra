# Connectra - Skill-Swap Social Platform

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
[![Database](https://img.shields.io/badge/Database-Firebase-yellow.svg)](https://firebase.google.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![PlayStore](https://img.shields.io/badge/Download-Google%20Play-brightgreen.svg)](https://play.google.com/store/apps/details?id=com.nachiket.connectra&hl=en_IN)

## 📱 Download Now

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" alt="Download on Google Play" height="80">](https://play.google.com/store/apps/details?id=com.nachiket.connectra&hl=en_IN)

## 🧠 Overview

Connectra is a skill-based social networking platform designed to connect students and professionals for mutual growth through skill exchange. Unlike conventional social networks focused on content consumption, Connectra facilitates purposeful connections where users can leverage complementary expertise to accelerate their learning and development.

![1](https://github.com/user-attachments/assets/6c8b75d5-3b60-41e7-840f-7251c7776a57)
![2](https://github.com/user-attachments/assets/c1c4e276-4136-4993-8418-da1d946efe75)
![3](https://github.com/user-attachments/assets/d7fdc5e0-3dcb-490e-8deb-035df1a9cbb5)
![4](https://github.com/user-attachments/assets/9dc472ca-9c49-4b13-8d42-e53fdbcb5db5)
![5](https://github.com/user-attachments/assets/3d9129fd-ebb8-4119-b229-9b62e7425b0f)
![5](https://github.com/user-attachments/assets/31f1eaf5-0d40-4c53-a3bc-b1f02d970f15)
![6](https://github.com/user-attachments/assets/f36efdb0-f1d5-4d25-9156-00104f109872)

---

## Feature Overview

### 1. Firebase Authentication
Security-focused user authentication system enabling secure access via email/password credentials with "Forgot Password" section.

### 2. Profile Management System
Comprehensive user profile framework allowing users to create and manage personalized profiles with contact information, skill listings, and profile media. Supports certificate uploads for skill validation.

### 3. Skill Discovery Interface
Scrollable tile-based interface displaying user profiles organized by skills, with integrated search functionality for targeted skill discovery. Users can browse profiles.

### 4. Real-time Chat System
Fully synchronized messaging platform with emoji support, message reactions, and content moderation. Features an Instagram-inspired inbox layout with optimized message rendering.

### 5. Task Management
Collaborative to-do list functionality enabling users to create, assign, and track tasks. Supports synchronization between skill partners for enhanced productivity tracking.

### 6. Ratings & Reviews
Post-collaboration feedback mechanism allowing users to rate interactions and provide qualitative feedback. Aggregated ratings appear on user profiles to establish credibility.

### 7. Media Management
Secure storage and retrieval system for profile pictures and skill certificates with content validation. Implements optimized compression for efficient bandwidth usage.

### 8. Safety Control
System to Block and Report accounts for NSFW behaviour. (Admin can band/ Suspend users)

## Technical Implementation

### Architecture & Core Components
- **Dependency Management**: Gradle-based dependency injection with modularized feature components
- **UI Framework**: XML-based layouts with Material Design components and custom view extensions

### Firebase Integration
- **Authentication**: Secure FirebaseAuth implementation with "Forgot Password" support.
- **Database**: Structured NoSQL schema with normalized references and optimized query paths
- **Storage**: CloudStorage implementation with content-type validation and access control rules
- **Security Rules**: Path-specific auth validation rules ensuring `auth != null` across all operations

### Frontend Implementation
- **UI Components**: RecyclerView with custom adapters for skill tiles, ChatMessageAdapter for messaging interface
- **Resource Handling**: Optimized drawable resources with density-specific assets

### Backend Systems
- **Data Synchronization**: Realtime Database listeners with connection state handling
- **Content Moderation**: Custom profanity filter with replaceable dictionary


### System Requirements
- Android 9.0 (Pie) or higher
- Internet connectivity
- Storage permissions for media uploads



## 📋 Implementation Guidelines

### Database Schema

```
connectra-db/
├── users/
│   ├── [uid]/
│   │   ├── profile/
│   │   │   ├── basic_info
│   │   │   ├── skills
│   │   │   └── certificates
│   │   └── ratings/
├── chats/
│   ├── [chat_id]/
│   │   ├── messages/
│   │   └── metadata/
└── tasks/
    └── [task_id]/
```


## 🌱 Future Roadmap

- **Advanced Matching Algorithm**: Machine learning-based skill compatibility analysis
- **Video Conferencing**: Integrated peer-to-peer video sessions
- **Skill Verification**: Third-party certificate validation system
- **Community Forums**: Topic-based discussion boards for group learning
- **Monetization Options**: Premium features for professional skill providers

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](https://github.com/QuantumPineapple68/Connectra/blob/master/LICENSE) file for details.

## 📞 Contact

For support or inquiries, please contact me +91 78228-51019 or nachiketj14@gmail.com

---

Made with ❤️ for learners and teachers everywhere
