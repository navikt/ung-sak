package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.kontrakt.søker.OverstyringSokersOpplysingspliktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringSokersOpplysingspliktDto.class, adapter = Overstyringshåndterer.class)
public class SøkersOpplysningspliktOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringSokersOpplysingspliktDto> {

    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    SøkersOpplysningspliktOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public SøkersOpplysningspliktOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                       InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST);
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;

    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringSokersOpplysingspliktDto dto) {
        leggTilEndretFeltIHistorikkInnslag(dto.getBegrunnelse(), dto.getErVilkarOk());
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringSokersOpplysingspliktDto dto, Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {

        Utfall utfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        inngangsvilkårTjeneste.overstyrAksjonspunktForSøkersopplysningsplikt(behandling.getId(), utfall, kontekst);

        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        if (Utfall.OPPFYLT.equals(utfall)) {
            // Rydd opp i aksjonspunkt tidligere opprettet i forbindelse med overstyring av søkers opplysningsplikt
            behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL)
                .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
            return OppdateringResultat.utenOveropp();
        } else {
            builder.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR).medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL, AksjonspunktStatus.OPPRETTET);
        }
        return builder.build();
    }

    private void leggTilEndretFeltIHistorikkInnslag(String begrunnelse, boolean vilkårOppfylt) {
        HistorikkEndretFeltVerdiType tilVerdi = vilkårOppfylt ? HistorikkEndretFeltVerdiType.OPPFYLT : HistorikkEndretFeltVerdiType.IKKE_OPPFYLT;

        HistorikkInnslagTekstBuilder tekstBuilder = getHistorikkAdapter().tekstBuilder();
        if (begrunnelse != null) {
            tekstBuilder.medBegrunnelse(begrunnelse);
        }
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.SOKERSOPPLYSNINGSPLIKT, null, tilVerdi)
            .medSkjermlenke(SkjermlenkeType.OPPLYSNINGSPLIKT);

    }
}
