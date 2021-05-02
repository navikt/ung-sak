package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.AktørId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UnntakEtablertTilsynGrunnlagRepositoryTest {

    @Inject
    private UnntakEtablertTilsynGrunnlagRepository grunnlagRepo;

    @Inject
    private UnntakEtablertTilsynRepository uetRepo;


    @Test
    void lagreOgHentIgjen() {

        var beredskap = new UnntakEtablertTilsyn();
        beredskap.setBeskrivelser(List.of(
            beskrivelse(beredskap, "Please få litt beredskap.", LocalDate.parse("2020-01-01"), LocalDate.parse("2020-02-01"))
        ));
        beredskap.setPerioder(List.of(
            periode(beredskap, LocalDate.parse("2020-01-01"), LocalDate.parse("2020-02-01"), "Det skal du få lille venn.", Resultat.OPPFYLT)
        ));
        uetRepo.lagre(beredskap);

        var uetForPleietrengende = new UnntakEtablertTilsynForPleietrengende(new AktørId(456L));
        uetForPleietrengende.medBeredskap(beredskap);

        grunnlagRepo.lagre(123L, uetForPleietrengende);


        var grunnlag = grunnlagRepo.hent(123L);

        System.out.println(grunnlag);


    }


    private UnntakEtablertTilsynPeriode periode(UnntakEtablertTilsyn unntakEtablertTilsyn, LocalDate fom, LocalDate tom, String begrunnelse, Resultat resultat) {
        var periode = new UnntakEtablertTilsynPeriode()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medBegrunnelse(begrunnelse)
            .medResultat(resultat);
        periode.setUnntakEtablertTilsyn(unntakEtablertTilsyn);
        return periode;
    }

    private UnntakEtablertTilsynBeskrivelse beskrivelse(UnntakEtablertTilsyn unntakEtablertTilsyn, String tekst, LocalDate fom, LocalDate tom) {
        return new UnntakEtablertTilsynBeskrivelse()
            .medUnntakEtablertTilsyn(unntakEtablertTilsyn)
            .medSøker(new AktørId(123L))
            .medMottattDato(LocalDate.now())
            .medPeriode(fom, tom)
            .medTekst(tekst);
    }

}
