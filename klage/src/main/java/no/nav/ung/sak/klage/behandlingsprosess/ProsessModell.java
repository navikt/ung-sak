package no.nav.ung.sak.klage.behandlingsprosess;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingModellImpl;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

@ApplicationScoped
public class ProsessModell {

    @FagsakYtelseTypeRef() // Default - dekker alle fagsakytelsestyper
    @BehandlingTypeRef(BehandlingType.KLAGE) // Behandlingtype = klage (på fagsakytelsene)
    @Produces
    @ApplicationScoped
    public BehandlingModell klage() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.KLAGE, null);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG, StartpunktType.UDEFINERT)
            .medSteg(BehandlingStegType.VURDER_FORMKRAV_KLAGE_FØRSTEINSTANS)
            .medSteg(BehandlingStegType.VURDER_KLAGE_FØRSTEINSTANS)

            .medSteg(BehandlingStegType.OVERFØRT_NK)

            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }
}
