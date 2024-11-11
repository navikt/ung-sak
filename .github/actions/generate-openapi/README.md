actions/generate-openapi
========================

Dette er ein custom composite action som genererer ny ung-sak.openapi.json ut frå java koden.

Viss ny ung-sak.openapi.json er forskjellig frå tidlegare, commit og push endringer til branch.

### Inputs

- _readerToken_: secrets.READER_TOKEN frå kallande workflow. Nødvendig for at maven install skal fungere.

### Outputs

- _haschanged_: true viss køyring av action førte til endring (og commit) av ung-sak.openapi.json
- _openapiVersion_: versjonsnr frå generert ung-sak.openapi.json

