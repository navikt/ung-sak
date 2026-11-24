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
- Etterlysning av type `UTTALELSE_OPPHOR_VED_MAKSDATO` opprettes i `VurderKompletthetSteg` via `UngEtterlysningOppretter` -> `MaksdatoEtterlysningTjeneste`. Oppgave til deltaker (`BEKREFT_OPPHOR_VED_MAKSDATO`) lages av `OpphørVedMaksdatoOppgaveOppretter`; behandlingen settes på vent (autopunkt `AUTO_SATT_PÅ_VENT_REVURDERING`). Oppgavens `sluttdato` vises til deltaker som «siste dag med ytelsen» (Min side-varsel og journalført PDF i ung-brukerdialog-api) og skal være `min(programperiode-tom, periodeMaksDato)` — aldri rå programperiode-tom, som er `TIDENES_ENDE` for åpne perioder (TSFF-3275).
- Bruker-svar: `EndringType.OPPHOR_VED_MAKSDATO` og bekreftelse `Bekreftelse.Type.UNG_OPPHOR_VED_MAKSDATO` håndteres av `GenerellOppgaveBekreftelseHåndterer`; etterlysning markeres `MOTTATT_SVAR` -> behandling tas av vent.
- Overstyring: `ProsessTriggerFilter` filtrerer bort `RE_VARSEL_OPPHOR_VED_MAKSDATO` når forlenget periode (`RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM`) eller manuelt opphør (`RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM`) også finnes. Samme overstyringsprinsipp følges i brevregler (`OpphørVedMaksdatoStrategy`, `EndringAvslagStrategy`, `ForlengetPeriodeStrategy`) og i årsaks-/periodevisning.
- Naturlig avslutning: `UngdomsprogramOpphørFagsakTilVurderingUtleder` ignorerer opphørshendelse når `opphørsdato == periodeMaksDato` (unngår duplikat revurdering).
- Brev/kodeverk: `DokumentMalType.OPPHOR_VED_MAKSDATO_DOK`, `TemplateType.OPPHOR_VED_MAKSDATO`, `opphør_ved_maksdato.hbs`, `BehandlingVisningsnavn.OPPHØR_VED_MAKSDATO`, `ÅrsakTilVurdering.OPPHØR_VED_MAKSDATO`. `RE_VARSEL_OPPHOR_VED_MAKSDATO` inngår i `BehandlingÅrsakType.årsakerForInnhentingAvProgramperiode()`.
- Ren varsel-opphør-behandlingsflyt = behandlingen har **utelukkende** årsak `RE_VARSEL_OPPHOR_VED_MAKSDATO`. Da skal det IKKE trigges inntektskontroll/programperiodeendring-varsling (`UngEtterlysningOppretter`), og `MaksdatoEtterlysningTjeneste` hardfeiler dersom ingen etterlysning ble opprettet — for å hindre vedtak/brev om opphør uten kontradiksjon. Har behandlingen tilleggsårsaker (f.eks. inntektskontroll) eller er overstyrt, kjøres alt som før og det hardfeiles ikke.
- Dedup i utvelgelsen ekskluderer fagsaker med aktiv `RE_VARSEL_OPPHOR_VED_MAKSDATO`-trigger som overlapper maksdato, OG fagsaker med åpen behandling (status != `AVSLU`/`IVED`) med samme årsak.
- Ved endringer i opphør-ved-maksdato: hold sammen batch/utvelgelse, etterlysning/varsling, mottak av bekreftelse, startpunkt/steg, brev og dedup i samme leveranse.

