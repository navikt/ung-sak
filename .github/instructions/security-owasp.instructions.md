---
applyTo: "**/*.java"
excludeAgent: "code-review"
---

# Security Essentials

Kodenivå-sjekker som alltid gjelder. For full OWASP Top 10-gjennomgang, bruk `$security-owasp`-skillen. For arkitektur-/trusselmodellering, bruk `@security-champion-agent`.

## Critical Rules

- **Parameteriserte spørringer** — aldri strengkonkatenering av brukerinput inn i JPQL/SQL. Bruk `setParameter(...)` / kriterie-API.
- **Ingen PII i logg** — ikke FNR, aktørId, navn, adresse eller tokens. Logg `saksnummer`/`behandlingId` i stedet.
- **Hemmeligheter fra miljø** — aldri hardkodede tokens, passord eller nøkler (heller ikke i tester).
- **Verifiser eierskap av ressurs** — ikke bare «er autentisert», men «har tilgang til denne saken». Følg etablert ABAC-/`@BeskyttetRessurs`-mønster i `web`.
- **Valider `azp` for maskin-til-maskin** — sjekk mot forhåndsautoriserte apper.
- **Ikke lekk input i valideringsfeil** — unngå `${validatedValue}` i `@Pattern`/`@Size`-meldinger; det kan eksponere FNR i feilrespons og logg.
- **Ikke svelg feil** — tomme `catch`-blokker og catch-all som skjuler feil i `ProsessTask`/async-flyt gir stille feilbehandling av ytelsessaker.
