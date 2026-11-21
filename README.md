# Console Emulation Toggle — PyCharm Plugin

A simple PyCharm plugin that adds toggle buttons for the **Emulate terminal in output console** option of Python run configurations, so you don't have to open the Run/Debug Configurations dialog to change it.

## What It Does

Some scripts need a real terminal: progress bars, `curses` interfaces, coloured output, and libraries that check `isatty()`. Others are easier to read and search in the plain output console. Switching normally means going through Edit Configurations > Modify options. This plugin makes it one click.

- **Button ON** (highlighted): The run configuration emulates a terminal in the output console.
- **Button OFF**: The run configuration uses the plain output console.

## Requirements

- **PyCharm** 2024.3 through 2026.2 (build 243 – 262.*)
- Tested on PyCharm Professional 2024.3 and PyCharm 2026.2.

## Installation

1. Download the `.zip` from the [latest release](../../releases/latest).
2. In PyCharm, go to Settings > Plugins > gear icon > Install Plugin from Disk.
3. Select the downloaded `.zip` and restart PyCharm if prompted.

You can also build the `.zip` yourself (see [Building from Source](#building-from-source)).

## Usage

After installation, the toggle appears in two places:

1. **Main Toolbar** (right side) — changes the run configuration selected in the run widget. It's disabled unless a Python script configuration is selected, so it's also disabled when the widget is set to "Current File".
2. **Run and Debug tool window toolbar** — changes the configuration that tab was started from. It's hidden on tabs for other kinds of configuration.

The tooltip names the configuration and shows its current state.

### Things to Know

- **It's per configuration.** PyCharm stores this option on each run configuration, so each one keeps its own value, the same as if you'd changed it in the dialog.
- **It applies on the next run.** A process that's already running isn't affected. After using the tool window toggle, click Rerun to see the change.

## Building from Source

Building requires a JDK 21, either set as `JAVA_HOME` or on your `PATH`. The Gradle wrapper downloads the correct version of Gradle itself.

```bash
./gradlew buildPlugin
```

On Windows, use `gradlew.bat buildPlugin`. The built plugin `.zip` will be in `build/distributions/`.

## Feedback

Bug reports and suggestions are welcome in [Issues](../../issues).

## License

[MIT](LICENSE)
