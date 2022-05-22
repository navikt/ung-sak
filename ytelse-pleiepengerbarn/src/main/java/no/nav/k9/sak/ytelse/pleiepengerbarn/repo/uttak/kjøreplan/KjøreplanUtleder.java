package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriode;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.utsatt.UtsattPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelsePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

@Dependent
public class KjøreplanUtleder {
    private final FagsakRepository fagsakRepository;
    private final BehandlingRepository behandlingRepository;

    private final MottatteDokumentRepository mottatteDokumentRepository;

    private final Instance<VurderSøknadsfristTjeneste<Søknadsperiode>> søknadsfristTjenester;
    private final SykdomVurderingService sykdomVurderingService;
    private final UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;

    @Inject
    public KjøreplanUtleder(FagsakRepository fagsakRepository,
                            BehandlingRepository behandlingRepository,
                            MottatteDokumentRepository mottatteDokumentRepository,
                            @Any Instance<VurderSøknadsfristTjeneste<Søknadsperiode>> søknadsfristTjenester,
                            SykdomVurderingService sykdomVurderingService,
                            UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadsfristTjenester = søknadsfristTjenester;
        this.sykdomVurderingService = sykdomVurderingService;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
    }

    public Kjøreplan utled(BehandlingReferanse referanse) {
        var søknadsfristTjeneste = VurderSøknadsfristTjeneste.finnSøknadsfristTjeneste(søknadsfristTjenester, referanse.getFagsakYtelseType());
        var aktuellFagsak = fagsakRepository.finnEksaktFagsak(referanse.getFagsakId());
        final List<SakOgBehandlinger> fagsaker = fagsakRepository.finnFagsakRelatertTil(aktuellFagsak.getYtelseType(), aktuellFagsak.getPleietrengendeAktørId(), null, null, null)
            .stream()
            .map(it -> mapTilSakOgBehandling(it, søknadsfristTjeneste))
            .toList();

        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());

        var innleggelseTimeline = hentInnleggelseTimeline(behandling);
        var utsattePerioderPerBehandling = utledUtsattePerioderFraBehandling(fagsaker);

        var input = new KravPrioInput(aktuellFagsak.getId(), aktuellFagsak.getSaksnummer(), utsattePerioderPerBehandling, innleggelseTimeline, fagsaker);

