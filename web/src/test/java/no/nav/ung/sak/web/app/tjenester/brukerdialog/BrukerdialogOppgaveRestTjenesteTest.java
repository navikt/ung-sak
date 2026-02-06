package no.nav.ung.sak.web.app.tjenester.brukerdialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import no.nav.ung.sak.JsonObjectMapper;
import no.nav.ung.sak.kontrakt.oppgaver.BrukerdialogOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveStatus;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettSøkYtelseOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.søkytelse.SøkYtelseOppgavetypeDataDTO;
import no.nav.ung.sak.oppgave.veileder.VeilederOppgaveTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrukerdialogOppgaveRestTjenesteTest {

    private VeilederOppgaveTjeneste veilederOppgaveTjeneste = mock(VeilederOppgaveTjeneste.class);
    private BrukerdialogOppgaveRestTjeneste brukerdialogOppgaveRestTjeneste;

    @BeforeEach
    void setUp() {

        SøkYtelseOppgavetypeDataDTO oppgavetypeData = new SøkYtelseOppgavetypeDataDTO(
            LocalDate.now()
        );
        BrukerdialogOppgaveDto t = new BrukerdialogOppgaveDto(UUID.randomUUID(), OppgaveType.SØK_YTELSE, oppgavetypeData, null,
            OppgaveStatus.ULØST, ZonedDateTime.now(),
            null,
            null, null, null);
        when(veilederOppgaveTjeneste.opprettSøkYtelseOppgave(any())).thenReturn(t);

        brukerdialogOppgaveRestTjeneste = new BrukerdialogOppgaveRestTjeneste(null, veilederOppgaveTjeneste, null);

    }

    @Test
    void opprettSøkYtelseOppgave() throws JsonProcessingException {

        var body = """
            {"aktørId":"9913438235064","fomDato":[2025,11,1],"oppgaveReferanse":"febb004d-b52d-4d27-9400-1d5390102b76"}""";


        OpprettSøkYtelseOppgaveDto opprettSøkYtelseOppgaveDto = JsonObjectMapper.getMapper().readValue(body, OpprettSøkYtelseOppgaveDto.class);
        brukerdialogOppgaveRestTjeneste.opprettSøkYtelseOppgave(
            opprettSøkYtelseOppgaveDto
        );


    }
}
