# Plugin Hub submission

## Listing

- Name: **Mixology AIO**
- Author: **Alex Johnson, qoooob**
- Description: **Plans your 28-potion batches for Mastering Mixology, with reward tracking and station timing cues.**
- Repository: https://github.com/alex-md/MixologyAIO
- Entry point: `com.mixologybatch.MixologyBatchPlugin`
- Build: `standard`
- Icon: repository-root `icon.png`, transparent 48 × 48 PNG

The icon location and size follow the [official Plugin Hub instructions](https://github.com/runelite/plugin-hub/blob/master/README.md#creating-new-plugins).
The README includes the same artwork at a larger size; the sidebar loads its
bundled copy through RuneLite's resource API. Runtime resources and BSD notices
live under `src/main/resources`, so they survive the standard build replacing
this repository's Gradle file.

## Before submitting

The local build and automated tests have passed. Live playthroughs are still needed
for the full batching/refill loop, unexpected actions, boost expiry, and Alembic
calibration and click-window timing. Record those results before describing the
release as live-validated.

Commit and push the reviewed files to the public repository. Then use the full
40-character hash of that pushed commit in the Plugin Hub manifest; the current
starter commit does not include this implementation.

Create `plugins/mixology-aio` in a fork of `runelite/plugin-hub`:

```properties
repository=https://github.com/alex-md/MixologyAIO.git
commit=REPLACE_WITH_THE_FULL_PUSHED_COMMIT_HASH
```

Get the hash after committing with `git rev-parse HEAD`. The placeholder above
is documentation, not a submission-ready manifest.

## Suggested PR

Title: **Add Mixology AIO**

Mixology AIO extends Brains Off Mixology Helper with automatic full-inventory
planning, multi-reward tracking, next-object guidance, and subtle station timing
cues. The workflow stays the same: prepare 27–28 potions, process station groups,
spam-click the conveyor, and refill.

Plans are held stable during a batch. Search runs on one background worker and
compares whole inventories using bounded, seeded conveyor simulations. Manual
recipe quantities remain available. The plugin provides guidance and swaps existing
station menu options; it does not automate clicks, movement, or gameplay actions.

Brains Off and Mastering Mixology Helper attribution and BSD licenses are preserved.
The plugin uses the standard build and adds no runtime dependencies.

Report the validation actually completed in the PR. Live-game validation is still
outstanding; the current automated checks do not establish live-game timing accuracy.
After opening the PR, check Plugin Hub CI and respond to review feedback. Approval
and distribution remain separate steps after submission.
