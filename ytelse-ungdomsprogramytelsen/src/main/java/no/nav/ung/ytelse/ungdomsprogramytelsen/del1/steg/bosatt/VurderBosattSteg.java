package no.nav.ung.ytelse.ungdomsprogramytelsen.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.ytelse.ungdomsprogramytelsen.del1.steg.aldersvilkår.VurderAldersVilkårTjeneste;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.ALDERSVILKÅRET;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_BOSTED;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_BOSTED)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderBosattSteg implements BehandlingSteg {

    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    VurderBosattSteg() {
        // for proxy
    }

    @Inject
    public VurderBosattSteg(@Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                            BehandlingRepository behandlingRepository,
                            VilkårResultatRepository vilkårResultatRepository) {
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.BOSTEDSVILKÅR);

        //TODO om det er tidligere vilkår, og disse har endt med avslag, så trener vi ikke å vurdere her også
        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandling);

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BOSTEDSVILKÅR);
        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            //FIXME AKT implementer regel for automatisk behandling eller opprett aksjonspunkt her
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode).medUtfall(Utfall.OPPFYLT).medRegelInput("TODO"));
        }
        resultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), resultatBuilder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
    }
}
