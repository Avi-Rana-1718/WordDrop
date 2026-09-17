# Capture real in-app screenshots from a connected device for the Play listing.
# Usage: install the release APK, set the device to the theme you want, then run:
#   .\tools\capture_screenshots.ps1
# It opens each screen, waits for you to press Enter, and saves a PNG to docs/play/assets/.

$out = Join-Path $PSScriptRoot "..\docs\play\assets"
New-Item -ItemType Directory -Force $out | Out-Null

$shots = @(
    @{ name = "screenshot_4_today";    hint = "Today tab. Add the widget to the home screen first so the state is populated." },
    @{ name = "screenshot_5_detail";   hint = "Tap the word to open Detail (or run: adb shell am start -a android.intent.action.VIEW -d worddrop://word/ephemeral com.worddrop.app)" },
    @{ name = "screenshot_6_saved";    hint = "Saved tab with a few saved words." },
    @{ name = "screenshot_7_quiz";     hint = "Quiz tab mid-question." },
    @{ name = "screenshot_8_settings"; hint = "Settings screen." }
)

adb shell am start -n com.worddrop.app/.MainActivity | Out-Null
foreach ($s in $shots) {
    Write-Host ""
    Write-Host "[$($s.name)] $($s.hint)"
    Read-Host "Press Enter to capture" | Out-Null
    $path = Join-Path $out "$($s.name).png"
    adb exec-out screencap -p > $path
    Write-Host "  saved $path"
}
Write-Host ""
Write-Host "Done. Play accepts 1080x1920 (or the device's native 9:16) PNGs as-is; no frames needed."
