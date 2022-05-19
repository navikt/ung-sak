package no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
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
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningAktivitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningInputPeriode;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrInputForBeregningDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
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
    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
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
                                    HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                    @Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning) {
        this.grunnlagRepository = grunnlagRepository;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.brukerTokenProvider = brukerTokenProvider;
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
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
        Long behandlingId = ref.getBehandlingId();
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        FagsakYtelseType fagsakYtelseType = ref.getFagsakYtelseType();
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = getPerioderTilVurderingTjeneste(fagsakYtelseType, ref.getBehandlingType())
            .utled(behandlingId, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var overstyrtePerioder = dto.getPerioder().stream()
            .filter(it -> !it.getAktivitetliste().isEmpty())
            .map(it -> mapPeriode(ref, iayGrunnlag, perioderTilVurdering, it))
            .collect(Collectors.toList());

        lagHistorikk(behandlingId, overstyrtePerioder);

        grunnlagRepository.lagreInputOverstyringer(behandlingId, overstyrtePerioder);
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

    private InputOverstyringPeriode mapPeriode(BehandlingReferanse ref,
                                               InntektArbeidYtelseGrunnlag iayGrunnlag,
                                               NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                               OverstyrBeregningInputPeriode overstyrtPeriode) {
        var vilkårsperiode = perioderTilVurdering.stream().filter(p -> p.getFomDato().equals(overstyrtPeriode.getSkjaeringstidspunkt())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Fikk inn periode som ikke er til vurdering i behandlingen"));

        var inntektsmeldingerForPeriode = finnInntektsmeldingerForPeriode(ref, overstyrtPeriode.getSkjaeringstidspunkt());
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
                                                           List<Inntektsmelding> inntektsmeldingerForPeriode) {
        return aktivitetliste.stream()
            .map(a -> mapAktivitet(opptjeningAktiviteter, skjaeringstidspunkt, a, inntektsmeldingerForPeriode))
            .collect(Collectors.toList());
    }


    private InputAktivitetOverstyring mapAktivitet(List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningAktiviteter,
                                                   LocalDate skjaeringstidspunkt,
                                                   OverstyrBeregningAktivitet overstyrtAktivitet,
                                                   List<Inntektsmelding> inntektsmeldingerForPeriode) {
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
        if (startdatoRefusjonFraInntektsmelding.isPresent() && overstyrtAktivitet.getStartdatoRefusjon().isBefore(startdatoRefusjonFraInntektsmelding.get())) {
            throw new IllegalStateException("Kan ikke sette startdato for refusjon tidligere enn oppgitt fra arbeidsgiver");
        }

        var skalKunneEndreRefusjon = aktivitetFraInntektsmelding.map(OverstyrBeregningAktivitet::getSkalKunneEndreRefusjon).orElse(true);
        return new InputAktivitetOverstyring(
            mapArbeidsgiver(overstyrtAktivitet),
            mapBeløp(overstyrtAktivitet.getInntektPrAar()),
            skalKunneEndreRefusjon ? mapBeløp(overstyrtAktivitet.getRefusjonPrAar()) : null,
            overstyrtAktivitet.getStartdatoRefusjon(),
            skalKunneEndreRefusjon ? overstyrtAktivitet.getOpphørRefusjon() : null,
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

    private List<Inntektsmelding> finnInntektsmeldingerForPeriode(BehandlingReferanse behandlingReferanse, LocalDate migrertStp) {
        var inntektsmeldingerForSak = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(behandlingReferanse.getSaksnummer());
        var imTjeneste = finnInntektsmeldingForBeregningTjeneste(behandlingReferanse);
        return imTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldingerForSak, DatoIntervallEntitet.fraOgMedTilOgMed(migrertStp, migrertStp));
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType type) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, fagsakYtelseType, type)
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + fagsakYtelseType + "]"));
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjeneste, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    private InntektsmeldingerRelevantForBeregning finnInntektsmeldingForBeregningTjeneste(BehandlingReferanse behandlingReferanse) {
        FagsakYtelseType ytelseType = behandlingReferanse.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(inntektsmeldingerRelevantForBeregning, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + InntektsmeldingerRelevantForBeregning.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }


}
