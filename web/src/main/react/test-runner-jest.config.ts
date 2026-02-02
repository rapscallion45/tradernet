import { getJestConfig } from '@storybook/test-runner';

/** The default Jest configuration comes from @storybook/test-runner */
const testRunnerJestConfig = getJestConfig()

/**
 * Storybook Test Runner Jest configuration
 */
module.exports = {
  ...testRunnerJestConfig,
  /** Add your own overrides below, and make sure
   *  to merge testRunnerConfig properties with your own
   * @see https://jestjs.io/docs/configuration
   */
  snapshotSerializers: [
    /** Sets up the custom serializer to preprocess the HTML before it's passed onto the test-runner */
    './storybook/snapshot-serializer.ts',
    // @ts-ignore
    ...testRunnerJestConfig.snapshotSerializers,
  ],
}
