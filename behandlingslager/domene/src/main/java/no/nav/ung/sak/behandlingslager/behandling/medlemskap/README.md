# Oppgitt forutgående medlemskap

Pakken inneholder entiteter for lagring av oppgitt forutgående medlemskap fra søknader om aktivitetspenger.

## Datamodell

```
Grunnlag (per behandling, aktiv-flagg)
  └─ Holder (aggregator, kan deles mellom behandlinger)
       └─ Periode (per søknad/journalpost)
            └─ Bosted (landkode + periode)
```

## Entiteter

| Klasse | Formål |
|--------|--------|
| `OppgittForutgåendeMedlemskapGrunnlag` | Knytter en behandling til en holder. Kun én aktiv rad per behandling. |
| `OppgittForutgåendeMedlemskapHolder` | Aggregator som samler alle søknadsperioder. Immutable — nye opplysninger gir ny holder. Deles mellom behandlinger ved revurdering. |
| `OppgittForutgåendeMedlemskapPeriode` | Immutable data fra én søknad (journalpostId, mottattTidspunkt, forutgående periode, bosteder). |
| `OppgittBosted` | Enkeltbosted i utlandet med landkode (ISO 3166-1 alpha-3) og periode. |
