package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.iay.modell.YtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningAktivitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningInputPeriode;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

@ApplicationScoped
public class OverstyrInputBeregningTjeneste {

    private FagsakRepository fagsakRepository;
    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjenester;
    private BeregningPerioderGrunnlagRepository grunnlagRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private final PåTversAvHelgErKantIKantVurderer kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();

    private FiltrerInntektsmeldingForBeregningInputOverstyring filtrerInntektsmeldingTjeneste;

    public OverstyrInputBeregningTjeneste() {
    }

    @Inject
    public OverstyrInputBeregningTjeneste(FagsakRepository fagsakRepository,
                                          @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjenester,
                                          BeregningPerioderGrunnlagRepository grunnlagRepository,
                                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                          @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                          FiltrerInntektsmeldingForBeregningInputOverstyring filtrerInntektsmeldingTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.opptjeningForBeregningTjenester = opptjeningForBeregningTjenester;
        this.grunnlagRepository = grunnlagRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.filtrerInntektsmeldingTjeneste = filtrerInntektsmeldingTjeneste;
    }

    /**
     * Lager dto for overstyring av input til beregning
     *
     * @param behandlingReferanse Behandlingreferanse
     * @return Overstyringsperioder for gui
     */
    public List<OverstyrBeregningInputPeriode> getPerioderForInputOverstyring(BehandlingReferanse behandlingReferanse) {
        List<SakInfotrygdMigrering> sakInfotrygdMigreringer = fagsakRepository.hentSakInfotrygdMigreringer(behandlingReferanse.getFagsakId());
        var inntektsmeldingerForSak = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(behandlingReferanse.getSaksnummer());
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getId());
        var perioderTilVurdering = getPerioderTilVurderingTjeneste(behandlingReferanse).utled(behandlingReferanse.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        return sakInfotrygdMigreringer.stream().map(sakInfotrygdMigrering -> {
            var migrertStp = sakInfotrygdMigrering.getSkjæringstidspunkt();
            var overstyrteAktiviteter = mapInputOverstyringerForSkjæringstidspunkt(behandlingReferanse, inntektsmeldingerForSak, perioderTilVurdering, migrertStp);
            var ytelseGrunnlag = finnYtelseGrunnlagForMigrering(behandlingReferanse, migrertStp, iayGrunnlag);
            var harKategoriNæring = harNæring(ytelseGrunnlag);
            var harKategoriFrilans = harFrilans(ytelseGrunnlag);
            return new OverstyrBeregningInputPeriode(migrertStp, overstyrteAktiviteter, harKategoriNæring, harKategoriFrilans);
        }).collect(Collectors.toList());
    }

    /**
     * Utleder overstyringsdata/preutfylling for arbeidsgiver basert på inntektmeldinger og perioder der disse skal brukes (tidslinjeformat)
     *
     * @param migrertStp                  Skjæringstidspunkt
     * @param arbeidsgiver                Arbeidsgiver
     * @param inntektsmeldingerForPeriode Tidslinje for inntektsmeldinger som skal brukes for vilkårsperioden
     * @return Data fra inntektsmelding dersom det finnes inntektsmeldinger for arbeidsgiveren
     */
    public static Optional<OverstyrBeregningAktivitet> mapTilInntektsmeldingAktivitet(LocalDate migrertStp,
                                                                                      Arbeidsgiver arbeidsgiver,
                                                                                      LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForPeriode) {

        var inntektsmeldingerForArbeidsgiver = inntektsmeldingerForPeriode.mapValue(ims -> ims.stream().filter(i -> arbeidsgiver.equals(i.getArbeidsgiver())).collect(Collectors.toSet()));

        if (inntektsmeldingerForArbeidsgiver.isEmpty()) {
            return Optional.empty();
        }
        var refusjonTidslinje = FinnRefusjonskravTidslinje.lagTidslinje(migrertStp, inntektsmeldingerForArbeidsgiver);
        var refusjonskravFraVedStart = utledRefusjonskravVedStpFraRefusjontidslinje(migrertStp, refusjonTidslinje);
        var startdatoRefusjon = utledStartdatoRefusjonFraRefusjontidslinje(refusjonTidslinje);
        var refusjonOpphører = utledOpphørRefujonFraRefusjontidslinje(refusjonTidslinje);
        return Optional.of(new OverstyrBeregningAktivitet(
            arbeidsgiver.getArbeidsgiverOrgnr() == null ? null : new OrgNummer(arbeidsgiver.getArbeidsgiverOrgnr()),
            arbeidsgiver.getArbeidsgiverAktørId() == null ? null : new AktørId(arbeidsgiver.getArbeidsgiverAktørId()),
            finnInntektFraInntektsmeldingtidslinje(migrertStp, inntektsmeldingerForArbeidsgiver),
            refusjonskravFraVedStart,
            startdatoRefusjon,
            refusjonOpphører,
            skalKunneEndreRefusjonskrav(refusjonTidslinje)
        ));
    }

