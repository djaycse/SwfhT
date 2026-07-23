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
