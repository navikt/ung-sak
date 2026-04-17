package no.nav.ung.ytelse.aktivitetspenger.revurdering.sats;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class AktivitetspengerSatsEndringRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private AktivitetspengerSatsEndringRepository repository;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        repository = new AktivitetspengerSatsEndringRepository(entityManager);
    }

    @Test
    void forventer_1_fagsak_der_bruker_ble_25_aar_før_dato() {
        var dato = LocalDate.now();
        Periode fagsakPeriode = new Periode(dato.minusDays(30), dato.plusDays(330));
        LocalDate fødselsdato = dato.minusYears(25).minusDays(1);
        Periode søktPeriode = new Periode(fødselsdato.plusMonths(301).withDayOfMonth(1), fagsakPeriode.getTom());

        klargjørDatagrunnlag(fagsakPeriode, søktPeriode, fødselsdato, false, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = repository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(dato);

        assertThat(fagsakerTilRevurdering).hasSize(1);
        assertThat(fagsakerTilRevurdering.values()).containsExactly(fødselsdato.plusYears(25));
    }

    @Test
    void forventer_ingen_fagsaker_der_bruker_er_under_25_aar_fra_dato() {
        var dato = LocalDate.now();
        Periode fagsakPeriode = new Periode(dato.minusDays(30), dato.plusDays(330));
        LocalDate fødselsdato = dato.minusYears(24);
        Periode søktPeriode = new Periode(dato.minusDays(10), dato.plusDays(10));

        klargjørDatagrunnlag(fagsakPeriode, søktPeriode, fødselsdato, false, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = repository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(dato);

        assertThat(fagsakerTilRevurdering).isEmpty();
    }

    @Test
    void forventer_ingen_fagsaker_fordi_søkt_periode_ikke_inneholder_endringsdatoen() {
        var dato = LocalDate.now();
        Periode fagsakPeriode = new Periode(dato.minusDays(30), dato.plusDays(330));
        LocalDate fødselsdato = dato.minusYears(25).minusDays(1);
        LocalDate endringsdatoIUtvalg = fødselsdato.plusMonths(301).withDayOfMonth(1);
        Periode søktPeriode = new Periode(dato.minusDays(10), endringsdatoIUtvalg.minusDays(1));

        klargjørDatagrunnlag(fagsakPeriode, søktPeriode, fødselsdato, false, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = repository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(dato);

        assertThat(fagsakerTilRevurdering).isEmpty();
    }

    @Test
    void forventer_ingen_fagsaker_fordi_fagsakperioden_ikke_inneholder_endringsdatoen() {
        var dato = LocalDate.now();
        LocalDate fødselsdato = dato.minusYears(25).minusDays(1);
        LocalDate endringsdatoIUtvalg = fødselsdato.plusMonths(301).withDayOfMonth(1);
        Periode fagsakPeriode = new Periode(dato.minusDays(10), endringsdatoIUtvalg.minusDays(1));
        Periode søktPeriode = new Periode(dato.minusDays(10), dato.plusDays(30));

        klargjørDatagrunnlag(fagsakPeriode, søktPeriode, fødselsdato, false, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = repository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(dato);

        assertThat(fagsakerTilRevurdering).isEmpty();
    }

    @Test
    void forventer_ingen_fagsaker_fordi_fagsaken_er_obselete() {
        var dato = LocalDate.now();
        Periode fagsakPeriode = new Periode(dato.minusDays(30), dato.plusDays(330));
        LocalDate fødselsdato = dato.minusYears(25).minusDays(1);
        Periode søktPeriode = new Periode(fødselsdato.plusMonths(301).withDayOfMonth(1), fagsakPeriode.getTom());

        Fagsak fagsak = opprettFagsak(fagsakPeriode, FagsakYtelseType.OBSOLETE);
        Behandling behandling = opprettBehandlingFor(fagsak);
        opprettPersonopplysningGrunnlag(behandling, fødselsdato);
        opprettSøktPeriode(behandling, søktPeriode);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = repository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(dato);

        assertThat(fagsakerTilRevurdering).isEmpty();
    }

    @Test
    void forventer_ingen_fagsaker_fordi_satsen_allerede_er_oppjustert() {
        var dato = LocalDate.now();
        Periode fagsakPeriode = new Periode(dato.minusDays(30), dato.plusDays(330));
        LocalDate fødselsdato = dato.minusYears(25).minusDays(1);
        Periode søktPeriode = new Periode(fødselsdato.plusMonths(301).withDayOfMonth(1), fagsakPeriode.getTom());

        klargjørDatagrunnlag(fagsakPeriode, søktPeriode, fødselsdato, true, false);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = repository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(dato);

        assertThat(fagsakerTilRevurdering).isEmpty();
    }

    @Test
    void forventer_ingen_fagsaker_fordi_høy_sats_trigger_allerede_finnes() {
        var dato = LocalDate.now();
        Periode fagsakPeriode = new Periode(dato.minusDays(30), dato.plusDays(330));
        LocalDate fødselsdato = dato.minusYears(25).minusDays(1);
        Periode søktPeriode = new Periode(fødselsdato.plusMonths(301).withDayOfMonth(1), fagsakPeriode.getTom());

        klargjørDatagrunnlag(fagsakPeriode, søktPeriode, fødselsdato, false, true);

        Map<Fagsak, LocalDate> fagsakerTilRevurdering = repository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(dato);

        assertThat(fagsakerTilRevurdering).isEmpty();
    }

    private void klargjørDatagrunnlag(Periode fagsakPeriode, Periode søktPeriode, LocalDate fødselsdato, boolean harHøySatsFraFør, boolean harTriggerFraFør) {
        Fagsak fagsak = opprettFagsak(fagsakPeriode);
        Behandling behandling = opprettBehandlingFor(fagsak);
        opprettPersonopplysningGrunnlag(behandling, fødselsdato);
        opprettSøktPeriode(behandling, søktPeriode);

        if (harHøySatsFraFør) {
            opprettSatsPeriode(behandling);
        }
        if (harTriggerFraFør) {
            opprettTriggerÅrsak(behandling);
        }
    }

    private void opprettSøktPeriode(Behandling behandling, Periode søktPeriode) {
        var søktPeriodeEntitet = new no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriode(
            behandling.getId(),
            new JournalpostId("JP-1"),
            LocalDateTime.now(),
            DatoIntervallEntitet.fraOgMedTilOgMed(søktPeriode.getFom(), søktPeriode.getTom())
        );
        entityManager.persist(søktPeriodeEntitet);
        entityManager.flush();
    }

    private void opprettSatsPeriode(Behandling behandling) {
        Long satsPerioderId = nesteId("SEQ_AVP_SATS_PERIODER");
        Long satsPeriodeId = nesteId("SEQ_AVP_SATS_PERIODE");
        Long grunnlagId = nesteId("SEQ_GR_AVP");

        entityManager.createNativeQuery(
                "INSERT INTO AVP_SATS_PERIODER (id, regel_input, regel_sporing) " +
                    "VALUES (:satsPerioderId, lo_from_bytea(0, 'regelinput'::bytea), lo_from_bytea(0, 'regelsporing'::bytea))")
            .setParameter("satsPerioderId", satsPerioderId)
            .executeUpdate();

        entityManager.createNativeQuery(
                "INSERT INTO AVP_SATS_PERIODE (id, avp_sats_perioder_id, periode, dagsats, grunnbeløp, grunnbeløp_faktor, sats_type, antall_barn, dagsats_barnetillegg, hjemmel, minsteytelse) " +
                    "VALUES (:satsPeriodeId, :satsPerioderId, '[2021-01-01,2021-12-31]', 1000, 1000, 1, :satsType, 0, 0, :hjemmel, 1000)")
            .setParameter("satsPeriodeId", satsPeriodeId)
            .setParameter("satsPerioderId", satsPerioderId)
            .setParameter("satsType", UngdomsytelseSatsType.HØY.getKode())
            .setParameter("hjemmel", Hjemmel.UNG_FORSKRIFT_PARAGRAF_9.getKode())
            .executeUpdate();

        entityManager.createNativeQuery(
                "INSERT INTO GR_AVP (id, behandling_id, avp_sats_perioder_id, aktiv) VALUES (:id, :behandlingId, :satsPerioderId, true)")
            .setParameter("id", grunnlagId)
            .setParameter("behandlingId", behandling.getId())
            .setParameter("satsPerioderId", satsPerioderId)
            .executeUpdate();
    }

    private void opprettTriggerÅrsak(Behandling behandling) {
        Long behandlingÅrsakId = nesteId("SEQ_BEHANDLING_ARSAK");
        entityManager.createNativeQuery(
                "INSERT INTO BEHANDLING_ARSAK (id, behandling_id, behandling_arsak_type) VALUES (:id, :behandlingId, 'RE_TRIGGER_BEREGNING_HØY_SATS')")
            .setParameter("id", behandlingÅrsakId)
            .setParameter("behandlingId", behandling.getId())
            .executeUpdate();
    }

    private Long nesteId(String sekvens) {
        String query = switch (sekvens) {
            case "SEQ_AVP_SATS_PERIODER" -> "SELECT nextval('SEQ_AVP_SATS_PERIODER')";
            case "SEQ_AVP_SATS_PERIODE" -> "SELECT nextval('SEQ_AVP_SATS_PERIODE')";
            case "SEQ_GR_AVP" -> "SELECT nextval('SEQ_GR_AVP')";
            case "SEQ_BEHANDLING_ARSAK" -> "SELECT nextval('SEQ_BEHANDLING_ARSAK')";
            default -> throw new IllegalArgumentException("Ukjent sekvens: " + sekvens);
        };
        return ((Number) entityManager.createNativeQuery(query).getSingleResult()).longValue();
    }

    private void opprettPersonopplysningGrunnlag(Behandling behandling, LocalDate fødselsdato) {
        PersonopplysningRepository personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        Long behandlingId = behandling.getId();

        PersonInformasjonBuilder personInformasjonBuilder = personopplysningRepository.opprettBuilderFraEksisterende(behandlingId, PersonopplysningVersjonType.REGISTRERT);

        PersonInformasjonBuilder.PersonopplysningBuilder personopplysningBuilder = personInformasjonBuilder.getPersonopplysningBuilder(behandling.getAktørId())
            .medFødselsdato(fødselsdato);

        personInformasjonBuilder.leggTil(personopplysningBuilder);
        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);
    }

    private Fagsak opprettFagsak(Periode periode) {
        return opprettFagsak(periode, FagsakYtelseType.AKTIVITETSPENGER);
    }

    private Fagsak opprettFagsak(Periode periode, FagsakYtelseType ytelseType) {
        FagsakRepository fagsakRepository = repositoryProvider.getFagsakRepository();
        Fagsak fagsak = Fagsak.opprettNy(ytelseType, AktørId.dummy(), null, periode.getFom(), periode.getTom());
        Long fagsakId = fagsakRepository.opprettNy(fagsak);
        return fagsakRepository.finnEksaktFagsak(fagsakId);
    }

    private Behandling opprettBehandlingFor(Fagsak fagsak) {
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
        Behandling behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        Long behandlingId = behandlingRepository.lagre(behandling, new BehandlingLås(behandling.getId()));
        return behandlingRepository.hentBehandling(behandlingId);
    }
}