        return utledKravprioInternt(input);
    }

    private HashMap<Long, NavigableSet<DatoIntervallEntitet>> utledUtsattePerioderFraBehandling(List<SakOgBehandlinger> fagsaker) {
        var behandlinger = fagsaker
            .stream()
            .map(SakOgBehandlinger::getBehandlinger)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        HashMap<Long, NavigableSet<DatoIntervallEntitet>> utsattePerioderPerBehandling = new HashMap<>();

        for (Long behandlingId : behandlinger) {
            var utsattePerioder = utsattBehandlingAvPeriodeRepository.hentGrunnlag(behandlingId)
                .map(UtsattBehandlingAvPeriode::getPerioder)
                .orElse(Set.of())
                .stream()
                .map(UtsattPeriode::getPeriode)
                .collect(Collectors.toCollection(TreeSet::new));

            utsattePerioderPerBehandling.put(behandlingId, utsattePerioder);
        }
        return utsattePerioderPerBehandling;
    }

    Kjøreplan utledKravprioInternt(KravPrioInput input) {
        LocalDateTimeline<List<InternalKravprioritet>> kravprioritetPåEndringerOgVedtakstatus = utledInternKravprio(input.getSakOgBehandlinger());

        var kravprioForEldsteKrav = utledKravprioForEldsteKrav(kravprioritetPåEndringerOgVedtakstatus);

        var kjøreplansTidslinje = utledKjøreplan(kravprioritetPåEndringerOgVedtakstatus, kravprioForEldsteKrav, input);


        return new Kjøreplan(input.getAktuellFagsakId(), input.getAktuellSak(), kjøreplansTidslinje, kravprioForEldsteKrav);
    }

    private LocalDateTimeline<Set<AksjonPerFagsak>> utledKjøreplan(LocalDateTimeline<List<InternalKravprioritet>> kravprioritetPåEndringerOgVedtakstatus,
                                                                   LocalDateTimeline<List<InternalKravprioritet>> kravprioForEldsteKrav,
                                                                   KravPrioInput input) {
        List<LocalDateSegment<Set<AksjonPerFagsak>>> segmenter = new ArrayList<>();

        Map<Long, Boolean> trengerÅUtsettePerioder = utledBehovForUtsettelse(input, kravprioritetPåEndringerOgVedtakstatus);

        for (LocalDateSegment<List<InternalKravprioritet>> segment : kravprioForEldsteKrav) {
            var prio = new HashSet<AksjonPerFagsak>();
            var kravprioritetPåEndringerOgVedtakstatusSegment = kravprioritetPåEndringerOgVedtakstatus.getSegment(segment.getLocalDateInterval());
            for (InternalKravprioritet internalKravprioritet : segment.getValue()) {
                var sakOgBehandlinger = input.getSakOgBehandlinger().stream().filter(it -> Objects.equals(it.getFagsak(), internalKravprioritet.getFagsak())).findFirst().orElseThrow();
                var harUbehandledeKrav = kravprioritetPåEndringerOgVedtakstatusSegment.getValue().stream().anyMatch(it -> Objects.equals(it.getFagsak(), internalKravprioritet.getFagsak()) && erUbehandlet(it, sakOgBehandlinger, DatoIntervallEntitet.fra(segment.getLocalDateInterval()), input.getUtsattePerioderPerBehandling()));
                if (prio.stream().anyMatch(it -> Objects.equals(it.getFagsakId(), internalKravprioritet.getFagsak()))) {
                    continue;
                }
                if (!harUbehandledeKrav) {
                    // DO NOTHING
                } else if (prio.isEmpty()) {
                    prio.add(new AksjonPerFagsak(internalKravprioritet.getFagsak(), Aksjon.BEHANDLE));
                } else if (trengerÅUtsettePerioder.get(internalKravprioritet.getFagsak())) {
                    prio.add(new AksjonPerFagsak(internalKravprioritet.getFagsak(), Aksjon.UTSETT));
                } else if (!trengerÅUtsettePerioder.get(internalKravprioritet.getFagsak())) {
                    prio.add(new AksjonPerFagsak(internalKravprioritet.getFagsak(), Aksjon.VENTE_PÅ_ANNEN));
                }
            }
            if (!prio.isEmpty()) {
                segmenter.add(new LocalDateSegment<>(segment.getLocalDateInterval(), prio));
            }
        }

        return new LocalDateTimeline<>(segmenter);
    }

    private boolean erUbehandlet(InternalKravprioritet it, SakOgBehandlinger sakOgBehandlinger, DatoIntervallEntitet periode, Map<Long, NavigableSet<DatoIntervallEntitet>> utsattePerioderPerBehandling) {
        if (it.erUbehandlet()) {
            return true;
        }
        return harVærtUtsattOgIkkeHattVedtakkSiden(sakOgBehandlinger, periode, utsattePerioderPerBehandling, it.getAktuellBehandling()) && it.erVedtatt();
    }

    private boolean harVærtUtsattOgIkkeHattVedtakkSiden(SakOgBehandlinger sakOgBehandlinger, DatoIntervallEntitet periode, Map<Long, NavigableSet<DatoIntervallEntitet>> utsattePerioderPerBehandling, Long aktuellBehandling) {
        // Antar at utsatte perioder blir behandlet i neste behandling, hvis premisset faller så må det sjekkes om den har blitt utsatt i påfølgende behandling også
        return utsattePerioderPerBehandling.getOrDefault(aktuellBehandling, new TreeSet<>()).stream().anyMatch(p -> p.overlapper(periode))
            && harIkkeHattVedtakSiden(sakOgBehandlinger, periode, utsattePerioderPerBehandling, aktuellBehandling);
    }

    private Boolean harIkkeHattVedtakSiden(SakOgBehandlinger sakOgBehandlinger, DatoIntervallEntitet periode, Map<Long, NavigableSet<DatoIntervallEntitet>> utsattePerioderPerBehandling, Long aktuellBehandling) {
        var etterfølgendeBehandling = sakOgBehandlinger.getEtterfølgendeBehandling(aktuellBehandling);
        if (etterfølgendeBehandling.isEmpty()) {
            return true;
        }
        var etterfølgendeBehandlingId = etterfølgendeBehandling.get();
        var vedtattBehandling = sakOgBehandlinger.getBehandlingStatus(etterfølgendeBehandlingId).erFerdigbehandletStatus();
        var erUtsatt = utsattePerioderPerBehandling.getOrDefault(etterfølgendeBehandlingId, new TreeSet<>()).stream().anyMatch(p -> p.overlapper(periode));

        if (!erUtsatt) {
            return vedtattBehandling;
        }

        return harIkkeHattVedtakSiden(sakOgBehandlinger, periode, utsattePerioderPerBehandling, etterfølgendeBehandlingId);
    }

    private Map<Long, Boolean> utledBehovForUtsettelse(KravPrioInput input, LocalDateTimeline<List<InternalKravprioritet>> kravprioritetPåEndringerOgVedtakstatus) {
        Map<Long, Boolean> resultat = new HashMap<>();

        var sakOgBehandlinger = input.getSakOgBehandlinger();
        var sorterteSaksnummer = sakOgBehandlinger.stream().map(SakOgBehandlinger::getSaksnummer).sorted().collect(Collectors.toCollection(ArrayList::new));

        for (SakOgBehandlinger sakOgBehandling : sakOgBehandlinger) {
            resultat.put(sakOgBehandling.getFagsak(), harPrioritetIEnPeriodeOgIkkeIenAnenn(sakOgBehandling.getFagsak(),
                kravprioritetPåEndringerOgVedtakstatus.toSegments(), input));
        }

        if (harFlereEnnEnSomSkalUtsettes(resultat)) {
            for (Saksnummer saksnummer : sorterteSaksnummer) {
                var relevantSak = sakOgBehandlinger.stream().filter(it -> Objects.equals(it.getSaksnummer(), saksnummer)).findFirst().orElseThrow();
                if (resultat.get(relevantSak.getFagsak())) {
                    resultat.put(relevantSak.getFagsak(), false); // Flipper en av saken for å redusere antall prev og sikre at denne VENTER istedenfor å utsette også
                    break;
                }
            }
        }

        return resultat;
    }

    private boolean harFlereEnnEnSomSkalUtsettes(Map<Long, Boolean> resultat) {
        return resultat.entrySet().stream().filter(Map.Entry::getValue).count() > 1;
    }

    private boolean harPrioritetIEnPeriodeOgIkkeIenAnenn(Long fagsakId, NavigableSet<LocalDateSegment<List<InternalKravprioritet>>> ikkeVedtattIkkeUtsatteKrav, KravPrioInput input) {
        boolean harPrioritetIPeriodeAndreHarKravPå = false;
        boolean harIkkePrioritetIPeriodeAndreHarKravPå = false;

        for (LocalDateSegment<List<InternalKravprioritet>> segment : ikkeVedtattIkkeUtsatteKrav) {
            var kravprioritetForPeriode = segment.getValue()
                .stream()
                .filter(it -> {
                    var sakOgBehandlinger = input.getSakOgBehandlinger().stream().filter(at -> Objects.equals(at.getFagsak(), it.getFagsak())).findFirst().orElseThrow();
                    return erUbehandlet(it, sakOgBehandlinger, DatoIntervallEntitet.fra(segment.getLocalDateInterval()), input.getUtsattePerioderPerBehandling());
                })
                .sorted()
                .toList();

            var unikeSakerMedKrav = kravprioritetForPeriode.stream().map(InternalKravprioritet::getFagsak).collect(Collectors.toSet());
            // TODO: Vurdere om det skal utvides med støtte for å kjøre videre ved innleggelse også (utlede antall felt på motorveien)
            if (unikeSakerMedKrav.size() < 2) {
                continue;
            }
            if (Objects.equals(kravprioritetForPeriode.get(0).getFagsak(), fagsakId)) {
                harPrioritetIPeriodeAndreHarKravPå = true;
            }
            if (!Objects.equals(kravprioritetForPeriode.get(0).getFagsak(), fagsakId)) {
                harIkkePrioritetIPeriodeAndreHarKravPå = true;
            }
        }
        return harPrioritetIPeriodeAndreHarKravPå && harIkkePrioritetIPeriodeAndreHarKravPå;
    }

    LocalDateTimeline<List<InternalKravprioritet>> utledInternKravprio(List<SakOgBehandlinger> sakOgBehandlinger) {
        LocalDateTimeline<List<InternalKravprioritet>> kravprioritetstidslinje = LocalDateTimeline.empty();
        for (SakOgBehandlinger sakOgBehandling : sakOgBehandlinger) {
            final LocalDateTimeline<List<InternalKravprioritet>> fagsakTidslinje = finnKravTidslinjeForFagsak(sakOgBehandling);
            kravprioritetstidslinje = kravprioritetstidslinje.union(fagsakTidslinje, this::mergeKravPåTversAvSaker);
        }
        return kravprioritetstidslinje;
    }

    private SakOgBehandlinger mapTilSakOgBehandling(Fagsak fagsak, VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId()).stream().filter(Behandling::erYtelseBehandling).toList();
        var behandlingerMedStatus = new HashMap<Long, BehandlingMedMetadata>();

        for (Behandling behandling1 : behandlinger) {
            behandlingerMedStatus.put(behandling1.getId(), new BehandlingMedMetadata(behandling1.getStatus(), behandling1.getOriginalBehandlingId().orElse(null)));
        }

        var mottattDokumenter = behandling.map(it -> mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(fagsak.getId()).stream().map(at -> new MottattKrav(at.getJournalpostId(), at.getBehandlingId())).toList()).orElse(List.of());
        final Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravDokumenter = behandling.map(ba -> søknadsfristTjeneste.vurderSøknadsfrist(BehandlingReferanse.fra(ba))).orElse(Map.of());

        return new SakOgBehandlinger(fagsak.getId(), fagsak.getSaksnummer(), behandling.map(Behandling::getId).orElse(null), behandlingerMedStatus, mottattDokumenter, kravDokumenter);
    }

    private LocalDateSegment<List<InternalKravprioritet>> mergeKravPåTversAvSaker(LocalDateInterval datoInterval, LocalDateSegment<List<InternalKravprioritet>> leftSegment, LocalDateSegment<List<InternalKravprioritet>> rightSegment) {
        if (leftSegment == null) {
            return new LocalDateSegment<>(datoInterval, rightSegment.getValue());
        }
        if (rightSegment == null) {
            return new LocalDateSegment<>(datoInterval, leftSegment.getValue());
        }
        var kravprioritet = new ArrayList<>(leftSegment.getValue());
        kravprioritet.addAll(rightSegment.getValue());
        Collections.sort(kravprioritet);

        return new LocalDateSegment<>(datoInterval, kravprioritet);
    }

    private LocalDateTimeline<List<InternalKravprioritet>> finnKravTidslinjeForFagsak(SakOgBehandlinger sakOgBehandling) {
        if (sakOgBehandling.getBehandling().isEmpty()) {
            return LocalDateTimeline.empty();
        }
        LocalDateTimeline<List<InternalKravprioritet>> fagsakTidslinje = LocalDateTimeline.empty();
        var mottattDokumenter = sakOgBehandling.getMottattDokumenter();
        final Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravDokumenter = sakOgBehandling.getKravDokumenter();
        for (Map.Entry<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravdokument : kravDokumenter.entrySet()) {
            final LocalDateTimeline<InternalKravprioritet> periodetidslinje = new LocalDateTimeline<>(kravdokument.getValue()
                .stream()
                .filter(vsp -> vsp.getUtfall() == no.nav.k9.kodeverk.vilkår.Utfall.OPPFYLT)
                .map(vsp -> new LocalDateSegment<>(vsp.getPeriode().toLocalDateInterval(), tilKravPrio(sakOgBehandling, mottattDokumenter, kravdokument)))
                .collect(Collectors.toList())
            );

            fagsakTidslinje = fagsakTidslinje.union(periodetidslinje, (datoInterval, leftSegment, rightSegment) -> {
                if (leftSegment == null) {
                    return new LocalDateSegment<>(datoInterval, new ArrayList<>(List.of(rightSegment.getValue())));
                }
                if (rightSegment == null) {
                    return new LocalDateSegment<>(datoInterval, leftSegment.getValue());
                }
                var kravprioritet = new ArrayList<>(leftSegment.getValue());
                kravprioritet.add(rightSegment.getValue());
                Collections.sort(kravprioritet);

                return new LocalDateSegment<>(datoInterval, kravprioritet);
            });
        }

        return fagsakTidslinje;
    }

    private LocalDateTimeline<Boolean> hentInnleggelseTimeline(Behandling behandling) {
        final List<SykdomInnleggelsePeriode> innleggelser = sykdomVurderingService.hentInnleggelser(behandling).getPerioder();
        return new LocalDateTimeline<>(innleggelser.stream()
            .map(i -> new LocalDateSegment<>(i.getFom(), i.getTom(), Boolean.TRUE))
            .collect(Collectors.toList()));
    }

    private InternalKravprioritet tilKravPrio(SakOgBehandlinger sakOgBehandling, List<MottattKrav> mottattDokumenter, Map.Entry<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravdokument) {
        var relevantBehandling = utledBehandling(mottattDokumenter, kravdokument);
        return new InternalKravprioritet(sakOgBehandling.getFagsak(), sakOgBehandling.getSaksnummer(), kravdokument.getKey().getJournalpostId(), relevantBehandling, sakOgBehandling.getBehandlingStatus(relevantBehandling), kravdokument.getKey().getInnsendingsTidspunkt());
    }

    private Long utledBehandling(List<MottattKrav> mottattDokumenter, Map.Entry<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravdokument) {
        return mottattDokumenter.stream().filter(it -> Objects.equals(it.getJournalpostId(), kravdokument.getKey().getJournalpostId())).findFirst().orElseThrow().getBehandlingId();
    }

    private LocalDateTimeline<List<InternalKravprioritet>> utledKravprioForEldsteKrav(LocalDateTimeline<List<InternalKravprioritet>> kravprioritet) {
        var segments = new ArrayList<LocalDateSegment<List<InternalKravprioritet>>>();

        for (LocalDateSegment<List<InternalKravprioritet>> segment : kravprioritet) {
            var resultat = new ArrayList<InternalKravprioritet>();
            var kravprio = segment.getValue();
            Collections.sort(kravprio);
            for (InternalKravprioritet kravprioritet1 : kravprio) {
                if (resultat.stream().noneMatch(it -> Objects.equals(it.getFagsak(), kravprioritet1.getFagsak()))) {
                    resultat.add(kravprioritet1);
                }
            }
            Collections.sort(resultat);
            segments.add(new LocalDateSegment<>(segment.getLocalDateInterval(), resultat));
        }

        return new LocalDateTimeline<>(segments);
    }
}
