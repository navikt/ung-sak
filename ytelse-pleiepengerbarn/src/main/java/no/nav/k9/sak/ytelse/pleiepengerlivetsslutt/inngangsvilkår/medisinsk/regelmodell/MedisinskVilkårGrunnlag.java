package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import java.time.LocalDate;

import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;

@RuleDocumentationGrunnlag
public class MedisinskVilkårGrunnlag implements VilkårGrunnlag {

    private final LocalDate fom;
    private final LocalDate tom;

    private LocalDateTimeline<Void> dokumentertLivetsSluttfasePerioder;
    private LocalDateTimeline<Void> innleggelsesPerioder;

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

    public LocalDateTimeline<Void> getDokumentertLivetsSluttfasePerioder() {
        return dokumentertLivetsSluttfasePerioder;
    }

    public LocalDateTimeline<Void> getInnleggelsesPerioder() {
        return innleggelsesPerioder;
    }

    public MedisinskVilkårGrunnlag medDokumentertLivetsSluttfasePerioder(LocalDateTimeline<Void> dokumentertLivetsSluttfasePerioder) {
        this.dokumentertLivetsSluttfasePerioder = dokumentertLivetsSluttfasePerioder;
        return this;
    }

    public MedisinskVilkårGrunnlag medInnleggelsesPerioder(LocalDateTimeline<Void> innleggelsesPerioder) {
        this.innleggelsesPerioder = innleggelsesPerioder;
        return this;
    }


    @Override
    public String toString() {
        return "MedisinskvilkårGrunnlag{" +
            "fom=" + fom +
            "tom=" + tom +
            ", dokumentertLivetsSluttfasePerioder=" + dokumentertLivetsSluttfasePerioder +
            ", innleggelsesPerioder=" + innleggelsesPerioder +
            '}';
    }
}
