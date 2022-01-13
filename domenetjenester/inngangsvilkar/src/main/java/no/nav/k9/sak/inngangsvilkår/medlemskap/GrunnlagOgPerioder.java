package no.nav.k9.sak.inngangsvilkår.medlemskap;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableSet;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.medlemskap.regelmodell.MedlemskapsvilkårGrunnlag;

public class GrunnlagOgPerioder {

    private Map<LocalDate, MedlemskapsvilkårGrunnlag> grunnlagPerVurderingsdato;
    private NavigableSet<DatoIntervallEntitet> forlengelsesPerioder;

    public GrunnlagOgPerioder(Map<LocalDate, MedlemskapsvilkårGrunnlag> grunnlagPerVurderingsdato, NavigableSet<DatoIntervallEntitet> forlengelsesPerioder) {
        this.grunnlagPerVurderingsdato = grunnlagPerVurderingsdato;
        this.forlengelsesPerioder = forlengelsesPerioder;
    }

    public Map<LocalDate, MedlemskapsvilkårGrunnlag> getGrunnlagPerVurderingsdato() {
        return grunnlagPerVurderingsdato;
    }

    public NavigableSet<DatoIntervallEntitet> getForlengelsesPerioder() {
        return forlengelsesPerioder;
    }
}
