package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Bosteder;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

import java.util.Collections;
import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class ForutgåendeMedlemskapsvilkårSteg implements BehandlingSteg {

    private final VilkårResultatRepository vilkårResultatRepository;
    private final ForutgåendeMedlemskapTjeneste forutgåendeMedlemskapTjeneste;
    private final BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;


    @Inject
    public ForutgåendeMedlemskapsvilkårSteg(VilkårResultatRepository vilkårResultatRepository,
                                            ForutgåendeMedlemskapTjeneste forutgåendeMedlemskapTjeneste,
                                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                            BehandlingRepository behandlingRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forutgåendeMedlemskapTjeneste = forutgåendeMedlemskapTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var vilkårOpt = vilkårene.getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        var harAvklartMedlemskap = !vilkårOpt.map(vilkår -> vilkår.getPerioder().stream().anyMatch(periode -> Utfall.IKKE_VURDERT.equals(periode.getUtfall()))).orElse(true);

        if (harAvklartMedlemskap) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var forutgåendeBosteder = forutgåendeMedlemskapTjeneste.utledForutgåendeBosteder(kontekst.getFagsakId(), kontekst.getBehandlingId());

        //Søknad finnes ikke
        if (forutgåendeBosteder.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP));
        }

        var aksjonspunkter = vurderForutgåendeMedlemskap(forutgåendeBosteder.orElseThrow());

        if (aksjonspunkter.isEmpty()) {
            oppfyllVilkår(vilkårene, kontekst.getBehandlingId());

            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    private void oppfyllVilkår(Vilkårene vilkårene, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var perioderTilVurdering = getPerioderTilVurderingTjeneste(behandling.getFagsakYtelseType(), behandling.getType())
            .utled(behandlingId, VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        perioderTilVurdering.stream()
            .map(it -> vilkårBuilder.hentBuilderFor(it)
                .medUtfall(Utfall.OPPFYLT)
                .medAvslagsårsak(null)
                .medRegelInput("TODO fra steg"))
            .forEach(vilkårBuilder::leggTil);

        vilkårResultatBuilder.leggTil(vilkårBuilder);

        vilkårResultatRepository.lagre(behandlingId, vilkårResultatBuilder.build());

    }


    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, fagsakYtelseType, behandlingType);
    }


    static List<AksjonspunktDefinisjon> vurderForutgåendeMedlemskap(Bosteder forutgåendeBosteder) {
        if (forutgåendeBosteder.getPerioder().isEmpty()) {
            return Collections.emptyList();
        }

        boolean alleLandGyldige = forutgåendeBosteder.getPerioder().entrySet().stream()
            .allMatch(entry -> TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
                entry.getValue().getLand(),
                entry.getKey().getFraOgMed()
            ));

        if (alleLandGyldige) {
            return Collections.emptyList();
        }
        return List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }
}
