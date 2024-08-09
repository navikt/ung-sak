package no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.FiltrerInntektsmeldingForBeregningInputOverstyring;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OverstyrInputBeregningTjeneste;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningAktivitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningInputPeriode;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrInputForBeregningDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

@Dependent
public class BeregningInputLagreTjeneste {

    private final BeregningPerioderGrunnlagRepository grunnlagRepository;
    private final Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;
    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;
    private final FiltrerInntektsmeldingForBeregningInputOverstyring filtrerInntektsmeldinger;

    @Inject
    public BeregningInputLagreTjeneste(BeregningPerioderGrunnlagRepository grunnlagRepository,
                                       InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                       @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                       @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste,
                                       FiltrerInntektsmeldingForBeregningInputOverstyring filtrerInntektsmeldinger) {
        this.grunnlagRepository = grunnlagRepository;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.filtrerInntektsmeldinger = filtrerInntektsmeldinger;
    }


    public void lagreInputOverstyringer(BehandlingReferanse ref, OverstyrInputForBeregningDto dto) {
        Long behandlingId = ref.getBehandlingId();
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        FagsakYtelseType fagsakYtelseType = ref.getFagsakYtelseType();
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = getPerioderTilVurderingTjeneste(fagsakYtelseType, ref.getBehandlingType())
            .utled(behandlingId, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var inntektsmeldingerForSak = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());
        var overstyrtePerioder = dto.getPerioder().stream()
            .filter(it -> !it.getAktivitetliste().isEmpty())
            .map(it -> mapPeriode(ref, iayGrunnlag, perioderTilVurdering, it, inntektsmeldingerForSak))
            .collect(Collectors.toList());
        grunnlagRepository.lagreInputOverstyringer(behandlingId, overstyrtePerioder);
    }

