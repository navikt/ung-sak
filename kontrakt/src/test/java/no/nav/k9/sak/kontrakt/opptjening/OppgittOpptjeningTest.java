package no.nav.k9.sak.kontrakt.opptjening;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.sak.kontrakt.RestUtils;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittEgenNæringDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittFrilansDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittFrilansoppdragDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.SøknadsperiodeOgOppgittOpptjeningDto;
import no.nav.k9.sak.typer.Beløp;

public class OppgittOpptjeningTest {

    public static final ObjectMapper OM = RestUtils.getObjectMapper();


    @Test
    public void skal_teste_roundtrip() throws JsonProcessingException {
        PeriodeDto periodeDto = new PeriodeDto(LocalDate.now(), LocalDate.now().plusMonths(1));
        Beløp bruttoInntekt = new Beløp(3000);

        OppgittFrilansoppdragDto oppgittFrilansoppdragDto = new OppgittFrilansoppdragDto();
        oppgittFrilansoppdragDto.setPeriode(periodeDto);
        oppgittFrilansoppdragDto.setBruttoInntekt(bruttoInntekt);
        OppgittFrilansDto oppgittFrilansDto = new OppgittFrilansDto();
        oppgittFrilansDto.setOppgittFrilansoppdrag(List.of(oppgittFrilansoppdragDto));

        OppgittEgenNæringDto oppgittEgenNæringDto = new OppgittEgenNæringDto();
        oppgittEgenNæringDto.setPeriode(periodeDto);
        oppgittEgenNæringDto.setBruttoInntekt(bruttoInntekt);

        OppgittOpptjeningDto oppgittOpptjeningDto = new OppgittOpptjeningDto();
        oppgittOpptjeningDto.setOppgittEgenNæring(List.of(oppgittEgenNæringDto));
        oppgittOpptjeningDto.setOppgittFrilans(oppgittFrilansDto);

        SøknadsperiodeOgOppgittOpptjeningDto søknadsperiodeOgOppgittOpptjeningDto = new SøknadsperiodeOgOppgittOpptjeningDto();
        søknadsperiodeOgOppgittOpptjeningDto.setFørSøkerPerioden(oppgittOpptjeningDto);
        søknadsperiodeOgOppgittOpptjeningDto.setISøkerPerioden(oppgittOpptjeningDto);
        søknadsperiodeOgOppgittOpptjeningDto.setSøkerYtelseForFrilans(true);
        søknadsperiodeOgOppgittOpptjeningDto.setSøkerYtelseForNæring(true);
        søknadsperiodeOgOppgittOpptjeningDto.setPeriodeFraSøknad(periodeDto);

        String JSON = OM.writeValueAsString(søknadsperiodeOgOppgittOpptjeningDto);

        SøknadsperiodeOgOppgittOpptjeningDto roundtrip = OM.readValue(JSON, SøknadsperiodeOgOppgittOpptjeningDto.class);

        Assertions.assertThat(roundtrip).isNotNull();
    }
}
