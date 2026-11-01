## Fix: a ship's hull now shades a body from the sun
- Sunlight read sky light, which cannot see a ship, so a body inside a ship charged as if in the open. A ray now finds a solid hull overhead (`Ships.hullOverhead`), and the body gets no sun there; glass lets it through. Test: `aShipsHullShadesABodyFromTheSun`.
