package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
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
    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
    private PåTversAvHelgErKantIKantVurderer kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();

    public OverstyrInputBeregningTjeneste() {
    }

    @Inject
    public OverstyrInputBeregningTjeneste(FagsakRepository fagsakRepository,
                                          @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjenester,
                                          BeregningPerioderGrunnlagRepository grunnlagRepository,
                                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                          @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                          @Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning) {
        this.fagsakRepository = fagsakRepository;
        this.opptjeningForBeregningTjenester = opptjeningForBeregningTjenester;
        this.grunnlagRepository = grunnlagRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
    }

    public List<OverstyrBeregningInputPeriode> getPerioderForInputOverstyring(BehandlingReferanse behandlingReferanse) {
        List<SakInfotrygdMigrering> sakInfotrygdMigreringer = fagsakRepository.hentSakInfotrygdMigreringer(behandlingReferanse.getFagsakId());
        return sakInfotrygdMigreringer.stream().map(sakInfotrygdMigrering -> {
            LocalDate migrertStp = sakInfotrygdMigrering.getSkjæringstidspunkt();
            List<OpptjeningAktiviteter.OpptjeningPeriode> arbeidsaktiviteter = finnArbeidsaktiviteterForOverstyring(behandlingReferanse, migrertStp);
            Optional<InputOverstyringPeriode> overstyrtInputPeriode = finnEksisterendeOverstyring(behandlingReferanse, migrertStp);
            var inntektsmeldingerForSak = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(behandlingReferanse.getSaksnummer());
            var imTjeneste = finnInntektsmeldingForBeregningTjeneste(behandlingReferanse);
            var inntektsmeldingerForPeriode = imTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldingerForSak, DatoIntervallEntitet.fraOgMedTilOgMed(migrertStp, migrertStp));
            var overstyrteAktiviteter = arbeidsaktiviteter.stream()
                .map(a -> mapTilOverstyrAktiviteter(migrertStp, overstyrtInputPeriode, a, inntektsmeldingerForPeriode))
                .collect(Collectors.toList());
            var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getId());
            var ytelseGrunnlag = finnYtelseGrunnlagForMigrering(behandlingReferanse, migrertStp, iayGrunnlag);
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

    private List<OpptjeningAktiviteter.OpptjeningPeriode> finnArbeidsaktiviteterForOverstyring(BehandlingReferanse behandlingReferanse, LocalDate migrertStp) {
        var perioderTilVurdering = getPerioderTilVurderingTjeneste(behandlingReferanse).utled(behandlingReferanse.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var vilkårsperiode = perioderTilVurdering.stream()
            .filter(p -> p.getFomDato().equals(migrertStp))
            .findFirst().orElseThrow(() -> new IllegalStateException("Fant ingen periode for sakinfotrygdmigrering"));

        var opptjeningAktiviteter = finnOpptjeningForBeregningTjeneste(behandlingReferanse).hentEksaktOpptjeningForBeregning(
            behandlingReferanse,
            inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getId()), vilkårsperiode);

        return opptjeningAktiviteter.stream()
            .flatMap(a -> a.getOpptjeningPerioder().stream())
            .filter(a -> !a.getPeriode().getTom().isBefore(migrertStp.minusDays(1)))
            .filter(a -> a.getType().equals(OpptjeningAktivitetType.ARBEID))
            .collect(Collectors.groupingBy(a -> a.getArbeidsgiverOrgNummer() != null ? a.getArbeidsgiverOrgNummer() : a.getArbeidsgiverAktørId()))
            .entrySet().stream()
            .flatMap(e -> e.getValue().stream().findFirst().stream())
            .collect(Collectors.toList());
    }

    private OverstyrBeregningAktivitet mapTilOverstyrAktiviteter(LocalDate migrertStp, Optional<InputOverstyringPeriode> overstyrtInputPeriode, OpptjeningAktiviteter.OpptjeningPeriode a, List<Inntektsmelding> inntektsmeldingerForPeriode) {
        var matchendeOverstyring = overstyrtInputPeriode.stream().flatMap(p -> p.getAktivitetOverstyringer().stream())
            .filter(overstyrt -> overstyrt.getAktivitetStatus().erArbeidstaker() &&
                matcherArbeidsgiver(a, overstyrt.getArbeidsgiver()))
            .findFirst();
        var inntektsmeldingerForArbeidsgiver = inntektsmeldingerForPeriode.stream().filter(i -> matcherArbeidsgiver(a, i.getArbeidsgiver())).collect(Collectors.toList());

        var harRefusjonskrav = inntektsmeldingerForArbeidsgiver.stream().anyMatch(im -> im.getRefusjonBeløpPerMnd() != null && !im.getRefusjonBeløpPerMnd().erNullEllerNulltall() || im.getEndringerRefusjon().size() > 0);
        var refusjonskravFraIM = inntektsmeldingerForArbeidsgiver.stream().map(Inntektsmelding::getRefusjonBeløpPerMnd)
            .filter(Objects::nonNull)
            .map(Beløp::getVerdi)
            .map(BigDecimal.valueOf(12)::multiply)
            .reduce(BigDecimal::add)
            .map(BigDecimal::intValue)
            .orElse(0);
        var refusjonOpphører = inntektsmeldingerForArbeidsgiver.stream().map(Inntektsmelding::getRefusjonOpphører)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder());
        return new OverstyrBeregningAktivitet(
            a.getArbeidsgiverOrgNummer() == null ? null : new OrgNummer(a.getArbeidsgiverOrgNummer()),
            a.getArbeidsgiverAktørId() == null ? null : new AktørId(a.getArbeidsgiverAktørId()),
            matchendeOverstyring.map(InputAktivitetOverstyring::getInntektPrÅr).map(Beløp::getVerdi).map(BigDecimal::intValue).orElse(null),
            harRefusjonskrav ? refusjonskravFraIM : matchendeOverstyring.map(InputAktivitetOverstyring::getRefusjonPrÅr).map(Beløp::getVerdi).map(BigDecimal::intValue).orElse(null),
            harRefusjonskrav ? refusjonOpphører.orElse(null) : matchendeOverstyring.map(InputAktivitetOverstyring::getOpphørRefusjon).orElse(null),
            !harRefusjonskrav
        );
    }

    private boolean matcherArbeidsgiver(OpptjeningAktiviteter.OpptjeningPeriode a, Arbeidsgiver arbeidsgiver) {
        return Objects.equals(arbeidsgiver.getArbeidsgiverOrgnr(), a.getArbeidsgiverOrgNummer()) &&
            Objects.equals(getAktørIdString(arbeidsgiver), a.getArbeidsgiverAktørId());
    }

    private String getAktørIdString(Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver.getArbeidsgiverAktørId() == null ? null :
            arbeidsgiver.getArbeidsgiverAktørId().getAktørId();
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

    private InntektsmeldingerRelevantForBeregning finnInntektsmeldingForBeregningTjeneste(BehandlingReferanse behandlingReferanse) {
        FagsakYtelseType ytelseType = behandlingReferanse.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(inntektsmeldingerRelevantForBeregning, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + InntektsmeldingerRelevantForBeregning.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }


}
