package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningAktivitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningInputPeriode;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
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

    public OverstyrInputBeregningTjeneste() {
    }

    @Inject
    public OverstyrInputBeregningTjeneste(FagsakRepository fagsakRepository,
                                          @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjenester,
                                          BeregningPerioderGrunnlagRepository grunnlagRepository,
                                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                          @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.opptjeningForBeregningTjenester = opptjeningForBeregningTjenester;
        this.grunnlagRepository = grunnlagRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    public List<OverstyrBeregningInputPeriode> getPerioderForInputOverstyring(Behandling behandling) {
        List<SakInfotrygdMigrering> sakInfotrygdMigreringer = fagsakRepository.hentSakInfotrygdMigreringer(behandling.getFagsakId());
        return sakInfotrygdMigreringer.stream().map(sakInfotrygdMigrering -> {
            LocalDate migrertStp = sakInfotrygdMigrering.getSkjæringstidspunkt();
            List<OpptjeningAktiviteter.OpptjeningPeriode> arbeidsaktiviteter = finnArbeidsaktiviteterForOverstyring(behandling, migrertStp);
            Optional<InputOverstyringPeriode> overstyrtInputPeriode = finnEksisterendeOverstyring(behandling, migrertStp);
            var overstyrteAktiviteter = arbeidsaktiviteter.stream()
                .map(a -> mapTilOverstyrAktiviteter(overstyrtInputPeriode, a))
                .collect(Collectors.toList());
            return new OverstyrBeregningInputPeriode(migrertStp, overstyrteAktiviteter);
        }).collect(Collectors.toList());
    }

    private Optional<InputOverstyringPeriode> finnEksisterendeOverstyring(Behandling behandling, LocalDate migrertStp) {
        return grunnlagRepository.hentGrunnlag(behandling.getId())
            .stream()
            .flatMap(gr -> gr.getInputOverstyringPerioder().stream())
            .filter(p -> p.getSkjæringstidspunkt().equals(migrertStp))
            .findFirst();
    }

    private List<OpptjeningAktiviteter.OpptjeningPeriode> finnArbeidsaktiviteterForOverstyring(Behandling behandling, LocalDate migrertStp) {
        var perioderTilVurdering = getPerioderTilVurderingTjeneste(behandling).utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var vilkårsperiode = perioderTilVurdering.stream()
            .filter(p -> p.getFomDato().equals(migrertStp))
            .findFirst().orElseThrow(() -> new IllegalStateException("Fant ingen periode for sakinfotrygdmigrering"));

        var opptjeningAktiviteter = finnOpptjeningForBeregningTjeneste(behandling).hentEksaktOpptjeningForBeregning(
            BehandlingReferanse.fra(behandling),
            inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId()), vilkårsperiode);

        return opptjeningAktiviteter.stream()
            .flatMap(a -> a.getOpptjeningPerioder().stream())
            .filter(a -> a.getType().equals(OpptjeningAktivitetType.ARBEID))
            .collect(Collectors.toList());
    }

    private OverstyrBeregningAktivitet mapTilOverstyrAktiviteter(Optional<InputOverstyringPeriode> overstyrtInputPeriode, OpptjeningAktiviteter.OpptjeningPeriode a) {
        var matchendeOverstyring = overstyrtInputPeriode.stream().flatMap(p -> p.getAktivitetOverstyringer().stream())
            .filter(overstyrt -> overstyrt.getAktivitetStatus().erArbeidstaker() &&
                matcherArbeidsgiver(overstyrt, a))
            .findFirst();
        return new OverstyrBeregningAktivitet(
            a.getArbeidsgiverOrgNummer() == null ? null : new OrgNummer(a.getArbeidsgiverOrgNummer()),
            a.getArbeidsgiverAktørId() == null ? null : new AktørId(a.getArbeidsgiverAktørId()),
            matchendeOverstyring.map(InputAktivitetOverstyring::getInntektPrÅr).map(Beløp::getVerdi).map(BigDecimal::intValue).orElse(null),
            matchendeOverstyring.map(InputAktivitetOverstyring::getRefusjonPrÅr).map(Beløp::getVerdi).map(BigDecimal::intValue).orElse(null)
        );
    }

    private boolean matcherArbeidsgiver(InputAktivitetOverstyring overstyrt, OpptjeningAktiviteter.OpptjeningPeriode a) {
        return Objects.equals(overstyrt.getArbeidsgiver().getArbeidsgiverOrgnr(), a.getArbeidsgiverOrgNummer()) &&
            Objects.equals(getAktørIdString(overstyrt), a.getArbeidsgiverAktørId());
    }

    private String getAktørIdString(InputAktivitetOverstyring overstyrt) {
        return overstyrt.getArbeidsgiver().getArbeidsgiverAktørId() == null ? null :
            overstyrt.getArbeidsgiver().getArbeidsgiverAktørId().getAktørId();
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(Behandling behandling) {
        FagsakYtelseType ytelseType = behandling.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjenester, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, vilkårsPerioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }


}
