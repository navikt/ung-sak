package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AutomatiskGrunnbelopReguleringTaskTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    @FagsakYtelseTypeRef
    private RevurderingTjeneste revurderingTjenesteMock;

    @Inject
    private ProsessTaskRepository prosessTaskRepositoryMock;

    private AutomatiskGrunnbelopReguleringTask task;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;


    @Test
    public void skal_opprette_revurderingsbehandling_med_årsak_når_avsluttet_behandling() {

        Behandling behandling = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskGrunnbelopReguleringTask.TASKTYPE);
        prosessTaskData.setFagsak(behandling.getFagsakId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_SATS_REGULERING);
    }

    private void assertRevurdering(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        Optional< Behandling> revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Ingen revurdering").isPresent();
        List<BehandlingÅrsak> behandlingÅrsaker = revurdering.get().getBehandlingÅrsaker();
        assertThat(behandlingÅrsaker).isNotEmpty();
        List<BehandlingÅrsakType> årsaker = behandlingÅrsaker.stream().map(bå -> bå.getBehandlingÅrsakType()).collect(Collectors.toList());
        assertThat(årsaker).contains(behandlingÅrsakType);
    }

    private void assertIngenRevurdering(Fagsak fagsak) {
        Optional<Behandling> revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Har revurdering: " + fagsak.getId()).isNotPresent();
    }

    private void createTask() {
        task = new AutomatiskGrunnbelopReguleringTask(repositoryProvider, prosessTaskRepositoryMock);

    }

    @Test
    public void skal_ikke_opprette_revurdering_dersom_åpen_behandling_på_fagsak() {
        Behandling behandling = opprettRevurderingsKandidat(BehandlingStatus.UTREDES);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskGrunnbelopReguleringTask.TASKTYPE);
        prosessTaskData.setFagsak(behandling.getFagsakId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling.getFagsak());
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status) {
        LocalDate dato = LocalDate.now().plusDays(10);

        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medSøknadDato(dato.minusDays(20));

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT);
        scenario.medVilkårResultatType(VilkårResultatType.INNVILGET);

        scenario.medBehandlingVedtak()
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medVedtakstidspunkt(dato.minusWeeks(2).atStartOfDay())
            .medAnsvarligSaksbehandler("Severin Saksbehandler")
            .build();

        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));

        Behandling behandling = scenario.lagre(repositoryProvider);

        Whitebox.setInternalState(behandling, "status", status);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandling, LocalDate.now().minusYears(1), LocalDate.now(), false);

        repoRule.getRepository().flushAndClear();

        return repoRule.getEntityManager().find(Behandling.class, behandling.getId());
    }

}
