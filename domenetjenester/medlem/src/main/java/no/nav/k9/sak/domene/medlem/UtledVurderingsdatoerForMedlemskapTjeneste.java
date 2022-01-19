package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.medlem.VurderingsÅrsak;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.medlem.impl.MedlemEndringssjekker;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.ForlengelseTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@Dependent
public class UtledVurderingsdatoerForMedlemskapTjeneste {

    private Instance<MedlemEndringssjekker> alleEndringssjekkere;
    private MedlemskapRepository medlemskapRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private Instance<ForlengelseTjeneste> forlengelseTjenester;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private Boolean enableForlengelse;
    private BehandlingRepository behandlingRepository;

    @Inject
    public UtledVurderingsdatoerForMedlemskapTjeneste(BehandlingRepository behandlingRepository,
                                                      MedlemskapRepository medlemskapRepository,
                                                      @Any Instance<MedlemEndringssjekker> alleEndringssjekkere,
                                                      PersonopplysningTjeneste personopplysningTjeneste,
                                                      @Any Instance<ForlengelseTjeneste> forlengelseTjenester,
                                                      @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                                      @KonfigVerdi(value = "forlengelse.medlemskap.enablet", defaultVerdi = "false") Boolean enableForlengelse) {
        this.behandlingRepository = behandlingRepository;
        this.alleEndringssjekkere = alleEndringssjekkere;
        this.medlemskapRepository = medlemskapRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.forlengelseTjenester = forlengelseTjenester;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.enableForlengelse = enableForlengelse;
    }

    UtledVurderingsdatoerForMedlemskapTjeneste() {
        // CDI
    }

    /**
     * Utleder vurderingsdatoer for:
     * - utledVurderingsdatoerForPersonopplysninger
     * - utledVurderingsdatoerForBortfallAvInntekt
     * - utledVurderingsdatoerForMedlemskap
     * <p>
     * Ser bare på datoer etter skjæringstidspunktet
     *
     * @param behandlingId id i databasen
     * @return datoer med diff i medlemskap
     */
    public Set<LocalDate> finnVurderingsdatoer(Long behandlingId) {
        return finnVurderingsdatoerMedForlengelse(behandlingId).getDatoerTilVurdering();
    }

    /**
     * Utleder vurderingsdatoer for:
     * - utledVurderingsdatoerForPersonopplysninger
     * - utledVurderingsdatoerForBortfallAvInntekt
     * - utledVurderingsdatoerForMedlemskap
     * <p>
     * Ser bare på datoer etter skjæringstidspunktet
     *
     * @param behandlingId id i databasen
     * @return datoer med diff i medlemskap
     */
    public Vurderingsdatoer finnVurderingsdatoerMedForlengelse(Long behandlingId) {

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var endringssjekker = FagsakYtelseTypeRef.Lookup.find(alleEndringssjekkere, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + behandling.getFagsakYtelseType().getKode()));
        var ref = BehandlingReferanse.fra(behandling);
        var perioderTilVurdering = utledPerioderTilVurdering(ref);

        var tidligsteStp = perioderTilVurdering.getTidligsteDatoTilVurdering();

        if (tidligsteStp.isEmpty()) {
            return new Vurderingsdatoer();
        }

        Set<LocalDate> datoer = new HashSet<>();

        for (DatoIntervallEntitet vilkårsPeriode : perioderTilVurdering.getPerioderTilVurdering()) {
            datoer.add(vilkårsPeriode.getFomDato());
            datoer.addAll(utledVurderingsdatoerForTPS(behandling, vilkårsPeriode).keySet());
        }
        for (DatoIntervallEntitet vilkårsPeriode : perioderTilVurdering.getForlengelseTilVurdering()) {
            datoer.addAll(utledVurderingsdatoerForTPS(behandling, vilkårsPeriode).keySet());
        }
        datoer.addAll(utledVurderingsdatoerForMedlemskap(behandlingId, endringssjekker).keySet());

