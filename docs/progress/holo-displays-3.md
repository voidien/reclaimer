## Holo-displays, part 3: the Create Display Link: done
A Display Link can write to a Holo Projector. `HoloGameTests`: `aCreateDisplayLinkDeliversText` (a Nixie Tube through a real link) and `aDirectAcceptTextKeepsOtherLines`.

## Decisions pending (for the owner)
- **Display Link:** a link only writes its own lines and leaves the others, so a projector can show two links at once.
