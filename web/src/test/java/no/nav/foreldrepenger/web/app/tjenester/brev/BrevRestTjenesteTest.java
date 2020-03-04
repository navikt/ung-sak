package no.nav.foreldrepenger.web.app.tjenester.brev;

import static no.nav.k9.kodeverk.dokument.DokumentMalType.INNHENT_DOK;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;

public class BrevRestTjenesteTest {

    private BrevRestTjeneste brevRestTjeneste;
    private final DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjenesteMock = mock(DokumentBestillerApplikasjonTjeneste.class);
    private final DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock = mock(DokumentBehandlingTjeneste.class);
    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private final VedtakVarselRepository vedtakVarselRepository = mock(VedtakVarselRepository.class);
    private final BehandlingVedtakRepository behandlingVedtakRepository = mock(BehandlingVedtakRepository.class);

    @Before
    public void setUp() {
        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(mock(Behandling.class));

        brevRestTjeneste = new BrevRestTjeneste(vedtakVarselRepository, behandlingVedtakRepository, dokumentBestillerApplikasjonTjenesteMock, dokumentBehandlingTjenesteMock);
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
}
