package no.nav.k9.sak.domene.arbeidsforhold;

import java.util.Collections;
import java.util.List;

import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.typer.PeriodeAndel;

/** Innhold i en builder. */
public class InntektsmeldingInnhold {

    private InntektsmeldingBuilder builder;
    private List<PeriodeAndel> omsorgspengerFravær;

    public InntektsmeldingInnhold(InntektsmeldingBuilder inntektsmeldingBuilder, List<PeriodeAndel> omsorgspengerFravær) {
        this.builder = inntektsmeldingBuilder;
        this.omsorgspengerFravær = omsorgspengerFravær;
    }

    public InntektsmeldingInnhold(InntektsmeldingBuilder inntektsmeldingBuilder) {
       this(inntektsmeldingBuilder, Collections.emptyList());
    }

    public Inntektsmelding getInntektsmelding() {
        return builder.build();
    }
    
    /** Inneholder ekstra info til bruk i mapping mot abakus. */
    public InntektsmeldingBuilder getInntektsmeldingBuilder() {
        return builder;
    }

    public List<PeriodeAndel> getOmsorgspengerFravær() {
        return omsorgspengerFravær;
    }
}
