package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerSyktBarnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.UttakRestKlient;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PsbYtelsesspesifiktGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<PleiepengerSyktBarnGrunnlag> {

    private UttakTjeneste uttakRestKlient;

    public PsbYtelsesspesifiktGrunnlagMapper() {
        // for proxy
    }

    @Inject
    public PsbYtelsesspesifiktGrunnlagMapper(UttakTjeneste uttakRestKlient) {
        this.uttakRestKlient = uttakRestKlient;
    }

    @Override
    public PleiepengerSyktBarnGrunnlag lagYtelsespesifiktGrunnlag(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode) {
        var uttaksplan = uttakRestKlient.hentUttaksplan(ref.getBehandlingUuid());

        var utbetalingsgrader = uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .filter(e -> Objects.equals(Utfall.OPPFYLT, e.getValue().getUtfall()))
            .flatMap(e -> lagUtbetalingsgrad(e.getKey(), e.getValue()).stream()).collect(Collectors.toList());

        return new PleiepengerSyktBarnGrunnlag(utbetalingsgrader);
    }

    private List<UtbetalingsgradPrAktivitetDto> lagUtbetalingsgrad(LukketPeriode periode, UttaksperiodeInfo plan) {
        var perArbeidsforhold = plan.getUtbetalingsgrader()
            .stream()
            .collect(Collectors.groupingBy(Utbetalingsgrader::getArbeidsforhold));

        List<UtbetalingsgradPrAktivitetDto> res = new ArrayList<>();
        for (var entry : perArbeidsforhold.entrySet()) {
            var arbeidsforhold = lagArbeidsforhold(entry.getKey());
            var perioder = lagPerioder(periode, entry.getValue());
            res.add(new UtbetalingsgradPrAktivitetDto(arbeidsforhold, perioder));
        }
        return res;
    }

    private List<PeriodeMedUtbetalingsgradDto> lagPerioder(LukketPeriode periode, List<Utbetalingsgrader> ut) {
        if (ut == null) {
            return Collections.emptyList();
        } else {
            return ut.stream().map(p -> {
                var kalkulusPeriode = new no.nav.folketrygdloven.kalkulus.felles.v1.Periode(periode.getFom(), periode.getTom());
                return new PeriodeMedUtbetalingsgradDto(kalkulusPeriode, p.getUtbetalingsgrad());
            }).collect(Collectors.toList());
        }
    }

    private UtbetalingsgradArbeidsforholdDto lagArbeidsforhold(Arbeidsforhold arb) {
        return new UtbetalingsgradArbeidsforholdDto(lagAktør(arb),
            arb.getArbeidsforholdId() != null ? new InternArbeidsforholdRefDto(arb.getArbeidsforholdId()) : null,
            new UttakArbeidType(arb.getType()));
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
