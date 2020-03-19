package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.util.Optional;

import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class IkkeTattStillingTil {
    private IkkeTattStillingTil() {
        // skjul public constructor
    }

    public static boolean vurder(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, InntektArbeidYtelseGrunnlag grunnlag) {
        final Optional<ArbeidsforholdInformasjon> informasjon = grunnlag.getArbeidsforholdInformasjon();
        if (informasjon.isPresent()) {
            final ArbeidsforholdInformasjon arbeidsforholdInformasjon = informasjon.get();
            return arbeidsforholdInformasjon.getOverstyringer()
                .stream()
                .noneMatch(ov -> ov.getArbeidsgiver().equals(arbeidsgiver)
                    && ov.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef));
        }
        return arbeidsforholdRef!=null && arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold();
    }
}
