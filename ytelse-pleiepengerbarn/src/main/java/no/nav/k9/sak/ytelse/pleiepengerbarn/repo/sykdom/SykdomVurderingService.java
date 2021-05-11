package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.kunPerioderSomIkkeFinnesI;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.toLocalDateTimeline;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.toPeriodeList;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class SykdomVurderingService {

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;


    SykdomVurderingService() {
        // CDI
    }

    @Inject
    public SykdomVurderingService(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
            SykdomVurderingRepository sykdomVurderingRepository, SykdomDokumentRepository sykdomDokumentRepository,
            SykdomGrunnlagService sykdomGrunnlagService, BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    
    public SykdomAksjonspunkt vurderAksjonspunkt(Behandling behandling) {
        // XXX: Denne er kastet sammen og bør trolig skrives enklere
        final AktørId pleietrengende = behandling.getFagsak().getPleietrengendeAktørId();

        final boolean harUklassifiserteDokumenter = sykdomDokumentRepository.hentAlleDokumenterFor(pleietrengende).stream().anyMatch(d -> d.getType() == SykdomDokumentType.UKLASSIFISERT);
        final boolean manglerGodkjentLegeerklæring = manglerGodkjentLegeerklæring(pleietrengende);
        final boolean manglerDiagnosekode = sykdomDokumentRepository.hentDiagnosekoder(pleietrengende).getDiagnosekoder().isEmpty();

        final var ktp = hentVurderingerForKontinuerligTilsynOgPleie(behandling);
        final boolean manglerVurderingAvKontinuerligTilsynOgPleie = !ktp.getResterendeVurderingsperioder().isEmpty();

        final var too = hentVurderingerForToOmsorgspersoner(behandling);
        final boolean manglerVurderingAvToOmsorgspersoner = !too.getResterendeVurderingsperioder().isEmpty();

        final boolean harDataSomIkkeHarBlittTattMedIBehandling = sykdomGrunnlagService.harDataSomIkkeHarBlittTattMedIBehandling(behandling);

        return new SykdomAksjonspunkt(harUklassifiserteDokumenter, manglerDiagnosekode, manglerGodkjentLegeerklæring, manglerVurderingAvKontinuerligTilsynOgPleie, manglerVurderingAvToOmsorgspersoner, harDataSomIkkeHarBlittTattMedIBehandling);
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

    private LocalDateTimeline<Boolean> hentInnleggelseUnder18årTidslinje(Behandling behandling) {
        final var innleggelser = hentInnleggelser(behandling);

        final LocalDateTimeline<Boolean> innleggelsesperioderTidslinje = new LocalDateTimeline<Boolean>(innleggelser.getPerioder()
                .stream()
                .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), Boolean.TRUE))
                .collect(Collectors.toList()));
        final LocalDate pleietrengendesFødselsdato = finnPleietrengendesFødselsdato(behandling);
        return innleggelsesperioderTidslinje.intersection(new LocalDateInterval(null, pleietrengendesFødselsdato.plusYears(18).minusDays(1))).compress();
    }

    private List<Periode> hentKontinuerligTilsynOgPleiePerioder(Behandling behandling) {
        final LocalDateTimeline<SykdomVurderingVersjon> vurderinger = hentVurderinger(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, behandling);
        return vurderinger.stream()
                .filter(s -> s.getValue().getResultat() == Resultat.OPPFYLT)
                .map(s -> new Periode(s.getFom(), s.getTom()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public SykdomVurderingerOgPerioder utledPerioder(SykdomVurderingType sykdomVurderingType, Behandling behandling) {
        final LocalDateTimeline<SykdomVurderingVersjon> vurderinger = hentVurderinger(sykdomVurderingType, behandling);
        final LocalDateTimeline<Set<Saksnummer>> behandledeSøknadsperioder = sykdomVurderingRepository.hentSaksnummerForSøktePerioder(behandling.getFagsak().getPleietrengendeAktørId());

        final LocalDateTimeline<Boolean> perioderTilVurdering = utledPerioderTilVurderingUtenOmsorgenFor(behandling);
        final List<Periode> nyeSøknadsperioder = Collections.emptyList(); // TODO;nyeSøknadsperioder
        final List<Periode> alleSøknadsperioder = behandledeSøknadsperioder.stream().map(s -> new Periode(s.getFom(), s.getTom())).collect(Collectors.toList());
        final LocalDateTimeline<Boolean> innleggelseUnder18årTidslinje = hentInnleggelseUnder18årTidslinje(behandling);

        LocalDateTimeline<Boolean> alleResterendeVurderingsperioder = finnResterendeVurderingsperioder(perioderTilVurdering, vurderinger);
        if (manglerGodkjentLegeerklæring(behandling.getFagsak().getPleietrengendeAktørId())) {
            alleResterendeVurderingsperioder = LocalDateTimeline.EMPTY_TIMELINE;
        }

        alleResterendeVurderingsperioder = kunPerioderSomIkkeFinnesI(alleResterendeVurderingsperioder, innleggelseUnder18årTidslinje);

        final List<Periode> resterendeVurderingsperioder;
        final List<Periode> resterendeValgfrieVurderingsperioder;
        if (sykdomVurderingType == SykdomVurderingType.TO_OMSORGSPERSONER) {
            // Kun vurder perioder for TO_OMSORGSPERSONER hvis det ligger en KTP-vurdering i bunn:
            alleResterendeVurderingsperioder = alleResterendeVurderingsperioder.intersection(toLocalDateTimeline(hentKontinuerligTilsynOgPleiePerioder(behandling)));

            final LocalDateTimeline<?> flereOmsorgspersoner = harAndreSakerEnn(behandling.getFagsak().getSaksnummer(), behandledeSøknadsperioder);
            final LocalDateTimeline<Boolean> resterendeVurderingsperioderTidslinje = alleResterendeVurderingsperioder.intersection(flereOmsorgspersoner);
            resterendeVurderingsperioder = toPeriodeList(
                resterendeVurderingsperioderTidslinje
            );
            resterendeValgfrieVurderingsperioder = toPeriodeList(
                kunPerioderSomIkkeFinnesI(alleResterendeVurderingsperioder, resterendeVurderingsperioderTidslinje)
            );
        } else {
            resterendeVurderingsperioder = toPeriodeList(alleResterendeVurderingsperioder);
            resterendeValgfrieVurderingsperioder = List.of();
        }

        return new SykdomVurderingerOgPerioder(
                vurderinger,
                behandledeSøknadsperioder,
                alleSøknadsperioder,
                resterendeVurderingsperioder,
                resterendeValgfrieVurderingsperioder,
                nyeSøknadsperioder,
                SykdomUtils.toPeriodeList(innleggelseUnder18årTidslinje)
            );
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

    private LocalDateTimeline<Boolean> utledPerioderTilVurderingUtenOmsorgenFor(Behandling behandling) {
        final var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandling);
        final var perioderTilVurderingUnder18 = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        final var perioderTilVurdering18 = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        final NavigableSet<DatoIntervallEntitet> perioderTilVurdering = union(perioderTilVurderingUnder18, perioderTilVurdering18);
        final LocalDateTimeline<Boolean> perioderTilVurderingTidslinje = new LocalDateTimeline<Boolean>(perioderTilVurdering.stream()
                .map(p -> new LocalDateSegment<Boolean>(p.getFomDato(), p.getTomDato(), Boolean.TRUE))
                .collect(Collectors.toList()));  
        final LocalDateTimeline<VilkårPeriode> omsorgenForTidslinje = sykdomGrunnlagService.hentOmsorgenForTidslinje(behandling.getId()).filterValue(vp -> vp.getUtfall() == Utfall.IKKE_OPPFYLT);
        
        return kunPerioderSomIkkeFinnesI(perioderTilVurderingTidslinje, omsorgenForTidslinje);
    }

    private static <T> NavigableSet<T> union(NavigableSet<T> s1, NavigableSet<T> s2)  {
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

    private LocalDateTimeline<Boolean> finnResterendeVurderingsperioder(LocalDateTimeline<Boolean> vurderingsperioder, LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje) {
        return kunPerioderSomIkkeFinnesI(vurderingsperioder, vurderingerTidslinje);
    }

    public static class SykdomVurderingerOgPerioder {
        private final LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje;
        private final LocalDateTimeline<Set<Saksnummer>> saksnummerForPerioder;
        private final List<Periode> søknadsperioder;
        private final List<Periode> nyeSøknadsperioder;
        private List<Periode> innleggelsesperioder;
        private List<Periode> resterendeVurderingsperioder;
        private List<Periode> resterendeValgfrieVurderingsperioder;


        public SykdomVurderingerOgPerioder(LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje,
                LocalDateTimeline<Set<Saksnummer>> saksnummerForPerioder, List<Periode> søknadsperioder,
                List<Periode> resterendeVurderingsperioder, List<Periode> resterendeValgfrieVurderingsperioder,
                List<Periode> nyeSøknadsperioder, List<Periode> innleggelsesperioder) {
            this.vurderingerTidslinje = vurderingerTidslinje;
            this.saksnummerForPerioder = saksnummerForPerioder;
            this.søknadsperioder = søknadsperioder;
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
            return Collections.unmodifiableList(søknadsperioder);
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
