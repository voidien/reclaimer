## Fix: the 3D view cut off parts of the body
- The panel behind the body wrote depth at z 0, so anything over 50 GUI units behind the focus failed the depth test: a flat slice that grew with zoom. `AssemblyScreen` now clears depth before it draws the body, and draws the Explode button again after it. Seen in a client on the full Ship, before and after.
