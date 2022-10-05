package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.RevurderingPerioderTjeneste;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@BehandlingTypeRef
@ApplicationScoped
public class PSBVilkårsPerioderTilVurderingTjeneste extends PleiepengerVilkårsPerioderTilVurderingTjeneste {

    // MERK: DERSOM PPN og PSB begynner å divergere, må det vurderes å erstatte arv med komposisjon

    PSBVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public PSBVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) VilkårUtleder vilkårUtleder,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste,
                                                  ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste,
                                                  EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                                  BasisPersonopplysningTjeneste basisPersonopplysningsTjeneste,
                                                  RevurderingPerioderTjeneste revurderingPerioderTjeneste,
                                                  PersoninfoAdapter personinfoAdapter,
                                                  SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                  UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                                  @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) PleiepengerEndringIUttakPeriodeUtleder endringIUttakPeriodeUtleder) {

        super(vilkårUtleder
            , Map.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, PleietrengendeAlderPeriode.under18(basisPersonopplysningsTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste, true),
                VilkårType.MEDISINSKEVILKÅR_18_ÅR, PleietrengendeAlderPeriode.overEllerLik18(basisPersonopplysningsTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste, true))
            , vilkårResultatRepository
            , behandlingRepository
            , medisinskGrunnlagTjeneste
            , etablertTilsynTjeneste
            , endringUnntakEtablertTilsynTjeneste
            , revurderingPerioderTjeneste
            , søknadsperiodeTjeneste
            , utsattBehandlingAvPeriodeRepository
            , endringIUttakPeriodeUtleder
        );
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        return Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR);
    }
}
