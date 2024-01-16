package no.nav.k9.sak.inngangsvilkår.medlemskap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
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
        var vurderingsperiodeMedGrunnlag = Map.of(LocalDate.now().minusDays(10), lagGrunnlag(),
            LocalDate.now().minusDays(5), lagGrunnlag(),
            LocalDate.now().minusDays(1), lagGrunnlag());
        var grunnlagOgPerioder = new GrunnlagOgPerioder(perioderTilVurdering, vurderingsperiodeMedGrunnlag, new TreeSet<>());
        var resultat = tjeneste.vurderPerioderMedForlengelse(grunnlagOgPerioder, 1L);

        assertThat(resultat.getVurderinger()).containsKeys(vurderingsperiodeMedGrunnlag.keySet().toArray(new LocalDate[0]));
    }

    private static MedlemskapsvilkårGrunnlag lagGrunnlag() {
        var medlemskapsvilkårGrunnlag = new MedlemskapsvilkårGrunnlag(true, PersonStatusType.BOSA, true, true);
        medlemskapsvilkårGrunnlag.setBrukerAvklartBosatt(true);
        return medlemskapsvilkårGrunnlag;
    }
}
