package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet.Kravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelsePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;

@Dependent
public class SamtidigUttakOverlappsjekker {

    private BehandlingRepository behandlingRepository;
    private PleietrengendeKravprioritet pleietrengendeKravprioritet;
    private SykdomVurderingService sykdomVurderingService;


    @Inject
    public SamtidigUttakOverlappsjekker(BehandlingRepository behandlingRepository,
            PleietrengendeKravprioritet pleietrengendeKravprioritet,
            SykdomVurderingService sykdomVurderingService) {
        this.behandlingRepository = behandlingRepository;
        this.pleietrengendeKravprioritet = pleietrengendeKravprioritet;
        this.sykdomVurderingService = sykdomVurderingService;
    }


    public boolean isHarRelevantOverlappMedAndreUbehandledeSaker(BehandlingReferanse ref) {
        final var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());

        final LocalDateTimeline<List<Kravprioritet>> kravprioritet = pleietrengendeKravprioritet.vurderKravprioritet(ref.getFagsakId(), ref.getPleietrengendeAktørId(), true);

        final LocalDateTimeline<List<Kravprioritet>> perioderMedOverlapp = kravprioritet
            .filterValue(kravprioritetsliste -> !kravprioritetsliste.isEmpty()
                && harDenneSaken(ref, kravprioritetsliste)
                && harIkkePrioritetBlantUbesluttedeBehandlinger(ref, kravprioritetsliste)
            );

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


    private LocalDateTimeline<Boolean> hentInnleggelseTimeline(Behandling behandling) {
        final List<SykdomInnleggelsePeriode> innleggelser = sykdomVurderingService.hentInnleggelser(behandling).getPerioder();
        final LocalDateTimeline<Boolean> innleggelseTimeline = new LocalDateTimeline<Boolean>(
                innleggelser.stream().map(i -> new LocalDateSegment<Boolean>(i.getFom(), i.getTom(), Boolean.TRUE)).collect(Collectors.toList())
                );
        return innleggelseTimeline;
    }

    private boolean harDenneSaken(BehandlingReferanse ref, List<Kravprioritet> kravprioritetsliste) {
        return kravprioritetsliste.stream().anyMatch(k -> k.getSaksnummer().equals(ref.getSaksnummer()));
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

    private boolean harIkkeAndreprioritet(BehandlingReferanse ref, List<Kravprioritet> kravprioritetsliste) {
        return !kravprioritetsliste.get(1).getSaksnummer().equals(ref.getSaksnummer());
    }

    private boolean manglerInnleggelseIPeriodeMedOverlapp(LocalDateTimeline<List<Kravprioritet>> perioderMedOverlapp,
            LocalDateTimeline<Boolean> innleggelseTimeline) {
        return !TidslinjeUtil.kunPerioderSomIkkeFinnesI(perioderMedOverlapp, innleggelseTimeline).isEmpty();
    }

    private boolean erIkkeSøkerMedAndreprioritetPåBarnetIPerioderMedInnleggelse(BehandlingReferanse ref,
            LocalDateTimeline<List<Kravprioritet>> perioderMedOverlapp,
            LocalDateTimeline<Boolean> innleggelseTimeline) {

        return !perioderMedOverlapp.intersection(innleggelseTimeline)
                .filterValue(kravprioritetsliste -> {
                    return harIkkeAndreprioritet(ref, kravprioritetsliste);
                }).isEmpty();
    }
}
