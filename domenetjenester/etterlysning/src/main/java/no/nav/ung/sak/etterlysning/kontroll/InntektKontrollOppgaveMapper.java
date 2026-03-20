package no.nav.ung.sak.etterlysning.kontroll;

import no.nav.ung.kodeverk.arbeidsforhold.OverordnetInntektYtelseType;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.ung.sak.kontroll.InntektType;
import no.nav.ung.sak.kontroll.InntekterForKilde;
import no.nav.ung.sak.kontroll.Inntektsperiode;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.kontrollerregisterinntekt.ArbeidOgFrilansRegisterInntektDTO;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.kontrollerregisterinntekt.RegisterinntektDTO;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseRegisterInntektDTO;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType;
import no.nav.ung.sak.typer.Beløp;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class InntektKontrollOppgaveMapper {

    static RegisterinntektDTO mapTilRegisterInntekter(List<InntekterForKilde> registerinntekter, List<ArbeidsgiverOpplysninger> arbeidsgiverOpplysninger) {
        final var arbeidOgFrilansInntekter = finnArbeidOgFrilansInntekter(registerinntekter, arbeidsgiverOpplysninger);
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

    private static List<ArbeidOgFrilansRegisterInntektDTO> finnArbeidOgFrilansInntekter(List<InntekterForKilde> registerinntekter, List<ArbeidsgiverOpplysninger> arbeidsgiverOpplysninger) {
        return registerinntekter.stream()
            .filter(it -> it.inntektType() == InntektType.ARBEIDSTAKER_ELLER_FRILANSER)
            .map(it -> new ArbeidOgFrilansRegisterInntektDTO(
                summerInntekter(it),
                it.arbeidsgiver().getIdentifikator(),
                it.arbeidsgiver().getIdentifikator(),
                finnArbeidsgivernavn(arbeidsgiverOpplysninger, it).orElse(null)
            ))
            .toList();
    }

    private static Optional<String> finnArbeidsgivernavn(List<ArbeidsgiverOpplysninger> arbeidsgiverOpplysninger, InntekterForKilde it) {
        return arbeidsgiverOpplysninger.stream()
            .filter(
                arbOppl -> arbOppl.getIdentifikator().equals(it.arbeidsgiver().getIdentifikator())
            ).findFirst().map(ArbeidsgiverOpplysninger::getNavn);
    }

    private static int summerInntekter(InntekterForKilde it) {
        return it.inntekter().stream().map(Inntektsperiode::beløp)
            .reduce(Beløp::adder).map(Beløp::getVerdi).orElse(BigDecimal.ZERO).intValue();
    }

}
