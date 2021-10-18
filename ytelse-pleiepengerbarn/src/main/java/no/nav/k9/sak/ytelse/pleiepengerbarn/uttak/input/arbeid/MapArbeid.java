package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;
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

        var perioderTilVurdering = tidslinjeTilVurdering.toSegments()
            .stream()
            .filter(it -> Objects.nonNull(it.getValue()))
            .map(it -> DatoIntervallEntitet.fra(it.getFom(), it.getTom()))
            .collect(Collectors.toSet());

        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            var måSpesialHåndteres = måPeriodenSpesialHåndteres(input.getSaksnummer(), periode.getFomDato(), input.getSakerSomMåSpesialhåndteres());

            if (måSpesialHåndteres) {
                mapSpesialhåndteringAvArbeidstid(vilkår, kravDokumenter, perioderFraSøknader, arbeidsforholdPerPeriode, periode, input);
            } else {
                mapArbeidstidForPeriode(vilkår, opptjeningResultat, kravDokumenter, perioderFraSøknader, arbeidsforholdPerPeriode, periode, input);
            }
        }

        var arbeidsforhold = slåSammenOpplysningerForSammeArbeidsforhold(arbeidsforholdPerPeriode);

        leggTilAnsettSomInaktivPerioder(arbeidsforhold, input.getInaktivTidslinje());

        return arbeidsforhold;
    }

    private void mapSpesialhåndteringAvArbeidstid(Vilkår vilkår,
                                                  Set<KravDokument> kravDokumenter,
                                                  Set<PerioderFraSøknad> perioderFraSøknader,
                                                  Map<DatoIntervallEntitet, Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>>> arbeidsforholdPerPeriode,
                                                  DatoIntervallEntitet periode,
                                                  ArbeidstidMappingInput input) {

        final Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold = new HashMap<>();

        List<VilkårPeriode> midlertidigInaktivVilkårsperioder = vilkår != null ? vilkår.getPerioder().stream()
            .filter(it -> it.getPeriode().overlapper(periode))
            .filter(it -> Objects.equals(VilkårUtfallMerknad.VM_7847_A, it.getMerknad()))
            .collect(Collectors.toList()) : List.of();

        var midlertidigInaktivPeriode = mapInaktivePerioder(arbeidsforhold, midlertidigInaktivVilkårsperioder);

        kravDokumenter.stream()
            .sorted(KravDokument::compareTo)
            .forEachOrdered(at -> prosesserDokument(perioderFraSøknader, periode, arbeidsforhold, midlertidigInaktivPeriode, at, input));
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
                                         ArbeidstidMappingInput input) {

        final Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold = new HashMap<>();

        List<VilkårPeriode> midlertidigInaktivVilkårsperioder = vilkår != null ? vilkår.getPerioder().stream()
            .filter(it -> it.getPeriode().overlapper(periode))
            .filter(it -> Objects.equals(VilkårUtfallMerknad.VM_7847_A, it.getMerknad()))
            .collect(Collectors.toList()) : List.of();

        List<VilkårPeriode> dagpengerPåSkjæringstidspunktet = vilkår != null ? vilkår.getPerioder().stream()
            .filter(it -> it.getPeriode().overlapper(periode))
            .filter(it -> harDagpengerPåSkjæringstidspunktet(it, opptjeningResultat))
            .collect(Collectors.toList()) : List.of();

        List<VilkårPeriode> sykepengerFraDagpengerOgIkkeDagpengerPåSkjæringstidspunktet = vilkår != null ? vilkår.getPerioder().stream()
            .filter(it -> it.getPeriode().overlapper(periode))
            .filter(it -> harSykepengerFraDagpengerPåSkjæringstidspunktet(it, opptjeningResultat, dagpengerPåSkjæringstidspunktet))
            .collect(Collectors.toList()) : List.of();

        List<VilkårPeriode> kunYtelsePåSkjæringstidspunktet = vilkår != null ? vilkår.getPerioder().stream()
            .filter(it -> it.getPeriode().overlapper(periode))
            .filter(it -> harKunYtelsePåSkjæringstidspunktet(it, opptjeningResultat))
            .collect(Collectors.toList()) : List.of();

        var midlertidigInaktivPeriode = mapInaktivePerioder(arbeidsforhold, midlertidigInaktivVilkårsperioder);
        mapPerioderMedType(arbeidsforhold, dagpengerPåSkjæringstidspunktet, UttakArbeidType.DAGPENGER);
        mapPerioderMedType(arbeidsforhold, sykepengerFraDagpengerOgIkkeDagpengerPåSkjæringstidspunktet, UttakArbeidType.SYKEPENGER_AV_DAGPENGER);
        mapPerioderMedType(arbeidsforhold, kunYtelsePåSkjæringstidspunktet, UttakArbeidType.KUN_YTELSE);

        kravDokumenter.stream()
            .sorted(KravDokument::compareTo)
            .forEachOrdered(at -> prosesserDokument(perioderFraSøknader, periode, arbeidsforhold, midlertidigInaktivPeriode, at, input));
        arbeidsforholdPerPeriode.put(periode, arbeidsforhold);
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

    private boolean harSykepengerFraDagpengerPåSkjæringstidspunktet(VilkårPeriode vilkårPeriode,
                                                                    OpptjeningResultat opptjeningResultat,
                                                                    List<VilkårPeriode> dagpengerPåSkjæringstidspunktet) {

        Optional<Opptjening> opptjening = finnOpptjeningForPeriode(vilkårPeriode, opptjeningResultat);
        if (opptjening.isEmpty()) {
            return false;
        }

        if (dagpengerPåSkjæringstidspunktet.stream().anyMatch(it -> it.getPeriode().equals(vilkårPeriode.getPeriode()))) {
            return false;
        }

        return opptjening.get().getOpptjeningAktivitet()
            .stream()
            .filter(it -> OpptjeningAktivitetType.SYKEPENGER_AV_DAGPENGER.equals(it.getAktivitetType()))
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

        return opptjening.get().getOpptjeningAktivitet()
            .stream()
            .filter(it -> DatoIntervallEntitet.fraOgMedTilOgMed(vilkårPeriode.getSkjæringstidspunkt().minusDays(1), vilkårPeriode.getSkjæringstidspunkt().minusDays(1)).overlapper(it.getFom(), it.getTom()))
            .allMatch(it -> OpptjeningAktivitetType.YTELSE.contains(it.getAktivitetType()));
    }

    private void mapPerioderMedType(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold,
                                    List<VilkårPeriode> dagpengerPåSkjæringstidspunktet,
                                    UttakArbeidType type) {

        var tidslinje = new LocalDateTimeline<WrappedArbeid>(List.of());
        for (VilkårPeriode vilkårPeriode : dagpengerPåSkjæringstidspunktet) {
            var vp = vilkårPeriode.getPeriode();
            var other = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(vp.getFomDato(), vp.getTomDato(), new WrappedArbeid(new ArbeidPeriode(vp, type, null, null, Duration.ofMinutes((long) (7.5 * 60)), Duration.ZERO)))));
            tidslinje = tidslinje.combine(other, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        if (!dagpengerPåSkjæringstidspunktet.isEmpty()) {
            arbeidsforhold.put(new AktivitetIdentifikator(type, null, null), tidslinje.compress());
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

    private HashSet<DatoIntervallEntitet> mapInaktivePerioder(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold,
                                                              List<VilkårPeriode> vilkårPerioder) {

        var midlertidigInaktivPeriode = new HashSet<DatoIntervallEntitet>();

        var tidslinje = new LocalDateTimeline<WrappedArbeid>(List.of());
        for (VilkårPeriode vilkårPeriode : vilkårPerioder) {
            var vp = vilkårPeriode.getPeriode();
            var other = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(vp.getFomDato(), vp.getTomDato(), new WrappedArbeid(new ArbeidPeriode(vp, UttakArbeidType.INAKTIV, null, null, Duration.ofMinutes((long) (7.5 * 60)), Duration.ZERO)))));
            tidslinje = tidslinje.combine(other, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            midlertidigInaktivPeriode.add(vp);
        }
        if (!vilkårPerioder.isEmpty()) {
            arbeidsforhold.put(new AktivitetIdentifikator(UttakArbeidType.INAKTIV, null, null), tidslinje.compress());
        }

        return midlertidigInaktivPeriode;
    }

    private void prosesserDokument(Set<PerioderFraSøknad> perioderFraSøknader,
                                   DatoIntervallEntitet periode,
                                   Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold,
                                   Set<DatoIntervallEntitet> midlertidigInaktivPeriode,
                                   KravDokument at,
                                   ArbeidstidMappingInput input) {
        var dokumenter = perioderFraSøknader.stream()
            .filter(it -> it.getJournalpostId().equals(at.getJournalpostId()))
            .collect(Collectors.toSet());
        if (dokumenter.size() == 1) {
            dokumenter.stream()
                .map(PerioderFraSøknad::getArbeidPerioder)
                .flatMap(Collection::stream)
                .forEach(p -> mapArbeidsopplysningerFraDokument(periode, arbeidsforhold, midlertidigInaktivPeriode, p, input));
        } else {
            throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + at);
        }
    }

    private void mapArbeidsopplysningerFraDokument(DatoIntervallEntitet periode,
                                                   Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold,
                                                   Set<DatoIntervallEntitet> midlertidigInaktivPeriode,
                                                   ArbeidPeriode p,
                                                   ArbeidstidMappingInput input) {

        var key = new AktivitetIdentifikator(p.getAktivitetType(), p.getArbeidsgiver(), InternArbeidsforholdRef.nullRef());
        var perioder = arbeidsforhold.getOrDefault(key, new LocalDateTimeline<>(List.of()));
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), new WrappedArbeid(p))));
        perioder = perioder.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        if (!midlertidigInaktivPeriode.isEmpty()) {
            // Passe på at periodene ikke overlapper med innaktive perioder
            var midlerTidigInaktivTimeline = new LocalDateTimeline<>(midlertidigInaktivPeriode.stream()
                .map(it -> new LocalDateSegment<WrappedArbeid>(it.toLocalDateInterval(), null))
                .collect(Collectors.toList()));
            perioder = perioder.combine(midlerTidigInaktivTimeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        perioder = justerPeriodenIHenholdTilStartenPåArbeidsforholdet(periode, input, key, perioder);
        arbeidsforhold.put(key, perioder.intersection(periode.toLocalDateInterval()));
    }

    private LocalDateTimeline<WrappedArbeid> justerPeriodenIHenholdTilStartenPåArbeidsforholdet(DatoIntervallEntitet periode, ArbeidstidMappingInput input, AktivitetIdentifikator key, LocalDateTimeline<WrappedArbeid> perioder) {
        if (UttakArbeidType.ARBEIDSTAKER.equals(key.getAktivitetType()) && input.getInntektArbeidYtelseGrunnlag() != null) {
            var aktørArbeid = input.getInntektArbeidYtelseGrunnlag().getAktørArbeidFraRegister(input.getBruker())
                .map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(List.of());

            var startDato = aktørArbeid.stream()
                .filter(it -> Set.of(ArbeidType.MARITIMT_ARBEIDSFORHOLD, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD).contains(it.getArbeidType()))
                .filter(it -> it.getArbeidsgiver().equals(key.getArbeidsgiver()))
                .map(Yrkesaktivitet::getAnsettelsesPeriode)
                .flatMap(Collection::stream)
                .map(AktivitetsAvtale::getPeriode)
                .map(DatoIntervallEntitet::getFomDato)
                .min(LocalDate::compareTo);

            if (startDato.isPresent()) {
                var gyldigArbeidsgiverTidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(startDato.get(), periode.getTomDato(), true)));
                perioder = perioder.intersection(gyldigArbeidsgiverTidslinje);
            }
        }
        return perioder;
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
            Optional.ofNullable(identifikator.getArbeidsgiver()).map(Arbeidsgiver::getArbeidsgiverAktørId).map(AktørId::getId).orElse(null),
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
