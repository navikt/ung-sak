package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.MapTilBrevkode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død.HåndterePleietrengendeDødsfallTjeneste;

@Dependent
public class SøknadsperiodeTjeneste {

    private final BehandlingRepository behandlingRepository;
    private final SøknadsperiodeRepository søknadsperiodeRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Instance<MapTilBrevkode> brevkodeMappere;
    private HåndterePleietrengendeDødsfallTjeneste håndterePleietrengendeDødsfallTjeneste;

    @Inject
    public SøknadsperiodeTjeneste(BehandlingRepository behandlingRepository,
                                  SøknadsperiodeRepository søknadsperiodeRepository,
                                  MottatteDokumentRepository mottatteDokumentRepository,
                                  @Any Instance<MapTilBrevkode> brevkodeMappere,
                                  HåndterePleietrengendeDødsfallTjeneste håndterePleietrengendeDødsfallTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.brevkodeMappere = brevkodeMappere;
        this.håndterePleietrengendeDødsfallTjeneste = håndterePleietrengendeDødsfallTjeneste;
    }

    public LocalDateTimeline<List<AktørId>> utledSamledePerioderMedSøkereFor(FagsakYtelseType ytelseType, AktørId pleietrengende) {
        final List<Fagsak> fagsaker = fagsakRepository.finnFagsakRelatertTil(ytelseType, pleietrengende, null, null, null);
        LocalDateTimeline<List<AktørId>> samletTimelineForAlleSøkere = LocalDateTimeline.empty();
        for (Fagsak fagsak : fagsaker) {
            var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
            if (behandling.isEmpty()) {
                continue;
            }
            NavigableSet<DatoIntervallEntitet> datoIntervallEntitets = utledFullstendigPeriode(behandling.get().getId());
            LocalDateTimeline<AktørId> timelineForSøker = new LocalDateTimeline<>(datoIntervallEntitets.stream()
                .map(e -> new LocalDateSegment<>(e.toLocalDateInterval(), fagsak.getAktørId()))
                .toList());
            samletTimelineForAlleSøkere = samletTimelineForAlleSøkere.union(timelineForSøker, StandardCombinators::allValues);
        }

        return samletTimelineForAlleSøkere;
    }

    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId)
            .map(SøknadsperiodeGrunnlag::getRelevantSøknadsperioder);

        if (søknadsperioder.isEmpty() || søknadsperioder.get().getPerioder().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            final var søknadsperioders = søknadsperioder.get().getPerioder();
            final var behandling = behandlingRepository.hentBehandling(behandlingId);

            return utledVurderingsperioderFraSøknadsperioder(BehandlingReferanse.fra(behandling), søknadsperioders);
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

            return utledVurderingsperioderFraSøknadsperioder(BehandlingReferanse.fra(behandling), søknadsperioders);
        }
    }

    public NavigableSet<DatoIntervallEntitet> utledVurderingsperioderFraSøknadsperioder(BehandlingReferanse referanse, Set<Søknadsperioder> søknadsperioders) {
        return hentKravperioder(referanse, søknadsperioders)
            .stream()
            .filter(kp -> !kp.isHarTrukketKrav())
            .map(Kravperiode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public List<Kravperiode> hentKravperioder(BehandlingReferanse referanse) {
        final List<Søknadsperioder> søknadsperioders = søknadsperiodeRepository.hentGrunnlag(referanse.getBehandlingId())
            .stream()
            .map(SøknadsperiodeGrunnlag::getOppgitteSøknadsperioder)
            .map(SøknadsperioderHolder::getPerioder)
            .flatMap(Collection::stream)
            .toList();

        return hentKravperioder(referanse, søknadsperioders);
    }

    public List<Kravperiode> hentKravperioder(BehandlingReferanse referanse, Collection<Søknadsperioder> søknadsperioders) {
        var brevkode = MapTilBrevkode.finnBrevkodeMapper(brevkodeMappere, referanse.getFagsakYtelseType()).getBrevkode();
        final List<MottattDokument> mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(referanse.getFagsakId())
            .stream()
            .filter(it -> brevkode.equals(it.getType()))
            .filter(it -> DokumentStatus.GYLDIG.equals(it.getStatus()))
            .sorted(Comparator.comparing(MottattDokument::getInnsendingstidspunkt))
            .toList();

        LocalDateTimeline<Kravperiode> tidslinje = LocalDateTimeline.empty();
        for (MottattDokument kd : mottatteDokumenter) {
            var segments = søknadsperioders.stream()
                .filter(sp -> sp.getJournalpostId().equals(kd.getJournalpostId()))
                .map(sp -> sp.getPerioder().stream().map(p -> new Kravperiode(p.getPeriode(), kd.getBehandlingId(), p.isHarTrukketKrav())).toList())
                .flatMap(Collection::stream)
                .map(p -> new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p))
                .toList();
            tidslinje = tidslinje.union(new LocalDateTimeline<>(segments), StandardCombinators::coalesceRightHandSide);
        }

        var utvidetPeriode = håndterePleietrengendeDødsfallTjeneste.utledUtvidetPeriodeForDødsfall(referanse);
        if (utvidetPeriode.isPresent()) {
            var periode = utvidetPeriode.get();
            tidslinje = tidslinje.union(new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.toLocalDateInterval(), new Kravperiode(periode, referanse.getBehandlingId(), false)))), StandardCombinators::coalesceRightHandSide);
        }

        return tidslinje.stream().map(s -> new Kravperiode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue().getBehandlingId(), s.getValue().isHarTrukketKrav())).toList();
    }

    public static class Kravperiode {
        private final DatoIntervallEntitet periode;
        private final Long behandlingId;
        private final boolean harTrukketKrav;

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
