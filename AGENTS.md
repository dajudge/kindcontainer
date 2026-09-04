# AGENTS.md

## Release hygiene

Before preparing a new release, **strongly prefer checking every pinned sidecar/default container image against its upstream current stable release and updating stale images in dedicated PRs**. Do not silently carry obsolete sidecar versions into a new release when a supported update is available.

Keep image versions pinned for reproducibility rather than switching defaults to floating `latest` tags. Treat updates that imply an API or behavior migration (for example, a new major version) as explicit migration work instead of routine hygiene, and let the full CI matrix validate compatibility before merging.

## Release workflow

Releases are cut from `master` using tags named `release/X.Y.Z` and GitHub releases named `vX.Y.Z`.

### 1. Freeze the release commit

Before doing anything release-specific, record the exact `master` commit SHA that is being released. Do not let later Dependabot or other merges silently become part of the release.

Verify CI on that exact SHA. The build matrix is generated from `k8s-versions.json`, so a green matrix is the release gate for the declared Kubernetes/container combinations.

### 2. Choose the version

Use semantic versioning:

- patch for compatible fixes only,
- minor for backwards-compatible user-visible additions such as support for new Kubernetes versions,
- major for breaking changes.

The Gradle build derives the artifact version from a clean Git tag named `release/X.Y.Z`. A build that is not on such a clean tag becomes a timestamped `-SNAPSHOT`, so the release tag must point at the intended frozen commit before publishing.

### 3. Prepare release notes

Use the previous GitHub release as the base and reconstruct all merged PRs/commits since its `release/X.Y.Z` tag. Reconcile the PR list with the raw Git compare so nothing is omitted.

Match the style of previous releases. Use only the sections that apply, typically:

- `## New features and improvements`
- `## Dependency updates`
- `## Bugfixes`
- `## Housekeeping`
- `## New Contributors`

Each item should follow the existing form:

```text
* <PR title or concise description> by @<author> in https://github.com/dajudge/kindcontainer/pull/<number>
```

Finish with:

```text
**Full Changelog**: https://github.com/dajudge/kindcontainer/compare/release/<previous>...release/<new>
```

Do not add or change PR labels merely to generate notes unless there is a concrete reason to do so; the release notes are intentionally curated.

### 4. Create the release tag

Create `release/X.Y.Z` at the frozen SHA, not at whatever `master` happens to point to later.

If the available GitHub integration cannot create Git tags directly, a temporary GitHub Actions helper with `permissions: contents: write` is an acceptable fallback. It should create `refs/tags/release/X.Y.Z` at the frozen SHA and then check out that tag with full history.

Immediately verify that:

```text
git rev-parse HEAD == <frozen SHA>
./gradlew properties
```

reports exactly:

```text
version: X.Y.Z
```

The release tag must point to the pre-helper frozen SHA so the released source does not contain temporary release machinery.

### 5. Publish to Maven Central before creating the final GitHub release

The repository currently publishes through the Vanniktech Maven Publish plugin. Run from the checked-out release tag:

```text
./gradlew publishToMavenCentral
```

using the existing Maven Central and signing secrets.

Important details learned from the v2.1.0 release:

- A GitHub release created by a workflow using the repository `GITHUB_TOKEN` does not recursively trigger another workflow from `release: created`. Do not rely on `.github/workflows/publish.yaml` firing when the release itself is created by such a helper workflow.
- The current Maven Central configuration uploads as `USER_MANAGED` and may print `Skipping deployment validation!`. In that case the Gradle task succeeding means only that the bundle upload succeeded, not that the artifact is publicly released.
- Record the Central deployment ID from the Gradle output.
- Ask the repository owner to open Central Portal, wait for the deployment to reach `VALIDATED`, and click `Publish`.

Therefore, when automation is creating the GitHub release, prefer this order:

1. freeze and verify the release SHA,
2. prepare notes,
3. create `release/X.Y.Z`,
4. check out the tag and verify Gradle resolves `X.Y.Z`,
5. run `publishToMavenCentral`,
6. if Central reports `USER_MANAGED`, have the owner validate/publish the deployment in Central Portal,
7. only then create/publish GitHub release `vX.Y.Z`.

The existing `.github/workflows/publish.yaml` still listens for manually created GitHub releases and can be used when the release is created through normal GitHub UI/API credentials that do trigger release workflows. Do not expect a tag push alone to publish artifacts.

### 6. Publish the GitHub release

The GitHub release must be named `vX.Y.Z`, use tag `release/X.Y.Z`, and contain the prepared curated notes.

If the available integration cannot create releases directly, a temporary Actions helper may run:

```text
gh release create release/X.Y.Z --title vX.Y.Z --notes-file <notes-file> --verify-tag
```

using `GITHUB_TOKEN` with `contents: write`.

If a GitHub release was accidentally created before a required Central manual-publish step, delete only the GitHub release and keep the release tag. Recreate the GitHub release after Central publication is confirmed.

### 7. Verify publication

Verify all of the following:

- `release/X.Y.Z` points to the original frozen SHA,
- Gradle resolved `version: X.Y.Z`, not a timestamped snapshot,
- the Maven Central upload completed successfully,
- any `USER_MANAGED` Central deployment was explicitly published,
- Maven Central eventually exposes the new version,
- the GitHub release exists with the intended notes.

Maven Central indexing can lag behind the successful publish action, so absence from search immediately after clicking Publish is not by itself evidence of failure.

### 8. Final cleanup

Remove all temporary release-helper workflows, trigger files, and temporary release-note files from `master`. Keep this `AGENTS.md` runbook updated with lessons learned from actual releases.

Final state should contain no temporary release machinery on `master`; only intentional documentation/code changes should remain.
