package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell;

import java.time.LocalDate;

import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;

@RuleDocumentationGrunnlag
public class MedisinskVilkårGrunnlag implements VilkårGrunnlag {

    private final LocalDate fom;
    private final LocalDate tom;

    private LocalDateTimeline<LangvarigSykdomDokumentasjon> dokumentertLangvarigSykdomPerioder;

    public MedisinskVilkårGrunnlag(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
        this.dokumentertLangvarigSykdomPerioder = null;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public LocalDateTimeline<LangvarigSykdomDokumentasjon> getDokumentertLangvarigSykdomPerioder() {
        return dokumentertLangvarigSykdomPerioder;
    }

    public MedisinskVilkårGrunnlag medDokumentertLangvarigSykdomPerioder(LocalDateTimeline<LangvarigSykdomDokumentasjon> dokumentertLangvarigSykdomPerioder) {
        this.dokumentertLangvarigSykdomPerioder = dokumentertLangvarigSykdomPerioder;
        return this;
    }

    @Override
    public String toString() {
        return "MedisinskvilkårGrunnlag{" +
            "fom=" + fom +
            "tom=" + tom +
            ", dokumentertLangvarigSykdomPerioder=" + dokumentertLangvarigSykdomPerioder +
            '}';
    }
}
