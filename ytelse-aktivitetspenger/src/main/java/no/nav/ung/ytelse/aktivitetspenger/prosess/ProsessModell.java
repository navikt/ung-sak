package no.nav.ung.ytelse.aktivitetspenger.prosess;


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

    @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
    @BehandlingTypeRef(BehandlingType.AKTIVITETSPENGER_DEL_1)
    @Produces
    @ApplicationScoped
    public BehandlingModell aktivitetspengerDel1() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.AKTIVITETSPENGER_DEL_1, FagsakYtelseType.AKTIVITETSPENGER);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG)
            .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP, StartpunktType.INNHENT_REGISTEROPPLYSNINGER)
            .medSteg(BehandlingStegType.ALDERSVILKÅRET)
            .medSteg(BehandlingStegType.VURDER_BOSTED)
            .medSteg(BehandlingStegType.FAKTA_14A_VEDTAK)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

}
