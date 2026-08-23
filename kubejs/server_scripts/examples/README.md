# examples/

Files here ending in `.js` are still loaded by KubeJS as server scripts.

Keep reference/example code as `*.js.disabled` so it does not join the shared
Rhino scope (duplicate `var NetworkHandler` etc. will abort other scripts).
