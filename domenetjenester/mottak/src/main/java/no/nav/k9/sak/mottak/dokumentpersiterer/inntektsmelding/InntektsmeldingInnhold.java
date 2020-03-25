package no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding;

import java.util.List;

import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.typer.PeriodeAndel;

public class InntektsmeldingInnhold {

    private Inntektsmelding inntektsmelding;
    private List<PeriodeAndel> omsorgspengerFravær;

    public InntektsmeldingInnhold(Inntektsmelding inntektsmelding, List<PeriodeAndel> omsorgspengerFravær) {
        this.inntektsmelding = inntektsmelding;
        this.omsorgspengerFravær = omsorgspengerFravær;
    }

    public Inntektsmelding getInntektsmelding() {
        return inntektsmelding;
    }

    public List<PeriodeAndel> getOmsorgspengerFravær() {
        return omsorgspengerFravær;
    }
}
