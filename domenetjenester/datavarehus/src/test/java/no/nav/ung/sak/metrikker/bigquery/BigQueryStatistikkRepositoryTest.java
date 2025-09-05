package no.nav.ung.sak.metrikker.bigquery;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.util.Tuple;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.aksjonspunkt.AksjonspunktRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.etterlysning.EtterlysningRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus.FagsakStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.personopplysninger.AlderOgKjønnRecord;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet.fraOgMedTilOgMed;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BigQueryStatistikkRepositoryTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private EtterlysningRepository etterlysningRepository;

    @Inject
    private SaksnummerRepository saksnummerRepository;

    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    private BigQueryStatistikkRepository statistikkRepository;


    @BeforeEach
    void setup() {
        statistikkRepository = new BigQueryStatistikkRepository(entityManager, null);
    }

    @Test
    void skal_kunne_hente_fagsak_status_statistikk() {
        // Gitt eksisterende fagsaker med ulike status
        lagreFagsaker(List.of(
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.OPPRETTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.OPPRETTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.UNDER_BEHANDLING),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.UNDER_BEHANDLING),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.UNDER_BEHANDLING),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.OBSOLETE, FagsakStatus.LØPENDE) // OBSOLETE fagsak for å teste at den ikke telles med
        ));

        // Når vi henter statistikken
        Collection<FagsakStatusRecord> fagsakStatusRecords = statistikkRepository.fagsakStatusStatistikk();

        // Så skal vi ha riktige records med korrekt antall for hver status
        assertThat(fagsakStatusRecords).isNotNull();
        assertThat(fagsakStatusRecords).hasSize(4); // 4 forskjellige statuser

        // Verifiser at vi har korrekt antall for hver status
        Map<FagsakStatus, Long> statusCounts = fagsakStatusRecords.stream()
            .collect(Collectors.groupingBy(
                FagsakStatusRecord::fagsakStatus,
                Collectors.summingLong(record -> record.antall().longValue())
            ));

        assertThat(statusCounts)
            .containsEntry(FagsakStatus.OPPRETTET, 2L)
            .containsEntry(FagsakStatus.UNDER_BEHANDLING, 3L)
            .containsEntry(FagsakStatus.LØPENDE, 4L)
            .containsEntry(FagsakStatus.AVSLUTTET, 5L);
    }

    @Test
    void skal_kunne_hente_ut_behandlingsstatus_statistikk() {
        Fagsak fagsak = byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.OPPRETTET);
        lagreFagsaker(List.of(fagsak));

        // Gitt en fagsak med ulike behandlinger
        lagreBehandlinger(List.of(
            byggBehandlingForFagsak(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.AVSLUTTET),
            byggBehandlingForFagsak(fagsak, BehandlingType.REVURDERING, BehandlingStatus.AVSLUTTET),
            byggBehandlingForFagsak(fagsak, BehandlingType.REVURDERING, BehandlingStatus.AVSLUTTET),
            byggBehandlingForFagsak(fagsak, BehandlingType.REVURDERING, BehandlingStatus.UTREDES),
            byggBehandlingForFagsak(fagsak, BehandlingType.REVURDERING, BehandlingStatus.OPPRETTET)
        ));

        // Når vi henter behandlingsstatus statistikken
        Collection<BehandlingStatusRecord> behandlingStatusRecords = statistikkRepository.behandlingStatusStatistikk();

        // Så skal vi ha riktige records med korrekt antall for hver kombinasjon av ytelse, type og status
        assertThat(behandlingStatusRecords).isNotNull();
        assertThat(behandlingStatusRecords).hasSize(4); // 3 forskjellige kombinasjoner

        Map<Triple<FagsakYtelseType, BehandlingType, BehandlingStatus>, Long> behandlingCounts = behandlingStatusRecords.stream()
            .collect(Collectors.groupingBy(
                record -> Triple.of(record.ytelseType(), record.behandlingType(), record.behandlingStatus()),
                Collectors.summingLong(record -> record.totaltAntall().longValue())
            ));

        assertThat(behandlingCounts)
            .containsEntry(Triple.of(FagsakYtelseType.UNGDOMSYTELSE, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.AVSLUTTET), 1L)
            .containsEntry(Triple.of(FagsakYtelseType.UNGDOMSYTELSE, BehandlingType.REVURDERING, BehandlingStatus.AVSLUTTET), 2L)
            .containsEntry(Triple.of(FagsakYtelseType.UNGDOMSYTELSE, BehandlingType.REVURDERING, BehandlingStatus.UTREDES), 1L)
            .containsEntry(Triple.of(FagsakYtelseType.UNGDOMSYTELSE, BehandlingType.REVURDERING, BehandlingStatus.OPPRETTET), 1L);
    }

    @Test
    void skal_kunne_hente_hyppig_rapporterte_metrikker() {
        // Gitt eksisterende fagsaker med ulike status
        lagreFagsaker(List.of(
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.OPPRETTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.OBSOLETE, FagsakStatus.LØPENDE) // OBSOLETE fagsak
        ));

        // Når vi henter hyppig rapporterte metrikker
        List<Tuple<BigQueryTabell<?>, Collection<?>>> metrikker = statistikkRepository.hentHyppigRapporterte(LocalDateTime.now().minusMinutes(1));

        // Så skal vi ha minst én metrikk
        assertThat(metrikker).isNotEmpty();

        // Og første metrikk skal være for FAGSAK_STATUS_V1 tabellen
        Tuple<BigQueryTabell<?>, Collection<?>> fagsakStatusMetrikk = metrikker.get(0);
        assertThat(fagsakStatusMetrikk.getElement1()).isEqualTo(FagsakStatusRecord.FAGSAK_STATUS_TABELL_V2);

        // Og den skal inneholde records (vi verifiserer ikke antallet her siden det er testet i den andre testen)
        Collection<?> records = fagsakStatusMetrikk.getElement2();
        assertThat(records).isNotEmpty();
        assertThat(records.iterator().next()).isInstanceOf(FagsakStatusRecord.class);
    }

    @Test
    void skal_kunne_hente_aksjonspunkter() {
        Fagsak fagsak = byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.OPPRETTET);
        lagreFagsaker(List.of(fagsak));

        // Gitt en fagsak med behandling med aksjonspunkt
        Behandling behandling = byggBehandlingForFagsak(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES);
        leggTilAksjonspunktPåBehandling(behandling, AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        lagreBehandling(behandling);

        // Når vi henter aksjonspunkt statistikken
        Collection<AksjonspunktRecord> behandlingStatusRecords = statistikkRepository.aksjonspunktStatistikk();
        // Og vi skal ha minst ett aksjonspunkt i statistikken
        assertThat(behandlingStatusRecords).isNotEmpty();
        // Verifiser at aksjonspunktet er med i statistikken
        assertThat(behandlingStatusRecords)
            .anyMatch(rec -> Objects.equals(rec.aksjonspunktDefinisjon(), AksjonspunktDefinisjon.KONTROLLER_INNTEKT));
    }


    @Test
    void skal_kunne_hente_etterlysninger() {
        Fagsak fagsak = byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.OPPRETTET);
        lagreFagsaker(List.of(fagsak));

        // Gitt en fagsak med behandling med etterlysning
        Behandling behandling = byggBehandlingForFagsak(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES);
        lagreBehandling(behandling);
        etterlysningRepository.lagre(new Etterlysning(behandling.getId(), UUID.randomUUID(), UUID.randomUUID(), fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10)), EtterlysningType.UTTALELSE_ENDRET_STARTDATO, EtterlysningStatus.VENTER));
        var etterlysningMedFrist = new Etterlysning(behandling.getId(), UUID.randomUUID(), UUID.randomUUID(), fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10)), EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO, EtterlysningStatus.AVBRUTT);
        etterlysningMedFrist.setFrist(LocalDateTime.now());
        etterlysningRepository.lagre(etterlysningMedFrist);

        // Når vi henter etterlysning statistikken
        Collection<EtterlysningRecord> etterlysninger = statistikkRepository.etterlysningData(LocalDateTime.now().minusMinutes(1));
        // Og vi skal ha minst en etterlysning i statistikken
        assertThat(etterlysninger).isNotEmpty();
        // Verifiser at etterlysningen er med i statistikken
        var etterlysningUtenFrist = etterlysninger.stream().filter(it -> it.etterlysningType().equals(EtterlysningType.UTTALELSE_ENDRET_STARTDATO)).findFirst().get();
        assertThat(etterlysningUtenFrist.frist()).isNull();
        var etterlysningRecordMedFrist = etterlysninger.stream().filter(it -> it.etterlysningType().equals(EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO)).findFirst().get();
        assertThat(etterlysningRecordMedFrist.frist()).isNotNull();
    }


    @Test
    void skal_kunne_hente_alder_og_kjønn_statistikk() {
        AktørId aktørId = AktørId.dummy();
        var fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.UNGDOMSYTELSE)
            .medBruker(aktørId)
            .medSaksnummer(new Saksnummer(saksnummerRepository.genererNyttSaksnummer())).medStatus(FagsakStatus.LØPENDE).build();
        lagreFagsaker(List.of(fagsak));

        // Gitt en fagsak med behandling med etterlysning
        Behandling behandling = byggBehandlingForFagsak(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES);
        lagreBehandling(behandling);

        PersonInformasjonBuilder pibuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        leggTilAktør(pibuilder, aktørId, 20, NavBrukerKjønn.MANN);
        leggTilAktør(pibuilder, AktørId.dummy(), 20, NavBrukerKjønn.KVINNE);

        new PersonopplysningRepository(entityManager).lagre(behandling.getId(), pibuilder);

        var alderOgKjønnStatistikk = statistikkRepository.alderOgKjønnStatistikk();
        // Og vi skal ha en bruker i statistikken
        assertThat(alderOgKjønnStatistikk.size()).isEqualTo(1);
        AlderOgKjønnRecord next = alderOgKjønnStatistikk.iterator().next();
        assertThat(next.alder()).isEqualTo(20);
        assertThat(next.navBrukerKjønn()).isEqualTo(NavBrukerKjønn.MANN);
        assertThat(next.antall().compareTo(BigDecimal.ONE)).isEqualTo(0);
    }

    private void lagFagsakOgBehandlingMedUngdomsprogram(DatoIntervallEntitet... perioder) {
        var fagsak1 = byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE);
        lagreFagsaker(List.of(fagsak1));

        // Gitt en fagsak med behandling med etterlysning
        Behandling behandling = byggBehandlingForFagsak(fagsak1, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES);
        lagreBehandling(behandling);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), Stream.of(perioder).map(UngdomsprogramPeriode::new).toList());
    }

    private static void leggTilAktør(PersonInformasjonBuilder pibuilder, AktørId aktørId, int alder, NavBrukerKjønn kjønn) {
        PersonInformasjonBuilder.PersonopplysningBuilder poBuilder = pibuilder.getPersonopplysningBuilder(aktørId);
        poBuilder.medFødselsdato(LocalDate.now().minusYears(alder));
        poBuilder.medNavn("Ola Nordmann");
        poBuilder.medKjønn(kjønn);
        pibuilder.leggTil(poBuilder);
    }


    private Behandling byggBehandlingForFagsak(Fagsak fagsak, BehandlingType behandlingType, BehandlingStatus behandlingStatus) {
        return Behandling.nyBehandlingFor(fagsak, behandlingType)
            .medBehandlingStatus(behandlingStatus)
            .build();
    }

    private Fagsak byggFagsak(FagsakYtelseType fagsakYtelseType, FagsakStatus status) {
        return FagsakBuilder.nyFagsak(fagsakYtelseType).medSaksnummer(new Saksnummer(saksnummerRepository.genererNyttSaksnummer())).medStatus(status).build();
    }

    private void lagreFagsaker(List<Fagsak> fagsaker) {
        fagsaker.forEach(
            fagsak -> {
                fagsakRepository.opprettNy(fagsak);
                entityManager.flush();
            }
        );
    }

    private void lagreBehandling(Behandling behandling) {
        behandlingRepository.lagre(behandling, new BehandlingLås(behandling.getId()));
        entityManager.flush();
    }

    private void lagreBehandlinger(List<Behandling> behandlinger) {
        behandlinger.forEach(this::lagreBehandling);
    }

    private Aksjonspunkt leggTilAksjonspunktPåBehandling(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
    }
}
