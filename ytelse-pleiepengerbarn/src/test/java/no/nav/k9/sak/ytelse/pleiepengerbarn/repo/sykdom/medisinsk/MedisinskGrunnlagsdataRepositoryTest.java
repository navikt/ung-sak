package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.fagsak.FagsakBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class MedisinskGrunnlagsdataRepositoryTest {
    private static final Saksnummer SAKSNUMMER = new Saksnummer("A21A");
    private static final AktørId PLEIETRENGENDE_AKTØR_ID = new AktørId("023456789");
    private static final AktørId SØKER_AKTØR_ID = new AktørId("222456789");
    @Inject
    private MedisinskGrunnlagRepository repo;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;


    @Test
    void sjekkGyldigHqlSyntax() {
        repo.hentSisteBehandlingFør(SAKSNUMMER, UUID.randomUUID());
        repo.hentGrunnlagForBehandling(UUID.randomUUID());
    }


    @Test
    void skalIkkeFinneBehandlingDersomKunEnEksisterer() {
        // Arrange
        var fagsak = lagFagsak();
        var behandling = lagFørstegangsbehandling(fagsak);
        lagGrunnlag(fagsak, behandling);

        // Act
        var uuid = repo.hentSisteBehandlingFør(fagsak.getSaksnummer(), behandling.getUuid());

        // Assert
        assertThat(uuid.isEmpty()).isTrue();
    }


    @Test
    void skalFinneForrigeBehandlingForRevurdering() {
        // Arrange
        var fagsak = lagFagsak();
        var original = lagFørstegangsbehandling(fagsak);
        lagGrunnlag(fagsak, original);
        var revurdering = lagRevurdering(original);
        lagGrunnlag(fagsak, revurdering);

        // Act
        var uuid = repo.hentSisteBehandlingFør(fagsak.getSaksnummer(), revurdering.getUuid());

        // Assert
        assertThat(uuid.isPresent()).isTrue();
        assertThat(uuid.get()).isEqualTo(original.getUuid());
    }

    @Test
    void skalFinneBehandlingenFørOmInnsendtBehandlingIkkeSiste() {
        // Arrange
        var fagsak = lagFagsak();
        var original = lagFørstegangsbehandling(fagsak);
        lagGrunnlag(fagsak, original);
        var revurdering = lagRevurdering(original);
        lagGrunnlag(fagsak, revurdering);
        var revurdering2 = lagRevurdering(revurdering);
        lagGrunnlag(fagsak, revurdering2);

        // Act
        var uuid = repo.hentSisteBehandlingFør(fagsak.getSaksnummer(), revurdering.getUuid());

        // Assert
        assertThat(uuid.isPresent()).isTrue();
        assertThat(uuid.get()).isEqualTo(original.getUuid());
    }

    @Test
    void skalFinneForrigeRevurderingOmFlereBehandlingerOgInnsendtSiste() {
        // Arrange
        var fagsak = lagFagsak();
        var original = lagFørstegangsbehandling(fagsak);
        lagGrunnlag(fagsak, original);
        var revurdering = lagRevurdering(original);
        lagGrunnlag(fagsak, revurdering);
        var revurdering2 = lagRevurdering(revurdering);
        lagGrunnlag(fagsak, revurdering2);

        // Act
        var uuid = repo.hentSisteBehandlingFør(fagsak.getSaksnummer(), revurdering2.getUuid());

        // assert
        assertThat(uuid.isPresent()).isTrue();
        assertThat(uuid.get()).isEqualTo(revurdering.getUuid());
    }


    private void lagGrunnlag(Fagsak fagsak, Behandling original) {
        repo.utledOgLagreGrunnlag(fagsak.getSaksnummer(), original.getUuid(), SØKER_AKTØR_ID, PLEIETRENGENDE_AKTØR_ID,
            new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()))),
            new TreeSet<>()
        );
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.PLEIEPENGER_SYKT_BARN).medBruker(SØKER_AKTØR_ID).medPleietrengende(PLEIETRENGENDE_AKTØR_ID)
            .medSaksnummer(SAKSNUMMER).build();
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    private Behandling lagFørstegangsbehandling(Fagsak fagsak) {
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        return behandling;
    }

    private Behandling lagRevurdering(Behandling behandling) {
        Behandling revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, behandlingLås);
        return revurdering;
    }


}
