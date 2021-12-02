package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class SøknadsperiodeTjeneste {

    private BehandlingRepository behandlingRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;


    @Inject
    public SøknadsperiodeTjeneste(BehandlingRepository behandlingRepository, SøknadsperiodeRepository søknadsperiodeRepository, MottatteDokumentRepository mottatteDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }


    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId).map(SøknadsperiodeGrunnlag::getRelevantSøknadsperioder);

        if (søknadsperioder.isEmpty() || søknadsperioder.get().getPerioder().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            final var søknadsperioders = søknadsperioder.get().getPerioder();
            final var behandling = behandlingRepository.hentBehandling(behandlingId);

            return utledVurderingsperioderFraSøknadsperioder(behandling.getFagsakId(), søknadsperioders);
        }
    }

    public NavigableSet<DatoIntervallEntitet> utledFullstendigPeriode(Long behandlingId) {
        var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId)
            .map(SøknadsperiodeGrunnlag::getOppgitteSøknadsperioder);

        if (søknadsperioder.isEmpty() || søknadsperioder.get().getPerioder().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            final var søknadsperioders = søknadsperioder.get().getPerioder();
            final var behandling = behandlingRepository.hentBehandling(behandlingId);

            return utledVurderingsperioderFraSøknadsperioder(behandling.getFagsakId(), søknadsperioders);
        }
    }

    public NavigableSet<DatoIntervallEntitet> utledVurderingsperioderFraSøknadsperioder(Long fagsakId, Set<Søknadsperioder> søknadsperioders) {
        return hentKravperioder(fagsakId, søknadsperioders)
            .stream()
            .filter(kp -> !kp.isHarTrukketKrav())
            .map(Kravperiode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public List<Kravperiode> hentKravperioder(Long fagsakId, Long behandlingId) {
        final List<Søknadsperioder> søknadsperioders = søknadsperiodeRepository.hentGrunnlag(behandlingId)
            .stream()
            .map(SøknadsperiodeGrunnlag::getOppgitteSøknadsperioder)
            .map(SøknadsperioderHolder::getPerioder)
            .flatMap(Collection::stream)
            .toList();

        return hentKravperioder(fagsakId, søknadsperioders);
    }

    public List<Kravperiode> hentKravperioder(Long fagsakId, Collection<Søknadsperioder> søknadsperioders) {
        final List<MottattDokument> mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(fagsakId)
            .stream()
            .filter(it -> Brevkode.PLEIEPENGER_BARN_SOKNAD.equals(it.getType()))
            .filter(it -> DokumentStatus.GYLDIG.equals(it.getStatus()))
            .sorted(Comparator.comparing(MottattDokument::getInnsendingstidspunkt))
            .toList();

        @SuppressWarnings("unchecked")
        LocalDateTimeline<Kravperiode> tidslinje = LocalDateTimeline.EMPTY_TIMELINE;
        for (MottattDokument kd : mottatteDokumenter) {
            var segments = søknadsperioders.stream()
                .filter(sp -> sp.getJournalpostId().equals(kd.getJournalpostId()))
                .map(sp -> sp.getPerioder().stream().map(p -> new Kravperiode(p.getPeriode(), kd.getBehandlingId(), p.isHarTrukketKrav())).toList())
                .flatMap(Collection::stream)
                .map(p -> new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p))
                .toList();
            tidslinje = tidslinje.union(new LocalDateTimeline<>(segments).compress(), StandardCombinators::coalesceRightHandSide);
        }

        return tidslinje.compress().stream().map(s -> new Kravperiode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue().getBehandlingId(), s.getValue().isHarTrukketKrav())).toList();
    }

    public static class Kravperiode {
        private DatoIntervallEntitet periode;
        private Long behandlingId;
        private boolean harTrukketKrav;

        public Kravperiode(DatoIntervallEntitet periode, Long behandlingId, boolean harTrukketKrav) {
            this.periode = periode;
            this.behandlingId = behandlingId;
            this.harTrukketKrav = harTrukketKrav;
        }

        public DatoIntervallEntitet getPeriode() {
            return periode;
        }

        public Long getBehandlingId() {
            return behandlingId;
        }

        public boolean isHarTrukketKrav() {
            return harTrukketKrav;
        }
    }
}
