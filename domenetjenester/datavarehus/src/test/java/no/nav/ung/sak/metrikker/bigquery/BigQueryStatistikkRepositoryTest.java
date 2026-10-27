package no.nav.ung.sak.metrikker.bigquery;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.util.Tuple;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
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
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsResultat;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.aksjonspunkt.AksjonspunktRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsresultat.BehandslingsresultatStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsårsak.BehandlingÅrsakRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.etterlysning.EtterlysningRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus.FagsakStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.personopplysninger.AlderOgKjønnRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.sats.SatsStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.soknad.SøknadStatistikkRecord;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet.fraOgMedTilOgMed;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private UngdomsprogramytelseStatistikk ungdomsprogramytelseStatistikk;

    private AktivitetspengerStatistikk aktivitetspengerStatistikk;

    private final AtomicLong journalpostSekvens = new AtomicLong(System.nanoTime() % 1_000_000_000L);


    @BeforeEach
    void setup() {
        statistikkRepository = new BigQueryStatistikkRepository(entityManager);
        ungdomsprogramytelseStatistikk = new UngdomsprogramytelseStatistikk(statistikkRepository, null, entityManager);
        aktivitetspengerStatistikk = new AktivitetspengerStatistikk(statistikkRepository, entityManager);
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"UNGDOMSYTELSE", "AKTIVITETSPENGER"})
    void skal_kunne_hente_fagsak_status_statistikk_kun_for_oppgitt_ytelse(FagsakYtelseType ytelse) {
        // Gitt eksisterende fagsaker med ulike status for oppgitt ytelse, annen ytelse og utdatert ytelse
        FagsakYtelseType annenYtelse = annenYtelse(ytelse);
        lagreFagsaker(List.of(
            byggFagsak(ytelse, FagsakStatus.OPPRETTET),
            byggFagsak(ytelse, FagsakStatus.OPPRETTET),
            byggFagsak(ytelse, FagsakStatus.UNDER_BEHANDLING),
            byggFagsak(ytelse, FagsakStatus.UNDER_BEHANDLING),
            byggFagsak(ytelse, FagsakStatus.UNDER_BEHANDLING),
            byggFagsak(ytelse, FagsakStatus.LØPENDE),
            byggFagsak(ytelse, FagsakStatus.LØPENDE),
            byggFagsak(ytelse, FagsakStatus.LØPENDE),
            byggFagsak(ytelse, FagsakStatus.LØPENDE),
            byggFagsak(ytelse, FagsakStatus.AVSLUTTET),
            byggFagsak(ytelse, FagsakStatus.AVSLUTTET),
            byggFagsak(ytelse, FagsakStatus.AVSLUTTET),
            byggFagsak(ytelse, FagsakStatus.AVSLUTTET),
            byggFagsak(ytelse, FagsakStatus.AVSLUTTET),
            byggFagsak(annenYtelse, FagsakStatus.OPPRETTET),
            byggFagsak(annenYtelse, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.OBSOLETE, FagsakStatus.LØPENDE)
        ));

        // Når vi henter statistikken
        Collection<FagsakStatusRecord> fagsakStatusRecords = statistikkRepository.fagsakStatusStatistikk(ytelse);

        // Så skal kun fagsaker for oppgitt ytelse telles med
        assertThat(fagsakStatusRecords).hasSize(4);

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

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"UNGDOMSYTELSE", "AKTIVITETSPENGER"})
    void skal_kunne_hente_ut_behandlingsstatus_statistikk_kun_for_oppgitt_ytelse(FagsakYtelseType ytelse) {
        Fagsak fagsak = byggFagsak(ytelse, FagsakStatus.OPPRETTET);
        Fagsak fagsakAnnenYtelse = byggFagsak(annenYtelse(ytelse), FagsakStatus.OPPRETTET);
        Fagsak utgåttFagsak = byggFagsak(FagsakYtelseType.OBSOLETE, FagsakStatus.OPPRETTET);
        lagreFagsaker(List.of(fagsak, fagsakAnnenYtelse, utgåttFagsak));

        // Gitt en fagsak med ulike behandlinger, og fagsaker for annen ytelse og utgått ytelse
        lagreBehandlinger(List.of(
            byggBehandlingForFagsak(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.AVSLUTTET),
            byggBehandlingForFagsak(fagsak, BehandlingType.REVURDERING, BehandlingStatus.AVSLUTTET),
            byggBehandlingForFagsak(fagsak, BehandlingType.REVURDERING, BehandlingStatus.AVSLUTTET),
            byggBehandlingForFagsak(fagsak, BehandlingType.REVURDERING, BehandlingStatus.UTREDES),
            byggBehandlingForFagsak(fagsak, BehandlingType.REVURDERING, BehandlingStatus.OPPRETTET),
            byggBehandlingForFagsak(fagsakAnnenYtelse, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES),
            byggBehandlingForFagsak(utgåttFagsak, BehandlingType.REVURDERING, BehandlingStatus.AVSLUTTET)
        ));

        // Når vi henter behandlingsstatus statistikken
        Collection<BehandlingStatusRecord> behandlingStatusRecords = statistikkRepository.behandlingStatusStatistikk(ytelse);

        // Så skal vi kun ha records for oppgitt ytelse
        assertThat(behandlingStatusRecords).hasSize(4);

        Map<Triple<FagsakYtelseType, BehandlingType, BehandlingStatus>, Long> behandlingCounts = behandlingStatusRecords.stream()
            .collect(Collectors.groupingBy(
                record -> Triple.of(record.ytelseType(), record.behandlingType(), record.behandlingStatus()),
                Collectors.summingLong(record -> record.totaltAntall().longValue())
            ));

        assertThat(behandlingCounts)
            .containsEntry(Triple.of(ytelse, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.AVSLUTTET), 1L)
            .containsEntry(Triple.of(ytelse, BehandlingType.REVURDERING, BehandlingStatus.AVSLUTTET), 2L)
            .containsEntry(Triple.of(ytelse, BehandlingType.REVURDERING, BehandlingStatus.UTREDES), 1L)
            .containsEntry(Triple.of(ytelse, BehandlingType.REVURDERING, BehandlingStatus.OPPRETTET), 1L);
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"UNGDOMSYTELSE", "AKTIVITETSPENGER"})
    void skal_kunne_hente_behandlingsresultat_statistikk_kun_for_oppgitt_ytelse(FagsakYtelseType ytelse) {
        Fagsak fagsak = byggFagsak(ytelse, FagsakStatus.LØPENDE);
        Fagsak fagsakAnnenYtelse = byggFagsak(annenYtelse(ytelse), FagsakStatus.LØPENDE);
        lagreFagsaker(List.of(fagsak, fagsakAnnenYtelse));

        lagreBehandlinger(List.of(
            byggAvsluttetBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET),
            byggAvsluttetBehandling(fagsak, BehandlingType.REVURDERING, BehandlingResultatType.INNVILGET),
            byggAvsluttetBehandling(fagsakAnnenYtelse, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.AVSLÅTT)
        ));

        Collection<BehandslingsresultatStatistikkRecord> resultat = statistikkRepository.behandlingResultatStatistikk(ytelse);

        assertThat(resultat)
            .extracting(BehandslingsresultatStatistikkRecord::behandlingType, BehandslingsresultatStatistikkRecord::behandlingResultatType, BehandslingsresultatStatistikkRecord::totalAntall)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET, 1L),
                org.assertj.core.groups.Tuple.tuple(BehandlingType.REVURDERING, BehandlingResultatType.INNVILGET, 1L)
            );
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"UNGDOMSYTELSE", "AKTIVITETSPENGER"})
    void skal_kunne_hente_behandling_årsak_statistikk_kun_for_oppgitt_ytelse(FagsakYtelseType ytelse) {
        Fagsak fagsak = byggFagsak(ytelse, FagsakStatus.LØPENDE);
        Fagsak fagsakAnnenYtelse = byggFagsak(annenYtelse(ytelse), FagsakStatus.LØPENDE);
        lagreFagsaker(List.of(fagsak, fagsakAnnenYtelse));

        lagreBehandlinger(List.of(
            byggBehandlingMedÅrsak(fagsak, BehandlingStatus.UTREDES, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT),
            byggBehandlingMedÅrsak(fagsak, BehandlingStatus.AVSLUTTET, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT),
            byggBehandlingMedÅrsak(fagsakAnnenYtelse, BehandlingStatus.UTREDES, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)
        ));

        Collection<BehandlingÅrsakRecord> resultat = statistikkRepository.behandlingÅrsakStatistikk(ytelse);

        Map<Boolean, Long> antallForKontrollÅrsak = resultat.stream()
            .filter(it -> it.behandlingÅrsakType() == BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)
            .collect(Collectors.toMap(BehandlingÅrsakRecord::erFerdigbehandlet, it -> it.totaltAntall().longValue()));

        assertThat(antallForKontrollÅrsak)
            .containsEntry(true, 1L)
            .containsEntry(false, 1L);
    }

    @Test
    void skal_publisere_hyppig_rapporterte_metrikker_til_egne_tabeller_per_ytelse() {
        // Gitt fagsaker for begge ytelser og en utdatert ytelse
        lagreFagsaker(List.of(
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.OPPRETTET),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.LØPENDE),
            byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.AVSLUTTET),
            byggFagsak(FagsakYtelseType.AKTIVITETSPENGER, FagsakStatus.UNDER_BEHANDLING),
            byggFagsak(FagsakYtelseType.OBSOLETE, FagsakStatus.LØPENDE)
        ));

        // Når vi henter hyppig rapporterte metrikker for hver ytelse
        LocalDateTime sistKjørt = LocalDateTime.now().minusMinutes(1);
        List<Tuple<BigQueryTabell<?>, Collection<?>>> ungMetrikker = ungdomsprogramytelseStatistikk.hentHyppigRapporterte(sistKjørt);
        Map<String, Collection<?>> ungPerTabell = perTabell(ungMetrikker);
        Map<String, Collection<?>> aktPerTabell = perTabell(aktivitetspengerStatistikk.hentHyppigRapporterte(sistKjørt));

        // Så skal første metrikk fortsatt være fagsak-status for ungdomsytelsen
        assertThat(ungMetrikker.get(0).getElement1()).isEqualTo(FagsakStatusRecord.FAGSAK_STATUS_TABELL_V2);

        // Og ungdomsytelsen skal publisere til de opprinnelige tabellene, uten søknadsstatistikk
        assertThat(ungPerTabell.keySet()).containsExactlyInAnyOrder(
            "fagsak_status_v2", "behandling_status", "aksjonspunkter_status", "sats_statistikk",
            "behandlingsresultat_statistikk_v2", "etterlysning", "uttalelse", "behandling_årsak"
        );

        // Og aktivitetspenger skal publisere til egne tabeller, inkludert søknadsstatistikk
        assertThat(aktPerTabell.keySet()).containsExactlyInAnyOrder(
            "aktivitetspenger_fagsak_status", "aktivitetspenger_behandling_status", "aktivitetspenger_aksjonspunkter_status",
            "aktivitetspenger_sats_statistikk", "aktivitetspenger_behandlingsresultat_statistikk", "aktivitetspenger_etterlysning",
            "aktivitetspenger_uttalelse", "aktivitetspenger_behandling_årsak", "aktivitetspenger_soknad_mottatt"
        );

        // Og fagsak-status skal være separert per ytelse
        assertThat(antallFagsaker(ungPerTabell.get("fagsak_status_v2"))).isEqualTo(3L);
        assertThat(antallFagsaker(aktPerTabell.get("aktivitetspenger_fagsak_status"))).isEqualTo(1L);
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"UNGDOMSYTELSE", "AKTIVITETSPENGER"})
    void skal_kunne_hente_aksjonspunkter_kun_for_oppgitt_ytelse(FagsakYtelseType ytelse) {
        Fagsak fagsak = byggFagsak(ytelse, FagsakStatus.OPPRETTET);
        Fagsak fagsakAnnenYtelse = byggFagsak(annenYtelse(ytelse), FagsakStatus.OPPRETTET);
        lagreFagsaker(List.of(fagsak, fagsakAnnenYtelse));

        // Gitt behandlinger med ulike aksjonspunkter for hver ytelse
        Behandling behandling = byggBehandlingForFagsak(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES);
        leggTilAksjonspunktPåBehandling(behandling, AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        lagreBehandling(behandling);

        Behandling behandlingAnnenYtelse = byggBehandlingForFagsak(fagsakAnnenYtelse, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES);
        leggTilAksjonspunktPåBehandling(behandlingAnnenYtelse, AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        lagreBehandling(behandlingAnnenYtelse);

        // Når vi henter aksjonspunkt statistikken
        Collection<AksjonspunktRecord> aksjonspunktRecords = statistikkRepository.aksjonspunktStatistikk(ytelse);

        // Så skal kun aksjonspunktet for oppgitt ytelse være med
        assertThat(aksjonspunktRecords)
            .extracting(AksjonspunktRecord::ytelseType, AksjonspunktRecord::aksjonspunktDefinisjon)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(ytelse, AksjonspunktDefinisjon.KONTROLLER_INNTEKT));
    }


    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"UNGDOMSYTELSE", "AKTIVITETSPENGER"})
    void skal_kunne_hente_etterlysninger_kun_for_oppgitt_ytelse(FagsakYtelseType ytelse) {
        Fagsak fagsak = byggFagsak(ytelse, FagsakStatus.OPPRETTET);
        Fagsak fagsakAnnenYtelse = byggFagsak(annenYtelse(ytelse), FagsakStatus.OPPRETTET);
        lagreFagsaker(List.of(fagsak, fagsakAnnenYtelse));

        // Gitt en fagsak med behandling med etterlysning
        Behandling behandling = byggBehandlingForFagsak(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES);
        lagreBehandling(behandling);
        etterlysningRepository.lagre(new Etterlysning(behandling.getId(), UUID.randomUUID(), UUID.randomUUID(), fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10)), EtterlysningType.UTTALELSE_ENDRET_STARTDATO, EtterlysningStatus.VENTER));
        var etterlysningMedFrist = new Etterlysning(behandling.getId(), UUID.randomUUID(), UUID.randomUUID(), fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10)), EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO, EtterlysningStatus.AVBRUTT);
        etterlysningMedFrist.setFrist(LocalDateTime.now());
        etterlysningRepository.lagre(etterlysningMedFrist);

        // Og en etterlysning på annen ytelse som ikke skal telles med
        Behandling behandlingAnnenYtelse = byggBehandlingForFagsak(fagsakAnnenYtelse, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES);
        lagreBehandling(behandlingAnnenYtelse);
        etterlysningRepository.lagre(new Etterlysning(behandlingAnnenYtelse.getId(), UUID.randomUUID(), UUID.randomUUID(), fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10)), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT, EtterlysningStatus.VENTER));

        // Når vi henter etterlysning statistikken
        Collection<EtterlysningRecord> etterlysninger = statistikkRepository.etterlysningData(ytelse, LocalDateTime.now().minusMinutes(1));

        // Så skal kun etterlysningene for oppgitt ytelse være med
        assertThat(etterlysninger).hasSize(2);
        assertThat(etterlysninger).allMatch(it -> it.saksnummer().equals(fagsak.getSaksnummer()));
        var etterlysningUtenFrist = etterlysninger.stream().filter(it -> it.etterlysningType().equals(EtterlysningType.UTTALELSE_ENDRET_STARTDATO)).findFirst().get();
        assertThat(etterlysningUtenFrist.frist()).isNull();
        var etterlysningRecordMedFrist = etterlysninger.stream().filter(it -> it.etterlysningType().equals(EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO)).findFirst().get();
        assertThat(etterlysningRecordMedFrist.frist()).isNotNull();
    }


    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"UNGDOMSYTELSE", "AKTIVITETSPENGER"})
    void skal_kunne_hente_alder_og_kjønn_statistikk_kun_for_oppgitt_ytelse(FagsakYtelseType ytelse) {
        AktørId aktørId = AktørId.dummy();
        var fagsak = FagsakBuilder.nyFagsak(ytelse)
            .medBruker(aktørId)
            .medSaksnummer(new Saksnummer(saksnummerRepository.genererNyttSaksnummer())).medStatus(FagsakStatus.LØPENDE).build();
        AktørId aktørIdAnnenYtelse = AktørId.dummy();
        var fagsakAnnenYtelse = FagsakBuilder.nyFagsak(annenYtelse(ytelse))
            .medBruker(aktørIdAnnenYtelse)
            .medSaksnummer(new Saksnummer(saksnummerRepository.genererNyttSaksnummer())).medStatus(FagsakStatus.LØPENDE).build();
        lagreFagsaker(List.of(fagsak, fagsakAnnenYtelse));

        // Gitt en fagsak med personopplysninger for bruker og en annen person
        Behandling behandling = byggBehandlingForFagsak(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES);
        lagreBehandling(behandling);

        PersonInformasjonBuilder pibuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        leggTilAktør(pibuilder, aktørId, 20, NavBrukerKjønn.MANN);
        leggTilAktør(pibuilder, AktørId.dummy(), 20, NavBrukerKjønn.KVINNE);
        new PersonopplysningRepository(entityManager).lagre(behandling.getId(), pibuilder);

        // Og en bruker på annen ytelse som ikke skal telles med
        Behandling behandlingAnnenYtelse = byggBehandlingForFagsak(fagsakAnnenYtelse, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatus.UTREDES);
        lagreBehandling(behandlingAnnenYtelse);
        PersonInformasjonBuilder pibuilderAnnenYtelse = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        leggTilAktør(pibuilderAnnenYtelse, aktørIdAnnenYtelse, 25, NavBrukerKjønn.KVINNE);
        new PersonopplysningRepository(entityManager).lagre(behandlingAnnenYtelse.getId(), pibuilderAnnenYtelse);

        var alderOgKjønnStatistikk = statistikkRepository.alderOgKjønnStatistikk(ytelse);

        // Så skal kun brukeren på oppgitt ytelse være med i statistikken
        assertThat(alderOgKjønnStatistikk.size()).isEqualTo(1);
        AlderOgKjønnRecord next = alderOgKjønnStatistikk.iterator().next();
        assertThat(next.alder()).isEqualTo(20);
        assertThat(next.navBrukerKjønn()).isEqualTo(NavBrukerKjønn.MANN);
        assertThat(next.antall().compareTo(BigDecimal.ONE)).isEqualTo(0);
    }

    @Test
    void skal_telle_mottatte_søknader_for_aktivitetspenger_per_status() {
        Fagsak fagsak = byggFagsak(FagsakYtelseType.AKTIVITETSPENGER, FagsakStatus.UNDER_BEHANDLING);
        Fagsak fagsak2 = byggFagsak(FagsakYtelseType.AKTIVITETSPENGER, FagsakStatus.UNDER_BEHANDLING);
        Fagsak ungFagsak = byggFagsak(FagsakYtelseType.UNGDOMSYTELSE, FagsakStatus.UNDER_BEHANDLING);
        Fagsak utgåttFagsak = byggFagsak(FagsakYtelseType.OBSOLETE, FagsakStatus.AVSLUTTET);
        lagreFagsaker(List.of(fagsak, fagsak2, ungFagsak, utgåttFagsak));

        LocalDate idag = LocalDate.now();
        LocalDate igår = idag.minusDays(1);

        // Gitt søknader for aktivitetspenger med ulik status og mottattdato
        lagreMottattDokument(fagsak, Brevkode.AKTIVITETSPENGER_SOKNAD, idag, DokumentStatus.MOTTATT);
        lagreMottattDokument(fagsak2, Brevkode.AKTIVITETSPENGER_SOKNAD, idag, DokumentStatus.MOTTATT);
        lagreMottattDokument(fagsak, Brevkode.AKTIVITETSPENGER_SOKNAD, igår, DokumentStatus.GYLDIG);
        lagreMottattDokument(fagsak2, Brevkode.AKTIVITETSPENGER_SOKNAD, igår, DokumentStatus.HENLAGT);

        // Og dokumenter som ikke skal telles med: annen brevkode, søknad for annen ytelse og søknad på utgått fagsak
        lagreMottattDokument(fagsak, Brevkode.AKTIVITETSPENGER_INNTEKTRAPPORTERING, idag, DokumentStatus.GYLDIG);
        lagreMottattDokument(ungFagsak, Brevkode.UNGDOMSYTELSE_SOKNAD, idag, DokumentStatus.GYLDIG);
        lagreMottattDokument(utgåttFagsak, Brevkode.AKTIVITETSPENGER_SOKNAD, idag, DokumentStatus.MOTTATT);

        // Når vi henter søknadsstatistikken
        Collection<SøknadStatistikkRecord> resultat = aktivitetspengerStatistikk.søknadStatistikk();

        // Så skal søknadene telles per status, med antall mottatt i dag
        assertThat(resultat)
            .extracting(SøknadStatistikkRecord::dokumentStatus, SøknadStatistikkRecord::antallTotalt, SøknadStatistikkRecord::antallMottattIDag)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(DokumentStatus.MOTTATT, 2L, 2L),
                org.assertj.core.groups.Tuple.tuple(DokumentStatus.GYLDIG, 1L, 0L),
                org.assertj.core.groups.Tuple.tuple(DokumentStatus.HENLAGT, 1L, 0L)
            );
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"UNGDOMSYTELSE", "AKTIVITETSPENGER"})
    void skal_telle_gjeldende_sats_på_siste_avsluttede_behandling(FagsakYtelseType ytelse) {
        LocalDate idag = LocalDate.now();

        // Gitt en avsluttet behandling med en tidligere og en gjeldende satsperiode
        lagreSatsperioder(ytelse, lagreBehandlingPåNyFagsak(ytelse, BehandlingStatus.AVSLUTTET),
            new Satsperiode(fraOgMedTilOgMed(idag.minusDays(60), idag.minusDays(31)), UngdomsytelseSatsType.LAV, 0),
            new Satsperiode(fraOgMedTilOgMed(idag.minusDays(30), idag.plusDays(30)), UngdomsytelseSatsType.HØY, 2));

        // Og en avsluttet behandling der første satsperiode starter fram i tid
        lagreSatsperioder(ytelse, lagreBehandlingPåNyFagsak(ytelse, BehandlingStatus.AVSLUTTET),
            new Satsperiode(fraOgMedTilOgMed(idag.plusDays(10), idag.plusDays(40)), UngdomsytelseSatsType.LAV, 1),
            new Satsperiode(fraOgMedTilOgMed(idag.plusDays(41), idag.plusDays(70)), UngdomsytelseSatsType.HØY, 1));

        // Og en behandling som ikke er avsluttet
        lagreSatsperioder(ytelse, lagreBehandlingPåNyFagsak(ytelse, BehandlingStatus.UTREDES),
            new Satsperiode(fraOgMedTilOgMed(idag.minusDays(30), idag.plusDays(30)), UngdomsytelseSatsType.HØY, 3));

        // Når vi henter satsstatistikk for ytelsen
        Collection<SatsStatistikkRecord> resultat = switch (ytelse) {
            case UNGDOMSYTELSE -> ungdomsprogramytelseStatistikk.satsStatistikk();
            case AKTIVITETSPENGER -> aktivitetspengerStatistikk.satsStatistikk();
            default -> throw new IllegalArgumentException("Ukjent ytelse " + ytelse);
        };

        // Så telles kun gjeldende eller første framtidige satsperiode på avsluttede behandlinger
        assertThat(resultat)
            .extracting(SatsStatistikkRecord::satsType, SatsStatistikkRecord::antallBarn, SatsStatistikkRecord::antall)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(UngdomsytelseSatsType.HØY, 2, 1L),
                org.assertj.core.groups.Tuple.tuple(UngdomsytelseSatsType.LAV, 1, 1L)
            );
    }

    private record Satsperiode(DatoIntervallEntitet periode, UngdomsytelseSatsType satsType, int antallBarn) {
    }

    private Behandling lagreBehandlingPåNyFagsak(FagsakYtelseType ytelse, BehandlingStatus status) {
        Fagsak fagsak = byggFagsak(ytelse, FagsakStatus.LØPENDE);
        lagreFagsaker(List.of(fagsak));
        Behandling behandling = byggBehandlingForFagsak(fagsak, BehandlingType.FØRSTEGANGSSØKNAD, status);
        lagreBehandling(behandling);
        return behandling;
    }

    private void lagreSatsperioder(FagsakYtelseType ytelse, Behandling behandling, Satsperiode... perioder) {
        switch (ytelse) {
            case UNGDOMSYTELSE -> lagreUngdomsytelseSatsperioder(behandling, perioder);
            case AKTIVITETSPENGER -> lagreAktivitetspengerSatsperioder(behandling, perioder);
            default -> throw new IllegalArgumentException("Ukjent ytelse " + ytelse);
        }
        entityManager.flush();
    }

    private void lagreUngdomsytelseSatsperioder(Behandling behandling, Satsperiode... perioder) {
        var tidslinje = new LocalDateTimeline<>(Stream.of(perioder)
            .map(p -> new LocalDateSegment<>(p.periode().toLocalDateInterval(), new UngdomsytelseSatser(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(124028), BigDecimal.valueOf(2), p.satsType(), p.antallBarn(), 0)))
            .toList());
        new UngdomsytelseGrunnlagRepository(entityManager).lagre(behandling.getId(), new UngdomsytelseSatsResultat(tidslinje, "input", "sporing"));
    }

    /**
     * Entitetene for aktivitetspenger ligger i <code>ytelse-aktivitetspenger</code>, som datavarehus ikke er avhengig av.
     * Satsperiodene lagres derfor med SQL.
     */
    private void lagreAktivitetspengerSatsperioder(Behandling behandling, Satsperiode... perioder) {
        long satsperioderId = nesteId("seq_avp_sats_perioder");
        // regel_input og regel_sporing er large objects som statistikken ikke leser
        entityManager.createNativeQuery("insert into avp_sats_perioder (id, regel_input, regel_sporing) values (:id, 0, 0)")
            .setParameter("id", satsperioderId)
            .executeUpdate();
        for (Satsperiode periode : perioder) {
            entityManager.createNativeQuery("""
                    insert into avp_sats_periode (id, avp_sats_perioder_id, dagsats, periode, grunnbeløp, grunnbeløp_faktor,
                                                  sats_type, hjemmel, antall_barn, dagsats_barnetillegg, minsteytelse)
                    values (:id, :satsperioderId, 1000, daterange(:fom, :tom, '[]'), 124028, 2,
                            :satsType, :hjemmel, :antallBarn, 0, 248056)
                    """)
                .setParameter("id", nesteId("seq_avp_sats_periode"))
                .setParameter("satsperioderId", satsperioderId)
                .setParameter("fom", periode.periode().getFomDato())
                .setParameter("tom", periode.periode().getTomDato())
                .setParameter("satsType", periode.satsType().getKode())
                .setParameter("hjemmel", Hjemmel.AKT_FORSKRIFT_PARAGRAF_12.getKode())
                .setParameter("antallBarn", periode.antallBarn())
                .executeUpdate();
        }
        entityManager.createNativeQuery("insert into gr_avp (id, behandling_id, avp_sats_perioder_id) values (:id, :behandlingId, :satsperioderId)")
            .setParameter("id", nesteId("seq_gr_avp"))
            .setParameter("behandlingId", behandling.getId())
            .setParameter("satsperioderId", satsperioderId)
            .executeUpdate();
    }

    private long nesteId(String sekvens) {
        return ((Number) entityManager.createNativeQuery("select nextval('" + sekvens + "')").getSingleResult()).longValue();
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

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"OBSOLETE", "UDEFINERT"})
    void skal_avvise_statistikk_for_ytelse_som_ikke_er_støttet(FagsakYtelseType ytelse) {
        LocalDateTime sistKjørt = LocalDateTime.now().minusMinutes(1);

        assertThatThrownBy(() -> statistikkRepository.fagsakStatusStatistikk(ytelse)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> statistikkRepository.behandlingStatusStatistikk(ytelse)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> statistikkRepository.aksjonspunktStatistikk(ytelse)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> statistikkRepository.behandlingResultatStatistikk(ytelse)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> statistikkRepository.etterlysningData(ytelse, sistKjørt)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> statistikkRepository.uttalelseData(ytelse, sistKjørt, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> statistikkRepository.behandlingÅrsakStatistikk(ytelse)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> statistikkRepository.alderOgKjønnStatistikk(ytelse)).isInstanceOf(IllegalArgumentException.class);
    }

    private static FagsakYtelseType annenYtelse(FagsakYtelseType ytelse) {
        return ytelse == FagsakYtelseType.UNGDOMSYTELSE ? FagsakYtelseType.AKTIVITETSPENGER : FagsakYtelseType.UNGDOMSYTELSE;
    }

    private static Map<String, Collection<?>> perTabell(List<Tuple<BigQueryTabell<?>, Collection<?>>> metrikker) {
        return metrikker.stream()
            .collect(Collectors.toMap(it -> it.getElement1().getTabellnavn(), Tuple::getElement2));
    }

    private static long antallFagsaker(Collection<?> fagsakStatusRecords) {
        return fagsakStatusRecords.stream()
            .map(FagsakStatusRecord.class::cast)
            .mapToLong(it -> it.antall().longValue())
            .sum();
    }

    private void lagreMottattDokument(Fagsak fagsak, Brevkode type, LocalDate mottattDato, DokumentStatus status) {
        MottattDokument dokument = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId(String.valueOf(journalpostSekvens.incrementAndGet())))
            .medFagsakId(fagsak.getId())
            .medType(type)
            .medMottattDato(mottattDato)
            .medMottattTidspunkt(mottattDato.atStartOfDay())
            .build();
        new MottatteDokumentRepository(entityManager).lagre(dokument, status);
    }

    private Behandling byggAvsluttetBehandling(Fagsak fagsak, BehandlingType behandlingType, BehandlingResultatType resultatType) {
        return Behandling.nyBehandlingFor(fagsak, behandlingType)
            .medBehandlingStatus(BehandlingStatus.AVSLUTTET)
            .medBehandlingResultatType(resultatType)
            .build();
    }

    private Behandling byggBehandlingMedÅrsak(Fagsak fagsak, BehandlingStatus behandlingStatus, BehandlingÅrsakType årsak) {
        return Behandling.nyBehandlingFor(fagsak, BehandlingType.REVURDERING)
            .medBehandlingStatus(behandlingStatus)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(årsak))
            .build();
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
