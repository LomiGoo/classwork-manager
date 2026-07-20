# Implementation Plan - Code Quality and Performance Optimization

This plan aims to resolve all compiler warnings, deprecations, and potential performance bottlenecks in the application.

## Proposed Changes

### 1. Data Layer Optimizations

#### [MODIFY] [UpdateManager.kt](file:///C:/Users/cheve/OneDrive/Dokumen/JAVA-college/CODING/Apps/ClassworkManager/app/src/main/java/com/lomigoo/classworkmanager/data/UpdateManager.kt)
- Use `@SerialName` to follow Kotlin naming conventions (camelCase) for JSON properties.
- Share a single `OkHttpClient` instance.
- Fix unused exception parameter `e`.
- Remove unnecessary safe calls and simplify response handling.

#### [MODIFY] [ClassworkDbHelper.kt](file:///C:/Users/cheve/OneDrive/Dokumen/JAVA-college/CODING/Apps/ClassworkManager/app/src/main/java/com/lomigoo/classworkmanager/data/ClassworkDbHelper.kt)
- Standardize code style and remove any minor lint warnings.

### 2. UI Layer Optimizations

#### [MODIFY] [ClassworkViewModel.kt](file:///C:/Users/cheve/OneDrive/Dokumen/JAVA-college/CODING/Apps/ClassworkManager/app/src/main/java/com/lomigoo/classworkmanager/ui/ClassworkViewModel.kt)
- Fix boolean literal warnings by adding parameter names (e.g., `value = false`).
- Add missing trailing commas for better git diffs and style consistency.

#### [MODIFY] [ClassworkApp.kt](file:///C:/Users/cheve/OneDrive/Dokumen/JAVA-college/CODING/Apps/ClassworkManager/app/src/main/java/com/lomigoo/classworkmanager/ui/ClassworkApp.kt)
- Replace deprecated `ClickableText` with modern `Text` + `LinkAnnotation` or `AnnotatedString` with inline links.
- Replace deprecated `Divider` with `HorizontalDivider`.
- Fix boolean literal warnings.
- Fix formatting and style warnings (trailing commas, lambda placement).

### 3. General Performance
- Verify that heavy list operations (sorting/filtering) remain wrapped in `remember` to prevent unnecessary re-computations during recomposition.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure all warnings are resolved (where possible) and no new errors are introduced.
- Run `lintDebug` to verify the overall project health.

### Manual Verification
- Open the app and verify all UI elements function correctly.
- Test the "App Info" links to ensure they are still clickable and open the browser.
- Verify sorting and filtering still work as expected.
- Verify the update checker still functions correctly.
