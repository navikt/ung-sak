package no.nav.ung.sak.behandling.revurdering.sats;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Map;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class SatsEndringRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private SatsEndringRepository satsEndringRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        satsEndringRepository = new SatsEndringRepository(entityManager);
    }

    @Test
    void forventer_1_fagsak_der_bruker_er_over_25_aar_fra_dato() {
        Periode fagsakPeriode = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now().plusWeeks(51));
        LocalDate fødselsdato = LocalDate.now().minusYears(25).minusWeeks(1);

        klargjørDatagrunnlag(fagsakPeriode, fagsakPeriode, fødselsdato, false, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = satsEndringRepository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(LocalDate.now());

        assertThat(fagsakerTilRevurdering.size()).isEqualTo(1);
        Map.Entry<Fagsak, LocalDate> fagsakTilRevurdering = fagsakerTilRevurdering.entrySet().stream().findFirst().get();
        assertThat(fagsakTilRevurdering.getKey().getPeriode().getFomDato()).isEqualTo(fagsakPeriode.getFom());
        assertThat(fagsakTilRevurdering.getKey().getPeriode().getTomDato()).isEqualTo(fagsakPeriode.getTom());

        LocalDate endringsdato = fødselsdato.plusYears(25);
        assertThat(fagsakTilRevurdering.getValue()).isEqualTo(endringsdato);
    }

    @Test
    void forventer_ingen_fagsaker_der_bruker_er_under_25_aar_fra_dato() {
        Periode fagsakPeriode = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now().plusWeeks(51));
        LocalDate fødselsdato = LocalDate.now().minusYears(20).minusWeeks(1);

        klargjørDatagrunnlag(fagsakPeriode, fagsakPeriode, fødselsdato, false, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = satsEndringRepository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(LocalDate.now());

        assertThat(fagsakerTilRevurdering.size()).isEqualTo(0);
    }

    @Test
    void forventer_ingen_fagsaker_fordi_fagsakperioden_ikke_inneholder_endringsdatoen() {
        LocalDate dagensDato = LocalDate.now();
        Periode fagsakPeriode = new Periode(LocalDate.parse("2024-12-01"), LocalDate.parse("2024-12-31"));
        LocalDate fødselsdato = LocalDate.parse("1999-12-30"); // 25 år og 1 uke gammel

        klargjørDatagrunnlag(fagsakPeriode, fagsakPeriode, fødselsdato, false, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = satsEndringRepository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(dagensDato);

        assertThat(fagsakerTilRevurdering.size()).isEqualTo(0);
    }

    @Test
    void forventer_ingen_fagsaker_fordi_fagsaken_er_obselete() {
        Periode fagsakPeriode = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now().plusWeeks(51));
        LocalDate fødselsdato = LocalDate.now().minusYears(25).minusWeeks(1);

        klargjørDatagrunnlag(fagsakPeriode, fagsakPeriode, fødselsdato, true, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = satsEndringRepository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(LocalDate.now());

        assertThat(fagsakerTilRevurdering.size()).isEqualTo(0);
    }

    @Test
    void forventer_ingen_fagsaker_fordi_satsen_allerede_er_oppjustert() {
        Periode fagsakPeriode = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now().plusWeeks(51));
        LocalDate fødselsdato = LocalDate.now().minusYears(25).minusWeeks(1);

        klargjørDatagrunnlag(fagsakPeriode, fagsakPeriode, fødselsdato, false, true);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = satsEndringRepository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(LocalDate.now());

        assertThat(fagsakerTilRevurdering.size()).isEqualTo(0);
    }

    private void klargjørDatagrunnlag(Periode fagsakPeriode, Periode ungdomsprogramPeriode, LocalDate fødselsdato, boolean fagsakObselete, boolean harHøySatsFraFør) {
        Map.of(
            "fagsakPeriode", fagsakPeriode,
            "ungdomsprogramPeriode", fagsakPeriode,
            "fødselsdato", fødselsdato,
            "fødselsdatoPluss301Måneder", fødselsdato.plusMonths(301).withDayOfMonth(1),
            "tjuefemÅrFørDato", LocalDate.now().minusYears(25)
        ).forEach((key, value) -> {
            System.out.println(key + ": " + value);
        });

        Fagsak fagsak = opprettFagsak(fagsakPeriode, fagsakObselete);
        Behandling behandling = opprettBehandlingFor(fagsak);
        opprettPersonopplysningGrunnlag(behandling, fødselsdato);
        opprettUngdomsprogramPeriodeGrunnlag(behandling, ungdomsprogramPeriode);

        if (harHøySatsFraFør) {
            opprettSatsPeriode(behandling);
        }
    }

    private void opprettSatsPeriode(Behandling behandling) {
        int satsPerioderId = entityManager.createNativeQuery("INSERT INTO UNG_SATS_PERIODER (id, regel_input, regel_sporing) VALUES (1, lo_from_bytea(0, 'regelinput'::bytea), lo_from_bytea(0, 'regelsporing'::bytea))")

            .executeUpdate();

        entityManager.createNativeQuery("INSERT INTO UNG_SATS_PERIODE (id, ung_sats_perioder_id, periode, dagsats, grunnbeløp, grunnbeløp_faktor, sats_type, antall_barn, dagsats_barnetillegg) VALUES (1, :satsPerioderId, '[2021-01-01,2021-12-31]', 1000, 1000, 1, '"+ UngdomsytelseSatsType.HØY.getKode() +"', 0, 0)")
            .setParameter("satsPerioderId", satsPerioderId)
            .executeUpdate();

        entityManager.createNativeQuery("INSERT INTO UNG_GR (id, behandling_id, ung_sats_perioder_id, aktiv) VALUES (1, :behandlingId, :satsPerioderId, true)")
            .setParameter("behandlingId", behandling.getId())
            .setParameter("satsPerioderId", satsPerioderId)
            .executeUpdate();
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
