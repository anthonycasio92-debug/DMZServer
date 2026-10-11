# Kai Path — shared Ultra UI bridge (drop-in)

**Purpose:** Let **any** client mod pull DMZUltra (`dmzultra`) themes/widgets through Kai Path,
without compiling against `com.dmzultra` and without each mod reinventing a soft-dep.

Not a patch to `dmzultra`. Not a second Forge mod id — lives inside **`kaipath`**.

## Layers

| Layer | Package | Who uses it |
|-------|---------|-------------|
| **Shared API** | `com.dmzlegacy.kaipath.ultra` | **Every** mod that wants Ultra look |
| Reflective impl | `…ultra.internal` | Kai Path only (do not call from outside) |
| Optional LM consumer | `…ultra.consumers.legacymechanics` | Server-only LegacyMechanics open/action channel |

```
any mod  ──►  UltraUiAccess / UltraScreen  ──►  dmzultra (if installed)
                 ▲
                 │ hosted by kaipath
LegacyMechanics (server) ──packets──► consumers.legacymechanics (optional)
```

## Public API (for any mod)

```java
import com.dmzlegacy.kaipath.ultra.UltraUiAccess;
import com.dmzlegacy.kaipath.ultra.UltraScreen;

// Probe / availability
UltraUiAccess.isAvailable();          // dmzultra widgets present?
Object theme = UltraUiAccess.forgeTheme();

// Widgets (vanilla Button fallback if Ultra missing)
addRenderableWidget((AbstractWidget) UltraUiAccess.button(x, y, w, h, "OK", this::onOk, theme));
UltraUiAccess.list(x, y, w, h, List.of("row A", "row B"), theme);

// Or extend the base screen
public class MyMenu extends UltraScreen {
    public MyMenu() { super(Component.literal("My Mod")); }
    @Override protected void init() {
        addThemedButton(20, 40, 120, 20, "Do thing", this::doThing);
    }
}
```

### Dependency for third-party mods

Soft-depend on Kai Path (and optionally dmzultra):

```toml
[[dependencies.yourmod]]
modId = "kaipath"
mandatory = false
versionRange = "[1.2,)"
ordering = "AFTER"
side = "CLIENT"

[[dependencies.yourmod]]
modId = "dmzultra"
mandatory = false
versionRange = "(,0),[0,)"
ordering = "AFTER"
side = "CLIENT"
```

If `kaipath` is missing, do not call `UltraUiAccess` (or guard with `ModList.get().isLoaded("kaipath")`).

## Copy into Kai Path

```
src/main/java/com/dmzlegacy/kaipath/ultra/
  UltraUiBridge.java          # bootstrap()
  UltraUiAccess.java          # PUBLIC facade for all mods
  UltraScreen.java            # PUBLIC base screen
  internal/UltraWidgetReflect.java
  consumers/legacymechanics/  # optional LM channel + hub (not for other mods)
```

## Kai Path one-line hook

```java
net.minecraftforge.fml.DistExecutor.safeRunWhenOn(
        net.minecraftforge.api.distmarker.Dist.CLIENT,
        () -> com.dmzlegacy.kaipath.ultra.UltraUiBridge::bootstrap);
```

`bootstrap()` probes dmzultra and starts optional first-party consumers (LM).  
Third-party mods only need the API classes at runtime — they do **not** register inside `bootstrap()`.

## kaipath mods.toml

Keep optional dmzultra (already present):

```toml
[[dependencies.kaipath]]
modId="dmzultra"
mandatory=false
versionRange="(,0),[0,)"
ordering="AFTER"
side="BOTH"
```

## Optional: LegacyMechanics consumer

Only if the server runs LM with `guiBackend=ultra`:

- Channel `legacymechanics:gui` / protocol `"1"`
- Hub actions: `character`, `difficulty`, `progression`, `prestige`, `spar`, `rival`
- Remove any old `LegacyMechanicsUltra` companion jar (channel conflict)
- Do **not** put `LegacyMechanics-*.jar` on clients

Other mods that want Ultra UI should **not** copy the LM consumer — use `UltraUiAccess` / `UltraScreen` and their own networking.
