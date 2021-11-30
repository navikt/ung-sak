package no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput;

import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningAktivitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningInputPeriode;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrInputForBeregningDto;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrInputForBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class BeregningInputOppdaterer implements AksjonspunktOppdaterer<OverstyrInputForBeregningDto> {


    private BeregningPerioderGrunnlagRepository grunnlagRepository;
    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;


    BeregningInputOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningInputOppdaterer(BeregningPerioderGrunnlagRepository grunnlagRepository,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                    @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                    @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste) {
        this.grunnlagRepository = grunnlagRepository;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(OverstyrInputForBeregningDto dto, AksjonspunktOppdaterParameter param) {
        lagreInputOverstyringer(param.getRef(), dto);
        return OppdateringResultat.utenOverhopp();
    }

    private void lagreInputOverstyringer(BehandlingReferanse ref, OverstyrInputForBeregningDto dto) {
        Long behandlingId = ref.getBehandlingId();
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        FagsakYtelseType fagsakYtelseType = ref.getFagsakYtelseType();
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = getPerioderTilVurderingTjeneste(fagsakYtelseType, ref.getBehandlingType())
            .utled(behandlingId, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var overstyrtePerioder = dto.getPerioder().stream()
            .map(it -> mapPeriode(ref, iayGrunnlag, perioderTilVurdering, it))
            .collect(Collectors.toList());
        grunnlagRepository.lagreInputOverstyringer(behandlingId, overstyrtePerioder);
    }

    @NotNull
    private InputOverstyringPeriode mapPeriode(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, OverstyrBeregningInputPeriode it) {
        var vilkårsperiode = perioderTilVurdering.stream().filter(p -> p.getFomDato().equals(it.getSkjaeringstidspunkt())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Fikk inn periode som ikke er til vurdering i behandlingen"));

        var opptjeningAktiviteter = finnOpptjeningForBeregningTjeneste(ref.getFagsakYtelseType()).hentEksaktOpptjeningForBeregning(ref, iayGrunnlag, vilkårsperiode)
            .orElseThrow()
            .getOpptjeningPerioder()
            .stream()
            .filter(p -> !p.getPeriode().getTom().isBefore(it.getSkjaeringstidspunkt()))
            .collect(Collectors.toList());
        return new InputOverstyringPeriode(it.getSkjaeringstidspunkt(), mapAktiviteter(it.getAktivitetliste(), opptjeningAktiviteter));
    }

    private List<InputAktivitetOverstyring> mapAktiviteter(List<OverstyrBeregningAktivitet> aktivitetliste, List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningAktiviteter) {
        return aktivitetliste.stream()
            .map(a -> mapAktivitet(opptjeningAktiviteter, a))
            .collect(Collectors.toList());
    }

    @NotNull
    private InputAktivitetOverstyring mapAktivitet(List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningAktiviteter, OverstyrBeregningAktivitet a) {
        List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningsaktivitetForArbeidsgiver = opptjeningAktiviteter.stream().filter(oa -> Objects.equals(oa.getArbeidsgiverOrgNummer(), finnOrgnrString(a)) ||
                Objects.equals(oa.getArbeidsgiverAktørId(), finnAktørIdString(a)))
            .collect(Collectors.toList());
        if (opptjeningsaktivitetForArbeidsgiver.isEmpty()) {
            throw new IllegalArgumentException("Fant ikke aktivitet på skjæringstidspunkt: " + a);
        }
        finnMinMaksPeriode(opptjeningsaktivitetForArbeidsgiver);
        return new InputAktivitetOverstyring(
            mapArbeidsgiver(a),
            mapBeløp(a.getInntektPrAar()),
            mapBeløp(a.getRefusjonPrAar()),
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

    private DatoIntervallEntitet mapPeriode(Periode periode) {
        return periode.getTom() == null ? DatoIntervallEntitet.fraOgMed(periode.getFom()) : DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
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

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType type) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, fagsakYtelseType, type)
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + fagsakYtelseType + "]"));
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(FagsakYtelseType ytelseType) {
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjeneste, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
        return tjeneste;
    }

}
