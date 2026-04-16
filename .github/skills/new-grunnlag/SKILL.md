---
name: new-grunnlag
description: Opprett nytt grunnlag (persistent domenedata knyttet til behandling) i ung-sak. USE FOR: opprette nye grunnlagstabeller, entiteter, repository, ORM-registrering, Flyway-migrasjon, kopiering ved revurdering, og mottak fra søknad eller eksterne kilder. DO NOT USE FOR: vilkår uten grunnlagsbehov (bruk new-vilkaar), aksjonspunkt (bruk new-aksjonspunkt), formidling/brev, frontend.
---

# Nytt grunnlag for Aktivitetspenger

Denne skillen beskriver mønsteret for å opprette et nytt **grunnlag** — persistent domenedata knyttet til en behandling.

Grunnlag brukes når et vilkår eller et steg trenger strukturert data som skal:
- Lagres i databasen (ikke bare leses fra dokument)
- Akkumuleres over flere søknader/dokumenter
- Kopieres ved revurdering
- Være sporbar per journalpost/kilde

**Grunnlag er valgfritt ved vilkårsopprettelse.** Ikke alle vilkår trenger eget grunnlag — noen vilkår vurderes basert på registerdata eller eksisterende grunnlag. Spør brukeren om grunnlag trengs.

## Når trenger man grunnlag?

| Situasjon | Trenger grunnlag? |
|-----------|-------------------|
| Data fra søknad som skal lagres strukturert | Ja |
| Data fra ekstern kilde (register, melding) | Ja |
| Vilkår som kun bruker eksisterende data (personopplysninger, IAY) | Nei |
| Vilkår som kun bruker søknadsdokument direkte | Vurder — grunnlag gir sporbarhet og testbarhet |

## Arbeidsflyt

**Steg 0 — Samle inn detaljer fra bruker**

Før du skriver kode, bruk `vscode_askQuestions` for å stille følgende spørsmål. Ikke anta verdier — vent på svar.

```
Spørsmål å stille (bruk vscode_askQuestions):

1. header: "Grunnlagsnavn"
   question: "Hva skal grunnlaget hete? (f.eks. 'OppgittForutgåendeMedlemskap', 'Inntektsopplysninger')"

2. header: "Datakilde"
   question: "Hvor kommer dataene fra?"
   options: ["Søknad", "Eksternt register", "Annet"]

3. header: "Akkumulering"
   question: "Kan det komme flere innsendinger med data som skal akkumuleres (f.eks. flere søknader)?"
   options: ["Ja — data akkumuleres per journalpost", "Nei — kun én kilde per behandling"]

4. header: "Datastruktur"
   question: "Beskriv dataene som skal lagres (f.eks. 'periode + liste av bosteder med landkode', 'beløp + periode')"

5. header: "Vilkårtilknytning"
   question: "Er grunnlaget knyttet til et vilkår?"
   options: ["Ja — bruk new-vilkaar-skillen etterpå", "Nei — brukes av steg/tjeneste direkte"]

6. header: "Revurdering"
   question: "Skal grunnlaget kopieres ved revurdering?"
   options: ["Ja (standard)", "Nei"]
```

Bruk svarene til å fylle inn konkrete verdier i alle steg under. Ikke bruk placeholder-navn.

## Arkitekturmønster

Grunnlag i ung-sak følger et **4-lags mønster** når data akkumuleres, eller et **2-lags mønster** for enklere tilfeller.

### 4-lags mønster (akkumulering fra flere kilder)

```
Grunnlag (GR_*) — per behandling, aktiv-flagg, versjon
  └─ Holder (*_HOLDER) — aggregator, kan deles mellom behandlinger
       └─ Periode/Data (*) — immutable, per kilde (journalpostId, mottattTidspunkt)
            └─ Detalj (*_DETALJ) — immutable verdiobjekter
```

Bruk dette når:
- Flere søknader/dokumenter kan sende inn data
- Hver innsending identifiseres med journalpostId
- Data skal akkumuleres (ny holder med alle eksisterende + nye data)