    private static boolean skalKunneEndreRefusjonskrav(LocalDateTimeline<BigDecimal> refusjonTidslinje) {
        return refusjonTidslinje.filterValue(r -> r.compareTo(BigDecimal.ZERO) > 0).isEmpty();
    }

    private static LocalDate utledStartdatoRefusjonFraRefusjontidslinje(LocalDateTimeline<BigDecimal> refusjonTidslinje) {
        var harRefusjonskrav = !refusjonTidslinje.filterValue(r -> r.compareTo(BigDecimal.ZERO) > 0).isEmpty();
        return harRefusjonskrav ? refusjonTidslinje.filterValue(r -> r.compareTo(BigDecimal.ZERO) > 0)
            .getMinLocalDate() : null;
    }

    private List<OverstyrBeregningAktivitet> mapInputOverstyringerForSkjæringstidspunkt(BehandlingReferanse behandlingReferanse, Set<Inntektsmelding> inntektsmeldingerForSak, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate migrertStp) {
        var vilkårsperiode = finnVilkårsperiode(perioderTilVurdering, migrertStp);
        var arbeidsaktiviteter = finnArbeidsaktiviteterForOverstyring(behandlingReferanse, vilkårsperiode);
        var overstyrtInputPeriode = finnEksisterendeOverstyring(behandlingReferanse, migrertStp);
        var inntektsmeldingerForPeriode = filtrerInntektsmeldingTjeneste.finnGyldighetstidslinjeForInntektsmeldinger(behandlingReferanse, inntektsmeldingerForSak, vilkårsperiode);
        return mapInputForAktiviteter(migrertStp, arbeidsaktiviteter, overstyrtInputPeriode, inntektsmeldingerForPeriode);
    }

    private List<OverstyrBeregningAktivitet> mapInputForAktiviteter(LocalDate migrertStp, List<OpptjeningAktiviteter.OpptjeningPeriode> arbeidsaktiviteter, Optional<InputOverstyringPeriode> overstyrtInputPeriode, LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForPeriode) {
        return arbeidsaktiviteter.stream()
            .map(a -> mapTilOverstyrAktiviteter(migrertStp, overstyrtInputPeriode, a, inntektsmeldingerForPeriode))
            .collect(Collectors.toList());
    }

    private static DatoIntervallEntitet finnVilkårsperiode(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate migrertStp) {
        return perioderTilVurdering.stream()
            .filter(p -> p.getFomDato().equals(migrertStp))
            .findFirst().orElseThrow(() -> new IllegalStateException("Fant ingen periode for sakinfotrygdmigrering"));
    }

