package no.nav.ung.sak.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.formidling.template.dto.InnvilgelseDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.SatsEndringHendelseDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.beregning.BarnetilleggDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.beregning.BeregningDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.beregning.SatsOgBeregningDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.FinnForbrukteDager;
import org.slf4j.Logger;

import java.time.LocalDate;
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
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private final boolean ignoreIkkeStøttedeBrev;
    private final TilkjentYtelseRepository tilkjentYtelseRepository;
    private final LocalDate overrideDagensDatoForTest;

    @Inject
    public FørstegangsInnvilgelseInnholdBygger(
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
        UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
        TilkjentYtelseRepository tilkjentYtelseRepository,
        @KonfigVerdi(value = "IGNORE_FEIL_INNVILGELSESBREV", defaultVerdi = "false") boolean ignoreFeil,
        @KonfigVerdi(value = "BREV_DAGENS_DATO_TEST", required = false) LocalDate overrideDagensDatoForTest) {

        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.ignoreIkkeStøttedeBrev = ignoreFeil;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.overrideDagensDatoForTest = overrideDagensDatoForTest;
    }


    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        var brevfeilSamler = new BrevfeilHåndterer(!ignoreIkkeStøttedeBrev);
        Long behandlingId = behandling.getId();

        var ytelseFom = detaljertResultatTidslinje.getMinLocalDate();
        var ytelseTom = finnEvtTomDato(detaljertResultatTidslinje, behandlingId, brevfeilSamler);

        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Mangler grunnlag"));

        LocalDateTimeline<UngdomsytelseSatser> satsTidslinje = ungdomsytelseGrunnlag.getSatsTidslinje();

        var førsteSatser = satsTidslinje.toSegments().first().getValue();
        var dagsatsFom = Satsberegner.beregnDagsatsInklBarnetillegg(førsteSatser);

        var satsEndringHendelseDtos = lagSatsEndringHendelser(satsTidslinje, brevfeilSamler);

        var satsOgBeregningDto = mapSatsOgBeregning(satsTidslinje.toSegments(), brevfeilSamler);

        var erEtterbetaling = erEtterbetaling(behandling, detaljertResultatTidslinje, brevfeilSamler);

        if (brevfeilSamler.harFeil()) {
            LOG.warn("Innvilgelse brev har feil som ignoreres. Brevet er mest sannsynlig feil! Feilmelding(er): {}", brevfeilSamler.samletFeiltekst());
        }

        return new TemplateInnholdResultat(DokumentMalType.INNVILGELSE_DOK, TemplateType.INNVILGELSE,
            new InnvilgelseDto(
                ytelseFom,
                ytelseTom,
                dagsatsFom,
                satsEndringHendelseDtos,
                satsOgBeregningDto,
                brevfeilSamler.samletFeiltekst(),
                erEtterbetaling));
    }

    private boolean erEtterbetaling(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje, BrevfeilHåndterer brevfeilSamler) {
        var tilkjentYtelseTimeline = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).intersection(detaljertResultatTidslinje);
        if (tilkjentYtelseTimeline.isEmpty()) {
            brevfeilSamler.registrerFeilmelding("Fant ingen tilkjent ytelse tidslinje for behandling i perioden %s".formatted(detaljertResultatTidslinje.getLocalDateIntervals()));
        }
        var førsteTilkjentMåned = tilkjentYtelseTimeline.getMinLocalDate().withDayOfMonth(1);
        var dagensDato = Environment.current().isLocal() && overrideDagensDatoForTest != null ? overrideDagensDatoForTest : LocalDate.now();

        return førsteTilkjentMåned.isBefore(dagensDato.withDayOfMonth(1));
    }

    private LocalDate finnEvtTomDato(LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje, Long behandlingId, BrevfeilHåndterer brevfeilSamler) {
        var vurderAntallDagerResultat = ungdomsprogramPeriodeTjeneste.finnVirkedagerTidslinje(behandlingId);

        long antallDager = vurderAntallDagerResultat.forbrukteDager();
        if (antallDager <= 0) {
            brevfeilSamler.registrerFeilmelding("Antall virkedager i programmet = %d, kan ikke sende innvilgelsesbrev da".formatted(antallDager));
        }
        return FinnForbrukteDager.MAKS_ANTALL_DAGER != antallDager ? detaljertResultatTidslinje.getMaxLocalDate() : null;
    }

    private List<SatsEndringHendelseDto> lagSatsEndringHendelser(LocalDateTimeline<UngdomsytelseSatser> satsTidslinje, BrevfeilHåndterer brevfeilHåndterer) {
        List<SatsEndringHendelseDto> resultat = new ArrayList<>();
        var satsSegments = satsTidslinje.toSegments();
        LocalDateSegment<UngdomsytelseSatser> previous = null;
        for (LocalDateSegment<UngdomsytelseSatser> current : satsSegments) {
            if (previous == null) {
                previous = current;
                continue;
            }
            resultat.add(lagSatsEndringHendelseDto(brevfeilHåndterer, current, previous));
            previous = current;
        }

        return resultat;

    }

    private static SatsEndringHendelseDto lagSatsEndringHendelseDto(BrevfeilHåndterer brevfeilHåndterer, LocalDateSegment<UngdomsytelseSatser> current, LocalDateSegment<UngdomsytelseSatser> previous) {
        var currentSatser = current.getValue();
        var previousSatser = previous.getValue();

        int gjeldendeAntallBarn = currentSatser.antallBarn();
        int tidligereAntallBarn = previousSatser.antallBarn();
        var fødselBarn = gjeldendeAntallBarn > tidligereAntallBarn;
        var dødsfallBarn = gjeldendeAntallBarn < tidligereAntallBarn;
        var fikkFlereBarn = gjeldendeAntallBarn > tidligereAntallBarn && gjeldendeAntallBarn - tidligereAntallBarn > 1;
        var overgangTilHøySats = currentSatser.satsType() == UngdomsytelseSatsType.HØY && previousSatser.satsType() == UngdomsytelseSatsType.LAV;
        var overgangLavSats = currentSatser.satsType() == UngdomsytelseSatsType.LAV && previousSatser.satsType() == UngdomsytelseSatsType.HØY;

        if (overgangLavSats) {
            brevfeilHåndterer.registrerFeilmelding("Kan ikke ha overgang fra høy til lav sats men fant det mellom %s og %s".formatted(previous.getLocalDateInterval(), current.getLocalDateInterval()));
        }

        return new SatsEndringHendelseDto(
            overgangTilHøySats,
            fødselBarn,
            dødsfallBarn,
            current.getFom(),
            Satsberegner.beregnDagsatsInklBarnetillegg(currentSatser),
            dødsfallBarn ? Satsberegner.beregnBarnetilleggSats(previousSatser) : Satsberegner.beregnBarnetilleggSats(currentSatser),
            fikkFlereBarn
        );
    }

    private static SatsOgBeregningDto mapSatsOgBeregning(NavigableSet<LocalDateSegment<UngdomsytelseSatser>> satsSegments, BrevfeilHåndterer brevfeilHåndterer) {
        var satser = satsSegments.stream()
            .map(it -> it.getValue().satsType())
            .collect(Collectors.toSet());

        var kunHøySats = Set.of(UngdomsytelseSatsType.HØY).equals(satser);

        var beregning = mapTilBeregningDto(satsSegments.first());

        var nyesteSegment = satsSegments.last();
        var nyesteSats = nyesteSegment.getValue();

        var overgangTilHøySats = satser.size() > 1 ? mapOvergangTilHøySats(nyesteSegment, brevfeilHåndterer) :  null;
        var grunnbeløp = tilHeltall(nyesteSats.grunnbeløp());

        var barnetillegg = nyesteSats.antallBarn() > 0
            ? new BarnetilleggDto(
            Satsberegner.tallTilNorskHunkjønnTekst(nyesteSats.antallBarn()),
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

    private static BeregningDto mapOvergangTilHøySats(LocalDateSegment<UngdomsytelseSatser> nyesteSegment, BrevfeilHåndterer brevfeilHåndterer) {
        var nyesteSats = nyesteSegment.getValue();
        if (nyesteSats.satsType() != UngdomsytelseSatsType.HØY) {
            brevfeilHåndterer.registrerFeilmelding("Forventet at nyeste sats skulle være høy når det er flere satser, men var %s for periode %s".formatted(nyesteSats.satsType(), nyesteSegment.getLocalDateInterval()));
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
