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
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Arrays;

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
        var behandlingId = opprettBehandling(opprettFagsak("101"));
        var nyttBeredskap = lagUnntakEtablertTilsyn(behandlingId, new Periode("2020-01-01/2020-02-01"));

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
        var behandlingId = opprettBehandling(opprettFagsak("201"));
        var nyttBeredskap = lagUnntakEtablertTilsyn(behandlingId, new Periode("2020-01-01/2020-02-01"));

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

        var nattevåk = lagUnntakEtablertTilsyn(behandlingId, new Periode("2020-03-01/2020-04-01"));

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


    @Test
    void skal_finne_sist_oppdatert_grunnlag_for_pleietrengende() throws InterruptedException {

        { // sak fra søker 1 for pleietrengende 456
            var behandlingId = opprettBehandling(opprettFagsak("301"));
            var nyttBeredskap = lagUnntakEtablertTilsyn(behandlingId, new Periode("2020-01-01/2020-01-31"));

            var uetForPleietrengende = new UnntakEtablertTilsynForPleietrengende(new AktørId(456L), nyttBeredskap, null);

            grunnlagRepo.lagre(behandlingId, uetForPleietrengende);

            var grunnlag = grunnlagRepo.hent(behandlingId);
        }

        Thread.sleep(100);

        { // sak fra søker 2 for pleietrengende 456
            var behandlingId = opprettBehandling(opprettFagsak("302"));
            var nyttBeredskap = lagUnntakEtablertTilsyn(behandlingId, new Periode("2020-01-01/2020-01-31"), new Periode("2020-02-01/2020-02-15"));

            var uetForPleietrengende = new UnntakEtablertTilsynForPleietrengende(new AktørId(456L), nyttBeredskap, null);

            grunnlagRepo.lagre(behandlingId, uetForPleietrengende);

            var grunnlag = grunnlagRepo.hent(behandlingId);
        }

        Thread.sleep(100);

        { // sak fra søker 3 for pleietrengende 789
            var behandlingId = opprettBehandling(opprettFagsak("303"));
            var nyttBeredskap = lagUnntakEtablertTilsyn(behandlingId, new Periode("2020-01-01/2020-01-31"), new Periode("2020-02-01/2020-02-15"));

            var uetForPleietrengende = new UnntakEtablertTilsynForPleietrengende(new AktørId(789L), nyttBeredskap, null);

            grunnlagRepo.lagre(behandlingId, uetForPleietrengende);

            var grunnlag = grunnlagRepo.hent(behandlingId);
        }

        var andreGrunnlag = grunnlagRepo.hentSisteForPleietrengende(new AktørId(456L));

        System.out.println(andreGrunnlag);

        //TODO: legge til asserts
    }



    private UnntakEtablertTilsyn lagUnntakEtablertTilsyn(Long kildeBehandlingId, Periode... søktePerioder) {
        var perioder = Arrays.stream(søktePerioder).map(periode ->
            periode(periode.getFom(), periode.getTom(), "DU skal få.", Resultat.OPPFYLT)
        ).toList();
        var beskrivelser = Arrays.stream(søktePerioder).map(periode ->
            beskrivelse("Jeg søker.", periode.getFom(), periode.getTom(), kildeBehandlingId)
        ).toList();
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

    private Fagsak opprettFagsak(String saksnummer) {
        var fagsak = new Fagsak(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), new Saksnummer(saksnummer));
        fagsakRepo.opprettNy(fagsak);
        return fagsak;
    }

    private Long opprettBehandling(Fagsak fagsak) {
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        return behandlingRepo.lagreOgClear(behandling, new BehandlingLås(null));
    }

}
