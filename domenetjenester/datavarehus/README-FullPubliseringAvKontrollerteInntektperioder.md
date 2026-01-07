# Full Publisering av Kontrollerte Inntektperioder Metrikk

## Oversikt

Det har blitt opprettet en ny prosesstask for å kjøre full publisering av kontrollerte inntektperioder metrikker til BigQuery. Dette er en batch-task som kan brukes til initiell opplasting eller re-publisering av historiske data.

## Opprettede filer

### 1. FullPubliseringAvKontrollerteInntektperioderMetrikkTask.java
**Plassering:** `domenetjenester/datavarehus/src/main/java/no/nav/ung/sak/metrikker/`

**Funksjonalitet:**
- Oppretter prosesstasks for alle behandlinger som oppfyller kriteriene
- Delegerer til eksisterende `PubliserKontrollerteInntektperioderMetrikkTask` for hver behandling
- Kjører som en batch-operasjon

**Kriterier for behandlinger:**
- Behandlingsstatus er `AVSLUTTET` eller `IVERKSATT`
- Fagsak er av type `UNGDOMSYTELSE`
- Behandlingsårsak er `RE_KONTROLL_REGISTER_INNTEKT`
- Vedtak har status `IVERKSATT`

### 2. FullPubliseringAvKontrollerteInntektperioderMetrikkTaskTest.java
**Plassering:** `domenetjenester/datavarehus/src/test/java/no/nav/ung/sak/metrikker/`

**Funksjonalitet:**
Tester som verifiserer at tasken:
- Oppretter prosesstasks for gyldige behandlinger
- Ikke oppretter prosesstasks for behandlinger uten inntektskontroll årsak
- Ikke oppretter prosesstasks for behandlinger som ikke er iverksatt

## Hvordan bruke


### Manuell kjøring via SQL

For å starte en full publisering, kjør følgende SQL i databasen:

```sql
INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
VALUES (nextval('seq_prosess_task'),
        'init.fullPubliseringAvKontrollerteInntektperioder',
        nextval('seq_prosess_task_gruppe'),
        current_timestamp at time zone 'UTC',
        '');
```

### Programmatisk kjøring

```java
@Inject
private ProsessTaskTjeneste prosessTaskTjeneste;

public void startFullPublisering() {
    ProsessTaskData taskData = ProsessTaskData.forProsessTask(
        FullPubliseringAvKontrollerteInntektperioderMetrikkTask.class
    );
    prosessTaskTjeneste.lagre(taskData);
}
```

## Arkitektur

```
FullPubliseringAvKontrollerteInntektperioderMetrikkTask
    │
    ├─> Identifiserer alle relevante behandlinger (SQL)
    │
    └─> Oppretter individuelle prosesstasks
         │
         └─> PubliserKontrollerteInntektperioderMetrikkTask (per behandling)
              │
              └─> KontrollerteInntektPerioderMetrikkPubliserer
                   │
                   └─> BigQueryKlient (publiserer til BigQuery)
```

## Konfigurasjon

Tasken respekterer følgende miljøvariabler:
- `BIGQUERY_ENABLED`: Må være `true` for at data faktisk skal publiseres til BigQuery
- `PUBLISER_KONTROLLERT_INNTEKT_METRIKK_ENABLED`: Kontrollerer om metrikker skal publiseres

## Merknader

1. Tasken oppretter én prosesstask per behandling, med 2 sekunders forsinkelse mellom hver task
2. Eksisterende `PubliserKontrollerteInntektperioderMetrikkTask` håndterer selve publiseringen
3. Tasken er idempotent - den kan kjøres flere ganger uten problemer (vil opprette nye tasks hver gang)

## Task Type

Task type: `init.fullPubliseringAvKontrollerteInntektperioder`

Dette følger samme navnekonvensjon som `FullPubliseringAvStønadstatistikkTask` (`init.fullPubliseringAvStonadstatistikk`).

