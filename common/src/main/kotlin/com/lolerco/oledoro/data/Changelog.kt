package com.lolerco.oledoro.data

data class ChangelogEntry(
    val version: String,
    val date: String,
    val highlights: List<String>
)

object ChangelogProvider {
    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            version = "1.6.0",
            date = "2026-10-24",
            highlights = listOf(
                "Study stats daily tracking & disk persistence across sessions",
                "Interactive ASCII '[ your stats ]' box around total time counters",
                "Visual bar chart dialog showing focus and break time across days",
                "Interactive changelog section in settings dialog"
            )
        ),
        ChangelogEntry(
            version = "1.5.0",
            date = "2026-10-22",
            highlights = listOf(
                "Fast-forward button in controls row to skip phase and auto-start next interval",
                "Clickable numeric input fields replacing on-screen numpads with native keyboard",
                "Input range validation and non-digit filtering for duration settings",
                "Unified accent color styling for timer settings inputs"
            )
        ),
        ChangelogEntry(
            version = "1.4.0",
            date = "2026-10-19",
            highlights = listOf(
                "Numpad input dialog for exact timer duration entry",
                "Modular project restructuring with desktopApp module",
                "Headless environment detection & portable JVM execution"
            )
        ),
        ChangelogEntry(
            version = "1.3.0",
            date = "2026-10-10",
            highlights = listOf(
                "Refactored package structure to com.lolerco.oledoro",
                "Transparent outline-only icon styled with Gruvbox colors",
                "Extended notification action handlers"
            )
        ),
        ChangelogEntry(
            version = "1.2.0",
            date = "2026-10-01",
            highlights = listOf(
                "Desktop support for Linux, Windows, and macOS",
                "Total focus and break time accumulation counters",
                "Numpad input for precise timer duration entry",
                "Native cross-platform system notifications",
                "Negative and overtime timer custom Gruvbox color"
            )
        ),
        ChangelogEntry(
            version = "1.1.0",
            date = "2026-09-15",
            highlights = listOf(
                "Independent Gruvbox color pickers for timers",
                "Auto lightbulb: dim during focus, bright when finished",
                "Tactile haptic feedback for ticks and actions",
                "Custom dim level percentage adjustment"
            )
        ),
        ChangelogEntry(
            version = "1.0.0",
            date = "2026-08-20",
            highlights = listOf(
                "Minimalist, battery-saving pomodoro for OLED displays",
                "Deep true black background with Gruvbox color accents",
                "Configurable focus, short break, and long break intervals",
                "Clean monospace typography using JetBrains Mono"
            )
        )
    )

    val latestVersion: String get() = entries.first().version
}
