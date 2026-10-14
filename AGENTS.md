# Agent guide

Revolver is a Kotlin Multiplatform state-management library (Android and iOS). The library is in `revolver/`, and a sample app is in `examples/basic/android/`.

## Knowledge

`docs/` is an [Open Knowledge Format](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) v0.2 bundle. Start at `docs/index.md`. [Core concepts](docs/concepts.md) lists the runtime rules (event ordering, error matching, dropped effects) that matter most when changing code.

When you change public behaviour, update the matching page in `docs/`: refresh its `generated` frontmatter and add an entry to `docs/log.md`. Every page except `index.md` and `log.md` needs a `type` in its frontmatter.

## Commands

- Run the tests: `./scripts/run_test_suite.sh`. It runs `commonTest` on the iOS simulator, so it needs macOS; there is no Android host-test target.
- Publish to local Maven: `./scripts/build_deploy_to_local_maven.sh`
