package no.nav.ung.sak.etterlysning;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifiserer den vilkårsuavhengige logikken i {@link VilkårsavklaringEtterlysningTjeneste#oppdaterEtterlysninger}
 * (eksakt-match, endret periode, ny avklaring uten tidligere, ingen varsel). Bruker BISTANDSVILKÅR kun som et
 * konkret eksempel på en vilkårstype — vilkårspesifikk logikk testes i den enkelte ytelsesmodulen.
 */
@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VilkårsavklaringEtterlysningTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 1, 31);

    @Inject
    private EntityManager entityManager;

    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private VilkårsavklaringEtterlysningTjeneste vilkårsavklaringEtterlysningTjeneste;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        etterlysningRepository = new EtterlysningRepository(entityManager);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        vilkårsavklaringEtterlysningTjeneste = new VilkårsavklaringEtterlysningTjeneste(etterlysningRepository, prosessTaskTjeneste);

        behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
    }

    @Test
    void eksakt_match_på_innhold_skal_ikke_påvirke_eksisterende_etterlysning() {
        var tidligereAvklaring = lagAvklaring(FOM, TOM, true);
        var etterlysningSomVenter = lagOgLagreEtterlysningSomVenterPåSvar(tidligereAvklaring);

        var nyAvklaringMedSammeInnhold = lagAvklaring(FOM, TOM, true);

        vilkårsavklaringEtterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_BISTAND, TestVilkårsvarselInnhold.tilMap(VilkårType.BISTANDSVILKÅR, tidligereAvklaring), TestVilkårsvarselInnhold.tilMap(VilkårType.BISTANDSVILKÅR, nyAvklaringMedSammeInnhold));

        assertThat(etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId())).isEmpty();
        assertThat(etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BISTAND))
            .extracting(Etterlysning::getId)
            .containsExactly(etterlysningSomVenter.getId());
        assertThat(etterlysningRepository.hentEtterlysning(etterlysningSomVenter.getId()).getStatus()).isEqualTo(EtterlysningStatus.OPPRETTET);

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void endret_periode_skal_avbryte_og_opprette_ny_etterlysning() {
        var tidligereAvklaring = lagAvklaring(FOM, TOM, true);
        var etterlysningSomVenter = lagOgLagreEtterlysningSomVenterPåSvar(tidligereAvklaring);

        var nyAvklaring = lagAvklaring(FOM, LocalDate.of(2024, 2, 15), true);

        vilkårsavklaringEtterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_BISTAND, TestVilkårsvarselInnhold.tilMap(VilkårType.BISTANDSVILKÅR, tidligereAvklaring), TestVilkårsvarselInnhold.tilMap(VilkårType.BISTANDSVILKÅR, nyAvklaring));

        assertThat(etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId()))
            .extracting(Etterlysning::getId).containsExactly(etterlysningSomVenter.getId());

        var nyeEtterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BISTAND);
        assertThat(nyeEtterlysninger).hasSize(1);
        assertThat(nyeEtterlysninger.getFirst().getGrunnlagsreferanse()).isEqualTo(nyAvklaring.getReferanse());

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(2)).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues())
            .extracting(ProsessTaskData::getTaskType)
            .containsExactlyInAnyOrder(AvbrytEtterlysningTask.TASKTYPE, OpprettEtterlysningTask.TASKTYPE);
    }

    @Test
    void ny_avklaring_uten_tidligere_avklaring_skal_opprette_etterlysning_og_task() {
        var nyAvklaring = lagAvklaring(FOM, TOM, true);

        vilkårsavklaringEtterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_BISTAND, Map.of(), TestVilkårsvarselInnhold.tilMap(VilkårType.BISTANDSVILKÅR, nyAvklaring));

        assertThat(etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId())).isEmpty();

        var nyeEtterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BISTAND);
        assertThat(nyeEtterlysninger).hasSize(1);
        assertThat(nyeEtterlysninger.getFirst().getGrunnlagsreferanse()).isEqualTo(nyAvklaring.getReferanse());
        assertThat(nyeEtterlysninger.getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(1)).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskType()).isEqualTo(OpprettEtterlysningTask.TASKTYPE);
        assertThat(taskCaptor.getValue().getPropertyValue(OpprettEtterlysningTask.ETTERLYSNING_TYPE))
            .isEqualTo(EtterlysningType.UTTALELSE_BISTAND.getKode());
    }

    @Test
    void ny_avklaring_uten_varsel_skal_ikke_opprette_etterlysninger() {
        var nyAvklaringUtenVarsel = lagAvklaring(FOM, TOM, false);

        vilkårsavklaringEtterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_BISTAND, Map.of(), TestVilkårsvarselInnhold.tilMap(VilkårType.BISTANDSVILKÅR, nyAvklaringUtenVarsel));

        assertThat(etterlysningRepository.hentEtterlysninger(behandling.getId())).isEmpty();
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    private VilkårPeriodeAvklaringForeslått lagAvklaring(LocalDate fom, LocalDate tom, boolean skalSendeVarsel) {
        return new VilkårPeriodeAvklaringForeslått(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK.getKode(),
            "begrunnelse",
            skalSendeVarsel,
            null,
            skalSendeVarsel ? null : "begrunnelse for at det ikke varsles",
            BistandsavklaringKildeType.BRUKER,
            null,
            "A12345",
            LocalDateTime.now(),
            Avklaringtype.AVSLAG
        );
    }

    private Etterlysning lagOgLagreEtterlysningSomVenterPåSvar(VilkårPeriodeAvklaring avklaring) {
        return etterlysningRepository.lagre(Etterlysning.opprettForType(
            behandling.getId(),
            avklaring.getReferanse(),
            UUID.randomUUID(),
            avklaring.getPeriode(),
            EtterlysningType.UTTALELSE_BISTAND
        ));
    }
}
