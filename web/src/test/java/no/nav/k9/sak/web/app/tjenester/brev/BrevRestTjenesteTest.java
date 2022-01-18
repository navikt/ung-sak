package no.nav.k9.sak.web.app.tjenester.brev;

import static no.nav.k9.kodeverk.dokument.DokumentMalType.INNHENT_DOK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.dokument.bestill.DokumentBehandlingTjeneste;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.kontrakt.vedtak.VedtakVarselDto;

public class BrevRestTjenesteTest {

    private BrevRestTjeneste brevRestTjeneste;
    private final DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjenesteMock = mock(DokumentBestillerApplikasjonTjeneste.class);
    private final DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock = mock(DokumentBehandlingTjeneste.class);
    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private final VilkårResultatRepository vilkårResultatRepository = mock(VilkårResultatRepository.class);
    private final VedtakVarselRepository vedtakVarselRepository = mock(VedtakVarselRepository.class);
    private final BehandlingVedtakRepository behandlingVedtakRepository = mock(BehandlingVedtakRepository.class);

    @BeforeEach
    public void setUp() {
        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(mock(Behandling.class));

        brevRestTjeneste = new BrevRestTjeneste(vedtakVarselRepository, behandlingVedtakRepository, vilkårResultatRepository, behandlingRepository, dokumentBestillerApplikasjonTjenesteMock, dokumentBehandlingTjenesteMock);
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

    @SuppressWarnings("resource")
    @Test
    public void skal_hente_redusertUtbetalingÅrsaker_hentVedtakVarsel() {
        // Arrange
        UUID behandlingUuid = UUID.randomUUID();
        Optional<VedtakVarsel> mockVedtakVarsel = Optional.of(mock(VedtakVarsel.class));
        Set<String> årsaker = Set.of("ÅRSAK_1", "ÅRSAK_2");
        when(mockVedtakVarsel.get().getRedusertUtbetalingÅrsaker()).thenReturn(årsaker);
        when(vedtakVarselRepository.hentHvisEksisterer(behandlingUuid)).thenReturn(mockVedtakVarsel);

        // Act
        Response response = brevRestTjeneste.hentVedtakVarsel(new BehandlingUuidDto(behandlingUuid));

        // Assert
        Set<String> redusertUtbetalingÅrsaker = ((VedtakVarselDto) response.getEntity()).getRedusertUtbetalingÅrsaker();
        assertThat(redusertUtbetalingÅrsaker).isEqualTo(årsaker);

    }
}
