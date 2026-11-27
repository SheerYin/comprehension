# Comprehension usage

Comprehension is a Fabric client mod that lets an agent drive Minecraft through a local HTTP API. Real mouse and keyboard input to the game window is ignored; the game only sees input sent through the API.

The server listens on `http://127.0.0.1:25592` (override with `-Dcomprehension.port=<port>`). Use `127.0.0.1`, not `localhost`: on Windows, `localhost` tries IPv6 first and adds about 200 ms to every request.

The dev client (`./gradlew :fabric:runClient`) starts with a 1600x900 window and the offline username `Agent`. Override them with Gradle properties:

```sh
./gradlew :fabric:runClient -PclientUsername=Tester -PclientWidth=1920 -PclientHeight=1080
```

The GUI scale is fixed to auto; override with `-Dcomprehension.guiScale=<n>`.

## Several clients at once

`scripts/client.sh <n> [username] [width] [height]` starts instance `n` directly with Java, without Gradle, so several can run together. Instance `n` uses the game directory `fabric/runs/<n>`, port `25592 + n`, and the username `Agent<n>` unless given. Run `./gradlew build` and `./gradlew :fabric:runClient` once first so the Loom launch files exist, and rerun `./gradlew build` after code changes.

```sh
scripts/client.sh 1   # Agent1 on http://127.0.0.1:25593
scripts/client.sh 2   # Agent2 on http://127.0.0.1:25594
```

## Workflow

Act, wait, then look:

1. Send an input (move, click, key, text). Every input call returns after the client has run at least one tick and rendered a frame, so the client's own reaction is done and the returned `screen` is current.
2. Sleep long enough for the server to react. Input calls never wait for the server.
3. Take a screenshot and read it. If nothing changed yet, wait and take another one.

## Coordinates

Mouse coordinates are pixels in the screenshot. `GET /mouse` returns the frame size as `width` and `height`.

## Endpoints

| Method and path | Body | Effect |
|---|---|---|
| `GET /screenshot` | | PNG of the next rendered frame, with the virtual cursor drawn in |
| `GET /mouse` | | Cursor position, frame size, whether the mouse is grabbed, current screen and overlay |
| `POST /mouse/move` | `{"x":425,"y":447}` | Move the cursor |
| `POST /mouse/drag` | `{"button":"left","path":[[584,531],[638,531],[692,531]]}` | Press at the first point, move through the rest, release |
| `POST /mouse/press` | `{"button":"left"}` | Press a button (`left`, `right`, `middle`) |
| `POST /mouse/release` | `{"button":"left"}` | Release a button |
| `POST /mouse/scroll` | `{"dy":-3}` | Scroll the wheel at the cursor; negative `dy` scrolls down, `dx` scrolls sideways. In game with no screen open it changes the hotbar slot |
| `POST /mouse/click` | `{"button":"left","count":2}` | Press and release; `count` 2 is a double click |
| `GET /key` | | Keys currently held, current screen and overlay |
| `POST /key/press` | `{"key":"w"}` | Press and hold a key |
| `POST /key/release` | `{"key":"w"}` | Release a key |
| `POST /key/tap` | `{"key":"escape"}` | Press and release |
| `POST /key/release-all` | | Release every held key |
| `POST /key/type` | `{"text":"hello"}` | Type text into the focused text field |
| `GET /effects` | | Virtual clipboard and recorded side effects |
| `POST /effects/clear` | | Clear recorded side effects |

`screen` and `overlay` are Java class names, or `null` when nothing is open.

Key names are Minecraft key names: `e`, `escape`, `enter`, `backspace`, `left.shift`, `left.control`, `f3`, and so on. Held modifier keys apply to mouse clicks too: press `left.shift`, click, release `left.shift` for a shift-click.

While the mouse is grabbed (in game with no screen open), moving the mouse turns the camera and no cursor is drawn.

## Screenshots

`GET /screenshot?x=540&y=440&w=520&h=260` crops to a region of the frame. Add `scale=2` to `scale=8` to enlarge it, which helps with small text and custom glyphs.

## Timing rules

- Input sent while `overlay` is not null (for example `LoadingOverlay` during a resource pack reload) is dropped by the game. Wait until `overlay` is `null`.
- Because each input call waits for a tick and a frame, hover is up to date when `POST /mouse/move` returns: a number key or `q` sent right after a move acts on the new slot.
- To spread a carried stack across slots, use `POST /mouse/drag` with one point per slot (`left` splits evenly, `right` places one per slot). It waits a tick and a frame per point and nudges inside the first slot after pressing, so every slot on the path is included.

## Side effects

Clicks that would copy to the clipboard, open a URL, or open a file do not touch the computer. Clipboard writes go to a virtual clipboard that the game also reads for pasting, and every such action is recorded in `GET /effects`. Opening a URL still shows the game's confirmation screen first.

## Sending request bodies

Pass request bodies to curl on stdin (`--data-binary @-` with a quoted heredoc), not with `-d`. Git Bash's curl reads command-line arguments in the system code page (GBK), so non-ASCII text in `-d` reaches the server garbled.

```sh
curl -s -X POST 127.0.0.1:25592/key/type --data-binary @- <<'EOF'
{"text":"你好世界"}
EOF
```

Save screenshots to a file and open the file:

```sh
curl -s -o shot.png 127.0.0.1:25592/screenshot
```
