package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittEgenNæringDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittFrilansDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittFrilansoppdragDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;
import no.nav.k9.sak.kontrakt.frisinn.PeriodeMedSNOgFLDto;
import no.nav.k9.sak.kontrakt.frisinn.SøknadsperiodeOgOppgittOpptjeningV2Dto;
import no.nav.k9.sak.typer.Beløp;

public class OverstyringOppgittOpptjeningOppdatererTest {


    @Test
    public void skal_teste_at_riktig_dato_blir_satt_hvis_den_har_blitt_fremskynet_av_saksbehanlder_FL() {
        OverstyringOppgittOpptjeningOppdaterer oppdaterer = new OverstyringOppgittOpptjeningOppdaterer();
        LocalDate start = LocalDate.of(2020, 4, 1);
        LocalDate startSN = LocalDate.of(2020, 4, 12);
        LocalDate startFL = LocalDate.of(2020, 4, 10);
        LocalDate slutt = LocalDate.of(2020, 4, 30);

        PeriodeDto søkerPeriode = new PeriodeDto(start, slutt);
        PeriodeDto snPeriode = new PeriodeDto(startSN, slutt);
        PeriodeDto flPeriode = new PeriodeDto(startFL, slutt);

        SøknadsperiodeOgOppgittOpptjeningV2Dto søknadsperiodeOgOppgittOpptjeningDto = lagDto(true, true, snPeriode, flPeriode, søkerPeriode);

        LocalDate dato = oppdaterer.finnFraOgMedDatoFL(søknadsperiodeOgOppgittOpptjeningDto.getMåneder().get(0).getOppgittIMåned()).get();

        Assertions.assertThat(dato).isEqualTo(startFL);
    }

    @Test
    public void skal_teste_at_riktig_dato_blir_satt_hvis_den_har_blitt_fremskynet_av_saksbehanlder_SN() {
        OverstyringOppgittOpptjeningOppdaterer oppdaterer = new OverstyringOppgittOpptjeningOppdaterer();
        LocalDate start = LocalDate.of(2020, 4, 1);
        LocalDate startSN = LocalDate.of(2020, 4, 9);
        LocalDate startFL = LocalDate.of(2020, 4, 10);
        LocalDate slutt = LocalDate.of(2020, 4, 30);

        PeriodeDto søkerPeriode = new PeriodeDto(start, slutt);
        PeriodeDto snPeriode = new PeriodeDto(startSN, slutt);
        PeriodeDto flPeriode = new PeriodeDto(startFL, slutt);

        SøknadsperiodeOgOppgittOpptjeningV2Dto søknadsperiodeOgOppgittOpptjeningDto = lagDto(true, true, snPeriode, flPeriode, søkerPeriode);

        LocalDate dato = oppdaterer.finnFraOgMedDatoSN(søknadsperiodeOgOppgittOpptjeningDto.getMåneder().get(0).getOppgittIMåned()).get();

        Assertions.assertThat(dato).isEqualTo(startSN);
    }

    private SøknadsperiodeOgOppgittOpptjeningV2Dto lagDto(Boolean søkerFrilans, Boolean søkerSN, PeriodeDto periodeSN, PeriodeDto periodeFL, PeriodeDto periodeFraSøknad) {
        SøknadsperiodeOgOppgittOpptjeningV2Dto dto = new SøknadsperiodeOgOppgittOpptjeningV2Dto();


        PeriodeMedSNOgFLDto periodeMedSNOgFLDto = new PeriodeMedSNOgFLDto();

        periodeMedSNOgFLDto.setMåned(periodeFraSøknad);
        periodeMedSNOgFLDto.setSøkerFL(søkerFrilans);
        periodeMedSNOgFLDto.setSøkerSN(søkerSN);
        OppgittOpptjeningDto oppgittOpptjeningDtoI = new OppgittOpptjeningDto();
        OppgittOpptjeningDto oppgittOpptjeningDtoFør = new OppgittOpptjeningDto();

        OppgittFrilansDto oppgittFrilansDto = new OppgittFrilansDto();
        OppgittFrilansoppdragDto oppgittFrilansoppdragDto = new OppgittFrilansoppdragDto();
        oppgittFrilansoppdragDto.setPeriode(periodeFL);
        oppgittFrilansoppdragDto.setBruttoInntekt(new Beløp(BigDecimal.TEN));
        oppgittFrilansDto.setOppgittFrilansoppdrag(List.of(oppgittFrilansoppdragDto));
        oppgittOpptjeningDtoI.setOppgittFrilans(oppgittFrilansDto);

        OppgittEgenNæringDto egenNæringDto = new OppgittEgenNæringDto();
        egenNæringDto.setPeriode(periodeSN);
        egenNæringDto.setBruttoInntekt(new Beløp(BigDecimal.TEN));
        oppgittOpptjeningDtoI.setOppgittEgenNæring(List.of(egenNæringDto));

        periodeMedSNOgFLDto.setOppgittIMåned(oppgittOpptjeningDtoI);
        dto.setMåneder(List.of(periodeMedSNOgFLDto));
        dto.setFørSøkerPerioden(oppgittOpptjeningDtoFør);

        return dto;
    }
}
