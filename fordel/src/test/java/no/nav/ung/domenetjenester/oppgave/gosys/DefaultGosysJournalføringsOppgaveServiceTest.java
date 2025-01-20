package no.nav.ung.domenetjenester.oppgave.gosys;


import no.nav.k9.felles.integrasjon.oppgave.v1.*;
import no.nav.ung.domenetjenester.oppgave.behandlendeenhet.BehandlendeEnhet;
import no.nav.ung.domenetjenester.oppgave.behandlendeenhet.BehandlendeEnhetService;
import no.nav.ung.fordel.kodeverdi.GosysKonstanter;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.dokument.FordelBehandlingType;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultGosysJournalføringsOppgaveServiceTest {

    private OppgaveRestKlient oppgaveKlient;
    private BehandlendeEnhetService behandlendeEnhetService;

    private static GosysOppgaveService service;

    private OmrådeTema tema = OmrådeTema.OMS;

    @BeforeEach
    public void initialisering() {
        oppgaveKlient = mock(OppgaveRestKlient.class);
        behandlendeEnhetService = mock(BehandlendeEnhetService.class);
        service = new GosysOppgaveService(
            oppgaveKlient,
            behandlendeEnhetService);
    }

    @Test
    public void OpprettelseAvJournalføringsoppgave() {
        when(behandlendeEnhetService.hentBehandlendeEnhet(any(), any(), any())).thenReturn(new BehandlendeEnhet("123", "foo"));
        when(oppgaveKlient.opprettetOppgave(any(OpprettOppgave.class))).thenReturn(new Oppgave(
            111L,
            "666",
            "UNG",
            "SAKEN",
            "111",
            "OMS",
            "OMS",
            GosysKonstanter.OppgaveType.JOURNALFØRING.getKode(),
            "",
            1,
            "",
            LocalDate.now(),
            LocalDate.now(),
            Prioritet.NORM,
            Oppgavestatus.AAPNET
        ));

        String gosysOppgave = service.opprettOppgave(
            tema,
            BehandlingTema.PLEIEPENGER_LIVETS_SLUTTFASE,
            FordelBehandlingType.UDEFINERT,
            new AktørId("111"),
            new JournalpostId("666"),
            null,
            GosysKonstanter.Fagsaksystem.INFOTRYGD,
            GosysKonstanter.OppgaveType.JOURNALFØRING);

        verify(behandlendeEnhetService, times(1)).hentBehandlendeEnhet(
                any(), any(), any());

        verify(oppgaveKlient, times(1)).opprettetOppgave(any(OpprettOppgave.class));

        assertEquals("111", gosysOppgave);
    }

}
