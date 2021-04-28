package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.kunPerioderSomIkkeFinnesI;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.toLocalDateTimeline;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.toPeriodeList;

import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomGrunnlagSammenlikningsresultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomSamletVurdering;

@Dependent
public class SykdomVurderingService {

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private UttakRepository uttakRepository;

    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;


    SykdomVurderingService() {
        // CDI
    }

    @Inject
    public SykdomVurderingService(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
            SykdomVurderingRepository sykdomVurderingRepository, SykdomDokumentRepository sykdomDokumentRepository, UttakRepository uttakRepository,
            SykdomGrunnlagRepository sykdomGrunnlagRepository) {
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
        this.uttakRepository = uttakRepository;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
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

        final boolean harDataSomIkkeHarBlittTattMedIBehandling = harDataSomIkkeHarBlittTattMedIBehandling(behandling, pleietrengende);

        return new SykdomAksjonspunkt(harUklassifiserteDokumenter, manglerDiagnosekode, manglerGodkjentLegeerklæring, manglerVurderingAvKontinuerligTilsynOgPleie, manglerVurderingAvToOmsorgspersoner, harDataSomIkkeHarBlittTattMedIBehandling);
    }

    private boolean manglerGodkjentLegeerklæring(final AktørId pleietrengende) {
        return !sykdomDokumentRepository.hentAlleDokumenterFor(pleietrengende).stream().anyMatch(d -> d.getType() == SykdomDokumentType.LEGEERKLÆRING_SYKEHUS);
    }

    public SykdomVurderingerOgPerioder hentVurderingerForKontinuerligTilsynOgPleie(Behandling behandling) {
        return utledPerioder(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, behandling);
    }

    public SykdomVurderingerOgPerioder hentVurderingerForToOmsorgspersoner(Behandling behandling) {
        return utledPerioder(SykdomVurderingType.TO_OMSORGSPERSONER, behandling);
    }

    private boolean harDataSomIkkeHarBlittTattMedIBehandling(Behandling behandling, final AktørId pleietrengende) {
        SykdomGrunnlagSammenlikningsresultat sykdomGrunnlagSammenlikningsresultat = utledRelevanteEndringerSidenForrigeGrunnlag(behandling.getFagsak().getSaksnummer(), behandling.getUuid(), pleietrengende);
        return sykdomGrunnlagSammenlikningsresultat.harBlittEndret();
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

    @SuppressWarnings("unchecked")
    public SykdomVurderingerOgPerioder utledPerioder(SykdomVurderingType sykdomVurderingType, Behandling behandling) {
        final LocalDateTimeline<SykdomVurderingVersjon> vurderinger = hentVurderinger(sykdomVurderingType, behandling);
        final LocalDateTimeline<Set<Saksnummer>> behandledeSøknadsperioder = sykdomVurderingRepository.hentSaksnummerForSøktePerioder(behandling.getFagsak().getPleietrengendeAktørId());

        final var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandling);
        final var perioderTilVurderingUnder18 = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        final var perioderTilVurdering18 = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.MEDISINSKEVILKÅR_18_ÅR);

        final NavigableSet<DatoIntervallEntitet> perioderTilVurdering = union(perioderTilVurderingUnder18, perioderTilVurdering18);
        final List<Periode> nyeSøknadsperioder = Collections.emptyList(); // TODO;nyeSøknadsperioder
        final List<Periode> alleSøknadsperioder = behandledeSøknadsperioder.stream().map(s -> new Periode(s.getFom(), s.getTom())).collect(Collectors.toList());
        final List<Periode> innleggelsesperioder = hentInnleggelsesperioder(behandling);

        LocalDateTimeline<Boolean> alleResterendeVurderingsperioder = finnResterendeVurderingsperioder(perioderTilVurdering, vurderinger);
        if (manglerGodkjentLegeerklæring(behandling.getFagsak().getPleietrengendeAktørId())) {
            alleResterendeVurderingsperioder = LocalDateTimeline.EMPTY_TIMELINE;
        }