    private InputOverstyringPeriode mapPeriode(BehandlingReferanse ref,
                                               InntektArbeidYtelseGrunnlag iayGrunnlag,
                                               NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                               OverstyrBeregningInputPeriode overstyrtPeriode,
                                               Set<Inntektsmelding> inntektsmeldingerForSak) {
        var vilkårsperiode = perioderTilVurdering.stream().filter(p -> p.getFomDato().equals(overstyrtPeriode.getSkjaeringstidspunkt())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Fikk inn periode som ikke er til vurdering i behandlingen"));

        var inntektsmeldingerForPeriode = finnInntektsmeldingerForPeriode(ref, inntektsmeldingerForSak, vilkårsperiode);
        var opptjeningAktiviteter = finnOpptjeningForBeregningTjeneste(ref.getFagsakYtelseType()).hentEksaktOpptjeningForBeregning(ref, iayGrunnlag, vilkårsperiode)
            .orElseThrow()
            .getOpptjeningPerioder()
            .stream()
            .filter(p -> !p.getPeriode().getTom().isBefore(overstyrtPeriode.getSkjaeringstidspunkt().minusDays(1)))
            .collect(Collectors.toList());
        var aktivitetOverstyringer = mapAktiviteter(overstyrtPeriode.getAktivitetliste(),
            overstyrtPeriode.getSkjaeringstidspunkt(),
            opptjeningAktiviteter, inntektsmeldingerForPeriode);
        return new InputOverstyringPeriode(overstyrtPeriode.getSkjaeringstidspunkt(), aktivitetOverstyringer);
    }

    private List<InputAktivitetOverstyring> mapAktiviteter(List<OverstyrBeregningAktivitet> aktivitetliste,
                                                           LocalDate skjaeringstidspunkt,
                                                           List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningAktiviteter,
                                                           LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForPeriode) {
        return aktivitetliste.stream()
            .map(a -> mapAktivitet(opptjeningAktiviteter, skjaeringstidspunkt, a, inntektsmeldingerForPeriode))
            .collect(Collectors.toList());
    }


    private InputAktivitetOverstyring mapAktivitet(List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningAktiviteter,
                                                   LocalDate skjaeringstidspunkt,
                                                   OverstyrBeregningAktivitet overstyrtAktivitet,
                                                   LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForPeriode) {
        List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningsaktivitetForArbeidsgiver = opptjeningAktiviteter.stream().filter(oa -> Objects.equals(oa.getArbeidsgiverOrgNummer(), finnOrgnrString(overstyrtAktivitet)) ||
                Objects.equals(oa.getArbeidsgiverAktørId(), finnAktørIdString(overstyrtAktivitet)))
            .collect(Collectors.toList());
        if (opptjeningsaktivitetForArbeidsgiver.isEmpty()) {
            throw new IllegalArgumentException("Fant ikke aktivitet på skjæringstidspunkt: " + overstyrtAktivitet);
        }

        var arbeidsgiver = overstyrtAktivitet.getArbeidsgiverOrgnr() != null ? Arbeidsgiver.virksomhet(overstyrtAktivitet.getArbeidsgiverOrgnr()) :
            Arbeidsgiver.person(new AktørId(overstyrtAktivitet.getArbeidsgiverAktørId().getAktørId()));
        var aktivitetFraInntektsmelding = OverstyrInputBeregningTjeneste.mapTilInntektsmeldingAktivitet(skjaeringstidspunkt, arbeidsgiver, inntektsmeldingerForPeriode);

        var startdatoRefusjonFraInntektsmelding = aktivitetFraInntektsmelding.map(OverstyrBeregningAktivitet::getStartdatoRefusjon);
        if (startdatoRefusjonFraInntektsmelding.isPresent() && overstyrtAktivitet.getStartdatoRefusjon() != null && overstyrtAktivitet.getStartdatoRefusjon().isBefore(startdatoRefusjonFraInntektsmelding.get())) {
            throw new IllegalStateException("Kan ikke sette startdato for refusjon tidligere enn oppgitt fra arbeidsgiver");
        }

        return new InputAktivitetOverstyring(
            mapArbeidsgiver(overstyrtAktivitet),
            mapBeløp(overstyrtAktivitet.getInntektPrAar()),
            overstyrtAktivitet.getSkalKunneEndreRefusjon() ? mapBeløp(overstyrtAktivitet.getRefusjonPrAar()) : null,
            overstyrtAktivitet.getStartdatoRefusjon(),
            overstyrtAktivitet.getSkalKunneEndreRefusjon() ? overstyrtAktivitet.getOpphørRefusjon() : null,
            AktivitetStatus.ARBEIDSTAKER,
            finnMinMaksPeriode(opptjeningsaktivitetForArbeidsgiver));
    }

    private DatoIntervallEntitet finnMinMaksPeriode(List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningsaktivitetForArbeidsgiver) {
        var førsteFom = opptjeningsaktivitetForArbeidsgiver.stream()
            .map(OpptjeningAktiviteter.OpptjeningPeriode::getPeriode)
            .map(no.nav.k9.sak.typer.Periode::getFom)
            .min(Comparator.naturalOrder())
            .orElseThrow();
        var sisteTom = opptjeningsaktivitetForArbeidsgiver.stream()
            .map(OpptjeningAktiviteter.OpptjeningPeriode::getPeriode)
            .map(no.nav.k9.sak.typer.Periode::getTom)
            .max(Comparator.naturalOrder())
            .orElseThrow();
        return DatoIntervallEntitet.fraOgMedTilOgMed(førsteFom, sisteTom);
    }

    private String finnAktørIdString(OverstyrBeregningAktivitet a) {
        return a.getArbeidsgiverAktørId() == null ? null : a.getArbeidsgiverAktørId().getAktørId();
    }

    private String finnOrgnrString(OverstyrBeregningAktivitet a) {
        return a.getArbeidsgiverOrgnr() == null ? null : a.getArbeidsgiverOrgnr().getId();
    }

    private Beløp mapBeløp(Integer beløp) {
        return beløp != null ? new Beløp(beløp) : null;
    }

    private Arbeidsgiver mapArbeidsgiver(OverstyrBeregningAktivitet a) {
        if (a.getArbeidsgiverOrgnr() == null && a.getArbeidsgiverAktørId() == null) {
            return null;
        }
        return a.getArbeidsgiverOrgnr() != null ? Arbeidsgiver.virksomhet(a.getArbeidsgiverOrgnr()) : Arbeidsgiver.person(a.getArbeidsgiverAktørId());
    }

    private LocalDateTimeline<Set<Inntektsmelding>> finnInntektsmeldingerForPeriode(BehandlingReferanse behandlingReferanse, Set<Inntektsmelding> inntektsmeldingerForSak, DatoIntervallEntitet vilkårsperiode) {
        return filtrerInntektsmeldinger.finnGyldighetstidslinjeForInntektsmeldinger(behandlingReferanse, inntektsmeldingerForSak, vilkårsperiode);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType type) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, fagsakYtelseType, type)
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + fagsakYtelseType + "]"));
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjeneste, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }


}
