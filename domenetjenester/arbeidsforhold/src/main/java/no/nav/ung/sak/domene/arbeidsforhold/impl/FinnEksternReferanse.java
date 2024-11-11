package no.nav.ung.sak.domene.arbeidsforhold.impl;

import java.util.function.BiFunction;

import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.EksternArbeidsforholdRef;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;

public class FinnEksternReferanse implements BiFunction<Arbeidsgiver, InternArbeidsforholdRef, EksternArbeidsforholdRef> {
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsforholdInformasjon arbInfo;
    private Long behandlingId;

    public FinnEksternReferanse(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, Long behandlingId) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingId = behandlingId;
    }

    @Override
    public EksternArbeidsforholdRef apply(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse) {
        if (arbInfo == null) {
            var grunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
            arbInfo = grunnlag.getArbeidsforholdInformasjon().orElseThrow(
                () -> new IllegalStateException("Utvikler-feil: mangler IAYG.ArbeidsforholdInformasjon, kan ikke slå opp ekstern referanse"));
        }
        return arbInfo.finnEkstern(arbeidsgiver, internReferanse);
    }
}
