package no.nav.k9.sak.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.inngangsvilkår.InngangsvilkårTjeneste;
import no.nav.k9.sak.kontrakt.vilkår.Overstyringk9VilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = Overstyringk9VilkåretDto.class, adapter = Overstyringshåndterer.class)
public class K9VilkåretOverstyringshåndterer extends AbstractOverstyringshåndterer<Overstyringk9VilkåretDto> {

    private InngangsvilkårTjeneste inngangsvilkårTjeneste;
    private AksjonspunktRepository aksjonspunktRepository;

    K9VilkåretOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public K9VilkåretOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                           InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_K9_VILKÅRET);
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(Overstyringk9VilkåretDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var utfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        var periode = dto.getPeriode();

        inngangsvilkårTjeneste.overstyrAksjonspunkt(behandling.getId(), VilkårType.K9_VILKÅRET, utfall, dto.getAvslagskode(),
            kontekst, periode.getFom(), periode.getTom(), dto.getBegrunnelse());

        behandling.setBehandlingResultatType(dto.getBehandlingResultatType());

        return OppdateringResultat.utenOveropp();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, Overstyringk9VilkåretDto dto) {
        // TODO: Legg inn SkjermlenkeType for k9-vilkåret
        lagHistorikkInnslagForOverstyrtVilkår(dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP);
    }
}
