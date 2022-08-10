package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår.regelmodell.AldersvilkårBarnVilkårGrunnlag;

@CdiDbAwareTest
class AldersvilkårBarnTjenesteTest {

    @Inject
    private AldersvilkårBarnTjeneste tjeneste;

    @Test
    void skal_innvilge_aldersvilkåret_når_starten_av_vilkårsperioden_er_i_det_året_barnet_fyller_12_år_for_aleneomsorg(){
        LocalDate fødselsdato = LocalDate.of(2010, 6, 1);
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 12, 31), LocalDate.of(fødselsdato.getYear() + 18, 12, 31));
        AldersvilkårBarnVilkårGrunnlag grunnlag = new AldersvilkårBarnVilkårGrunnlag(List.of(fødselsdato), FagsakYtelseType.OMSORGSPENGER_AO, vilkårsperiode);
        VilkårData resultat = tjeneste.vurder(grunnlag);
        Assertions.assertThat(resultat.getUtfallType()).isEqualTo(no.nav.k9.kodeverk.vilkår.Utfall.OPPFYLT);
    }

    @Test
    void skal_sende_til_manuell_vurdering_av_aldersvilkåret_når_starten_av_vilkårsperioden_er_i_det_året_barnet_fyller_13_år_for_aleneomsorg(){
        LocalDate fødselsdato = LocalDate.of(2010, 6, 1);
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2023, 1, 1), LocalDate.of(fødselsdato.getYear() + 18, 12, 31));
        AldersvilkårBarnVilkårGrunnlag grunnlag = new AldersvilkårBarnVilkårGrunnlag(List.of(fødselsdato), FagsakYtelseType.OMSORGSPENGER_AO, vilkårsperiode);
        VilkårData resultat = tjeneste.vurder(grunnlag);
        Assertions.assertThat(resultat.getUtfallType()).isEqualTo(Utfall.IKKE_VURDERT);
        Assertions.assertThat(resultat.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_9015);
    }

    @Test
    void skal_innvilge_aldersvilkåret_når_starten_av_vilkårsperioden_er_i_det_året_ett_av_barna_fyller_12_år_for_midlertidig_alene(){
        LocalDate fødselsdato1 = LocalDate.of(2000, 6, 1);
        LocalDate fødselsdato2 = LocalDate.of(2010, 6, 1);
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 12, 31), LocalDate.of(fødselsdato2.getYear() + 18, 12, 31));
        AldersvilkårBarnVilkårGrunnlag grunnlag = new AldersvilkårBarnVilkårGrunnlag(List.of(fødselsdato1, fødselsdato2), FagsakYtelseType.OMSORGSPENGER_MA, vilkårsperiode);
        VilkårData resultat = tjeneste.vurder(grunnlag);
        Assertions.assertThat(resultat.getUtfallType()).isEqualTo(no.nav.k9.kodeverk.vilkår.Utfall.OPPFYLT);
    }

    @Test
    void skal_sende_til_manuell_vurdering_av_aldersvilkåret_når_starten_av_vilkårsperioden_er_i_et_år_alle_barna_passerer_12_år(){
        LocalDate fødselsdato1 = LocalDate.of(2010, 6, 1);
        LocalDate fødselsdato2 = LocalDate.of(2010, 6, 1);
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2023, 1, 1), LocalDate.of(fødselsdato2.getYear() + 18, 12, 31));
        AldersvilkårBarnVilkårGrunnlag grunnlag = new AldersvilkårBarnVilkårGrunnlag(List.of(fødselsdato1, fødselsdato2), FagsakYtelseType.OMSORGSPENGER_MA, vilkårsperiode);
        VilkårData resultat = tjeneste.vurder(grunnlag);
        Assertions.assertThat(resultat.getUtfallType()).isEqualTo(Utfall.IKKE_VURDERT);
        Assertions.assertThat(resultat.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_9015);
    }

    @Test
    void skal_innvilge_aldersvilkåret_når_starten_av_vilkårsperioden_er_i_det_året_barnet_fyller_18_år_for_kronisk_syk(){
        LocalDate fødselsdato = LocalDate.of(2004, 6, 1);
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 12, 31), LocalDate.of(fødselsdato.getYear() + 18, 12, 31));
        AldersvilkårBarnVilkårGrunnlag grunnlag = new AldersvilkårBarnVilkårGrunnlag(List.of(fødselsdato), FagsakYtelseType.OMSORGSPENGER_KS, vilkårsperiode);
        VilkårData resultat = tjeneste.vurder(grunnlag);
        Assertions.assertThat(resultat.getUtfallType()).isEqualTo(no.nav.k9.kodeverk.vilkår.Utfall.OPPFYLT);
    }

    @Test
    void skal_sende_til_manuell_vurdering_av_aldersvilkåret_når_starten_av_vilkårsperioden_er_i_det_året_barnet_fyller_18_år_for_kronisk_syk(){
        LocalDate fødselsdato = LocalDate.of(2004, 6, 1);
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 1));
        AldersvilkårBarnVilkårGrunnlag grunnlag = new AldersvilkårBarnVilkårGrunnlag(List.of(fødselsdato), FagsakYtelseType.OMSORGSPENGER_KS, vilkårsperiode);
        VilkårData resultat = tjeneste.vurder(grunnlag);
        Assertions.assertThat(resultat.getUtfallType()).isEqualTo(Utfall.IKKE_VURDERT);
        Assertions.assertThat(resultat.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_9015);
    }

}
