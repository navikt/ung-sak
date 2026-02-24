package no.nav.ung.sak.oppgave.typer.varsel.typer.kontrollerregisterinntekt;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.ArbeidOgFrilansRegisterInntektDTO;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.RegisterinntektDTO;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseRegisterInntektDTO;
import no.nav.ung.sak.oppgave.OppgaveDataMapperFraEntitetTilDto;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.util.List;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_AVVIK_REGISTERINNTEKT)
public class KontrollerRegisterinntektOppgaveDataTilDtoMapperFraEntitetTilDto implements OppgaveDataMapperFraEntitetTilDto {

    protected KontrollerRegisterinntektOppgaveDataTilDtoMapperFraEntitetTilDto() {
        // CDI proxy
    }

    @Override
    public OppgavetypeDataDto tilDto(OppgaveDataEntitet entitet) {
        var e = (KontrollerRegisterinntektOppgaveDataEntitet) entitet;

        List<ArbeidOgFrilansRegisterInntektDTO> arbeidOgFrilans = e.getArbeidOgFrilansInntekter().stream()
            .map(i -> new ArbeidOgFrilansRegisterInntektDTO(i.getInntekt(), i.getArbeidsgiver(), null))
            .toList();

        List<YtelseRegisterInntektDTO> ytelse = e.getYtelseInntekter().stream()
            .map(i -> new YtelseRegisterInntektDTO(i.getInntekt(), i.getYtelsetype()))
            .toList();

        var registerinntekt = new RegisterinntektDTO(
            arbeidOgFrilans,
            ytelse,
            e.getTotalInntektArbeidFrilans(),
            e.getTotalInntektYtelse(),
            e.getTotalInntekt()
        );

        return new KontrollerRegisterinntektOppgavetypeDataDto(
            e.getFraOgMed(),
            e.getTilOgMed(),
            registerinntekt,
            e.isGjelderDelerAvMåned()
        );
    }
}

