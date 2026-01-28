# Brukerdialog Oppgave - Modulstruktur

## Oversikt

Brukerdialog-oppgave er nå delt i **to separate moduler** for å oppnå bedre separasjon av ansvar:

### 1. **brukerdialog-oppgave-api** (Ny modul)
- **Formål:** API/interface for oppgaveopprettelse og statushåndtering
- **Innhold:** 
  - `BrukerdialogOppgaveService` interface
- **Brukes av:** 
  - `etterlysning` modul (kun tilgang til interface-metoder)
  - Andre moduler som trenger å opprette/administrere oppgaver
- **Avhengigheter:**
  - `ung-deltakelseopplyser:kontrakt` (for DTOer)

### 2. **brukerdialog-oppgave** (Eksisterende modul)
- **Formål:** Implementering og utvidede tjenester
- **Innhold:**
  - `BrukerdialogOppgaveTjeneste` (implementerer `BrukerdialogOppgaveService`)
  - `BrukerdialogOppgaveRepository`
  - `BrukerdialogOppgaveMapper`
  - Entitetsklasser (`BrukerdialogOppgaveEntitet`, `BrukerdialogVarselEntitet`, etc.)
  - Støtteklasser (`OppgaveStatus`, `OppgaveType`, `OppgaveData`, etc.)
- **Brukes av:**
  - `web` modul (REST-tjenester)
- **Avhengigheter:**
  - `brukerdialog-oppgave-api`
  - `felles`
  - `kontrakt`
  - `ung-deltakelseopplyser:kontrakt`

## Avhengighetsdiagram

```
┌─────────────────────────────────────┐
│   etterlysning-modul                │
│                                     │
│  - UngOppgaveKlient                 │
│    implements                       │
│    BrukerdialogOppgaveService       │
└──────────────┬──────────────────────┘
               │
               │ depends on (kun interface)
               │
               ▼
┌─────────────────────────────────────┐
│   brukerdialog-oppgave-api          │
│                                     │
│  - BrukerdialogOppgaveService       │
│    (interface)                      │
└──────────────┬──────────────────────┘
               │
               │ depends on
               │
               ▼
┌─────────────────────────────────────┐
│   brukerdialog-oppgave              │
│                                     │
│  - BrukerdialogOppgaveTjeneste      │
│    implements                       │
│    BrukerdialogOppgaveService       │
│  - Repository, Mapper, Entiteter    │
│  + Ekstra metoder:                  │
│    - hentAlleOppgaverForAktør()     │
│    - hentAlleVarslerForAktør()      │
│    - lukkOppgave()                  │
│    - åpneOppgave()                  │
│    - løsOppgave()                   │
└──────────────┬──────────────────────┘
               │
               │ used by
               │
               ▼
┌─────────────────────────────────────┐
│   web-modul                         │
│                                     │
│  - BrukerdialogOppgaveRestTjeneste  │
│  - Bruker alle metoder              │
└─────────────────────────────────────┘
```

## Fordeler med denne strukturen

### ✅ Separasjon av ansvar
- **etterlysning-modulen** har kun tilgang til interface-metodene fra `BrukerdialogOppgaveService`
- **web-modulen** har full tilgang til alle metoder i `BrukerdialogOppgaveTjeneste`

### ✅ Tydelige grenser
- Interface-metoder: Opprettelse og statushåndtering (10 metoder)
- Implementeringsmetoder: Henting og visning av oppgaver (5 metoder)

### ✅ Testbarhet
- Lettere å mocke interface i etterlysning-tester
- Ingen utilsiktet bruk av interne metoder

### ✅ Fleksibilitet
- Kan bytte implementering uten å påvirke etterlysning
- API er stabilt og uavhengig av implementeringsdetaljer

## Interface-metoder (brukerdialog-oppgave-api)

