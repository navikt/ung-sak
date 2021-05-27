package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
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
    private FagsakRepository fagsakRepo;

    @Inject
    private BehandlingRepository behandlingRepo;

    @Test
    void lagre_og_hent_igjen() {
        var behandlingId = opprettBehandling(opprettFagsak());
        var nyttBeredskap = lagUnntakEtablertTilsyn("2020-01-01", "2020-02-01", behandlingId);

        var uetForPleietrengende = new UnntakEtablertTilsynForPleietrengende(new AktørId(456L), nyttBeredskap, null);

        grunnlagRepo.lagre(behandlingId, uetForPleietrengende);

        grunnlagRepo.entityManager.flush();
        grunnlagRepo.entityManager.clear();

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
    void lagre_oppdatere_og_hent_igjen() {
        var behandlingId = opprettBehandling(opprettFagsak());
        var nyttBeredskap = lagUnntakEtablertTilsyn("2020-01-01", "2020-02-01", behandlingId);

        var uetForPleietrengende = new UnntakEtablertTilsynForPleietrengende(new AktørId(456L), nyttBeredskap, null);

        grunnlagRepo.lagre(behandlingId, uetForPleietrengende);

        grunnlagRepo.entityManager.flush();
        grunnlagRepo.entityManager.clear();

        var grunnlag = grunnlagRepo.hent(behandlingId);

        assertThat(grunnlag).isNotNull();
        assertThat(grunnlag.getUnntakEtablertTilsynForPleietrengende()).isNotNull();
        assertThat(grunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap()).isNotNull();
        assertThat(grunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk()).isNull();
        var beredskap = grunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap();
        assertThat(beredskap.getBeskrivelser()).hasSize(1);
        assertThat(beredskap.getBeskrivelser().get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-02-01")));
        assertThat(beredskap.getBeskrivelser().get(0).getKildeBehandlingId()).isEqualTo(behandlingId);

        var nattevåk = lagUnntakEtablertTilsyn("2020-03-01", "2020-04-01", behandlingId);

        uetForPleietrengende.medNattevåk(nattevåk);
        grunnlagRepo.lagre(behandlingId, new UnntakEtablertTilsynForPleietrengende(
            grunnlag.getUnntakEtablertTilsynForPleietrengende().getPleietrengendeAktørId(),
            new UnntakEtablertTilsyn(grunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap()),
            nattevåk));

        grunnlagRepo.entityManager.flush();
        grunnlagRepo.entityManager.clear();

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

    private UnntakEtablertTilsyn lagUnntakEtablertTilsyn(String fom, String tom, Long kildeBehandlingId) {
        var perioder = List.of(
            periode(LocalDate.parse(fom), LocalDate.parse(tom), "DU skal få.", Resultat.OPPFYLT)
        );
        var beskrivelser = List.of(
            beskrivelse("Jeg søker.", LocalDate.parse(fom), LocalDate.parse(tom), kildeBehandlingId)
        );
        return new UnntakEtablertTilsyn(perioder, beskrivelser);
    }

    private UnntakEtablertTilsynPeriode periode(LocalDate fom, LocalDate tom, String begrunnelse, Resultat resultat) {
        return new UnntakEtablertTilsynPeriode()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medBegrunnelse(begrunnelse)
            .medResultat(resultat)
            .medAktørId(new AktørId(456L))
            .medKildeBehandlingId(123L);
    }

    private UnntakEtablertTilsynBeskrivelse beskrivelse(String tekst, LocalDate fom, LocalDate tom, Long kildeBehandlingId) {
        return new UnntakEtablertTilsynBeskrivelse()
            .medSøker(new AktørId(456L))
            .medMottattDato(LocalDate.now())
            .medPeriode(fom, tom)
            .medTekst(tekst)
            .medKildeBehandlingId(kildeBehandlingId);
    }

    private Fagsak opprettFagsak() {
        var fagsak = new Fagsak(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), new Saksnummer("123"));
        fagsakRepo.opprettNy(fagsak);
        return fagsak;
    }

    private Long opprettBehandling(Fagsak fagsak) {
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        return behandlingRepo.lagreOgClear(behandling, new BehandlingLås(null));
    }


}
