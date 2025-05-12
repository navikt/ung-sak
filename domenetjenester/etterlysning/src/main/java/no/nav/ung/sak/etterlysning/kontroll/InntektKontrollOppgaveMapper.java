package no.nav.ung.sak.etterlysning.kontroll;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektArbeidOgFrilansDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektYtelseDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.YtelseType;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.Virkedager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class InntektKontrollOppgaveMapper {

    static RegisterInntektDTO mapTilRegisterInntekter(InntektArbeidYtelseGrunnlag grunnlag, DatoIntervallEntitet periode) {

        final var arbeidOgFrilansInntekter = finnArbeidOgFrilansInntekter(grunnlag, periode);
        final var ytelseInntekter = finnYtelseInntekter(grunnlag, periode);
        return new RegisterInntektDTO(arbeidOgFrilansInntekter, ytelseInntekter);
    }

    private static List<RegisterInntektYtelseDTO> finnYtelseInntekter(InntektArbeidYtelseGrunnlag grunnlag, DatoIntervallEntitet periode) {
        return grunnlag.getRegisterVersjon().stream().flatMap(it -> it.getAktørInntekt().stream())
            .flatMap(i -> i.getInntekt().stream())
            .flatMap(inntekt -> inntekt.getAlleInntektsposter().stream()
                .filter(ip -> ip.getInntektspostType().equals(InntektspostType.YTELSE))
                .filter(ip -> ip.getPeriode().overlapper(periode))
                .map(inntektspost -> {
                    var beløp = finnBeløpInnenforPeriode(periode, inntektspost);
                    YtelseType ytelseType = maptilYtelseType(inntektspost);
                    return new RegisterInntektYtelseDTO(beløp.intValue(), ytelseType);
                })).toList();
    }


    private static YtelseType maptilYtelseType(Inntektspost ip) {
        switch (ip.getInntektYtelseType().getYtelseType()) {
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
            default -> throw new IllegalStateException("Ikke støttet ytelsetype: " + ip.getInntektYtelseType().getYtelseType());
        }
    }

    private static List<RegisterInntektArbeidOgFrilansDTO> finnArbeidOgFrilansInntekter(InntektArbeidYtelseGrunnlag grunnlag, DatoIntervallEntitet periode) {
        return grunnlag.getRegisterVersjon().stream().flatMap(it -> it.getAktørInntekt().stream())
            .flatMap(i -> i.getInntekt().stream())
            .flatMap(inntekt -> inntekt.getAlleInntektsposter().stream()
                .filter(ip -> ip.getInntektspostType().equals(InntektspostType.LØNN))
                .filter(ip -> ip.getPeriode().overlapper(periode))
                .map(ip -> {
                    var beløp = finnBeløpInnenforPeriode(periode, ip);
                    return new RegisterInntektArbeidOgFrilansDTO(beløp.intValue(), inntekt.getArbeidsgiver().getIdentifikator());
                })).toList();
    }

    private static BigDecimal finnBeløpInnenforPeriode(DatoIntervallEntitet intervall, Inntektspost it) {
        final var inntektsperiode = it.getPeriode();
        final var overlapp = new LocalDateTimeline<>(intervall.toLocalDateInterval(), true).intersection(new LocalDateInterval(inntektsperiode.getFomDato(), inntektsperiode.getTomDato()));
        final var overlappPeriode = overlapp.getLocalDateIntervals().getFirst();

        final var antallVirkedager = inntektsperiode.antallArbeidsdager();
        final var overlappAntallVirkedager = Virkedager.beregnAntallVirkedager(overlappPeriode.getFomDato(), overlappPeriode.getTomDato());

        return it.getBeløp().getVerdi().multiply(BigDecimal.valueOf(overlappAntallVirkedager).divide(BigDecimal.valueOf(antallVirkedager), 10, RoundingMode.HALF_UP));
    }


}
