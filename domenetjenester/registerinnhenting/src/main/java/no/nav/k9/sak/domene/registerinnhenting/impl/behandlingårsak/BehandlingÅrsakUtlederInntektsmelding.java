package no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

@Dependent
class BehandlingÅrsakUtlederInntektsmelding {

    @Inject
    public BehandlingÅrsakUtlederInntektsmelding() {
        //For CDI
    }

    BehandlingÅrsakType utledBehandlingÅrsak() {
        return BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
    }
}
