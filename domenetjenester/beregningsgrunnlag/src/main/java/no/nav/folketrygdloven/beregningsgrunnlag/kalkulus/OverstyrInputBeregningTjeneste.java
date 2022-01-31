package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.iay.modell.YtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
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
    private PåTversAvHelgErKantIKantVurderer kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();

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

            var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
            var ytelseGrunnlag = finnYtelseGrunnlagForMigrering(behandling, migrertStp, iayGrunnlag);
            var harKategoriNæring = harNæring(ytelseGrunnlag);
            var harKategoriFrilans = harFrilans(ytelseGrunnlag);
            return new OverstyrBeregningInputPeriode(migrertStp, overstyrteAktiviteter, harKategoriNæring, harKategoriFrilans);
        }).collect(Collectors.toList());
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


    private Optional<YtelseGrunnlag> finnYtelseGrunnlagForMigrering(Behandling behandling, LocalDate migrertStp, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var ytelseGrunnlagListe = new YtelseFilter(iayGrunnlag.getAktørYtelseFraRegister(behandling.getAktørId()))
            .filter(y -> y.getYtelseType().equals(FagsakYtelseType.PSB) && y.getKilde().equals(Fagsystem.INFOTRYGD))
            .filter(y -> y.getYtelseAnvist().stream().anyMatch(ya -> {
                var stpIntervall = DatoIntervallEntitet.fraOgMedTilOgMed(migrertStp, migrertStp);
                var anvistIntervall = DatoIntervallEntitet.fraOgMedTilOgMed(ya.getAnvistFOM(), ya.getAnvistTOM());
                return anvistIntervall.inkluderer(migrertStp) || kantIKantVurderer.erKantIKant(anvistIntervall, stpIntervall);
            })).getFiltrertYtelser().stream()
            .flatMap(y -> y.getYtelseGrunnlag().stream())
            .collect(Collectors.toList());
        if (ytelseGrunnlagListe.size() > 1) {
            throw new IllegalStateException("Fant mer enn ett ytelsegrunnlag fra infotrygd for PSB. Fant " + ytelseGrunnlagListe.size());
        } else if (ytelseGrunnlagListe.size() == 1) {
            return Optional.of(ytelseGrunnlagListe.get(0));
        }
        return Optional.empty();
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
            .filter(a -> !a.getPeriode().getTom().isBefore(migrertStp))
            .filter(a -> a.getType().equals(OpptjeningAktivitetType.ARBEID))
            .collect(Collectors.groupingBy(a -> a.getArbeidsgiverOrgNummer() != null ? a.getArbeidsgiverOrgNummer() : a.getArbeidsgiverAktørId()))
            .entrySet().stream()
            .flatMap(e -> e.getValue().stream().findFirst().stream())
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
            matchendeOverstyring.map(InputAktivitetOverstyring::getRefusjonPrÅr).map(Beløp::getVerdi).map(BigDecimal::intValue).orElse(null),
            matchendeOverstyring.map(InputAktivitetOverstyring::getOpphørRefusjon).orElse(null)
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
