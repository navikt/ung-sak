package no.nav.k9.sak.ytelse.unntaksbehandling.prosess;


import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

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

    private static final String YTELSE_OMS = "OMP";
    private static final String YTELSE_PLEIEPENGER = "PSB";
    private static final String YTELSE_FRISINN = "FRISINN";

    private static final FagsakYtelseType YTELSE_TYPE_OMS = FagsakYtelseType.OMSORGSPENGER;
    private static final FagsakYtelseType YTELSE_TYPE_PSB  = FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
    private static final FagsakYtelseType YTELSE_TYPE_FRISINN = FagsakYtelseType.FRISINN;

    @FagsakYtelseTypeRef(YTELSE_OMS)
    @BehandlingTypeRef("BT-010")
    @Produces
    @ApplicationScoped
    public BehandlingModell manuellBehandlingOmsorgspenger() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.UNNTAKSBEHANDLING, YTELSE_TYPE_OMS);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA, StartpunktType.KONTROLLER_FAKTA)
            .medSteg(BehandlingStegType.MANUELL_VILKÅRSVURDERING)
            .medSteg(BehandlingStegType.MANUELL_TILKJENNING_YTELSE)
            .medSteg(BehandlingStegType.SIMULER_OPPDRAG)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

    @FagsakYtelseTypeRef(YTELSE_FRISINN)
    @BehandlingTypeRef("BT-010")
    @Produces
    @ApplicationScoped
    public BehandlingModell manuellBehandlingFrisinn() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.UNNTAKSBEHANDLING, YTELSE_TYPE_FRISINN);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA, StartpunktType.KONTROLLER_FAKTA)
            .medSteg(BehandlingStegType.MANUELL_VILKÅRSVURDERING)
            .medSteg(BehandlingStegType.MANUELL_TILKJENNING_YTELSE)
            .medSteg(BehandlingStegType.SIMULER_OPPDRAG)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

    @FagsakYtelseTypeRef(YTELSE_PLEIEPENGER)
    @BehandlingTypeRef("BT-010")
    @Produces
    @ApplicationScoped
    public BehandlingModell manuellBehandlingPleiepenger() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.UNNTAKSBEHANDLING, YTELSE_TYPE_PSB);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA, StartpunktType.KONTROLLER_FAKTA)
            .medSteg(BehandlingStegType.MANUELL_VILKÅRSVURDERING)
            .medSteg(BehandlingStegType.MANUELL_TILKJENNING_YTELSE)
            .medSteg(BehandlingStegType.SIMULER_OPPDRAG)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

}
