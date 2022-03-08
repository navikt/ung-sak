package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell;

import java.time.LocalDate;

import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;

@RuleDocumentationGrunnlag
public class PleiesIHjemmetVilkårGrunnlag implements VilkårGrunnlag {

    private final LocalDate fom;
    private final LocalDate tom;

    public PleiesIHjemmetVilkårGrunnlag(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }


    @Override
    public String toString() {
        return "PleiesIHjemmetVilkårGrunnlag{" +
            "fom=" + fom +
            "tom=" + tom +
            '}';
    }
}
