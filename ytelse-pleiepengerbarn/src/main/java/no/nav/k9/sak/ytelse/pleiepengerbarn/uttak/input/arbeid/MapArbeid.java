package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.Permisjon;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.typer.Stillingsprosent;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

public class MapArbeid {

    public MapArbeid() {
    }

    public List<Arbeid> map(ArbeidstidMappingInput input) {

        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold = mapTilRaw(input);

        return arbeidsforhold.keySet()
            .stream()
            .map(key -> mapArbeidsgiver(arbeidsforhold, key))
            .collect(Collectors.toList());
    }

    public Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> mapTilRaw(ArbeidstidMappingInput input) {
        var tidslinjeTilVurdering = input.getTidslinjeTilVurdering();
        var vilkår = input.getVilkår();
        var opptjeningResultat = input.getOpptjeningResultat();
        var kravDokumenter = input.getKravDokumenter();
        var perioderFraSøknader = input.getPerioderFraSøknader();

        final Map<DatoIntervallEntitet, Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>>> arbeidsforholdPerPeriode = new HashMap<>();

        var perioderTilVurdering = TidslinjeUtil.tilDatoIntervallEntiteter(tidslinjeTilVurdering
            .filterValue(Objects::nonNull));

        var fiktivtKravPgaDødsfall = utledFiktivtKravPgaDødsfall(input);

        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            var måSpesialHåndteres = måPeriodenSpesialHåndteres(input.getSaksnummer(), periode.getFomDato(), input.getSakerSomMåSpesialhåndteres());

            if (måSpesialHåndteres) {
                mapSpesialhåndteringAvArbeidstid(vilkår, kravDokumenter, perioderFraSøknader, arbeidsforholdPerPeriode, periode, input, fiktivtKravPgaDødsfall);
            } else {
                mapArbeidstidForPeriode(vilkår, opptjeningResultat, kravDokumenter, perioderFraSøknader, arbeidsforholdPerPeriode, periode, input, fiktivtKravPgaDødsfall);
            }
        }

        var arbeidsforhold = slåSammenOpplysningerForSammeArbeidsforhold(arbeidsforholdPerPeriode);

        leggTilAnsettSomInaktivPerioder(arbeidsforhold, input.getInaktivTidslinje());

