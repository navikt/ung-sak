package no.nav.ung.sak.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.formidling.template.dto.InnvilgelseDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.*;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.FinnForbrukteDager;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.VurderAntallDagerResultat;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger.tilFaktor;
import static no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger.tilHeltall;

@Dependent
public class InnvilgelseInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(InnvilgelseInnholdBygger.class);

    private final UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private final PersonopplysningRepository personopplysningRepository;
    private final TilkjentYtelseRepository tilkjentYtelseRepository;
    private final boolean ignoreIkkeStøttedeBrev;

    @Inject
    public InnvilgelseInnholdBygger(
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
        UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
        PersonopplysningRepository personopplysningRepository,
        TilkjentYtelseRepository tilkjentYtelseRepository,
        @KonfigVerdi(value = "IGNORE_FLERE_SATSPERIODER_BREV", defaultVerdi = "false") boolean ignoreFlereSatsperioder) {

        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.personopplysningRepository = personopplysningRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.ignoreIkkeStøttedeBrev = ignoreFlereSatsperioder;
    }


    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        Long behandlingId = behandling.getId();
        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandlingId);

        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Mangler grunnlag"));

        var grunnlagOgTilkjentYtelseTidslinje = tilkjentYtelseTidslinje
            .intersection(ungdomsytelseGrunnlag.getSatsTidslinje(),
                InnvilgelseInnholdBygger::sammenstillGrunnlagOgTilkjentYtelse)
            .compress();

        var tilkjentePerioder = lagTilkjentePerioderDto(grunnlagOgTilkjentYtelseTidslinje);

        var tilkjentSatserHøy = mapTilTilkjentSatsDto(grunnlagOgTilkjentYtelseTidslinje, UngdomsytelseSatsType.HØY);
        var tilkjentSatserLav = mapTilTilkjentSatsDto(grunnlagOgTilkjentYtelseTidslinje, UngdomsytelseSatsType.LAV);
        var ikkeStøttetBrevTekst = validerTilkjentYtelse(tilkjentSatserHøy, tilkjentSatserLav);

        var tilkjentSatsHøy = tilkjentSatserHøy.stream().findFirst();
        var tilkjentSatsLav = tilkjentSatserLav.stream().findFirst();


        var gBeløpPerioder = lagGbeløpPerioderDto(grunnlagOgTilkjentYtelseTidslinje);
        var ytelseFom = detaljertResultatTidslinje.getMinLocalDate();
        var satser = lagSatsDto(grunnlagOgTilkjentYtelseTidslinje);

        var vurderAntallDagerResultat = ungdomsprogramPeriodeTjeneste.finnVirkedagerTidslinje(behandlingId);
        var resultatFlagg = lagResultatFlaggDto(grunnlagOgTilkjentYtelseTidslinje, vurderAntallDagerResultat, behandling);

        long antallDager = vurderAntallDagerResultat.forbrukteDager();
        if (antallDager <= 0) {
            throw new IllegalStateException("Antall virkedager i programmet = %d, kan ikke sende innvilgelsesbrev da".formatted(antallDager));
        }
        var ytelseTom = FinnForbrukteDager.MAKS_ANTALL_DAGER != antallDager ? detaljertResultatTidslinje.getMaxLocalDate() : null;

        return new TemplateInnholdResultat(DokumentMalType.INNVILGELSE_DOK, TemplateType.INNVILGELSE,
            new InnvilgelseDto(
                resultatFlagg,
                ytelseFom,
                ytelseTom,
                tilkjentePerioder,
                gBeløpPerioder,
                satser,
                tilkjentSatsLav.orElseGet(tilkjentSatsHøy::orElseThrow),
                tilkjentSatsHøy.orElse(null),
                ikkeStøttetBrevTekst));
    }

    private String validerTilkjentYtelse(List<TilkjentPeriodeDto> tilkjenteYtelserHøy, List<TilkjentPeriodeDto> tilkjenteYtelserLav) {
        if (tilkjenteYtelserHøy.size() > 1) {
            String feiltekst = "Kan ikke ha mer enn 1 periode med høy sats. Fant %d".formatted(tilkjenteYtelserHøy.size());
            if (ignoreIkkeStøttedeBrev){
                var logg = "Dette brevet vil ikke bli laget i prod: " + feiltekst;
                LOG.warn(logg);
                return logg;
            } else {
                throw new IllegalStateException(feiltekst);

            }
        }

        if (tilkjenteYtelserLav.size() > 1 ) {
            String feiltekst = "Kan ikke ha mer enn 1 periode med lav sats. Fant %d".formatted(tilkjenteYtelserLav.size());
            if (ignoreIkkeStøttedeBrev){
                String logg = "Dette brevet vil ikke bli laget i prod: " + feiltekst;
                LOG.warn(logg);
                return logg;
            } else {
                throw new IllegalStateException(feiltekst);

            }
        }

        if (tilkjenteYtelserLav.isEmpty() && tilkjenteYtelserHøy.isEmpty()) {
            throw new IllegalStateException("Fant ingen tilkjente perioder");
        }

        return null;
    }

    private static List<TilkjentPeriodeDto> mapTilTilkjentSatsDto(LocalDateTimeline<GrunnlagOgTilkjentYtelse> grunnlagOgTilkjentYtelseTidslinje, UngdomsytelseSatsType satsType) {
        return grunnlagOgTilkjentYtelseTidslinje
            .filterValue(it -> it.satsType() == satsType)
            .stream()
            .map(s -> {
                var it = s.getValue();
                return new TilkjentPeriodeDto(
                    new PeriodeDto(s.getFom(), s.getTom()),
                    new TilkjentSatsDto(it.dagsats(), it.grunnbeløpFaktor(), it.dagsatsGrunnbeløpFaktor(), it.grunnbeløp(), it.årsbeløp(), it.antallBarn(), it.barnetilleggSats()));
            }).toList();
    }

    private ResultatFlaggDto lagResultatFlaggDto(
        LocalDateTimeline<GrunnlagOgTilkjentYtelse> grunnlagOgTilkjentYtelseTimeline,
        VurderAntallDagerResultat vurderAntallDagerResultat,
        Behandling behandling) {

        var grunnlagOgTilkjentYtelse = grunnlagOgTilkjentYtelseTimeline.stream()
            .map(LocalDateSegment::getValue)
            .distinct()
            .toList();


        boolean enDagsats = grunnlagOgTilkjentYtelse.stream().collect(Collectors.groupingBy(GrunnlagOgTilkjentYtelse::dagsatsTilkjentYtelse)).size() == 1;
        boolean ettGbeløp = grunnlagOgTilkjentYtelse.stream().collect(Collectors.groupingBy(GrunnlagOgTilkjentYtelse::grunnbeløp)).size() == 1;

        var satsTyper = grunnlagOgTilkjentYtelse.stream().map(GrunnlagOgTilkjentYtelse::satsType).collect(Collectors.toSet());
        boolean lavSats = satsTyper.stream().allMatch(it -> it == UngdomsytelseSatsType.LAV);
        boolean høySats = satsTyper.stream().allMatch(it -> it == UngdomsytelseSatsType.HØY);
        boolean varierendeSats = satsTyper.contains(UngdomsytelseSatsType.LAV) && satsTyper.contains(UngdomsytelseSatsType.HØY);
        boolean harBarn = grunnlagOgTilkjentYtelse.stream().anyMatch(it -> it.antallBarn() > 0);

        LocalDate fødselsdato = personopplysningRepository.hentPersonopplysninger(behandling.getId())
            .getGjeldendeVersjon()
            .getPersonopplysning(behandling.getAktørId())
            .getFødselsdato();
        //TODO er dette mulig?
        boolean oppnårMaksAlder = vurderAntallDagerResultat.tidslinjeNokDager()
            .getMaxLocalDate()
            .isAfter(fødselsdato.plusYears(Sats.HØY.getTomAlder()));

        return new ResultatFlaggDto(
            enDagsats,
            ettGbeløp,
            lavSats,
            høySats,
            varierendeSats,
            oppnårMaksAlder,
            harBarn);
    }

    private static LocalDateSegment<GrunnlagOgTilkjentYtelse> sammenstillGrunnlagOgTilkjentYtelse(
        LocalDateInterval di, LocalDateSegment<TilkjentYtelseVerdi> lhs, LocalDateSegment<UngdomsytelseSatser> rhs) {
        var tilkjentYtelse = lhs.getValue();
        var satsPerioder = rhs.getValue();
        return new LocalDateSegment<>(di,
            new GrunnlagOgTilkjentYtelse(
                satsPerioder.satsType(),
                tilHeltall(satsPerioder.dagsats().add(BigDecimal.valueOf(satsPerioder.dagsatsBarnetillegg()))), tilHeltall(satsPerioder.grunnbeløp()),
                tilFaktor(satsPerioder.grunnbeløpFaktor()),
                tilHeltall(satsPerioder.grunnbeløp().multiply(satsPerioder.grunnbeløpFaktor())),
                tilHeltall(satsPerioder.dagsats()),
                satsPerioder.antallBarn(),
                satsPerioder.dagsatsBarnetillegg(),
                tilHeltall(tilkjentYtelse.dagsats()),
                tilkjentYtelse.utbetalingsgrad()
                ));

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

    @Deprecated
    private static List<TilkjentPeriodeDto> lagTilkjentePerioderDto(LocalDateTimeline<GrunnlagOgTilkjentYtelse> grunnlagOgTilkjentYtelseTimeline) {
        return grunnlagOgTilkjentYtelseTimeline
            .mapSegment(it ->
                new TilkjentSatsDto(it.dagsats(), it.grunnbeløpFaktor(), it.dagsatsTilkjentYtelse(), it.grunnbeløp(), it.årsbeløp(), it.antallBarn(), it.barnetilleggSats()))
            .compress().stream()
            .sorted(Comparator.comparing(LocalDateSegment::getLocalDateInterval)) //eldste først for tilkjent perioder
            .map(it ->
                new TilkjentPeriodeDto(new PeriodeDto(it.getFom(), it.getTom()), it.getValue()))
            .toList();
    }

    private static BigDecimal avrundTilHeltall(BigDecimal decimal) {
        return decimal.setScale(0, RoundingMode.HALF_UP);
    }

}
