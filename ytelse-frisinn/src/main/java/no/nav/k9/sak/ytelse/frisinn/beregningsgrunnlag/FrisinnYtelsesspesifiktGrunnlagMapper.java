package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.beregning.v1.FrisinnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedSøkerInfoDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnYtelsesspesifiktGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<FrisinnGrunnlag> {

    private UttakRepository uttakRepository;

    FrisinnYtelsesspesifiktGrunnlagMapper() {
    }

    @Inject
    public FrisinnYtelsesspesifiktGrunnlagMapper(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public FrisinnGrunnlag lagYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var søknadsperiode = uttakRepository.hentOppgittSøknadsperioder(ref.getBehandlingId()).getMaksPeriode();
        var fastsattUttak = uttakRepository.hentFastsattUttak(ref.getBehandlingId());

        //TODO(OJR) fjern dette når man har fikset kalkulus
        boolean søkerYtelseForFrilans = fastsattUttak.getPerioder().stream()
                .anyMatch(p -> p.getPeriode().overlapper(søknadsperiode) && p.getAktivitetType() == UttakArbeidType.FRILANSER);

        boolean søkerYtelseForNæring = fastsattUttak.getPerioder().stream()
                .anyMatch(p -> p.getPeriode().overlapper(søknadsperiode) && p.getAktivitetType() == UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);
        //TODO(OJR) fjern mellom todoene

        List<PeriodeMedSøkerInfoDto> periodeMedSøkerInfoDtos = mapPeriodeMedSøkerInfoDto(fastsattUttak);

        FrisinnGrunnlag frisinnGrunnlag = new FrisinnGrunnlag(søkerYtelseForFrilans, søkerYtelseForNæring);
        frisinnGrunnlag.medPerioderMedSøkerInfo(periodeMedSøkerInfoDtos);

        return frisinnGrunnlag;
    }

    List<PeriodeMedSøkerInfoDto> mapPeriodeMedSøkerInfoDto(UttakAktivitet fastsattUttak) {
        List<LocalDateSegment<PeriodeMedSøkerInfoDto>> frilans = lagSegmenter(fastsattUttak, UttakArbeidType.FRILANSER);
        List<LocalDateSegment<PeriodeMedSøkerInfoDto>> næringsdrivende = lagSegmenter(fastsattUttak, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);

        LocalDateTimeline<PeriodeMedSøkerInfoDto> frilansTidsserie = new LocalDateTimeline<>(frilans);
        LocalDateTimeline<PeriodeMedSøkerInfoDto> næringsdrivendeTidsserie = new LocalDateTimeline<>(næringsdrivende);
        LocalDateTimeline<PeriodeMedSøkerInfoDto> kombinert = frilansTidsserie.combine(næringsdrivendeTidsserie, FrisinnYtelsesspesifiktGrunnlagMapper::combine, JoinStyle.CROSS_JOIN);

        return kombinert.getDatoIntervaller().stream().map(intervall -> {
            LocalDateSegment<PeriodeMedSøkerInfoDto> segment = kombinert.getSegment(intervall);
            return segment.getValue();
        }).collect(Collectors.toList());
    }

    private List<LocalDateSegment<PeriodeMedSøkerInfoDto>> lagSegmenter(UttakAktivitet fastsattUttak, UttakArbeidType aktivitetType) {
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
}
