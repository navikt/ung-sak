actions/generate-typescript-client
==================================

Dette er ein custom composite action som genererer ny typescript klient kode ut frå openapi json.

Viss den blir køyrt på master branch blir generert klient og publisert som npm pakke.

### Inputs

- _openapiVersion_: Resultat frå generate-openapi action
- _patchVersion_: Settast til build-version resultat frå build-app workflow.
- _githubToken_: secrets.GITHUB_TOKEN frå kallande workflow. Må ha lov til å publisere npm pakke.
- _forcePublish_: Sett til true for å publisere pakke sjølv om action ikkje køyre på master bracnh. Brukast til debug av pipeline på anna branch.

