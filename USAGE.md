# Comprehension usage

Comprehension is a Fabric client mod that lets an agent drive Minecraft through a local HTTP API. Real mouse and keyboard input to the game window is ignored; the game only sees input sent through the API.

The server listens on `http://127.0.0.1:25592` (override with `-Dcomprehension.port=<port>`).

The dev client (`./gradlew :fabric:runClient`) starts with a 1600x900 window and the username `Agent`. The GUI scale is fixed to auto; override with `-Dcomprehension.guiScale=<n>`.

## Workflow

Act, wait, then look:

1. Send an input (move, click, key, text).
2. Sleep long enough for the client and server to react. Input returns immediately and does not wait for any result.
3. Take a screenshot and read it. If nothing changed yet, wait and take another one.

## Coordinates

Mouse coordinates are pixels in the screenshot. `GET /mouse` returns the frame size as `width` and `height`.

## Endpoints

| Method and path | Body | Effect |
|---|---|---|
| `GET /screenshot` | | PNG of the next rendered frame, with the virtual cursor drawn in |
| `GET /mouse` | | Cursor position, frame size, whether the mouse is grabbed, current screen and overlay |
| `POST /mouse/move` | `{"x":425,"y":447}` | Move the cursor |
| `POST /mouse/press` | `{"button":"left"}` | Press a button (`left`, `right`, `middle`) |
| `POST /mouse/release` | `{"button":"left"}` | Release a button |
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
- A key that acts on the hovered slot (number keys, `q`) uses the hover from the last rendered frame. Sleep about 100 ms between moving onto a slot and pressing the key.
- Dragging items across slots: press on the first slot, move a few pixels inside that same slot, then move across the other slots with a short sleep between moves, then release. The game only adds a slot to the drag when a move happens over it, so jumping straight off the first slot skips it.
- Keys pressed in game (no screen open) take effect on the next tick; the reply does not show the result yet.

## Side effects

Clicks that would copy to the clipboard, open a URL, or open a file do not touch the computer. Clipboard writes go to a virtual clipboard that the game also reads for pasting, and every such action is recorded in `GET /effects`. Opening a URL still shows the game's confirmation screen first.

## Sending request bodies

Pass request bodies to curl on stdin (`--data-binary @-` with a quoted heredoc), not with `-d`. Git Bash's curl reads command-line arguments in the system code page (GBK), so non-ASCII text in `-d` reaches the server garbled.

```sh
curl -s -X POST localhost:25592/key/type --data-binary @- <<'EOF'
{"text":"你好世界"}
EOF
```

Save screenshots to a file and open the file:

```sh
curl -s -o shot.png localhost:25592/screenshot
```
