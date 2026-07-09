# AGENTS.md — ung-sak

## Repository Overview
`ung-sak` er en Maven multi-module monolitt for saksbehandling av ungdomsprogramytelsen.

## Architecture
- `behandlingslager`: persistens og domenelagring (Hibernate/JPA, repositories).
- `domenetjenester`: domene- og forretningslogikk.
- `behandlingsprosess`: orkestrering/tilstandsmaskin for behandling.
- `web`: Jetty/Jersey REST API med OpenAPI-annotasjoner.
- `migreringer`: Flyway-migreringer og databaseskjema-initialisering.
- `formidling` og `formidling-pdfgen-templates`: dokumentgenerering.

## Domain Notes: Forlenget periode
- `UngdomsprogramMaksPeriode` er sentral modell for maks varighet og inneholder `harForlengetPeriode`, `periodeMaksDato` og hjemmel.
- `periodeMaksDato` fra register er kilde til sannhet for maksdato. Unngaa lokal materialisering av sluttdato for aapne perioder.
- Hendelsesflyt bruker `UNGDOMSPROGRAM_FORLENGET_PERIODE`.
- Startpunkt for ny vurdering ved forlenget periode avhenger av endring i `periodeMaksDato` (diff-sporing), ikke kun boolsk flagg.
- Revurdering ved forlenget periode skal vise triggerperioden (de nye 8 ukene), ikke hele opprinnelig programperiode.

## Domain Notes: Opphør ved maksdato
- Når ungdomsprogramytelsen nærmer seg maksdato (260/300 virkedager), skal deltaker varsles ~3 uker før og få uttale seg før opphør.
- Batch `VarselOpphørVedMaksdatoBatchTask` (cron ~07:30) spawner `VarselOpphørVedMaksdatoTask`, som henter løpende `UNGDOMSYTELSE`-fagsaker (`AktuelleFagsakerForMaksdatoVarselRepository`), bruker `periodeMaksDato` fra registergrunnlag (kilde til sannhet) og oppretter revurdering med årsak `RE_VARSEL_OPPHOR_VED_MAKSDATO` innenfor 3-ukers vindu (`MaksdatoOpphørVarslingPeriode.VARSEL_UKER_FØR_MAKSDATO = 3`).
- Batch-tasker bruker felles baseklasse `DuplikatbeskyttetBatchTask` (unngår duplikate child-tasks på FEILET/KLAR/VETO); delt med inntektskontroll- og høysats-batch.
- Etterlysning av type `UTTALELSE_OPPHOR_VED_MAKSDATO` opprettes i `VurderKompletthetSteg` via `UngEtterlysningOppretter` -> `MaksdatoEtterlysningTjeneste`. Oppgave til deltaker (`BEKREFT_OPPHOR_VED_MAKSDATO`) lages av `OpphørVedMaksdatoOppgaveOppretter`; behandlingen settes på vent (autopunkt `AUTO_SATT_PÅ_VENT_REVURDERING`).
- Bruker-svar: `EndringType.OPPHOR_VED_MAKSDATO` og bekreftelse `Bekreftelse.Type.UNG_OPPHOR_VED_MAKSDATO` håndteres av `GenerellOppgaveBekreftelseHåndterer`; etterlysning markeres `MOTTATT_SVAR` -> behandling tas av vent.
- Overstyring: `ProsessTriggerFilter` filtrerer bort `RE_VARSEL_OPPHOR_VED_MAKSDATO` når forlenget periode (`RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM`) eller manuelt opphør (`RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM`) også finnes. Samme overstyringsprinsipp følges i brevregler (`OpphørVedMaksdatoStrategy`, `OpphørStrategy`, `ForlengetPeriodeStrategy`) og i årsaks-/periodevisning.
- Naturlig avslutning: `UngdomsprogramOpphørFagsakTilVurderingUtleder` ignorerer opphørshendelse når `opphørsdato == periodeMaksDato` (unngår duplikat revurdering).
- Brev/kodeverk: `DokumentMalType.OPPHOR_VED_MAKSDATO_DOK`, `TemplateType.OPPHOR_VED_MAKSDATO`, `opphør_ved_maksdato.hbs`, `DetaljertResultatType.OPPHØR_VED_MAKSDATO`, `BehandlingVisningsnavn.OPPHØR_VED_MAKSDATO`, `ÅrsakTilVurdering.OPPHØR_VED_MAKSDATO`. `RE_VARSEL_OPPHOR_VED_MAKSDATO` inngår i `BehandlingÅrsakType.årsakerForInnhentingAvProgramperiode()`.
- Ren varsel-opphør-behandlingsflyt = behandlingen har **utelukkende** årsak `RE_VARSEL_OPPHOR_VED_MAKSDATO`. Da skal det IKKE trigges inntektskontroll/programperiodeendring-varsling (`UngEtterlysningOppretter`), og `MaksdatoEtterlysningTjeneste` hardfeiler dersom ingen etterlysning ble opprettet — for å hindre vedtak/brev om opphør uten kontradiksjon. Har behandlingen tilleggsårsaker (f.eks. inntektskontroll) eller er overstyrt, kjøres alt som før og det hardfeiles ikke.
- Dedup i utvelgelsen ekskluderer fagsaker med aktiv `RE_VARSEL_OPPHOR_VED_MAKSDATO`-trigger som overlapper maksdato, OG fagsaker med åpen behandling (status != `AVSLU`/`IVED`) med samme årsak.
- Ved endringer i opphør-ved-maksdato: hold sammen batch/utvelgelse, etterlysning/varsling, mottak av bekreftelse, startpunkt/steg, brev og dedup i samme leveranse.

