package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriode;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet.Kravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelsePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;

@Dependent
public class SamtidigUttakOverlappsjekker {

    private BehandlingRepository behandlingRepository;
    private PleietrengendeKravprioritet pleietrengendeKravprioritet;
    private SykdomVurderingService sykdomVurderingService;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;
    private Boolean utsattBehandlingAvPeriode;


    @Inject
    public SamtidigUttakOverlappsjekker(BehandlingRepository behandlingRepository,
                                        PleietrengendeKravprioritet pleietrengendeKravprioritet,
                                        SykdomVurderingService sykdomVurderingService,
                                        UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                        @KonfigVerdi(value = "utsatt.behandling.av.periode.aktivert", defaultVerdi = "false") Boolean utsattBehandlingAvPeriode) {
        this.behandlingRepository = behandlingRepository;
        this.pleietrengendeKravprioritet = pleietrengendeKravprioritet;
        this.sykdomVurderingService = sykdomVurderingService;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        this.utsattBehandlingAvPeriode = utsattBehandlingAvPeriode;
    }


    public boolean isHarRelevantOverlappMedAndreUbehandledeSaker(BehandlingReferanse ref) {
        final var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());

        final LocalDateTimeline<List<Kravprioritet>> kravprioritet = pleietrengendeKravprioritet.vurderKravprioritet(ref.getFagsakId(), ref.getPleietrengendeAktørId(), true);
        final LocalDateTimeline<List<Kravprioritet>> perioderMedOverlapp;

        if (utsattBehandlingAvPeriode) {
            final LocalDateTimeline<List<Kravprioritet>> vedtatteKrav = pleietrengendeKravprioritet.vurderKravprioritet(ref.getFagsakId(), ref.getPleietrengendeAktørId(), false);
            perioderMedOverlapp = new LocalDateTimeline<>(kravprioritet
                .stream()
                .map(segment -> hensyntaBesluttedeKrav(vedtatteKrav, segment))
                .flatMap(Collection::stream)
                .filter(segment -> hensyntaUtsettelserPåÅpneBehandlinger(ref, segment))
                .collect(Collectors.toList()));
        } else {
            perioderMedOverlapp = kravprioritet
                .filterValue(kravprioritetsliste -> !kravprioritetsliste.isEmpty()
                    && harDenneSaken(ref, kravprioritetsliste)
                    && harIkkePrioritetBlantUbesluttedeBehandlinger(ref, kravprioritetsliste)
                );
        }
        // TODO: Fjern perioder fra "perioderMedOverlapp" som ikke er til vurdering i noen av de åpne behandlingene.

        if (perioderMedOverlapp.isEmpty()) {
            return false;
        }

        final LocalDateTimeline<Boolean> innleggelseTimeline = hentInnleggelseTimeline(behandling);
        if (manglerInnleggelseIPeriodeMedOverlapp(perioderMedOverlapp, innleggelseTimeline)) {
            return true;
        }

