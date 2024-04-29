package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import static java.util.Arrays.asList;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;

@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
@BehandlingTypeRef
@ApplicationScoped
public class UtvidetRettInngangsvilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> YTELSE_VILKÅR = asList(
        VilkårType.OMSORGEN_FOR,
        VilkårType.UTVIDETRETT);

    @Override
    public UtledeteVilkår utledVilkår(BehandlingReferanse referanse) {
        return new UtledeteVilkår(null, YTELSE_VILKÅR);
    }

}
