package no.nav.k9.sak.web.app.tjenester.behandling.medlem;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.medlem.OverstyringMedlemskapsvilkåretLøpendeDto;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringMedlemskapsvilkåretLøpendeDto.class, adapter = Overstyringshåndterer.class)
public class MedlemskapsvilkåretLøpendeOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringMedlemskapsvilkåretLøpendeDto> {

    private VilkårResultatRepository vilkårResultatRepository;

    MedlemskapsvilkåretLøpendeOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public MedlemskapsvilkåretLøpendeOverstyringshåndterer(BehandlingRepositoryProvider repositoryProvider,
                                                           HistorikkTjenesteAdapter historikkAdapter) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_LØPENDE);
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringMedlemskapsvilkåretLøpendeDto dto) {
        HistorikkEndretFeltVerdiType tilVerdi = dto.getErVilkarOk() ? HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT;
        HistorikkEndretFeltVerdiType fraVerdi = dto.getErVilkarOk() ? HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT;

        getHistorikkAdapter().tekstBuilder()
            .medHendelse(HistorikkinnslagType.OVERSTYRT)
            .medBegrunnelse(dto.getBegrunnelse())
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP_LØPENDE)
            .medEndretFelt(HistorikkEndretFeltType.OVERSTYRT_VURDERING, fraVerdi, tilVerdi);
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringMedlemskapsvilkåretLøpendeDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        final var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        VilkårResultatBuilder vilkårBuilder = Vilkårene.builderFraEksisterende(vilkårene);
        if (dto.getErVilkarOk()) {
            final var builder = vilkårBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
            builder.leggTil(builder.hentBuilderFor(dto.getOverstryingsdato(), Tid.TIDENES_ENDE) // FIXME (k9) : få periode fra dto
                .medUtfallOverstyrt(Utfall.OPPFYLT));
            vilkårBuilder.leggTil(builder);
        } else {
            Avslagsårsak avslagsårsak = Avslagsårsak.fraKode(dto.getAvslagskode());
            final var builder = vilkårBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
            builder.leggTil(builder.hentBuilderFor(dto.getOverstryingsdato(), Tid.TIDENES_ENDE) // FIXME (k9) : få periode fra dto
                .medUtfallOverstyrt(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(avslagsårsak));
            vilkårBuilder.leggTil(builder);
        }
        final var vilkårResultat = vilkårBuilder.build();
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultat);
        return OppdateringResultat.utenOveropp();
    }
}
