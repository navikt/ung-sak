package no.nav.ung.sak.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import static no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger.tilFaktor;
import static no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger.tilHeltall;

@Dependent
public class InnvilgelseInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(InnvilgelseInnholdBygger.class);

    private final UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private final boolean ignoreIkkeStøttedeBrev;

    @Inject
    public InnvilgelseInnholdBygger(
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
        UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
        @KonfigVerdi(value = "IGNORE_FEIL_INNVILGELSESBREV", defaultVerdi = "false") boolean ignoreFeil) {

        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.ignoreIkkeStøttedeBrev = ignoreFeil;
    }


    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        var brevfeilSamler = new BrevfeilHåndterer(!ignoreIkkeStøttedeBrev);
        Long behandlingId = behandling.getId();

        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Mangler grunnlag"));

        LocalDateTimeline<UngdomsytelseSatser> satsTidslinje = ungdomsytelseGrunnlag.getSatsTidslinje();

        var ytelseFom = detaljertResultatTidslinje.getMinLocalDate();
        var satsEndringHendelseDtos = lagSatsEndringHendelser(satsTidslinje, brevfeilSamler);
        var satsOgBeregningDto = mapSatsOgBeregning(satsTidslinje.toSegments(), brevfeilSamler);
        var førsteSatser = satsTidslinje.toSegments().first().getValue();
        var dagsatsFom = tilHeltall(førsteSatser.dagsats().add(BigDecimal.valueOf(førsteSatser.dagsatsBarnetillegg())));

        var vurderAntallDagerResultat = ungdomsprogramPeriodeTjeneste.finnVirkedagerTidslinje(behandlingId);

        long antallDager = vurderAntallDagerResultat.forbrukteDager();
        if (antallDager <= 0) {
            brevfeilSamler.registrerFeilmelding("Antall virkedager i programmet = %d, kan ikke sende innvilgelsesbrev da".formatted(antallDager));
        }
        var ytelseTom = FinnForbrukteDager.MAKS_ANTALL_DAGER != antallDager ? detaljertResultatTidslinje.getMaxLocalDate() : null;

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
                brevfeilSamler.samletFeiltekst()));
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

        var fødselBarn = currentSatser.antallBarn() > previousSatser.antallBarn();
        var dødsfallBarn = currentSatser.antallBarn() < previousSatser.antallBarn();
        var fikkFlereBarn = currentSatser.antallBarn() > previousSatser.antallBarn()
            && currentSatser.antallBarn() - previousSatser.antallBarn() > 1;
        var overgangTilHøySats = currentSatser.satsType() == UngdomsytelseSatsType.HØY && previousSatser.satsType() == UngdomsytelseSatsType.LAV;
        var overgangLavSats = currentSatser.satsType() == UngdomsytelseSatsType.LAV && previousSatser.satsType() == UngdomsytelseSatsType.HØY;

        if (overgangLavSats) {
            brevfeilHåndterer.registrerFeilmelding("Kan ikke ha overgang fra høy til lav sats men fant det mellom %s og %s".formatted(previous.getLocalDateInterval(), current.getLocalDateInterval()));
        }
        int antallEndringer = (overgangTilHøySats ? 1 : 0) + (fødselBarn ? 1 : 0) + (dødsfallBarn ? 1 : 0);
        if (antallEndringer > 1) {
            brevfeilHåndterer.registrerFeilmelding("Støtter ikke flere endringer for samme periode. Fant overgangTilHøySats=%s, fødselBarn=%s eller dødsfallBarn=%s for perioden %s"
                .formatted(overgangTilHøySats, fødselBarn, dødsfallBarn, current.getLocalDateInterval()));
        }

        return new SatsEndringHendelseDto(
            overgangTilHøySats,
            fødselBarn,
            dødsfallBarn,
            current.getFom(),
            tilHeltall(currentSatser.dagsats().add(BigDecimal.valueOf(currentSatser.dagsatsBarnetillegg()))),
            dødsfallBarn ? previousSatser.dagsatsBarnetillegg() : currentSatser.dagsatsBarnetillegg(),
            fikkFlereBarn
        );
    }

    private static SatsOgBeregningDto mapSatsOgBeregning(NavigableSet<LocalDateSegment<UngdomsytelseSatser>> satsSegments, BrevfeilHåndterer brevfeilHåndterer) {
        var satser = satsSegments.stream()
            .map(it -> it.getValue().satsType())
            .collect(Collectors.toSet());

        var kunHøySats = satser.size() == 1 && satser.contains(UngdomsytelseSatsType.HØY);

        var beregning = mapTilBeregningDto(satsSegments.first().getValue());

        var nyesteSegment = satsSegments.last();
        var nyesteSats = nyesteSegment.getValue();

        var overgangTilHøySats = satser.size() > 1 ? mapOvergangTilHøySats(nyesteSegment, brevfeilHåndterer) :  null;

        var barnetillegg = nyesteSats.antallBarn() > 0
            ? new BarnetilleggDto(
                nyesteSats.antallBarn(),
                nyesteSats.dagsatsBarnetillegg(),
                tilHeltall(nyesteSats.dagsats().add(BigDecimal.valueOf(nyesteSats.dagsatsBarnetillegg()))))
            : null;

        return new SatsOgBeregningDto(
            Sats.HØY.getFomAlder(),
            kunHøySats,
            beregning,
            overgangTilHøySats,
            barnetillegg);
    }

    private static BeregningDto mapOvergangTilHøySats(LocalDateSegment<UngdomsytelseSatser> nyesteSegment, BrevfeilHåndterer brevfeilHåndterer) {
        var nyesteSats = nyesteSegment.getValue();
        if (nyesteSats.satsType() != UngdomsytelseSatsType.HØY) {
            brevfeilHåndterer.registrerFeilmelding("Forventet at nyeste sats skulle være høy når det er flere satser, men var %s for periode %s".formatted(nyesteSats.satsType(), nyesteSegment.getLocalDateInterval()));
        }
        return mapTilBeregningDto(nyesteSats);
    }

    private static BeregningDto mapTilBeregningDto(UngdomsytelseSatser sats) {
        return new BeregningDto(
            tilFaktor(sats.grunnbeløpFaktor()),
            tilHeltall(sats.grunnbeløp().multiply(sats.grunnbeløpFaktor())),
            tilHeltall(sats.dagsats()));
    }

}
