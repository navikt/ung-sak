package no.nav.ung.sak.domene.behandling.steg.inngangsvilkår;

import static java.util.Collections.singletonList;
import static no.nav.k9.kodeverk.behandling.BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

@BehandlingStegRef(value = KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class SøkersopplysningspliktVilkårStegImpl extends InngangsvilkårStegImpl {

    private static List<VilkårType> STØTTEDE_VILKÅR = singletonList(
        VilkårType.SØKERSOPPLYSNINGSPLIKT
    );

    @Inject
    public SøkersopplysningspliktVilkårStegImpl(BehandlingRepositoryProvider repositoryProvider, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT);
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }

}