Fra `BrukerdialogOppgaveService`:
1. `opprettKontrollerRegisterInntektOppgave(RegisterInntektOppgaveDTO)`
2. `opprettInntektrapporteringOppgave(InntektsrapporteringOppgaveDTO)`
3. `opprettEndretStartdatoOppgave(EndretStartdatoOppgaveDTO)`
4. `opprettEndretSluttdatoOppgave(EndretSluttdatoOppgaveDTO)`
5. `opprettEndretPeriodeOppgave(EndretPeriodeOppgaveDTO)`
6. `avbrytOppgave(UUID eksternRef)`
7. `oppgaveUtløpt(UUID eksternRef)`
8. `settOppgaveTilUtløpt(EndreStatusDTO)`
9. `settOppgaveTilAvbrutt(EndreStatusDTO)`
10. `løsSøkYtelseOppgave(DeltakerDTO)`

## Implementeringsmetoder (brukerdialog-oppgave)

Ekstra metoder i `BrukerdialogOppgaveTjeneste` (kun tilgjengelig for web):
1. `hentAlleOppgaverForAktør(AktørId)` → `List<BrukerdialogOppgaveDto>`
2. `hentAlleVarslerForAktør(AktørId)` → `List<BrukerdialogOppgaveDto>`
3. `hentAlleSøknaderForAktør(AktørId)` → `List<BrukerdialogOppgaveDto>`
4. `hentOppgaveForOppgavereferanse(UUID)` → `BrukerdialogOppgaveDto`
5. `lukkOppgave(UUID)` → `BrukerdialogOppgaveDto`
6. `åpneOppgave(UUID)` → `BrukerdialogOppgaveDto`
7. `løsOppgave(UUID)` → `BrukerdialogOppgaveDto`

## POM-endringer

### Root pom.xml
```xml
<modules>
    ...
    <module>brukerdialog-oppgave-api</module>
    <module>brukerdialog-oppgave</module>
    ...
</modules>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>no.nav.ung.sak</groupId>
            <artifactId>brukerdialog-oppgave-api</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>no.nav.ung.sak</groupId>
            <artifactId>brukerdialog-oppgave</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### brukerdialog-oppgave/pom.xml
```xml
<dependencies>
    <dependency>
        <groupId>no.nav.ung.sak</groupId>
        <artifactId>brukerdialog-oppgave-api</artifactId>
    </dependency>
    <!-- ... andre avhengigheter -->
</dependencies>
```

### domenetjenester/etterlysning/pom.xml
```xml
<dependencies>
    <dependency>
        <groupId>no.nav.ung.sak</groupId>
        <artifactId>brukerdialog-oppgave-api</artifactId>
    </dependency>
    <!-- ... andre avhengigheter -->
</dependencies>
```

### web/pom.xml
```xml
<dependencies>
    <dependency>
        <groupId>no.nav.ung.sak</groupId>
        <artifactId>brukerdialog-oppgave</artifactId>
    </dependency>
    <!-- ... andre avhengigheter -->
</dependencies>
```

## Kompilering

```bash
# Kompiler hele prosjektet
mvn clean compile -DskipTests

# Eller individuelt
mvn clean compile -DskipTests -pl brukerdialog-oppgave-api
mvn clean compile -DskipTests -pl brukerdialog-oppgave -am
mvn clean compile -DskipTests -pl domenetjenester/etterlysning -am
mvn clean compile -DskipTests -pl web -am
```

**Status:** ✅ BUILD SUCCESS for alle moduler

## Migrering

### For eksisterende kode som bruker BrukerdialogOppgaveTjeneste:

**Før:**
```java
@Inject
BrukerdialogOppgaveTjeneste oppgaveTjeneste;
```

**Etter (hvis du kun trenger interface-metoder):**
```java
@Inject
BrukerdialogOppgaveService oppgaveService;
```

**Hvis du trenger alle metoder (f.eks. i web):**
```java
@Inject
BrukerdialogOppgaveTjeneste oppgaveTjeneste; // Fortsett som før
```

## Oppsummering

✅ **brukerdialog-oppgave-api** - Kun interface for oppgaveoperasjoner  
✅ **brukerdialog-oppgave** - Full implementering + ekstra metoder for REST  
✅ **etterlysning** - Kun tilgang til interface (via api-modul)  
✅ **web** - Full tilgang til alle metoder  
✅ Alle moduler kompilerer uten feil

