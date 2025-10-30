package no.nav.ung.sak.etterlysning.kontroll;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektArbeidOgFrilansDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektYtelseDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.YtelseType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.kontroll.InntektType;
import no.nav.ung.sak.kontroll.InntekterForKilde;
import no.nav.ung.sak.kontroll.Inntektsperiode;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;
import java.util.List;

public class InntektKontrollOppgaveMapper {

    static RegisterInntektDTO mapTilRegisterInntekter(List<InntekterForKilde> registerinntekter) {

        final var arbeidOgFrilansInntekter = finnArbeidOgFrilansInntekter(registerinntekter);
        final var ytelseInntekter = finnYtelseInntekter(registerinntekter);
        return new RegisterInntektDTO(arbeidOgFrilansInntekter, ytelseInntekter);
    }

    private static List<RegisterInntektYtelseDTO> finnYtelseInntekter(List<InntekterForKilde> registerinntekter) {
        return registerinntekter
            .stream()
            .filter(it -> it.inntektType() == InntektType.YTELSE)
            .map(it -> new RegisterInntektYtelseDTO(summerInntekter(it), maptilYtelseType(it.ytelseType().getYtelseType()))).toList();
    }


    private static YtelseType maptilYtelseType(FagsakYtelseType ytelseType) {
        switch (ytelseType) {
            case SYKEPENGER -> {
                return YtelseType.SYKEPENGER;
            }
            case OMSORGSPENGER, OMSORGSPENGER_AO, OMSORGSPENGER_KS, OMSORGSPENGER_MA -> {
                return YtelseType.OMSORGSPENGER;
            }
            case PLEIEPENGER_SYKT_BARN -> {
                return YtelseType.PLEIEPENGER_SYKT_BARN;
            }
            case PLEIEPENGER_NÆRSTÅENDE -> {
                return YtelseType.PLEIEPENGER_LIVETS_SLUTTFASE;
            }
            case OPPLÆRINGSPENGER -> {
                return YtelseType.OPPLAERINGSPENGER;
            }
            default -> throw new IllegalStateException("Ikke støttet ytelsetype: " + ytelseType);
        }
    }

    private static List<RegisterInntektArbeidOgFrilansDTO> finnArbeidOgFrilansInntekter(List<InntekterForKilde> registerinntekter) {
        return registerinntekter.stream()
            .filter(it -> it.inntektType() == InntektType.ARBEIDSTAKER_ELLER_FRILANSER)
            .map(it -> new RegisterInntektArbeidOgFrilansDTO(
                summerInntekter(it), // Inntektene her er allerede filtrert på periode
                it.arbeidsgiver().getIdentifikator()
            )).toList();
    }

    private static int summerInntekter(InntekterForKilde it) {
        return it.inntekter().stream().map(Inntektsperiode::beløp)
            .reduce(Beløp::adder).map(Beløp::getVerdi).orElse(BigDecimal.ZERO).intValue();
    }

}
