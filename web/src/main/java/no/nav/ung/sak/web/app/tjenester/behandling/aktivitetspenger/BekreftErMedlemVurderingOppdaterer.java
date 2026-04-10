package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.kontrakt.aktivitetspenger.BekreftErMedlemVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.medlemskap.ForutgåendeMedlemskapTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftErMedlemVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftErMedlemVurderingOppdaterer implements AksjonspunktOppdaterer<BekreftErMedlemVurderingDto> {

    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private final ForutgåendeMedlemskapTjeneste forutgåendeMedlemskapTjeneste;

    @Inject
    public BekreftErMedlemVurderingOppdaterer(@Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                             ForutgåendeMedlemskapTjeneste forutgåendeMedlemskapTjeneste) {
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.forutgåendeMedlemskapTjeneste = forutgåendeMedlemskapTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftErMedlemVurderingDto dto, AksjonspunktOppdaterParameter param) {
        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(param.getRef().getFagsakYtelseType(), param.getRef().getBehandlingType());

        var resultatBuilder = param.getVilkårResultatBuilder();
        var forutgåendeMedlemskapBuilder = resultatBuilder.hentBuilderFor(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        Utfall utfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        Avslagsårsak avslagsårsak = utfall == Utfall.IKKE_OPPFYLT ? mapAvslagsårsak(dto.getAvslagsårsak()) : null;

        var bostederDto = forutgåendeMedlemskapTjeneste.hentBostederSomDto(param.getBehandlingId());
        String regelInput = new VilkårJsonObjectMapper().writeValueAsString(bostederDto);

        perioderTilVurdering.stream()
            .map(periode -> forutgåendeMedlemskapBuilder
                .hentBuilderFor(periode)
                .medUtfallManuell(utfall)
                .medAvslagsårsak(avslagsårsak)
                .medRegelInput(regelInput)
                .medBegrunnelse(dto.getBegrunnelse())
            )
            .forEach(forutgåendeMedlemskapBuilder::leggTil);

        resultatBuilder.leggTil(forutgåendeMedlemskapBuilder);

        return OppdateringResultat.nyttResultat();
    }

    private Avslagsårsak mapAvslagsårsak(MedlemskapAvslagsÅrsakType medlemskapAvslagsÅrsakType) {
        return switch (medlemskapAvslagsÅrsakType) {
            case SØKER_IKKE_MEDLEM -> Avslagsårsak.SØKER_ER_IKKE_MEDLEM;
        };
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, fagsakYtelseType, behandlingType);
    }
}
