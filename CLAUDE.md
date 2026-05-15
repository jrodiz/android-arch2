# CLAUDE.md

Project-level instructions for Claude Code working in this repo.

## Plans

- **Save every implementation plan to `plans/` in this project root**, using kebab-case file names that describe the feature (e.g. `plans/login-feature.md`, `plans/onboarding.md`, `plans/payments-checkout.md`).
- Do **not** leave plans in `~/.claude/plans/` for this project — they're hard to find later and don't get versioned with the code.
- When iterating on the same feature, **update the existing plan in place** rather than creating a new file. Append a dated "Revision N" section if the change is substantial.
- The canonical spec for the project is `ANDROID_APP_SCAFFOLD_PROMPT.md` at the repo root — plans should reference it, not duplicate it.

## Toolchain (actual, not aspirational)

- AGP **8.10.1** (AGP 9 alpha breaks `CommonExtension`).
- Kotlin **2.0.21** + KSP **2.0.21-1.0.27** + Hilt **2.52** — bump as a triple.
- compileSdk / targetSdk **36**, minSdk **26**.
- JVM target **17** (JDK 21 once installed locally — flip both `JavaVersion.VERSION_17` and `JvmTarget.JVM_17` in `build-logic/`).
- `androidx.navigation3:*` **1.0.0-alpha02**; the typealias receiver is `EntryProviderBuilder<Any>`.

## Architecture invariants (enforced by the scaffold spec)

- `:feature:<name>:nav` and `:feature:<name>:domain` are pure Kotlin/JVM (apply only `arch.jvm.library`). They must not transitively pull in `androidx.*`, Compose, Room, Retrofit, or Hilt.
- A `:presentation` module may depend on **another feature's `:nav` only** — never another feature's `:presentation`, `:domain`, or `:data`. Cross-feature navigation goes through the `:nav` contract.
- Core modules consumed by a JVM `:domain`/`:nav` must themselves be JVM, or be split into a JVM `:domain` and an Android `:data` (as `:core:session` is).
- Domain models cross module boundaries; DTOs and Room entities stay inside `:data`.
- No `runBlocking` in production code except the single intentional one in `StartDestinationModule.provideStartDestination` (commented at the site).

## Useful commands

```bash
JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew assembleDebug
JAVA_HOME=...                                                                       ./gradlew test
./gradlew :feature:<name>:nav:dependencies --configuration runtimeClasspath          # verify JVM isolation
./gradlew :feature:<name>:domain:dependencies --configuration runtimeClasspath       # verify JVM isolation
```
