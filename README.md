# SocialKingdomGuide

SocialKingdomGuide is a small, configurable chat-announcement plugin for
[Paper](https://papermc.io/) Minecraft servers.

The plugin broadcasts guide messages at a configurable interval and lets
administrators add, list, remove, and reload announcements in-game. It was
created for the Norwegian Minecraft community Social Kingdom.

> This project was developed with substantial assistance from an AI coding
> model. It is published as a compact, readable example of AI-assisted plugin
> development. The code and resulting behavior remain the responsibility of
> the project maintainers.

## Features

- Rotating chat announcements
- Configurable broadcast interval
- Add and remove announcements without editing YAML manually
- Persistent announcements stored in `config.yml`
- Permission-protected administration commands
- Tab completion for commands and announcement IDs
- Configurable messages, prefixes, and legacy `&` color codes

## Requirements

- Paper 1.21.4 or a compatible server version
- Java 21

## Commands

The main command is `/socialkingdomguide`, with `/skg` as an alias.

| Command | Description |
| --- | --- |
| `/skg add announce <text>` | Add a new announcement |
| `/skg list announcement` | List configured announcements |
| `/skg remove announce <id>` | Remove an announcement |
| `/skg set <minutes>` | Change the broadcast interval |
| `/skg reload` | Reload `config.yml` |

All commands require the `socialkingdomguide.admin` permission, which defaults
to server operators.

## Configuration

The plugin creates `plugins/SocialKingdomGuide/config.yml` on first startup.

```yaml
interval-minutes: 15

messages:
  prefix: "&8[&6SocialKingdom&8] &r"
  announce-format: "&8[&6Info&8] &f{message}"

announcements:
  - id: 1
    message: "&eRemember to lock your chests."
```

Announcement IDs must be positive integers. Messages are broadcast in ascending
ID order and wrap back to the beginning after the final message.

## Building

Clone the repository and run:

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The compiled plugin is written to `build/libs/`.

## Installation

1. Build the plugin or download a release JAR.
2. Stop the Paper server.
3. Copy the JAR into the server's `plugins/` directory.
4. Start the server.
5. Edit the generated `config.yml` or manage announcements with `/skg`.

## Code overview

The project intentionally keeps the implementation compact:

- `SocialKingdomGuidePlugin.java` contains the plugin lifecycle, command
  handling, scheduling, persistence, and tab completion.
- `plugin.yml` declares the plugin entry point, command, and permission.
- `config.yml` provides the default announcements and translatable messages.
- `build.gradle.kts` defines the Java 21 and Paper API build.

## License

This project is available under the [MIT License](LICENSE).
