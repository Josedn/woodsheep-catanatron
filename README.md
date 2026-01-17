# Catanatron Core (Java)

A minimal Java port of the Catanatron core engine focused on the game loop,
map/board, move generation, reducer (rules), AI‑agnostic Player interface,
and a small test suite. It is intended for simulations, rules validation, and
as a foundation for search/learning in Java.

## Features

- Map/Board
  - BASE map topology using cube coordinates; node/edge adjacency caches.
  - Buildable settlement nodes with distance‑1 rule; buildable road edges
    connected to owned nodes or extending from owned roads.
  - Robber tile tracking; payouts ignore robber tile.
- Engine
  - Initial placement: settlement then road; initial road must touch last
    settlement; distance‑1 enforced.
  - Turn flow: ROLL → payouts or DISCARD/MOVE_ROBBER (on 7) → PLAY_TURN.
  - Build actions: settlements, roads, cities with affordability checks.
  - Deterministic rolls for testing by passing `int[]{d1, d2}` to ROLL.
  - Longest Road computation (DFS over edges) with +2 VP award/revoke.
  - Robber move with random steal from adjacent victim.
- Development Cards
  - Buy Dev Card (pays 1 sheep/wheat/ore; adds to hand; +1 VP for Victory Point).
  - Year of Plenty (1 or 2 cards) grants resources; per‑turn dev lock enforced.
  - Road Building: 2 free road placements (no resource cost).
  - Knight: sets MOVE_ROBBER and maintains Largest Army (+2 VP) on leadership.
  - Monopoly: collects all of one resource from opponents.
- Tests (JUnit 5)
  - Distance‑1 rule, road connectivity, payouts/robber, robber steal.
  - Dev card plays (YOP single/two cards, Road Building, Monopoly).
  - Largest Army award and leadership change.

## Not (yet) implemented / simplified

- Bank depletion/visibility and exact trade/maritime/domestic trade rules.
- Full dev timing nuances beyond “owned at start” and per‑turn lock.
- UI/serialization beyond core JSON in the Python project.

## Project Structure

```
 catanatron-java/
 ├─ pom.xml                      # Maven module
 ├─ src/main/java/com/catanatron/core/
 │   ├─ engine/                  # Game, State, Reducer, MoveGeneration, Player
 │   ├─ map/                     # Coordinates, tiles, BASE map builder
 │   ├─ board/                   # Board model (buildability, longest road)
 │   └─ model/                   # Action, ActionType, Edge, enums (Resource, DevCard, ...)
 └─ src/test/java/com/catanatron/core/engine/  # JUnit tests
```

## Build & Test

Requires Java 17+

```
# From repository root
mvn -f catanatron-java/pom.xml -DskipTests package
mvn -f catanatron-java/pom.xml test
```

## Quick Usage

```java
import com.catanatron.core.engine.*;
import com.catanatron.core.model.*;
import java.util.List;

// Create a simple 4‑player random game and play a few ticks
var players = List.of(
  new RandomPlayer(PlayerColor.RED),
  new RandomPlayer(PlayerColor.BLUE),
  new RandomPlayer(PlayerColor.ORANGE),
  new RandomPlayer(PlayerColor.WHITE)
);
var game = new Game(players);
for (int i = 0; i < 10; i++) {
  game.playTick();
}
System.out.println("Turns=" + game.state.numTurns +
                   " prompt=" + game.state.currentPrompt +
                   " actions=" + game.playableActions);
```

### Deterministic rolls

To force a roll outcome for testing:

```java
game.execute(new Action<>(PlayerColor.RED, ActionType.ROLL, new int[]{3,5})); // sum=8
```

### Playing dev cards

```java
// Seed Year of Plenty in hand and owned at start (for demo/testing)
var s = game.state;
s.playerState.put("P0_YEAR_OF_PLENTY_IN_HAND", 1);
s.playerState.put("P0_YEAR_OF_PLENTY_OWNED_AT_START", 1);
s.currentPrompt = ActionPrompt.PLAY_TURN;
s.playerState.put("P0_HAS_ROLLED", 1);

// Play YOP for two resources
game.execute(new Action<>(PlayerColor.RED, ActionType.PLAY_YEAR_OF_PLENTY,
                          new String[]{"WOOD","BRICK"}));
```

## Notes

- This module mirrors the Python core’s architecture (actions → reducer → state
  transitions) but favors simple, explicit data structures for Java.
- Longest Road uses a DFS over player‑owned road edges with edge‑usage tracking
  and stops expansion at enemy‑occupied nodes.
- Largest Army awards are recalculated on each Knight play and switch leadership
  when appropriate.

## License

This module is part of Catanatron and follows the repository’s license.