**Referanseimplementasjon:** Oppgitt forutgående medlemskap (denne PR-en):
- `GR_OPPGITT_FMEDLEMSKAP` → `OPPGITT_FMEDLEMSKAP_HOLDER` → `OPPGITT_FMEDLEMSKAP` → `OPPGITT_FMEDLEMSKAP_BOSTED`

### 2-lags mønster (enkel grunnlagsdata)

```
Grunnlag (GR_*) — per behandling, aktiv-flagg, versjon
  └─ Data (*) — innholdet som lagres
```

Bruk dette når:
- Kun én kilde per behandling
- Ingen akkumulering nødvendig

**Referanseimplementasjon:** `SøknadGrunnlagEntitet` → `SøknadEntitet`

## Steg-for-steg sjekkliste

### 1. Flyway-migrasjon

Fil: `migreringer/src/main/resources/db/postgres/defaultDS/1.0/V1.0_NNN__mitt_grunnlag.sql`

Finn neste ledige versjonsnummer. Følg databaseinstruksjonene i `.github/instructions/database.instructions.md`.

#### Sekvenser

Én sekvens per tabell. Bruk `increment by 50` og `minvalue 1000000`:

```sql
create sequence seq_mitt_grunnlag_holder increment by 50 minvalue 1000000;
create sequence seq_mitt_grunnlag increment by 50 minvalue 1000000;
create sequence seq_gr_mitt_grunnlag increment by 50 minvalue 1000000;
```

#### Tabeller (4-lags eksempel)

**Holder-tabell** — aggregator:
```sql
create table mitt_grunnlag_holder
(
    id            bigint                                 not null primary key,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table mitt_grunnlag_holder is 'Kort beskrivelse av holderens formål.';
```

**Data-tabell** — immutable per kilde:
```sql
create table mitt_grunnlag_data
(
    id                      bigint                                           not null primary key,
    mitt_grunnlag_holder_id bigint references mitt_grunnlag_holder (id)      not null,
    journalpost_id          varchar(20)                                      not null,
    periode                 daterange                                        not null,
    -- domene-spesifikke kolonner her
    opprettet_av            varchar(20)  default 'VL'                        not null,
    opprettet_tid           timestamp(3) default current_timestamp           not null,
    endret_av               varchar(20),
    endret_tid              timestamp(3)
);

comment on table mitt_grunnlag_data is 'Kort beskrivelse.';

create index idx_mitt_grunnlag_data_holder on mitt_grunnlag_data (mitt_grunnlag_holder_id);
```

**Merk:** `mottatt_tidspunkt` lagres **ikke** i data-tabellen. Bruk `MottatteDokumentRepository.hentMottatteDokument(fagsakId, journalpostIder)` for å hente mottatt tidspunkt basert på journalpostId. Dette unngår duplisering av data som allerede finnes i mottatt-dokument-tabellen.
```

**Grunnlag-tabell** — knytter behandling til holder:
```sql
create table gr_mitt_grunnlag
(
    id                      bigint                                           not null primary key,
    behandling_id           bigint references behandling (id)                not null,
    mitt_grunnlag_holder_id bigint references mitt_grunnlag_holder (id)      not null,
    aktiv                   boolean      default true                        not null,
    versjon                 bigint       default 0                           not null,
    opprettet_av            varchar(20)  default 'VL'                        not null,
    opprettet_tid           timestamp(3) default current_timestamp           not null,
    endret_av               varchar(20),
    endret_tid              timestamp(3)
);

comment on table gr_mitt_grunnlag is 'Grunnlag som knytter en behandling til en holder. Kun én aktiv rad per behandling.';