## Domain Notes: Opphevelse av opphør
- Lar en veileder rette et feilaktig opphør ved å slette sluttdatoen i ung-deltakelse-opplyser — typisk aktuelt når bruker får medhold i klage på opphør av ungdomsprogrammet. Det er ikke behov for varsel til bruker i disse tilfellene, siden bruker allerede har vært i kontakt med Nav i forkant.
- Egen hendelse `UngdomsprogramOpphørOpphevetHendelse` (kontrakt, se PR #1453) brukes i stedet for å gjenbruke opphørshendelsen, siden flyt og brev er forskjellig.
- `UngdomsprogramOpphørOpphevetFagsakTilVurderingUtleder` (`@HendelseTypeRef("UNGDOMSPROGRAM_OPPHØR_OPPHEVET")`) utleder perioden som revurderes: fra dagen etter tidligere opphørsdato og fram til **uendret** `periodeMaksDato` (maksdato forholder seg kun til startdato, og skal ikke endres av opphevelsen). Selve gjenåpningen av programperioden skjer automatisk ved re-innhenting av periodegrunnlag fra register i den påfølgende revurderingen — utlederen avgjør kun hvilken fagsak/periode som skal revurderes, ikke periodeinnholdet. Idempotent dersom periodegrunnlaget allerede strekker seg forbi tidligere opphørsdato.
- `BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM` inngår i `årsakerForInnhentingAvProgramperiode()` slik at programperioden hentes på nytt fra register ved revurderingen.
- Ren opphevelse-av-opphør-behandlingsflyt = behandlingens (relevante) årsaker består **kun** av `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM` (ev. sammen med den nå utdaterte `RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM` — se under). Da opprettes ingen etterlysninger/oppgaver til bruker i det hele tatt (`UngEtterlysningOppretter`). Har behandlingen reelle tilleggsårsaker (f.eks. `RE_KONTROLL_REGISTER_INNTEKT`), kjøres kontroll av inntekt og varsel om opphør ved maksdato som normalt — men programperiodeendring-etterlysningen (sluttdato/periode) hoppes **alltid** over når opphevelses-årsaken er til stede, siden periodegrunnlag-diffen da alltid skyldes selve opphevelsen, som ikke skal gi ny uttalelse.
- Selvutløst etterkontroll (skill fra punktet over — dette gjelder *nye* årsaker opphevelsen selv skaper, ikke allerede eksisterende tilleggsårsaker): `KontrollerteInntektperioderTjeneste.ryddPerioderFritattForKontrollEllerTilVurderingIBehandlingen` fritar første/siste måned i programperioden for kontroll. Når et tidligere opphør korter ned sluttdatoen slik at en allerede kontrollert måned blir ny (fritatt) siste måned, mister den sin kontrollert-status. Gjenåpner opphevelsen perioden igjen, er måneden ikke lenger siste måned og trenger kontroll på nytt — men den fremstår da som "aldri kontrollert". `VurderManglendeKontrollAvPeriode`/`ManglendeKontrollperioderTjeneste` (kjører etter *ethvert* vedtak, uavhengig av `VurderKompletthetSteg`/`UngEtterlysningOppretter`) oppdager dette og oppretter automatisk en **ny** revurdering med årsak `RE_KONTROLL_REGISTER_INNTEKT` for opphevOpphør-behandlingen. Dette er forventet/riktig — ikke en bug — men kan overraske i saksbilder som viser en ekstra "Kontroll av inntekt"-behandling rett etter opphevelsen.
- Sammenslåing av hendelser på en åpen behandling: dersom opphevOpphør-hendelsen slås sammen med en fortsatt åpen behandling som venter på bekreftelse av det (nå opphevede) opphøret (jf. `OpprettRevurderingEllerOpprettDiffTask`), kan behandlingen ende opp med både `RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM` og `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM`. Da avbrytes eventuelle ventende `UTTALELSE_ENDRET_SLUTTDATO`- og `UTTALELSE_ENDRET_PERIODE`-etterlysninger (`ProgramperiodeendringEtterlysningTjeneste.avbrytVentendeSluttdatoEtterlysninger` — begge etterlysningstyper dekkes for å håndtere gammel og ny programperiode-flyt), og den utdaterte opphør-årsaken teller ikke som en reell tilleggsårsak. Etterlysninger om endret startdato berøres ikke. Samme toleranse for den utdaterte årsaken gjelder i `BehandlingDtoUtil.utledVisningsnavn` (returnerer fortsatt `OPPHØR_OPPHEVET_UNGDOMSPROGRAM`, ikke `FLERE_BEHANDLINGÅRSAKER`) — hold disse to stedene i sync ved videre endringer.
- Overstyring: `ProsessTriggerFilter` filtrerer bort `RE_VARSEL_OPPHOR_VED_MAKSDATO` når `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM` også finnes, på linje med forlenget periode og manuelt opphør.
- Periodevisning: `UtledStatusForPerioderPåBehandling.RELEVANTE_ÅRSAKER` inkluderer `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM` slik at behandlingen viser triggerperioden i sidepanelet (samme "diff-vindu"-mønster som forlenget periode, ikke hele programperioden fra startdato). `finnÅrsakerFraTriggereTidslinje` filtrerer i tillegg bort `RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM` når opphevelses-årsaken er til stede, slik at kun «Opphevelse av opphør» vises — jf. samme "stale cause"-konvensjon som brukes i `UngEtterlysningOppretter`, `BehandlingDtoUtil` og `UngDetaljertResultatTidslinjeUtleder`.
- Brev: `UngDetaljertResultatTidslinjeUtleder` undertrykker `ENDRING_SLUTTDATO`-resultatet for `RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM`-perioder når behandlingen har `harOpphevelseAvOpphør == true` (`UngDetaljertResultatBehandlingGrunnlag`). Dette forhindrer at både Opphør-brev og Opphør opphevet-brev bestilles ved sammenslåing (race). Flagget settes fra `behandling.getBehandlingÅrsakerTyper().contains(RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM)` i utledningssteget.
- Brev/kodeverk: `DokumentMalType.OPPHOR_OPPHEVET_DOK`, `TemplateType.OPPHOR_OPPHEVET`, mal `opphør_opphevet.hbs`, `OpphørOpphevetStrategy`/`OpphørOpphevetInnholdBygger`/`OpphørOpphevetDto` (viser tidligere sluttdato og gjeldende maksdato), `DetaljertResultatType.OPPHØR_OPPHEVET`, `BehandlingVisningsnavn.OPPHØR_OPPHEVET_UNGDOMSPROGRAM`, `ÅrsakTilVurdering.OPPHØR_OPPHEVET_UNGDOMSPROGRAM`.
- **Opphevelsesbrev kun når opphøret faktisk ble vedtatt**: Dersom opphevelseshendelsen slås sammen med en behandling som ennå venter på uttalelse fra deltaker om opphøret (autopunkt `AUTO_SATT_PÅ_VENT_REVURDERING`, jf. punktet over) og deltaker ikke rekker å svare før opphevelsen kommer inn, ble opphøret aldri reelt vedtatt/iverksatt — da finnes det ikke noe opphørsbrev å oppheve, og «Opphør opphevet»-brevet skal **ikke** sendes. `OpphørOpphevetStrategy` sjekker derfor om originalbehandlingen (siste vedtatte, via `behandling.getOriginalBehandlingId()` + `UngdomsprogramPeriodeRepository`) faktisk hadde en **lukket** sluttdato (ikke `TIDENES_ENDE`) før brevet sendes — samme mønster som `ProgramPeriodeStrategy.haddeÅpenSluttdatoIForrigeBehandling`, men med motsatt hensikt. Behandlingsårsaken `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM` forblir synlig som normalt (append-only) selv om brevet uteblir — sporing av hva som skjedde dekkes av historikkinnslag-mekanismen i `RegisterdataEndringshåndterer`/`RegisterinnhentingHistorikkinnslagTjeneste` (se eget punkt under om påkrevd årsak-registrering). Se `OpphørOpphevetStrategyTest`.
- **`RegisterdataEndringshåndterer.REGISTEROPPLYSNING_BEHANDLINGÅRSAKER` må inkludere `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM`**: Historikkinnslaget "Behandlingen oppdatert med nye opplysninger" (med årsakstekst) opprettes av `RegisterdataEndringshåndterer.lagHistorikkinnslag` kun for årsaker i dette settet — manglet en årsak her, ble historikkinnslaget stille utelatt (ingen feilmelding), til tross for at behandlingsårsaken selv fortsatt var synlig i behandlingsårsaker-visningen. Dette var opprinnelig glemt for opphevelse-av-opphør-årsaken, slik at verken det generiske eller det spesifikke innslaget dukket opp for rene opphevelser. Rettet ved å legge årsaken til i settet; regresjonstest i `RegisterdataEndringshåndtererTest`. Sjekk dette settet ved fremtidige nye hendelsestyper som skal reposisjonere behandlingen.
- **Opphør og opphevelse i samme vs. ulik behandling**: Avgjørende skille er om opphøret ble vedtatt i en tidligere, avsluttet behandling (`DetaljertResultatType.OPPHØR_OPPHEVET`, gir eget vedtaksbrev), eller om opphør og opphevelse havnet på **samme, fortsatt åpne** behandling (`DetaljertResultatType.OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING`, ingen brev — det finnes ikke noe opphørsvedtak å reversere). Skillet avgjøres sentralt i `UngDetaljertResultatTidslinjeUtleder` (ved hjelp av `UngdomsprogramOpphørUtleder.opphørAvUngdomsprogrammetVarInkludertIVedtaket(Behandling, UngdomsprogramPeriodeRepository)` i `behandlingslager`, som sjekker om originalbehandlingen hadde lukket sluttdato, ikke `TIDENES_ENDE`) — én gang, som del av `DetaljertResultat`-tidslinjen, slik at alle konsumenter (nå og i fremtiden) automatisk får riktig håndtering uten å gjøre egen utledning. `OpphørOpphevetStrategy` (brevutsendelse) leser kun av `DetaljertResultatType` direkte. Tilsvarende skille finnes også i `BehandlingÅrsakType`-domenet for visning: `BehandlingDtoUtil.utledVisningsnavn` (velger `BehandlingVisningsnavn.OPPHØR_OPPHEVET_UNGDOMSPROGRAM` vs. `OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING_UNGDOMSPROGRAM`) og `UtledStatusForPerioderPåBehandling.mapTilÅrsakTilVurdering` (velger `ÅrsakTilVurdering.OPPHØR_OPPHEVET_UNGDOMSPROGRAM` vs. `OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING_UNGDOMSPROGRAM` i periodevisningen) bruker samme `UngdomsprogramOpphørUtleder`, kun for tilfeller der behandlingsårsaken er `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM`. `ÅrsakTilVurdering.SAMMENHENG` (statisk 1:1-mapping `BehandlingÅrsakType`→`ÅrsakTilVurdering`) beholder default-mappingen til `OPPHØR_OPPHEVET_UNGDOMSPROGRAM`; overstyringen håndteres kontekstuelt i `mapTilÅrsakTilVurdering`, ikke i det statiske kartet.
- Ved endringer i opphevelse av opphør: hold sammen kontrakt (ung-deltakelse-opplyser), hendelsesutleder, etterlysningsunntak, prosesstrigger-overstyring, brev og periodevisning i samme leveranse.

## Tech Stack
- Java 25 (hovedkodebase)
- Java 21 (`kodeverk` og `kontrakt`)
- Maven
- PostgreSQL
- Flyway
- Jetty 12 + Jersey

## Build & Test Commands
```bash
mvn clean install
mvn test
mvn clean package -DskipTests
dev/generate-openapi-ts-client.sh
```

## Local Dev Prerequisites
- PostgreSQL maa vaere oppe med schema `ung_sak_unit` for tester.
- Lokal app bruker typisk schema `ung_sak`.
- Start DB via `k9-verdikjede/saksbehandling` (se `README.md`).
- Kjor `no.nav.ung.sak.db.util.Databaseskjemainitialisering` eller `mvn test-compile` ved behov for skjemaendringer.
- Lokal server startes via `JettyDevServer --vtp`.

## Conventions
- Hold dependency-versjoner sentralt i root `pom.xml` med mindre modulspesifikk grunn finnes.
- Unngaa scopes i `dependencyManagement` med mindre eksisterende mønster tilsier det.
- API-endringer i `web` skal oppdatere OpenAPI og ved behov regenerere TypeScript-klient.
- Flyway-migreringer skal følge eksisterende naming/orden i `migreringer/src/main/resources/db/migration`.
- Behold pakke- og navnekonvensjoner under `no.nav.ung.sak.<module>...` (`*Repository`, `*Tjeneste`, `*Steg`).
- Bruk `Range<LocalDate>` med `@Type(PostgreSQLRangeType.class)` og `columnDefinition = "daterange"` for perioder i JPA-entiteter.
- I nye Flyway-migreringer skal SQL skrives med lowercase. Eksisterende migreringer trenger ikke omskrives kun for casing.
- Ved endringer for forlenget periode: oppdater baade hendelser, grunnlagsmodell (`UngdomsprogramMaksPeriode`), startpunktutledning og brev/API i samme leveranse.

## Boundaries
### Always
- Folg etablerte mønstre i modulen du endrer.
- Hold endringer smaaa og fokuserte.
- Kjor relevante tester for berorte moduler.
- Verifiser kombinasjonsflyt der forlenget periode samhandler med andre revurderingsaarsaker (f.eks. inntektskontroll).
- Verifiser kombinasjonsflyt der opphevelse av opphør samhandler med andre revurderingsaarsaker (ren behandlingsflyt vs. tilleggsårsaker).

### Ask First
- Store refaktoreringer paa tvers av modulgrenser.
- Innforing av nye rammeverk eller infrastrukturavhengigheter.
- Endringer i deploy/CI-oppsett uten tydelig behov.

### Never
- Commit hemmeligheter, tokens eller credentials.
- Bryt Java 21-kompatibilitet i `kodeverk`/`kontrakt`.
- Lag migreringer som ikke er Flyway-kompatible.

## Key References
- `README.md`
- `.github/workflows/build.yaml`
- `dokumentasjon/arkitekturbeslutninger.md`
- `migreringer/pom.xml`
- `web/README.md`
- PR-historikk for forlenget periode:
  - https://github.com/navikt/ung-sak/pull/1299
  - https://github.com/navikt/ung-sak/pull/1336
  - https://github.com/navikt/ung-sak/pull/1350
  - https://github.com/navikt/ung-sak/pull/1365
  - https://github.com/navikt/ung-sak/pull/1367
  - https://github.com/navikt/ung-sak/pull/1370
- PR-historikk for opphør ved maksdato:
  - https://github.com/navikt/ung-sak/pull/1333
  - https://github.com/navikt/ung-sak/pull/1449
- PR-historikk for opphevelse av opphør:
  - https://github.com/navikt/ung-sak/pull/1453
  - https://github.com/navikt/ung-sak/pull/1454
  - https://github.com/navikt/ung-sak/pull/1463
  - https://github.com/navikt/ung-sak/pull/1468
