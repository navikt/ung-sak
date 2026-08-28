---
applyTo: ".github/workflows/*.{yml,yaml}"
excludeAgent: "code-review"
---

# GitHub Actions (ung-sak)

CI/CD er bygget på **gjenbrukbare workflows fra `navikt/sif-gha-workflows`**. Det meste av logikken (Maven-bygg, Postgres-oppsett, image-bygg/push, Nais-deploy) ligger der — ikke i dette repoet.

## Etablert mønster

```yaml
jobs:
  build-app:
    permissions:
      contents: read
      packages: write
      id-token: write
    uses: navikt/sif-gha-workflows/.github/workflows/maven-build-app-db.yml@main
    with:
      java-version: 25
      db_schema: ung_sak_unit
      pg_version: 16
    secrets: inherit

  deploy-dev:
    if: github.ref_name == 'master'
    needs: [build-app, run-unit-tests]
    permissions:
      id-token: write
      contents: write
    uses: navikt/sif-gha-workflows/.github/workflows/maven-deploy.yml@main
    with:
      gar: true
      image: ${{ needs.build-app.outputs.build-version }}
      cluster: dev-gcp
      naiserator_file: deploy/dev-gcp.yml
    secrets: inherit
```

Viktige repo-fakta å ikke bryte:

- Nais-manifestene ligger i `deploy/dev-gcp.yml` og `deploy/prod-gcp.yml` (ikke `.nais/nais.yaml`).
- Java 25, Maven. Testene krever Postgres med schema `ung_sak_unit` — settes opp av `maven-build-app-db.yml`.
- Deploy-rekkefølge: `build-app` → `run-unit-tests` + `verdikjede-tester` → `deploy-dev` → `prod-typescript-check` → `deploy-prod`.
- `prod-typescript-check` kjører kompatibilitetssjekk mot `k9-sak-web` før prod. Ikke hopp over den — feil her betyr at frontend brekker ved utrulling.
- Endres API-et i `web`, må OpenAPI/TypeScript-klientflyten (`openapi-generate.yml`, `typescript-client-*.yml`) fortsatt henge sammen.

## Versjonspinning

Gjeldende praksis i repoet:

- `navikt/sif-gha-workflows/...@main` og `@openapi-next` — bevisst branch-ref for å følge felles oppdateringer. Ikke endre til SHA uten å avklare med teamet som eier workflowene.
- Førsteparts actions (`actions/checkout@v7`, `actions/setup-java@v5.7.0`, `nais/deploy/actions/deploy@v2`) tag-pinnes.
- Tredjeparts actions SHA-pinnes med versjonskommentar, slik Dependabot holder dem oppdatert:

```yaml
- uses: release-drafter/release-drafter@34d80673e067bdc0c24568d3af899c216adcfaa9 # v7.7.0
```

Nye tredjeparts actions skal SHA-pinnes.

## Permissions

Sett alltid eksplisitte `permissions` per job — aldri `write-all`:

```yaml
permissions:
  contents: read
  id-token: write   # OIDC mot Nais/GAR
  packages: write   # kun der image/pakke faktisk publiseres
```

## Sikkerhet

```yaml
# ❌ Hardkodet hemmelighet
env:
  API_KEY: "sk-1234567890"

# ❌ Logging av hemmelighet
- run: echo ${{ secrets.MY_API_KEY }}

# ✅ GitHub Secrets
env:
  API_KEY: ${{ secrets.MY_API_KEY }}
```

- `pull_request_target` kombinert med utsjekk av PR-branch er code injection — ikke bruk det.
- `secrets: inherit` er etablert mot `navikt/*`-workflows, men skal ikke brukes mot tredjeparts workflows.

## Boundaries

### ✅ Always

- Gjenbruk `sif-gha-workflows` fremfor å skrive egne bygg-/deploy-steg.
- Eksplisitte `permissions` per job.
- SHA-pinn nye tredjeparts actions.

### ⚠️ Ask First

- Nye secrets eller environment variables.
- Endring i deploy-rekkefølge eller `environment`/protection rules (dobbeltgodkjenning-fella er dokumentert i `build.yaml`).
- Endre ref på `sif-gha-workflows` (påvirker flere repoer).

### 🚫 Never

- `permissions: write-all`.
- Fjerne eller svekke `run-unit-tests`, `verdikjede-tester` eller `prod-typescript-check` for å få grønn bygg.
- Logge secrets i workflow-output.
- `pull_request_target` med utsjekk av PR-koden.
