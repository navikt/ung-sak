package no.nav.ung.sak.inngangsvilkår.alder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
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
import no.nav.ung.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.ALDERSVILKÅRET;

@ApplicationScoped
@BehandlingStegRef(value = ALDERSVILKÅRET)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class VurderAldersvilkåretSteg implements BehandlingSteg {

    private final VurderAldersVilkårTjeneste vurderAldersVilkårTjeneste = new VurderAldersVilkårTjeneste();
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    VurderAldersvilkåretSteg() {
        // for proxy
    }

    @Inject
    public VurderAldersvilkåretSteg(@Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                    BehandlingRepository behandlingRepository,
                                    VilkårResultatRepository vilkårResultatRepository,
                                    BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.ALDERSVILKÅR);

        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandling);

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.ALDERSVILKÅR);
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(BehandlingReferanse.fra(behandling));
        var fødselsdato = personopplysningerAggregat.getSøker().getFødselsdato();
        vurderAldersVilkårTjeneste.vurderPerioder(vilkårBuilder, perioderTilVurdering, fødselsdato);
        resultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), resultatBuilder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }
}
