# k9-sak

![](https://github.com/navikt/k9-sak/workflows/Bygg%20og%20deploy/badge.svg)

Dette er repository for kildkode applikasjonen for saksbehandling av ytelser i kapittel 9 i folketrygden. 

# Utvikling
   
## Enhetstester
Start postgres først for å kjøre alle enhetstester. Bruker schema k9sak_unit
> cd dev; docker-compose up postgres


## Lokal utvikling
1. Start postgres først. Bruker schema k9sak lokalt
   `cd dev; docker-compose up postgres`

2. Start webserver fra f.eks. IDE
   Start `JettyDevServer --vtp` 