    private boolean harNæring(Optional<YtelseGrunnlag> ytelseGrunnlag) {
        return ytelseGrunnlag.map(yg -> yg.getArbeidskategori().stream().anyMatch(ak -> ak.equals(Arbeidskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            || ak.equals(Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_FISKER)
            || ak.equals(Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_JORDBRUKER)
            || ak.equals(Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE))).orElse(false);
    }


    private boolean harFrilans(Optional<YtelseGrunnlag> ytelseGrunnlag) {
        return ytelseGrunnlag.map(yg -> yg.getArbeidskategori().stream().anyMatch(ak -> ak.equals(Arbeidskategori.FRILANSER)
            || ak.equals(Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_FRILANSER))).orElse(false);
    }


    private Optional<YtelseGrunnlag> finnYtelseGrunnlagForMigrering(BehandlingReferanse behandlingReferanse, LocalDate migrertStp, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var overlappGrunnlagListe = finnOverlappendeGrunnlag(behandlingReferanse, migrertStp, iayGrunnlag);
        // Sjekker overlapp og deretter kant i kant
        if (overlappGrunnlagListe.isPresent()) {
            return overlappGrunnlagListe;
        }
        return finnKantIKantGrunnlagsliste(behandlingReferanse, migrertStp, iayGrunnlag);
    }

    private Optional<YtelseGrunnlag> finnKantIKantGrunnlagsliste(BehandlingReferanse behandlingReferanse, LocalDate migrertStp, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return new YtelseFilter(iayGrunnlag.getAktørYtelseFraRegister(behandlingReferanse.getAktørId()))
            //det er riktig at PPN ikke er med her, siden dette bare gjelder migrerte data for PSB
            .filter(y -> y.getYtelseType().equals(FagsakYtelseType.PSB) && y.getKilde().equals(Fagsystem.INFOTRYGD))
            .filter(y -> y.getYtelseAnvist().stream().anyMatch(ya -> {
                var stpIntervall = DatoIntervallEntitet.fraOgMedTilOgMed(migrertStp, migrertStp);
                var anvistIntervall = DatoIntervallEntitet.fraOgMedTilOgMed(ya.getAnvistFOM(), ya.getAnvistTOM());
                return kantIKantVurderer.erKantIKant(anvistIntervall, stpIntervall);
            })).getFiltrertYtelser().stream()
            .min(Comparator.comparing(y -> y.getPeriode().getTomDato()))
            .flatMap(Ytelse::getYtelseGrunnlag);
    }

    private Optional<YtelseGrunnlag> finnOverlappendeGrunnlag(BehandlingReferanse behandlingReferanse, LocalDate migrertStp, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return new YtelseFilter(iayGrunnlag.getAktørYtelseFraRegister(behandlingReferanse.getAktørId()))
            //det er riktig at PPN ikke er med her, siden dette bare gjelder migrerte data for PSB
            .filter(y -> y.getYtelseType().equals(FagsakYtelseType.PSB) && y.getKilde().equals(Fagsystem.INFOTRYGD))
            .filter(y -> y.getYtelseAnvist().stream().anyMatch(ya -> {
                var stpIntervall = DatoIntervallEntitet.fraOgMedTilOgMed(migrertStp, migrertStp);
                var anvistIntervall = DatoIntervallEntitet.fraOgMedTilOgMed(ya.getAnvistFOM(), ya.getAnvistTOM());
                return anvistIntervall.inkluderer(migrertStp) || kantIKantVurderer.erKantIKant(anvistIntervall, stpIntervall);
            })).getFiltrertYtelser().stream()
            .min(Comparator.comparing(y -> y.getPeriode().getTomDato()))
            .flatMap(Ytelse::getYtelseGrunnlag);
    }

    private Optional<InputOverstyringPeriode> finnEksisterendeOverstyring(BehandlingReferanse behandlingReferanse, LocalDate migrertStp) {
        return grunnlagRepository.hentGrunnlag(behandlingReferanse.getId())
            .stream()
            .flatMap(gr -> gr.getInputOverstyringPerioder().stream())
            .filter(p -> p.getSkjæringstidspunkt().equals(migrertStp))
            .findFirst();
    }

    private List<OpptjeningAktiviteter.OpptjeningPeriode> finnArbeidsaktiviteterForOverstyring(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsperiode) {
        var opptjeningAktiviteter = finnOpptjeningForBeregningTjeneste(behandlingReferanse).hentEksaktOpptjeningForBeregning(
            behandlingReferanse,
            inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getId()), vilkårsperiode, true);

        return opptjeningAktiviteter.stream()
            .flatMap(a -> a.getOpptjeningPerioder().stream())
            .filter(a -> !a.getPeriode().getTom().isBefore(vilkårsperiode.getFomDato().minusDays(1)))
            .filter(a -> a.getType().equals(OpptjeningAktivitetType.ARBEID))
            .collect(Collectors.groupingBy(a -> a.getArbeidsgiverOrgNummer() != null ? a.getArbeidsgiverOrgNummer() : a.getArbeidsgiverAktørId()))
            .entrySet().stream()
            .flatMap(e -> e.getValue().stream().findFirst().stream())
            .collect(Collectors.toList());
    }

