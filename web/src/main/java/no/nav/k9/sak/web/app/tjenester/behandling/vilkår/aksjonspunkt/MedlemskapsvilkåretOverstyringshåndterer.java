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
import no.nav.k9.sak.kontrakt.medlem.OverstyringMedlemskapsvilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringMedlemskapsvilkåretDto.class, adapter = Overstyringshåndterer.class)
public class MedlemskapsvilkåretOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringMedlemskapsvilkåretDto> {

    MedlemskapsvilkåretOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public MedlemskapsvilkåretOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                    InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter,
            AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            inngangsvilkårTjeneste);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringMedlemskapsvilkåretDto dto) {
        lagHistorikkInnslagForOverstyrtVilkår(dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP);
    }
}
