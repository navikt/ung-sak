package no.nav.ung.sak.formidling.innhold;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.InnvilgelseDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.GbeløpPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.ResultatFlaggDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.SatserDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.TilkjentPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.TilkjentYtelseDto;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.VurderAntallDagerResultat;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;

@Dependent
public class InnvilgelseInnholdBygger implements VedtaksbrevInnholdBygger {

    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private TilkjentYtelseUtleder tilkjentYtelseUtleder;
    private PersonopplysningRepository personopplysningRepository;

    @Inject
    public InnvilgelseInnholdBygger(
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
        UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
        TilkjentYtelseUtleder tilkjentYtelseUtleder,
        PersonopplysningRepository personopplysningRepository) {

        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.tilkjentYtelseUtleder = tilkjentYtelseUtleder;
        this.personopplysningRepository = personopplysningRepository;
    }

    public InnvilgelseInnholdBygger() {
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling) {
        Long behandlingId = behandling.getId();
        var tilkjentYtelseTidslinje =
            tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(behandlingId);

        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Mangler grunnlag"));

        var grunnlagOgTilkjentYtelseTidslinje = tilkjentYtelseTidslinje
            .intersection(ungdomsytelseGrunnlag.getSatsTidslinje(), sammenstillGrunnlagOgTilkjentYtelse())
            .compress();

        var tilkjentePerioder = lagTilkjentePerioderDto(grunnlagOgTilkjentYtelseTidslinje);
        var gBeløpPerioder = lagGbeløpPerioderDto(grunnlagOgTilkjentYtelseTidslinje);
        var ytelseFom = grunnlagOgTilkjentYtelseTidslinje.getMinLocalDate();
        var satser = lagSatsDto(grunnlagOgTilkjentYtelseTidslinje);

        var vurderAntallDagerResultat = ungdomsprogramPeriodeTjeneste.finnVirkedagerTidslinje(behandlingId);
        var resultatFlagg = lagResultatFlaggDto(grunnlagOgTilkjentYtelseTidslinje, vurderAntallDagerResultat, behandling);

        long antallDager = vurderAntallDagerResultat.forbrukteDager();
        if (antallDager <= 0) {
            throw new IllegalStateException("Antall virkedager i programmet = %d, kan ikke sende innvilgelsesbrev da".formatted(antallDager));
        }

        return new TemplateInnholdResultat(DokumentMalType.INNVILGELSE_DOK, TemplateType.INNVILGELSE,
            new InnvilgelseDto(
                resultatFlagg,
                ytelseFom,
                antallDager,
                tilkjentePerioder,
                gBeløpPerioder,
                satser));
    }

    private ResultatFlaggDto lagResultatFlaggDto(
        LocalDateTimeline<GrunnlagOgTilkjentYtelse> grunnlagOgTilkjentYtelseTimeline,
        VurderAntallDagerResultat vurderAntallDagerResultat,
        Behandling behandling) {

        var grunnlagOgTilkjentYtelse = grunnlagOgTilkjentYtelseTimeline.stream()
            .map(LocalDateSegment::getValue)
            .distinct()
            .toList();


        boolean enDagsats = grunnlagOgTilkjentYtelse.stream().collect(Collectors.groupingBy(GrunnlagOgTilkjentYtelse::dagsats)).size() == 1;
        boolean ettGbeløp = grunnlagOgTilkjentYtelse.stream().collect(Collectors.groupingBy(GrunnlagOgTilkjentYtelse::grunnbeløp)).size() == 1;

        var satsTyper = grunnlagOgTilkjentYtelse.stream().map(GrunnlagOgTilkjentYtelse::satsType).collect(Collectors.toSet());
        boolean lavSats = satsTyper.stream().allMatch(it -> it == UngdomsytelseSatsType.LAV);
        boolean høySats = satsTyper.stream().allMatch(it -> it == UngdomsytelseSatsType.HØY);
        boolean varierendeSats = satsTyper.contains(UngdomsytelseSatsType.LAV) && satsTyper.contains(UngdomsytelseSatsType.HØY);

        LocalDate fødselsdato = personopplysningRepository.hentPersonopplysninger(behandling.getId())
            .getGjeldendeVersjon()
            .getPersonopplysning(behandling.getAktørId())
            .getFødselsdato();
        boolean oppnårMaksAlder = vurderAntallDagerResultat.tidslinjeNokDager()
            .getMaxLocalDate()
            .isAfter(fødselsdato.plusYears(Sats.HØY.getTomAlder()));

        return new ResultatFlaggDto(
            enDagsats,
            ettGbeløp,
            lavSats,
            høySats,
            varierendeSats,
            oppnårMaksAlder);
    }