create index idx_gr_mitt_grunnlag_behandling on gr_mitt_grunnlag (behandling_id);
create unique index uidx_gr_mitt_grunnlag_aktiv on gr_mitt_grunnlag (behandling_id) where (aktiv = true);
```

**Viktige regler for grunnlagstabeller:**
- `gr_`-prefix for grunnlagstabellen (kobler behandling til data)
- `aktiv`-kolonne med unik partiell indeks `where (aktiv = true)` — sikrer kun én aktiv rad per behandling
- `versjon`-kolonne for optimistisk låsing
- `behandling_id` med foreign key til `behandling`
- Alle kolonnenavn, tabellnavn og SQL-nøkkelord i lowercase
- `comment on table` etter hver tabellopprettelse
- Indeks på alle foreign keys

### 2. Entiteter

Alle entiteter plasseres i `behandlingslager/domene/src/main/java/no/nav/ung/sak/behandlingslager/behandling/<domene>/`.

#### 2a. Grunnlag-entitet

Mønster:
```java
@Entity(name = "MittGrunnlag")
@Table(name = "GR_MITT_GRUNNLAG")
public class MittGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_MITT_GRUNNLAG")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "mitt_grunnlag_holder_id", nullable = false, updatable = false)
    private MittGrunnlagHolder holder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public MittGrunnlag() { }

    public MittGrunnlag(Long behandlingId, MittGrunnlagHolder holder) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(holder, "holder");
        this.behandlingId = behandlingId;
        this.holder = holder;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    // getters, equals/hashCode basert på holder
}
```

**Faste felter:** `behandlingId`, `aktiv`, `versjon`, `holder`-referanse. Grunnlaget eier ingen domenedata direkte — det delegerer til holder.

#### 2b. Holder-entitet (4-lags)

```java
@Entity(name = "MittGrunnlagHolder")
@Table(name = "MITT_GRUNNLAG_HOLDER")
public class MittGrunnlagHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MITT_GRUNNLAG_HOLDER")
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "mitt_grunnlag_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MittGrunnlagData> data = new LinkedHashSet<>();

    public MittGrunnlagHolder() { }

    // Copy constructor — kopierer alle barn
    MittGrunnlagHolder(MittGrunnlagHolder other) {
        this.data = other.data.stream()
            .map(MittGrunnlagData::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<MittGrunnlagData> getData() {
        return Collections.unmodifiableSet(data);
    }

    void leggTilData(MittGrunnlagData d) {
        this.data.add(d);
    }
}
```

**Viktig:** Holder er **ikke** `@Immutable` fordi den må bygges opp med `leggTilData()` før persistering. Men etter persistering behandles den som immutable — nye data gir ny holder.

#### 2c. Data-entitet (immutable)

```java
@Immutable
@Entity(name = "MittGrunnlagData")
@Table(name = "MITT_GRUNNLAG_DATA")
public class MittGrunnlagData extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MITT_GRUNNLAG")
    private Long id;

    @Column(name = "journalpost_id", nullable = false, updatable = false)
    private String journalpostId;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    // domene-spesifikke felter og barn her

    public MittGrunnlagData() { }

    // Copy constructor for holder-kopiering
    MittGrunnlagData(MittGrunnlagData other) {
        this.journalpostId = other.journalpostId;
        this.periode = other.periode;
        // kopier barn
    }

    // getters, equals/hashCode
}
```

**Viktig for perioder:** Bruk `Range<LocalDate>` med `@Type(PostgreSQLRangeType.class)` og `columnDefinition = "daterange"`. Eksponer som `DatoIntervallEntitet` via getter.

#### 2d. Detalj-entitet (immutable, valgfritt)

For barn av data-entiteten, bruk `@Immutable` og copy constructor.

**Referansefil:** `behandlingslager/domene/src/main/java/no/nav/ung/sak/behandlingslager/behandling/medlemskap/OppgittBosted.java`

### 3. ORM-registrering

Opprett fil: `behandlingslager/domene/src/main/resources/META-INF/pu-default.mittgrunnlag.orm.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence/orm https://jakarta.ee/xml/ns/persistence/orm/orm_3_2.xsd"
                 version="3.2">

    <sequence-generator name="SEQ_MITT_GRUNNLAG_HOLDER" allocation-size="50" sequence-name="SEQ_MITT_GRUNNLAG_HOLDER"/>
    <sequence-generator name="SEQ_MITT_GRUNNLAG" allocation-size="50" sequence-name="SEQ_MITT_GRUNNLAG"/>
    <sequence-generator name="SEQ_GR_MITT_GRUNNLAG" allocation-size="50" sequence-name="SEQ_GR_MITT_GRUNNLAG"/>

    <entity class="no.nav.ung.sak.behandlingslager.behandling.domene.MittGrunnlagHolder"/>
    <entity class="no.nav.ung.sak.behandlingslager.behandling.domene.MittGrunnlagData"/>
    <entity class="no.nav.ung.sak.behandlingslager.behandling.domene.MittGrunnlag"/>

