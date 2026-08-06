package no.nav.ung.sak.web.app.tjenester.forvaltning;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class ForvaltningBehandlingRestTjenesteTest {

    private static final Saksnummer SAKSNUMMER = new Saksnummer("987654321");
    private static final String BEGRUNNELSE = "Feilaktig opprettet som følge av duplikatsøknad";

    @Inject
    private EntityManager entityManager;

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    private ForvaltningBehandlingRestTjeneste tjeneste;

    private final Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.UNGDOMSYTELSE).medSaksnummer(SAKSNUMMER).build();
    private Behandling behandling;

    @BeforeEach
    void setup() {
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        henleggBehandlingTjeneste = mock(HenleggBehandlingTjeneste.class);

        tjeneste = new ForvaltningBehandlingRestTjeneste(behandlingRepository, henleggBehandlingTjeneste);

        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    void skal_henlegge_aapen_behandling() {
        // Arrange
        var request = new ForvaltningBehandlingRestTjeneste.HenleggForvaltningRequest(
            BehandlingResultatType.HENLAGT_FEILOPPRETTET, BEGRUNNELSE
        );

        // Act
        Response response = tjeneste.henleggBehandling(behandling.getId(), request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verify(henleggBehandlingTjeneste).henleggBehandlingAvSaksbehandler(
            String.valueOf(behandling.getId()),
            BehandlingResultatType.HENLAGT_FEILOPPRETTET,
            BEGRUNNELSE
        );
    }

    @Test
    void skal_returnere_404_naar_behandling_ikke_finnes() {
        // Arrange
        var request = new ForvaltningBehandlingRestTjeneste.HenleggForvaltningRequest(
            BehandlingResultatType.HENLAGT_FEILOPPRETTET, BEGRUNNELSE
        );

        // Act
        Response response = tjeneste.henleggBehandling(Long.MAX_VALUE, request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        verifyNoInteractions(henleggBehandlingTjeneste);
    }

    @Test
    void skal_returnere_409_naar_behandling_er_avsluttet() {
        // Arrange
        behandling.avsluttBehandling();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        var request = new ForvaltningBehandlingRestTjeneste.HenleggForvaltningRequest(
            BehandlingResultatType.HENLAGT_FEILOPPRETTET, BEGRUNNELSE
        );

        // Act
        Response response = tjeneste.henleggBehandling(behandling.getId(), request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
        verifyNoInteractions(henleggBehandlingTjeneste);
    }

    @Test
    void skal_returnere_400_for_ikke_henleggbar_aarsak() {
        // Arrange — INNVILGET er ikke en gyldig henleggelseskode
        var request = new ForvaltningBehandlingRestTjeneste.HenleggForvaltningRequest(
            BehandlingResultatType.INNVILGET, BEGRUNNELSE
        );

        // Act
        Response response = tjeneste.henleggBehandling(behandling.getId(), request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verifyNoInteractions(henleggBehandlingTjeneste);
    }
}
