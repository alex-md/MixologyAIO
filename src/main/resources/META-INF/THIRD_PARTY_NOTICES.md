# Third-party notices

Mixology AIO extends **Brains Off Mixology Helper** by qoooob / giuqoob.
The original Java workflow, overlays, tests, Gradle wrapper, and build foundation
were imported from https://github.com/giuqoob/osrs-plugin-brains-off-mixology
at `e33d702829708237030b783e2df0ff2bd69effde`.

Copyright (c) 2026, Brains Off Mixology Helper contributors. All rights reserved.
The complete BSD 2-Clause terms are retained in `LICENSE.brains-off`.

## Mastering Mixology Extended / Mastering Mixology Helper

Reward names, item IDs, and resin-cost data in `RewardItem.java` were adapted from
https://github.com/PDBoegel/mastering-mixology at
`3d84eae4fbec1727fc845e68b967842836189090`, itself a fork of
https://github.com/hex-agon/mastering-mixology.
Its multi-reward tracking and Monte-Carlo-informed policy were studied as design
references. Station timing detection also follows the upstream helper’s visual-effect,
progress-varbit, and delayed Alembic sound approach, with an independent timing
state model and dedicated cue overlay. Its order-by-order policy is not included in the final plugin;
Mixology AIO evaluates whole inventories and repeated conveyor clicks instead.

Copyright (c) 2024, hexagon@fking.work. All rights reserved.
The complete BSD 2-Clause terms are retained in `LICENSE.mastering-mixology`.

## References acknowledged by the primary foundation

- Mastering Mixology Helper: Copyright (c) 2024, hexagon@fking.work.
  https://github.com/hex-agon/mastering-mixology
- Noot's Mixology: Copyright (c) 2026, Liam Rourke (Nooters).
  https://github.com/lrdigital/noots-mixology
- Simplifying Mixology: Copyright 2024 Thource.
  https://github.com/Thource/simplifying-mixology

These references use BSD 2-Clause terms; the notice and disclaimer template is
reproduced in the bundled license files. They are not runtime dependencies.

Game-mechanics reference: https://oldschool.runescape.wiki/w/Mastering_Mixology
(retrieved November 1, 2026). Recipe unlocks, order weights, paste use, and conveyor
submission behavior were checked against this page. No wiki text is bundled.
