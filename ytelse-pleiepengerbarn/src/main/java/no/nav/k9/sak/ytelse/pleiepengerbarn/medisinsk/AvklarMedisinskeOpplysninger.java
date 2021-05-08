package no.nav.k9.sak.ytelse.pleiepengerbarn.medisinsk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.AvklarMedisinskeOpplysningerDto;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomAksjonspunkt;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarMedisinskeOpplysningerDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarMedisinskeOpplysninger implements AksjonspunktOppdaterer<AvklarMedisinskeOpplysningerDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private SykdomVurderingService sykdomVurderingService;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    
    AvklarMedisinskeOpplysninger() {
        // for CDI proxy
    }
    
    @Inject
    public AvklarMedisinskeOpplysninger(HistorikkTjenesteAdapter historikkTjenesteAdapter, SykdomVurderingService sykdomVurderingService,
            BehandlingRepository behandlingRepository, VilkårResultatRepository vilkårResultatRepository) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.sykdomVurderingService = sykdomVurderingService;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarMedisinskeOpplysningerDto dto, AksjonspunktOppdaterParameter param) {        
        if (dto.isIkkeVentPåGodkjentLegeerklæring()) {
            final Behandling behandling = behandlingRepository.hentBehandling(param.getRef().getBehandlingId());
            final SykdomAksjonspunkt sykdomAksjonspunkt = sykdomVurderingService.vurderAksjonspunkt(behandling);
            if (!sykdomAksjonspunkt.isManglerGodkjentLegeerklæring()) {
                throw new IllegalStateException("Saksbehandler har bedt om å avslå sykdomsvilkåret grunnet manglende legeerklæring, selv om en godkjent legeerklæring allerede ligger inne.");
            }
            if (sykdomAksjonspunkt.isHarUklassifiserteDokumenter()) {
                throw new IllegalStateException("Det finnes uklassifiserte dokumenter på behandlingen. Disse må klassifiseres før man kan gi avslag grunnet manglende godkjent legeerklæring.");
            }
            
            lagHistorikkinnslag(param, "Sykdom manuelt behandlet: Mangler godkjent legeerklæring.");
            
            oppdaterMedIkkeOppfylt(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, param, behandling);
            oppdaterMedIkkeOppfylt(VilkårType.MEDISINSKEVILKÅR_18_ÅR, param, behandling);
            return OppdateringResultat.utenTransisjon()
                    .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
                    .medTotrinn()
                    .build()
                    ;
        }

        lagHistorikkinnslag(param, "Sykdom manuelt behandlet.");
        
        final OppdateringResultat resultat = OppdateringResultat.utenOveropp();
        resultat.skalRekjøreSteg();
        resultat.setSteg(BehandlingStegType.VURDER_MEDISINSKVILKÅR);
        
        return resultat;
    }

    private void oppdaterMedIkkeOppfylt(VilkårType vilkårType, AksjonspunktOppdaterParameter param, final Behandling behandling) {
        var vilkårene = vilkårResultatRepository.hent(behandling.getId());
        var timeline = vilkårene.getVilkårTimeline(vilkårType);
        if (!timeline.isEmpty()) {
            var builder = param.getVilkårResultatBuilder();
            var vilkårBuilder = builder.hentBuilderFor(vilkårType);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(timeline.getMinLocalDate(), timeline.getMaxLocalDate())
                .medUtfallManuell(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(Avslagsårsak.DOKUMENTASJON_IKKE_FRA_RETT_ORGAN));
            builder.leggTil(vilkårBuilder);
        }
    }
    
    private void lagHistorikkinnslag(AksjonspunktOppdaterParameter param, String begrunnelse) {
        historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_MEDISINSK)
            .medBegrunnelse(begrunnelse);
        historikkTjenesteAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
    }
}
