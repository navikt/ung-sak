package no.nav.ung.sak.ytelse.ung.mottak;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakTestUtil;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriode;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeTjeneste;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class FagsakperiodeUtlederTest {

    @Inject
    private EntityManager entityManager;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private FagsakperiodeUtleder fagsakperiodeUtleder;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @BeforeEach
    void setUp() {
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(entityManager);
        ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository);
        fagsakperiodeUtleder = new FagsakperiodeUtleder(ungdomsprogramPeriodeTjeneste);
    }

    @Test
    void skal_sette_periode_for_førstegangsbehandling() {
        var fom = LocalDate.now();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, null);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        var periode = fagsakperiodeUtleder.utledNyPeriodeForFagsak(behandling, fom);
        assertThat(periode.getFomDato()).isEqualTo(fom);
        assertThat(periode.getTomDato()).isEqualTo(fom.plusWeeks(52).minusDays(1));
    }

    @Test
    void skal_sette_periode_for_revurdering_uten_forbrukte_dager() {
        var fom = LocalDate.now();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, null);
        entityManager.persist(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        entityManager.persist(behandling);
        var revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();

        var periode = fagsakperiodeUtleder.utledNyPeriodeForFagsak(revurdering, fom);
        assertThat(periode.getFomDato()).isEqualTo(fom);
        assertThat(periode.getTomDato()).isEqualTo(fom.plusWeeks(52).minusDays(1));
    }

    @Test
    void skal_sette_periode_for_revurdering_med_en_uke_forbrukte_dager() {
        var fom = LocalDate.now();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, null);
        entityManager.persist(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        entityManager.persist(behandling);

        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, fom.plusWeeks(1).minusDays(1))));

        var revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        var nySøknadFom = fom.plusWeeks(2);
        var periode = fagsakperiodeUtleder.utledNyPeriodeForFagsak(revurdering, nySøknadFom);
        assertThat(periode.getFomDato()).isEqualTo(fom);
        assertThat(periode.getTomDato()).isEqualTo(nySøknadFom.plusWeeks(51).minusDays(1));
    }

    @Test
    void skal_sette_periode_for_revurdering_med_tre_forbrukte_dager() {
        var fom = LocalDate.of(2024,12,3);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, null);
        entityManager.persist(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        entityManager.persist(behandling);

        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, fom.plusDays(2))));

        var revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        var nySøknadFom = fom.plusWeeks(2);
        var periode = fagsakperiodeUtleder.utledNyPeriodeForFagsak(revurdering, nySøknadFom);
        assertThat(periode.getFomDato()).isEqualTo(fom);
        assertThat(periode.getTomDato()).isEqualTo(nySøknadFom.plusWeeks(51).plusDays(1));
    }

    @Test
    void skal_sette_periode_for_revurdering_med_tre_forbrukte_dager_der_resten_går_over_helg() {
        var fom = LocalDate.of(2024,12,3);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, null);
        entityManager.persist(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        entityManager.persist(behandling);

        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, fom.plusDays(2))));

        var revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        var nySøknadFom = fom.plusWeeks(2).plusDays(3);
        var periode = fagsakperiodeUtleder.utledNyPeriodeForFagsak(revurdering, nySøknadFom);
        assertThat(periode.getFomDato()).isEqualTo(fom);
        assertThat(periode.getTomDato()).isEqualTo(nySøknadFom.plusWeeks(51).plusDays(3));
    }


}
