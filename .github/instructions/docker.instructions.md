---
applyTo: "**/Dockerfile"
excludeAgent: "code-review"
---

# Dockerfile (ung-sak)

Repoet har én `Dockerfile` i rot. Appen bygges med Maven **utenfor** Dockerfile — imaget kopierer inn ferdige artefakter fra `web/target/`.

## Base image

`ung-sak` bruker det delte k9-baseimaget, ikke et generisk Chainguard-image:

```dockerfile
FROM ghcr.io/navikt/k9-felles/felles-java-25:12.1.0
```

- Imaget vedlikeholdes av k9/sif-teamene og inneholder JRE, `apprunner`-bruker og `scripts/RyddBiblioteker`.
- Oppgrader ved å bumpe taggen (samme tag i begge stages) — ikke bytt til et annet baseimage uten å avklare det.
- For nye, frittstående Nav-apper er Chainguard fra Navs registry standarden
  (`europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:<tag>`), men det gjelder ikke dette imaget.

## Struktur

Dockerfila bruker et multi-stage-oppsett der første stage kun rydder bort duplikate biblioteker:

```dockerfile
FROM <base> AS duplikatfjerner
COPY --link --exclude=no.nav.ung.sak* web/target/lib/ /build/lib/
RUN ["java", "scripts/RyddBiblioteker", "DUPLIKAT", "/app/lib", "/build/lib"]

FROM <base>
COPY --link --from=duplikatfjerner /build/lib/ /app/lib/
RUN ["java", "scripts/RyddBiblioteker", "UBRUKT", "/app/lib"]
USER apprunner
```

- Bruk `COPY --link` (krever `# syntax=docker/dockerfile:1.7.0-labs` øverst for `--exclude`).
- `USER root` kun rundt `RyddBiblioteker`-stegene; avslutt alltid med `USER apprunner`.
- Kopier eksplisitte stier (`web/target/app.jar`, `web/target/lib/no.nav.ung.sak*`, pdfgen-templates) — aldri `COPY . .`.

## Sikkerhet

- Kjør som ikke-root (`apprunner`) i sluttimaget.
- Ingen hemmeligheter i `ENV`/`ARG`.
- Minimal `COPY` — kun det appen faktisk trenger i runtime.
- Hold `.dockerignore` oppdatert (finnes i rot) så `target/`-støy og `.git` ikke havner i byggkonteksten.

## Boundaries

### ✅ Always

- Behold `apprunner` som sluttbruker.
- Bruk samme baseimage-tag i alle stages.
- Verifiser at nye moduler faktisk kopieres inn i `/app/lib/` hvis de trengs i runtime.

### ⚠️ Ask First

- Bytte baseimage eller major-versjon av Java.
- Endre `RyddBiblioteker`-stegene (kan fjerne klasser appen trenger).
- Legge til nye `RUN`-steg som krever `root`.

### 🚫 Never

- `COPY . .` i sluttimaget.
- Kjøre som `root` i runtime.
- Hemmeligheter i Dockerfile.
- Full-OS-baseimages (`ubuntu`, `debian`, `openjdk`).
