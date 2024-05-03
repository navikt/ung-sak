# k9-sak

[![](https://github.com/navikt/k9-sak/workflows/Bygg%20og%20deploy/badge.svg)](https://github.com/navikt/k9-sak/actions?query=workflow%3A%22Bygg+og+deploy%22) [![](https://github.com/navikt/k9-verdikjede/workflows/Tester%20saksbehandling/badge.svg)](https://navikt.github.io/k9-verdikjede)

Dette er repository for kildkode applikasjonen for saksbehandling av ytelser i [Folketrygdloven kapittel 9](https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_5-5#%C2%A79-1)

# Utvikling
   
## Enhetstester
Start postgres først for å kjøre alle enhetstester. Bruker schema k9sak_unit i
[Verdikjede](https://github.com/navikt/k9-verdikjede/tree/master/saksbehandling)
`git clone git@github.com:navikt/k9-verdikjede.git; cd k9-verdikjede/saksbehandling; ./update-versions.sh; docker-compose up postgres`  

Kjør `no.nav.k9.sak.db.util.Databaseskjemainitialisering` for å få med skjemaendringer

## Lokal utvikling
1. Start postgres først. Bruker schema k9sak lokalt
   `cd dev; docker-compose up postgres`

2. Start webserver fra f.eks. IDE
   Start `JettyDevServer --vtp` 

Swagger: http://localhost:8080/k9/sak/swagger

### Generering av openapi spesifikasjon og typescript klient lokalt.

Ved behov kan openapi spesifikasjon og typescript klient genererast med lokalt utviklingsmiljø.

```bash
dev/generate-openapi-ts-client.sh
```
Kommandoen over vil generere fil _web/src/main/resources/openapi-ts-client/k9-sak.openapi.json_. Deretter vil den 
køyre docker image for å generere typescript klient ut frå generert openapi.json fil.

Generert typescript/javascript blir skrive til _web/target/ts-client_.