        return arbeidsforhold;
    }

    private FiktivtKravPgaDødsfall utledFiktivtKravPgaDødsfall(ArbeidstidMappingInput input) {
        if (input.getUtvidetPeriodeSomFølgeAvDødsfall() == null) {
            return FiktivtKravPgaDødsfall.ikkeDød();
        }
        var sisteDokumentFørDødsfall = input.getKravDokumenter()
            .stream()
            .takeWhile(it -> it.getInnsendingsTidspunkt().isBefore(input.getUtvidetPeriodeSomFølgeAvDødsfall().getFomDato().atStartOfDay()))
            .max(KravDokument::compareTo);

        if (sisteDokumentFørDødsfall.isEmpty()) {
            return FiktivtKravPgaDødsfall.dødUtenSøknadFørDødsfall();
        }

        return FiktivtKravPgaDødsfall.død(sisteDokumentFørDødsfall.get().getJournalpostId());
    }

    private void mapSpesialhåndteringAvArbeidstid(Vilkår vilkår,
                                                  Set<KravDokument> kravDokumenter,
                                                  Set<PerioderFraSøknad> perioderFraSøknader,
                                                  Map<DatoIntervallEntitet, Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>>> arbeidsforholdPerPeriode,
                                                  DatoIntervallEntitet periode,
                                                  ArbeidstidMappingInput input, FiktivtKravPgaDødsfall fiktivtKravPgaDødsfall) {

        final Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold = new HashMap<>();

        List<VilkårPeriode> midlertidigInaktivVilkårsperioder = vilkår != null ? vilkår.getPerioder().stream()
            .filter(it -> it.getPeriode().overlapper(periode))
            .filter(it -> Objects.equals(VilkårUtfallMerknad.VM_7847_A, it.getMerknad()))
            .collect(Collectors.toList()) : List.of();

        mapInaktivePerioder(arbeidsforhold, midlertidigInaktivVilkårsperioder);

        kravDokumenter.stream()
            .sorted(KravDokument::compareTo)
            .forEachOrdered(at -> prosesserDokument(perioderFraSøknader, periode, arbeidsforhold, at, input, fiktivtKravPgaDødsfall));
        arbeidsforholdPerPeriode.put(periode, arbeidsforhold);
    }

    private boolean måPeriodenSpesialHåndteres(Saksnummer saksnummer,
                                               LocalDate fomDato,
                                               Map<Saksnummer, Set<LocalDate>> sakerSomMåSpesialhåndteres) {

        if (saksnummer == null) {
            return false;
        }

        if (sakerSomMåSpesialhåndteres.containsKey(saksnummer)) {
            return sakerSomMåSpesialhåndteres.get(saksnummer).contains(fomDato);
        }

        return false;
    }

    private void mapArbeidstidForPeriode(Vilkår vilkår,
                                         OpptjeningResultat opptjeningResultat,
                                         Set<KravDokument> kravDokumenter,
                                         Set<PerioderFraSøknad> perioderFraSøknader,
                                         Map<DatoIntervallEntitet, Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>>> arbeidsforholdPerPeriode,
                                         DatoIntervallEntitet periode,
                                         ArbeidstidMappingInput input, FiktivtKravPgaDødsfall harHåndtertDødsfall) {

        final Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold = new HashMap<>();

        List<VilkårPeriode> midlertidigInaktivVilkårsperioder = vilkår != null ? vilkår.getPerioder().stream()
            .filter(it -> it.getPeriode().overlapper(periode))
            .filter(it -> Objects.equals(VilkårUtfallMerknad.VM_7847_A, it.getMerknad()))
            .collect(Collectors.toList()) : List.of();

        List<VilkårPeriode> dagpengerPåSkjæringstidspunktet = vilkår != null ? vilkår.getPerioder().stream()
            .filter(it -> it.getPeriode().overlapper(periode))
            .filter(it -> harDagpengerPåSkjæringstidspunktet(it, opptjeningResultat))
            .collect(Collectors.toList()) : List.of();

        Predicate<VilkårPeriode> harSykepengerAvDagpengerFilter = vilkårPeriode -> harYtelseTypeUtenDagpengerPåSkjæringstidspunktet(vilkårPeriode, opptjeningResultat, dagpengerPåSkjæringstidspunktet, OpptjeningAktivitetType.SYKEPENGER_AV_DAGPENGER);
        var sykepengerFraDagpengerOgIkkeDagpengerPåSkjæringstidspunktet = filtrerPeriode(vilkår, periode, harSykepengerAvDagpengerFilter);
        Predicate<VilkårPeriode> harPleiepengerAvDagpengerFilter = vilkårPeriode -> harYtelseTypeUtenDagpengerPåSkjæringstidspunktet(vilkårPeriode, opptjeningResultat, dagpengerPåSkjæringstidspunktet, OpptjeningAktivitetType.PLEIEPENGER_AV_DAGPENGER);
        var pleiepengerFraDagpengerOgIkkeDagpengerPåSkjæringstidspunktet = filtrerPeriode(vilkår, periode, harPleiepengerAvDagpengerFilter);
        Predicate<VilkårPeriode> kunYtelseFilter = vilkårPeriode -> harKunYtelsePåSkjæringstidspunktet(vilkårPeriode, opptjeningResultat);
        var kunYtelsePåSkjæringstidspunktet = filtrerPeriode(vilkår, periode, kunYtelseFilter);

        mapInaktivePerioder(arbeidsforhold, midlertidigInaktivVilkårsperioder);
        mapPerioderMedType(arbeidsforhold, dagpengerPåSkjæringstidspunktet, periode, UttakArbeidType.DAGPENGER);
        mapPerioderMedType(arbeidsforhold, sykepengerFraDagpengerOgIkkeDagpengerPåSkjæringstidspunktet, periode, UttakArbeidType.SYKEPENGER_AV_DAGPENGER);
        mapPerioderMedType(arbeidsforhold, pleiepengerFraDagpengerOgIkkeDagpengerPåSkjæringstidspunktet, periode, UttakArbeidType.PLEIEPENGER_AV_DAGPENGER);
        mapPerioderMedType(arbeidsforhold, kunYtelsePåSkjæringstidspunktet, periode, UttakArbeidType.KUN_YTELSE);

        kravDokumenter.stream()
            .sorted(KravDokument::compareTo)
            .forEachOrdered(at -> prosesserDokument(perioderFraSøknader, periode, arbeidsforhold, at, input, harHåndtertDødsfall));
        arbeidsforholdPerPeriode.put(periode, arbeidsforhold);
    }

    private List<VilkårPeriode> filtrerPeriode(Vilkår vilkår, DatoIntervallEntitet periode, Predicate<VilkårPeriode> periodeFilter) {
        return vilkår != null ? vilkår.getPerioder().stream()
            .filter(it -> it.getPeriode().overlapper(periode))
            .filter(periodeFilter)
            .collect(Collectors.toList()) : List.of();
    }

    private void leggTilAnsettSomInaktivPerioder(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold, Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> inaktivPerioder) {
        if (inaktivPerioder.isEmpty()) {
            return;
        }

        for (Map.Entry<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> inaktivArbeidsforhold : inaktivPerioder.entrySet()) {
            var relevanteAktiviteter = arbeidsforhold.entrySet()
                .stream()
                .filter(it -> new AktivitetIdentifikator(UttakArbeidType.ARBEIDSTAKER, inaktivArbeidsforhold.getKey().getArbeidsgiver(), null).equals(it.getKey()))
                .collect(Collectors.toSet());

            var inaktivTimeline = inaktivArbeidsforhold.getValue();
            for (Map.Entry<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> aktivitet : relevanteAktiviteter) {
                inaktivTimeline = inaktivTimeline.disjoint(aktivitet.getValue());
            }

            arbeidsforhold.put(inaktivArbeidsforhold.getKey(), inaktivTimeline.compress());
        }
    }

    private boolean harYtelseTypeUtenDagpengerPåSkjæringstidspunktet(VilkårPeriode vilkårPeriode,
                                                                     OpptjeningResultat opptjeningResultat,
                                                                     List<VilkårPeriode> dagpengerPåSkjæringstidspunktet,
                                                                     OpptjeningAktivitetType ytelsetype) {

        Optional<Opptjening> opptjening = finnOpptjeningForPeriode(vilkårPeriode, opptjeningResultat);
        if (opptjening.isEmpty()) {
            return false;
        }

        if (dagpengerPåSkjæringstidspunktet.stream().anyMatch(it -> it.getPeriode().equals(vilkårPeriode.getPeriode()))) {
            return false;
        }

        return opptjening.get().getOpptjeningAktivitet()
            .stream()
            .filter(it -> ytelsetype.equals(it.getAktivitetType()))
            .anyMatch(it -> DatoIntervallEntitet.fraOgMedTilOgMed(vilkårPeriode.getSkjæringstidspunkt().minusDays(1), vilkårPeriode.getSkjæringstidspunkt().minusDays(1)).overlapper(it.getFom(), it.getTom()));
    }

    private Optional<Opptjening> finnOpptjeningForPeriode(VilkårPeriode vilkårPeriode, OpptjeningResultat opptjeningResultat) {
        if (opptjeningResultat == null) {
            return Optional.empty();
        }
        var opptjening = opptjeningResultat.finnOpptjening(vilkårPeriode.getSkjæringstidspunkt());

        if (opptjening.isEmpty()) {
            return Optional.empty();
        }
        return opptjening;
    }

    private boolean harKunYtelsePåSkjæringstidspunktet(VilkårPeriode vilkårPeriode, OpptjeningResultat opptjeningResultat) {
        Optional<Opptjening> opptjening = finnOpptjeningForPeriode(vilkårPeriode, opptjeningResultat);
        if (opptjening.isEmpty()) {
            return false;
        }

        var aktiviteterPåStp = opptjening.get().getOpptjeningAktivitet()
            .stream()
            .filter(it -> Set.of(OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT).contains(it.getKlassifisering()))
            .filter(it -> DatoIntervallEntitet.fraOgMedTilOgMed(vilkårPeriode.getSkjæringstidspunkt().minusDays(1), vilkårPeriode.getSkjæringstidspunkt().minusDays(1)).overlapper(it.getFom(), it.getTom()))
            .toList();
        return !aktiviteterPåStp.isEmpty() &&
            aktiviteterPåStp.stream()
                .allMatch(it -> OpptjeningAktivitetType.YTELSE.contains(it.getAktivitetType()));
    }

    private void mapPerioderMedType(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold,
                                    List<VilkårPeriode> dagpengerPåSkjæringstidspunktet,
                                    DatoIntervallEntitet periode, UttakArbeidType type) {

        var tidslinje = new LocalDateTimeline<WrappedArbeid>(List.of());
        for (VilkårPeriode vilkårPeriode : dagpengerPåSkjæringstidspunktet) {
            var vp = vilkårPeriode.getPeriode();
            var other = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(vp.getFomDato(), vp.getTomDato(), new WrappedArbeid(new ArbeidPeriode(vp, type, null, null, Duration.ofMinutes((long) (7.5 * 60)), Duration.ZERO)))));
            tidslinje = tidslinje.combine(other, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        if (!dagpengerPåSkjæringstidspunktet.isEmpty()) {
            arbeidsforhold.put(new AktivitetIdentifikator(type, null, null), tidslinje.intersection(periode.toLocalDateInterval()).compress());
        }
    }

    private boolean harDagpengerPåSkjæringstidspunktet(VilkårPeriode vilkårPeriode, OpptjeningResultat opptjeningResultat) {
        Optional<Opptjening> opptjening = finnOpptjeningForPeriode(vilkårPeriode, opptjeningResultat);
        if (opptjening.isEmpty()) {
            return false;
        }

        return opptjening.get().getOpptjeningAktivitet()
            .stream()
            .filter(it -> OpptjeningAktivitetType.DAGPENGER.equals(it.getAktivitetType()))
            .anyMatch(it -> DatoIntervallEntitet.fraOgMedTilOgMed(vilkårPeriode.getSkjæringstidspunkt().minusDays(1), vilkårPeriode.getSkjæringstidspunkt().minusDays(1)).overlapper(it.getFom(), it.getTom()));
    }

    private void mapInaktivePerioder(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold,
                                     List<VilkårPeriode> vilkårPerioder) {

        var tidslinje = new LocalDateTimeline<WrappedArbeid>(List.of());
        for (VilkårPeriode vilkårPeriode : vilkårPerioder) {
            var vp = vilkårPeriode.getPeriode();
            var other = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(vp.getFomDato(), vp.getTomDato(), new WrappedArbeid(new ArbeidPeriode(vp, UttakArbeidType.INAKTIV, null, null, Duration.ofMinutes((long) (7.5 * 60)), Duration.ZERO)))));
            tidslinje = tidslinje.combine(other, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        if (!vilkårPerioder.isEmpty()) {
            arbeidsforhold.put(new AktivitetIdentifikator(UttakArbeidType.INAKTIV, null, null), tidslinje.compress());
        }
    }

    private void prosesserDokument(Set<PerioderFraSøknad> perioderFraSøknader,
                                   DatoIntervallEntitet periode,
                                   Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold,
                                   KravDokument at,
                                   ArbeidstidMappingInput input,
                                   FiktivtKravPgaDødsfall fiktivtKravPgaDødsfall) {

        var dokumenter = perioderFraSøknader.stream()
            .filter(it -> it.getJournalpostId().equals(at.getJournalpostId()))
            .collect(Collectors.toSet());
        if (dokumenter.size() == 1) {

            dokumenter.stream()
                .map(PerioderFraSøknad::getArbeidPerioder)
                .flatMap(Collection::stream)
                .forEach(p -> mapArbeidsopplysningerFraDokument(periode, arbeidsforhold, p, input));

            var dødsdatoIPerioden = input.getUtvidetPeriodeSomFølgeAvDødsfall() != null && periodeOverlapperMedDødsdato(periode, input);
            var harKravFørDødsfallOgDetteErSiste = fiktivtKravPgaDødsfall.getHarKravdokumentInnsendtFørDødsfall() && Objects.equals(at.getJournalpostId(), fiktivtKravPgaDødsfall.getSisteKravFørDødsfall());
            if (dødsdatoIPerioden && !fiktivtKravPgaDødsfall.getHarHåndtertDødsfall() && (harKravFørDødsfallOgDetteErSiste || !fiktivtKravPgaDødsfall.getHarKravdokumentInnsendtFørDødsfall())) {
                håndterDødsfall(arbeidsforhold, input, fiktivtKravPgaDødsfall);
                fiktivtKravPgaDødsfall.markerHåndtert();
            }
        } else {
            throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + at);
        }
    }

    private boolean periodeOverlapperMedDødsdato(DatoIntervallEntitet periode, ArbeidstidMappingInput input) {
        var dødsdato = DatoIntervallEntitet.fraOgMedTilOgMed(input.getUtvidetPeriodeSomFølgeAvDødsfall().getFomDato().minusDays(1), input.getUtvidetPeriodeSomFølgeAvDødsfall().getFomDato());
        return periode.overlapper(dødsdato);
    }

    private DatoIntervallEntitet periodeMedDødsfall(ArbeidstidMappingInput input) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(input.getUtvidetPeriodeSomFølgeAvDødsfall().getFomDato().minusDays(1), input.getUtvidetPeriodeSomFølgeAvDødsfall().getTomDato());
    }

    private void håndterDødsfall(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold, ArbeidstidMappingInput input, FiktivtKravPgaDødsfall fiktivtKravPgaDødsfall) {
        var dødstidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeMedDødsfall(input).toLocalDateInterval(), null)));

        var relevantArbeidPåDødsdato = arbeidsforhold.entrySet()
            .stream()
            .filter(this::erAvRelevantType)
            .filter(it -> it.getValue().intersects(dødstidslinje))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        relevantArbeidPåDødsdato.entrySet().forEach(entry -> leggTilFraværForDødsfall(arbeidsforhold, entry, input, fiktivtKravPgaDødsfall));
    }

    private void leggTilFraværForDødsfall(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold, Map.Entry<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> entry, ArbeidstidMappingInput input, FiktivtKravPgaDødsfall fiktivtKravPgaDødsfall) {
        var dødsperiode = input.getUtvidetPeriodeSomFølgeAvDødsfall(); // Dødsdato + justert periode
        var periodeSomSkalJusteresSomFølgeAvDødsfall = DatoIntervallEntitet.fraOgMedTilOgMed(dødsperiode.getFomDato().plusDays(1), dødsperiode.getTomDato()); // justert periode
        var intersection = entry.getValue().intersection(periodeMedDødsfall(input).toLocalDateInterval());
        var manglendePerioderIDødsPerioden = new LocalDateTimeline<WrappedArbeid>(List.of(new LocalDateSegment<>(periodeSomSkalJusteresSomFølgeAvDødsfall.toLocalDateInterval(), null))).disjoint(intersection);
        var key = utledKey(intersection);
        var perioder = arbeidsforhold.getOrDefault(key, new LocalDateTimeline<>(List.of()));

        for (LocalDateSegment<WrappedArbeid> segment : intersection.toSegments()) {
            var value = segment.getValue();
            var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), new WrappedArbeid(value.getPeriode(), fiktivtKravPgaDødsfall.getHarKravdokumentInnsendtFørDødsfall() ? Duration.ZERO : value.getPeriode().getFaktiskArbeidTimerPerDag()))));
            perioder = perioder.combine(timeline.intersection(periodeSomSkalJusteresSomFølgeAvDødsfall.toLocalDateInterval()), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        for (LocalDateSegment<WrappedArbeid> segment : manglendePerioderIDødsPerioden.toSegments()) {
            var value = finnVerdiForutFor(perioder, segment.getLocalDateInterval());
            if (value == null) {
                continue;
            }
            var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), new WrappedArbeid(value.getPeriode(), Duration.ZERO))));
            perioder = perioder.combine(timeline.intersection(periodeSomSkalJusteresSomFølgeAvDødsfall.toLocalDateInterval()), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        perioder = justerPeriodenIHenholdTilStartenPåArbeidsforholdet(input, key, perioder);
        arbeidsforhold.put(key, perioder);
    }

    private WrappedArbeid finnVerdiForutFor(LocalDateTimeline<WrappedArbeid> perioder, LocalDateInterval interval) {
        var dato = interval.getFomDato().minusDays(1);
        var iterator = perioder.intersection(new LocalDateInterval(dato, dato)).toSegments().iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        return iterator.next().getValue();
    }

    private AktivitetIdentifikator utledKey(LocalDateTimeline<WrappedArbeid> intersection) {
        var p = intersection.toSegments().iterator().next().getValue().getPeriode();
        return new AktivitetIdentifikator(p.getAktivitetType(), p.getArbeidsgiver(), InternArbeidsforholdRef.nullRef());
    }

    private boolean erAvRelevantType(Map.Entry<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> it) {
        return Set.of(UttakArbeidType.ARBEIDSTAKER, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, UttakArbeidType.FRILANSER).contains(it.getKey().getAktivitetType());
    }

    private void mapArbeidsopplysningerFraDokument(DatoIntervallEntitet periode,
                                                   Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold,
                                                   ArbeidPeriode p,
                                                   ArbeidstidMappingInput input) {

        var key = new AktivitetIdentifikator(p.getAktivitetType(), p.getArbeidsgiver(), InternArbeidsforholdRef.nullRef());
        var perioder = arbeidsforhold.getOrDefault(key, new LocalDateTimeline<>(List.of()));
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), new WrappedArbeid(p))));
        perioder = perioder.combine(timeline.intersection(periode.toLocalDateInterval()), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        perioder = justerPeriodenIHenholdTilStartenPåArbeidsforholdet(input, key, perioder);
        arbeidsforhold.put(key, perioder);
    }

    private LocalDateTimeline<WrappedArbeid> justerPeriodenIHenholdTilStartenPåArbeidsforholdet(ArbeidstidMappingInput input, AktivitetIdentifikator key, LocalDateTimeline<WrappedArbeid> perioder) {
        if (UttakArbeidType.ARBEIDSTAKER.equals(key.getAktivitetType()) && input.getInntektArbeidYtelseGrunnlag() != null) {
            var aktørArbeid = input.getInntektArbeidYtelseGrunnlag().getAktørArbeidFraRegister(input.getBruker())
                .map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(List.of());

            var yrkesaktiviteter = aktørArbeid.stream()
                .filter(it -> Set.of(ArbeidType.MARITIMT_ARBEIDSFORHOLD, ArbeidType.FORENKLET_OPPGJØRSORDNING, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD).contains(it.getArbeidType()))
                .filter(it -> it.getArbeidsgiver().equals(key.getArbeidsgiver()))
                .collect(Collectors.toList());

            var arbeidstidslinje = mapArbeidsTidslinje(yrkesaktiviteter);

            perioder = perioder.intersection(arbeidstidslinje);
        }
        return perioder;
    }

    private LocalDateTimeline<Boolean> mapArbeidsTidslinje(List<Yrkesaktivitet> yrkesaktiviteter) {
        var timeline = new LocalDateTimeline<Boolean>(List.of());

        for (Yrkesaktivitet yrkesaktivitet : yrkesaktiviteter) {
            var aktivitet = new LocalDateTimeline<Boolean>(List.of());

            var segmenter = yrkesaktivitet.getAnsettelsesPeriode()
                .stream()
                .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
                .toList();
            // Har ikke helt kontroll på aa-reg mtp overlapp her så better safe than sorry
            for (LocalDateSegment<Boolean> segment : segmenter) {
                var arbeidsforholdTidslinje = new LocalDateTimeline<>(List.of(segment));
                aktivitet = aktivitet.combine(arbeidsforholdTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }

            var relevantePermitteringer = yrkesaktivitet.getPermisjon().stream()
                .filter(it -> Objects.equals(it.getPermisjonsbeskrivelseType(), PermisjonsbeskrivelseType.PERMITTERING))
                .filter(it -> erStørreEllerLik100Prosent(it.getProsentsats()))
                .toList();

            for (Permisjon permisjon : relevantePermitteringer) {
                var permittert = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(permisjon.getFraOgMed(), permisjon.getTilOgMed(), true)));
                aktivitet = aktivitet.disjoint(permittert);
            }

            timeline = timeline.combine(aktivitet, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return timeline.compress();
    }

    private boolean erStørreEllerLik100Prosent(Stillingsprosent prosentsats) {
        return Stillingsprosent.HUNDRED.getVerdi().intValue() <= prosentsats.getVerdi().intValue();
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> slåSammenOpplysningerForSammeArbeidsforhold(Map<DatoIntervallEntitet, Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>>> arbeidsforholdPerPeriode) {
        var arbeidsforhold = new HashMap<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>>();

        for (Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> aktiviteterIPeriode : arbeidsforholdPerPeriode.values()) {
            for (AktivitetIdentifikator aktivitetIdentifikator : aktiviteterIPeriode.keySet()) {
                var perioder = arbeidsforhold.getOrDefault(aktivitetIdentifikator, new LocalDateTimeline<>(List.of()));
                var arbeidIPerioden = aktiviteterIPeriode.get(aktivitetIdentifikator);
                perioder = perioder.combine(arbeidIPerioden, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                arbeidsforhold.put(aktivitetIdentifikator, perioder);
            }
        }

        return arbeidsforhold;
    }

    private Arbeid mapArbeidsgiver(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold, AktivitetIdentifikator key) {
        var perioder = new HashMap<LukketPeriode, ArbeidsforholdPeriodeInfo>();
        arbeidsforhold.get(key)
            .compress()
            .toSegments()
            .stream()
            .filter(it -> Objects.nonNull(it.getValue()))
            .forEach(p -> mapArbeidForPeriode(arbeidsforhold, perioder, p));

        return new Arbeid(mapArbeidsforhold(key), perioder);
    }

    private void mapArbeidForPeriode(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold, HashMap<LukketPeriode, ArbeidsforholdPeriodeInfo> perioder, LocalDateSegment<WrappedArbeid> p) {
        var periode = p.getValue().getPeriode();
        var antallLinjerPerArbeidsgiver = arbeidsforhold.keySet().stream().filter(it -> Objects.equals(periode.getAktivitetType(), it.getAktivitetType()) && periode.getArbeidsgiver() != null && Objects.equals(it.getArbeidsgiver(), periode.getArbeidsgiver())).count();
        var jobberNormalt = justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Optional.ofNullable(periode.getJobberNormaltTimerPerDag()).orElse(justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Duration.ZERO)));
        var jobberFaktisk = justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Optional.ofNullable(periode.getFaktiskArbeidTimerPerDag()).orElse(justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Duration.ZERO)));
        perioder.put(new LukketPeriode(p.getFom(), p.getTom()),
            new ArbeidsforholdPeriodeInfo(jobberNormalt, jobberFaktisk));
    }

    private Duration justerIHenholdTilAntallet(long antallLinjerPerArbeidsgiver, Duration duration) {
        if (Duration.ZERO.equals(duration) || antallLinjerPerArbeidsgiver == 0 || antallLinjerPerArbeidsgiver == 1) {
            return duration;
        }
        return duration.dividedBy(antallLinjerPerArbeidsgiver);
    }

    private Arbeidsforhold mapArbeidsforhold(AktivitetIdentifikator identifikator) {
        var arbeidsforhold = new Arbeidsforhold(identifikator.getAktivitetType().getKode(),
            Optional.ofNullable(identifikator.getArbeidsgiver()).map(Arbeidsgiver::getArbeidsgiverOrgnr).orElse(null),
            Optional.ofNullable(identifikator.getArbeidsgiver()).map(Arbeidsgiver::getAktørId).map(AktørId::getId).orElse(null),
            Optional.ofNullable(identifikator.getArbeidsforhold()).map(InternArbeidsforholdRef::getReferanse).orElse(null)
        );
        valider(arbeidsforhold);
        return arbeidsforhold;
    }

    private void valider(Arbeidsforhold arbeidsforhold) {
        var arbeidType = UttakArbeidType.fraKode(arbeidsforhold.getType());
        if (UttakArbeidType.ARBEIDSTAKER.equals(arbeidType)) {
            if (arbeidsforhold.getOrganisasjonsnummer() == null && arbeidsforhold.getAktørId() == null) {
                throw new IllegalStateException("Arbeidsforhold må ha identifikator");
            }
        }
    }
}
