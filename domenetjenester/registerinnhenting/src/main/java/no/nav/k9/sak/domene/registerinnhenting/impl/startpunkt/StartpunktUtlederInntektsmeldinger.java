package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;

@Dependent
class StartpunktUtlederInntektsmeldinger {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    StartpunktUtlederInntektsmeldinger(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    StartpunktType utledStartpunkt(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag1) {

        if (inntektsmeldingErSøknad(ref)) {
            var nyeInntektsmeldinger = inntektArbeidYtelseTjeneste.hentInntektsmeldingerSidenRef(ref.getSaksnummer(), ref.getBehandlingId(), grunnlag1.getEksternReferanse());

            // TODO: Validere at det faktisk blir endring i periodene
            if (!nyeInntektsmeldinger.isEmpty()) {
                return StartpunktType.INIT_PERIODER;
            }
        }

        return StartpunktType.UDEFINERT;
    }

    boolean inntektsmeldingErSøknad(BehandlingReferanse ref) {
        return FagsakYtelseType.OMSORGSPENGER.equals(ref.getFagsakYtelseType());
    }
}


