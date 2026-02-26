package no.nav.ung.sak.etterlysning.kontroll;

import no.nav.ung.kodeverk.arbeidsforhold.OverordnetInntektYtelseType;
import no.nav.ung.sak.kontroll.InntektType;
import no.nav.ung.sak.kontroll.InntekterForKilde;
import no.nav.ung.sak.kontroll.Inntektsperiode;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.ArbeidOgFrilansRegisterInntektDTO;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.RegisterinntektDTO;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseRegisterInntektDTO;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;
import java.util.List;

public class InntektKontrollOppgaveMapper {

    static RegisterinntektDTO mapTilRegisterInntekter(List<InntekterForKilde> registerinntekter) {
        final var arbeidOgFrilansInntekter = finnArbeidOgFrilansInntekter(registerinntekter);
        final var ytelseInntekter = finnYtelseInntekter(registerinntekter);
        return new RegisterinntektDTO(arbeidOgFrilansInntekter, ytelseInntekter);
    }

    private static List<YtelseRegisterInntektDTO> finnYtelseInntekter(List<InntekterForKilde> registerinntekter) {
        return registerinntekter
            .stream()
            .filter(it -> it.inntektType() == InntektType.YTELSE)
            .map(it -> new YtelseRegisterInntektDTO(summerInntekter(it), mapTilYtelseType(it.ytelseType().getOverordnetYtelseType())))
            .toList();
    }

    private static YtelseType mapTilYtelseType(OverordnetInntektYtelseType ytelseType) {
        return switch (ytelseType) {
            case SYKEPENGER -> YtelseType.SYKEPENGER;
            case OMSORGSPENGER -> YtelseType.OMSORGSPENGER;
            case PLEIEPENGER -> YtelseType.PLEIEPENGER;
            case OPPLÆRINGSPENGER -> YtelseType.OPPLÆRINGSPENGER;
            default -> throw new IllegalStateException("Ikke støttet ytelsetype: " + ytelseType);
        };
    }

    private static List<ArbeidOgFrilansRegisterInntektDTO> finnArbeidOgFrilansInntekter(List<InntekterForKilde> registerinntekter) {
        return registerinntekter.stream()
            .filter(it -> it.inntektType() == InntektType.ARBEIDSTAKER_ELLER_FRILANSER)
            .map(it -> new ArbeidOgFrilansRegisterInntektDTO(
                summerInntekter(it),
                it.arbeidsgiver().getIdentifikator(),
                null
            ))
            .toList();
    }

    private static int summerInntekter(InntekterForKilde it) {
        return it.inntekter().stream().map(Inntektsperiode::beløp)
            .reduce(Beløp::adder).map(Beløp::getVerdi).orElse(BigDecimal.ZERO).intValue();
    }

}