        // ønsker bare å se på datoer etter skjæringstidspunktet
        return new Vurderingsdatoer(datoer.stream()
            .filter(entry -> entry.isAfter(tidligsteStp.get().minusDays(1)))
            .sorted().collect(Collectors.toCollection(TreeSet::new)), perioderTilVurdering.getForlengelseTilVurdering());
    }

    private PerioderTilVurdering utledPerioderTilVurdering(BehandlingReferanse referanse) {
        var vilkårsPerioder = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, referanse.getFagsakYtelseType(), referanse.getBehandlingType())
            .utled(referanse.getBehandlingId(), VilkårType.MEDLEMSKAPSVILKÅRET);
        NavigableSet<DatoIntervallEntitet> forlengelsePerioder;
        if (enableForlengelse) {
            var forlengelseTjeneste = ForlengelseTjeneste.finnTjeneste(forlengelseTjenester, referanse.getFagsakYtelseType(), referanse.getBehandlingType());
            forlengelsePerioder = forlengelseTjeneste.utledPerioderSomSkalBehandlesSomForlengelse(referanse, vilkårsPerioder, VilkårType.MEDLEMSKAPSVILKÅRET);
        } else {
            forlengelsePerioder = new TreeSet<>();
        }

        return new PerioderTilVurdering(vilkårsPerioder, forlengelsePerioder);
    }

    Map<LocalDate, Set<VurderingsÅrsak>> finnVurderingsdatoerMedÅrsak(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var endringssjekker = FagsakYtelseTypeRef.Lookup.find(alleEndringssjekkere, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + behandling.getFagsakYtelseType().getKode()));
        Map<LocalDate, Set<VurderingsÅrsak>> datoer = new HashMap<>();

        var ref = BehandlingReferanse.fra(behandling);
        var perioderTilVurdering = utledPerioderTilVurdering(ref);

        var tidligsteStp = perioderTilVurdering.getTidligsteDatoTilVurdering();

        if (tidligsteStp.isEmpty()) {
            return Map.of();
        }

        for (DatoIntervallEntitet periode : perioderTilVurdering.getPerioderTilVurdering()) {
            mergeResultat(datoer, Map.of(periode.getFomDato(), Set.of(VurderingsÅrsak.SKJÆRINGSTIDSPUNKT)));
            mergeResultat(datoer, utledVurderingsdatoerForTPS(behandling, periode));
        }
        for (DatoIntervallEntitet periode : perioderTilVurdering.getForlengelseTilVurdering()) {
            mergeResultat(datoer, utledVurderingsdatoerForTPS(behandling, periode));
        }
        mergeResultat(datoer, utledVurderingsdatoerForMedlemskap(behandlingId, endringssjekker));

        return datoer.entrySet().stream()
            .filter(entry -> entry.getKey().isAfter(tidligsteStp.get().minusDays(1)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<LocalDate, Set<VurderingsÅrsak>> utledVurderingsdatoerForTPS(Behandling revurdering, DatoIntervallEntitet relevantPeriode) {
        final Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat = new HashMap<>();

        Optional<PersonopplysningerAggregat> personopplysningerOpt = personopplysningTjeneste
            .hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(revurdering.getId(), revurdering.getAktørId(), relevantPeriode);
        if (personopplysningerOpt.isPresent()) {
            PersonopplysningerAggregat personopplysningerAggregat = personopplysningerOpt.get();

            utledetResultat.putAll(hentEndringForStatsborgerskap(personopplysningerAggregat, revurdering));
            mergeResultat(utledetResultat, hentEndringForPersonstatus(personopplysningerAggregat, revurdering));
            mergeResultat(utledetResultat, hentEndringForAdresse(personopplysningerAggregat, revurdering));
        }
        return utledetResultat;
    }

    private void mergeResultat(Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat, Map<LocalDate, Set<VurderingsÅrsak>> nyeEndringer) {
        for (Map.Entry<LocalDate, Set<VurderingsÅrsak>> localDateSetEntry : nyeEndringer.entrySet()) {
            utledetResultat.merge(localDateSetEntry.getKey(), localDateSetEntry.getValue(), this::slåSammenSet);
        }
    }

    private Set<VurderingsÅrsak> slåSammenSet(Set<VurderingsÅrsak> value1, Set<VurderingsÅrsak> value2) {
        Set<VurderingsÅrsak> vurderingsÅrsaks = new HashSet<>(value1);
        vurderingsÅrsaks.addAll(value2);
        return vurderingsÅrsaks;
    }

    private Map<LocalDate, Set<VurderingsÅrsak>> utledVurderingsdatoerForMedlemskap(Long revurderingId, MedlemEndringssjekker endringssjekker) {
        Optional<MedlemskapAggregat> førsteVersjon = medlemskapRepository.hentFørsteVersjonAvMedlemskap(revurderingId);
        Optional<MedlemskapAggregat> sisteVersjon = medlemskapRepository.hentMedlemskap(revurderingId);

        Set<MedlemskapPerioderEntitet> første = førsteVersjon.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet());
        Set<MedlemskapPerioderEntitet> siste = sisteVersjon.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet());

        List<LocalDateSegment<MedlemskapPerioderEntitet>> førsteListe = første.stream().map(r -> new LocalDateSegment<>(r.getFom(), r.getTom(), r))
            .collect(Collectors.toList());
        List<LocalDateSegment<MedlemskapPerioderEntitet>> sisteListe = siste.stream().map(r -> new LocalDateSegment<>(r.getFom(), r.getTom(), r))
            .collect(Collectors.toList());

        LocalDateTimeline<MedlemskapPerioderEntitet> førsteTidsserie = new LocalDateTimeline<>(førsteListe, this::slåSammenMedlemskapPerioder);
        LocalDateTimeline<MedlemskapPerioderEntitet> andreTidsserie = new LocalDateTimeline<>(sisteListe, this::slåSammenMedlemskapPerioder);

        LocalDateTimeline<MedlemskapPerioderEntitet> resultat = førsteTidsserie.combine(andreTidsserie,
            (di, førsteVersjon1, sisteVersjon1) -> sjekkForEndringIMedl(di, førsteVersjon1, sisteVersjon1, endringssjekker),
            LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return utledResultat(resultat);
    }

    private Map<LocalDate, Set<VurderingsÅrsak>> utledResultat(LocalDateTimeline<MedlemskapPerioderEntitet> resultat) {
        final Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat = new HashMap<>();
        NavigableSet<LocalDateInterval> datoIntervaller = resultat.getDatoIntervaller();
        for (LocalDateInterval localDateInterval : datoIntervaller) {
            LocalDateSegment<MedlemskapPerioderEntitet> perioden = resultat.getSegment(localDateInterval);

            utledetResultat.put(perioden.getFom(), Set.of(VurderingsÅrsak.MEDL_PERIODE));
        }
        return utledetResultat;
    }

    private Map<LocalDate, Set<VurderingsÅrsak>> hentEndringForAdresse(PersonopplysningerAggregat personopplysningerAggregat, Behandling revurdering) {
        List<PersonAdresseEntitet> adresser = personopplysningerAggregat.getAdresserFor(revurdering.getAktørId())
            .stream().sorted(Comparator.comparing(s -> s.getPeriode().getFomDato()))
            .collect(Collectors.toList());
        final Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat = new HashMap<>();
        IntStream.range(0, adresser.size() - 1).forEach(i -> {
            if (i != adresser.size() - 1) { // sjekker om det er siste element
                PersonAdresseEntitet førsteElement = adresser.get(i);
                PersonAdresseEntitet nesteElement = adresser.get(i + 1);
                if (!førsteElement.getAdresseType().equals(nesteElement.getAdresseType())) {
                    utledetResultat.put(nesteElement.getPeriode().getFomDato(), Set.of(VurderingsÅrsak.ADRESSE));
                }
            }
        });
        return utledetResultat;
    }

    private Map<LocalDate, Set<VurderingsÅrsak>> hentEndringForPersonstatus(PersonopplysningerAggregat personopplysningerAggregat, Behandling revurdering) {
        List<PersonstatusEntitet> personstatus = personopplysningerAggregat.getPersonstatuserFor(revurdering.getAktørId())
            .stream().sorted(Comparator.comparing(s -> s.getPeriode().getFomDato()))
            .collect(Collectors.toList());
        final Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat = new HashMap<>();
        IntStream.range(0, personstatus.size() - 1).forEach(i -> {
            if (i != personstatus.size() - 1) { // sjekker om det er siste element
                PersonstatusEntitet førsteElement = personstatus.get(i);
                PersonstatusEntitet nesteElement = personstatus.get(i + 1);
                // skal ikke trigge på personstaus død
                boolean personStausInneholderDød = PersonstatusType.erDød(førsteElement.getPersonstatus())
                    || PersonstatusType.erDød(nesteElement.getPersonstatus());
                if (!personStausInneholderDød && !førsteElement.getPersonstatus().equals(nesteElement.getPersonstatus())) {
                    utledetResultat.put(nesteElement.getPeriode().getFomDato(), Set.of(VurderingsÅrsak.PERSONSTATUS));
                }
            }
        });
        return utledetResultat;
    }

    private Map<LocalDate, Set<VurderingsÅrsak>> hentEndringForStatsborgerskap(PersonopplysningerAggregat personopplysningerAggregat, Behandling revurdering) {
        List<StatsborgerskapEntitet> statsborgerskap = personopplysningerAggregat.getStatsborgerskapFor(revurdering.getAktørId())
            .stream().sorted(Comparator.comparing(s -> s.getPeriode().getFomDato()))
            .collect(Collectors.toList());
        final Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat = new HashMap<>();
        IntStream.range(0, statsborgerskap.size() - 1).forEach(i -> {
            if (i != statsborgerskap.size() - 1) { // sjekker om det er siste element
                StatsborgerskapEntitet førsteElement = statsborgerskap.get(i);
                StatsborgerskapEntitet nesteElement = statsborgerskap.get(i + 1);
                if (!førsteElement.getStatsborgerskap().equals(nesteElement.getStatsborgerskap())) {
                    utledetResultat.put(nesteElement.getPeriode().getFomDato(), Set.of(VurderingsÅrsak.STATSBORGERSKAP));
                }
            }
        });
        return utledetResultat;
    }

    private LocalDateSegment<MedlemskapPerioderEntitet> sjekkForEndringIMedl(@SuppressWarnings("unused") LocalDateInterval di, // NOSONAR
                                                                             LocalDateSegment<MedlemskapPerioderEntitet> førsteVersjon,
                                                                             LocalDateSegment<MedlemskapPerioderEntitet> sisteVersjon,
                                                                             MedlemEndringssjekker endringssjekker) {

        // må alltid sjekke datoer med overlapp
        if (førsteVersjon != null && førsteVersjon.getValue().getKildeType() == null) {
            return førsteVersjon;
        }

        // må alltid sjekke datoer med overlapp
        if (sisteVersjon != null && sisteVersjon.getValue().getKildeType() == null) {
            return sisteVersjon;
        }

        // ny periode registrert
        if (førsteVersjon == null) {
            return sisteVersjon;
        }
        if (sisteVersjon != null) {
            // sjekker om gammel periode har endret verdier
            if (endringssjekker.erEndring(førsteVersjon.getValue(), sisteVersjon.getValue())) {
                return sisteVersjon;
            } else {
                return null;
            }
        }
        // gammel periode fjernet
        return førsteVersjon;
    }

    // Ligger her som en guard mot dårlig datakvalitet i medl.. Skal nemlig aldri inntreffe
    private LocalDateSegment<MedlemskapPerioderEntitet> slåSammenMedlemskapPerioder(LocalDateInterval di,
                                                                                    LocalDateSegment<MedlemskapPerioderEntitet> førsteVersjon,
                                                                                    LocalDateSegment<MedlemskapPerioderEntitet> sisteVersjon) {
        if (førsteVersjon == null || førsteVersjon.getValue() == null) {
            return sisteVersjon;
        } else if (sisteVersjon == null || sisteVersjon.getValue() == null) {
            return førsteVersjon;
        }

        MedlemskapPerioderEntitet senesteBesluttetPeriode = finnMedlemskapPeriodeMedSenestBeslutningsdato(førsteVersjon, sisteVersjon);
        MedlemskapPerioderBuilder builder = new MedlemskapPerioderBuilder(senesteBesluttetPeriode);
        builder.medPeriode(di.getFomDato(), di.getTomDato());
        builder.medKildeType(senesteBesluttetPeriode.getKildeType());

        return new LocalDateSegment<>(di.getFomDato(), di.getTomDato(), builder.build());
    }

    private MedlemskapPerioderEntitet finnMedlemskapPeriodeMedSenestBeslutningsdato(LocalDateSegment<MedlemskapPerioderEntitet> førsteVersjon,
                                                                                    LocalDateSegment<MedlemskapPerioderEntitet> sisteVersjon) {
        MedlemskapPerioderEntitet riktigEntitetVerdi;
        LocalDate førsteBeslutningsdato = førsteVersjon.getValue().getBeslutningsdato();
        LocalDate sisteBeslutningsdato = sisteVersjon.getValue().getBeslutningsdato();
        if (førsteBeslutningsdato != null && (sisteBeslutningsdato == null || førsteBeslutningsdato.isAfter(sisteBeslutningsdato))) {
            riktigEntitetVerdi = førsteVersjon.getValue();
        } else {
            riktigEntitetVerdi = sisteVersjon.getValue();
        }
        return riktigEntitetVerdi;
    }

}
