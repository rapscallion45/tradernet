# Storybook Developer Instructions

To run the Storybook development server:

1. Run `yarn storybook`
2. Storybook should start on port 6006

## Storybook Snapshots

<mark><b>To perform snapshot testing, there must be an instance of Storybook running on port 6006.</b></mark>

Snapshot testing is performed as part of the
built-in [Test Runner](https://storybook.js.org/docs/writing-tests/test-runner), which turns all stories into
executable tests.

To run the `snapshots` test, as part of the Test Runner, run `yarn test-storybook`. This will generate snapshots
for all stories in the `lib` directory.

Snapshots are used to verify that the storybook components are rendering correctly. If you make a change to a component,
you will need to update the snapshot accordingly. The updated snapshot can then be reviewed as part of the PR review
process.

The Storybook snapshots are stored in the `./components/[component name]/__snapshots__/` directory,
of each component.

