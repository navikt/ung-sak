package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.AktørId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UnntakEtablertTilsynGrunnlagRepositoryTest {

    @Inject
    private UnntakEtablertTilsynGrunnlagRepository grunnlagRepo;

    @Test
    void lagre_og_hent_igjen() {
        var behandlingId = 123L;
        var nyttBeredskap = lagUnntakEtablertTilsyn("2020-01-01", "2020-02-01");
        var pleietrengendeAktørId = new AktørId(456L);
        var uetForPleietrengende = new UnntakEtablertTilsynForPleietrengende(pleietrengendeAktørId, nyttBeredskap, null);

        grunnlagRepo.lagre(behandlingId, uetForPleietrengende);

        grunnlagRepo.entityManager.flush();
        grunnlagRepo.entityManager.clear();

        var unntakPleietrengende = grunnlagRepo.hentHvisEksistererUnntakPleietrengende(pleietrengendeAktørId).orElse(null);

        assertThat(unntakPleietrengende).isNotNull();
        assertThat(unntakPleietrengende.getBeredskap()).isNotNull();
        assertThat(unntakPleietrengende.getNattevåk()).isNull();
        var beredskap = unntakPleietrengende.getBeredskap();
        assertThat(beredskap.getBeskrivelser()).hasSize(1);
        assertThat(beredskap.getBeskrivelser().get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-02-01")));
        assertThat(beredskap.getBeskrivelser().get(0).getKildeBehandlingId()).isEqualTo(behandlingId);
    }

    @Test
    void lagre_oppdatere_og_hent_igjen() {
        var behandlingId = 123L;
        var nyttBeredskap = lagUnntakEtablertTilsyn("2020-01-01", "2020-02-01");
        var pleietrengendeAktørId = new AktørId(456L);
        var uetForPleietrengende = new UnntakEtablertTilsynForPleietrengende(pleietrengendeAktørId, nyttBeredskap, null);

        grunnlagRepo.lagre(behandlingId, uetForPleietrengende);

        grunnlagRepo.entityManager.flush();
        grunnlagRepo.entityManager.clear();

        var unntakPleietrengende = grunnlagRepo.hentHvisEksistererUnntakPleietrengende(pleietrengendeAktørId).orElse(null);

        assertThat(unntakPleietrengende).isNotNull();
        assertThat(unntakPleietrengende.getBeredskap()).isNotNull();
        assertThat(unntakPleietrengende.getNattevåk()).isNull();
        var beredskap = unntakPleietrengende.getBeredskap();
        assertThat(beredskap.getBeskrivelser()).hasSize(1);
        assertThat(beredskap.getBeskrivelser().get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-02-01")));
        assertThat(beredskap.getBeskrivelser().get(0).getKildeBehandlingId()).isEqualTo(behandlingId);

        var nattevåk = lagUnntakEtablertTilsyn("2020-03-01", "2020-04-01");

        uetForPleietrengende.medNattevåk(nattevåk);
        grunnlagRepo.lagre(behandlingId, new UnntakEtablertTilsynForPleietrengende(
                unntakPleietrengende.getPleietrengendeAktørId(),
            new UnntakEtablertTilsyn(unntakPleietrengende.getBeredskap()),
            nattevåk));

        grunnlagRepo.entityManager.flush();
        grunnlagRepo.entityManager.clear();

        /*
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
        */

        var oppdatertUnntakPleietrengende = grunnlagRepo.hentHvisEksistererUnntakPleietrengende(pleietrengendeAktørId).orElse(null);

        assertThat(oppdatertUnntakPleietrengende).isNotNull();
        assertThat(oppdatertUnntakPleietrengende.getBeredskap()).isNotNull();
        assertThat(oppdatertUnntakPleietrengende.getNattevåk()).isNotNull();
        var oppdatertBeredskap = oppdatertUnntakPleietrengende.getBeredskap();
        assertThat(oppdatertBeredskap.getBeskrivelser()).hasSize(1);
        assertThat(oppdatertBeredskap.getBeskrivelser().get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-02-01")));
        assertThat(oppdatertBeredskap.getBeskrivelser().get(0).getKildeBehandlingId()).isEqualTo(behandlingId);
        var nyNattevåk = oppdatertUnntakPleietrengende.getNattevåk();
        assertThat(nyNattevåk.getBeskrivelser()).hasSize(1);
        assertThat(nyNattevåk.getBeskrivelser().get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-03-01"), LocalDate.parse("2020-04-01")));
        assertThat(nyNattevåk.getBeskrivelser().get(0).getKildeBehandlingId()).isEqualTo(behandlingId);
    }

    private UnntakEtablertTilsyn lagUnntakEtablertTilsyn(String fom, String tom) {
        var perioder = List.of(
            periode(LocalDate.parse(fom), LocalDate.parse(tom), "DU skal få.", Resultat.OPPFYLT)
        );
        var beskrivelser = List.of(
            beskrivelse("Jeg søker.", LocalDate.parse(fom), LocalDate.parse(tom))
        );
        var unntakEtablertTilsyn = new UnntakEtablertTilsyn(perioder, beskrivelser);
        return unntakEtablertTilsyn;
    }

    private UnntakEtablertTilsynPeriode periode(LocalDate fom, LocalDate tom, String begrunnelse, Resultat resultat) {
        var periode = new UnntakEtablertTilsynPeriode()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medBegrunnelse(begrunnelse)
            .medResultat(resultat)
            .medAktørId(new AktørId(456L))
            .medKildeBehandlingId(123L);
        return periode;
    }

    private UnntakEtablertTilsynBeskrivelse beskrivelse(String tekst, LocalDate fom, LocalDate tom) {
        return new UnntakEtablertTilsynBeskrivelse()
            .medSøker(new AktørId(456L))
            .medMottattDato(LocalDate.now())
            .medPeriode(fom, tom)
            .medTekst(tekst)
            .medKildeBehandlingId(123L);
    }

}