## Domain Notes: Opphevelse av opphør
- Lar en veileder rette et feilaktig opphør ved å slette sluttdatoen i ung-deltakelse-opplyser — typisk aktuelt når bruker får medhold i klage på opphør av ungdomsprogrammet. Det er ikke behov for varsel til bruker i disse tilfellene, siden bruker allerede har vært i kontakt med Nav i forkant.
- Egen hendelse `UngdomsprogramOpphørOpphevetHendelse` (kontrakt, se PR #1453) brukes i stedet for å gjenbruke opphørshendelsen, siden flyt og brev er forskjellig.
- `UngdomsprogramOpphørOpphevetFagsakTilVurderingUtleder` (`@HendelseTypeRef("UNGDOMSPROGRAM_OPPHØR_OPPHEVET")`) utleder perioden som revurderes: fra dagen etter tidligere opphørsdato og fram til **uendret** `periodeMaksDato` (maksdato forholder seg kun til startdato, og skal ikke endres av opphevelsen). Selve gjenåpningen av programperioden skjer automatisk ved re-innhenting av periodegrunnlag fra register i den påfølgende revurderingen — utlederen avgjør kun hvilken fagsak/periode som skal revurderes, ikke periodeinnholdet. Idempotent dersom periodegrunnlaget allerede strekker seg forbi tidligere opphørsdato.
- `BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM` inngår i `årsakerForInnhentingAvProgramperiode()` slik at programperioden hentes på nytt fra register ved revurderingen.
- Ren opphevelse-av-opphør-behandlingsflyt = behandlingens (relevante) årsaker består **kun** av `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM` (ev. sammen med den nå utdaterte `RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM` — se under). Da opprettes ingen etterlysninger/oppgaver til bruker i det hele tatt (`UngEtterlysningOppretter`). Har behandlingen reelle tilleggsårsaker (f.eks. `RE_KONTROLL_REGISTER_INNTEKT`), kjøres kontroll av inntekt og varsel om opphør ved maksdato som normalt — men programperiodeendring-etterlysningen (sluttdato/periode) hoppes **alltid** over når opphevelses-årsaken er til stede, siden periodegrunnlag-diffen da alltid skyldes selve opphevelsen, som ikke skal gi ny uttalelse.
- Selvutløst etterkontroll (skill fra punktet over — dette gjelder *nye* årsaker opphevelsen selv skaper, ikke allerede eksisterende tilleggsårsaker): `KontrollerteInntektperioderTjeneste.ryddPerioderFritattForKontrollEllerTilVurderingIBehandlingen` fritar første/siste måned i programperioden for kontroll. Når et tidligere opphør korter ned sluttdatoen slik at en allerede kontrollert måned blir ny (fritatt) siste måned, mister den sin kontrollert-status. Gjenåpner opphevelsen perioden igjen, er måneden ikke lenger siste måned og trenger kontroll på nytt — men den fremstår da som "aldri kontrollert". `VurderManglendeKontrollAvPeriode`/`ManglendeKontrollperioderTjeneste` (kjører etter *ethvert* vedtak, uavhengig av `VurderKompletthetSteg`/`UngEtterlysningOppretter`) oppdager dette og oppretter automatisk en **ny** revurdering med årsak `RE_KONTROLL_REGISTER_INNTEKT` for opphevOpphør-behandlingen. Med bryteren for kontrollbehandling på blir det en kontrollbehandling i stedet (se «Domain Notes: Kontrollbehandling»). Dette er forventet/riktig — ikke en bug — men kan overraske i saksbilder som viser en ekstra "Kontroll av inntekt"-behandling rett etter opphevelsen.
- Sammenslåing av hendelser på en åpen behandling: dersom opphevOpphør-hendelsen slås sammen med en fortsatt åpen behandling som venter på bekreftelse av det (nå opphevede) opphøret (jf. `OpprettRevurderingEllerOpprettDiffTask`), kan behandlingen ende opp med både `RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM` og `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM`. Da avbrytes eventuelle ventende `UTTALELSE_ENDRET_SLUTTDATO`- og `UTTALELSE_ENDRET_PERIODE`-etterlysninger (`ProgramperiodeendringEtterlysningTjeneste.avbrytVentendeSluttdatoEtterlysninger` — begge etterlysningstyper dekkes for å håndtere gammel og ny programperiode-flyt), og den utdaterte opphør-årsaken teller ikke som en reell tilleggsårsak. Etterlysninger om endret startdato berøres ikke. Samme toleranse for den utdaterte årsaken gjelder i `BehandlingDtoUtil.utledVisningsnavn` (returnerer fortsatt `UNGDOMSPROGRAM_OPPHØR_OPPHEVET`, ikke `FLERE_BEHANDLINGÅRSAKER`) — hold disse to stedene i sync ved videre endringer.
- Overstyring: `ProsessTriggerFilter` filtrerer bort `RE_VARSEL_OPPHOR_VED_MAKSDATO` når `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM` også finnes, på linje med forlenget periode og manuelt opphør.
- Periodevisning: `UtledStatusForPerioderPåBehandling.RELEVANTE_ÅRSAKER` inkluderer `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM` slik at behandlingen viser triggerperioden i sidepanelet (samme "diff-vindu"-mønster som forlenget periode, ikke hele programperioden fra startdato). `finnÅrsakerFraTriggereTidslinje` filtrerer i tillegg bort `RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM` når opphevelses-årsaken er til stede, slik at kun «Opphevelse av opphør» vises — jf. samme "stale cause"-konvensjon som brukes i `UngEtterlysningOppretter`, `BehandlingDtoUtil` og `ProgramPeriodeStrategy`.
- Brev: `DefaultDetaljertResultatTidslinjeUtleder` produserer kun en tynn grunnlagstidslinje (behandlingsårsaker + vilkår + tilkjent ytelse) og gjør ingen egen utledning av «opphevet vs. avbrutt» eller stale-opphør-undertrykking. Selve avstemmingen mot periodegrunnlaget ligger i frittstående brev-strategier som hver avstemmer sin resultattype mot om programperioden er lukket/åpen, og som kombineres av resolveren (`YtelseVedtaksbrevRegler`): `ProgramPeriodeStrategy` (start-/sluttdato/opphør), `ForlengetPeriodeStrategy`, `OpphørVedMaksdatoStrategy` og `OpphørOpphevetStrategy`. `ProgramPeriodeStrategy` regner en `ENDRING_SLUTTDATO` som stale (og hopper over opphørs-/flyttebrev) når programperioden fortsatt er åpen — dvs. gjenåpnet av en opphevelse på samme behandling — via `UngdomsprogramOpphørUtleder.harLukketSluttdato` på periodegrunnlaget. `ForlengetPeriodeStrategy`/`OpphørVedMaksdatoStrategy` gir kun brev når perioden er åpen (ellers har det skjedd en reell sluttdatoendring).
- Brev/kodeverk: `DokumentMalType.OPPHOR_OPPHEVET_DOK`, `TemplateType.OPPHOR_OPPHEVET`, mal `opphør_opphevet.hbs`, `OpphørOpphevetStrategy` (bestiller opphevelse-brevet)/`OpphørOpphevetInnholdBygger`/`OpphørOpphevetDto` (viser tidligere sluttdato og gjeldende maksdato), `BehandlingVisningsnavn.UNGDOMSPROGRAM_OPPHØR_OPPHEVET`, `ÅrsakTilVurdering.UNGDOMSPROGRAM_OPPHØR_OPPHEVET`.
- **Opphevelsesbrev kun når opphøret faktisk ble vedtatt**: Dersom opphevelseshendelsen slås sammen med en behandling som ennå venter på uttalelse fra deltaker om opphøret (autopunkt `AUTO_SATT_PÅ_VENT_REVURDERING`, jf. punktet over) og deltaker ikke rekker å svare før opphevelsen kommer inn, ble opphøret aldri reelt vedtatt/iverksatt — da finnes det ikke noe opphørsbrev å oppheve, og «Opphør opphevet»-brevet skal **ikke** sendes. `OpphørOpphevetStrategy` sjekker derfor om originalbehandlingen (siste vedtatte, via `behandling.getOriginalBehandlingId()` + `UngdomsprogramPeriodeRepository`) faktisk hadde en **lukket** sluttdato (ikke `TIDENES_ENDE`) før brevet sendes — ved å sjekke `UngdomsprogramOpphørUtleder.harLukketSluttdato` på originalbehandlingens periodegrunnlag. Er opphøret ikke iverksatt returneres et eksplisitt «ingen brev, `IKKE_RELEVANT`»-resultat (ikke tom liste, som ville gitt fallback `IKKE_IMPLEMENTERT` og manuell «Fatt vedtak»). Behandlingsårsaken `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM` forblir synlig som normalt (append-only) selv om brevet uteblir — sporing av hva som skjedde dekkes av historikkinnslag-mekanismen i `RegisterdataEndringshåndterer`/`RegisterinnhentingHistorikkinnslagTjeneste` (se eget punkt under om påkrevd årsak-registrering). Se `YtelseVedtaksbrevReglerTest` og `OpphørTest`.
- **`RegisterdataEndringshåndterer.REGISTEROPPLYSNING_BEHANDLINGÅRSAKER` må inkludere `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM`**: Historikkinnslaget "Behandlingen oppdatert med nye opplysninger" (med årsakstekst) opprettes av `RegisterdataEndringshåndterer.lagHistorikkinnslag` kun for årsaker i dette settet — manglet en årsak her, ble historikkinnslaget stille utelatt (ingen feilmelding), til tross for at behandlingsårsaken selv fortsatt var synlig i behandlingsårsaker-visningen. Dette var opprinnelig glemt for opphevelse-av-opphør-årsaken, slik at verken det generiske eller det spesifikke innslaget dukket opp for rene opphevelser. Rettet ved å legge årsaken til i settet; regresjonstest i `RegisterdataEndringshåndtererTest`. Sjekk dette settet ved fremtidige nye hendelsestyper som skal reposisjonere behandlingen.
- **Opphør og opphevelse i samme vs. ulik behandling**: Avgjørende skille er om opphøret ble vedtatt i en tidligere, avsluttet behandling (gir eget «Opphør opphevet»-vedtaksbrev), eller om opphør og opphevelse havnet på **samme, fortsatt åpne** behandling (ingen brev — det finnes ikke noe opphørsvedtak å reversere). På brev-siden utleder `OpphørOpphevetStrategy` selv på behandlingsårsaken, og avgjør brev vs. ingen brev ved å hente originalbehandlingens periodegrunnlag og sjekke `UngdomsprogramOpphørUtleder.harLukketSluttdato` (`behandlingslager`, dvs. sluttdato != `TIDENES_ENDE`). Tilsvarende skille finnes også i `BehandlingÅrsakType`-domenet for visning: `BehandlingDtoUtil.utledVisningsnavn` (velger `BehandlingVisningsnavn.UNGDOMSPROGRAM_OPPHØR_OPPHEVET` vs. `UNGDOMSPROGRAM_OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING`) og `UtledStatusForPerioderPåBehandling.mapTilÅrsakTilVurdering` (velger `ÅrsakTilVurdering.UNGDOMSPROGRAM_OPPHØR_OPPHEVET` vs. `UNGDOMSPROGRAM_OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING` i periodevisningen) bruker samme `UngdomsprogramOpphørUtleder`, kun for tilfeller der behandlingsårsaken er `RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM`. `ÅrsakTilVurdering.SAMMENHENG` (statisk 1:1-mapping `BehandlingÅrsakType`→`ÅrsakTilVurdering`) beholder default-mappingen til `UNGDOMSPROGRAM_OPPHØR_OPPHEVET`; overstyringen håndteres kontekstuelt i `mapTilÅrsakTilVurdering`, ikke i det statiske kartet.
- Brev-strategier for programperiode er delt opp og frittstående (jf. `VedtaksbrevInnholdbyggerStrategy` — «en strategi skal aldri inspisere andre strategiers resultater»): kombinasjoner (f.eks. forlengelse + opphevelse, eller opphør + opphevelse på samme behandling) håndteres av resolveren `YtelseVedtaksbrevRegler` (brev slår globalt ut ingen-brev; `IKKE_IMPLEMENTERT` kun når ingen strategi ga noe), ikke ved manuell aggregering i én strategi. Den delte «lukket/åpen sluttdato»-definisjonen ligger i `UngdomsprogramOpphørUtleder` (`finnLukketSluttdato`, `harLukketSluttdato`, `harÅpenSluttdato`) så den ikke drifter mellom strategiene. Utlederen gjør ingen repository-kall selv — kallestedet henter `UngdomsprogramPeriodeRepository.hentGrunnlag(...)` for behandlingen eller originalbehandlingen og sender inn grunnlaget.
- Ved endringer i opphevelse av opphør: hold sammen kontrakt (ung-deltakelse-opplyser), hendelsesutleder, etterlysningsunntak, prosesstrigger-overstyring, brev og periodevisning i samme leveranse.

## Domain Notes: Tilleggsstønader henter aktivitetspengeperioder
- Tilleggsstønader (`tilleggsstonader-integrasjoner`, namespace `tilleggsstonader`, gcp) henter innvilgede aktivitetspengeperioder med `POST /api/ekstern/tilleggsstonader/aktivitetspenger/perioder` (`TilleggsstonaderRestTjeneste`). Body: `{ident, fom, tom}` (fnr i body, ikke i url). Svar: `{perioder: [{fom, tom}]}`, tom liste ved ukjent person/ingen saker (ikke 404).
- `AktivitetspengerPerioderTjeneste` bruker gjeldende vedtak per fagsak (`finnSisteAvsluttedeIkkeHenlagteYtelsebehandling`) og samlet vilkårsutfall `OPPFYLT`, uavhengig av dagsats. Åpne behandlinger tas ikke med; opphør og revurderinger slår igjennom. Fagsaker på alle historiske aktørIder tas med (`AktørTjeneste.hentAlleAktørIderForPersonIdent`).
- Perioder som overlapper forespurt fom/tom returneres **uten å kuttes** (samme som EF), ellers får TS-oppfølgingen falske `TOM_ENDRET`.
- Tilgang: rent maskin-til-maskin via den generiske ressurstypen `EKSTERN_SYSTEM` (k9-felles) med påkrevd liste `@BeskyttetRessurs(eksterneSystemer = {...})` i formatet `cluster:namespace:app` (konstanter i `EksterneSystemer`). Beslutningen tas i k9-felles `PepImpl` uten kall til PDP: kun Azure client_credentials, systemet må både være pre-autorisert (Nais `accessPolicy.inbound`) **og** stå i listen på endepunktet. Saksbehandler-/TokenX-token avvises. Eksterne systemer skal ikke legges i `AppPdpRequestBuilderImpl.INTERNAL_CLUSTER_NAMESPACE` — da får de systemtilgang til hele API-et. `RestApiAbacTest` validerer annotasjonene, og `TilleggsstønaderTilgangTest` sikrer at listen stemmer med Nais-oppsettet. Systemkall auditlogges ikke i ung-sak; TS har ansvaret for tilgangskontroll per person og revisjonslogg. Logg aldri fnr/aktørId fra endepunktet.
- Nytt eksternt system: legg til konstant i `EksterneSystemer`, bruk `resource = EKSTERN_SYSTEM` + `eksterneSystemer` på endepunktet, og legg til `inbound`-regel i `deploy/<cluster>.yml`.
- Vedtakshendelsen (`ung-vedtakhendelse`) endres ikke for TS. Den planlagte `tilleggsstonader-oppfolging` vil bruke hendelsen som trigger og dette endepunktet for perioder.

## Domain Notes: Min side-mikrofrontend for aktivitetspenger
- Når en ny aktivitetspenger-fagsak opprettes, ber ung-sak ung-brukerdialog-api om å aktivere Min side-mikrofrontenden `aktivitetspenger-innsyn` for brukeren.
- Trigger: `AktivitetspengerMikrofrontendObserver` (`ytelse-aktivitetspenger`, pakke `minside`) lytter på `FagsakStatusEvent` og reagerer kun på `forrigeStatus == null` → `OPPRETTET` for `FagsakYtelseType.AKTIVITETSPENGER`. Fagsaker med `erIkkeDigitalBruker()` hoppes over — ikke-digitale brukere får ikke mikrofrontend foreløpig.
- Observeren oppretter `AktiverMikrofrontendBrukerdialogTask` (`brukerdialog.aktiver.mikrofrontend`), som kaller `UngBrukerdialogSakKlient.aktiverMikrofrontend` (`POST aktivitetspenger/mikrofrontend/aktiver`) med `AktiverMikrofrontendRequest`.
- ung-sak sender kun aktørId — aldri fnr. Logg kun saksnummer, aldri aktørId/fnr (ung-brukerdialog-api logger uten identifikatorer; callId kobler loggene).
- ung-brukerdialog-api eier statustabellen og Kafka-integrasjonen mot `min-side.aapen-microfrontend-v1` (mikrofrontend-id `aktivitetspenger-innsyn`, sensitivitet `high`). ung-sak har ingen Kafka-kobling mot Min side.
- Toggle `AKTIVITETSPENGER_MIKROFRONTEND_ENABLED` (default av). På i `dev-gcp` og prod; av i `app-vtp.properties` til en verdikjedetest finnes.
- Ingen automatisk deaktivering — deaktivering skjer kun manuelt via forvaltning i ung-brukerdialog-api (`POST /forvaltning/sak/mikrofrontend/aktivitetspenger/{aktiver,deaktiver}` med fnr og begrunnelse, DRIFT-tilgang; fnr slås opp til aktørId i PDL og lagres ikke). Samme endepunkt brukes til etterfylling av eksisterende fagsaker. Endret aktørId støttes ikke foreløpig.

## Domain Notes: Vedtaksbrev-resultat
- Brev-genereringen har ingen resultatklassifisering (tidligere `DetaljertResultatType`). `DefaultDetaljertResultatTidslinjeUtleder` (felles for begge ytelser) produserer kun en tynn grunnlagstidslinje av `DetaljertResultat` (behandlingsårsaker, avslåtte/ikke-vurderte vilkår, tilkjent ytelse, `vurderesIBehandlingen`), og hver brev-strategi utleder selv om den er relevant.
- Grunnlaget krysses (`CROSS_JOIN`) med hele vilkårstidslinjen, så perioder utenfor vurdering er også med — med tomt årsaks-sett og `tilVurdering=false`. Typen `DetaljertResultatTidslinje` gjør skillet eksplisitt: `tilVurdering()` er normalen, `heleBildet()` brukes **kun** av avslagsstrategiene (som må kunne skille fullt avslag fra delvis). `harÅrsak(...)` spør alltid periodene til vurdering.
- Innholdbyggerne får samme `DetaljertResultatTidslinje` som strategiene og velger selv om de trenger hele vilkårsbildet eller kun `tilVurdering()`.
- Resolveren `YtelseVedtaksbrevRegler` kombinerer strategiene i tre faser (`OVERSTYRENDE_INGEN_BREV` → `OVERSTYRENDE_ENKELTBREV` → `NORMAL`); brev slår ut ingen-brev, og `IKKE_IMPLEMENTERT` er sikkerhetsnettet som gir manuell «Fatt vedtak». En strategi skal aldri inspisere andre strategiers resultater.
- Felles utledninger som deles av flere ytelser/byggere ligger i `formidling`-modulen (`EndringInntektUtleder`), ikke duplisert per ytelse.

## Domain Notes: Kontrollbehandling
- Inntektskontrollen kan kjøres i en egen behandlingstype, `BehandlingType.KONTROLL` (kontrollbehandling, K), parallelt med ordinær behandling (O). Bryterne `KONTROLLBEHANDLING_UNG_ENABLED` og `KONTROLLBEHANDLING_AVP_ENABLED` (`KontrollbehandlingBryter`) er av som standard. Koden `BT-012` er foreløpig og må avtales med DVH før bryteren slås på, også i `dev-gcp`.
- K er ytelsesbehandling (`getYtelseBehandlingTyper()`) og får vedtak, oppdrag og vedtakshendelse. K er ikke ordinær (`getOrdinæreYtelseBehandlingTyper()`). Søknader, hendelser, G-regulering og forvaltning ser bare på O, blant annet via `BehandlingRepository.hentSisteOrdinæreYtelsesBehandlingForFagsakId`.
- En fagsak har maks én åpen O og én åpen K. Den delvis unike indeksen i `V1.0_107` stopper to åpne K-er. Indeksen inneholder koden for K og må lages på nytt hvis koden endres.
- `RevurderingTjeneste.opprettKontrollbehandling` lager K fra siste avsluttede, ikke-henlagte ytelsesbehandling, under fagsaklåsen. K får samme grunnlagskopi som en revurdering, inkludert vilkårsresultat. I UNG kopieres også sats og uttak, fordi K ikke beregner dem. K-modellen kjører `START_STEG`, `INNHENT_REGISTEROPP`, `VURDER_KOMPLETTHET` (eneste startpunkt), `KONTROLLER_REGISTER_INNTEKT`, `FORESLÅ_BEHANDLINGSRESULTAT`, porten og stegene fra `BEREGN_YTELSE` til `IVERKSETT_VEDTAK`. K henter bare IAY fra register og lager bare inntektsetterlysninger.
- `KontrollbehandlingRuting` avgjør hvor kontrollen går, og kalles under fagsaklåsen. Har fagsaken åpen K, går kontrollen dit, også med bryteren av. Er bryteren på og ingen åpen O kontrollerer inntekt, opprettes ny K. Ellers brukes O som før. Batchen, etterkontrollen og forvaltningen går via `OpprettRevurderingEllerOpprettDiffTask`, manuell opprettelse via `BehandlingsoppretterTjeneste`. Er K eller siste vedtak under iverksetting, utsettes tasken med `lagreKopi`-mønsteret i stedet for å feile.
- `MottattDokumentRuting` sender inntektsrapportering samme vei som kontrollen (`KontrollbehandlingRuting.skalInntektsrapporteringTilKontrollbehandling`). Svar på etterlysning går til K når K eier etterlysningen og ikke er avsluttet. Ellers går svaret til O, som sene svar gjør i dag. Søknader og andre dokumenter går alltid til O.
- Porten (`VurderParallelleBehandlingerSteg`, steg `VURDER_PARALLELLE_BEHANDLINGER`) står rett før `BEREGN_YTELSE`, alltid i K-modellene og i O-modellene bare med bryteren på. Steget tar fagsaklåsen og leser de andre behandlingene med native SQL (`ParallelleBehandlingerRepository`), så Hibernate-sesjonen ikke gir gammel tilstand. Bygger behandlingen ikke på siste vedtak, eller har en annen åpen, ikke-henlagt behandling passert `BEREGN_YTELSE`, settes den på vent med autopunkt 7050 (`AUTO_SATT_PÅ_VENT_PARALLELL_BEHANDLING`, `VENT_TIDLIGERE_BEHANDLING`, frist P1D). Autopunktet har `TILBAKE`, så porten kjøres på nytt etter all gjenopptakelse.
- Ny kode skal aldri sammenligne en O mot portens steg. Med bryteren av mangler steget i O-modellen, og `BehandlingModellImpl.indexOf` kaster exception. Bruk `ParallelleBehandlingerTjeneste.FØRSTE_STEG_ETTER_PORTEN` (`BEREGN_YTELSE`) som referanse, slik `VekkVentendeVedTilbakeføringObserver` gjør, eller sjekk `inneholderSteg` først.
- Når en ytelsesbehandling avsluttes, lager `OpprettSynkVedAvslutningObserver` en `SynkroniserÅpneBehandlingerTask` (gruppesekvens per fagsak). Tasken lages bare når fagsaken har andre åpne ytelsesbehandlinger, og når bryteren er på eller fagsaken har åpen K. `SynkroniserÅpneBehandlingerTjeneste` erstatter data på de åpne behandlingene etter eierskap:
  - Avsluttes O, får K personopplysninger, programperiode, startdatoer, vilkårsresultat, ytelsesgrunnlag (`YtelsesgrunnlagSynkroniserer` per ytelse) og uttalelser som ikke gjelder inntekt.
  - Avsluttes K, får O kontrollperioder og uttalelser av typen `ENDRET_INNTEKT`.
  - `originalBehandlingId` settes til behandlingen som ble avsluttet (`Behandling.oppdaterOriginalBehandling`). Snapshotet tas før kopieringen, og dagens diff og startpunktutledning avgjør tilbakehopp.
  - Er den avsluttede behandlingen henlagt, kopieres ingenting, og bare behandlinger som venter i porten, tas av vent.
  - Står en åpen behandling i eller etter `BEREGN_YTELSE`, eller har den vedtak, feiler synken. Har den uferdige tasker, lager synken en ny synk-task bak dem og endrer ingenting.
- K uten kontrollmåneder etter synk henlegges maskinelt av `HenleggKontrollbehandlingUtenMånederTask`, som ligger først i synk-gruppen. `HenleggBehandlingTjeneste` avbryter åpne etterlysninger når K henlegges, også ved manuell henleggelse, så deltakeren ikke sitter igjen med en oppgave i ung-brukerdialog-api. Henleggelse av O er uendret.
- Har fagsaken åpen K, eier K inntektsabonnementet (`InntektAbonnentTjeneste`). O lager ikke nytt abonnement, og avslutning av O avslutter det ikke. `HentInntektHendelserTask` frisker opp alle åpne ytelsesbehandlinger, så en K som venter, får sen a-inntekt.
- `BigQueryStatistikkRepository.parallellBehandlingVentStatistikk` teller behandlinger som venter i porten, med antall og eldste ventetid per behandlingstype. Over 7 dager logges warn med behandlingId-er. Selve varslingen settes opp utenfor repoet.
- Slå bryteren av bare når ingen K er åpen og ingen behandling står eller venter i porten. Har bryteren vært på i et miljø, ligger de nye kodene i databasen, og imaget kan ikke rulles tilbake til en versjon uten dem.
- Ikke implementert: kopi av rapportert inntekt (`OPPGITT_OPPTJENING` i k9-abakus) fra K til O. Tilpasningene i ung-sak-web, k9-los-api, DVH/statistikk og ung-tilbake er utsatt.

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
- Lokal server startes via `JettyDevServer` med working directory `web`.
  Se `.github/instructions/lokal-kjoring.instructions.md`.

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
- Verifiser kombinasjonsflyt der kontrollbehandling går parallelt med ordinær behandling (synk begge veier, porten og henleggelse av kontrollbehandling).

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
