# amade-mines-game

**amade-mines-game** is the mine-sweeping game of the [amadeOS](https://github.com/datazuul/amadeos) desktop: find all mines on the field without stepping on one. Built on [amade-game-engine](https://github.com/datazuul/amade-game-engine); runs on other operating systems as well.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

## Playing

- **Left click** opens a cell. A number tells how many mines touch it; an empty cell opens its neighbours.
- **Right click** puts a flag on a cell, a second one a question mark (*Game → Marks (?)* switches them off), a third clears it.
- **Middle click**, or both buttons together, on a number opens all its unflagged neighbours once as many flags surround it as the number says. A wrong flag loses the game.
- The **first click is always safe** and opens an area: the mines are laid afterwards, never on that cell or its neighbours.
- The counter on the left shows the mines minus the flags, the clock on the right the seconds. The face starts a new game (also F2).
- Three sizes: *Beginner* (9 × 9, 10 mines), *Intermediate* (16 × 16, 40), *Expert* (30 × 16, 99). The choice is remembered.
- **Best times** per size, ten each (*Game → High Scores...*).

The field uses the colours of the amadeOS desktop theme and changes with the Style Manager. Mines, flags, faces and the seven-segment counters are drawn, not taken from fonts, so they look the same everywhere. Cells grow with the font size.

Texts in English, Deutsch, Français, Español and Italiano.

## Files

| What | Where |
|---|---|
| Settings (size, question marks) | `~/.config/amadeos/mines/mines.properties` |
| Name for high scores | `~/.config/amadeos/mines/player.properties` |
| Best times | `~/.local/share/amadeos/games/mines/<size>.scores` |

## Building

```bash
./gradlew test          # the window tests need a display (xvfb-run on servers)
./gradlew run
./gradlew deb           # build/distributions/amade-mines-game_<version>_all.deb
./gradlew installToAmadeOs   # copies the package to ../amadeos/config/packages.chroot/
```

With `../amade-game-engine` (and `../amade-commons`) checked out next to this repository, Gradle builds those checkouts instead of the JitPack versions.

## Structure

- `domain`: the rules without user interface. `Board` (field, safe first click, flood fill without recursion, chords, marks), `Difficulty`, `GameClock`.
- `repository`: `MinesSettings`.
- `ui.main`: Model-View-Presenter like the amade applications. `MinesPresenter` runs the game without Swing (moves, clock, face, best times), `MinesView` is what it needs from the window, `MinesWindow` implements it (menu, counters, face button). Tests drive the presenter through a `FakeView`, without a display.
- `ui.view`: the drawn components: `BoardView` (field and mouse gestures), `Counter`, `FaceIcon`, `Face`.

Comments of the form `// --8<-- [start:name]` mark the excerpts the [Game Developer's Handbook](https://github.com/datazuul/amade-game-engine/tree/main/docs/handbook) quotes.

Ported from EuroMines in [EuroWorks](https://github.com/datazuul/euroworks).

## Licence

[EUPL-1.2](LICENSE)
