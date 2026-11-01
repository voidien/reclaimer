p='docs/CC_PERIPHERALS.md'; s=open(p).read()
a='| `getMode()` | `"text"`, `"readout"`, `"model"`, `"body"` or `"globe"` |\n'
assert a in s
s=s.replace(a,'| `setShip()` | shows a miniature of the ship the projector stands on, or else the nearest within 64 blocks |\n| `getMode()` | `"text"`, `"readout"`, `"model"`, `"body"`, `"globe"` or `"ship"` |\n')
open(p,'w').write(s)
p='docs/ADVANCED_COMPUTER_TECHNIC_PATCH.md'; s=open(p).read()
a='| `holo.get(addr)` |'
s=s.replace(a,'| `holo.ship(addr)` | shows a miniature of a ship |\n'+a,1)
open(p,'w').write(s)
p='docs/design/plans.md'; s=open(p).read()
k="- **Drawing:** `client/render/holo/HoloProjectorRenderer`"
s=s.replace(k,"- **Ship miniature:** mode SHIP shows the ship the projector stands on, or else the nearest within 64 blocks, turning slowly. All on the client (`client/render/holo/HoloShips`): the ship's blocks are copied on the render thread at most every 2 seconds (4096 blocks at most), and only a copy that differs is meshed, in the background, into a `HoloMesh` (positions and texture coordinates, 60,000 vertices at most) that each frame replays with one tint. `HoloShipRegion` is the copy as a block model reads it, so the mesher never touches the live level. Block models only: what a block entity draws, such as Create's shafts, is not in the miniature. Mesh positions start from one of the ship's blocks, because a ship's plot lies millions of blocks out, past what a float holds exactly.\n"+k)
open(p,'w').write(s)
open('docs/progress/holo-displays-7.md','w').write("""## Holo-displays, part 7: the ship miniature: done
`/void_reclaimer holo <pos> ship`, CC `setShip`, CogScript `holo.ship`. `HoloMeshTest`, `aShipMiniatureIsSavedAndTheLastWriterWins`.

## Decisions pending (for the owner)
- **Ship miniature:** the projector shows its own ship or the nearest within 64 blocks, with no way to pick another; a ship over 4096 blocks shows only the blocks copied first, not a scaled-down whole; Create's moving parts are left out.
""")