</entity-mappings>
```

Filen oppdages automatisk av `VLPersistenceUnitProvider` — ingen registrering i `persistence.xml` nødvendig.

**Navnekonvensjon:** `pu-default.<grunnlagsnavn>.orm.xml` (lowercase, uten æøå).

### 4. Repository

Plasser i samme pakke som entitetene.

```java
@Dependent
public class MittGrunnlagRepository {

    private final EntityManager entityManager;

    @Inject
    public MittGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public MittGrunnlag hentGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g FROM MittGrunnlag g WHERE g.behandlingId = :behandlingId AND g.aktiv = true",
            MittGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentEksaktResultat(query);
    }

    public Optional<MittGrunnlag> hentGrunnlagHvisEksisterer(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g FROM MittGrunnlag g WHERE g.behandlingId = :behandlingId AND g.aktiv = true",
            MittGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /** Akkumulerer: kopierer eksisterende data + legger til ny. */
    public void leggTilData(Long behandlingId, /* domeneparametere */) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId);

        MittGrunnlagHolder nyHolder = eksisterende
            .map(it -> new MittGrunnlagHolder(it.getHolder()))
            .orElseGet(MittGrunnlagHolder::new);
        nyHolder.leggTilData(new MittGrunnlagData(/* ... */));

        var nyttGrunnlag = new MittGrunnlag(behandlingId, nyHolder);
        persister(eksisterende, nyttGrunnlag);
    }

    /** Kopierer grunnlag ved revurdering — deler samme holder. */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        hentGrunnlagHvisEksisterer(gammelBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = new MittGrunnlag(nyBehandlingId, eksisterende.getHolder());
            persister(Optional.empty(), nyttGrunnlag);
        });
    }

    private void persister(Optional<MittGrunnlag> eksisterende, MittGrunnlag nytt) {
        eksisterende.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nytt);
        entityManager.flush();
    }

    private void deaktiverEksisterende(MittGrunnlag gr) {
        gr.deaktiver();
        entityManager.persist(gr);
        entityManager.flush();
    }
}
```

**Nøkkelmønstre:**
- `leggTilData()` — copy-on-write: kopierer eksisterende holder + legger til ny data → nytt grunnlag med ny holder
- `kopierGrunnlagFraEksisterendeBehandling()` — deler **samme holder** (ingen kopiering av data)
- `deaktiverEksisterende()` — soft delete av gammel grunnlagsrad, ny rad med `aktiv=true`
- Bruk `@Dependent` scope (ikke `@ApplicationScoped`) for repositories med `EntityManager`

### 5. Kopiering ved revurdering

Fil: `ytelse-aktivitetspenger/src/main/java/no/nav/ung/ytelse/aktivitetspenger/revurdering/GrunnlagKopiererAktivitetspenger.java`

Legg til det nye repositoryet og kall `kopierGrunnlagFraEksisterendeBehandling`:

```java
@Inject
public GrunnlagKopiererAktivitetspenger(/* eksisterende parametere */,
                                        MittGrunnlagRepository mittGrunnlagRepository) {
    // ...
    this.mittGrunnlagRepository = mittGrunnlagRepository;
}

