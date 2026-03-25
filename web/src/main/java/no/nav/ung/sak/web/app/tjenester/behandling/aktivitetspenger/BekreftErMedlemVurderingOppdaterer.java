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
import no.nav.ung.sak.kontrakt.aktivitetspenger.BekreftErMedlemVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftErMedlemVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftErMedlemVurderingOppdaterer implements AksjonspunktOppdaterer<BekreftErMedlemVurderingDto> {

    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    @Inject
    public BekreftErMedlemVurderingOppdaterer(@Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @Override
    public OppdateringResultat oppdater(BekreftErMedlemVurderingDto dto, AksjonspunktOppdaterParameter param) {
        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(param.getRef().getFagsakYtelseType(), param.getRef().getBehandlingType());

        var resultatBuilder = param.getVilkårResultatBuilder();
        var forutgåendeMedlemskapBuilder = resultatBuilder.hentBuilderFor(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        Utfall utfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        Avslagsårsak avslagsårsak = dto.getAvslagsårsak() != null ? mapAvslagsårsak(dto.getAvslagsårsak()) : null;

        perioderTilVurdering.stream()
            .map(periode -> forutgåendeMedlemskapBuilder
                .hentBuilderFor(periode)
                .medUtfall(utfall)
                .medAvslagsårsak(avslagsårsak)
                .medRegelInput("TODO"))
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
