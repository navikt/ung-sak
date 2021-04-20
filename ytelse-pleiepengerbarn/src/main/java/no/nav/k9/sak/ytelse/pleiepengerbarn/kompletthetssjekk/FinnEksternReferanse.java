package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.util.function.BiFunction;

import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

class FinnEksternReferanse implements BiFunction<Arbeidsgiver, InternArbeidsforholdRef, EksternArbeidsforholdRef> {
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsforholdInformasjon arbInfo;
    private Long behandlingId;

    FinnEksternReferanse(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, Long behandlingId) {
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
