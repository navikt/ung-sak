package no.nav.k9.sak.ytelse.opplaeringspenger.vilkår;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.MaksSøktePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleiepengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.RevurderingPerioderTjeneste;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
public class OLPVilkårsPerioderTilVurderingTjeneste extends PleiepengerVilkårsPerioderTilVurderingTjeneste {

    // MERK: DERSOM PPN og PSB begynner å divergere, må det vurderes å erstatte arv med komposisjon

    OLPVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public OLPVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER) VilkårUtleder vilkårUtleder,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  SykdomGrunnlagTjeneste sykdomGrunnlagService,
                                                  ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste,
                                                  EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                                  BasisPersonopplysningTjeneste basisPersonopplysningsTjeneste,
                                                  RevurderingPerioderTjeneste revurderingPerioderTjeneste,
                                                  PersoninfoAdapter personinfoAdapter,
                                                  SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                  UttakTjeneste uttakTjeneste,
                                                  UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                                  @KonfigVerdi(value = "ENABLE_RELEVANT_SYKDOMSPERIODE", defaultVerdi = "false") boolean brukRelevantPeriode) {

        super(vilkårUtleder
            , Map.of(VilkårType.MEDLEMSKAPSVILKÅRET, new MaksSøktePeriode(søknadsperiodeTjeneste),
                VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, PleietrengendeAlderPeriode.under18(basisPersonopplysningsTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste, brukRelevantPeriode),
                VilkårType.MEDISINSKEVILKÅR_18_ÅR, PleietrengendeAlderPeriode.overEllerLik18(basisPersonopplysningsTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste, brukRelevantPeriode))
            , vilkårResultatRepository
            , behandlingRepository
            , sykdomGrunnlagService
            , etablertTilsynTjeneste
            , endringUnntakEtablertTilsynTjeneste
            , revurderingPerioderTjeneste
            , søknadsperiodeTjeneste
            , utsattBehandlingAvPeriodeRepository
            , uttakTjeneste
        );
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        return Set.of(VilkårType.LANGVARIG_SYKDOM); // TODO: Endre til kursvilkåret
    }
}
