# ung-sak

[![](https://github.com/navikt/ung-sak/workflows/Bygg%20og%20deploy/badge.svg)](https://github.com/navikt/ung-sak/actions?query=workflow%3A%22Bygg+og+deploy%22) [![](https://github.com/navikt/k9-verdikjede/workflows/Tester%20saksbehandling/badge.svg)](https://navikt.github.io/k9-verdikjede)

Dette er repository for kildkode applikasjonen for saksbehandling av ungdomsytelsen

# Utvikling
   
## Enhetstester
Start postgres først for å kjøre alle enhetstester. Bruker schema ung_sak_unit i
[Verdikjede](https://github.com/navikt/k9-verdikjede/tree/master/saksbehandling)
`git clone git@github.com:navikt/k9-verdikjede.git; cd k9-verdikjede/saksbehandling; ./update-versions.sh; docker-compose up postgres`  

Kjør `no.nav.ung.sak.db.util.Databaseskjemainitialisering` for å få med skjemaendringer

## Lokal utvikling
1. Start postgres først. Bruker schema ung_sak lokalt
   `cd dev; docker-compose up postgres`

2. Start webserver fra f.eks. IDE
   Start `JettyDevServer --vtp` 

Swagger: http://localhost:8901/ung/sak/swagger

### Generering av openapi spesifikasjon og typescript klient.

I build pipeline blir det automatisk generert en ung-sak.openapi.json fil som beskriver rest apiet på samme måte som 
swagger serveren. Denne fila blir deretter brukt til å automatisk generere et typescript klientbibliotek som kan brukes
til å kommunisere med serveren fra nettleser.

Generert typescript klient blir publisert som **GitHub npm pakke [@navikt/ung-sak-typescript-client](https://github.com/navikt/ung-sak/pkgs/npm/ung-sak-typescript-client)**

Ved behov kan openapi spesifikasjon og typescript klient genereres i lokalt utviklingsmiljø.

Man kan kjøre dette script:

```bash
dev/generate-openapi-ts-client.sh
```

Eller en av intellij run konfigurasjonene som heter `web/generate typescript client`

Dette vil generere fil _web/src/main/resources/openapi-ts-client/ung-sak.openapi.json_. Deretter vil den 
kjøre docker image for å generere typescript klient ut fra generert openapi.json fil.

Generert typescript/javascript blir plassert i _web/target/ts-client_.

Man kan deretter linke direkte til denne plassering fra web koden som skal bruke den, som beskrevet i 
[ung-sak-web/.../backend/README](https://github.com/navikt/ung-sak-web/tree/master/packages/v2/backend#lokal-ung-sak-typescript-client-bruk)

På denne måten kan man teste om backend endringer fører til feil i frontend før man pusher til github.

Se også [openapi-ts-client/README](web/src/main/resources/openapi-ts-client/README.md) for mer teknisk info.
