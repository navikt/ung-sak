package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
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
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode;

@Dependent
public class SykdomVurderingService {

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;


    SykdomVurderingService() {
        // CDI
    }

    @Inject
    public SykdomVurderingService(
            @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
            SykdomVurderingRepository sykdomVurderingRepository,
            SykdomDokumentRepository sykdomDokumentRepository,
            SykdomGrunnlagService sykdomGrunnlagService,
            BasisPersonopplysningTjeneste personopplysningTjeneste,
            SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
    }


    public SykdomAksjonspunkt vurderAksjonspunkt(Behandling behandling) {
        // XXX: Denne er kastet sammen og bør trolig skrives enklere
        final AktørId pleietrengende = behandling.getFagsak().getPleietrengendeAktørId();

        if (behandling.getStatus().erFerdigbehandletStatus()) {
            return SykdomAksjonspunkt.bareFalse();
        }

        final boolean harUklassifiserteDokumenter = sykdomDokumentRepository.hentAlleDokumenterFor(pleietrengende).stream().anyMatch(d -> d.getType() == SykdomDokumentType.UKLASSIFISERT);
        boolean dokumenterUtenUtkvittering = !sykdomDokumentRepository.hentDokumentSomIkkeHarOppdatertEksisterendeVurderinger(pleietrengende).isEmpty();

        final boolean manglerGodkjentLegeerklæring = manglerGodkjentLegeerklæring(pleietrengende);

        final boolean eksisterendeVurderinger = !sykdomVurderingRepository.hentSisteVurderingerFor(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, pleietrengende).isEmpty();
        final boolean nyttDokumentHarIkkekontrollertEksisterendeVurderinger = dokumenterUtenUtkvittering && eksisterendeVurderinger;

        final boolean harDataSomIkkeHarBlittTattMedIBehandling = sykdomGrunnlagService.harDataSomIkkeHarBlittTattMedIBehandling(behandling);

        boolean manglerDiagnosekode;
        boolean manglerVurderingAvKontinuerligTilsynOgPleie;
        boolean manglerVurderingAvToOmsorgspersoner;
        boolean manglerVurderingAvILivetsSluttfase;

        switch (behandling.getFagsakYtelseType()) {
            case PLEIEPENGER_SYKT_BARN -> {
                if (!utledPerioderTilVurderingMedOmsorgenFor(behandling).isEmpty()) {
                    manglerDiagnosekode = sykdomDokumentRepository.hentDiagnosekoder(pleietrengende).getDiagnosekoder().isEmpty();
                } else {
                    manglerDiagnosekode = false;
                }
                manglerVurderingAvKontinuerligTilsynOgPleie = !hentVurderingerForKontinuerligTilsynOgPleie(behandling).getResterendeVurderingsperioder().isEmpty();
                manglerVurderingAvToOmsorgspersoner = !hentVurderingerForToOmsorgspersoner(behandling).getResterendeVurderingsperioder().isEmpty();
                manglerVurderingAvILivetsSluttfase = false;

                boolean harUbesluttedeVurderinger = harUbesluttedeVurderinger(behandling);

                if (!manglerDiagnosekode && !harUbesluttedeVurderinger && !harUklassifiserteDokumenter && !dokumenterUtenUtkvittering && !manglerVurderingAvKontinuerligTilsynOgPleie && !manglerVurderingAvToOmsorgspersoner) {
                    return SykdomAksjonspunkt.bareFalse();
                }
            }
            case PLEIEPENGER_NÆRSTÅENDE -> {
                manglerDiagnosekode = false;
                manglerVurderingAvToOmsorgspersoner = false;
                manglerVurderingAvKontinuerligTilsynOgPleie = false;
                manglerVurderingAvILivetsSluttfase = !hentVurderingerForILivetsSluttfase(behandling).getResterendeVurderingsperioder().isEmpty();
            }
            default -> throw new IllegalArgumentException("Ikke-støttet ytelstype: " + behandling.getFagsakYtelseType());
        }

        return new SykdomAksjonspunkt(
            harUklassifiserteDokumenter,
            manglerDiagnosekode,
            manglerGodkjentLegeerklæring,
            manglerVurderingAvKontinuerligTilsynOgPleie,
            manglerVurderingAvToOmsorgspersoner,
            manglerVurderingAvILivetsSluttfase,
            harDataSomIkkeHarBlittTattMedIBehandling,
            nyttDokumentHarIkkekontrollertEksisterendeVurderinger);
    }