        return erIkkeSøkerMedAndreprioritetPåBarnetIPerioderMedInnleggelse(ref, perioderMedOverlapp, innleggelseTimeline);
    }

    private boolean hensyntaUtsettelserPåÅpneBehandlinger(BehandlingReferanse ref, LocalDateSegment<List<Kravprioritet>> segment) {
        var kravprioritetsliste = segment.getValue();
        return !kravprioritetsliste.isEmpty()
            && harDenneSaken(ref, kravprioritetsliste)
            && harIkkePrioritetBlantUbesluttedeBehandlinger(ref, kravprioritetsliste, DatoIntervallEntitet.fra(segment.getLocalDateInterval()));
    }

    public LocalDateTimeline<Boolean> utledPerioderHvorSøkerIkkeHarPrioritetMedUbesluttetOverlapp(BehandlingReferanse ref) {
        final LocalDateTimeline<List<Kravprioritet>> vedtatteKrav = pleietrengendeKravprioritet.vurderKravprioritet(ref.getFagsakId(), ref.getPleietrengendeAktørId(), false);
        final LocalDateTimeline<List<Kravprioritet>> kravprioritet = pleietrengendeKravprioritet.vurderKravprioritet(ref.getFagsakId(), ref.getPleietrengendeAktørId(), true);

        final LocalDateTimeline<List<Kravprioritet>> perioderMedOverlapp = new LocalDateTimeline<>(kravprioritet
            .stream()
            .map(segment -> hensyntaBesluttedeKrav(vedtatteKrav, segment))
            .flatMap(Collection::stream)
            .filter(segment -> hensyntaUtsettelserPåÅpneBehandlinger(ref, segment))
            .collect(Collectors.toList()));

        final var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final LocalDateTimeline<Boolean> innleggelseTimeline = hentInnleggelseTimeline(behandling);

        var perioderHvorOverlappKanSesBortIfraPgaInnleggelse = taHensynTilInnleggelser(perioderMedOverlapp, innleggelseTimeline, ref);
        var perioderMedOverlappUtenInnleggelse = SykdomUtils.kunPerioderSomIkkeFinnesI(perioderMedOverlapp, perioderHvorOverlappKanSesBortIfraPgaInnleggelse);

        return perioderMedOverlappUtenInnleggelse.filterValue(it -> !it.isEmpty()).mapValue(it -> true);
    }

    private NavigableSet<LocalDateSegment<List<Kravprioritet>>> hensyntaBesluttedeKrav(LocalDateTimeline<List<Kravprioritet>> vedtatteKrav, LocalDateSegment<List<Kravprioritet>> segment) {
        var vedtatteKravSomOverlapper = vedtatteKrav.union(new LocalDateTimeline<>(List.of(segment)), (datoInterval, segmentLeft, segmentRight) -> {
            if (segmentLeft == null) {
                return new LocalDateSegment<>(datoInterval, segmentRight.getValue());
            }
            if (segmentRight == null) {
                return new LocalDateSegment<>(datoInterval, segmentLeft.getValue());
            }

            var filtrertKravliste = segmentRight.getValue()
                .stream()
                .filter(it -> segmentLeft.getValue()
                    .stream()
                    .filter(at -> Objects.equals(it.getSaksnummer(), at.getSaksnummer())) // relevante vedtak
                    .noneMatch(at -> harBlittVedtatt(it, at)))
                .collect(Collectors.toList());
            return new LocalDateSegment<>(datoInterval, filtrertKravliste);
        });

        return vedtatteKravSomOverlapper.toSegments();
    }

    private boolean harBlittVedtatt(Kravprioritet it, Kravprioritet at) {
        return Objects.equals(at.getTidspunktForKrav(), it.getTidspunktForKrav()) && at.getAktuellBehandling().erSaksbehandlingAvsluttet();
    }

    private LocalDateTimeline<Boolean> utledUtsattTidslinje(BehandlingReferanse ref) {
        return new LocalDateTimeline<>(utsattBehandlingAvPeriodeRepository.hentGrunnlag(ref.getBehandlingId()).stream().map(UtsattBehandlingAvPeriode::getPerioder).flatMap(Collection::stream).map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true)).collect(Collectors.toList()), StandardCombinators::alwaysTrueForMatch);
    }

    private LocalDateTimeline<List<Kravprioritet>> taHensynTilInnleggelser(LocalDateTimeline<List<Kravprioritet>> perioderMedOverlapp, LocalDateTimeline<Boolean> innleggelseTimeline, BehandlingReferanse ref) {
        return perioderMedOverlapp.intersection(innleggelseTimeline).filterValue(kravprioritetsliste -> harIkkeAndreprioritet(ref, kravprioritetsliste));
    }

    private LocalDateTimeline<Boolean> hentInnleggelseTimeline(Behandling behandling) {
        final List<SykdomInnleggelsePeriode> innleggelser = sykdomVurderingService.hentInnleggelser(behandling).getPerioder();
        return new LocalDateTimeline<>(innleggelser.stream().map(i -> new LocalDateSegment<>(i.getFom(), i.getTom(), Boolean.TRUE)).collect(Collectors.toList()));
    }

    private boolean harDenneSaken(BehandlingReferanse ref, List<Kravprioritet> kravprioritetsliste) {
        return kravprioritetsliste.stream().anyMatch(k -> k.getSaksnummer().equals(ref.getSaksnummer()));
    }

    private boolean harIkkePrioritetBlantUbesluttedeBehandlinger(BehandlingReferanse ref, List<Kravprioritet> kravprioritetsliste, DatoIntervallEntitet periode) {
        for (Kravprioritet k : kravprioritetsliste) {
            if (k.getSaksnummer().equals(ref.getSaksnummer())) {
                return false;
            }
            if (!k.getAktuellBehandling().erStatusFerdigbehandlet() && !harUtsattBehandlingAvPeriode(k.getAktuellBehandling(), periode)) {
                return true;
            }
        }
        throw new IllegalStateException("Dette skal ikke kunne skje fordi det er en forutsetning om at ref.getSaksnummer finnes i 'kravprioritetsliste'.");
    }

    private boolean harIkkePrioritetBlantUbesluttedeBehandlinger(BehandlingReferanse ref, List<Kravprioritet> kravprioritetsliste) {
        for (Kravprioritet k : kravprioritetsliste) {
            if (k.getSaksnummer().equals(ref.getSaksnummer())) {
                return false;
            }
            if (!k.getAktuellBehandling().erStatusFerdigbehandlet()) {
                return true;
            }
        }
        throw new IllegalStateException("Dette skal ikke kunne skje fordi det er en forutsetning om at ref.getSaksnummer finnes i 'kravprioritetsliste'.");
    }

    private boolean harUtsattBehandlingAvPeriode(Behandling aktuellBehandling, DatoIntervallEntitet periode) {
        var utsattTidslinje = utledUtsattTidslinje(BehandlingReferanse.fra(aktuellBehandling));
        if (utsattTidslinje.isEmpty()) {
            return false;
        }

        return new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.toLocalDateInterval(), true))).disjoint(utsattTidslinje).isEmpty();
    }

    private boolean harIkkeAndreprioritet(BehandlingReferanse ref, List<Kravprioritet> kravprioritetsliste) {
        return !kravprioritetsliste.get(1).getSaksnummer().equals(ref.getSaksnummer());
    }

    private boolean manglerInnleggelseIPeriodeMedOverlapp(LocalDateTimeline<List<Kravprioritet>> perioderMedOverlapp,
                                                          LocalDateTimeline<Boolean> innleggelseTimeline) {
        return !SykdomUtils.kunPerioderSomIkkeFinnesI(perioderMedOverlapp, innleggelseTimeline).isEmpty();
    }

    private boolean erIkkeSøkerMedAndreprioritetPåBarnetIPerioderMedInnleggelse(BehandlingReferanse ref,
                                                                                LocalDateTimeline<List<Kravprioritet>> perioderMedOverlapp,
                                                                                LocalDateTimeline<Boolean> innleggelseTimeline) {

        return !perioderMedOverlapp.intersection(innleggelseTimeline)
            .filterValue(kravprioritetsliste -> harIkkeAndreprioritet(ref, kravprioritetsliste))
            .isEmpty();
    }
}
