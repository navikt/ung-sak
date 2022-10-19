package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import java.time.LocalDate;

import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;

@RuleDocumentationGrunnlag
public class NødvendighetVilkårGrunnlag implements VilkårGrunnlag {

    private final LocalDate fom;
    private final LocalDate tom;
    private final LocalDateTimeline<OpplæringVurdering> vurdertOpplæringPerioder;

    public NødvendighetVilkårGrunnlag(LocalDate fom, LocalDate tom,
                                      LocalDateTimeline<OpplæringVurdering> vurdertOpplæringPerioder) {
        this.fom = fom;
        this.tom = tom;
        this.vurdertOpplæringPerioder = vurdertOpplæringPerioder;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public LocalDateTimeline<OpplæringVurdering> getVurdertOpplæringPerioder() {
        return vurdertOpplæringPerioder;
    }
}
