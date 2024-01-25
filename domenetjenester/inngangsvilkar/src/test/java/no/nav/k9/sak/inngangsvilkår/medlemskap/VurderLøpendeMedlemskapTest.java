package no.nav.k9.sak.inngangsvilkår.medlemskap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.medlemskap.regelmodell.MedlemskapsvilkårGrunnlag;
import no.nav.k9.sak.inngangsvilkår.medlemskap.regelmodell.PersonStatusType;

class VurderLøpendeMedlemskapTest {

    private VurderLøpendeMedlemskap tjeneste = new VurderLøpendeMedlemskap();

    @Test
    void utledningavperiodeflyttes() {
        var perioderTilVurdering = new TreeSet<DatoIntervallEntitet>();
        perioderTilVurdering.add(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now()));
        var vurderingsperiodeMedGrunnlag = Map.of(
            LocalDate.now().minusDays(10), lagGrunnlagMedlem(),
            LocalDate.now().minusDays(5), lagGrunnlagMedlem(),
            LocalDate.now().minusDays(1), lagGrunnlagMedlem());
        var grunnlagOgPerioder = new GrunnlagOgPerioder(perioderTilVurdering, vurderingsperiodeMedGrunnlag, new TreeSet<>());
        var resultat = tjeneste.vurderPerioderMedForlengelse(grunnlagOgPerioder);

        assertThat(resultat.getVurderinger()).containsKeys(vurderingsperiodeMedGrunnlag.keySet().toArray(new LocalDate[0]));
    }

    @Test
    void lagrerIkkeFlereVurderingerForSammePeriodeDersomIkkeOppfylt() {
        var perioderTilVurdering = new TreeSet<DatoIntervallEntitet>();
        perioderTilVurdering.add(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now()));
        var vurderingsperiodeMedGrunnlag = new TreeMap<>(Map.of(
            LocalDate.now().minusDays(10), lagGrunnlagMedlem(),
            LocalDate.now().minusDays(5), lagGrunnlagIkkeMedlem(),
            LocalDate.now().minusDays(2), lagGrunnlagMedlem(),
            LocalDate.now().minusDays(1), lagGrunnlagIkkeMedlem()));
        var grunnlagOgPerioder = new GrunnlagOgPerioder(perioderTilVurdering, vurderingsperiodeMedGrunnlag, new TreeSet<>());
        var resultat = tjeneste.vurderPerioderMedForlengelse(grunnlagOgPerioder);

        assertThat(resultat.getVurderinger()).containsKey(LocalDate.now().minusDays(10));
        assertThat(resultat.getVurderinger()).containsKey(LocalDate.now().minusDays(5));
        assertThat(resultat.getVurderinger()).doesNotContainKey(LocalDate.now().minusDays(2));
        assertThat(resultat.getVurderinger()).doesNotContainKey(LocalDate.now().minusDays(1));
    }

    @Test
    void lagrerFlereIkkeOppfyltVurderingerDersomFlerePerioder() {
        var perioderTilVurdering = new TreeSet<DatoIntervallEntitet>();
        perioderTilVurdering.add(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now()));
        perioderTilVurdering.add(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(20), LocalDate.now().minusDays(12)));
        var vurderingsperiodeMedGrunnlag = Map.of(
            LocalDate.now().minusDays(20), lagGrunnlagIkkeMedlem(),
            LocalDate.now().minusDays(10), lagGrunnlagIkkeMedlem());
        var grunnlagOgPerioder = new GrunnlagOgPerioder(perioderTilVurdering, vurderingsperiodeMedGrunnlag, new TreeSet<>());
        var resultat = tjeneste.vurderPerioderMedForlengelse(grunnlagOgPerioder);

        assertThat(resultat.getVurderinger()).containsKeys(vurderingsperiodeMedGrunnlag.keySet().toArray(new LocalDate[0]));
    }

    private static MedlemskapsvilkårGrunnlag lagGrunnlagMedlem() {
        var medlemskapsvilkårGrunnlag = new MedlemskapsvilkårGrunnlag(true, PersonStatusType.BOSA, true, true);
        medlemskapsvilkårGrunnlag.setBrukerAvklartBosatt(true);
        return medlemskapsvilkårGrunnlag;
    }

    private static MedlemskapsvilkårGrunnlag lagGrunnlagIkkeMedlem() {
        var medlemskapsvilkårGrunnlag = new MedlemskapsvilkårGrunnlag(false, PersonStatusType.UTVA, false, false);
        medlemskapsvilkårGrunnlag.setBrukerAvklartBosatt(false);
        return medlemskapsvilkårGrunnlag;
    }
}
