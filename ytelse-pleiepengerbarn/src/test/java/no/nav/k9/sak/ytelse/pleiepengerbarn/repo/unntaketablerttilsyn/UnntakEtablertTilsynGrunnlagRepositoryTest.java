package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

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
class UnntakEtablertTilsynGrunnlagRepositoryTest {

    @Inject
    private UnntakEtablertTilsynGrunnlagRepository grunnlagRepo;

    @Inject
    private UnntakEtablertTilsynRepository uetRepo;


    @Test
    void lagre_og_hent_igjen() {
        var behandlingId = 123L;
        var nyttBeredskap = lagUnntakEtablertTilsyn("2020-01-01", "2020-02-01");
        uetRepo.lagre(nyttBeredskap);

        var uetForPleietrengende = new UnntakEtablertTilsynForPleietrengende(new AktørId(456L));
        uetForPleietrengende.medBeredskap(nyttBeredskap);

        grunnlagRepo.lagre(behandlingId, uetForPleietrengende);

        var grunnlag = grunnlagRepo.hent(behandlingId);

        assertThat(grunnlag).isNotNull();
        assertThat(grunnlag.getUnntakEtablertTilsynForPleietrengende()).isNotNull();
        assertThat(grunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap()).isNotNull();
        assertThat(grunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk()).isNull();
        var beredskap = grunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap();
        assertThat(beredskap.getBeskrivelser()).hasSize(1);
        assertThat(beredskap.getBeskrivelser().get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-02-01")));
        assertThat(beredskap.getBeskrivelser().get(0).getKildeBehandlingId()).isEqualTo(behandlingId);
    }

    @Test
    void lagreOppdatereOgHentIgjen() {
        var behandlingId = 123L;
        var nyttBeredskap = lagUnntakEtablertTilsyn("2020-01-01", "2020-02-01");
        uetRepo.lagre(nyttBeredskap);

        var uetForPleietrengende = new UnntakEtablertTilsynForPleietrengende(new AktørId(456L));
        uetForPleietrengende.medBeredskap(nyttBeredskap);

        grunnlagRepo.lagre(behandlingId, uetForPleietrengende);

        var grunnlag = grunnlagRepo.hent(behandlingId);

        assertThat(grunnlag).isNotNull();
        assertThat(grunnlag.getUnntakEtablertTilsynForPleietrengende()).isNotNull();
        assertThat(grunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap()).isNotNull();
        assertThat(grunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk()).isNull();
        var beredskap = grunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap();
        assertThat(beredskap.getBeskrivelser()).hasSize(1);
        assertThat(beredskap.getBeskrivelser().get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-02-01")));
        assertThat(beredskap.getBeskrivelser().get(0).getKildeBehandlingId()).isEqualTo(behandlingId);

        var nattevåk = lagUnntakEtablertTilsyn("2020-03-01", "2020-04-01");
        uetRepo.lagre(nattevåk);
        uetForPleietrengende.medNattevåk(nattevåk);
        grunnlagRepo.lagre(behandlingId, uetForPleietrengende);

        var oppdatertGrunnlag = grunnlagRepo.hent(behandlingId);

        assertThat(oppdatertGrunnlag).isNotNull();
        assertThat(oppdatertGrunnlag.getUnntakEtablertTilsynForPleietrengende()).isNotNull();
        assertThat(oppdatertGrunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap()).isNotNull();
        assertThat(oppdatertGrunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk()).isNotNull();
        var oppdatertBeredskap = oppdatertGrunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap();
        assertThat(oppdatertBeredskap.getBeskrivelser()).hasSize(1);
        assertThat(oppdatertBeredskap.getBeskrivelser().get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-02-01")));
        assertThat(oppdatertBeredskap.getBeskrivelser().get(0).getKildeBehandlingId()).isEqualTo(behandlingId);
        var nyNattevåk = oppdatertGrunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk();
        assertThat(nyNattevåk.getBeskrivelser()).hasSize(1);
        assertThat(nyNattevåk.getBeskrivelser().get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-03-01"), LocalDate.parse("2020-04-01")));
        assertThat(nyNattevåk.getBeskrivelser().get(0).getKildeBehandlingId()).isEqualTo(behandlingId);
    }

    private UnntakEtablertTilsyn lagUnntakEtablertTilsyn(String fom, String tom) {
        var unntakEtablertTilsyn = new UnntakEtablertTilsyn();
        unntakEtablertTilsyn.setBeskrivelser(List.of(
            beskrivelse(unntakEtablertTilsyn, "Jeg søker.", LocalDate.parse(fom), LocalDate.parse(tom))
        ));
        unntakEtablertTilsyn.setPerioder(List.of(
            periode(unntakEtablertTilsyn, LocalDate.parse(fom), LocalDate.parse(tom), "DU skal få.", Resultat.OPPFYLT)
        ));
        return unntakEtablertTilsyn;
    }

    private UnntakEtablertTilsynPeriode periode(UnntakEtablertTilsyn unntakEtablertTilsyn, LocalDate fom, LocalDate tom, String begrunnelse, Resultat resultat) {
        var periode = new UnntakEtablertTilsynPeriode()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medBegrunnelse(begrunnelse)
            .medResultat(resultat)
            .medKildeBehandlingId(123L);
        periode.setUnntakEtablertTilsyn(unntakEtablertTilsyn);
        return periode;
    }

    private UnntakEtablertTilsynBeskrivelse beskrivelse(UnntakEtablertTilsyn unntakEtablertTilsyn, String tekst, LocalDate fom, LocalDate tom) {
        return new UnntakEtablertTilsynBeskrivelse()
            .medUnntakEtablertTilsyn(unntakEtablertTilsyn)
            .medSøker(new AktørId(123L))
            .medMottattDato(LocalDate.now())
            .medPeriode(fom, tom)
            .medTekst(tekst)
            .medKildeBehandlingId(123L);
    }

}
