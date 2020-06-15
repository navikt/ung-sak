package no.nav.k9.sak.ytelse.frisinn.mapper;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedSøkerInfoDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;
import no.nav.k9.sak.kontrakt.frisinn.PeriodeMedSNOgFLDto;

public class FrisinnMapper {

    public static final LocalDate FØRSTE_DAG_I_APRIL = LocalDate.of(2020, 4, 1);
    public static final LocalDate SISTE_DAG_I_APRIL = LocalDate.of(2020, 4, 30);
    public static final LocalDate SISTE_DAG_I_MARS = LocalDate.of(2020, 3, 30);

    public static List<PeriodeMedSøkerInfoDto> mapPeriodeMedSøkerInfoDto(UttakAktivitet fastsattUttak) {
        List<LocalDateSegment<PeriodeMedSøkerInfoDto>> frilans = lagSegmenter(fastsattUttak, UttakArbeidType.FRILANSER);
        List<LocalDateSegment<PeriodeMedSøkerInfoDto>> næringsdrivende = lagSegmenter(fastsattUttak, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);

        LocalDateTimeline<PeriodeMedSøkerInfoDto> frilansTidsserie = new LocalDateTimeline<>(frilans);
        LocalDateTimeline<PeriodeMedSøkerInfoDto> næringsdrivendeTidsserie = new LocalDateTimeline<>(næringsdrivende);
        LocalDateTimeline<PeriodeMedSøkerInfoDto> kombinert = frilansTidsserie.combine(næringsdrivendeTidsserie, FrisinnMapper::combine, JoinStyle.CROSS_JOIN);

        return kombinert.getDatoIntervaller().stream().map(intervall -> {
            LocalDateSegment<PeriodeMedSøkerInfoDto> segment = kombinert.getSegment(intervall);
            return segment.getValue();
        }).collect(Collectors.toList());
    }

    public static List<PeriodeDto> finnMåneder(UttakAktivitet fastsattUttak) {
        return fastsattUttak.getPerioder()
            .stream()
            .map(uttakAktivitetPeriode -> finnMåned(uttakAktivitetPeriode.getPeriode()))
            .distinct()
            .sorted((Comparator.comparing(PeriodeDto::getTom)))
            .collect(Collectors.toList());
    }

    private static PeriodeDto finnMåned(DatoIntervallEntitet periode) {
        LocalDate tomDato = periode.getTomDato();
        //spesial behandling der april søknader starter med fom i mars

        if (tomDato.isEqual(SISTE_DAG_I_APRIL)) {
            return new PeriodeDto(SISTE_DAG_I_MARS, tomDato);
        }
        LocalDate føsteDag = MonthDay.of(tomDato.getMonth(), 1).atYear(LocalDate.now().getYear());
        return new PeriodeDto(føsteDag, tomDato);
    }

    private static List<LocalDateSegment<PeriodeMedSøkerInfoDto>> lagSegmenter(UttakAktivitet fastsattUttak, UttakArbeidType aktivitetType) {
        return fastsattUttak.getPerioder()
            .stream()
            .filter(p -> p.getAktivitetType() == aktivitetType)
            .map(uttakAktivitetPeriode -> {
                PeriodeMedSøkerInfoDto dto = new PeriodeMedSøkerInfoDto(new Periode(uttakAktivitetPeriode.getPeriode().getFomDato(), uttakAktivitetPeriode.getPeriode().getTomDato()), aktivitetType == UttakArbeidType.FRILANSER, aktivitetType == UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);

                return new LocalDateSegment<>(uttakAktivitetPeriode.getPeriode().getFomDato(), uttakAktivitetPeriode.getPeriode().getTomDato(), dto);
            }).collect(Collectors.toList());
    }

    private static LocalDateSegment<PeriodeMedSøkerInfoDto> combine(LocalDateInterval interval,
                                                                    LocalDateSegment<PeriodeMedSøkerInfoDto> fl,
                                                                    LocalDateSegment<PeriodeMedSøkerInfoDto> sn) {
        if (fl != null && sn != null) {
            return new LocalDateSegment<>(interval, new PeriodeMedSøkerInfoDto(new Periode(interval.getFomDato(), interval.getTomDato()), true, true));
        } else if (fl != null) {
            return new LocalDateSegment<>(interval, new PeriodeMedSøkerInfoDto(new Periode(interval.getFomDato(), interval.getTomDato()), true, false));
        }
        return new LocalDateSegment<>(interval, new PeriodeMedSøkerInfoDto(new Periode(interval.getFomDato(), interval.getTomDato()), false, true));
    }

    public static PeriodeMedSNOgFLDto map(PeriodeDto måned, OppgittOpptjeningDto dto, UttakAktivitet fastsattUttak) {
        PeriodeMedSNOgFLDto periodeMedSNOgFLDto = new PeriodeMedSNOgFLDto();
        periodeMedSNOgFLDto.setMåned(måned);
        periodeMedSNOgFLDto.setOppgittIMåned(dto);
        periodeMedSNOgFLDto.setSøkerFL(fastsattUttak.getPerioder().stream().anyMatch(p -> måned.getTom().equals(p.getPeriode().getTomDato()) && p.getAktivitetType() == UttakArbeidType.FRILANSER));
        periodeMedSNOgFLDto.setSøkerSN(fastsattUttak.getPerioder().stream().anyMatch(p -> måned.getTom().equals(p.getPeriode().getTomDato()) && p.getAktivitetType() == UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE));

        return periodeMedSNOgFLDto;
    }
}