    private OverstyrBeregningAktivitet mapTilOverstyrAktiviteter(LocalDate migrertStp,
                                                                 Optional<InputOverstyringPeriode> overstyrtInputPeriode,
                                                                 OpptjeningAktiviteter.OpptjeningPeriode a,
                                                                 LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForPeriode) {
        var arbeidsgiver = a.getArbeidsgiverOrgNummer() != null ? Arbeidsgiver.virksomhet(a.getArbeidsgiverOrgNummer()) :
            Arbeidsgiver.person(new AktørId(a.getArbeidsgiverAktørId()));
        var inntektsmeldingAktivitet = mapTilInntektsmeldingAktivitet(migrertStp, arbeidsgiver, inntektsmeldingerForPeriode);
        var matchendeOverstyring = finnOverstyringForArbeidgiver(overstyrtInputPeriode, arbeidsgiver);
        var harRefusjonskrav = inntektsmeldingAktivitet.map(OverstyrBeregningAktivitet::getRefusjonPrAar).isPresent();
        var refusjonskravFraIM = inntektsmeldingAktivitet.map(OverstyrBeregningAktivitet::getRefusjonPrAar).orElse(null);
        var startdatoRefusjon = inntektsmeldingAktivitet.map(OverstyrBeregningAktivitet::getStartdatoRefusjon);
        var refusjonOpphører = inntektsmeldingAktivitet.map(OverstyrBeregningAktivitet::getOpphørRefusjon);
        return new OverstyrBeregningAktivitet(
            a.getArbeidsgiverOrgNummer() == null ? null : new OrgNummer(a.getArbeidsgiverOrgNummer()),
            a.getArbeidsgiverAktørId() == null ? null : new AktørId(a.getArbeidsgiverAktørId()),
            finnInntekt(matchendeOverstyring),
            utledKravFraStart(matchendeOverstyring, harRefusjonskrav, refusjonskravFraIM),
            finnStartdato(matchendeOverstyring, startdatoRefusjon),
            utledOpphør(matchendeOverstyring, harRefusjonskrav, refusjonOpphører),
            skalKunneEndreRefusjon(harRefusjonskrav)
        );
    }

    private static boolean skalKunneEndreRefusjon(boolean harRefusjonskrav) {
        return !harRefusjonskrav;
    }

    private static Integer finnInntekt(Optional<InputAktivitetOverstyring> matchendeOverstyring) {
        return matchendeOverstyring.map(InputAktivitetOverstyring::getInntektPrÅr).map(Beløp::getVerdi).map(BigDecimal::intValue).orElse(null);
    }

    private static LocalDate finnStartdato(Optional<InputAktivitetOverstyring> matchendeOverstyring, Optional<LocalDate> startdatoRefusjon) {
        return matchendeOverstyring.flatMap(InputAktivitetOverstyring::getStartdatoRefusjon).orElse(startdatoRefusjon.orElse(null));
    }

