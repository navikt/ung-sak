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
    private final LocalDateTimeline<InstitusjonVurdering> vurdertInstitusjonPerioder;
    private final LocalDateTimeline<SykdomVurdering> vurdertSykdomPerioder;

    public NødvendighetVilkårGrunnlag(LocalDate fom, LocalDate tom,
                                      LocalDateTimeline<OpplæringVurdering> vurdertOpplæringPerioder,
                                      LocalDateTimeline<InstitusjonVurdering> vurdertInstitusjonPerioder,
                                      LocalDateTimeline<SykdomVurdering> vurdertSykdomPerioder) {
        this.fom = fom;
        this.tom = tom;
        this.vurdertOpplæringPerioder = vurdertOpplæringPerioder;
        this.vurdertInstitusjonPerioder = vurdertInstitusjonPerioder;
        this.vurdertSykdomPerioder = vurdertSykdomPerioder;
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

    public LocalDateTimeline<InstitusjonVurdering> getVurdertInstitusjonPerioder() {
        return vurdertInstitusjonPerioder;
    }

    public LocalDateTimeline<SykdomVurdering> getVurdertSykdomPerioder() {
        return vurdertSykdomPerioder;
    }
}
