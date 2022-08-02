package no.nav.k9.sak.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.inngangsvilkår.InngangsvilkårTjeneste;
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.OverstyringOmsorgenForDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringOmsorgenForDto.class, adapter = Overstyringshåndterer.class)
public class OmsorgenForOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringOmsorgenForDto> {

    OmsorgenForOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public OmsorgenForOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                            InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter,
            AksjonspunktDefinisjon.OVERSTYRING_AV_OMSORGEN_FOR,
            VilkårType.OMSORGEN_FOR,
            inngangsvilkårTjeneste);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringOmsorgenForDto dto) {
        lagHistorikkInnslagForOverstyrtVilkår(dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_OMSORGEN_FOR);
    }
}
