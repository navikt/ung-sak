package no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OverstyrInputBeregningTjeneste;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningAktivitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningInputPeriode;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrInputForBeregningDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;
import no.nav.k9.sikkerhet.oidc.token.bruker.BrukerTokenProvider;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrInputForBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class BeregningInputOppdaterer implements AksjonspunktOppdaterer<OverstyrInputForBeregningDto> {


    private BeregningPerioderGrunnlagRepository grunnlagRepository;
    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;
    private BrukerTokenProvider brukerTokenProvider;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private OverstyrInputBeregningTjeneste overstyrInputBeregningTjeneste;
    private Environment environment;


    BeregningInputOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningInputOppdaterer(BeregningPerioderGrunnlagRepository grunnlagRepository,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                    @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                    @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste,
                                    BrukerTokenProvider brukerTokenProvider,
                                    ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag,
                                    HistorikkTjenesteAdapter historikkTjenesteAdapter, OverstyrInputBeregningTjeneste overstyrInputBeregningTjeneste) {
        this.grunnlagRepository = grunnlagRepository;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.brukerTokenProvider = brukerTokenProvider;
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.overstyrInputBeregningTjeneste = overstyrInputBeregningTjeneste;
        this.environment = Environment.current();
    }

    @Override
    public OppdateringResultat oppdater(OverstyrInputForBeregningDto dto, AksjonspunktOppdaterParameter param) {
        if (!harTillatelseTilÅLøseAksjonspunkt()) {
            throw new ManglerTilgangException("K9-IF-01", "Har ikke tilgang til å løse aksjonspunkt.");
        }
        lagreInputOverstyringer(param.getRef(), dto);
        return OppdateringResultat.nyttResultat();
    }

    private boolean harTillatelseTilÅLøseAksjonspunkt() {
        return Arrays.stream(environment.getProperty("INFOTRYGD_MIGRERING_TILLATELSER", "").split(",")).anyMatch(id -> id.equalsIgnoreCase(brukerTokenProvider.getUserId()));
    }

    private void lagreInputOverstyringer(BehandlingReferanse ref, OverstyrInputForBeregningDto dto) {
        var inputPerioder = overstyrInputBeregningTjeneste.getPerioderForInputOverstyring(ref);
        Long behandlingId = ref.getBehandlingId();
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        FagsakYtelseType fagsakYtelseType = ref.getFagsakYtelseType();
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = getPerioderTilVurderingTjeneste(fagsakYtelseType, ref.getBehandlingType())
            .utled(behandlingId, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var overstyrtePerioder = dto.getPerioder().stream()
            .filter(it -> !it.getAktivitetliste().isEmpty())
            .map(it -> mapPeriode(ref, iayGrunnlag, perioderTilVurdering, finnInputPeriode(inputPerioder, it), it))
            .collect(Collectors.toList());

        lagHistorikk(behandlingId, overstyrtePerioder);

        grunnlagRepository.lagreInputOverstyringer(behandlingId, overstyrtePerioder);
    }

    private OverstyrBeregningInputPeriode finnInputPeriode(List<OverstyrBeregningInputPeriode> perioderForInputOverstyring, OverstyrBeregningInputPeriode it) {
        return perioderForInputOverstyring.stream().filter(p -> p.getSkjaeringstidspunkt().equals(it.getSkjaeringstidspunkt())).findFirst().orElseThrow();
    }

    private void lagHistorikk(Long behandlingId, List<InputOverstyringPeriode> overstyrtePerioder) {
        HistorikkInnslagTekstBuilder tekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        var inputOverstyringPerioder = grunnlagRepository.hentGrunnlag(behandlingId)
            .map(BeregningsgrunnlagPerioderGrunnlag::getInputOverstyringPerioder)
            .orElse(Collections.emptyList());
        tekstBuilder.medSkjermlenke(SkjermlenkeType.OVERSTYR_INPUT_BEREGNING);
        overstyrtePerioder.forEach(p -> lagHistorikk(p, tekstBuilder, inputOverstyringPerioder));
    }

    private void lagHistorikk(InputOverstyringPeriode p,
                              HistorikkInnslagTekstBuilder tekstBuilder,
                              List<InputOverstyringPeriode> eksisterende) {
        tekstBuilder.medNavnOgGjeldendeFra(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD, null, p.getSkjæringstidspunkt());
        var eksisterendeOverstyrtperiode = eksisterende.stream().filter(periode -> periode.getSkjæringstidspunkt().equals(p.getSkjæringstidspunkt()))
            .findFirst();
        p.getAktivitetOverstyringer().forEach(a -> lagAktivitetHistorikk(a, tekstBuilder, eksisterendeOverstyrtperiode));
        tekstBuilder.ferdigstillHistorikkinnslagDel();
    }

    private void lagAktivitetHistorikk(InputAktivitetOverstyring a, HistorikkInnslagTekstBuilder tekstBuilder, Optional<InputOverstyringPeriode> eksisterendeOverstyrtperiode) {
        if (a.getArbeidsgiver() != null) {
            var eksisterende = eksisterendeOverstyrtperiode.stream().flatMap(p -> p.getAktivitetOverstyringer().stream())
                .filter(eksisterendeAktivitet -> eksisterendeAktivitet.getArbeidsgiver().equals(a.getArbeidsgiver()))
                .findFirst();
            String arbeidsforholdInfo = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(
                a.getArbeidsgiver(),
                InternArbeidsforholdRef.nullRef(),
                Collections.emptyList());
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD,
                arbeidsforholdInfo,
                finnFraBeløp(a.getInntektPrÅr(), eksisterende.map(InputAktivitetOverstyring::getInntektPrÅr)),
                a.getInntektPrÅr().getVerdi());
            if (a.getRefusjonPrÅr() != null) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.NYTT_REFUSJONSKRAV,
                    arbeidsforholdInfo,
                    finnFraBeløp(a.getRefusjonPrÅr(), eksisterende.map(InputAktivitetOverstyring::getRefusjonPrÅr)),
                    a.getRefusjonPrÅr().getVerdi());
            }
        }
    }

    private BigDecimal finnFraBeløp(Beløp fastsatt, Optional<Beløp> fra) {
        var forrige = fra.map(Beløp::getVerdi).orElse(null);
        return forrige == null || forrige.compareTo(fastsatt.getVerdi()) == 0 ? null : forrige;
    }

    private InputOverstyringPeriode mapPeriode(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag,
                                               NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                               OverstyrBeregningInputPeriode inputPeriode,
                                               OverstyrBeregningInputPeriode overstyrtPeriode) {
        var vilkårsperiode = perioderTilVurdering.stream().filter(p -> p.getFomDato().equals(overstyrtPeriode.getSkjaeringstidspunkt())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Fikk inn periode som ikke er til vurdering i behandlingen"));

        var opptjeningAktiviteter = finnOpptjeningForBeregningTjeneste(ref.getFagsakYtelseType()).hentEksaktOpptjeningForBeregning(ref, iayGrunnlag, vilkårsperiode)
            .orElseThrow()
            .getOpptjeningPerioder()
            .stream()
            .filter(p -> !p.getPeriode().getTom().isBefore(overstyrtPeriode.getSkjaeringstidspunkt().minusDays(1)))
            .collect(Collectors.toList());
        return new InputOverstyringPeriode(overstyrtPeriode.getSkjaeringstidspunkt(), mapAktiviteter(overstyrtPeriode.getAktivitetliste(), inputPeriode.getAktivitetliste(), opptjeningAktiviteter));
    }

    private List<InputAktivitetOverstyring> mapAktiviteter(List<OverstyrBeregningAktivitet> aktivitetliste, List<OverstyrBeregningAktivitet> inputAktiviteter, List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningAktiviteter) {
        return aktivitetliste.stream()
            .map(a -> mapAktivitet(opptjeningAktiviteter, finnInputAktivitet(inputAktiviteter, a), a))
            .collect(Collectors.toList());
    }

    private OverstyrBeregningAktivitet finnInputAktivitet(List<OverstyrBeregningAktivitet> inputAktiviteter, OverstyrBeregningAktivitet a) {
        return inputAktiviteter.stream().filter(akt -> Objects.equals(akt.getArbeidsgiverAktørId(), a.getArbeidsgiverAktørId()) && Objects.equals(akt.getArbeidsgiverOrgnr(), a.getArbeidsgiverOrgnr())).findFirst().orElseThrow();
    }

    private InputAktivitetOverstyring mapAktivitet(List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningAktiviteter,
                                                   OverstyrBeregningAktivitet inputAktivitet,
                                                   OverstyrBeregningAktivitet overstyrtAktivitet) {
        List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningsaktivitetForArbeidsgiver = opptjeningAktiviteter.stream().filter(oa -> Objects.equals(oa.getArbeidsgiverOrgNummer(), finnOrgnrString(overstyrtAktivitet)) ||
                Objects.equals(oa.getArbeidsgiverAktørId(), finnAktørIdString(overstyrtAktivitet)))
            .collect(Collectors.toList());
        if (opptjeningsaktivitetForArbeidsgiver.isEmpty()) {
            throw new IllegalArgumentException("Fant ikke aktivitet på skjæringstidspunkt: " + overstyrtAktivitet);
        }
        finnMinMaksPeriode(opptjeningsaktivitetForArbeidsgiver);
        return new InputAktivitetOverstyring(
            mapArbeidsgiver(overstyrtAktivitet),
            mapBeløp(overstyrtAktivitet.getInntektPrAar()),
            inputAktivitet.getSkalKunneEndreRefusjon() ? mapBeløp(overstyrtAktivitet.getRefusjonPrAar()) : null,
            inputAktivitet.getSkalKunneEndreRefusjon() ? overstyrtAktivitet.getOpphørRefusjon() : null,
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
