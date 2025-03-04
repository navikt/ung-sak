package no.nav.ung.sak.behandlingskontroll;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingModellImpl;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

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
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.VURDER_UNGDOMSPROGRAMVILKÅR)
            .medSteg(BehandlingStegType.ALDERSVILKÅRET)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.UNGDOMSYTELSE_BEREGNING, StartpunktType.BEREGNING)
            .medSteg(BehandlingStegType.VURDER_UTTAK)
            .medSteg(BehandlingStegType.BEREGN_YTELSE)
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
            .medSteg(BehandlingStegType.VARSEL_REVURDERING)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.VURDER_UNGDOMSPROGRAMVILKÅR)
            .medSteg(BehandlingStegType.ALDERSVILKÅRET)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.UNGDOMSYTELSE_BEREGNING, StartpunktType.BEREGNING)
            .medSteg(BehandlingStegType.VURDER_UTTAK)
            .medSteg(BehandlingStegType.BEREGN_YTELSE)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }
}
