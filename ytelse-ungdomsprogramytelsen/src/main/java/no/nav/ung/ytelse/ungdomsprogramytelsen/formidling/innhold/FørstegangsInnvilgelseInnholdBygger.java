package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramOpphørUtleder;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.formidling.vedtak.satsendring.SatsEndring;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.InnvilgelseDto;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.innvilgelse.SatsEndringHendelseDto;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.innvilgelse.beregning.BarnetilleggDto;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.innvilgelse.beregning.BeregningDto;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.innvilgelse.beregning.SatsOgBeregningDto;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger.tilHeltall;

@Dependent
public class FørstegangsInnvilgelseInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FørstegangsInnvilgelseInnholdBygger.class);

    private final UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final boolean ignoreIkkeStøttedeBrev;

    @Inject
    public FørstegangsInnvilgelseInnholdBygger(
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
        @KonfigVerdi(value = "IGNORE_FEIL_INNVILGELSESBREV", defaultVerdi = "false") boolean ignoreFeil) {

        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ignoreIkkeStøttedeBrev = ignoreFeil;
    }


    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, DetaljertResultatTidslinje tidslinje) {
        var detaljertResultatTidslinje = tidslinje.tilVurdering();
        Long behandlingId = behandling.getId();

        var innvilgetTidslinje = detaljertResultatTidslinje
            .filterValue(r -> erInnvilgelseMedUtbetaling(r, behandling.erManueltOpprettet()));

        var ytelseFom = innvilgetTidslinje.getMinLocalDate();

        var ytelseTom = finnEvtTomDato(behandlingId);

        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Mangler grunnlag"));

        LocalDateTimeline<UngdomsytelseSatser> satsTidslinje = ungdomsytelseGrunnlag.getSatsTidslinje();

        var førsteSatser = satsTidslinje.toSegments().first().getValue();
        var dagsatsFom = Satsberegner.beregnDagsatsInklBarnetillegg(førsteSatser);

        var satsEndringHendelseDtos = lagSatsEndringHendelser(satsTidslinje);

        var satsOgBeregningDto = mapSatsOgBeregning(satsTidslinje.toSegments());

        var erEtterbetaling = erEtterbetaling(innvilgetTidslinje);

        var sisteUtbetalingsdato = ytelseTom != null ? PeriodeBeregner.utledFremtidigUtbetalingsdato(ytelseTom, YearMonth.from(bestemDagensDato())) : null;

        return new TemplateInnholdResultat(TemplateType.INNVILGELSE,
            new InnvilgelseDto(
                ytelseFom,
                ytelseTom,
                dagsatsFom,
                satsEndringHendelseDtos,
                satsOgBeregningDto,
                null,
                erEtterbetaling,
                satsEndringHendelseDtos.isEmpty(),
                sisteUtbetalingsdato));
    }

    private boolean erEtterbetaling(LocalDateTimeline<DetaljertResultat> innvilgetTidslinje) {
        if (innvilgetTidslinje.isEmpty()) {
            throw new IllegalStateException("Fant ingen innvilget tidslinje med utbetaling for behandlingen");
        }
        var førsteUtbetalingsmåned = innvilgetTidslinje.getMinLocalDate().withDayOfMonth(1);
        return førsteUtbetalingsmåned.isBefore(bestemDagensDato().withDayOfMonth(1));
    }

    private LocalDate bestemDagensDato() {
        //Kan ikke injectes i konstruktør fordi den settes én gang for hele testkjøringen pga application scoped
        var overrideDagensDatoForTest = Environment.current().getProperty("BREV_DAGENS_DATO_TEST", LocalDate.class);
        return Environment.current().isLocal() && overrideDagensDatoForTest != null ? overrideDagensDatoForTest : LocalDate.now();
    }

    private LocalDate finnEvtTomDato(Long behandlingId) {
        // Åpen sluttdato (tidenes ende) => løpende program, ingen tom-dato i brevet. Ellers vises faktisk sluttdato.
        return ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId)
            .flatMap(UngdomsprogramOpphørUtleder::finnLukketSluttdato)
            .orElse(null);
    }

    private static boolean erInnvilgelseMedUtbetaling(DetaljertResultat r, boolean manueltOpprettet) {
        boolean nyPeriode = r.harÅrsak(BehandlingÅrsakType.NY_SØKT_PERIODE)
            || (manueltOpprettet && r.harÅrsak(BehandlingÅrsakType.RE_SATS_ENDRING));
        return nyPeriode && r.utbetalingsgrad().erSatt();
    }

    private List<SatsEndringHendelseDto> lagSatsEndringHendelser(LocalDateTimeline<UngdomsytelseSatser> satsTidslinje) {
        List<SatsEndringHendelseDto> resultat = new ArrayList<>();
        var satsSegments = satsTidslinje.toSegments();
        LocalDateSegment<UngdomsytelseSatser> previous = null;
        for (LocalDateSegment<UngdomsytelseSatser> current : satsSegments) {
            if (previous == null) {
                previous = current;
                continue;
            }
            resultat.add(lagSatsEndringHendelseDto(current, previous));
            previous = current;
        }

        return resultat;

    }

    private static SatsEndringHendelseDto lagSatsEndringHendelseDto(LocalDateSegment<UngdomsytelseSatser> current, LocalDateSegment<UngdomsytelseSatser> previous) {
        var currentSatser = current.getValue();
        var previousSatser = previous.getValue();

        SatsEndring result = SatsEndringUtleder.bestemSatsendring(currentSatser, previousSatser);

        if (result.overgangLavSats()) {
            throw new IllegalStateException("Kan ikke ha overgang fra høy til lav sats men fant det mellom %s og %s".formatted(previous.getLocalDateInterval(), current.getLocalDateInterval()));
        }

        if (result.dødsfallBarn()) {
            throw new IllegalStateException("Støtter ikke brev ved dødsfall av barn");
        }

        return new SatsEndringHendelseDto(
            result.overgangTilHøySats(),
            result.fødselBarn(),
            result.dødsfallBarn(),
            current.getFom(),
            Satsberegner.beregnDagsatsInklBarnetillegg(currentSatser),
            result.dødsfallBarn() ? Satsberegner.beregnBarnetilleggSats(previousSatser) : Satsberegner.beregnBarnetilleggSats(currentSatser),
            result.fikkFlereBarn()
        );
    }

    private static SatsOgBeregningDto mapSatsOgBeregning(NavigableSet<LocalDateSegment<UngdomsytelseSatser>> satsSegments) {
        var satser = satsSegments.stream()
            .map(it -> it.getValue().satsType())
            .collect(Collectors.toSet());

        var kunHøySats = Set.of(UngdomsytelseSatsType.HØY).equals(satser);

        var beregning = mapTilBeregningDto(satsSegments.first());

        var nyesteSegment = satsSegments.last();
        var nyesteSats = nyesteSegment.getValue();

        var overgangTilHøySats = satser.size() > 1 ? mapOvergangTilHøySats(nyesteSegment) :  null;
        var grunnbeløp = tilHeltall(nyesteSats.grunnbeløp());

        var barnetillegg = nyesteSats.antallBarn() > 0
            ? new BarnetilleggDto(
            Satsberegner.tallTilNorskHunkjønnTekst(nyesteSats.antallBarn()),
            nyesteSats.antallBarn() > 1,
            Satsberegner.beregnBarnetilleggSats(nyesteSats),
            Satsberegner.beregnDagsatsInklBarnetillegg(nyesteSats))
            : null;

        return new SatsOgBeregningDto(
            Sats.HØY.getFomAlder(),
            kunHøySats,
            grunnbeløp,
            beregning,
            overgangTilHøySats,
            barnetillegg);
    }

    private static BeregningDto mapOvergangTilHøySats(LocalDateSegment<UngdomsytelseSatser> nyesteSegment) {
        var nyesteSats = nyesteSegment.getValue();
        if (nyesteSats.satsType() != UngdomsytelseSatsType.HØY) {
            throw new IllegalStateException("Forventet at nyeste sats skulle være høy når det er flere satser, men var %s for periode %s".formatted(nyesteSats.satsType(), nyesteSegment.getLocalDateInterval()));
        }
        return mapTilBeregningDto(nyesteSegment);
    }

    private static BeregningDto mapTilBeregningDto(LocalDateSegment<UngdomsytelseSatser> satssegment) {
        var sats = satssegment.getValue();

        return new BeregningDto(
            Satsberegner.lagGrunnbeløpFaktorTekst(satssegment), tilHeltall(sats.grunnbeløp().multiply(sats.grunnbeløpFaktor())),
            tilHeltall(sats.dagsats())
        );
    }

}
