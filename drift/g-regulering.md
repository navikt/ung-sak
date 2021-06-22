# G-regulering

Dokumentasjon for å kjøre g-regulering for ytelser i k9-sak.

## Forutsettninger

- Oppdatert g-sats i ft-kalkulus
- Bruker med driftstilgang

## Utførelse

1. Logg inn i produksjonsmiljø med driftsbruker
2. Gå til swagger https://----produksjon----/k9/sak/swagger
3. Velg tjenesten '/api/prosesstask/createProsessTask' (Oppretter en prosess task i henhold til request)
4. Kjør med payload, hvor YTELSE(ytelsen det skal kjøres for), FOM og TOM settes til datoene fom = dagen ny g-sats gjelder fra, tom = dagen satsen ble produksjonssatt i kalkulus
   
``{
   "taskType": "gregulering.kandidatUtledning",
   "taskParametre": {
   "ytelseType": "YTELSE",
   "fom": "FOM",
   "tom": "TOM",
   "dryrun": true } }``

5. Sjekk loggen etter antall kandidater som skal sjekkes Eks. 'DRYRUN - Fant 3442 kandidater til g-regulering for 'OMSORGSPENGER' og perioden 'Periode: [01.05.2021,22.05.2021]'.'
6. Hvis OK, sett i gang g-regulering ved å bruke samme endepunkt med følgende payload:

``{
"taskType": "gregulering.kandidatUtledning",
"taskParametre": {
"ytelseType": "YTELSE",
"fom": "FOM",
"tom": "TOM" } }``

7. G-regulering starter, kan følges ved å sjekke kibana.
'+envclass:"p" +application:k9-sak +component:"no.nav.folketrygdloven.beregningsgrunnlag.regulering.VurderGReguleringKandidatTask"'
