# Runehearth — manual testing guide

## Build and install
1. Install Android Studio (or JDK 17+ and the Android SDK 36).
2. `gradlew.bat :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` (release: `:app:assembleRelease`, then sign).
3. `adb install -r app-debug.apk` or Run from Android Studio. Automated tests: `gradlew.bat :app:testDebugUnitTest` and
   `gradlew.bat :app:connectedDebugAndroidTest` (needs a device/emulator).

## 1. Single device (Local Only) — 10 minutes
1. Home → **Sample campaign**: you land in the DM "Hall" with 3 characters, a quest, an encounter and rolls.
2. **Party** → open *Aelwyn*: check Overview (HP/MP bars, final stats "+gear"), Skills (Cast reduces mana), Gear (equipped slots),
   Pack (search, filter chips, sort, tap an item → compare / equip / use / move), Effects (add *Blessed*), Journal.
3. In Pack, **Add item** → pick a template or open the image picker: choose a design, colour and toggle **Enchant glow**.
4. **Purse**: add `2 gold`, remove coins, transfer between purses, use the converter, toggle the eye/lock to hide a purse.
5. **Dice**: tap d20; watch the tumble → total → tier text (Low / Low-Medium / Medium / High-Medium / High) and critical banners.
   Try Advantage, a custom `4d6+2`, a coin toss, a private roll. Hall → Tools lets you change the tier thresholds.
6. **Combat**: pick the sample encounter → Roll initiative → Start → Next turn; damage/heal enemies; add a status; End.
7. **Lore**: create a quest with a reward, Complete it: the reward lands in the party purse (Purse tab history).
8. Back to Home → export (share icon) the campaign, delete it, Import it again.
9. Settings → Performance: try Lite and compare.

## 2. Two phones (Local Co-op) — same Wi-Fi or one phone's hotspot (no internet needed)
1. Phone A: New Campaign → **Local Co-op** → open it → Hall → **Open session**. Note the player code, DM code and address.
2. Phone B: Join Game → enter name + player code → pick the host under *Nearby Hosts* (or type the address) → Connect →
   choose a character or create one. You land on the player sheet; the top tag shows *Online*.
3. On A change B's money (Purse → add) or HP: it appears on B within a second. On B use a potion/equip an item/roll dice/transfer coins:
   A sees it. B cannot award XP or change other people's items (the host rejects it).
4. Hide B's purse on A: it disappears from B. Toggle a quest to *All players* / assign it to B.
5. Turn B's Wi-Fi off for 20 s, change things on A, turn it back on: B reconnects by itself and catches up.
6. Wrong code → "Wrong pairing code"; five wrong tries lock out for a minute.

## 3. Separate DM and host (three phones)
1. Phone C (the sturdy/old phone): Home → the green Wi-Fi icon on the co-op campaign → **host console** (no DM UI; shows both codes + health).
2. Phone A (the DM): Join Game → **I am the Dungeon Master** → DM code → Connect. You get the full DM interface; the console lists you as DM.
3. Phone B joins as a player with the player code. Actions made on A run through C and reach B.

## 4. Recovery
1. While B and A are connected, force-stop the app on C. A and B show "host unreachable" and keep their data.
2. On the DM device: Hall → Tools → **Export migration package**. On a new host phone: Home → Import (validated; it becomes host).
   Players use Join Game again; nothing is duplicated.
3. On the host: Tools → **Restore snapshot** to roll back to the last session-close snapshot.

## 5. Things to look at
- Asset Library (Home → Assets): 16 categories, search, 20 colours, glow.
- Rotate the phone, kill/reopen the app: data persists, sessions resume.
