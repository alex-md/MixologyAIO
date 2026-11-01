<img src="docs/assets/mixology-aio.png" width="112" height="112" align="right" alt="Mixology AIO potion flask icon">

# Mixology AIO

A Mastering Mixology helper built around **28-potion batches**. Make a full inventory upfront, process the potions in station groups, then spam-click the conveyor and start the next batch.

Choose what you're working toward and the plugin figures out the potion quantities based on your Herblore level, available paste, and resin balances. You don't need to enter counts for every recipe or think about each individual order.

Once you start making an inventory, the queue stays the same. Changes to your goal or resources normally apply to the next batch.

## How to use it

1. Open Mixology AIO in the RuneLite sidebar and choose your goal.
2. If you're working toward rewards, select the items and quantities you want.
3. Load paste into the hopper and follow the highlighted lever or mixing vessel until your inventory is full.
4. Process each highlighted station group.
5. Keep clicking the conveyor. The plugin plans the next refill around any potions left over.

The usual batch is 28 potions, or 27 if something like Digweed is taking up a slot. If you're short on paste, it plans a smaller batch with what you have.

## Goals

- **Maximum XP:** Focus on estimated XP per hour.
- **Specific rewards:** Work toward one or more rewards using your current resin balances.
- **Balanced:** Work toward rewards while also considering XP. Without a reward selected, it favors XP.
- **Resource efficiency:** Focus on useful progress for the paste you spend and reduce excess resin.

The sidebar shows your potion distribution, reward progress, next action, and estimated rates. You can still set your own recipe quantities through **Manual batch (legacy)** in the settings.

## Highlights and timing

Only the next lever or vessel is highlighted. Inventory markers show which station group each potion belongs to, and the potion queue lets you see what's coming next.

At the Alembic, a thin border fills around the station as the speedup approaches. It shifts from teal to amber, then turns green when it's time to click. The timer learns from the first observed cycle, so that first potion uses the green cue without a countdown.

The Agitator turns green at its speedup cue. At the Retort, keep clicking each tick. Timing sounds and an optional text banner are available under **Display**; the banner is off by default.

## Notes

Paste must be in the hopper for the planner to use it. Reward quantities mean what you still want to buy, so reduce them after a purchase. Specific rewards stops planning new batches once you have enough resin for the combined target.

You do all the clicking and moving. The plugin provides instructions and optional menu guards for the processing stations.

Rates are estimates, and latency can still affect timing cues. More on the planning model and its limits is in [the planner notes](docs/planner.md).

If something goes wrong, [open an issue](https://github.com/alex-md/MixologyAIO/issues) with your Herblore level, goal, and what happened. Include a screenshot if you can.

## Credits

Thanks to the original contributors. [Credits and license notices](THIRD_PARTY_NOTICES.md).
