# Comprehension

A Fabric client mod for Minecraft 26.3 that lets an agent drive the game through a local HTTP API, so Paper plugins can be tested without anyone at the keyboard.

The agent works like a player looking at the screen: it moves a virtual mouse, clicks, presses keys and types, then takes a screenshot to see what happened. All input goes through the game's own mouse and keyboard handlers, so every vanilla or plugin screen (menus, inventories, chests, dialogs, chat, resource pack prompts) behaves as it would for a real player.

## Features

- Virtual mouse (move, click, double click, drag, scroll) and keyboard (keys, held modifiers, text input)
- Screenshots of the next rendered frame, with the cursor drawn in, optional crop and zoom
- Silent mode: real mouse and keyboard input is ignored, the game never pauses or throttles when unfocused, and the real cursor is never grabbed or moved
- Accessibility onboarding, tutorial, multiplayer warning and sound are switched off without touching `options.txt`
- Clipboard, URL and file actions are recorded instead of reaching the computer
- Every input call returns after the client has run a tick and rendered a frame
- Several clients can run at once, each with its own game directory, port and username

## Requirements

- Java 25
- Minecraft 26.3, Fabric Loader 0.19.5, Fabric API, Fabric Language Kotlin

## Build and run

```sh
./gradlew build
./gradlew :fabric:runClient
```

The API listens on `http://127.0.0.1:25592`. The mod jar is in `fabric/build/libs/`.

## Usage

See [USAGE-Windows.md](USAGE-Windows.md) for the endpoints, timing rules, multi-client script and Windows-specific notes.
