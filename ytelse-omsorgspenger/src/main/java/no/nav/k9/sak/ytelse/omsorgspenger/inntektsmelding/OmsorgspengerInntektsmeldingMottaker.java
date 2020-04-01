package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingMottaker;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;

@FagsakYtelseTypeRef("OMP")
@BehandlingTypeRef
@ApplicationScoped
public class OmsorgspengerInntektsmeldingMottaker implements InntektsmeldingMottaker {

    @SuppressWarnings("unused")
    private OmsorgspengerGrunnlagRepository grunnlagRepository;

    public OmsorgspengerInntektsmeldingMottaker() {
        // for proxy
    }

    @Inject
    public OmsorgspengerInntektsmeldingMottaker(OmsorgspengerGrunnlagRepository grunnlagRepository) {
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public void mottattInntektsmelding(BehandlingReferanse ref, List<Inntektsmelding> inntektsmeldinger) {
        // trekker ut fravær i FaktaForÅrskvantumUttakSteg, ikke ved mottak.
    }


}
