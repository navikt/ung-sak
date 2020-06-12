package no.nav.k9.sak.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.inngangsvilkår.InngangsvilkårTjeneste;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;

public abstract class InngangsvilkårOverstyringshåndterer<T extends OverstyringAksjonspunktDto> extends AbstractOverstyringshåndterer<T> {

    private VilkårType vilkårType;
    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    protected InngangsvilkårOverstyringshåndterer() {
        // for CDI proxy
    }

    public InngangsvilkårOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                               AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                               VilkårType vilkårType,
                                               InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter, aksjonspunktDefinisjon);
        this.vilkårType = vilkårType;
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(T dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        Utfall utfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;

        inngangsvilkårTjeneste.overstyrAksjonspunkt(behandling.getId(), vilkårType, utfall, dto.getAvslagskode(), kontekst, dto.getPeriode().getFom(), dto.getPeriode().getTom(), dto.getBegrunnelse());

        return OppdateringResultat.utenOveropp();
    }
}
