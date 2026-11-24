package no.nav.ung.sak.domene.behandling.steg.iverksettevedtak;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.etterlysning.EtterlysningProssesseringTjeneste;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class HenleggBehandlingTjenesteTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    @Any
    private Instance<HenleggelsePostopsTjeneste> henleggelsePostopsTjenester;

    private final BehandlingskontrollTjeneste behandlingskontrollTjeneste = mock(BehandlingskontrollTjeneste.class);
    private final EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste = mock(EtterlysningProssesseringTjeneste.class);
    private BehandlingRepository behandlingRepository;
    private HenleggBehandlingTjeneste tjeneste;
    private Behandling førstegangsbehandling;

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        tjeneste = new HenleggBehandlingTjeneste(repositoryProvider, behandlingskontrollTjeneste, mock(FagsakProsessTaskRepository.class),
            mock(ProsessTaskTjeneste.class), new MottatteDokumentRepository(entityManager), henleggelsePostopsTjenester,
            etterlysningProssesseringTjeneste);
        førstegangsbehandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
    }

    @Test
    void saksbehandlers_henleggelse_av_kontrollbehandling_avbryter_åpne_etterlysninger() {
        var kontroll = nyBehandling(BehandlingType.KONTROLL);

        tjeneste.henleggBehandlingAvSaksbehandler(kontroll.getId().toString(), BehandlingResultatType.HENLAGT_FEILOPPRETTET, "Feilopprettet");

        verify(etterlysningProssesseringTjeneste).avbrytÅpneEtterlysninger(argThat(b -> b.getId().equals(kontroll.getId())));
        verify(behandlingskontrollTjeneste).henleggBehandling(any(), eq(BehandlingResultatType.HENLAGT_FEILOPPRETTET));
    }

    @Test
    void maskinell_henleggelse_av_kontrollbehandling_avbryter_åpne_etterlysninger() {
        var kontroll = nyBehandling(BehandlingType.KONTROLL);

        tjeneste.henleggBehandlingOgAksjonspunkter(kontroll.getId().toString(), BehandlingResultatType.HENLAGT_MASKINELT, "Ingen måneder");

        verify(etterlysningProssesseringTjeneste).avbrytÅpneEtterlysninger(argThat(b -> b.getId().equals(kontroll.getId())));
        verify(behandlingskontrollTjeneste).henleggBehandling(any(), eq(BehandlingResultatType.HENLAGT_MASKINELT));
    }

    @Test
    void henleggelse_av_revurdering_avbryter_ikke_etterlysninger() {
        var revurdering = nyBehandling(BehandlingType.REVURDERING);

        tjeneste.henleggBehandlingAvSaksbehandler(revurdering.getId().toString(), BehandlingResultatType.HENLAGT_FEILOPPRETTET, "Feilopprettet");

        verifyNoInteractions(etterlysningProssesseringTjeneste);
        verify(behandlingskontrollTjeneste).henleggBehandling(any(), eq(BehandlingResultatType.HENLAGT_FEILOPPRETTET));
    }

    private Behandling nyBehandling(BehandlingType type) {
        var behandling = Behandling.fraTidligereBehandling(førstegangsbehandling, type).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        entityManager.flush();
        return behandling;
    }
}
