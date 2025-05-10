# Distributed Real-Time Music Sharing System 

This is a Java-based project for **real-time audio streaming** over a network. 
It allows music to be streamed from a server to one or more clients using TCP. 
It supports playback of WAV audio files and is designed for low latency and simplicity, making it ideal for LAN-based music sharing.

  Features

-  Real-time audio streaming of WAV files
-  AES encryption for secure transmission
-  Cross-device support on the same network
-  Simple file-based audio source (`.wav`)
-  Audio output on client device using Java Sound API

> Note: GUI-based controls (play/pause, volume, track display) are planned for future updates.

Technologies Used

- Java 17+ (Tested on JDK 23 with JavaFX)
- Java Sound API
- Maven for build and dependency management
- AES (Advanced Encryption Standard)
- TCP sockets for real-time communication

## 🖥️ How It Works

1. The server reads and streams a `.wav` file over TCP after encrypting it.
2. The client connects to the server, decrypts incoming audio, and plays it in real time.
3. The system is designed to work in a local network environment for low-latency audio sharing.

## 📦 Setup and Run

### Prerequisites

- Java (preferably Liberica or ZuluFX with JavaFX)
- Maven
- A `.wav` file placed in the root directory
