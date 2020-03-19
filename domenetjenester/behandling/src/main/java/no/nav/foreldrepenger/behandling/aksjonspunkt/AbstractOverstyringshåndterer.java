package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunkt;

public abstract class AbstractOverstyringshåndterer<T extends OverstyringAksjonspunkt> implements Overstyringshåndterer<T> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private AksjonspunktDefinisjon aksjonspunktDefinisjon;

    protected AbstractOverstyringshåndterer() {
        // for CDI proxy
    }

    protected AbstractOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                            AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        this.historikkAdapter = historikkAdapter;
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
    }

    @Override
    public void håndterAksjonspunktForOverstyringPrecondition(T dto, Behandling behandling) {
        precondition(behandling, dto);
    }

    @Override
    public void håndterAksjonspunktForOverstyringHistorikk(T dto, Behandling behandling, boolean endretBegrunnese) {
        lagHistorikkInnslag(behandling, dto);
    }

    @Override
    public AksjonspunktDefinisjon aksjonspunktForInstans() {
        return  aksjonspunktDefinisjon;
    }

    /**
     * Valider om precondition for overstyring er møtt. Kaster exception hvis ikke.
     *
     * @param behandling behandling
     * @param dto
     */
    protected void precondition(Behandling behandling, T dto) {
        // all good, do NOTHING.
    }

    protected abstract void lagHistorikkInnslag(Behandling behandling, T dto);

    protected void lagHistorikkInnslagForOverstyrtVilkår(String begrunnelse, boolean vilkårOppfylt, SkjermlenkeType skjermlenkeType) {
        HistorikkEndretFeltVerdiType tilVerdi = vilkårOppfylt ? HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT;
        HistorikkEndretFeltVerdiType fraVerdi = vilkårOppfylt ? HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT;

        getHistorikkAdapter().tekstBuilder()
            .medHendelse(HistorikkinnslagType.OVERSTYRT)
            .medBegrunnelse(begrunnelse)
            .medSkjermlenke(skjermlenkeType)
            .medEndretFelt(HistorikkEndretFeltType.OVERSTYRT_VURDERING, fraVerdi, tilVerdi);
    }

    protected HistorikkTjenesteAdapter getHistorikkAdapter() {
        return historikkAdapter;
    }

}
