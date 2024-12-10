package no.nav.ung.sak.behandling.revurdering.sats;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class SatsEndringRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private SatsEndringRepository satsEndringRepository;

    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private PersonopplysningRepository personopplysningRepository;


    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        satsEndringRepository = new SatsEndringRepository(entityManager);
    }

    @Test
    void forventer_1_fagsak_der_bruker_er_over_25_aar_fra_dato() {
        Periode fagsakPeriode = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now().plusWeeks(51));
        LocalDate fødselsdato = LocalDate.now().minusYears(25).minusWeeks(1);

        klargjørDatagrunnlag(fagsakPeriode, fagsakPeriode, fødselsdato, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = satsEndringRepository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(LocalDate.now());

        assertThat(fagsakerTilRevurdering.size()).isEqualTo(1);
        Map.Entry<Fagsak, LocalDate> fagsakLocalDateEntry = fagsakerTilRevurdering.entrySet().stream().findFirst().get();
        assertThat(fagsakLocalDateEntry.getKey().getPeriode().getFomDato()).isEqualTo(fagsakPeriode.getFom());
        assertThat(fagsakLocalDateEntry.getKey().getPeriode().getTomDato()).isEqualTo(fagsakPeriode.getTom());

        LocalDate endringsdato = fødselsdato.plusMonths(1).withDayOfMonth(1);
        assertThat(fagsakLocalDateEntry.getValue()).isEqualTo(endringsdato);
    }

    @Test
    void forventer_ingen_fagsaker_der_bruker_er_under_25_aar_fra_dato() {
        Periode fagsakPeriode = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now().plusWeeks(51));
        LocalDate fødselsdato = LocalDate.now().minusYears(20).minusWeeks(1);

        klargjørDatagrunnlag(fagsakPeriode, fagsakPeriode, fødselsdato, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = satsEndringRepository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(LocalDate.now());

        assertThat(fagsakerTilRevurdering.size()).isEqualTo(0);
    }

    @Test
    void forventer_ingen_fagsaker_fordi_fagsakperioden_ikke_inneholder_endringsdatoen() {
        Periode fagsakPeriode = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now().plusWeeks(1));
        LocalDate fødselsdato = LocalDate.now().minusYears(25).minusWeeks(1);

        klargjørDatagrunnlag(fagsakPeriode, fagsakPeriode, fødselsdato, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = satsEndringRepository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(LocalDate.now());

        assertThat(fagsakerTilRevurdering.size()).isEqualTo(0);
    }

    @Test
    void forventer_ingen_fagsaker_fordi_fagsaken_er_obselete() {
        Periode fagsakPeriode = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now().plusWeeks(51));
        LocalDate fødselsdato = LocalDate.now().minusYears(25).minusWeeks(1);

        klargjørDatagrunnlag(fagsakPeriode, fagsakPeriode, fødselsdato, true);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = satsEndringRepository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(LocalDate.now());

        assertThat(fagsakerTilRevurdering.size()).isEqualTo(0);
    }

    private void klargjørDatagrunnlag(Periode fagsakPeriode, Periode ungdomsprogramPeriode, LocalDate fødselsdato, boolean fagsakObselete) {
        Fagsak fagsak = opprettFagsak(fagsakPeriode, fagsakObselete);
        Behandling behandling = opprettBehandlingFor(fagsak);
        opprettPersonopplysningGrunnlag(behandling, fødselsdato);
        opprettUngdomsprogramPeriodeGrunnlag(behandling, ungdomsprogramPeriode);
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(Behandling behandling, LocalDate fødselsdato) {
        PersonopplysningRepository personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        Long behandlingId = behandling.getId();

        PersonInformasjonBuilder personInformasjonBuilder = personopplysningRepository.opprettBuilderFraEksisterende(behandlingId, PersonopplysningVersjonType.REGISTRERT);

        PersonInformasjonBuilder.PersonopplysningBuilder personopplysningBuilder = personInformasjonBuilder.getPersonopplysningBuilder(behandling.getAktørId())
            .medFødselsdato(fødselsdato);

        personInformasjonBuilder.leggTil(personopplysningBuilder);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);
        return personopplysningRepository.hentPersonopplysninger(behandlingId);
    }

    private Fagsak opprettFagsak(Periode periode, boolean fagsakObselete) {
        FagsakRepository fagsakRepository = repositoryProvider.getFagsakRepository();
        Fagsak fagsak = Fagsak.opprettNy(!fagsakObselete ? FagsakYtelseType.UNGDOMSYTELSE : FagsakYtelseType.OBSOLETE, AktørId.dummy(), null, periode.getFom(), periode.getTom());
        Long fagsakId = fagsakRepository.opprettNy(fagsak);
        return fagsakRepository.finnEksaktFagsak(fagsakId);
    }

    private Behandling opprettBehandlingFor(Fagsak fagsak) {
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
        Behandling behandling = Behandling
            .nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD)
            .build();

        Long behandlingId = behandlingRepository.lagre(behandling, new BehandlingLås(behandling.getId()));
        return behandlingRepository.hentBehandling(behandlingId);
    }

    private int opprettUngdomsprogramPeriodeGrunnlag(Behandling behandling, Periode programPeriode) {
        int programPerioderId = entityManager.createNativeQuery("INSERT INTO UNG_UNGDOMSPROGRAMPERIODER (id) VALUES (1)")
            .executeUpdate();

        entityManager.createNativeQuery("INSERT INTO UNG_UNGDOMSPROGRAMPERIODE (id, ung_ungdomsprogramperioder_id, fom, tom) VALUES (1, :programPerioderId, :fom, :tom)")
            .setParameter("programPerioderId", programPerioderId)
            .setParameter("fom", programPeriode.getFom())
            .setParameter("tom", programPeriode.getTom())
            .executeUpdate();

        return entityManager.createNativeQuery("INSERT INTO UNG_GR_UNGDOMSPROGRAMPERIODE (id, behandling_id, ung_ungdomsprogramperioder_id, aktiv) VALUES (1, :behandlingId, :programPerioderId, true)")
            .setParameter("behandlingId", behandling.getId())
            .setParameter("programPerioderId", programPerioderId)
            .executeUpdate();
    }
}
