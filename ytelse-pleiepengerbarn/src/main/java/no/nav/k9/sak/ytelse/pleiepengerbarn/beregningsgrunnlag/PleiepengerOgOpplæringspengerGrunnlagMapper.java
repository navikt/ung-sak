package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.OpplæringspengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerNærståendeGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerSyktBarnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@ApplicationScoped
public class PleiepengerOgOpplæringspengerGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<YtelsespesifiktGrunnlagDto> {

    private UttakTjeneste uttakRestKlient;

    public PleiepengerOgOpplæringspengerGrunnlagMapper() {
        // for proxy
    }

    @Inject
    public PleiepengerOgOpplæringspengerGrunnlagMapper(UttakTjeneste uttakRestKlient) {
        this.uttakRestKlient = uttakRestKlient;
    }
    
    @Override
    public YtelsespesifiktGrunnlagDto lagYtelsespesifiktGrunnlag(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode) {
        var uttaksplan = uttakRestKlient.hentUttaksplan(ref.getBehandlingUuid(), false);

        List<UtbetalingsgradPrAktivitetDto> utbetalingsgrader = new ArrayList<>();
        if (uttaksplan != null) {
            utbetalingsgrader = uttaksplan.getPerioder()
                .entrySet()
                .stream()
                .filter(it -> vilkårsperiode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(it.getKey().getFom(), it.getKey().getTom())))
                .flatMap(e -> lagUtbetalingsgrad(e.getKey(), e.getValue()).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())))
                .entrySet()
                .stream()
                .map(e -> new UtbetalingsgradPrAktivitetDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        }

        return mapTilYtelseSpesifikkType(ref, utbetalingsgrader);
    }

    private YtelsespesifiktGrunnlagDto mapTilYtelseSpesifikkType(BehandlingReferanse ref, List<UtbetalingsgradPrAktivitetDto> utbetalingsgrader) {
        return switch (ref.getFagsakYtelseType()) {
            case PLEIEPENGER_SYKT_BARN -> new PleiepengerSyktBarnGrunnlag(utbetalingsgrader);
            case PLEIEPENGER_NÆRSTÅENDE -> new PleiepengerNærståendeGrunnlag(utbetalingsgrader);
            case OPPLÆRINGSPENGER -> new OpplæringspengerGrunnlag(utbetalingsgrader);
            default -> throw new IllegalStateException("Ikke støttet ytelse for kalkulus Pleiepenger: " + ref.getFagsakYtelseType());
        };
    }

    private Map<AktivitetDto, PeriodeMedUtbetalingsgradDto> lagUtbetalingsgrad(LukketPeriode periode, UttaksperiodeInfo plan) {
        var perArbeidsforhold = plan.getUtbetalingsgrader()
            .stream()
            .collect(Collectors.toMap(this::mapUtbetalingsgradArbeidsforhold, Function.identity()));

        Map<AktivitetDto, PeriodeMedUtbetalingsgradDto> res = new HashMap<>();
        for (var entry : perArbeidsforhold.entrySet()) {
            var utbetalingsgrader = entry.getValue();
            var aktivitetsgrad = hentAktivitetsgrad(utbetalingsgrader);
            var utbetalingsgradPeriode = lagPeriode(periode, utbetalingsgrader.getUtbetalingsgrad(), aktivitetsgrad);
            res.put(entry.getKey(), utbetalingsgradPeriode);
        }
        return res;
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
        
        /*
         * XXX: Dette er samme måte å regne ut på som i uttak. På sikt bør vi nok flytte
         *      denne logikken til pleiepenger-barn-uttak.
         */
        final BigDecimal aktivitetsgrad = new BigDecimal(faktiskArbeidstid.toMillis()).setScale(2, RoundingMode.HALF_UP)
                .divide(new BigDecimal(utbetalingsgrader.getNormalArbeidstid().toMillis()), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));
        
        return aktivitetsgrad;
    }

    private AktivitetDto mapUtbetalingsgradArbeidsforhold(Utbetalingsgrader utbGrad) {
        Arbeidsforhold arbeidsforhold = utbGrad.getArbeidsforhold();
        if (erTypeMedArbeidsforhold(arbeidsforhold)) {
            return lagArbeidsforhold(arbeidsforhold);
        } else {
            return new AktivitetDto(null, null, mapUttakArbeidType(arbeidsforhold));
        }
    }

    private boolean erTypeMedArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        return arbeidsforhold.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.ARBEIDSTAKER.getKode()) ||
            arbeidsforhold.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.IKKE_YRKESAKTIV.getKode()) ||
            arbeidsforhold.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING.getKode());
    }

    private PeriodeMedUtbetalingsgradDto lagPeriode(LukketPeriode periode, BigDecimal utbetalingsgrad, BigDecimal aktivitetsgrad) {
        var kalkulusPeriode = new no.nav.folketrygdloven.kalkulus.felles.v1.Periode(periode.getFom(), periode.getTom());
        return new PeriodeMedUtbetalingsgradDto(kalkulusPeriode, utbetalingsgrad, aktivitetsgrad);
    }

    private AktivitetDto lagArbeidsforhold(Arbeidsforhold arb) {
        return new AktivitetDto(lagAktør(arb),
            arb.getArbeidsforholdId() != null ? new InternArbeidsforholdRefDto(arb.getArbeidsforholdId()) : null,
            mapUttakArbeidType(arb));
    }

    private UttakArbeidType mapUttakArbeidType(Arbeidsforhold arb) {
        if (arb.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING.getKode())) {
            return new UttakArbeidType(no.nav.k9.kodeverk.uttak.UttakArbeidType.IKKE_YRKESAKTIV.getKode());
        }
        return new UttakArbeidType(arb.getType());
    }

    private Aktør lagAktør(Arbeidsforhold arb) {
        if (arb.getAktørId() != null) {
            return new AktørIdPersonident(arb.getAktørId());
        } else if (arb.getOrganisasjonsnummer() != null) {
            return new Organisasjon(arb.getOrganisasjonsnummer());
        } else {
            return null;
        }
    }

}