@Override
public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
    // ... eksisterende kall ...
    mittGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
}
```

### 6. Mottak/persistering av data

Avhengig av datakilde:

**Fra søknad:** Utvid persisterer i `ytelse-aktivitetspenger/src/main/java/no/nav/ung/ytelse/aktivitetspenger/mottak/AktivitetspengerSøknadPersisterer.java`

```java
public void lagreMittGrunnlag(/* søknadsdata */, JournalpostId journalpostId, Long behandlingId) {
    // Map søknadsdata til domene-entiteter
    mittGrunnlagRepository.leggTilData(behandlingId, journalpostId, /* ... */);
}
```

Kall metoden fra `AktivitetspengerSøknadDokumentMottaker` som sender med `dokument.getJournalpostId()`.

**Merk:** `mottattTidspunkt` lagres ikke i grunnlaget — det hentes fra `MottatteDokumentRepository` ved behov (f.eks. i steget for å finne nyeste journalpost).

**Fra eksternt register:** Lag en egen tjeneste som henter data og kaller repository.

### 7. Test

Opprett test i `behandlingslager/domene/src/test/java/` i samme pakkestruktur.

```java
@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class MittGrunnlagRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private MittGrunnlagRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MittGrunnlagRepository(entityManager);
    }
}
```

**Testcaser som bør dekkes:**
- Lagre og hente grunnlag
- Akkumulering: ny innsending kopierer eksisterende + legger til
- Kopiering ved revurdering: ny behandling deler holder
- Ny innsending etter kopiering gir ny holder (copy-on-write)
- Henting uten grunnlag returnerer `Optional.empty()`

#### Bruk eller utvid TestScenarioBuilder

Bruk `AktivitetspengerTestScenarioBuilder` for å bygge testscenarioer. Hvis testen trenger data builderen ikke støtter ennå (f.eks. mottatt dokument med journalpostId, nye grunnlagstyper), **utvid builderen** med nye metoder i stedet for å bygge testdata manuelt.

```java
var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
    .leggTilVilkår(VilkårType.MITT_VILKÅR, Utfall.IKKE_VURDERT, vilkårPeriode)
    .lagre(entityManager);
```

**Referanse:** `behandlingslager/domene/src/test/java/no/nav/ung/sak/behandlingslager/behandling/medlemskap/OppgittForutgåendeMedlemskapRepositoryTest.java`

## Sjekkliste før ferdigstilling

- [ ] Flyway-migrasjon med tabeller, sekvenser, indekser, `comment on table`
- [ ] Entiteter: Grunnlag, Holder, Data (og evt. Detalj)
- [ ] ORM XML med sequence-generators og entity-registreringer
- [ ] Repository med hent, leggTil (akkumulering), kopier
- [ ] Kopiering registrert i `GrunnlagKopiererAktivitetspenger`
- [ ] Mottak/persistering fra kilde (søknad/register)
- [ ] Tester for repository
- [ ] Kompilering OK: `mvn -pl behandlingslager/domene -am -T1C -B compile`
- [ ] Tester OK: `mvn -pl behandlingslager/domene -am -T1C -B test`

## Referansefiler

| Fil | Formål |
|-----|--------|
| `behandlingslager/.../medlemskap/OppgittForutgåendeMedlemskapGrunnlag.java` | Grunnlag-entitet (4-lags) |
| `behandlingslager/.../medlemskap/OppgittForutgåendeMedlemskapHolder.java` | Holder-entitet |
| `behandlingslager/.../medlemskap/OppgittForutgåendeMedlemskapPeriode.java` | Immutable data-entitet |
| `behandlingslager/.../medlemskap/OppgittBosted.java` | Immutable detalj-entitet |
| `behandlingslager/.../medlemskap/OppgittForutgåendeMedlemskapRepository.java` | Repository med akkumulering |
| `behandlingslager/.../medlemskap/README.md` | Dokumentasjon av datamodell |
| `META-INF/pu-default.oppgittforutgaaendemedlemskap.orm.xml` | ORM-registrering |
| `migreringer/.../V1.0_080__oppgitt_forutgaaende_medlemskap_grunnlag.sql` | Flyway-migrasjon |
| `behandlingslager/.../søknad/SøknadGrunnlagEntitet.java` | Enkel grunnlag-entitet (2-lags) |
| `behandlingslager/.../personopplysning/PersonopplysningGrunnlagEntitet.java` | Grunnlag med flere relasjoner |
| `.../revurdering/GrunnlagKopiererAktivitetspenger.java` | Kopiering ved revurdering |
| `.../mottak/AktivitetspengerSøknadPersisterer.java` | Mottak fra søknad |

## Utenfor scope

Denne skillen dekker **ikke**:
- Vilkår (VilkårType, Avslagsårsak, vilkårssteg) — bruk `new-vilkaar`-skillen
- Aksjonspunkt, steg-registrering, oppdaterer — bruk `new-aksjonspunkt`-skillen
- Formidling/brev
- Frontend-endringer
- Ungdomsprogramytelsen (kan ha andre mønstre)
