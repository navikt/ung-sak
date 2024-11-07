package no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

@Dependent
class BehandlingÅrsakUtlederAktørYtelse {

    @Inject
    public BehandlingÅrsakUtlederAktørYtelse() {
        //For CDI
    }

    BehandlingÅrsakType utledBehandlingÅrsak() {
        return BehandlingÅrsakType.RE_OPPLYSNINGER_OM_YTELSER;
    }
}