    private boolean harUbesluttedeVurderinger(Behandling behandling) {
        SykdomGrunnlagBehandling sykdomGrunnlagBehandling = sykdomGrunnlagService.hentGrunnlag(behandling.getUuid());

        boolean harUbesluttet = sykdomGrunnlagBehandling.getGrunnlag().getVurderinger()
            .stream()
            .anyMatch(v -> !v.isBesluttet());
        return harUbesluttet;
    }

    private boolean manglerGodkjentLegeerklæring(final AktørId pleietrengende) {
        return sykdomDokumentRepository.hentGodkjenteLegeerklæringer(pleietrengende).isEmpty();
    }

    public SykdomVurderingerOgPerioder hentVurderingerForKontinuerligTilsynOgPleie(Behandling behandling) {
        return utledPerioder(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, behandling);
    }

    public SykdomVurderingerOgPerioder hentVurderingerForToOmsorgspersoner(Behandling behandling) {
        return utledPerioder(SykdomVurderingType.TO_OMSORGSPERSONER, behandling);
    }

    public SykdomVurderingerOgPerioder hentVurderingerForILivetsSluttfase(Behandling behandling) {
        return utledPerioderPPN(behandling);
    }

    private LocalDateTimeline<Boolean> hentInnleggelseUnder18årTidslinje(Behandling behandling) {
        final var innleggelser = hentInnleggelser(behandling);

        final LocalDateTimeline<Boolean> innleggelsesperioderTidslinje = new LocalDateTimeline<Boolean>(innleggelser.getPerioder()
            .stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), Boolean.TRUE))
            .toList());
        final LocalDate pleietrengendesFødselsdato = finnPleietrengendesFødselsdato(behandling);
        return innleggelsesperioderTidslinje.intersection(new LocalDateInterval(null, pleietrengendesFødselsdato.plusYears(PleietrengendeAlderPeriode.ALDER_FOR_STRENGERE_PSB_VURDERING).minusDays(1))).compress();
    }

    private LocalDateTimeline<Boolean> hentAlleInnleggelserTidslinje(Behandling behandling) {
        final var innleggelser = hentInnleggelser(behandling);

        return new LocalDateTimeline<Boolean>(innleggelser.getPerioder()
            .stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), Boolean.TRUE))
            .toList())
            .compress();
    }

    private List<Periode> hentKontinuerligTilsynOgPleiePerioder(Behandling behandling) {
        final LocalDateTimeline<SykdomVurderingVersjon> vurderinger = hentVurderinger(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, behandling);
        return vurderinger.stream()
            .filter(s -> s.getValue().getResultat() == Resultat.OPPFYLT)
            .map(s -> new Periode(s.getFom(), s.getTom()))
            .toList();
    }

    public SykdomVurderingerOgPerioder utledPerioderPPN(Behandling behandling) {
        LocalDateTimeline<SykdomVurderingVersjon> vurderinger = hentVurderinger(SykdomVurderingType.LIVETS_SLUTTFASE, behandling);
        LocalDateTimeline<Set<Saksnummer>> behandledeSøknadsperioder = sykdomVurderingRepository.hentSaksnummerForSøktePerioder(behandling.getFagsak().getPleietrengendeAktørId());

        List<Periode> perioderKreverVurdering = behandledeSøknadsperioder.stream().map(s->new Periode(s.getFom(), s.getTom())).toList();
        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandling);
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.I_LIVETS_SLUTTFASE);
        LocalDateTimeline<Boolean> tidslinjeKreverVurdering = new LocalDateTimeline<>(perioderTilVurdering.stream().map(p -> new LocalDateSegment<>(p.toLocalDateInterval(), true)).toList());

        LocalDateTimeline<Boolean> innleggelserTidslinje = hentAlleInnleggelserTidslinje(behandling);
        LocalDateTimeline<Boolean> alleResterendeVurderingsperioder = finnResterendeVurderingsperioder(behandling, tidslinjeKreverVurdering, vurderinger);
        if (manglerGodkjentLegeerklæring(behandling.getFagsak().getPleietrengendeAktørId())) {
            alleResterendeVurderingsperioder = LocalDateTimeline.empty();
        }

        //ta høyde for perioder trukket av søker(e)
        LocalDateTimeline<List<AktørId>> søknadsperioderForAlleSøkere = søknadsperiodeTjeneste.utledSamledePerioderMedSøkereFor(FagsakYtelseType.PPN, behandling.getFagsak().getPleietrengendeAktørId());
        LocalDateTimeline<List<AktørId>> søknadsperioderForInneværendeBehandling = new LocalDateTimeline<>(søknadsperioderForAlleSøkere.stream().filter(s -> s.getValue().contains(behandling.getAktørId())).collect(Collectors.toList()));

        List<Periode> resterendeVurderingsperioder = TidslinjeUtil.tilPerioder(alleResterendeVurderingsperioder.intersection(søknadsperioderForInneværendeBehandling));

        List<Periode> resterendeValgfrieVurderingsperioder =
            TidslinjeUtil.tilPerioder(
            tidslinjeKreverVurdering
                .disjoint(alleResterendeVurderingsperioder)
                .disjoint(vurderinger)
                .intersection(søknadsperioderForAlleSøkere)
                .compress());
        List<Periode> nyeSøknadsperioder = Collections.emptyList();

        return new SykdomVurderingerOgPerioder(
            vurderinger,
            behandledeSøknadsperioder,
            perioderKreverVurdering,
            resterendeVurderingsperioder,
            resterendeValgfrieVurderingsperioder,
            nyeSøknadsperioder,
                TidslinjeUtil.tilPerioder(innleggelserTidslinje)
        );
    }

    public SykdomVurderingerOgPerioder utledPerioder(SykdomVurderingType sykdomVurderingType, Behandling behandling) {
        final Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        final LocalDateTimeline<SykdomVurderingVersjon> eksisterendeVurderinger = hentVurderinger(sykdomVurderingType, behandling);
        //TODO: Dette er "søknadsperioder" fra sykdomsgrunnlaget. Det skjærer litt med søknadsperiodene vi nå henter rett fra kilden, hvor vi filtrere på trukkede perioder. Bedre navn?
        // Er det evt nå redundant, pga direkte henting fra søknadsperioder, og kan/bør fjernes?
        final LocalDateTimeline<Set<Saksnummer>> søknadsperioderPåPleietrengende = sykdomVurderingRepository.hentSaksnummerForSøktePerioder(behandling.getFagsak().getPleietrengendeAktørId());
        final LocalDateTimeline<Boolean> søknadsperioderTilSøker = søknadsperioderPåPleietrengende.filterValue(s -> s.contains(saksnummer)).mapValue(s -> Boolean.TRUE);

        final LocalDateTimeline<Boolean> innleggelseUnder18årTidslinje = hentInnleggelseUnder18årTidslinje(behandling);
        final LocalDateTimeline<Boolean> manglerGodkjentLegeerklæringTidslinje = utledManglerGodkjentLegeerklæringTidslinje(behandling.getFagsak().getPleietrengendeAktørId());
        final LocalDateTimeline<VilkårPeriode> utenOmsorgenForTidslinje = sykdomGrunnlagService.hentManglendeOmsorgenForTidslinje(behandling.getId());

        LocalDateTimeline<Boolean> alleResterendeVurderingsperioder = TidslinjeUtil.toBooleanTimeline(søknadsperioderPåPleietrengende)
                .disjoint(eksisterendeVurderinger)
                .disjoint(innleggelseUnder18årTidslinje)
                .disjoint(manglerGodkjentLegeerklæringTidslinje);

        LocalDateTimeline<Boolean> resterendeVurderingsperioder;

        //ta høyde for perioder trukket av søker(e)
        LocalDateTimeline<List<AktørId>> søknadsperioderForAlleSøkere = søknadsperiodeTjeneste.utledSamledePerioderMedSøkereFor(FagsakYtelseType.PSB, behandling.getFagsak().getPleietrengendeAktørId());
        LocalDateTimeline<List<AktørId>> søknadsperioderForInneværendeBehandling = new LocalDateTimeline<>(søknadsperioderForAlleSøkere.stream().filter(s -> s.getValue().contains(behandling.getAktørId())).collect(Collectors.toList()));

        if (sykdomVurderingType.equals(SykdomVurderingType.TO_OMSORGSPERSONER)) {
            /*
             * To omsorgspersoner skal kun vurderes for oppfylte periode med kontinuerlig tilsyn og pleie.
             *
             * I tillegg er to omsorgspersoner kun obligatorisk å vurdere for perioder med flere søkere.
             */
            LocalDateTimeline<Boolean> ktpPerioder = TidslinjeUtil.tilTidslinjeKomprimert(hentKontinuerligTilsynOgPleiePerioder(behandling));
            alleResterendeVurderingsperioder = alleResterendeVurderingsperioder.intersection(ktpPerioder);
            resterendeVurderingsperioder = alleResterendeVurderingsperioder.disjoint(utenOmsorgenForTidslinje)
                    .intersection(søknadsperioderForAlleSøkere)
                    .intersection(søknadsperioderTilSøker);
        } else {
            resterendeVurderingsperioder = alleResterendeVurderingsperioder.disjoint(utenOmsorgenForTidslinje)
                    .intersection(søknadsperioderTilSøker)
                    .intersection(søknadsperioderForInneværendeBehandling);
        }

        final LocalDateTimeline<Boolean> resterendeValgfrieVurderingsperioder = alleResterendeVurderingsperioder
            .disjoint(resterendeVurderingsperioder)
            .intersection(søknadsperioderForAlleSøkere);

        /*
         * Denne er ikke satt siden brukerdialog ikke lenger benytter denne verdien. Hvis den
         * skal benyttes må feltet utledes. Hvis den over lengre tid ikke er interessant bør
         * den fjernes fra kontrakten. 2022-04-05
         */
        final List<Periode> nyeSøknadsperioder = Collections.emptyList();

        return new SykdomVurderingerOgPerioder(
            eksisterendeVurderinger,
            søknadsperioderPåPleietrengende,
                TidslinjeUtil.tilPerioder(søknadsperioderPåPleietrengende),
                TidslinjeUtil.tilPerioder(resterendeVurderingsperioder),
                TidslinjeUtil.tilPerioder(resterendeValgfrieVurderingsperioder),
            nyeSøknadsperioder,
                TidslinjeUtil.tilPerioder(innleggelseUnder18årTidslinje)
        );
    }

    private LocalDateTimeline<Boolean> utledManglerGodkjentLegeerklæringTidslinje(AktørId pleietrengende) {
        if (manglerGodkjentLegeerklæring(pleietrengende)) {
            return new LocalDateTimeline<Boolean>(LocalDate.MIN, LocalDate.MAX, true);
        }

        return LocalDateTimeline.empty();
    }

    public LocalDate finnPleietrengendesFødselsdato(Behandling behandling) {
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(
            behandling.getId(),
            behandling.getFagsak().getAktørId(),
            behandling.getFagsak().getPeriode().getFomDato()
        );
        var pleietrengendePersonopplysning = personopplysningerAggregat.getPersonopplysning(behandling.getFagsak().getPleietrengendeAktørId());
        return pleietrengendePersonopplysning.getFødselsdato();
    }

    private LocalDateTimeline<Boolean> utledPerioderTilVurderingMedOmsorgenFor(Behandling behandling) {
        final var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandling);
        final var perioderTilVurderingUnder18 = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        final var perioderTilVurdering18 = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        final NavigableSet<DatoIntervallEntitet> perioderTilVurdering = union(perioderTilVurderingUnder18, perioderTilVurdering18);
        final LocalDateTimeline<Boolean> perioderTilVurderingTidslinje = new LocalDateTimeline<Boolean>(perioderTilVurdering.stream()
            .map(p -> new LocalDateSegment<Boolean>(p.getFomDato(), p.getTomDato(), Boolean.TRUE))
            .collect(Collectors.toList()));
        final LocalDateTimeline<VilkårPeriode> omsorgenForTidslinje = sykdomGrunnlagService.hentOmsorgenForTidslinje(behandling.getId()).filterValue(vp -> vp.getUtfall() == Utfall.IKKE_OPPFYLT);

        return TidslinjeUtil.kunPerioderSomIkkeFinnesI(perioderTilVurderingTidslinje, omsorgenForTidslinje);
    }

    private static <T> NavigableSet<T> union(NavigableSet<T> s1, NavigableSet<T> s2) {
        final var resultat = new TreeSet<>(s1);
        resultat.addAll(s2);
        return resultat;
    }

    private LocalDateTimeline<Set<Saksnummer>> harAndreSakerEnn(Saksnummer saksnummer,
                                                                final LocalDateTimeline<Set<Saksnummer>> behandledeSøknadsperioder) {
        return behandledeSøknadsperioder.filterValue(
            s -> s.size() > 1 || (s.size() == 1 && !s.contains(saksnummer))
        );
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

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }

    private LocalDateTimeline<Boolean> finnResterendeVurderingsperioder(Behandling behandling, LocalDateTimeline<Boolean> vurderingsperioder, LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje) {
        if (manglerGodkjentLegeerklæring(behandling.getFagsak().getPleietrengendeAktørId())) {
            return LocalDateTimeline.EMPTY_TIMELINE;
        }
        return TidslinjeUtil.kunPerioderSomIkkeFinnesI(vurderingsperioder, vurderingerTidslinje);
    }

    public static class SykdomVurderingerOgPerioder {
        /**
         * Alle gjeldende vurderinger i en tidslinje.
         */
        private final LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje;

        /**
         * Tidslinje over periodene til alle sakene som er tilknyttet den pleietrengende.
         */
        private final LocalDateTimeline<Set<Saksnummer>> saksnummerForPerioder;

        /**
         * Perioder som saksbehandler skal få lov til å gjøre vurderinger innenfor. Dette
         * er nå satt til alle søknadsperioder som finnes på barnet (på tvers av søkere).
         */
        private final List<Periode> perioderSomKanVurderes;

        /**
         * Nye søknadsperioder på søker som skal håndteres av saksbehandler i denne behandlingen.
         */
        private final List<Periode> nyeSøknadsperioder;

        /**
         * Alle perioder på pleietrengende med innleggelse.
         */
        private List<Periode> innleggelsesperioder;

        /**
         * Perioder som er obligatoriske å vurdere før behandlingen kommer videre.
         */
        private List<Periode> resterendeVurderingsperioder;

        /**
         * Perioder som vises som forslag til saksbehandler for vurdering, men som ikke
         * er påkrevd å utføre.
         */
        private List<Periode> resterendeValgfrieVurderingsperioder;


        public SykdomVurderingerOgPerioder(LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje,
                                           LocalDateTimeline<Set<Saksnummer>> saksnummerForPerioder,
                                           List<Periode> perioderSomKanVurderes,
                                           List<Periode> resterendeVurderingsperioder,
                                           List<Periode> resterendeValgfrieVurderingsperioder,
                                           List<Periode> nyeSøknadsperioder,
                                           List<Periode> innleggelsesperioder) {
            this.vurderingerTidslinje = vurderingerTidslinje;
            this.saksnummerForPerioder = saksnummerForPerioder;
            this.perioderSomKanVurderes = perioderSomKanVurderes;
            this.resterendeVurderingsperioder = resterendeVurderingsperioder;
            this.resterendeValgfrieVurderingsperioder = resterendeValgfrieVurderingsperioder;
            this.nyeSøknadsperioder = nyeSøknadsperioder;
            this.innleggelsesperioder = innleggelsesperioder;
        }


        public LocalDateTimeline<SykdomVurderingVersjon> getVurderingerTidslinje() {
            return vurderingerTidslinje;
        }

        public LocalDateTimeline<Set<Saksnummer>> getSaksnummerForPerioder() {
            return saksnummerForPerioder;
        }

        public List<Periode> getPerioderSomKanVurderes() {
            return Collections.unmodifiableList(perioderSomKanVurderes);
        }

        public List<Periode> getResterendeVurderingsperioder() {
            return Collections.unmodifiableList(resterendeVurderingsperioder);
        }

        public List<Periode> getResterendeValgfrieVurderingsperioder() {
            return Collections.unmodifiableList(resterendeValgfrieVurderingsperioder);
        }

        public List<Periode> getNyeSøknadsperioder() {
            return Collections.unmodifiableList(nyeSøknadsperioder);
        }

        public List<Periode> getInnleggelsesperioder() {
            return innleggelsesperioder;
        }

        void fjernAlleResterendeVurderingsperioder() {
            this.resterendeVurderingsperioder = List.of();
        }
    }

}
