package no.nav.k9.sak.domene.arbeidsforhold;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class DefaultInntektsmeldingMottaker implements InntektsmeldingMottaker {

    @Override
    public void mottattInntektsmelding(BehandlingReferanse ref, List<Inntektsmelding> inntektsmeldinger) {
        // template method, gjør ingenting.
    }

}
