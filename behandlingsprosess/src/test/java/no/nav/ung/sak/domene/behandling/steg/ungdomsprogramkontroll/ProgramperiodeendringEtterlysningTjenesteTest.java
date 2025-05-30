package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class ProgramperiodeendringEtterlysningTjenesteTest {

    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;

    @Inject
    private ProsessTaskTjenesteImpl prosessTaskTjeneste;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Inject
    private EtterlysningRepository etterlysningRepository;

    @Inject
    private EntityManager entityManager;

    private TestScenarioBuilder scenario;
    private Behandling behandling;
    private MottatteDokumentRepository mottatteDokumentRepository;


    @BeforeEach
    void setUp() {
        scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.medBehandlingÅrsak(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM).lagre(entityManager);
        final var ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository);
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        programperiodeendringEtterlysningTjeneste = new ProgramperiodeendringEtterlysningTjeneste(
            ungdomsprogramPeriodeTjeneste,
            ungdomsprogramPeriodeRepository,
            prosessTaskTjeneste,
            etterlysningRepository,
            new EtterlysningTjeneste(mottatteDokumentRepository, etterlysningRepository),
            new BehandlingRepository(entityManager)
        );
    }

    @Test
    void skal_kaste_feil_ved_flere_perioder() {

        final var fom1 = LocalDate.now();
        final var fom2 = fom1.plusMonths(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(
            new UngdomsprogramPeriode(fom1, fom1.plusDays(1)),
            new UngdomsprogramPeriode(fom2, fom2.plusDays(1))
        ));

        assertThrows(IllegalStateException.class, () -> programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(BehandlingReferanse.fra(behandling)));
    }


    @Test
    void skal_opprette_etterlysning_for_endret_periode() {

        final var fom = LocalDate.now();
        final var tom = fom.plusDays(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, tom)));
        final var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        // act
        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(BehandlingReferanse.fra(behandling));

        final var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger.size()).isEqualTo(1);
        final var etterlysning = etterlysninger.get(0);
        assertThat(etterlysning.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        assertThat(etterlysning.getStatus()).isEqualTo(EtterlysningStatus.OPPRETTET);
        assertThat(etterlysning.getGrunnlagsreferanse()).isEqualTo(ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse());
    }


    @Test
    void skal_ikke_opprette_ny_etterlysning_for_endret_periode_dersom_ventende_etterlysning_er_gyldig() {

        final var fom = LocalDate.now();
        final var tom = fom.plusDays(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, tom)));
        final var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        opprettEtterlysningPåVent(ungdomsprogramPeriodeGrunnlag, fom, tom);

        // act
        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(BehandlingReferanse.fra(behandling));

        // assert
        final var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger.size()).isEqualTo(1);
        final var etterlysning = etterlysninger.get(0);
        assertThat(etterlysning.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        assertThat(etterlysning.getStatus()).isEqualTo(EtterlysningStatus.VENTER);
        assertThat(etterlysning.getGrunnlagsreferanse()).isEqualTo(ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse());
    }


    @Test
    void skal_opprette_ny_etterlysning_for_endret_periode_dersom_ventende_etterlysning_ikke_er_gyldig() {

        final var gammelFom = LocalDate.now();
        final var tom = gammelFom.plusYears(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(
            new UngdomsprogramPeriode(gammelFom, tom)
        ));

        final var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        opprettEtterlysningPåVent(ungdomsprogramPeriodeGrunnlag, gammelFom, tom);

        final var nyFom = LocalDate.now().plusMonths(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(
            new UngdomsprogramPeriode(nyFom, tom)
        ));

        final var ungdomsprogramPeriodeGrunnlag2 = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();


        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(BehandlingReferanse.fra(behandling));


        final var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger.size()).isEqualTo(2);
        final var nyopprettetEtterlysning = etterlysninger.stream().filter(it -> it.getStatus().equals(EtterlysningStatus.OPPRETTET)).findFirst().orElseThrow();
        assertThat(nyopprettetEtterlysning.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(nyFom, tom));
        assertThat(nyopprettetEtterlysning.getGrunnlagsreferanse()).isEqualTo(ungdomsprogramPeriodeGrunnlag2.getGrunnlagsreferanse());

        final var skalAvbrytesEtterlysning = etterlysninger.stream().filter(it -> it.getStatus().equals(EtterlysningStatus.SKAL_AVBRYTES)).findFirst().orElseThrow();
        assertThat(skalAvbrytesEtterlysning.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(gammelFom, tom));
        assertThat(skalAvbrytesEtterlysning.getGrunnlagsreferanse()).isEqualTo(ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse());
    }


    @Test
    void skal_ikke_opprette_ny_etterlysning_for_endret_grunnlagsreferanse_dersom_ventende_etterlysning_er_gyldig() {

        final var fom = LocalDate.now();
        final var tom = fom.plusDays(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, tom)));

        final var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        opprettEtterlysningPåVent(ungdomsprogramPeriodeGrunnlag, fom, tom);

        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, tom)));

        // ACT
        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(BehandlingReferanse.fra(behandling));


        final var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger.size()).isEqualTo(1);
        final var etterlysning = etterlysninger.get(0);
        assertThat(etterlysning.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        assertThat(etterlysning.getStatus()).isEqualTo(EtterlysningStatus.VENTER);
        assertThat(etterlysning.getGrunnlagsreferanse()).isEqualTo(ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse());
    }


    @Test
    void skal_ikke_opprette_ny_etterlysning_dersom_etterlysning_med_mottatt_svar_er_gyldig() {

        final var fom = LocalDate.now();
        final var tom = fom.plusDays(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, tom)));
        final var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        opprettEtterlysningMedMottattSvar(ungdomsprogramPeriodeGrunnlag, fom, tom);

        // act
        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(BehandlingReferanse.fra(behandling));

        // Assert
        final var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger.size()).isEqualTo(1);
        final var etterlysning = etterlysninger.get(0);
        assertThat(etterlysning.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        assertThat(etterlysning.getStatus()).isEqualTo(EtterlysningStatus.MOTTATT_SVAR);
        assertThat(etterlysning.getGrunnlagsreferanse()).isEqualTo(ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse());
    }


    @Test
    void skal_opprette_ny_etterlysning_dersom_etterlysning_med_mottatt_svar_ikke_er_gyldig() {

        final var gammelFom = LocalDate.now();
        final var tom = gammelFom.plusYears(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(gammelFom, tom)));
        final var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        opprettEtterlysningMedMottattSvar(ungdomsprogramPeriodeGrunnlag, gammelFom, tom);

        final var nyFom = LocalDate.now().plusMonths(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(nyFom, tom)));
        final var ungdomsprogramPeriodeGrunnlag2 = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());

        // act
        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(BehandlingReferanse.fra(behandling));

        // Assert
        final var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger.size()).isEqualTo(2);
        final var nyopprettetEtterlysning = etterlysninger.stream().filter(it -> it.getStatus().equals(EtterlysningStatus.OPPRETTET)).findFirst().orElseThrow();
        assertThat(nyopprettetEtterlysning.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(nyFom, tom));
        assertThat(nyopprettetEtterlysning.getGrunnlagsreferanse()).isEqualTo(ungdomsprogramPeriodeGrunnlag2.get().getGrunnlagsreferanse());

        final var mottattSvarEtterlysning = etterlysninger.stream().filter(it -> it.getStatus().equals(EtterlysningStatus.MOTTATT_SVAR)).findFirst().orElseThrow();
        assertThat(mottattSvarEtterlysning.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(gammelFom, tom));
        assertThat(mottattSvarEtterlysning.getGrunnlagsreferanse()).isEqualTo(ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse());
    }


    @Test
    void skal_opprette_ny_etterlysning_dersom_siste_etterlysning_med_mottatt_svar_ikke_er_gyldig_og_har_mottatt_svar_for_samme_endring_tidligere() {
        final var opprinneligFom = LocalDate.now().plusMonths(1);
        final var tom = opprinneligFom.plusYears(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(opprinneligFom, tom)));
        final var ungdomsprogramPeriodeGrunnlag1 = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();
        opprettEtterlysningMedMottattSvar(ungdomsprogramPeriodeGrunnlag1, opprinneligFom, tom);

        final var nyFom = LocalDate.now();
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(nyFom, tom)));
        final var ungdomsprogramPeriodeGrunnlag2 = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        opprettEtterlysningMedMottattSvar(ungdomsprogramPeriodeGrunnlag2, nyFom, tom);

        // Tilbake til opprinnelig fom
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(opprinneligFom, tom)));
        final var ungdomsprogramPeriodeGrunnlag3 = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        // act
        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(BehandlingReferanse.fra(behandling));

        // Assert
        final var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger.size()).isEqualTo(3);
        final var nyopprettetEtterlysning = etterlysninger.stream().filter(it -> it.getStatus().equals(EtterlysningStatus.OPPRETTET)).findFirst().orElseThrow();
        assertThat(nyopprettetEtterlysning.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(opprinneligFom, tom));
        assertThat(nyopprettetEtterlysning.getGrunnlagsreferanse()).isEqualTo(ungdomsprogramPeriodeGrunnlag3.getGrunnlagsreferanse());

        final var mottatteSvarEtterlysninger = etterlysninger.stream().filter(it -> it.getStatus().equals(EtterlysningStatus.MOTTATT_SVAR)).toList();
        assertThat(mottatteSvarEtterlysninger.size()).isEqualTo(2);

    }

    private void opprettEtterlysningMedMottattSvar(UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag, LocalDate nyFom, LocalDate tom) {
        final var eksisterendeEtterlysning = Etterlysning.opprettForType(behandling.getId(), ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse(), UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(nyFom, tom), EtterlysningType.UTTALELSE_ENDRET_PROGRAMPERIODE);
        eksisterendeEtterlysning.vent(LocalDateTime.now());
        final var svarJournalpostId = new JournalpostId(12L);
        mottatteDokumentRepository.lagre(new MottattDokument.Builder().medJournalPostId(svarJournalpostId)
                .medInnsendingstidspunkt(LocalDateTime.now())
                .medFagsakId(behandling.getFagsakId())
            .build(), DokumentStatus.GYLDIG);
        eksisterendeEtterlysning.mottattUttalelse(svarJournalpostId, true, null);
        etterlysningRepository.lagre(eksisterendeEtterlysning);
    }

    private void opprettEtterlysningPåVent(UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag, LocalDate fom, LocalDate tom) {
        final var eksisterendeEtterlysning = Etterlysning.opprettForType(behandling.getId(), ungdomsprogramPeriodeGrunnlag.getGrunnlagsreferanse(), UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), EtterlysningType.UTTALELSE_ENDRET_PROGRAMPERIODE);
        eksisterendeEtterlysning.vent(LocalDateTime.now());
        etterlysningRepository.lagre(eksisterendeEtterlysning);
    }


}