        alleResterendeVurderingsperioder = kunPerioderSomIkkeFinnesI(alleResterendeVurderingsperioder, toLocalDateTimeline(innleggelsesperioder));

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
                innleggelsesperioder
            );
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

    private LocalDateTimeline<Boolean> finnResterendeVurderingsperioder(NavigableSet<DatoIntervallEntitet> vurderingsperioder, LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje) {
        return kunPerioderSomIkkeFinnesI(toLocalDateTimeline(vurderingsperioder), vurderingerTidslinje);
    }

    public SykdomGrunnlagSammenlikningsresultat utledRelevanteEndringerSidenForrigeGrunnlag(
            final Saksnummer saksnummer,
            final UUID behandlingUuid,
            final AktørId pleietrengende) {
        final Optional<SykdomGrunnlagBehandling> grunnlagBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(behandlingUuid);
        final SykdomGrunnlag utledetGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(saksnummer, behandlingUuid, pleietrengende, List.of());

        return sammenlignGrunnlag(grunnlagBehandling.map(SykdomGrunnlagBehandling::getGrunnlag), utledetGrunnlag);
    }

    public SykdomGrunnlagSammenlikningsresultat utledRelevanteEndringerSidenForrigeBehandling(
            final Saksnummer saksnummer,
            final UUID behandlingUuid,
            final AktørId pleietrengende,
            List<Periode> nyeVurderingsperioder) {
        final Optional<SykdomGrunnlagBehandling> forrigeGrunnlagBehandling = sykdomGrunnlagRepository.hentGrunnlagFraForrigeBehandling(saksnummer, behandlingUuid);
        final SykdomGrunnlag utledetGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(saksnummer, behandlingUuid, pleietrengende, nyeVurderingsperioder);

        return sammenlignGrunnlag(forrigeGrunnlagBehandling.map(SykdomGrunnlagBehandling::getGrunnlag), utledetGrunnlag);
    }

    public SykdomGrunnlagSammenlikningsresultat sammenlignGrunnlag(Optional<SykdomGrunnlag> forrigeGrunnlagBehandling, SykdomGrunnlag utledetGrunnlag) {
        boolean harEndretDiagnosekoder = sammenlignDiagnosekoder(forrigeGrunnlagBehandling, utledetGrunnlag);
        final LocalDateTimeline<Boolean> endringerISøktePerioder = sammenlignTidfestedeGrunnlagsdata(forrigeGrunnlagBehandling, utledetGrunnlag);
        return new SykdomGrunnlagSammenlikningsresultat(endringerISøktePerioder, harEndretDiagnosekoder);
    }

    LocalDateTimeline<Boolean> sammenlignTidfestedeGrunnlagsdata(Optional<SykdomGrunnlag> grunnlagBehandling, SykdomGrunnlag utledetGrunnlag) {
        LocalDateTimeline<SykdomSamletVurdering> grunnlagBehandlingTidslinje;

        if (grunnlagBehandling.isPresent()) {
            final SykdomGrunnlag forrigeGrunnlag = grunnlagBehandling.get();
            grunnlagBehandlingTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(forrigeGrunnlag);
        } else {
            grunnlagBehandlingTidslinje = LocalDateTimeline.EMPTY_TIMELINE;
        }
        final LocalDateTimeline<SykdomSamletVurdering> forrigeGrunnlagTidslinje = grunnlagBehandlingTidslinje;

        final LocalDateTimeline<SykdomSamletVurdering> nyBehandlingTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(utledetGrunnlag);
        final LocalDateTimeline<Boolean> endringerSidenForrigeBehandling = SykdomSamletVurdering.finnGrunnlagsforskjeller(forrigeGrunnlagTidslinje, nyBehandlingTidslinje);

        final LocalDateTimeline<Boolean> søktePerioderTimeline = SykdomUtils.toLocalDateTimeline(utledetGrunnlag.getSøktePerioder().stream().map(p -> new Periode(p.getFom(), p.getTom())).collect(Collectors.toList()));
        return endringerSidenForrigeBehandling.intersection(søktePerioderTimeline);
    }

    private boolean sammenlignDiagnosekoder(Optional<SykdomGrunnlag> grunnlagBehandling, SykdomGrunnlag utledetGrunnlag) {
        List<String> forrigeDiagnosekoder;
        if (grunnlagBehandling.isPresent()) {
            final SykdomGrunnlag forrigeGrunnlag = grunnlagBehandling.get();
            forrigeDiagnosekoder = forrigeGrunnlag.getSammenlignbarDiagnoseliste();
        } else {
            forrigeDiagnosekoder = Collections.emptyList();
        }
        final List<String> nyeDiagnosekoder = utledetGrunnlag.getSammenlignbarDiagnoseliste();

        return !forrigeDiagnosekoder.equals(nyeDiagnosekoder);
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
