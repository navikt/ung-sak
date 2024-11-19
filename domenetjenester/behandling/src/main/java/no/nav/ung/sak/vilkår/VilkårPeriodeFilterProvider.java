package no.nav.ung.sak.vilkår;



import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.perioder.ForlengelseTjeneste;

@Dependent
public class VilkårPeriodeFilterProvider {

    private final FagsakRepository fagsakRepository;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final Instance<ForlengelseTjeneste> forlengelseTjenester;
    private final Instance<EndringIUttakPeriodeUtleder> endringIUttakPeriodeUtledere;


    @Inject
    public VilkårPeriodeFilterProvider(FagsakRepository fagsakRepository,
                                       VilkårResultatRepository vilkårResultatRepository,
                                       @Any Instance<ForlengelseTjeneste> forlengelseTjenester,
                                       @Any Instance<EndringIUttakPeriodeUtleder> endringIUttakPeriodeUtledere) {
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forlengelseTjenester = forlengelseTjenester;
        this.endringIUttakPeriodeUtledere = endringIUttakPeriodeUtledere;
    }

    public VilkårPeriodeFilter getFilter(BehandlingReferanse referanse) {
        return new VilkårPeriodeFilter(
            referanse,
            vilkårResultatRepository,
            getForlengelsetjeneste(referanse.getFagsakYtelseType(), referanse.getBehandlingType()),
            EndringIUttakPeriodeUtleder.finnTjeneste(endringIUttakPeriodeUtledere, referanse.getFagsakYtelseType()));
    };

    private ForlengelseTjeneste getForlengelsetjeneste(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(ForlengelseTjeneste.class, forlengelseTjenester, ytelseType, behandlingType)
            .orElseThrow(() -> new UnsupportedOperationException(
                "ForlengelseTjeneste ikke implementert for ytelse [" + ytelseType + "], behandlingtype [" + behandlingType + "]"));
    }

}
