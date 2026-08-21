package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class BostedAvklaringTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 1, 31);

    @Inject
    private EntityManager entityManager;

    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private BostedAvklaringTjeneste tjeneste;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        etterlysningRepository = new EtterlysningRepository(entityManager);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        BostedsGrunnlagRepository bostedsGrunnlagRepository = new BostedsGrunnlagRepository(entityManager);

        tjeneste = new BostedAvklaringTjeneste(
            bostedsGrunnlagRepository,
            null,
            etterlysningRepository,
            prosessTaskTjeneste
        );

        behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
    }

    private BostedsPeriodeAvklaring lagAvklaring(LocalDate fom, LocalDate tom, boolean skalSendeVarsel) {
        return lagAvklaring(fom, tom, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, "begrunnelse", skalSendeVarsel);
    }

    private BostedsPeriodeAvklaring lagAvklaring(LocalDate fom, LocalDate tom, BostedsvilkårIkkeOppfyltÅrsak årsak, String begrunnelse, boolean skalSendeVarsel) {
        return new BostedsPeriodeAvklaring(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            årsak,
            begrunnelse,
            skalSendeVarsel,
            null,
            skalSendeVarsel ? null : "begrunnelse for at det ikke varsles",
            UUID.randomUUID().toString(),
            LocalDateTime.now(),
            Avklaringtype.AVSLAG
        );
    }

    private Etterlysning lagOgLagreEtterlysningSomVenterPåSvar(BostedsPeriodeAvklaring avklaring) {
        return etterlysningRepository.lagre(Etterlysning.opprettForType(
            behandling.getId(),
            avklaring.getReferanse(),
            UUID.randomUUID(),
            avklaring.getPeriode(),
            EtterlysningType.UTTALELSE_BOSTED
        ));
    }

    @Test
    void eksakt_match_på_innhold_skal_ikke_påvirke_eksisterende_etterlysning() {
        var tidligereAvklaring = lagAvklaring(FOM, TOM, true);
        var etterlysningSomVenter = lagOgLagreEtterlysningSomVenterPåSvar(tidligereAvklaring);

        // Ny avklaring med identisk innhold (men ny referanse), slik at det er en ren "resave" av samme innhold
        var nyAvklaringMedSammeInnhold = lagAvklaring(FOM, TOM, true);

        tjeneste.oppdaterEtterlysninger(
            behandling,
            List.of(tidligereAvklaring),
            List.of(nyAvklaringMedSammeInnhold)
        );

        assertThat(etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId())).isEmpty();
        assertThat(etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BOSTED))
            .extracting(Etterlysning::getId)
            .containsExactly(etterlysningSomVenter.getId());

        assertThat(etterlysningRepository.hentEtterlysning(etterlysningSomVenter.getId()).getStatus()).isEqualTo(EtterlysningStatus.OPPRETTET);

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void delvis_overlapp_med_tidligere_etterlysning_skal_avbrytes_og_ny_opprettes() {
        var tidligereAvklaring = lagAvklaring(FOM, TOM, true);
        var etterlysningSomVenter = lagOgLagreEtterlysningSomVenterPåSvar(tidligereAvklaring);

        var nyTom = LocalDate.of(2024, 2, 15);
        var nyAvklaringMedEndretInnhold = lagAvklaring(FOM, nyTom, true);

        tjeneste.oppdaterEtterlysninger(
            behandling,
            List.of(tidligereAvklaring),
            List.of(nyAvklaringMedEndretInnhold)
        );

        var avbrutteEtterlysninger = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId());
        assertThat(avbrutteEtterlysninger).extracting(Etterlysning::getId).containsExactly(etterlysningSomVenter.getId());

        var nyeEtterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BOSTED);
        assertThat(nyeEtterlysninger).hasSize(1);
        assertThat(nyeEtterlysninger.getFirst().getGrunnlagsreferanse()).isEqualTo(nyAvklaringMedEndretInnhold.getReferanse());

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(2)).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues())
            .extracting(ProsessTaskData::getTaskType)
            .containsExactlyInAnyOrder(AvbrytEtterlysningTask.TASKTYPE, OpprettEtterlysningTask.TASKTYPE);
    }

    @Test
    void ny_avklaring_som_ikke_skal_varsles_skal_avbryte_eksisterende_etterlysninger_uten_å_opprette_nye() {
        var tidligereAvklaring = lagAvklaring(FOM, TOM, true);
        var etterlysningSomVenter = lagOgLagreEtterlysningSomVenterPåSvar(tidligereAvklaring);

        var nyAvklaringUtenVarsel = lagAvklaring(FOM, TOM, false);

        tjeneste.oppdaterEtterlysninger(
            behandling,
            List.of(tidligereAvklaring),
            List.of(nyAvklaringUtenVarsel)
        );

        var avbrutteEtterlysninger = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId());
        assertThat(avbrutteEtterlysninger).extracting(Etterlysning::getId).containsExactly(etterlysningSomVenter.getId());
        assertThat(etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BOSTED)).isEmpty();

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(1)).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskType()).isEqualTo(AvbrytEtterlysningTask.TASKTYPE);
    }

    @Test
    void ny_avklaring_uten_tidligere_avklaring_skal_opprette_etterlysning_og_task() {
        var nyAvklaring = lagAvklaring(FOM, TOM, true);

        tjeneste.oppdaterEtterlysninger(
            behandling,
            List.of(),
            List.of(nyAvklaring)
        );

        assertThat(etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId())).isEmpty();

        var nyeEtterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BOSTED);
        assertThat(nyeEtterlysninger).hasSize(1);
        assertThat(nyeEtterlysninger.getFirst().getGrunnlagsreferanse()).isEqualTo(nyAvklaring.getReferanse());
        assertThat(nyeEtterlysninger.getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(1)).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskType()).isEqualTo(OpprettEtterlysningTask.TASKTYPE);
        assertThat(taskCaptor.getValue().getPropertyValue(OpprettEtterlysningTask.ETTERLYSNING_TYPE))
            .isEqualTo(EtterlysningType.UTTALELSE_BOSTED.getKode());
    }

    @Test
    void eksisterende_etterlysning_uten_matchende_tidligere_avklaring_skal_avbrytes_når_ny_avklaring_opprettes() {
        var eksisterendeEtterlysning = etterlysningRepository.lagre(Etterlysning.opprettForType(
            behandling.getId(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            EtterlysningType.UTTALELSE_BOSTED
        ));

        var nyAvklaring = lagAvklaring(FOM, TOM, true);

        tjeneste.oppdaterEtterlysninger(
            behandling,
            List.of(),
            List.of(nyAvklaring)
        );

        var avbrutteEtterlysninger = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId());
        assertThat(avbrutteEtterlysninger).extracting(Etterlysning::getId).containsExactly(eksisterendeEtterlysning.getId());

        var nyeEtterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BOSTED);
        assertThat(nyeEtterlysninger).hasSize(1);
        assertThat(nyeEtterlysninger.getFirst().getGrunnlagsreferanse()).isEqualTo(nyAvklaring.getReferanse());

        verify(prosessTaskTjeneste, times(2)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void endret_årsak_med_uendret_periode_skal_avbryte_og_opprette_ny_etterlysning() {
        var tidligereAvklaring = lagAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, "begrunnelse", true);
        var etterlysningSomVenter = lagOgLagreEtterlysningSomVenterPåSvar(tidligereAvklaring);

        // Samme periode, men annen årsak -> innholdet regnes som endret selv om perioden er uendret
        var nyAvklaringMedEndretÅrsak = lagAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM, "begrunnelse", true);

        tjeneste.oppdaterEtterlysninger(
            behandling,
            List.of(tidligereAvklaring),
            List.of(nyAvklaringMedEndretÅrsak)
        );

        var avbrutteEtterlysninger = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId());
        assertThat(avbrutteEtterlysninger).extracting(Etterlysning::getId).containsExactly(etterlysningSomVenter.getId());

        var nyeEtterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BOSTED);
        assertThat(nyeEtterlysninger).hasSize(1);
        assertThat(nyeEtterlysninger.getFirst().getGrunnlagsreferanse()).isEqualTo(nyAvklaringMedEndretÅrsak.getReferanse());

        verify(prosessTaskTjeneste, times(2)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void endret_begrunnelse_med_uendret_periode_skal_avbryte_og_opprette_ny_etterlysning() {
        var tidligereAvklaring = lagAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, "begrunnelse", true);
        var etterlysningSomVenter = lagOgLagreEtterlysningSomVenterPåSvar(tidligereAvklaring);

        // Samme periode og årsak, men annen begrunnelse -> innholdet regnes som endret
        var nyAvklaringMedEndretBegrunnelse = lagAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, "en annen begrunnelse", true);

        tjeneste.oppdaterEtterlysninger(
            behandling,
            List.of(tidligereAvklaring),
            List.of(nyAvklaringMedEndretBegrunnelse)
        );

        var avbrutteEtterlysninger = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId());
        assertThat(avbrutteEtterlysninger).extracting(Etterlysning::getId).containsExactly(etterlysningSomVenter.getId());

        var nyeEtterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BOSTED);
        assertThat(nyeEtterlysninger).hasSize(1);
        assertThat(nyeEtterlysninger.getFirst().getGrunnlagsreferanse()).isEqualTo(nyAvklaringMedEndretBegrunnelse.getReferanse());

        verify(prosessTaskTjeneste, times(2)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void ny_avklaring_uten_varsel_og_uten_tidligere_avklaring_skal_ikke_opprette_etterlysninger() {
        var nyAvklaringUtenVarsel = lagAvklaring(FOM, TOM, false);

        tjeneste.oppdaterEtterlysninger(
            behandling,
            List.of(),
            List.of(nyAvklaringUtenVarsel)
        );

        assertThat(etterlysningRepository.hentEtterlysninger(behandling.getId())).isEmpty();
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }
}
