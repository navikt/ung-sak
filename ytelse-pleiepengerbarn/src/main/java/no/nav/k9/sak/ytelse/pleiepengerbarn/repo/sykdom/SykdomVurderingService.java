package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.kunPerioderSomIkkeFinnesI;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.toLocalDateTimeline;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.toPeriodeList;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class SykdomVurderingService {

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private UttakRepository uttakRepository;

    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;
    
    
    SykdomVurderingService() {
        // CDI
    }

    @Inject
    public SykdomVurderingService(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
            SykdomVurderingRepository sykdomVurderingRepository, SykdomDokumentRepository sykdomDokumentRepository, UttakRepository uttakRepository) {
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
        this.uttakRepository = uttakRepository;
    }    
    
    
    public boolean harAksjonspunkt(Behandling behandling) {
        // XXX: Denne er kastet sammen og bør trolig skrives enklere
        final AktørId pleietrengende = behandling.getFagsak().getPleietrengendeAktørId();
        final boolean legeerklæring = sykdomDokumentRepository.hentAlleDokumenterFor(pleietrengende).stream().anyMatch(d -> d.getType() == SykdomDokumentType.LEGEERKLÆRING_SYKEHUS);
        if (!legeerklæring) {
            return true;
        }
        
        final boolean diagnosekode = !sykdomDokumentRepository.hentDiagnosekoder(pleietrengende).getDiagnosekoder().isEmpty();
        if (!diagnosekode) {
            return true;
        }
        
        final var ktp = hentVurderingerForKontinuerligTilsynOgPleie(behandling);
        if (!ktp.getResterendeVurderingsperioder().isEmpty()) {
            return true;
        }
        
        final var too = hentVurderingerForToOmsorgspersoner(behandling);
        if (!too.getResterendeVurderingsperioder().isEmpty()) {
            return true;
        }
        
        return false;
    }
    
    public SykdomVurderingerOgPerioder hentVurderingerForKontinuerligTilsynOgPleie(Behandling behandling) {
        final var vurderingerOgPerioder = utledPerioder(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, behandling);
        vurderingerOgPerioder.trekkFraResterendeVurderingsperioder(hentInnleggelsesperioder(behandling));
        
        return vurderingerOgPerioder;
    }
    
    public SykdomVurderingerOgPerioder hentVurderingerForToOmsorgspersoner(Behandling behandling) {
        final SykdomVurderingerOgPerioder vurderingerOgPerioder = utledPerioder(SykdomVurderingType.TO_OMSORGSPERSONER, behandling);        
        vurderingerOgPerioder.beholdKunResterendeVurderingsperioderSomFinnesI(hentKontinuerligTilsynOgPleiePerioder(behandling));
        vurderingerOgPerioder.trekkFraResterendeVurderingsperioder(hentInnleggelsesperioder(behandling));
        
        return vurderingerOgPerioder;
    }
    
    private List<Periode> hentInnleggelsesperioder(Behandling behandling) {
        final var innleggelser = hentInnleggelser(behandling);
        return innleggelser.getPerioder().stream().map(p -> new Periode(p.getFom(), p.getTom())).collect(Collectors.toList());
    }
    
    private List<Periode> hentKontinuerligTilsynOgPleiePerioder(Behandling behandling) {
        final LocalDateTimeline<SykdomVurderingVersjon> vurderinger = hentVurderinger(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, behandling);
        return vurderinger.stream()
                .filter(s -> s.getValue().getResultat() == Resultat.OPPFYLT)
                .map(s -> new Periode(s.getFom(), s.getTom()))
                .collect(Collectors.toList());
    }
    
    public SykdomVurderingerOgPerioder utledPerioder(SykdomVurderingType sykdomVurderingType, Behandling behandling) {
        final LocalDateTimeline<SykdomVurderingVersjon> vurderinger = hentVurderinger(sykdomVurderingType, behandling);
        
        final LocalDateTimeline<Set<Saksnummer>> behandledeSøknadsperioder = sykdomVurderingRepository.hentSaksnummerForSøktePerioder(behandling.getFagsak().getPleietrengendeAktørId());
        //final NavigableSet<DatoIntervallEntitet> søknadsperioderFraNyBehandling = getAlleSøknadsperioderFraNyBehandling(behandling.getUuid());
        
        final NavigableSet<DatoIntervallEntitet> perioderTilVurdering = getPerioderTilVurderingTjeneste(behandling).utled(behandling.getId(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        //final List<Periode> nyeSøknadsperioder = finnNyeSøknadsperioder(søknadsperioderFraNyBehandling, behandledeSøknadsperioder);
        //final LocalDateTimeline<Set<Saksnummer>> saksnummertidslinjeeMedNyePerioder = saksnummertidslinjeeMedNyePerioder(behandledeSøknadsperioder, søknadsperioderFraNyBehandling, behandling.getFagsak().getSaksnummer());
        final List<Periode> resterendeVurderingsperioder = finnResterendeVurderingsperioder(perioderTilVurdering, vurderinger);
        
        //final List<Periode> alleSøknadsperioder = saksnummertidslinjeeMedNyePerioder.stream().map(s -> new Periode(s.getFom(), s.getTom())).collect(Collectors.toList());
        //return new SykdomVurderingerOgPerioder(vurderinger, saksnummertidslinjeeMedNyePerioder, alleSøknadsperioder, resterendeVurderingsperioder, nyeSøknadsperioder);
        final List<Periode> nyeSøknadsperioder = Collections.emptyList(); // TODO;nyeSøknadsperioder
        final List<Periode> alleSøknadsperioder = behandledeSøknadsperioder.stream().map(s -> new Periode(s.getFom(), s.getTom())).collect(Collectors.toList());
        return new SykdomVurderingerOgPerioder(vurderinger, behandledeSøknadsperioder, alleSøknadsperioder, resterendeVurderingsperioder, nyeSøknadsperioder);
    }
    
    
    public SykdomInnleggelser hentInnleggelser(final Behandling behandling) {
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            return sykdomDokumentRepository.hentInnleggelse(behandling.getUuid());
        } else {
            return sykdomDokumentRepository.hentInnleggelse(behandling.getFagsak().getPleietrengendeAktørId());
        }
    }
    
    public LocalDateTimeline<SykdomVurderingVersjon> hentVurderinger(SykdomVurderingType sykdomVurderingType, final Behandling behandling) {
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            return sykdomVurderingRepository.getVurderingstidslinjeFor(sykdomVurderingType, behandling.getUuid());
        } else {
            return sykdomVurderingRepository.getSisteVurderingstidslinjeFor(sykdomVurderingType, behandling.getFagsak().getPleietrengendeAktørId());
        }
    }
    
    private NavigableSet<DatoIntervallEntitet> getAlleSøknadsperioderFraNyBehandling(UUID behandlingUuid) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingUuid);
        if (søknadsperioder.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(søknadsperioder.get().getMaksPeriode())));
        }
    }
    
    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }

    private List<Periode> finnResterendeVurderingsperioder(NavigableSet<DatoIntervallEntitet> vurderingsperioder, LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje) {
        return toPeriodeList(
                    kunPerioderSomIkkeFinnesI(toLocalDateTimeline(vurderingsperioder), vurderingerTidslinje)
                );
    }
    
    private List<Periode> finnNyeSøknadsperioder(NavigableSet<DatoIntervallEntitet> søknadsperioder, LocalDateTimeline<Set<Saksnummer>> saksnummerForPerioder) {
        return toPeriodeList(
                    kunPerioderSomIkkeFinnesI(toLocalDateTimeline(søknadsperioder), saksnummerForPerioder)
               );
    }
    
    private static LocalDateTimeline<Set<Saksnummer>> saksnummertidslinjeeMedNyePerioder(LocalDateTimeline<Set<Saksnummer>> saksnummerForPerioder, NavigableSet<DatoIntervallEntitet> nyeSøknadsperioder, Saksnummer saksnummer) {
        final LocalDateTimeline<Set<Saksnummer>> nyTidslinje = new LocalDateTimeline<Set<Saksnummer>>(nyeSøknadsperioder.stream().map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), Collections.singleton(saksnummer))).collect(Collectors.toList()));
        return saksnummerForPerioder.union(nyTidslinje, new LocalDateSegmentCombinator<Set<Saksnummer>, Set<Saksnummer>, Set<Saksnummer>>() {
            @Override
            public LocalDateSegment<Set<Saksnummer>> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<Set<Saksnummer>> datoSegment,
                    LocalDateSegment<Set<Saksnummer>> datoSegment2) {
                if (datoSegment == null) {
                    return new LocalDateSegment<Set<Saksnummer>>(datoInterval, datoSegment2.getValue());
                }
                if (datoSegment2 == null) {
                    return new LocalDateSegment<Set<Saksnummer>>(datoInterval, datoSegment.getValue());
                }
                
                final Set<Saksnummer> saksnumre = new HashSet<>(datoSegment.getValue());
                saksnumre.addAll(datoSegment2.getValue());
                return new LocalDateSegment<Set<Saksnummer>>(datoInterval, saksnumre);
            }
        });
    }

    public static class SykdomVurderingerOgPerioder {
        private final LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje;
        private final LocalDateTimeline<Set<Saksnummer>> saksnummerForPerioder;
        private final List<Periode> søknadsperioder;
        private final List<Periode> nyeSøknadsperioder;
        private List<Periode> resterendeVurderingsperioder;
        
        
        public SykdomVurderingerOgPerioder(LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje,
                LocalDateTimeline<Set<Saksnummer>> saksnummerForPerioder, List<Periode> søknadsperioder,
                List<Periode> resterendeVurderingsperioder, List<Periode> nyeSøknadsperioder) {
            this.vurderingerTidslinje = vurderingerTidslinje;
            this.saksnummerForPerioder = saksnummerForPerioder;
            this.søknadsperioder = søknadsperioder;
            this.resterendeVurderingsperioder = resterendeVurderingsperioder;
            this.nyeSøknadsperioder = nyeSøknadsperioder;
        }


        public LocalDateTimeline<SykdomVurderingVersjon> getVurderingerTidslinje() {
            return vurderingerTidslinje;
        }

        public LocalDateTimeline<Set<Saksnummer>> getSaksnummerForPerioder() {
            return saksnummerForPerioder;
        }

        public List<Periode> getPerioderSomKanVurderes() {
            return Collections.unmodifiableList(søknadsperioder);
        }

        public List<Periode> getResterendeVurderingsperioder() {
            return Collections.unmodifiableList(resterendeVurderingsperioder);
        }

        public List<Periode> getNyeSøknadsperioder() {
            return Collections.unmodifiableList(nyeSøknadsperioder);
        }
        
        void trekkFraResterendeVurderingsperioder(List<Periode> perioder) {
            var opprinneligTidslinje = toLocalDateTimeline(resterendeVurderingsperioder);
            var trekkFraTidslinje = toLocalDateTimeline(perioder);
            var nyTidslinje = kunPerioderSomIkkeFinnesI(opprinneligTidslinje, trekkFraTidslinje);
            
            this.resterendeVurderingsperioder = toPeriodeList(nyTidslinje);
        }
        
        void beholdKunResterendeVurderingsperioderSomFinnesI(List<Periode> perioder) {
            var opprinneligTidslinje = toLocalDateTimeline(resterendeVurderingsperioder);
            var intersectTidslinje = toLocalDateTimeline(perioder);
            var nyTidslinje = opprinneligTidslinje.intersection(intersectTidslinje);
            
            this.resterendeVurderingsperioder = toPeriodeList(nyTidslinje);
        }
    }
    
}
