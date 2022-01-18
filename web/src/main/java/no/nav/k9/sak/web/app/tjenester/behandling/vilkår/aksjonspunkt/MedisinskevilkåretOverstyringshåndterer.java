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
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.OverstyringMedisinskevilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringMedisinskevilkåretDto.class, adapter = Overstyringshåndterer.class)
public class MedisinskevilkåretOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringMedisinskevilkåretDto> {

    MedisinskevilkåretOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public MedisinskevilkåretOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                   InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter,
            AksjonspunktDefinisjon.OVERSTYRING_AV_MEDISINSKESVILKÅRET_UNDER_18,
            VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR,
            inngangsvilkårTjeneste);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringMedisinskevilkåretDto dto) {
        lagHistorikkInnslagForOverstyrtVilkår(dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_MEDISINSK);
    }
}
