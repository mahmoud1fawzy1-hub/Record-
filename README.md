# Floating Voice Recorder (Android App)

A lightweight Android application with a floating overlay bar that draws over other apps.

### Features
1. **Draws Over Other Apps**: Uses Android's `SYSTEM_ALERT_WINDOW` and `TYPE_APPLICATION_OVERLAY` via a background Foreground Service.
2. **Simple & Un-styled Floating Bar**:
   - **Record**: Starts audio recording via microphone.
   - **Pause**: Pauses and resumes recording on the fly.
   - **Finish/Preview**: Stops recording and immediately starts playback for instant preview!
   - No save button or complex UI.
3. **Draggable**: Drag anywhere across the screen using the left grip handle.

### How to Build & Run in Android Studio
1. Open Android Studio and select **Open** -> choose this project directory.
2. Connect an Android phone or launch an Android emulator (API 24+).
3. Click **Run (Shift + F10)**.
4. When prompted, grant:
   - **"Draw over other apps"** permission in Android Settings.
   - **"Record Audio"** permission.
5. The simple 3-button floating overlay bar will appear on screen. Switch to any app (WhatsApp, Browser, Home screen, Games) - the bar stays on top!
