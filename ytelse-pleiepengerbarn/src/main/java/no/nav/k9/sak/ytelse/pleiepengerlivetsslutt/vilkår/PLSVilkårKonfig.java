package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.vilkår;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.MaksSøktePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleiepengerVilkårKonfig;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SøktePerioder;

@FagsakYtelseTypeRef("PPN")
@ApplicationScoped
class PLSVilkårKonfig implements PleiepengerVilkårKonfig {

    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();
    private VilkårUtleder vilkårUtleder;
    private SøktePerioder søktePerioder;

    PLSVilkårKonfig() {
        // CDI
    }

    @Inject
    public PLSVilkårKonfig(@FagsakYtelseTypeRef("PPN") VilkårUtleder vilkårUtleder,
                           SøknadsperiodeTjeneste søknadsperiodeTjeneste) {

        this.vilkårUtleder = vilkårUtleder;
        søktePerioder = new SøktePerioder(søknadsperiodeTjeneste);
        var maksSøktePeriode = new MaksSøktePeriode(søknadsperiodeTjeneste);

        vilkårsPeriodisering.put(VilkårType.MEDLEMSKAPSVILKÅRET, maksSøktePeriode);
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
        return Set.of(VilkårType.I_LIVETS_SLUTTFASE);
    }

    @Override
    public VilkårUtleder getVilkårUtleder() {
        return vilkårUtleder;
    }
}
