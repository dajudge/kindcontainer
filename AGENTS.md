# AGENTS.md

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

If the available GitHub integration cannot create Git tags/releases directly, a temporary GitHub Actions helper is an acceptable fallback:

1. add a narrowly scoped temporary workflow with `permissions: contents: write`,
2. make it create `refs/tags/release/X.Y.Z` at the frozen SHA,
3. make it create the GitHub release from that tag using the prepared release notes,
4. remove the helper workflow afterward.

The release tag must point to the pre-helper frozen SHA so the released source does not contain the temporary release machinery.

### 5. Publish the GitHub release

The GitHub release must be named `vX.Y.Z` and use tag `release/X.Y.Z`.

Creating the GitHub release is also the Maven Central publication trigger. `.github/workflows/publish.yaml` listens for:

```yaml
on:
  release:
    types: [created]
```

and runs:

```text
./gradlew publishToMavenCentral
```

using the repository's Maven Central and signing secrets.

Therefore the order is:

1. freeze and verify the release SHA,
2. prepare notes,
3. create `release/X.Y.Z`,
4. create/publish GitHub release `vX.Y.Z`,
5. let the existing Maven Central workflow run.

Do not expect a tag push alone to publish artifacts.

### 6. Verify publication

After creating the GitHub release:

- verify the Maven Central GitHub Actions workflow starts,
- inspect failed jobs/steps if it does not complete successfully,
- confirm the build reports version `X.Y.Z` rather than a timestamped snapshot,
- verify the Maven Central deployment becomes available.

If Maven Central accepts the upload but requires an interactive/manual portal action, stop at that point and ask the repository owner to perform only that required action. Continue verification afterward.

### 7. Final cleanup and checks

If a temporary release-helper workflow was used, remove it from `master` after it has completed. Verify that:

- `release/X.Y.Z` still points at the original frozen SHA,
- the GitHub release is published and has the intended notes,
- the Maven publication workflow succeeded,
- Maven Central exposes the new version,
- no temporary release machinery remains in `master`.
