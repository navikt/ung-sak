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

### Generering av openapi spesifikasjon og typescript klient.

I build pipeline blir det automatisk generert en k9-sak.openapi.json fil som beskriver rest apiet på samme måte som 
swagger serveren. Denne fila blir deretter brukt til å automatisk generere et typescript klientbibliotek som kan brukes
til å kommunisere med serveren fra nettleser.

Generert typescript klient blir publisert som **GitHub npm pakke [@navikt/k9-sak-typescript-client](https://github.com/navikt/k9-sak/pkgs/npm/k9-sak-typescript-client)**

Ved behov kan openapi spesifikasjon og typescript klient genereres i lokalt utviklingsmiljø.

Man kan kjøre dette script:

```bash
dev/generate-openapi-ts-client.sh
```

Eller en av intellij run konfigurasjonene som heter `web/generate typescript client`

Dette vil generere fil _web/src/main/resources/openapi-ts-client/k9-sak.openapi.json_. Deretter vil den 
kjøre docker image for å generere typescript klient ut fra generert openapi.json fil.

Generert typescript/javascript blir plassert i _web/target/ts-client_.

Man kan deretter linke direkte til denne plassering fra web koden som skal bruke den, som beskrevet i 
[k9-sak-web/.../backend/README](https://github.com/navikt/k9-sak-web/tree/master/packages/v2/backend#lokal-k9-sak-typescript-client-bruk)

På denne måten kan man teste om backend endringer fører til feil i frontend før man pusher til github.

Se også [openapi-ts-client/README](web/src/main/resources/openapi-ts-client/README.md) for mer teknisk info.


