package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.repo.søknadsperiode;

import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;


@Dependent
public class SøknadsperiodeTjeneste {

    private BehandlingRepository behandlingRepository;
    //private SøknadsperiodeRepository søknadsperiodeRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;


    @Inject
    public SøknadsperiodeTjeneste(BehandlingRepository behandlingRepository, MottatteDokumentRepository mottatteDokumentRepository, UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        //this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
    }


    // getRelevantSøknadsperioder() - relevant for denne behandlingen
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var perioderFraSøknader = uttakPerioderGrunnlagRepository.hentGrunnlag(behandlingId)
            .map(UttaksPerioderGrunnlag::getRelevantSøknadsperioder)
            .map(UttakPerioderHolder::getPerioderFraSøknadene)
            .orElse(Set.of());
        return perioderFraSøknader.stream()
            .flatMap(fraSøknad -> fraSøknad.getArbeidPerioder().stream())
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()))
            .collect(Collectors.toCollection(TreeSet::new));

        /*var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId).map(SøknadsperiodeGrunnlag::getRelevantSøknadsperioder);

        if (søknadsperioder.isEmpty() || søknadsperioder.get().getPerioder().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            final var søknadsperioders = søknadsperioder.get().getPerioder();
            final var behandling = behandlingRepository.hentBehandling(behandlingId);

            return utledVurderingsperioderFraSøknadsperioder(behandling.getFagsakId(), søknadsperioders);
        }*/
    }

    // getOppgitteSøknadsperioder() - alle oppgitt for hele fagsaken
    public NavigableSet<DatoIntervallEntitet> utledFullstendigPeriode(Long behandlingId) {
        var perioderFraSøknader = uttakPerioderGrunnlagRepository.hentGrunnlag(behandlingId)
            .map(UttaksPerioderGrunnlag::getOppgitteSøknadsperioder)
            .map(UttakPerioderHolder::getPerioderFraSøknadene)
            .orElse(Set.of());
        return perioderFraSøknader.stream()
            .flatMap(fraSøknad -> fraSøknad.getArbeidPerioder().stream())
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()))
            .collect(Collectors.toCollection(TreeSet::new));
        /*
        var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId)
            .map(SøknadsperiodeGrunnlag::getOppgitteSøknadsperioder);

        if (søknadsperioder.isEmpty() || søknadsperioder.get().getPerioder().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            final var søknadsperioders = søknadsperioder.get().getPerioder();
            final var behandling = behandlingRepository.hentBehandling(behandlingId);

            return utledVurderingsperioderFraSøknadsperioder(behandling.getFagsakId(), søknadsperioders);
        }*/
    }
/*
    public NavigableSet<DatoIntervallEntitet> utledVurderingsperioderFraSøknadsperioder(Long fagsakId, Set<Søknadsperioder> søknadsperioders) {
        return hentKravperioder(fagsakId, søknadsperioders)
                .stream()
                .filter(kp -> !kp.isHarTrukketKrav())
                .map(Kravperiode::getPeriode)
                .collect(Collectors.toCollection(TreeSet::new));
    }*/

    public List<Kravperiode> hentKravperioder(Long fagsakId, Long behandlingId) {
        var perioderFraSøknader = uttakPerioderGrunnlagRepository.hentGrunnlag(behandlingId)
            .map(UttaksPerioderGrunnlag::getOppgitteSøknadsperioder)
            .map(UttakPerioderHolder::getPerioderFraSøknadene)
            .orElse(Set.of());
        return perioderFraSøknader.stream()
            .flatMap(fraSøknad -> fraSøknad.getArbeidPerioder().stream())
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()))
            .map(it -> new Kravperiode(it, behandlingId, false))
            .collect(Collectors.toList());
        /*
        final List<Søknadsperioder> søknadsperioders = søknadsperiodeRepository.hentGrunnlag(behandlingId)
            .stream()
            .map(SøknadsperiodeGrunnlag::getOppgitteSøknadsperioder)
            .map(SøknadsperioderHolder::getPerioder)
            .flatMap(Collection::stream)
            .toList();

        return hentKravperioder(fagsakId, søknadsperioders);*/
    }

    /*public List<Kravperiode> hentKravperioder(Long fagsakId, Collection<Søknadsperioder> søknadsperioders) {
        final List<MottattDokument> mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(fagsakId)
                .stream()
                .filter(it -> Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE.equals(it.getType()))
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
    }*/

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
