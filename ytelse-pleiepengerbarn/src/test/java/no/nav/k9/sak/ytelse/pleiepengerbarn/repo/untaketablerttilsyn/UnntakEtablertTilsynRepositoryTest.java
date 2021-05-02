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

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UnntakEtablertTilsynRepositoryTest {

    @Inject
    private UnntakEtablertTilsynRepository repository;

    @Test
    void lagreOgHentIgjen() {
        var uet = new UnntakEtablertTilsyn();
        uet.setPerioder(List.of(
            periode(uet, LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10"), "OK", Resultat.OPPFYLT)
        ));
        uet.setBeskrivelser(List.of(
            beskrivelse(uet, "Har så lyst på litt beredskap", LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10"))
        ));

        var id = repository.lagre(uet);

        var uetFraDb = repository.hent(id);

        assertThat(uetFraDb).isNotNull();
        assertThat(uetFraDb.getPerioder()).hasSize(1);
        var periode = uetFraDb.getPerioder().get(0);
        assertThat(periode.getResultat()).isEqualTo(Resultat.OPPFYLT);
        assertThat(periode.getBegrunnelse()).isEqualTo("OK");
        assertThat(periode.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")));
        assertThat(uetFraDb.getBeskrivelser()).hasSize(1);
        var beskrivelse = uetFraDb.getBeskrivelser().get(0);
        assertThat(beskrivelse.getTekst()).isEqualTo("Har så lyst på litt beredskap");
        assertThat(beskrivelse.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")));
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
