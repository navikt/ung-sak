package no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll;

import java.util.List;

import no.nav.k9.kodeverk.risikoklassifisering.Kontrollresultat;

public class FaresignalgruppeDto {
    private Kontrollresultat kontrollresultat;
    private List<String> faresignaler;

    public Kontrollresultat getKontrollresultat() {
        return kontrollresultat;
    }

    public void setKontrollresultat(Kontrollresultat kontrollresultat) {
        this.kontrollresultat = kontrollresultat;
    }

    public List<String> getFaresignaler() {
        return faresignaler;
    }

    public void setFaresignaler(List<String> faresignaler) {
        this.faresignaler = faresignaler;
    }
}
