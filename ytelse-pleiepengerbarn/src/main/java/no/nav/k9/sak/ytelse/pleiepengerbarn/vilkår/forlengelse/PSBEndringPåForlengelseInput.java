package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse;

import java.util.Collection;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.perioder.EndringPåForlengelseInput;

public class PSBEndringPåForlengelseInput implements EndringPåForlengelseInput {

    private BehandlingReferanse referanse;
    private Collection<Inntektsmelding> sakInntektsmeldinger;

    public PSBEndringPåForlengelseInput(BehandlingReferanse referanse) {
        this.referanse = referanse;
    }

    public PSBEndringPåForlengelseInput medInntektsmeldinger(Collection<Inntektsmelding> sakInntektsmeldinger) {
        this.sakInntektsmeldinger = sakInntektsmeldinger;
        return this;
    }

    public Collection<Inntektsmelding> getSakInntektsmeldinger() {
        return sakInntektsmeldinger;
    }

    @Override
    public BehandlingReferanse getBehandlingReferanse() {
        return referanse;
    }
}
