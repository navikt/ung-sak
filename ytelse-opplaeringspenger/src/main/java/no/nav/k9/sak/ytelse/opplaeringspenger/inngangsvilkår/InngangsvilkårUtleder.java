package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår;

import static java.util.Arrays.asList;
import static no.nav.k9.kodeverk.vilkår.VilkårType.ALDERSVILKÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON;
import static no.nav.k9.kodeverk.vilkår.VilkårType.LANGVARIG_SYKDOM;
import static no.nav.k9.kodeverk.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.k9.kodeverk.vilkår.VilkårType.NØDVENDIG_OPPLÆRING;
import static no.nav.k9.kodeverk.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.OPPTJENINGSVILKÅRET;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
@BehandlingTypeRef
public class InngangsvilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> YTELSE_VILKÅR = asList(
        ALDERSVILKÅR,
        LANGVARIG_SYKDOM,
        NØDVENDIG_OPPLÆRING,
        GODKJENT_OPPLÆRINGSINSTITUSJON,
        MEDLEMSKAPSVILKÅRET,
        OPPTJENINGSPERIODEVILKÅR,
        OPPTJENINGSVILKÅRET,
        BEREGNINGSGRUNNLAGVILKÅR);

    public InngangsvilkårUtleder() {
    }

    @Override
    public UtledeteVilkår utledVilkår(Behandling behandling) {
        return new UtledeteVilkår(null, YTELSE_VILKÅR);
    }

}
