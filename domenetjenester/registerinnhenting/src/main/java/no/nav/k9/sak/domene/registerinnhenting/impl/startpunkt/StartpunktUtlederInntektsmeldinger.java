package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;

@Dependent
class StartpunktUtlederInntektsmeldinger {

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    StartpunktUtlederInntektsmeldinger(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    StartpunktType utledStartpunkt(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag1) {

        if (grunnlag1 != null) {
            var nyeInntektsmeldinger = inntektArbeidYtelseTjeneste.hentInntektsmeldingerSidenRef(ref.getSaksnummer(), ref.getBehandlingId(), grunnlag1.getEksternReferanse());

            if (!nyeInntektsmeldinger.isEmpty()) {
                // TODO: Validere at det faktisk blir endring i periodene / Endring i IM i bruk
                return inntektsmeldingErSøknad(ref) ? StartpunktType.INIT_PERIODER : StartpunktType.BEREGNING;
            }
        }

        return StartpunktType.UDEFINERT;
    }

    boolean inntektsmeldingErSøknad(BehandlingReferanse ref) {
        return FagsakYtelseType.OMSORGSPENGER.equals(ref.getFagsakYtelseType());
    }
}


