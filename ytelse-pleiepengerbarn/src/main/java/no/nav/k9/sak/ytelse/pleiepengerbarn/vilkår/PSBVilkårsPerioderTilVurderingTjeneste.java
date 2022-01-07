package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.RevurderingPerioderTjeneste;

@FagsakYtelseTypeRef("PSB")
@BehandlingTypeRef
@ApplicationScoped
public class PSBVilkårsPerioderTilVurderingTjeneste extends PleiepengerVilkårsPerioderTilVurderingTjeneste {

    // MERK: DERSOM PPN og PSB begynner å divergere, må det vurderes å erstatte arv med komposisjon

    PSBVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public PSBVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef("PSB") VilkårUtleder vilkårUtleder,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  SykdomGrunnlagService sykdomGrunnlagService,
                                                  ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste,
                                                  EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                                  BasisPersonopplysningTjeneste basisPersonopplysningsTjeneste,
                                                  RevurderingPerioderTjeneste revurderingPerioderTjeneste,
                                                  PersoninfoAdapter personinfoAdapter,
                                                  SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                  UttakTjeneste uttakTjeneste,
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
            , uttakTjeneste
        );
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        return Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR);
    }
}
