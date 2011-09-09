This feature is a helper project that allows to us to easily create a list of plugins and a jnlp file for the p2 lineup web installer.

This project is not built by the nexus-p2 build.

It requires the eclipse delta pack and the p2 installer in the target platform.

To generate the plugins:
Export the feature with the following options:
- Generate metadata repository - checked
- Export for multiple platforms - checked (this option is displayed only when the delta pack is in the target platform)
- Sign the jar archives - unchecked (it doesn't seem to work)

To generate the jnlp:
- Generate metadata repository - unchecked
- Export for multiple platforms - checked (this option is displayed only when the delta pack is in the target platform)
- Sign the jar archives - unchecked (it doesn't seem to work)
- Create JNLP manifests for the JAR archives - checked