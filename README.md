# Wear OS companion for Tasks.org — build & install

## Layout
- `wear/` — Wear OS app: task list UI, DataLayer **sender** (sends complete/undo requests to the phone) and a `WearableListenerService` that receives task snapshots from the phone.
- `phone/` — phone-side library/app module: Astrid API `ContentResolver` bridge (reads/writes the Tasks.org content provider) plus the DataLayer listener that answers wear requests and pushes updates.

## Build
Requires JDK 17 + Android SDK with platform 34 and Wear OS 4.0 (API 33+) emulator or device.

```bash
./gradlew :wear:assembleDebug :phone:assembleDebug
adb install -r wear/build/outputs/apk/debug/wear-debug.apk
adb install -r phone/build/outputs/apk/debug/phone-debug.apk
```

## Protocol (DataLayer)
Paths are defined once in `shared/Protocol.kt` (copied into both modules):

| Path | Direction | Payload |
|---|---|---|
| `/tasks/snapshot` | phone → wear | JSON array of open tasks |
| `/tasks/request` | wear → phone | empty, triggers snapshot push |
| `/tasks/complete` | wear → phone | `{ "id": <long> }` |
| `/tasks/uncomplete` | wear → phone | `{ "id": <long> }` |

JSON encoding uses `org.json` (no extra dependency).

## Astrid API notes
The Tasks.org app exposes the `org.tasks.content` authority via its public
[Astrid API](https://github.com/tasks/tasks/tree/main/app/src/main/java/org/tasks/activities)
(`AstridContentProvider`). Columns used here: `_id`, `TITLE`, `COMPLETED`,
`DUE_DATE`, `DELETED`. Declare
`<uses-permission android:name="org.tasks.permission.READ_TASKS"/>` /
`WRITE_TASKS`; the user must grant them at runtime on Android 6+.
