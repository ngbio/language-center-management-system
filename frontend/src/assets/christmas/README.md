# Christmas template assets

These files were copied from the user-provided `Christmas.2006/` template:

- `christmas-festive-garden-day-v1-2x.jpg` and `christmas-festive-garden-night-v1-2x.jpg`: window scenery, selected by the existing public light/dark theme.
- `window-frame-desktop.svg`: decorative window frame, with the outer wall fill removed to blend into each page.
- `christmas-red-snowflake-texture-v1.jpg`: account-page background.
- `jingle-bells-orchestra.mp3`: optional music, loaded only when the visitor starts playback.

`ChristmasScene.jsx` implements the presentation in React. No template scripts, authentication code, network endpoints, or macOS metadata are used.

The existing `VITE_UI_EVENT=christmas` setting enables the seasonal UI (the current default). Use `VITE_UI_EVENT=none` to restore the regular presentation. Snow respects the existing preference and reduced-motion setting. Audio stops when the scene unmounts.

The downloaded source template and `__MACOSX/` are excluded from Git; the runtime assets are tracked here so builds do not depend on those directories.
