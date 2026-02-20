package no.nav.ung.sak.oppgave.typer.varsel.typer.kontrollerregisterinntekt;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektArbeidOgFrilansDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektYtelseDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.YtelseType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.ArbeidOgFrilansRegisterInntektDTO;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgavetypeDataDTO;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.RegisterinntektDTO;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseRegisterInntektDTO;

import java.util.List;
import java.util.stream.Collectors;

public class KontrollerRegisterInntektOppgaveMapper {

    public static BrukerdialogOppgaveEntitet map(RegisterInntektOppgaveDTO oppgaveDto, AktørId aktørId) {
        // Map registerinntekter
        var arbeidOgFrilansInntekter = mapArbeidOgFrilans(oppgaveDto.getRegisterInntekter().getRegisterinntekterForArbeidOgFrilans());
        var ytelseInntekter = mapYtelse(oppgaveDto.getRegisterInntekter().getRegisterinntekterForYtelse());

        RegisterinntektDTO registerinntektData = new RegisterinntektDTO(arbeidOgFrilansInntekter, ytelseInntekter);

        // Opprett oppgavedata
        OppgavetypeDataDTO kontrollerRegisterInntektOppgaveData = new KontrollerRegisterinntektOppgavetypeDataDTO(
            oppgaveDto.getFomDato(),
            oppgaveDto.getTomDato(),
            registerinntektData,
            oppgaveDto.getGjelderDelerAvMåned()
        );

        return new BrukerdialogOppgaveEntitet(
            oppgaveDto.getReferanse(),
            OppgaveType.BEKREFT_AVVIK_REGISTERINNTEKT,
            aktørId,
            kontrollerRegisterInntektOppgaveData,
            oppgaveDto.getFrist()
        );
    }

    private static List<YtelseRegisterInntektDTO> mapYtelse(List<RegisterInntektYtelseDTO> registerinntekterForYtelse) {
       if (registerinntekterForYtelse == null) {
           return null;
       }
        return registerinntekterForYtelse
            .stream()
            .map(dto -> new YtelseRegisterInntektDTO(dto.getBeløp(), mapYtelseType(dto.getYtelseType())))
            .collect(Collectors.toList());
    }

    private static List<ArbeidOgFrilansRegisterInntektDTO> mapArbeidOgFrilans(List<RegisterInntektArbeidOgFrilansDTO> registerinntekterForArbeidOgFrilans) {
        if (registerinntekterForArbeidOgFrilans == null) {
            return null;
        }
        return registerinntekterForArbeidOgFrilans
            .stream()
            .map(dto -> new ArbeidOgFrilansRegisterInntektDTO(dto.getBeløp(), dto.getArbeidsgiverIdent(), null))
            .collect(Collectors.toList());
    }

    private static no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType mapYtelseType(YtelseType ytelseType) {
        return switch (ytelseType) {
            case SYKEPENGER -> no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType.SYKEPENGER;
            case OMSORGSPENGER -> no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType.OMSORGSPENGER;
            case PLEIEPENGER -> no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType.PLEIEPENGER;
            case OPPLAERINGSPENGER -> no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType.OPPLÆRINGSPENGER;
            default -> no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType.ANNET;
        };
    }
}

