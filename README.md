# Net Atlas — Android 16 prototype

This is a new Android Studio project, not a modified Spy Monitor APK. It contains:
- Android 16 / API 36 project configuration
- MAP and LIST tabs
- Installed-app listing using package visibility permission
- OpenStreetMap world map and current-device location marker (after location permission)
- Explicit UI status that live network destinations are not yet monitored

## Important limitation
This prototype does **not** yet inspect per-app remote IP addresses or draw destination-country lines. Android does not expose this information to an ordinary app through the installed-app list or standard permissions. A production implementation needs a consent-based `VpnService` plus a packet-forwarding backend; simply routing traffic into a VPN without forwarding it would break connectivity, so no such incomplete service is enabled here. IP geolocation would also need a provider and privacy disclosure.

## Build
Open this folder in Android Studio with Android SDK Platform 36 installed. Let Gradle sync, then Build > Build APK(s). The build environment used to prepare this source archive did not have Android SDK / Gradle installed, so this project has not been compiled or device-tested here.

## Privacy / map
- Location is requested at runtime and is only used to show the device marker.
- The map uses OpenStreetMap tiles and Leaflet from their public HTTPS endpoints, so map rendering needs internet access.
- `QUERY_ALL_PACKAGES` is included for a sideloaded personal-use app so the app list can include installed packages. Distribution through Google Play would require policy review.
