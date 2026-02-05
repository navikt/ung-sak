# Brukerdialog Oppgave - POM-struktur

## ✅ Ny struktur implementert

Brukerdialog-oppgave er nå organisert som en **parent POM-modul** med to sub-moduler, akkurat som `domenetjenester`.

## 📂 Mappestruktur

```
brukerdialog-oppgave/
├── pom.xml (packaging=pom, parent for sub-moduler)
├── api/
│   ├── pom.xml (brukerdialog-oppgave-api)
│   └── src/
│       └── main/
│           └── java/
│               └── no/nav/ung/sak/oppgave/
│                   └── BrukerdialogOppgaveService.java
└── tjeneste/
    ├── pom.xml (brukerdialog-oppgave-tjeneste)
    └── src/
        └── main/
            ├── java/
            │   └── no/nav/ung/sak/oppgave/
            │       ├── BrukerdialogOppgaveTjeneste.java
            │       ├── BrukerdialogOppgaveRepository.java
            │       ├── BrukerdialogOppgaveMapper.java
            │       ├── BrukerdialogOppgaveEntitet.java
            │       ├── OppgaveStatus.java
            │       ├── OppgaveType.java
            │       └── ... (andre klasser)
            └── resources/
                └── META-INF/
                    ├── beans.xml
                    └── orm.xml
```

## 📋 POM-filer

### 1. Parent POM (brukerdialog-oppgave/pom.xml)
```xml
<artifactId>brukerdialog-oppgave-pom</artifactId>
<packaging>pom</packaging>

<modules>
    <module>api</module>
    <module>tjeneste</module>
</modules>
```

### 2. API-modul (brukerdialog-oppgave/api/pom.xml)
```xml
<parent>
    <artifactId>brukerdialog-oppgave-pom</artifactId>
    <groupId>no.nav.ung.sak</groupId>
    <version>${revision}${sha1}${changelist}</version>
</parent>

<artifactId>brukerdialog-oppgave-api</artifactId>
<packaging>jar</packaging>
```

### 3. Tjeneste-modul (brukerdialog-oppgave/tjeneste/pom.xml)
```xml
<parent>
    <artifactId>brukerdialog-oppgave-pom</artifactId>
    <groupId>no.nav.ung.sak</groupId>
    <version>${revision}${sha1}${changelist}</version>
</parent>

<artifactId>brukerdialog-oppgave-tjeneste</artifactId>
<packaging>jar</packaging>

<dependencies>
    <dependency>
        <groupId>no.nav.ung.sak</groupId>
        <artifactId>brukerdialog-oppgave-api</artifactId>
    </dependency>
    <!-- ... andre avhengigheter -->
</dependencies>
```

## 🔗 Avhengigheter i andre moduler

### Root pom.xml
```xml
<modules>
    <module>brukerdialog-oppgave</module>
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
            <artifactId>brukerdialog-oppgave-tjeneste</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### etterlysning/pom.xml
```xml
<dependency>
    <groupId>no.nav.ung.sak</groupId>
    <artifactId>brukerdialog-oppgave-api</artifactId>
</dependency>
```

### web/pom.xml
```xml
<dependency>
    <groupId>no.nav.ung.sak</groupId>
    <artifactId>brukerdialog-oppgave-tjeneste</artifactId>
</dependency>
```

## 🎯 Fordeler med POM-struktur

### ✅ Lik struktur som domenetjenester
- Konsistent organisering av moduler
- Enklere å navigere i prosjektet
- Følger etablerte konvensjoner

### ✅ Klar separasjon
- **api**: Kun interface, ingen implementering
- **tjeneste**: Full implementering + ekstra metoder

### ✅ Fleksibel bygging
```bash
# Bygg hele brukerdialog-oppgave (begge sub-moduler)
mvn clean install -pl brukerdialog-oppgave

# Bygg kun api
mvn clean install -pl brukerdialog-oppgave/api

# Bygg kun tjeneste
mvn clean install -pl brukerdialog-oppgave/tjeneste
```

### ✅ Maven reaktor
Maven bygger automatisk i riktig rekkefølge:
1. api (ingen avhengigheter til andre brukerdialog-moduler)
2. tjeneste (avhenger av api)

## 📊 Avhengighetsdiagram

```
┌─────────────────────────────────────┐
│   brukerdialog-oppgave-pom          │
│   (packaging=pom)                   │
└──────────────┬──────────────────────┘
               │
       ┌───────┴───────┐
       │               │
       ▼               ▼
┌────────────┐  ┌──────────────────┐
│    api     │  │    tjeneste      │
│ (jar)      │◄─│ (jar)            │
└────────────┘  └──────────────────┘
       ▲               ▲
       │               │
       │               │
┌──────┴─────┐  ┌──────┴──────┐
│etterlysning│  │    web      │
└────────────┘  └─────────────┘
```

## 🔄 Sammenligning med gammel struktur

### Før (flat struktur):
```
brukerdialog-oppgave-api/      (separat modul)
brukerdialog-oppgave/          (separat modul)
```

### Etter (POM-hierarki):
```
brukerdialog-oppgave/          (parent POM)
├── api/                       (sub-modul)
└── tjeneste/                  (sub-modul)
```

## ✅ Kompilering

Alle moduler kompilerer uten feil:

```bash
# Kompiler alt
mvn clean compile -DskipTests

# Spesifikke moduler
mvn clean compile -DskipTests -pl brukerdialog-oppgave -am
mvn clean compile -DskipTests -pl domenetjenester/etterlysning -am
mvn clean compile -DskipTests -pl web -am
```

**Status:** ✅ BUILD SUCCESS

## 📝 Maven Reactor Output

```
[INFO] Reactor Build Order:
[INFO] 
[INFO] ung-sak :: Root                                    [pom]
[INFO] UNGSAK :: Fellestjenester                          [jar]
[INFO] ung-sak :: Kodeverk                                [jar]
[INFO] ung-sak :: Kontrakter                              [jar]
[INFO] UNGSAK :: Brukerdialog oppgave - Pom               [pom]
[INFO] UNGSAK :: Brukerdialog oppgave - API               [jar]
[INFO] UNGSAK :: Brukerdialog oppgave - Tjeneste          [jar]
```

## 🎉 Resultat

Brukerdialog-oppgave følger nå samme struktur som `domenetjenester`:
- Parent POM med `packaging=pom`
- To sub-moduler: `api` og `tjeneste`
- Klar separasjon av ansvar
- Konsistent med resten av prosjektet