    private static Integer utledKravFraStart(Optional<InputAktivitetOverstyring> matchendeOverstyring, boolean harRefusjonskrav, Integer refusjonskravFraIM) {
        return harRefusjonskrav ? refusjonskravFraIM : matchendeOverstyring.map(InputAktivitetOverstyring::getRefusjonPrÅr).map(Beløp::getVerdi).map(BigDecimal::intValue).orElse(null);
    }

    private static LocalDate utledOpphør(Optional<InputAktivitetOverstyring> matchendeOverstyring, boolean harRefusjonskrav, Optional<LocalDate> refusjonOpphører) {
        return harRefusjonskrav ? refusjonOpphører.orElse(null) : matchendeOverstyring.map(InputAktivitetOverstyring::getOpphørRefusjon).orElse(null);
    }

    private static Optional<InputAktivitetOverstyring> finnOverstyringForArbeidgiver(Optional<InputOverstyringPeriode> overstyrtInputPeriode, Arbeidsgiver arbeidsgiver) {
        return overstyrtInputPeriode.stream().flatMap(p -> p.getAktivitetOverstyringer().stream())
            .filter(overstyrt -> overstyrt.getAktivitetStatus().erArbeidstaker() &&
                arbeidsgiver.equals(overstyrt.getArbeidsgiver()))
            .findFirst();
    }

    private static Integer utledRefusjonskravVedStpFraRefusjontidslinje(LocalDate migrertStp, LocalDateTimeline<BigDecimal> refusjonTidslinje) {
        Optional<LocalDateSegment<BigDecimal>> førsteRefusjonssegment = finnFørsteRefusjonsegment(migrertStp, refusjonTidslinje);
        var refusjonskravFraIM = førsteRefusjonssegment
            .map(LocalDateSegment::getValue)
            .map(BigDecimal.valueOf(12)::multiply)
            .map(BigDecimal::intValue);
        return refusjonskravFraIM.orElse(null);
    }

    private static Integer finnInntektFraInntektsmeldingtidslinje(LocalDate migrertStp, LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForArbeidsgiver) {
        var inntektsmeldingerVedStart = inntektsmeldingerForArbeidsgiver.intersection(new LocalDateInterval(migrertStp, migrertStp)).toSegments().iterator().next().getValue();
        var inntekt = inntektsmeldingerVedStart.stream()
            .map(Inntektsmelding::getInntektBeløp)
            .map(Beløp::getVerdi)
            .map(BigDecimal.valueOf(12)::multiply)
            .reduce(BigDecimal::add)
            .map(BigDecimal::intValue)
            .orElse(0);
        return inntekt;
    }

    private static LocalDate utledOpphørRefujonFraRefusjontidslinje(LocalDateTimeline<BigDecimal> refusjonTidslinje) {
        var harRefusjonskrav = !refusjonTidslinje.filterValue(r -> r.compareTo(BigDecimal.ZERO) > 0).isEmpty();
        return harRefusjonskrav ? refusjonTidslinje.filterValue(r -> r.compareTo(BigDecimal.ZERO) > 0)
            .getMaxLocalDate() : null;
    }

    private static Optional<LocalDateSegment<BigDecimal>> finnFørsteRefusjonsegment(LocalDate migrertStp, LocalDateTimeline<BigDecimal> refusjonTidslinje) {
        return refusjonTidslinje.toSegments().stream()
            .filter(s -> s.getValue().compareTo(BigDecimal.ZERO) > 0)
            .filter(s -> s.getTom().isAfter(migrertStp))
            .min(Comparator.comparing(LocalDateSegment::getFom));
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(BehandlingReferanse behandlingReferanse) {
        FagsakYtelseType ytelseType = behandlingReferanse.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjenester, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(BehandlingReferanse behandlingReferanse) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, vilkårsPerioderTilVurderingTjeneste, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandlingReferanse.getFagsakYtelseType() + "], behandlingtype [" + behandlingReferanse.getBehandlingType() + "]"));
    }

}
