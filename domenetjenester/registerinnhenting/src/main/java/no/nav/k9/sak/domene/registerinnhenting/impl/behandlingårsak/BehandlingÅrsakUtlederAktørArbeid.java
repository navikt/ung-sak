package no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

@Dependent
class BehandlingÅrsakUtlederAktørArbeid {

    @Inject
    public BehandlingÅrsakUtlederAktørArbeid() {
        //For CDI
    }

    BehandlingÅrsakType utledBehandlingÅrsak() {
        return BehandlingÅrsakType.RE_REGISTEROPPLYSNING;
    }
}
