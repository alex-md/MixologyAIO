# Development

Use Java 11 or newer. To run a local development client:

```sh
./gradlew run
```

To build and run the tests:

```sh
./gradlew clean build
```

The plugin uses the standard RuneLite Plugin Hub build. Keep the listing metadata in `runelite-plugin.properties` consistent with the plugin descriptor.

Live-game timing and full-session validation are still outstanding. Automated tests cover the planning and timing state models, but do not establish live-game timing accuracy. Include any playthrough results when reporting or fixing an issue.

The README is published on the RuneLite website from the pinned commit. Keep it focused on using the plugin; put development and submission details here or in [the submission notes](docs/plugin-hub-submission.md).
