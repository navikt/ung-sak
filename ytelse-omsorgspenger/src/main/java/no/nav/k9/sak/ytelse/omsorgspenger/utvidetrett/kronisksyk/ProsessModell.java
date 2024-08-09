package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
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

    private static final FagsakYtelseType YTELSE_TYPE = OMSORGSPENGER_KS;
    private boolean aldersvilkårIKroniskSyk;

    @Inject
    public ProsessModell(
        @KonfigVerdi(value = "ALDERSVILKÅR_I_KRONISK_SYK", defaultVerdi = "false") boolean aldersvilkårIKroniskSyk
    ) {
        this.aldersvilkårIKroniskSyk = aldersvilkårIKroniskSyk;
    }

    @FagsakYtelseTypeRef(OMSORGSPENGER_KS)
    @BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
    @Produces
    @ApplicationScoped
    public BehandlingModell førstegangsbehandling() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.FØRSTEGANGSSØKNAD, YTELSE_TYPE);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG)
            .medSteg(BehandlingStegType.VURDER_KOMPLETTHET)
            .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP);
        if(aldersvilkårIKroniskSyk) {
            modellBuilder
                .medSteg(BehandlingStegType.VURDER_ALDERSVILKÅR_BARN);
        }
        modellBuilder
            .medSteg(BehandlingStegType.VURDER_OMSORG_FOR)
            .medSteg(BehandlingStegType.MANUELL_VILKÅRSVURDERING)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);

        return modellBuilder.build();
    }

    @FagsakYtelseTypeRef(OMSORGSPENGER_KS)
    @BehandlingTypeRef(BehandlingType.REVURDERING)
    @Produces
    @ApplicationScoped
    public BehandlingModell revurdering() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.REVURDERING, YTELSE_TYPE);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG)
            .medSteg(BehandlingStegType.VURDER_KOMPLETTHET)
            .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.VURDER_ALDERSVILKÅR_BARN)
            .medSteg(BehandlingStegType.VURDER_OMSORG_FOR)
            .medSteg(BehandlingStegType.MANUELL_VILKÅRSVURDERING)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

}
