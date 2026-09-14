# MeshChess

<p align="center">
  <img src="app/src/main/res/drawable/app_logo.xml" alt="MeshChess Logo" width="100"/>
</p>

**MeshChess** is an innovative, peer-to-peer (Mesh) and online chess game created by **Luberisse Karl Brad (Karlito)**. Designed initially for Android, the game now also supports a desktop terminal version.

## 🚀 Features

### Cross-Platform Play
- **Android App**: Play seamlessly on your smartphone with a beautiful Jetpack Compose UI.
- **Desktop / Terminal PC**: Play a local game on your computer by simply running `Main.py`. No external dependencies are required.

### 🌐 Multiple Game Modes
- **Solo Mode**: Play against powerful AI models (ChatGPT, Claude, Gemini, DeepSeek, Qwen). *Note: Requires internet and your own API key.*
- **Online Mode**: Real-time multiplayer using a personal WebSocket server hosted on Railway.
- **Mesh Network Mode**: Play over Bluetooth or WiFi Mesh, perfect for areas with no internet connection.
- **Offline / Hotseat**: Pass and play on the same device.

### 🔄 Advanced Functionality
- **GitHub Auto-Updater**: The Android app can automatically detect, download, and install new updates directly from the GitHub releases page.
- **Save & Replay**: Export your games (including AI matches) in `.json` format and import them later to analyze or replay.
- **QR Code Sharing**: Share a match instantly with a friend by scanning a QR Code (in P2P mode).
- **Multilingual Support**: Fully localized in French (FR), English (EN), and Spanish (ES). The app automatically adapts or lets you choose your language at launch.

## 🛠️ Architecture

- **Engine**: The core chess logic is written entirely in **Python** (`chess_rules.py`) for simplicity and efficiency. 
- **Android Integration**: The Python engine is embedded seamlessly into the Android Kotlin app using **Chaquopy**.
- **Server**: The online mode relies on a WebSocket server located in the `mesh-server` directory (`https://meshchess-server-production.up.railway.app`).

## 💻 How to play on PC

Ensure you have Python 3 installed. Navigate to the project root and run:

```bash
python3 app/src/main/java/com/karlitodev/meshchess/python/Main.py
```

## 📱 Permissions
To ensure smooth gameplay and features, the Android app requests the following permissions on launch:
- `Bluetooth` & `WiFi`: For Mesh networking and P2P matches.
- `Camera`: For QR Code scanning.
- `Internet`: For Online play and AI integrations.
- `Request Install Packages`: For the GitHub auto-updater to work.

## 👨‍💻 About the Creator
**Creator:** Luberisse Karl Brad (Alias: Karlito)
**Age:** 17
*Grand fan d'anime, webtoon, manga, manhuas et manhwa et aime aussi la culture asiatique.*