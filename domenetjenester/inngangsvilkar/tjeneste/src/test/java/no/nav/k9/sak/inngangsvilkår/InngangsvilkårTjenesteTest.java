package no.nav.k9.sak.inngangsvilkår;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class InngangsvilkårTjenesteTest {

    @Inject
    InngangsvilkårTjeneste inngangsvilkårTjeneste;

    @Test
    public void skal_slå_opp_inngangsvilkår_meg_fagsak_ytelse_type_der_inngangsvilkåret_er_forskjellig_pr_ytelse(){
        sjekkVilkårKonfigurasjon(VilkårType.OPPTJENINGSPERIODEVILKÅR, FagsakYtelseType.SVANGERSKAPSPENGER, true);
        sjekkVilkårKonfigurasjon(VilkårType.OPPTJENINGSPERIODEVILKÅR, FagsakYtelseType.FORELDREPENGER, true);
    }

    private void sjekkVilkårKonfigurasjon(VilkårType vilkårType, FagsakYtelseType foreldrepenger, boolean sjekkForFagYtelseType) {
        Inngangsvilkår vilkår = inngangsvilkårTjeneste.finnVilkår(vilkårType, foreldrepenger);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår).isSameAs(inngangsvilkårTjeneste.finnVilkår(vilkårType, foreldrepenger));
        assertThat(vilkår.getClass()).hasAnnotation(ApplicationScoped.class);
        assertThat(vilkår.getClass()).hasAnnotation(VilkårTypeRef.class);
        if (sjekkForFagYtelseType) {
            assertThat(vilkår.getClass()).hasAnnotation(FagsakYtelseTypeRef.class);
        }
    }
}
