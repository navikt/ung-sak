package no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

@Dependent
class BehandlingÅrsakUtlederAktørInntekt {

    @Inject
    public BehandlingÅrsakUtlederAktørInntekt() {
        //For CDI
    }

    BehandlingÅrsakType utledBehandlingÅrsak() {
        return BehandlingÅrsakType.RE_REGISTEROPPLYSNING;
    }
}
