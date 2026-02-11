package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.aktivitetspenger.VurderBehovForBistandDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.tid.DatoIntervallEntitet;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderBehovForBistandDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderBehovForBistandOppdaterer implements AksjonspunktOppdaterer<VurderBehovForBistandDto> {

    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;


    @Inject
    public VurderBehovForBistandOppdaterer(BehandlingRepository behandlingRepository,
                                           @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @Override
    public OppdateringResultat oppdater(VurderBehovForBistandDto dto, AksjonspunktOppdaterParameter param) {
        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(param.getRef().getFagsakYtelseType(), param.getRef().getBehandlingType());

        var resultatBuilder = param.getVilkårResultatBuilder();
        var bistandsvilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.BISTANDSVILKÅR);

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.BISTANDSVILKÅR);
        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            //FIXME AKT implementer regel for automatisk behandling eller opprett aksjonspunkt her
            var periodeBuilderOppfylt = bistandsvilkårBuilder.hentBuilderFor(periode).medUtfall(Utfall.OPPFYLT).medRegelInput("TODO");
            bistandsvilkårBuilder.leggTil(periodeBuilderOppfylt);
        }

        resultatBuilder.leggTil(bistandsvilkårBuilder);

        return OppdateringResultat.nyttResultat();
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, fagsakYtelseType, behandlingType);
    }
}
