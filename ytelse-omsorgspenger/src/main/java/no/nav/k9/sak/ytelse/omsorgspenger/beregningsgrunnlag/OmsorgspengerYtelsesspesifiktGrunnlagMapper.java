package no.nav.k9.sak.ytelse.omsorgspenger.beregningsgrunnlag;

import static java.util.Comparator.comparing;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;

import no.nav.folketrygdloven.kalkulus.beregning.v1.OmsorgspengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradArbeidsforholdDto;
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
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerYtelsesspesifiktGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<OmsorgspengerGrunnlag> {

    private static final Comparator<Uttaksperiode> COMP_PERIODE = comparing(uttaksperiode -> uttaksperiode.getPeriode().getFom());

    private Boolean hentFullUttaksplan;
    private ÅrskvantumTjeneste årskvantumTjeneste;

    protected OmsorgspengerYtelsesspesifiktGrunnlagMapper() {
        // for proxy
    }

    @Inject
    public OmsorgspengerYtelsesspesifiktGrunnlagMapper(@KonfigVerdi(value = "OMP_HENT_FULLUTTAKSPLAN_AKTIVERT", defaultVerdi = "true") Boolean hentFullUttaksplan, ÅrskvantumTjeneste årskvantumTjeneste) {
        this.hentFullUttaksplan = hentFullUttaksplan;
        this.årskvantumTjeneste = årskvantumTjeneste;
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
        if (aktiviteter == null || aktiviteter.isEmpty()) {
            return new OmsorgspengerGrunnlag(Collections.emptyList());
        }

        var arbeidsforholdPerioder = aktiviteter;
        var utbetalingsgradPrAktivitet = arbeidsforholdPerioder.stream()
            .filter(e -> !e.getUttaksperioder().isEmpty())
            .map(e -> mapTilUtbetalingsgrad(e.getArbeidsforhold(), e.getUttaksperioder().stream()
                .filter(it -> vilkårsperiode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(it.getPeriode().getFom(), it.getPeriode().getTom())))
                .collect(Collectors.toList())))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return new OmsorgspengerGrunnlag(utbetalingsgradPrAktivitet);
    }

    @NotNull
    private List<Aktivitet> hentAktiviteter(BehandlingReferanse ref) {
        if (hentFullUttaksplan) {
            var fullUttaksplan = årskvantumTjeneste.hentFullUttaksplan(ref.getSaksnummer());
            return fullUttaksplan.getAktiviteter();
        } else {
            var forbrukteDager = årskvantumTjeneste.hentÅrskvantumForBehandling(ref.getBehandlingUuid());
            return forbrukteDager.getSisteUttaksplan().getAktiviteter();
        }
    }

    private UtbetalingsgradPrAktivitetDto mapTilUtbetalingsgrad(Arbeidsforhold uttakArbeidsforhold, List<Uttaksperiode> perioder) {
        var arbeidsforhold = mapTilKalkulusArbeidsforhold(uttakArbeidsforhold);
        var utbetalingsgrad = perioder.stream()
            .sorted(COMP_PERIODE) // stabil rekkefølge output
            .map(p -> new PeriodeMedUtbetalingsgradDto(tilKalkulusPeriode(p.getPeriode()), mapUtbetalingsgrad(p)))
            .collect(Collectors.toList());

        if (perioder.size() != utbetalingsgrad.size()) {
            throw new IllegalArgumentException("Utvikler-feil: Skal ikke komme til kalkulus uten innvilgede perioder for " + arbeidsforhold + ", angitte uttaksperioder: " + perioder);
        }
        if (perioder.isEmpty()) {
            return null;
        }
        return new UtbetalingsgradPrAktivitetDto(arbeidsforhold, utbetalingsgrad);
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

    private UtbetalingsgradArbeidsforholdDto mapTilKalkulusArbeidsforhold(Arbeidsforhold arb) {
        var aktør = mapTilKalkulusAktør(arb);
        var type = mapType(arb.getType());
        var internArbeidsforholdId = mapArbeidsforholdId(arb.getArbeidsforholdId());
        var utbetalingsgradARbeidsforhold = new UtbetalingsgradArbeidsforholdDto(aktør, internArbeidsforholdId, type);
        return utbetalingsgradARbeidsforhold;
    }

}
