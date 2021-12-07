package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import java.time.LocalDate;
import java.util.List;

import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;

@RuleDocumentationGrunnlag
public class MedisinskvilkårGrunnlag implements VilkårGrunnlag {

    private final LocalDate fom;
    private final LocalDate tom;

    private List<LocalDateInterval> relevantLivetsSlutt = List.of();

    public
    MedisinskvilkårGrunnlag(LocalDate fom, LocalDate tom) {
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

    public MedisinskvilkårGrunnlag medLivetsSluttBehov(List<LocalDateInterval> relevantLivetsSlutt) {
        this.relevantLivetsSlutt = relevantLivetsSlutt;
        return this;
    }


    @Override
    public String toString() {
        return "MedisinskvilkårGrunnlag{" +
            "fom=" + fom +
            ", tom=" + tom +
            '}';
    }
}
