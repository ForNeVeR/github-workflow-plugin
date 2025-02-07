# GitHub Workflow Plugin

![Build](https://github.com/YunaBraska/github-workflow-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/21396-github-workflow.svg)](https://plugins.jetbrains.com/plugin/21396-github-workflow)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/21396-github-workflow.svg)](https://plugins.jetbrains.com/plugin/21396-github-workflow)
[![](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub&color=%23fe8e86)](https://github.com/sponsors/YunaBraska)

<!-- Plugin description -->
Autocompletion for GitHub workflows
Enjoy lightning-fast autocompletion for

* actions
* workflows
* inputs
* envs
* secrets
* jobs

### GitHub repository access

Add your GitHub account to access private repositories:
> File | Settings | Version Control | GitHub

<!-- Plugin description end -->

#### TODO

- [ ] Autocomplete workflow and actions refs e.g. `@main`, `@v1`, ...
- [ ] Add links to Workflows and action files (GitHubUrl && MarketplaceUrl)
- [ ] Add links to Definitions (jobs, steps, needs, secrets, inputs, envs)
  e.g. (https://github.com/cunla/ghactions-manager/blob/master/src/main/kotlin/com/dsoftware/ghmanager/api/Workflows.kt)
- [ ] Autogenerate `getGitHubContextEnvs`
  from (https://docs.github.com/en/actions/learn-github-actions/contexts#github-context)
- [ ] Autogenerate `getGitHubEnvs`
  from (https://docs.github.com/en/actions/learn-github-actions/variables#using-the-vars-context-to-access-configuration-variable-values)

## Learning List

- [ ] Create Tests
- [ ] Refactor - less custom elements == less memory leaks
- [ ] Auto Complete Uses with local action files
- [ ] Auto Complete Uses field with Tags & Branches
- [ ] Link local files action files aka find usages 
- [ ] implement CMD+B 
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Get familiar with the [template documentation][template].
- [x] Adjust the [pluginGroup](./gradle.properties), [plugin ID](./src/main/resources/META-INF/plugin.xml).
- [x] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])
- [x] Review
  the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [x] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate)
  for the first time.
- [ ] Set the `21396-github-workflow` in the above README badges.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate)
  related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set
  the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified
  about releases containing new features and fixes.

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
