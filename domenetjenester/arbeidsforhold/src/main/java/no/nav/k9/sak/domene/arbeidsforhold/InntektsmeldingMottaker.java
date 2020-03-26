package no.nav.k9.sak.domene.arbeidsforhold;

import java.util.List;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

/**
 * Kalles når inntektsmelding mottatt. Forventes at implementasjoner implementerer med {@link FagsakYtelseTypeRef} for angitt ytelse type.
 * Default implementasjon gjør ingenting.
 */
@FunctionalInterface
public interface InntektsmeldingMottaker {

    void mottattInntektsmelding(BehandlingReferanse ref, List<Inntektsmelding> inntektsmelding);
}
