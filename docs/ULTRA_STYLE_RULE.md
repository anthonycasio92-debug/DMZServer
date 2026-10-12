# Standing rule: no raw `§` in player-facing code

## Rule

**Never** embed a bare Minecraft section-sign character (`§` / U+00A7) in LegacyMechanics
Java sources for player-facing text.

- Use [`UltraStyle`](../tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/gui/ultra/UltraStyle.java) constants and helpers.
- `UltraStyle.java` is the **only** allowed owner of the mark (via `\u00A7` in its static init).

```java
// BAD
return "§eClick again within 10 seconds.";

// GOOD
return UltraStyle.YELLOW + "Click again within 10 seconds.";
// or
return UltraStyle.confirm("Free");
```

## Why

Feature branches kept reintroducing `§` into `DifficultyChatMenu` / `DifficultyActions`
(chat Free labels, confirm prompts, Result.tell prefixes). That is whack-a-mole without a
build gate.

## Enforcement

| Check | Scope |
|-------|--------|
| `sim/audit_ultra_style_codes.py` | `gui/ultra/**` sources + jar bytes |
| `sim/audit_no_section_sign.py` | `DifficultyChatMenu`, `DifficultyActions`, `gui/ultra/**` |

Both run from `tools/dmz-adaptive-difficulty/build.sh`.

## When touching older GUI APIs

Files like `ProgressionGuiApi` / `DifficultyTeamGuiApi` may still contain legacy marks.
When you edit player-facing strings there, migrate those lines to `UltraStyle` in the same
change — do not copy `§` into ChatMenu/Actions/Ultra screens.
