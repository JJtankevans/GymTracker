# Fix Unresolved Reference 'util' in app/build.gradle.kts

The project sync is failing due to an unresolved reference to `util` in the `app/build.gradle.kts` file. This happens because the `java` package is being shadowed by the `java` extension property provided by the Gradle Java plugin (or Android plugin which interacts with it) in the Kotlin DSL.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///D:/programmingProjects/AndroidCompose/GymTracker/app/build.gradle.kts)
- Add `import java.util.Properties` at the top of the file.
- Update the code to use `Properties()` instead of `java.util.Properties()`.

## Verification Plan

### Manual Verification
- Run a Gradle sync to verify that the `Unresolved reference 'util'` error is resolved and the project syncs successfully.
