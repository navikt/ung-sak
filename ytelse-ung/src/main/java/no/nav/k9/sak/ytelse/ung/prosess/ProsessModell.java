package no.nav.k9.sak.ytelse.ung.prosess;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellImpl;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

@ApplicationScoped
public class ProsessModell {

    @FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
    @BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
    @Produces
    @ApplicationScoped
    public BehandlingModell førstegangsbehandling() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.FØRSTEGANGSSØKNAD, FagsakYtelseType.UNGDOMSYTELSE);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG)
            .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

    @FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
    @BehandlingTypeRef(BehandlingType.REVURDERING)
    @Produces
    @ApplicationScoped
    public BehandlingModell revurdering() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.REVURDERING, FagsakYtelseType.UNGDOMSYTELSE);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG)
            .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }
}
