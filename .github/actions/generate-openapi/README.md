actions/generate-openapi
========================

Dette er ein custom composite action som utfører følgande:

1. Genererer ny k9-sak.openapi.json ut frå java koden
2. Henter inn [navikt/k9-sak-typescript-client](https://github.com/navikt/k9-sak-typescript-client)
3. Genererer ny typescript klient kode ut frå ny openapi json
4. Viss ny klient kode er forskjellig frå tidlegare, commit, tag og push ny klient kode til [navikt/k9-sak-typescript-client](https://github.com/navikt/k9-sak-typescript-client)

Tag på på commit vil vere basert på deklarert openapi versjonsnr for major og minor delane, og input _patchVersion_for patch 
versjon. Dette vil i neste omgang føre til at det blir publisert ny javascript pakke med generert klient-kode på dette versjonsnr.

### Inputs

Nevnte _patchVersion_ er nødvendig. Dette skal settast til _TAG_ frå build workflow, altså (<TIMESTAMP>-<GIT_COMMIT_SHA>), feks _20240229124402-a2425c8_.


