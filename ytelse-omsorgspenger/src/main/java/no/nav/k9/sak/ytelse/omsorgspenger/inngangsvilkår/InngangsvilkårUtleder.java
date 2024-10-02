package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår;

import static java.util.Arrays.asList;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.k9.kodeverk.vilkår.VilkårType.OMSORGEN_FOR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.OPPTJENINGSVILKÅRET;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;

@FagsakYtelseTypeRef(OMSORGSPENGER)
@BehandlingTypeRef
@ApplicationScoped
public class InngangsvilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> YTELSE_VILKÅR = asList(
        MEDLEMSKAPSVILKÅRET,
        OPPTJENINGSPERIODEVILKÅR,
        OPPTJENINGSVILKÅRET,
        BEREGNINGSGRUNNLAGVILKÅR);
    private static final List<VilkårType> YTELSE_VILKÅR_POST_2022 = asList(
        MEDLEMSKAPSVILKÅRET,
        OMSORGEN_FOR,
        OPPTJENINGSPERIODEVILKÅR,
        OPPTJENINGSVILKÅRET,
        BEREGNINGSGRUNNLAGVILKÅR);
    private Long sisteÅrUtenOmsorg;

    public InngangsvilkårUtleder() {
    }

    @Inject
    public InngangsvilkårUtleder(@KonfigVerdi(value = "oms.omsorgenfor.aktiv.etter.aar", defaultVerdi = "2022", required = false) Long førsteÅr) {
        this.sisteÅrUtenOmsorg = førsteÅr;
    }

    @Override
    public UtledeteVilkår utledVilkår(BehandlingReferanse referanse) {
        if (referanse.getFagsakPeriode().getFomDato().getYear() > sisteÅrUtenOmsorg) {
            return new UtledeteVilkår(null, YTELSE_VILKÅR_POST_2022);
        }
        return new UtledeteVilkår(null, YTELSE_VILKÅR);
    }

}
