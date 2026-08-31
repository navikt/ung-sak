package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.etterlysning.VilkårsavklaringEtterlysningTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class BistandAvklaringTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 1, 31);

    @Inject
    private EntityManager entityManager;

    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private BistandAvklaringTjeneste tjeneste;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        etterlysningRepository = new EtterlysningRepository(entityManager);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        vilkårsavklaringGrunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);

        tjeneste = new BistandAvklaringTjeneste(
            vilkårsavklaringGrunnlagRepository,
            new VilkårsavklaringEtterlysningTjeneste(etterlysningRepository, prosessTaskTjeneste),
            null,
            null
        );

        behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
    }

    @Test
    void skal_lagre_foreslatt_avklaring_pa_bistandsvilkaret() {
        var innhold = new BistandAvklaringInnhold(
            new no.nav.ung.sak.typer.Periode(FOM, TOM),
            BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK,
            "begrunnelse",
            true,
            "fritekst til varsel",
            null,
            Avklaringtype.AVSLAG);

        var lagret = tjeneste.lagreForeslåttAvklaringOgSettVilkårIkkeVurdert(List.of(innhold), "A12345", LocalDateTime.now(), behandling.getId());

        assertThat(lagret).hasSize(1);
        assertThat(tjeneste.hentForeslåtteAvklaringer(behandling.getId()))
            .extracting(VilkårPeriodeAvklaring::getIkkeOppfyltÅrsakKode)
            .containsExactly(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK.getKode());

        // Skal ikke berøre andre vilkårstyper
        assertThat(vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BOSTEDSVILKÅR)).isEmpty();
    }

    @Test
    void skal_hente_seneste_avklaring_for_behandling() {
        var avklaring = lagreAvklaring(lagAvklaring(FOM, TOM, true));

        var senesteAvklaring = tjeneste.hentSenesteAvklaringForBehandling(behandling.getId()).orElseThrow();

        assertThat(senesteAvklaring.avklaringtype()).isEqualTo(Avklaringtype.AVSLAG);
        assertThat(senesteAvklaring.periode()).isEqualTo(avklaring.getPeriode());
    }

    @Test
    void skal_ferdigstille_foreslatte_avklaringer() {
        lagreAvklaring(lagAvklaring(FOM, TOM, true));

        tjeneste.ferdigstillForeslåtteAvklaringer(behandling.getId());

        var grunnlag = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BISTANDSVILKÅR).orElseThrow();
        // Foreslåtte avklaringer beholdes urørt etter ferdigstilling (jf. fase 0), men speiles inn i holderen
        assertThat(grunnlag.getForeslåtteAvklaringer()).hasSize(1);
        assertThat(grunnlag.getFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getPeriode)
            .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
    }

    @Test
    void eksakt_match_på_innhold_skal_ikke_påvirke_eksisterende_etterlysning() {
        var tidligereAvklaring = lagAvklaring(FOM, TOM, true);
        var etterlysningSomVenter = lagOgLagreEtterlysningSomVenterPåSvar(tidligereAvklaring);

        var nyAvklaringMedSammeInnhold = lagAvklaring(FOM, TOM, true);

        tjeneste.oppdaterEtterlysninger(behandling, List.of(tidligereAvklaring), List.of(nyAvklaringMedSammeInnhold));

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

        tjeneste.oppdaterEtterlysninger(behandling, List.of(tidligereAvklaring), List.of(nyAvklaring));

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

        tjeneste.oppdaterEtterlysninger(behandling, List.of(), List.of(nyAvklaring));

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

        tjeneste.oppdaterEtterlysninger(behandling, List.of(), List.of(nyAvklaringUtenVarsel));

        assertThat(etterlysningRepository.hentEtterlysninger(behandling.getId())).isEmpty();
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    private VilkårPeriodeAvklaring lagreAvklaring(VilkårPeriodeAvklaringForeslått avklaring) {
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BISTANDSVILKÅR, Set.of(avklaring));
        return lagret.stream()
            .filter(a -> a.getPeriode().equals(avklaring.getPeriode()) && a.skalSendeVarsel() == avklaring.skalSendeVarsel())
            .findFirst()
            .orElseThrow();
    }

    private VilkårPeriodeAvklaringForeslått lagAvklaring(LocalDate fom, LocalDate tom, boolean skalSendeVarsel) {
        return new VilkårPeriodeAvklaringForeslått(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK.getKode(),
            "begrunnelse",
            skalSendeVarsel,
            null,
            skalSendeVarsel ? null : "begrunnelse for at det ikke varsles",
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



