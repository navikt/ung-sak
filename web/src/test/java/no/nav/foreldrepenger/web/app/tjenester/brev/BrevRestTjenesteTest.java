package no.nav.foreldrepenger.web.app.tjenester.brev;

import static no.nav.k9.kodeverk.dokument.DokumentMalType.INNHENT_DOK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.kodeverk.dokument.DokumentMalRestriksjon;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.kontrakt.dokument.BrevmalDto;

public class BrevRestTjenesteTest {

    private BrevRestTjeneste brevRestTjeneste;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjenesteMock;
    private DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock;
    private BehandlingRepository behandlingRepository;

    @Before
    public void setUp() {
        dokumentBestillerApplikasjonTjenesteMock = mock(DokumentBestillerApplikasjonTjeneste.class);
        dokumentBehandlingTjenesteMock = mock(DokumentBehandlingTjeneste.class);
        behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(mock(Behandling.class));

        brevRestTjeneste = new BrevRestTjeneste(dokumentBestillerApplikasjonTjenesteMock, dokumentBehandlingTjenesteMock, behandlingRepository);
    }

    @Test
    public void bestillerDokument() {
        // Arrange
        long behandlingId = 2L;
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandlingId, INNHENT_DOK, "Dette er en fritekst");

        // Act
        brevRestTjeneste.bestillDokument(bestillBrevDto);

        // Assert
        verify(dokumentBestillerApplikasjonTjenesteMock).bestillDokument(eq(bestillBrevDto), eq(HistorikkAktør.SAKSBEHANDLER));
    }

    @Test
    public void henterBrevmaler() {
        // Arrange
        long behandlingId = 1L;
        when(dokumentBehandlingTjenesteMock.hentBrevmalerFor(behandlingId))
            .thenReturn(Collections.singletonList(new BrevmalDto("INNHEN", "Innhent dokumentasjon", DokumentMalRestriksjon.INGEN, true)));
        Behandling behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(behandlingId);
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);

        // Act
        List<BrevmalDto> brevmaler = brevRestTjeneste.hentMaler(new BehandlingUuidDto(UUID.randomUUID()));

        // Assert
        verify(dokumentBehandlingTjenesteMock).hentBrevmalerFor(behandlingId);
        assertThat(brevmaler).hasSize(1);
        assertThat(brevmaler.get(0).getNavn()).isEqualTo("Innhent dokumentasjon");
    }

    @Test
    public void harSendtVarselOmRevurdering() {
        // Arrange
        when(dokumentBehandlingTjenesteMock.erDokumentProdusert(any(), any())).thenReturn(true);
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(mock(Behandling.class));

        // Act
        Boolean harSendt = brevRestTjeneste.harSendtVarselOmRevurdering(new BehandlingUuidDto(UUID.randomUUID()));

        // Assert
        verify(dokumentBehandlingTjenesteMock).erDokumentProdusert(any(), any());
        assertThat(harSendt).isEqualTo(true);
    }
}
