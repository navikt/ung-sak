package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import java.time.LocalDate;
import java.util.List;

import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;

@RuleDocumentationGrunnlag
public class MedisinskVilkårGrunnlag implements VilkårGrunnlag {

    private final LocalDate fom;
    private final LocalDate tom;

    private List<LocalDateInterval> relevantLivetsSlutt = List.of();
    private List<PleiePeriode> innleggelsesPerioder;

    public MedisinskVilkårGrunnlag(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public LocalDateInterval getInterval() {
        return new LocalDateInterval(fom, tom);
    }

    public List<LocalDateInterval> getRelevantVurderingLivetsSlutt() {
        return relevantLivetsSlutt;
    }

    public List<PleiePeriode> getInnleggelsesPerioder() {
        return innleggelsesPerioder;
    }

    public MedisinskVilkårGrunnlag medLivetsSluttBehov(List<LocalDateInterval> relevantLivetsSlutt) {
        this.relevantLivetsSlutt = relevantLivetsSlutt;
        return this;
    }

    public MedisinskVilkårGrunnlag medInnleggelsesPerioder(List<PleiePeriode> innleggelsesPerioder) {
        this.innleggelsesPerioder = innleggelsesPerioder;
        return this;
    }


    @Override
    public String toString() {
        return "MedisinskvilkårGrunnlag{" +
            "fom=" + fom +
            ", relevantLivetsSlutt=" + relevantLivetsSlutt +
            ", innleggelsesPerioder=" + innleggelsesPerioder +
            '}';
    }
}
