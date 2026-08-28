---
applyTo: "**"
excludeAgent: "code-review"
---

# Bevisst AI-bruk

Hvordan du bruker AI betyr mer enn om du bruker det. Utviklere som aktivt stiller spørsmål til generert kode forstår den langt bedre enn de som delegerer blindt.

## Grønn og rød sone

🟢 **Grønn sone — AI-egnet**: boilerplate, kjent teknologi, konfigurasjon, refaktorering med kjent mål (rename/extract/move), testdata og fixtures.

🔴 **Rød sone — kode manuelt først**: debugging, nye konsepter, kjernelogikk (vilkårsvurdering, beregning, tilstandsmaskiner i `behandlingsprosess`), sikkerhetskritisk kode (tilgangskontroll, inputvalidering), og arkitekturbeslutninger (datamodell, API-kontrakter, Flyway-migreringer).

**Tre-forsøks-regelen**: prøv minst tre tilnærminger selv før du ber AI om hjelp på rød-sone-oppgaver. Juniorutviklere bør holde mer i rød sone.

## Generer-så-forstå

Når AI genererer kode: **generer → forstå → verifiser → tilpass**. Gode oppfølgingsspørsmål:

- «Hvorfor denne tilnærmingen fremfor alternativene?»
- «Hva kan gå galt, og hvilke edge cases dekkes ikke?»
- «Forklar tradeoffene i denne designbeslutningen»

## For agenter

- Forklar *hvorfor*, ikke bare *hva*, når du genererer kode.
- Marker kjernelogikk og sikkerhetskode som «rød sone — forstå dette grundig».
- Aldri hoppe over feilhåndtering eller sikkerhetsmønstre i eksempler.
- Aldri oppfordre til blind copy-paste.

Kilder: [Anthropic (2026)](https://www.anthropic.com/research/AI-assistance-coding-skills), [METR (2025)](https://metr.org/blog/2025-07-10-early-2025-ai-experienced-os-dev-study/), [Stray et al., HICSS-59 (2026)](https://arxiv.org/abs/2509.20353).
