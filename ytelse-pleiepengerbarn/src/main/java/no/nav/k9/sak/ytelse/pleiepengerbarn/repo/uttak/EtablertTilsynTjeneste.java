package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

@Dependent
public class EtablertTilsynTjeneste  {

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private Instance<VurderSøknadsfristTjeneste<?>> søknadsfristTjenester;

    EtablertTilsynTjeneste() {
        // CDI
    }

    @Inject
    public EtablertTilsynTjeneste(FagsakRepository fagsakRepository,
            BehandlingRepository behandlingRepository,
            UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
            @Any Instance<VurderSøknadsfristTjeneste<?>> søknadsfristTjenester) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.søknadsfristTjenester = søknadsfristTjenester;
    }
    
    
    /*
    public List<SøktUttak> beregnTilsynstidlinjeForPerioder(AktørId pleietrengende, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        return beregnTilsynstidlinje(pleietrengende)
                .intersection(tidslinjeTilVurdering)
                .toSegments()
                .stream()
                .map(it -> new SøktUttak(new LukketPeriode(it.getFom(), it.getTom()), it.getValue().getPeriode().getTimerPleieAvBarnetPerDag()))
                .collect(Collectors.toList());
    }
    */
    
    public LocalDateTimeline<UtledetEtablertTilsyn> beregnTilsynstidlinje(AktørId pleietrengende) {
        final var tilsynsgrunnlagPåTversAvFagsaker = hentAllePerioderTilVurdering(pleietrengende);
        return byggTidslinje(tilsynsgrunnlagPåTversAvFagsaker);
    }
    
    
    private TilsynsgrunnlagPåTversAvFagsaker hentAllePerioderTilVurdering(AktørId pleietrengende) {
        final List<Fagsak> fagsaker = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, pleietrengende, null, null, null);
        
        final var kravDokumenter = new HashSet<KravDokument>();
        final var perioderFraSøknader = new HashSet<PerioderFraSøknad>();
        for (Fagsak f : fagsaker) {
            final Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(f.getId());
            if (behandlingOpt.isEmpty()) {
                continue;
            }
            final var behandling = behandlingOpt.get();
            final var behandlingReferanse = BehandlingReferanse.fra(behandling);
            
            final var uttakGrunnlagOpt = uttakPerioderGrunnlagRepository.hentGrunnlag(behandling.getId());
            if (uttakGrunnlagOpt.isEmpty() || uttakGrunnlagOpt.get().getOppgitteSøknadsperioder() == null) {
                continue;
            }
            
            @SuppressWarnings("unchecked")
            final Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> fagsakKravdokumenter = finnVurderSøknadsfristTjeneste(behandlingReferanse).hentPerioderTilVurdering(behandlingReferanse);
            kravDokumenter.addAll(fagsakKravdokumenter.keySet());
            
            final Set<PerioderFraSøknad> fagsakPerioderFraSøknadene = uttakGrunnlagOpt.get().getOppgitteSøknadsperioder().getPerioderFraSøknadene();
            perioderFraSøknader.addAll(fagsakPerioderFraSøknadene);
        }
        
        return new TilsynsgrunnlagPåTversAvFagsaker(kravDokumenter, perioderFraSøknader);
    }
    
    private LocalDateTimeline<UtledetEtablertTilsyn> byggTidslinje(TilsynsgrunnlagPåTversAvFagsaker tilsynsgrunnlagPåTversAvFagsaker) {
        var kravDokumenterSorted = tilsynsgrunnlagPåTversAvFagsaker.kravDokumenter.stream().sorted(KravDokument::compareTo).collect(Collectors.toCollection(LinkedHashSet::new));
        var resultatTimeline = new LocalDateTimeline<UtledetEtablertTilsyn>(List.of());
        for (KravDokument kravDokument : kravDokumenterSorted) {
            var dokumenter = tilsynsgrunnlagPåTversAvFagsaker.perioderFraSøknader.stream()
                .filter(it -> it.getJournalpostId().equals(kravDokument.getJournalpostId()))
                .collect(Collectors.toSet());
            if (dokumenter.size() == 1) {
                var perioderFraSøknad = dokumenter.iterator().next();
                for (var periode : perioderFraSøknad.getTilsynsordning().stream().map(Tilsynsordning::getPerioder).flatMap(Collection::stream).collect(Collectors.toList())) {
                    var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), new UtledetEtablertTilsyn(periode.getVarighet()))));
                    resultatTimeline = resultatTimeline.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
            } else {
                throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + dokumenter);
            }
        }
        return resultatTimeline.compress();
    }
    
    @SuppressWarnings("rawtypes")
    private VurderSøknadsfristTjeneste finnVurderSøknadsfristTjeneste(BehandlingReferanse ref) {
        FagsakYtelseType ytelseType = ref.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(søknadsfristTjenester, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + VurderSøknadsfristTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    private static final class TilsynsgrunnlagPåTversAvFagsaker {
        private final Set<KravDokument> kravDokumenter;
        private final Set<PerioderFraSøknad> perioderFraSøknader;
        
        public TilsynsgrunnlagPåTversAvFagsaker(Set<KravDokument> kravDokumenter, Set<PerioderFraSøknad> perioderFraSøknader) {
            this.kravDokumenter = kravDokumenter;
            this.perioderFraSøknader = perioderFraSøknader;
        }
    }
}
