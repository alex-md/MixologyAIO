# Full-inventory planning

## What stays fixed

The active `BatchPlan` and `CyclePlan` remain the original workflow's source of
truth. The optimizer runs at entry, an empty-inventory boundary, an explicit new
refill, or the original rolling-refill threshold. Changing goals, reward quantities,
station preferences, resin balances, or a newly unlocked recipe does not replace
the active queue. Settings are visibly queued for the next inventory. A recipe
that becomes impossible or an unexpectedly oversized inventory triggers recovery.

`BatchStateResolver`, `BatchMode`, mixing prediction, queue look-ahead, overlays,
and `StationMenuGuard` retain their responsibilities. The planner never subscribes
to individual order changes or tells the player to wait for or select an order.

## Feasibility and station layout

`FullInventoryOptimizer.Input` copies the effective Herblore level, total usable
potion capacity, hopper quantities, reward deficits, objective, station preference,
and physical inventory. Existing potions impose lower bounds; their already-spent
paste is not charged again, and boosted recipes already held may remain after a
boost expires. New potions must be unlocked and affordable.

Seeds include recipe-weight coverage, the original quantities, deficit pressure,
XP, and recipe-heavy alternatives. A memoized feasibility fill also tries to avoid
stranding small paste fragments. It stops after 20,000 failed states. The largest
feasible inventory found is prioritized before objective scores. With plentiful
paste the plan always fills the requested capacity. A bounded feasibility search
cannot establish global optimality for every resource-constrained case.

`BatchPlan.forInventory` forms contiguous groups with sizes differing by at most
one: 10/9/9 at 28 and 9/9/9 at 27. It reserves carried potions in their physical
projected ranks and spreads the remaining copies across stations, preserving
coverage while ensuring each group's target can actually be filled. The original
manual `BatchPlan.create` and its round-robin-per-recipe behavior remain available.
No item is moved programmatically.

## Conveyor simulation

Each candidate receives the same 64 seeded streams of up to 28 conveyor clicks.
New orders are drawn independently using eligible recipe weights and an assumed
uniform choice among the three processing modifiers. Duplicate orders consume
distinct copies. Each click:

1. Delivers up to three matching potions from the modeled inventory.
2. Applies a 1.0/1.2/1.4 resin multiplier for one/two/three matched orders.
3. If none match, delivers the first remaining potion for one-third delivery XP
   and 10 resin of its highest-tier ingredient.
4. Refreshes all three orders.

Thus bonuses can benefit this workflow automatically, without changing recipes
or interrupting conveyor clicks. The planner also accounts for unmatched fallback
losses. An all-Mixalot inventory performs poorly under this model despite its high
raw XP per potion: many random orders cannot use it, leaving reduced-reward deposits.

Recipe XP, levels, order weights, paste amounts and resin data were checked against
[the game-mechanics reference](https://oldschool.runescape.wiki/w/Mastering_Mixology).
The model is not fitted to measured player sessions. Future order independence
and uniformly distributed station modifiers are assumptions. The first visible
orders are not used to bias a batch that will undergo many subsequent refreshes.

## Objective and search

Up to 384 candidate distributions are evaluated, including seeds and repeated
best-improving single-potion exchanges. Constraints and carried lower bounds are
checked before simulation. Common random streams and deterministic tie-breaking
make identical inputs reproducible. The worker checks cancellation between search
iterations and in feasibility search.

- **Maximum XP:** expected delivery XP divided by modeled whole-inventory time.
- **Specific rewards:** projected progress on the slowest required resin,
  `min(min(expectedYield, deficit) * maxDeficit / deficit)`, divided by time;
  adds 5% of pressure-weighted useful total resin as a tie-breaking preference.
- **Balanced:** an equal mixture of this reward-rate score and scaled XP rate
  (`XP / 30 / seconds`). Without deficits it uses the XP score.
- **Resource efficiency:** bottleneck progress plus 10% useful resin minus 5%
  excess resin, divided by new paste plus a small time term. Without deficits it
  uses scaled XP per paste. Full-batch feasibility still takes precedence.

Each new potion costs 30 paste. Efficiency therefore mostly changes color mix,
useful rewards per paste, and excess, not the total cost of a fresh full inventory.
These weights are explicit heuristics; the bounded search cannot prove that a
particular distribution is mathematically best or universally outperforms others.

Time is `4.8 + N * (2.4 + configuredStationSeconds) + meanClicks * 1.2` seconds.
The entire inventory is charged for preparation and processing to avoid overstating
steady-state hourly rates when it includes carried finished potions. These are
estimates, not calibrated timing guarantees. Group balance and the selected station
order constrain station efficiency; pathfinding, per-level processing speed,
station-specific speedups, Digweed effects and quick-action XP are not modeled.

## Threading and failure handling

The client thread captures input and adopts completed plans only at refill
boundaries. One daemon executor performs search. A changed boundary snapshot
cancels obsolete work; shutdown/leaving the lab cancels pending work and clears
state. Repeated unchanged boundary snapshots reuse their last result. Active
inventory guidance continues using the original resolver and mixing predictions.
A failed plan presents a manual-mode recovery message. No network or account data
is needed by the running plugin.

## Validation and model comparison

`FullBatchComparisonTest` uses 2,048 independent holdout streams, different from
search streams, with level 81, 28 slots, abundant paste, an empty inventory,
18 seconds per station, and a reagent-pouch target (13,800/11,200/15,100).
It evaluates the final search distribution, the original 28-potion quantities,
and 28 Mixalots using the same full-batch station assignment and delivery model.
Results are in [full-batch-comparison.csv](full-batch-comparison.csv).

The recorded XP choice models roughly 39,903 XP/hour against 38,518 for the original
counts (+3.6%). The balanced reward plan's slowest resin models about 7.82 hours
to the pouch versus 9.45 hours for the original counts (about 17% less). Reward and
balanced objectives sacrifice some XP relative to the XP goal. These are surrogate
projections, not a simulation of purchasing the reward over a full session: real
batches replan as deficits change. No confidence intervals or live-game superiority
claims are made.

The earlier live-order approach is not part of the delivered plugin. Both upstream
repositories were inspected, but the secondary reference's per-order adaptive
policy does not preserve the requested batch workflow and is not a suitable
like-for-like execution baseline here.

Still required: play through complete and partial inventories, deliberate mistakes,
Digweed pickups/moves, boost expiry, repeated conveyor deposits, and login/hop
transitions in a live client. Plugin Hub compatibility has a passing local standard
build; listing approval and distribution are separate external steps.
