---
okf_version: "0.2"
---

# Revolver documentation

This directory is an [Open Knowledge Format](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) (OKF v0.2) bundle. Every page is plain markdown with YAML frontmatter, so people can read it on GitHub and agents can parse it without extra tooling.

# Guides

* [Getting started](getting-started.md) - Install Revolver, then build and connect your first ViewModel on Android and iOS.
* [Error handling](error-handling.md) - Catch exceptions from event handlers and map them to states and effects.
* [Testing](testing.md) - Unit test ViewModels in `commonTest` with Turbine and kotlinx-coroutines-test.
* [iOS integration](ios-integration.md) - Observe state and effects from Swift with `watch`, and manage the ViewModel lifecycle.

# Concepts

* [Core concepts](concepts.md) - Events, states, effects, the ViewModel and the emitter, plus how events and errors are processed at runtime.

# Reference

* [API reference](api-reference.md) - Every public type and function in `com.umain.revolver` and `com.umain.revolver.flow`.

# History

* [Update log](log.md) - Changes to this documentation bundle.
