package no.nav.ung.sak.oppgave.typer.kontrollerregisterinntekt;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektArbeidOgFrilansDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektYtelseDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.YtelseType;
import no.nav.ung.kodeverk.arbeidsforhold.OverordnetInntektYtelseType;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveData;
import no.nav.ung.sak.oppgave.OppgaveType;

import java.util.List;
import java.util.stream.Collectors;

public class KontrollerRegisterInntektOppgaveMapper {

    public static BrukerdialogOppgaveEntitet map(RegisterInntektOppgaveDTO oppgaveDto, AktørId aktørId) {
        // Map registerinntekter
        var arbeidOgFrilansInntekter = mapArbeidOgFrilans(oppgaveDto.getRegisterInntekter().getRegisterinntekterForArbeidOgFrilans());
        var ytelseInntekter = mapYtelse(oppgaveDto.getRegisterInntekter().getRegisterinntekterForYtelse());

        RegisterinntektData registerinntektData = new RegisterinntektData(arbeidOgFrilansInntekter, ytelseInntekter);

        // Opprett oppgavedata
        OppgaveData kontrollerRegisterInntektOppgaveData = new KontrollerRegisterInntektOppgaveData(
            registerinntektData,
            oppgaveDto.getFomDato(),
            oppgaveDto.getTomDato(),
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

    private static List<YtelseRegisterInntektData> mapYtelse(List<RegisterInntektYtelseDTO> registerinntekterForYtelse) {
       if (registerinntekterForYtelse == null) {
           return null;
       }
        return registerinntekterForYtelse
            .stream()
            .map(dto -> new YtelseRegisterInntektData(dto.getBeløp(), mapYtelseType(dto.getYtelseType())))
            .collect(Collectors.toList());
    }

    private static List<ArbeidOgFrilansRegisterInntektData> mapArbeidOgFrilans(List<RegisterInntektArbeidOgFrilansDTO> registerinntekterForArbeidOgFrilans) {
        if (registerinntekterForArbeidOgFrilans == null) {
            return null;
        }
        return registerinntekterForArbeidOgFrilans
            .stream()
            .map(dto -> new ArbeidOgFrilansRegisterInntektData(dto.getBeløp(), dto.getArbeidsgiverIdent()))
            .collect(Collectors.toList());
    }

    private static OverordnetInntektYtelseType mapYtelseType(YtelseType ytelseType) {
        switch (ytelseType) {
            case SYKEPENGER -> {
                return OverordnetInntektYtelseType.SYKEPENGER;
            }
            case OMSORGSPENGER -> {
                return OverordnetInntektYtelseType.OMSORGSPENGER;
            }
            case PLEIEPENGER -> {
                return OverordnetInntektYtelseType.PLEIEPENGER;
            }
            case OPPLAERINGSPENGER -> {
                return OverordnetInntektYtelseType.OPPLÆRINGSPENGER;
            }
            default -> throw new IllegalStateException("Ikke støttet ytelsetype: " + ytelseType);
        }
    }
}

