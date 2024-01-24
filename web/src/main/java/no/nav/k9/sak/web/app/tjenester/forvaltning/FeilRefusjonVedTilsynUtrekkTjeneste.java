package no.nav.k9.sak.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerSyktBarnGrunnlag;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType;
import no.nav.folketrygdloven.kalkulus.request.v1.forvaltning.OppdaterYtelsesspesifiktGrunnlagForRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.forvaltning.OppdaterYtelsesspesifiktGrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.forvaltning.EndretPeriodeListeRespons;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.PleiepengerOgOpplæringspengerGrunnlagMapper;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.UttakRestKlient;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class FeilRefusjonVedTilsynUtrekkTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeilRefusjonVedTilsynUtrekkTjeneste.class);


    private BeregningsresultatRepository beregningsresultatRepository;

    private UttakRestKlient uttakRestKlient;
    private VilkårResultatRepository vilkårResultatRepository;

    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;

    private KalkulusRestKlient kalkulusRestKlient;

    public FeilRefusjonVedTilsynUtrekkTjeneste() {
    }


    @Inject
    public FeilRefusjonVedTilsynUtrekkTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                               UttakRestKlient uttakRestKlient,
                                               VilkårResultatRepository vilkårResultatRepository,
                                               BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                               KalkulusRestKlient kalkulusRestKlient) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.uttakRestKlient = uttakRestKlient;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.kalkulusRestKlient = kalkulusRestKlient;
    }

    public Optional<KalkulusDiffRequestOgRespons> finnFeilForBehandling(Behandling behandling, boolean debugLogging) {


        var beregningsresultatEntitet = beregningsresultatRepository.hentEndeligBeregningsresultat(behandling.getId());
        var uttaksplan = uttakRestKlient.hentUttaksplan(behandling.getUuid(), false);
        var perioderMedForventetEndring = finnPerioderMedForventetEndring(beregningsresultatEntitet, uttaksplan, debugLogging);
        if (!perioderMedForventetEndring.isEmpty()) {
            return Optional.of(hentDiffFraKalkulus(behandling, uttaksplan, perioderMedForventetEndring));
        }

        if (debugLogging) {
            LOGGER.info("Behandling " + behandling.getId() + " hadde ingen perioder med forventet endring");
        }

        return Optional.empty();
    }


    private KalkulusDiffRequestOgRespons hentDiffFraKalkulus(Behandling sisteBehandling, Uttaksplan uttaksplan, List<DatoIntervallEntitet> perioderMedForventetEndring) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(sisteBehandling.getId());

        var vilkårsperioder = vilkårene.flatMap(it -> it.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toSet());

        var vilkårsperioderSomMåSjekkes = vilkårsperioder.stream().filter(it -> perioderMedForventetEndring.stream().anyMatch(p -> p.overlapper(it))).collect(Collectors.toSet());

        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(sisteBehandling.getId());

        var bgReferanserSomMåSjekkes = beregningsgrunnlagPerioderGrunnlag
            .stream().flatMap(it -> it.getGrunnlagPerioder().stream())
            .filter(it -> vilkårsperioderSomMåSjekkes.stream().anyMatch(p -> p.getFomDato().equals(it.getSkjæringstidspunkt())))
            .collect(Collectors.toSet());

        var requesterPrReferanse = bgReferanserSomMåSjekkes.stream().map(ref -> {
            var vilkårsperiode = vilkårsperioderSomMåSjekkes.stream().filter(p -> p.getFomDato().equals(ref.getSkjæringstidspunkt())).findFirst().orElseThrow();
            var perioderMedEndring = perioderMedForventetEndring.stream().filter(p -> p.overlapper(vilkårsperiode)).toList();
            return new OppdaterYtelsesspesifiktGrunnlagForRequest(ref.getEksternReferanse(),
                new PleiepengerSyktBarnGrunnlag(PleiepengerOgOpplæringspengerGrunnlagMapper.finnUtbetalingsgrader(perioderMedEndring, uttaksplan), null));
        }).toList();

        var request = new OppdaterYtelsesspesifiktGrunnlagListeRequest(sisteBehandling.getFagsak().getSaksnummer().getVerdi(),
            sisteBehandling.getUuid(),
            new AktørIdPersonident(sisteBehandling.getAktørId().getAktørId()),
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            requesterPrReferanse);


        var endretPeriodeListeRespons = kalkulusRestKlient.simulerFastsettMedOppdatertUttak(request);
        return new KalkulusDiffRequestOgRespons(request, endretPeriodeListeRespons);
    }

    private List<DatoIntervallEntitet> finnPerioderMedForventetEndring(Optional<BeregningsresultatEntitet> beregningsresultatEntitet, Uttaksplan uttaksplan, boolean debugLogging) {

        if (uttaksplan == null) {
            if (debugLogging) {
                LOGGER.info("Fant ingen uttaksplan");
            }
            return Collections.emptyList();
        }

        var perioderMedUtbetalingTilArbeidsgiver = beregningsresultatEntitet.stream().flatMap(it -> it.getBeregningsresultatPerioder().stream())
            .filter(p -> p.getBeregningsresultatAndelList().stream().anyMatch(a -> !a.erBrukerMottaker() && a.getDagsats() > 0))
            .filter(p -> harMerEnnEnArbeidsgiverMedRefusjon(p.getBeregningsresultatAndelList()) || harUtbetalingTilBådeArbeidsgiverOgBruker(p.getBeregningsresultatAndelList()))
            .toList();

        if (debugLogging) {
            var intervaller = perioderMedUtbetalingTilArbeidsgiver.stream().map(BeregningsresultatPeriode::getPeriode).collect(Collectors.toSet());
            LOGGER.info("Fant følgende perioder med utbetaling til arbeidsgiver " + intervaller);
        }

        if (perioderMedUtbetalingTilArbeidsgiver.isEmpty()) {
            return Collections.emptyList();
        }


        var perioderMedDiff = uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .map(e -> new Uttaksperiode(
                DatoIntervallEntitet.fraOgMedTilOgMed(e.getKey().getFom(), e.getKey().getTom()),
                lagArbeidsforholdMedDiffListe(e.getValue())))
            .filter(p -> !p.arbeidsforholdMedDiff().isEmpty())
            .toList();

        if (debugLogging) {
            var intervaller = perioderMedDiff.stream().map(Uttaksperiode::periode).collect(Collectors.toSet());
            LOGGER.info("Fant følgende perioder med diff mellom aktivitetsgrad og utbetalingsgrad " + intervaller);
        }

        if (perioderMedDiff.isEmpty()) {
            return Collections.emptyList();
        }

        var utbetalingTidslinje = new LocalDateTimeline<>(lagSegmenterMedUtbetalingTilArbeidsgiver(perioderMedUtbetalingTilArbeidsgiver));
        var uttakTidslinje = new LocalDateTimeline<>(perioderMedDiff.stream().map(it -> new LocalDateSegment<>(it.periode().getFomDato(), it.periode().getTomDato(), it.arbeidsforholdMedDiff())).toList());
        var påvirketTidslinje = uttakTidslinje.intersection(utbetalingTidslinje, StandardCombinators::alwaysTrueForMatch);
        return påvirketTidslinje.toSegments().stream().map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom())).toList();
    }

    private static boolean harUtbetalingTilBådeArbeidsgiverOgBruker(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        return beregningsresultatAndelList.stream().anyMatch(a -> !a.erBrukerMottaker() && a.getDagsats() > 0) &&
            beregningsresultatAndelList.stream().anyMatch(a -> a.erBrukerMottaker() && a.getDagsats() > 0);
    }

    private static boolean harMerEnnEnArbeidsgiverMedRefusjon(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        return beregningsresultatAndelList.stream().filter(a -> a.getArbeidsgiver().isPresent() && !a.erBrukerMottaker() && a.getDagsats() > 0)
            .map(BeregningsresultatAndel::getArbeidsgiver)
            .map(Optional::get)
            .distinct()
            .count() > 1;
    }

    private static List<LocalDateSegment<List<BeregningsresultatAndel>>> lagSegmenterMedUtbetalingTilArbeidsgiver(List<BeregningsresultatPeriode> perioderMedUtbetalingTilArbeidsgiver) {
        return perioderMedUtbetalingTilArbeidsgiver.stream().map(
                it -> new LocalDateSegment<>(it.getBeregningsresultatPeriodeFom(), it.getBeregningsresultatPeriodeTom(),
                    it.getBeregningsresultatAndelList()))
            .toList();
    }

    private List<Arbeidsforhold> lagArbeidsforholdMedDiffListe(UttaksperiodeInfo plan) {
        return plan.getUtbetalingsgrader()
            .stream()
            .filter(a -> a.getArbeidsforhold().getOrganisasjonsnummer() != null)
            .map(a -> new Arbeidsforhold(a.getArbeidsforhold().getOrganisasjonsnummer(),
                hentAktivitetsgrad(a), a.getUtbetalingsgrad()))
            .filter(a -> a.aktivitetsgrad().subtract(BigDecimal.valueOf(100).subtract(a.utbetalingsgrad())).abs().compareTo(BigDecimal.valueOf(1)) > 0)
            .toList();
    }

    private BigDecimal hentAktivitetsgrad(Utbetalingsgrader utbetalingsgrader) {
        if (utbetalingsgrader.getNormalArbeidstid().isZero()) {
            return new BigDecimal(100).subtract(utbetalingsgrader.getUtbetalingsgrad());
        }

        final Duration faktiskArbeidstid;
        if (utbetalingsgrader.getFaktiskArbeidstid() != null) {
            faktiskArbeidstid = utbetalingsgrader.getFaktiskArbeidstid();
        } else {
            faktiskArbeidstid = Duration.ofHours(0L);
        }

        final BigDecimal HUNDRE_PROSENT = new BigDecimal(100);


        final BigDecimal aktivitetsgrad = new BigDecimal(faktiskArbeidstid.toMillis()).setScale(2, RoundingMode.HALF_DOWN)
            .divide(new BigDecimal(utbetalingsgrader.getNormalArbeidstid().toMillis()), 2, RoundingMode.HALF_DOWN)
            .multiply(HUNDRE_PROSENT);

        if (aktivitetsgrad.compareTo(HUNDRE_PROSENT) >= 0) {
            return HUNDRE_PROSENT;
        }

        return aktivitetsgrad;
    }


    record Arbeidsforhold(String orgnr, BigDecimal aktivitetsgrad, BigDecimal utbetalingsgrad) {
    }

    record Uttaksperiode(DatoIntervallEntitet periode, List<Arbeidsforhold> arbeidsforholdMedDiff) {
    }

    public record KalkulusDiffRequestOgRespons(OppdaterYtelsesspesifiktGrunnlagListeRequest request,
                                               EndretPeriodeListeRespons respons) {
    }

}