    private static Set<GbeløpPeriodeDto> lagGbeløpPerioderDto(LocalDateTimeline<GrunnlagOgTilkjentYtelse> grunnlagOgTilkjentYtelseTimeline) {
        return grunnlagOgTilkjentYtelseTimeline
            .mapSegment(GrunnlagOgTilkjentYtelse::grunnbeløp)
            .compress().stream()
            .map(it -> new GbeløpPeriodeDto(
                new PeriodeDto(it.getFom(), it.getTom()), it.getValue()))
            .collect(Collectors.toSet());
    }

    private static SatserDto lagSatsDto(LocalDateTimeline<GrunnlagOgTilkjentYtelse> grunnlagOgTilkjentYtelseTimeline) {
        List<GrunnlagOgTilkjentYtelse> sortertGrunnlagOgTilkjentYtelseVerdier = grunnlagOgTilkjentYtelseTimeline.stream()
            .sorted(Comparator.comparing(LocalDateSegment::getLocalDateInterval, Comparator.reverseOrder())) // nyeste først
            .map(LocalDateSegment::getValue)
            .distinct()
            .toList();

        var nyesteLavSats = sortertGrunnlagOgTilkjentYtelseVerdier.stream()
            .filter(it -> it.satsType() == UngdomsytelseSatsType.LAV)
            .findFirst()
            .map(GrunnlagOgTilkjentYtelse::grunnbeløpFaktor);

        var nyesteHøySats = sortertGrunnlagOgTilkjentYtelseVerdier.stream()
            .filter(it -> it.satsType() == UngdomsytelseSatsType.HØY)
            .findFirst()
            .map(GrunnlagOgTilkjentYtelse::grunnbeløpFaktor);

        return new SatserDto(nyesteHøySats.orElse(null), nyesteLavSats.orElse(null), Sats.LAV.getTomAlder(), Sats.HØY.getTomAlder());
    }

    private static List<TilkjentPeriodeDto> lagTilkjentePerioderDto(LocalDateTimeline<GrunnlagOgTilkjentYtelse> grunnlagOgTilkjentYtelseTimeline) {
        return grunnlagOgTilkjentYtelseTimeline
            .mapSegment(it ->
                new TilkjentYtelseDto(it.dagsats(), it.grunnbeløpFaktor(), it.grunnbeløp(), it.årsbeløp()))
            .compress().stream()
            .sorted(Comparator.comparing(LocalDateSegment::getLocalDateInterval)) //eldste først for tilkjent perioder
            .map(it ->
                new TilkjentPeriodeDto(new PeriodeDto(it.getFom(), it.getTom()), it.getValue()))
            .toList();
    }

    private static LocalDateSegmentCombinator<DagsatsOgUtbetalingsgrad, UngdomsytelseSatser, GrunnlagOgTilkjentYtelse> sammenstillGrunnlagOgTilkjentYtelse() {
        return (di, lhs, rhs) -> {
            var dg = lhs.getValue();
            var sp = rhs.getValue();
            return new LocalDateSegment<>(di,
                new GrunnlagOgTilkjentYtelse(
                    dg.dagsats(),
                    avrundTilHeltall(dg.utbetalingsgrad()),
                    sp.satsType(),
                    sp.grunnbeløpFaktor().setScale(2, RoundingMode.HALF_UP),
                    avrundTilHeltall(sp.grunnbeløp()).longValue(),
                    avrundTilHeltall(sp.grunnbeløp().multiply(sp.grunnbeløpFaktor())).longValue(),
                    sp.antallBarn(),
                    sp.dagsatsBarnetillegg()
                ));

        };
    }

    private static BigDecimal avrundTilHeltall(BigDecimal decimal) {
        return decimal.setScale(0, RoundingMode.HALF_UP);
    }

}
