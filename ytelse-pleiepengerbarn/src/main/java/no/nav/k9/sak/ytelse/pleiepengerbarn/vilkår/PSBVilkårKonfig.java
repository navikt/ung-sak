package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
class PSBVilkårKonfig implements PleiepengerVilkårKonfig {

    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();
    private VilkårUtleder vilkårUtleder;
    private SøktePerioder søktePerioder;

    PSBVilkårKonfig() {
        // CDI
    }

    @Inject
    public PSBVilkårKonfig(@FagsakYtelseTypeRef("PSB") VilkårUtleder vilkårUtleder,
                           BehandlingRepository behandlingRepository,
                           BasisPersonopplysningTjeneste basisPersonopplysningsTjeneste,
                           PersoninfoAdapter personinfoAdapter,
                           SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                           @KonfigVerdi(value = "ENABLE_RELEVANT_SYKDOMSPERIODE", defaultVerdi = "false") boolean brukRelevantPeriode) {
        this.vilkårUtleder = vilkårUtleder;


        søktePerioder = new SøktePerioder(søknadsperiodeTjeneste);
        var maksSøktePeriode = new MaksSøktePeriode(søknadsperiodeTjeneste);

        vilkårsPeriodisering.put(VilkårType.MEDLEMSKAPSVILKÅRET, maksSøktePeriode);
        vilkårsPeriodisering.put(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, PleietrengendeAlderPeriode.under18(basisPersonopplysningsTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste, brukRelevantPeriode));
        vilkårsPeriodisering.put(VilkårType.MEDISINSKEVILKÅR_18_ÅR, PleietrengendeAlderPeriode.overEllerLik18(basisPersonopplysningsTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste, brukRelevantPeriode));
    }

    @Override
    public Map<VilkårType, VilkårsPeriodiseringsFunksjon> getVilkårsPeriodisering() {
        return vilkårsPeriodisering;
    }

    @Override
    public VilkårsPeriodiseringsFunksjon getDefaultVilkårsperiodisering() {
        return søktePerioder;
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        return Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR);
    }

    @Override
    public VilkårUtleder getVilkårUtleder() {
        return vilkårUtleder;
    }
}
