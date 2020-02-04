package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class InngangsvilkårTjenesteTest {

    @Inject
    InngangsvilkårTjeneste inngangsvilkårTjeneste;

    @Test
    public void skal_slå_opp_inngangsvilkår() {
        sjekkVilkårKonfigurasjon(VilkårType.MEDLEMSKAPSVILKÅRET, FagsakYtelseType.FORELDREPENGER, false);
    }

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
