package no.nav.k9.sak.ytelse.omsorgspenger.beregningsgrunnlag;

import static java.util.Comparator.comparing;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.OmsorgspengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.SøknadsperioderPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Arbeidsforhold;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.SøknadPerioderTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.KravDokumentFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerYtelsesspesifiktGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<OmsorgspengerGrunnlag> {

    private static final Comparator<Uttaksperiode> COMP_PERIODE = comparing(uttaksperiode -> uttaksperiode.getPeriode().getFom());

    private ÅrskvantumTjeneste årskvantumTjeneste;
    private SøknadPerioderTjeneste søknadPerioderTjeneste;
    private VurderSøknadsfristTjeneste<OppgittFraværPeriode> søknadsfristTjeneste;
    private boolean sendSøknadsperioderTilKalkulus;

    protected OmsorgspengerYtelsesspesifiktGrunnlagMapper() {
        // for proxy
    }

    @Inject
    public OmsorgspengerYtelsesspesifiktGrunnlagMapper(ÅrskvantumTjeneste årskvantumTjeneste,
                                                       SøknadPerioderTjeneste søknadPerioderTjeneste,
                                                       @FagsakYtelseTypeRef("OMP") VurderSøknadsfristTjeneste<OppgittFraværPeriode> søknadsfristTjeneste,
                                                       @KonfigVerdi(value = "OMP_SOKNADSPERIODER_KALKULUS", defaultVerdi = "true", required = false) boolean sendSøknadsperioderTilKalkulus) {
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.søknadPerioderTjeneste = søknadPerioderTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.sendSøknadsperioderTilKalkulus = sendSøknadsperioderTilKalkulus;
    }

    private static InternArbeidsforholdRefDto mapArbeidsforholdId(String arbeidsforholdId) {
        return arbeidsforholdId == null ? null : new InternArbeidsforholdRefDto(arbeidsforholdId);
    }

    private static Aktør mapTilKalkulusAktør(Arbeidsforhold arb) {
        if (arb != null && arb.getAktørId() != null) {
            return new AktørIdPersonident(arb.getAktørId());
        } else if (arb != null && arb.getOrganisasjonsnummer() != null) {
            return new Organisasjon(arb.getOrganisasjonsnummer());
        } else {
            return null;
        }
    }

    private static UttakArbeidType mapType(String type) {
        return new UttakArbeidType(type);
    }

    private static Periode tilKalkulusPeriode(LukketPeriode periode) {
        return new Periode(periode.getFom(), periode.getTom());
    }

    @Override
    public OmsorgspengerGrunnlag lagYtelsespesifiktGrunnlag(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode) {
        List<Aktivitet> aktiviteter = hentAktiviteter(ref);
        if (aktiviteter.isEmpty()) {
            List<SøknadsperioderPrAktivitetDto> søknadsperioder = sendSøknadsperioderTilKalkulus ? Collections.emptyList() : null;
            return new OmsorgspengerGrunnlag(Collections.emptyList(), søknadsperioder);
        }

        // Kalkulus forventer å ikke få duplikate arbeidsforhold, så vi samler alle perioder pr arbeidsforhold/aktivitet
        var gruppertPrAktivitet = aktiviteter.stream()
            .filter(e -> !e.getUttaksperioder().isEmpty())
            .collect(Collectors.groupingBy(a -> mapTilKalkulusArbeidsforhold(a.getArbeidsforhold())));


        var utbetalingsgradPrAktivitet = gruppertPrAktivitet
            .entrySet()
            .stream()
            .map(e -> {
                var utbetalingsgraderForVilkårsperiode = filtrerForVilkårsperiode(vilkårsperiode, e.getValue().stream().flatMap(a -> a.getUttaksperioder().stream()));
                return mapTilUtbetalingsgrad(utbetalingsgraderForVilkårsperiode, e.getKey());
            })
            .filter(Objects::nonNull)
            .toList();

        List<SøknadsperioderPrAktivitetDto> søkadsperioder;
        if (sendSøknadsperioderTilKalkulus) {
            søkadsperioder = hentSøknadsperioder(ref);
        } else {
            søkadsperioder = null;
        }
        return new OmsorgspengerGrunnlag(utbetalingsgradPrAktivitet, søkadsperioder);
    }

    private List<SøknadsperioderPrAktivitetDto> hentSøknadsperioder(BehandlingReferanse ref) {
        var søkteFraværsperioder = søknadPerioderTjeneste.hentSøktePerioderMedKravdokumentPåFagsak(ref);
        var søkteFraværsperioderSøknadsfristbehandlet = søknadsfristTjeneste.vurderSøknadsfrist(ref.getBehandlingId(), søkteFraværsperioder);
        var søkteFraværesperioderUtenOverlapp = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(søkteFraværsperioderSøknadsfristbehandlet);

        return søkteFraværesperioderUtenOverlapp.stream()
            //.filter(wofp -> wofp.getSøknadsfristUtfall() == no.nav.k9.kodeverk.vilkår.Utfall.OPPFYLT)
            .collect(Collectors.groupingBy(this::mapAktivitet, Collectors.mapping(this::mapPeriode, Collectors.toList())))
            .entrySet().stream()
            .map(e -> new SøknadsperioderPrAktivitetDto(e.getKey(), e.getValue()))
            .toList();
    }

    private Periode mapPeriode(WrappedOppgittFraværPeriode wrappedPeriode) {
        OppgittFraværPeriode periode = wrappedPeriode.getPeriode();
        return new Periode(periode.getFom(), periode.getTom());
    }

    private AktivitetDto mapAktivitet(WrappedOppgittFraværPeriode wrappedPeriode) {
        OppgittFraværPeriode periode = wrappedPeriode.getPeriode();
        if (periode.getArbeidsforholdRef().getReferanse() != null) {
            throw new IllegalArgumentException("Forventer ikke arbeidsforhold her, skal kun få søknadsperioder her p.t. ikke mulig å opplyse arbeidsforhold i søknad.");
        }
        Aktør arbeidsgiver = mapTilAktør(periode.getArbeidsgiver());
        return new AktivitetDto(arbeidsgiver, null, new UttakArbeidType(periode.getAktivitetType().getKode()));
    }

    public static Aktør mapTilAktør(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        return arbeidsgiver.getErVirksomhet() ? new Organisasjon(arbeidsgiver.getOrgnr()) : new AktørIdPersonident(arbeidsgiver.getAktørId().getId());
    }

    @NotNull
    private List<Uttaksperiode> filtrerForVilkårsperiode(DatoIntervallEntitet vilkårsperiode, Stream<Uttaksperiode> uttaksperiodeStream) {
        return uttaksperiodeStream
            .filter(it -> vilkårsperiode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(it.getPeriode().getFom(), it.getPeriode().getTom())))
            .collect(Collectors.toList());
    }

    @NotNull
    private List<Aktivitet> hentAktiviteter(BehandlingReferanse ref) {
        var fullUttaksplan = årskvantumTjeneste.hentFullUttaksplan(ref.getSaksnummer());
        return fullUttaksplan.getAktiviteter();
    }

    private UtbetalingsgradPrAktivitetDto mapTilUtbetalingsgrad(List<Uttaksperiode> perioder, AktivitetDto arbeidsforhold) {
        var utbetalingsgrad = mapUtbetalingsgradPerioder(perioder);

        if (perioder.size() != utbetalingsgrad.size()) {
            throw new IllegalArgumentException("Utvikler-feil: Skal ikke komme til kalkulus uten innvilgede perioder for " + arbeidsforhold + ", angitte uttaksperioder: " + perioder);
        }
        if (perioder.isEmpty()) {
            return null;
        }
        return new UtbetalingsgradPrAktivitetDto(arbeidsforhold, utbetalingsgrad);
    }

    @NotNull
    private List<PeriodeMedUtbetalingsgradDto> mapUtbetalingsgradPerioder(List<Uttaksperiode> perioder) {
        return perioder.stream()
            .sorted(COMP_PERIODE) // stabil rekkefølge output
            .map(p -> new PeriodeMedUtbetalingsgradDto(tilKalkulusPeriode(p.getPeriode()), mapUtbetalingsgrad(p)))
            .collect(Collectors.toList());
    }

    @NotNull
    private BigDecimal mapUtbetalingsgrad(Uttaksperiode p) {
        if (!Utfall.INNVILGET.equals(p.getUtfall()) && BigDecimal.ZERO.compareTo(p.getUtbetalingsgrad()) != 0) {
            throw new IllegalStateException("Uttaksperiode med utfall=" + p.getUtfall()
                + " og utbetalingsgrad(" + p.getUtbetalingsgrad()
                + ") er ikke 0 som forventet");
        }
        return p.getUtbetalingsgrad();
    }

    private AktivitetDto mapTilKalkulusArbeidsforhold(Arbeidsforhold arb) {
        if (erTypeMedArbeidsforhold(arb)) {
            var aktør = mapTilKalkulusAktør(arb);
            var type = mapType(arb.getType());
            var internArbeidsforholdId = mapArbeidsforholdId(arb.getArbeidsforholdId());
            return new AktivitetDto(aktør, internArbeidsforholdId, type);
        } else {
            return new AktivitetDto(null, null, mapType(arb.getType()));
        }
    }

    private boolean erTypeMedArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        return arbeidsforhold.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.ARBEIDSTAKER.getKode());
    }

}
