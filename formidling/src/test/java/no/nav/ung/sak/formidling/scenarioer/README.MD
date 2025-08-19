# Test Scenarioer for Formidling

Denne mappen inneholder testscenarioer for formidlingsmodulen (kommunikasjon/brevgenerering).

## Oversikt

Scenarioer-pakken tilbyr forhåndsdefinerte testscenarioer som simulerer ulike forretningssaker og livssyklushendelser for ungdomsytelse-saker. Disse scenarioene brukes til å teste brevgenerering og kommunikasjonsflyter.

## Formål

Disse scenarioene bidrar til å sikre at:
- Brevgenerering fungerer korrekt for ulike sakstyper
- Forretningslogikk testes grundig på tvers av ulike scenarioer
- Kanttilfeller og tilstandsoverganger dekkes
- Kommunikasjonsflyter håndterer ulike ytelseendringer på riktig måte

## Bruk

### Typisk testoppbygning
```java
@Test
void testBrevgenerering() {
    // 1. Opprett scenario
    var ungTestGrunnlag = FørstegangsbehandlingScenarioer.innvilget19år(LocalDate.of(2024, 12, 1));
    
    // 2. Bygg testscenario
    var scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestGrunnlag);
    
    // 3. Lagre og test
    var behandling = scenarioBuilder.buildOgLagreMedUng(repositories);
    
    // 4. Test brevgenerering eller andre tjenester
    // ...
}
```

Hvert scenario returnerer et `UngTestScenario` objekt som kan brukes med `TestScenarioBuilder` for å opprette komplette testbehandlinger med realistiske testdata inkludert datoer, beløp og statusoverganger.
