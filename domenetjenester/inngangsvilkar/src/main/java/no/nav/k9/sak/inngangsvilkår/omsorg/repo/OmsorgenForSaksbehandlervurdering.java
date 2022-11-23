package no.nav.k9.sak.inngangsvilkår.omsorg.repo;

import java.util.Objects;

import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class OmsorgenForSaksbehandlervurdering {
    private final DatoIntervallEntitet periode;
    private final String begrunnelse;
    private final Resultat resultat;

    public OmsorgenForSaksbehandlervurdering(DatoIntervallEntitet periode, String begrunnelse, Resultat resultat) {
        this.periode = Objects.requireNonNull(periode, "periode");
        this.begrunnelse = Objects.requireNonNull(begrunnelse, "begrunnelse");
        this.resultat = Objects.requireNonNull(resultat, "resultat");
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Resultat getResultat() {
        return resultat;
    }
}
