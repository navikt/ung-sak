package no.nav.k9.sak.web.app.tjenester.historikk;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagDelDto;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagDto;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagHendelseDto;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;

public class HistorikkRestTjenesteTest {

    private HistorikkTjenesteAdapter historikkApplikasjonTjenesteMock;
    private HistorikkRestTjeneste historikkRestTjeneste;

    @Before
    public void setUp() {
        historikkApplikasjonTjenesteMock = mock(HistorikkTjenesteAdapter.class);
        historikkRestTjeneste = new HistorikkRestTjeneste(historikkApplikasjonTjenesteMock);
    }

    @SuppressWarnings("resource")
    @Test
    public void hentAlleInnslag() {
        // Arrange
        HistorikkinnslagDto innslagDto = new HistorikkinnslagDto();
        lagHistorikkinnslagDel(innslagDto);
        innslagDto.setDokumentLinks(Collections.emptyList());
        when(historikkApplikasjonTjenesteMock.hentAlleHistorikkInnslagForSak(Mockito.any(Saksnummer.class)))
            .thenReturn(Collections.singletonList(innslagDto));

        // Act
        historikkRestTjeneste.hentAlleInnslag(null, new SaksnummerDto("1234"));

        // Assert
        verify(historikkApplikasjonTjenesteMock).hentAlleHistorikkInnslagForSak(Mockito.any(Saksnummer.class));
    }

    private void lagHistorikkinnslagDel(HistorikkinnslagDto innslagDto) {
        HistorikkinnslagDelDto delDto = new HistorikkinnslagDelDto();
        lagHendelseDto(delDto);
        innslagDto.setHistorikkinnslagDeler(Collections.singletonList(delDto));
    }

    private void lagHendelseDto(HistorikkinnslagDelDto delDto) {
        HistorikkinnslagHendelseDto hendelseDto = new HistorikkinnslagHendelseDto();
        hendelseDto.setNavn(HistorikkinnslagType.BEH_STARTET);
        delDto.setHendelse(hendelseDto);
    }
}
