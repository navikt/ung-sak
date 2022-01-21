package no.nav.k9.sak.vilkår;



import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.perioder.ForlengelseTjeneste;

@Dependent
public class VilkårPeriodeFilterProvider {

    private final FagsakRepository fagsakRepository;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final Instance<ForlengelseTjeneste> forlengelseTjenester;


    @Inject
    public VilkårPeriodeFilterProvider(FagsakRepository fagsakRepository,
                                       VilkårResultatRepository vilkårResultatRepository,
                                       @Any Instance<ForlengelseTjeneste> forlengelseTjenester) {
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forlengelseTjenester = forlengelseTjenester;
    }

    public VilkårPeriodeFilter getFilter(BehandlingReferanse referanse, boolean skalMarkereForlengelser) {
        return new VilkårPeriodeFilter(skalMarkereForlengelser, referanse, fagsakRepository, vilkårResultatRepository, getForlengelsetjeneste(referanse.getFagsakYtelseType(), referanse.getBehandlingType()));
    };

    private ForlengelseTjeneste getForlengelsetjeneste(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(ForlengelseTjeneste.class, forlengelseTjenester, ytelseType, behandlingType)
            .orElseThrow(() -> new UnsupportedOperationException(
                "ForlengelseTjeneste ikke implementert for ytelse [" + ytelseType + "], behandlingtype [" + behandlingType + "]"));
    }

}
