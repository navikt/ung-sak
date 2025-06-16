# Arkitekturbeslutninger

## Gjenbruk fra k9
Det ble besluttet å gjenbruke arkitektur og kode fra k9-sak, saksbehandlingssytemet for ytelser i folketrygdloven kapittel 9 (k9). Grunnen til dette er:
1. K9-sak er et velfungerende system med en solid arkitektur som har vært i produksjon i flere år og som har vist seg å være robust og skalerbar
2. K9-sak har en arkitektur som er godt dokumentert og forstått av teamet
3. Ved å gjenbruke kode fra k9-sak kunne vi raskt komme i gang med utviklingen, noe som var viktig grunnet den korte fristen.
4. Det ble videre besluttet å kopiere koden fra k9-sak til en ny kodebase. Dette ble gjort fordi de funksjonelle kravene var så forskjellige at å dele kodebasen ville i stor grad påvirke kompleksiteten og forvaltbarheten til k9-sak. Siden ungdomsytelsen er et prøveprosjekt som muligens har begrenset levetid, ble det ansett som mer hensiktsmessig å ha en egen kodebase.

## Modularisering og mikrotjenester
1. K9-sak ble utviklet ved bruk av [Monolith First](https://martinfowler.com/bliki/MonolithFirst.html) for å gå opp grensegang, og deretter delt opp i mikrotjenester. Av delene som ble delt ut ble noen av disse delene tatt inn igjen i monolitten (ung-sak) og noen står fortsatt utenfor. Dette er delene slik de ser ut i dag:
    1. Mottak av dokumenter (modul i ung-sak)
    1. Mottak av eksterne hendelser (del av ung-sak)
    1. Abakus: Registeropplysninger for inntekt-arbeid-ytelse (Mikrotjeneste utenfor ung-sak)
    1. Formidling og Dokgen: Aggreging av data og produksjon av brev til bruker (modul i ung-sak)
    1. Registrering av programperioder fra veileder og oppgaver til bruker (ny funksjonalitet, mikrotjeneste utenfor ung-sak)
1. Architecture Refactoring - for fornyelse og forbedring av grensegang mellom moduler og datamodeller
1. Evergreen - kontinuerlig oppdatering av teknologistack og avhengigheter (biblioteker, container OS, rammeverk)

## Behandling og prosesskontroll
1. Adaptiv saksbehandling (~ koordinerende og orkestrerende prosesskontroll)
    - Hendelser oppstår utenfor kontroll av saksbehandlingssytemet (eks. ny inntektsmelding, flytting, dødsfall, innringing, nye opplysninger i registeret etc)
    - Behandlingskontroll sjekker hva som endres av datagrunnlag hver gang en behandling kjøres og tilpasser behandlingsprosess (eks. tilbakehopp i saksbehandling)
1. Hovedflyt i saksbehandling går gjennom steg som vurderer datagrunnlag og produserer aggregater av data som benyttes i videre prosessering

## Revurdering
1. Konseptuelt likt andre behandlinger, men tar utgangspunkt at det finnes minst ett tidligere behandling/vedtak. Gir et enhetlig konsept for saksbehandling uavhengig av trigger (hendelse, søknad, inntektsopplysninger, innsendt dokumntasjon, klage, g-regulering, manuelt igangsatt revurdering, håndtering av feil på tidligere saker, etterkontroll) og hva som er endret i datagrunnlag (oppgitte opplysninger, innhentende opplysninger, endring av vurderinger), kjente feil eller endring i regler.
1. Endringer og forlengelser også likt revurderinger (sjekk for endringer i datagrunnlag skjer automatisk)
1. Hendelser (eks. dødsfall) håndteres likt.
1. Sjekker hva som er endret før vurderer saksbehandlingsløpet (eks. registergrunnlag, inntektsopplysninger, dokumentasjon/søknad/underlag fra bruker)

## Automatisering
1. All prosesskjøring er automatisert, med støtte fra saksbehandler ved behov (eks. skjønn, avklaring, vurderinger).  Medfører at saksbehandler får automatisk støtte i vurderinger også ved manuell saksbehandlin (eks. innhenting og vurdering av data)
1. Inkrementell automatisering - behandlingssteg kjøres inkrementelt med lagring imellom.
1. Automatisering kjøres vha. tasks håndtert i database (redusert behov for infrastruktur, enkelt skaleringskonsept og robusthet, uavhengig koordinert kjøring og prioritering per sak/behandling)
1. Feil i en task påvirker ikke andre behandlinger/saker (unngår stoppe opp og reprosessere køer)


## Informasjonsarkitektur
1. Fagsak og behandlinger modellert for sakskomplekset
    -  Egenskaper:  Søker, Varighet, Type, Ytelsessubjekt (person som utløser behov for ytelse). Relasjon til andre søkere
1. Behandlingslager
    - All info på behandling lagres i Behandlingslager for enkel tilgang til alle opplysninger i sak
1. Vedtak
    - Vedtak adskilt (*\*tbd*), benyttes som juridisk dokument for arkivering
1. Bruk av Aggregater i modellering (Domain Driven Design)
    - Alle endring på aggregater lagres atomisk i relasjonsstruktur, og spores i helhetlig versjon (så tidligere versjoner av aggregater kan gjenopprettes)
        - Eksempler:  Medlemskap, Personopplysninger, Opptjening, Beregningsgrunnlag, InntektArbeidYtelse Grunnlag, Tilsyn, Sykdomsdokumentasjon

## Database og lagring
1. Benytter Relasjonsdatabase (Postgresql) og normalisert datalagring
    - gjør det enklere å migrere datamodell framfor spoling/support for gamle lesemønstre
1. Benytter Expand/Contract pattern med flyway for alle endringer i database
1. Benytter Hibernate ORM rammeverk for modellering av datastrukturer
1. Anti-corruption layer mapper hibernate entiteter til eksterne representasjoner (annen modell - eks. InntektArbeidYtelseGrunnlag og Beregningsgrunnlag) for å støtte separat evolusjon

# Integrasjon
1. Asynk kommunikasjon mellom eksterne domener (når tilgjengelige), samt interne sub-domener med lav avhengighet (1-veis) og som er stateful, eller sub-domener der et er betydelig prosesseringstid (eksempel: abakus innhenting)
1. System-til-system kall gjøres idempotent og asynk internt gjennom ProsessTask biblioteket (håndtere retry, atLeastOnce, idempotente kall synkront)
1. REST over HTTP med JSON foretrukket for skjemaer

# Frontend
1. REST HATEOAS benyttes for å dele opp informasjonsbehovet og kunne presisere hva som er tilgjengelig i en gitt behandling, uten å hardkode alle endepunkt.  Gir lokasjonsuavhengighet (kan flytte en REST tjeneste til annen mikrotjeneste)
1. REST HATEOAS benyttes for å tilby standardist fagsak/behandling grensesnitt fra 3 ulike saksbehandlingsmotorer i en felles grenseflate (Vedtak, Klage, Tilbakekreving)
1. Single Page Application (SPA) benyttes for å gi en høy-interakti saksbehandlingsflate med adskilte funksjonelle domener
1. NPM pakker og Lerna.js benyttes for å bygge mikrofrontend fra ulike repositories og funksjonelle komponenter/ flater og separate avhengigheter. (Tidligere også separate Backend-for-frontends - men ga mer kompleksitet enn verdi, så er på vei ut).


# Etterlevelse
1. Regelmodell for å definere juridiske regler.  Gjør det mulig å lagre og spore input/gjennomføring og utfall av regler i et graph objekt (json struktur).  Gjør det lett å etterprøve hvorvidt regel er juridisk anvendt riktig
1. Regelmodell er byg som eget bibliotek basert på Expression Trees / Specification Pattern (fp-nare).  Forenkler tilpasninger av API, og minimerer behov for teknologikompetanse på tvers av andre lisensbaserte systemer
1. Audit logging
1. Informasjon lagret i aggregater for sporing og struktur
1. VedtakXML (*\*tbd*)

## Sikkerhet
1. Autentisering (OpenID Connect og JWKS for innloggede brukere og systembrukere)
1. Autorisasjon (ABAC)
    - Sjekk for tilgang på rolle (RBAC)
    - Sjekk for tilgang Kode6/7/19
    - Sjekk for tilgang EgenAnsatt
    - Sjekk for to-trinnskontroll (Saksbehandler vs. Beslutter)
    - Sjekk for veileder (read-only)
    - Sjekk for driftspersonell (tekniske grensesnitt)
1. Secrets Management
    - Hashicorp Vault
1. Audit
    - Audit av drift og diagnosetjenester
    - Audit av sikkerhetsoperasjoner (innlogging, ..)

## Produksjonsmiljø
1. K8S på NAIS
1. On-prem primært p.t. pga sensitive persondata i Saksbehandling.  Avventer modenhet og grunnlag for å kunne flytte til GCP
1. Auditlog og Securelogs
    - Hendelser
    - Prosesssteg
    - Tjenestekall
    - Diagnosetjenester (eks. diagnose av saker)
1. Grafana, Prometheus, Sensu
1. Kibana for logging
1. Rollforward og auto-scaling
1. Feature toggles (benytter deployment/env flag da ikke behov for eget system)


## DevSecOps
1. [NAV Security Playbook](https://sikkerhet.nav.no/docs/)
1. Kontinuerlige leveranser
    1. med Pull request for code review
    1. med dependabot for avh. scanning, Snyk optional
    1. SonarQube/SonarCloud (tidligere) for kodekvalitet og OWASP
    1. Autotest
        - med VTP (Virtuell Tjeneste Plattform) for sjekk av tjenester end-2-end
        - med VTP for generering av syntetiske personer on-the-fly
        - med scan av logger for lekkasje av sensitive data
