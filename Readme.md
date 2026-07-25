# SwfhT - Attendance Tracker

SwfhT is a lightweight Android application designed to help employees track their office attendance and ensure they meet organizational requirements. It provides a simple calendar interface to log planned and actual work locations, automatically calculating key statistics such as in-office percentage and "Team Hub" days.

## Key Features
- **Manual Logging**: Tap to set planned work locations and long-press to set actual attendance.
- **Visual Statistics**: Real-time tracking of office attendance vs. WFH requirements.
- **Auto-Detection**: Optional background scanning that detects office Wi-Fi networks and uses GPS to automatically log your attendance when you are at a configured office location.
- **Customizable Locations**: Configure up to 5 office locations with custom names and GPS coordinates.
- **Flexible Settings**: Adjust polling intervals and target Wi-Fi networks to suit your workplace environment.

---

## Changelog

### Version 1.3.1
*   **Visual Polish**: Switched "Day off" highlights to a dedicated brown theme and disabled dynamic coloring to ensure a consistent brand identity regardless of device wallpaper.
*   **Progress Accuracy**: Improved visual feedback for progress bars by adding a visible track and using square stroke caps, making it easier to see exactly how close you are to your goals.
*   **Calculation Fix**: Updated the in-office percentage calculation to use mathematical rounding, resolving discrepancies where the display was slightly lower than expected due to integer truncation.

### Version 1.3.0
*   **New Feature: Attendance Goals**: Requirements (50% office, 5 days Team Hub) are now fully configurable via a new settings screen.
*   **New State: Leave Tracking**: Added a "Leave" option to the location toggle. Leave days are visually distinguished and automatically excluded from attendance percentage and streak calculations.
*   **UI & UX Refinement**:
    *   **High-Contrast Calendar**: Weekends and leave days are now highlighted with high-contrast backgrounds (Black in light theme, White in dark theme) for better visibility.
    *   **Simplified Office Locations**: Combined Latitude and Longitude into a single field with flexible parsing and improved keyboard visibility.
    *   **Improved Navigation**: Added system back button handling for all settings screens to return to the dashboard instead of closing the app.
    *   **Legend & Instructions Update**: Redesigned the legend and updated "How to Use" for better clarity and consistent visual language.
    *   **Smart Detection Display**: The "Currently at" label now only shows when a known office has been auto-detected.

### Version 1.2.2
*   **Bug-fix**: Version string again not updated in UI (D'oh!).

### Version 1.2.1
*   **Bug-fix**: Version string was not updated in UI.

### Version 1.2.0
*   **UI Layout Overhaul**: Anchored the Legend and Stats sections to the bottom of the screen for constant visibility while the calendar remains scrollable.
*   **Enhanced Stats Visualization**:
    - Redesigned the Stats panel into "In Office" and "Team Hub" categories.
    - Introduced "Planned" vs "Actual" progress bars for each category.
    - Added real-time status icons (green tick/red cross) to track progress toward the 50% office and 5-day Team Hub requirements.
*   **Smart Detection Status**:
    - Added a "Currently detected location" label on the main dashboard.
    - Displays the specific configured office name (e.g., "HQ") instead of generic categories when a match is found.
    - Triggers an immediate location scan on app launch and after saving settings for instant feedback.
*   **API Modernization**:
    - Migrated Wi-Fi detection to modern `ConnectivityManager` APIs for better support on Android 12+ and 13+.
    - Refined GPS detection radius to 50m for improved accuracy.
*   **Reliability & Performance**:
    - Implemented date key normalization to ensure data consistency between manual entries and background detections.
    - Added user-friendly Toast notifications when an office location is automatically detected.

### Version 1.1.0
*   **Office Auto-Detection**: Introduced a background service that periodically polls for specific office Wi-Fi networks.
*   **GPS Integration**: Added GPS cross-referencing to automatically update your "Actual" attendance state when within 50m of a configured office.
*   **Office Locations Management**: New screen to manage up to 5 office locations with custom names and coordinates.
*   **Current Location Sync**: Added a "Set to current location" button to easily capture GPS coordinates using the device's sensors.
*   **Enhanced Wi-Fi & Polling Settings**: 
    *   Ability to configure the specific Wi-Fi SSID to trigger detection.
    *   Adjustable polling interval (default 30 minutes).
*   **UI Optimizations**:
    *   Added vertical scrolling to the main dashboard to support smaller screens.
    *   Redesigned the statistics panel into a compact two-column layout.
    *   Consolidated the legend into a dedicated, styled panel for better readability.
    *   Optimized the calendar grid to only render necessary rows, saving vertical space.
*   **Improved User Experience**: Refactored settings screens with local state management for smoother text entry and explicit saving.

### Version 1.0.0
*   Initial release.
*   Manual tracking of planned and actual work days (Home, Team Hub, Other Office).
*   Monthly statistics for 50% in-office and 5-day Team Hub requirements.
*   Persistent storage using Jetpack DataStore.
