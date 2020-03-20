package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.input;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerSyktBarnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.k9.kodeverk.uttak.UtfallType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.InnvilgetUttaksplanperiode;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Periode;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.UttakArbeidsforhold;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.UttakUtbetalingsgrad;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PsbYtelsesspesifiktGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<PleiepengerSyktBarnGrunnlag> {

    private UttakTjeneste uttakTjeneste;

    public PsbYtelsesspesifiktGrunnlagMapper() {
        // for proxy
    }

    @Inject
    public PsbYtelsesspesifiktGrunnlagMapper(UttakTjeneste uttakTjeneste) {
        this.uttakTjeneste = uttakTjeneste;
    }

    @Override
    public PleiepengerSyktBarnGrunnlag lagYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var uttaksplan = uttakTjeneste.hentUttaksplan(ref.getBehandlingUuid()).orElseThrow();

        var utbetalingsgrader = uttaksplan.getPerioder().entrySet().stream()
            .filter(e -> Objects.equals(UtfallType.INNVILGET, e.getValue().getUtfall()))
            .flatMap(e -> lagUtbetalingsgrad(e.getKey(), (InnvilgetUttaksplanperiode) e.getValue()).stream()).collect(Collectors.toList());

        return new PleiepengerSyktBarnGrunnlag(BigDecimal.valueOf(100), utbetalingsgrader);
    }

    private List<UtbetalingsgradPrAktivitetDto> lagUtbetalingsgrad(Periode periode, InnvilgetUttaksplanperiode plan) {
        var perArbeidsforhold = plan.getUtbetalingsgrader().stream().collect(Collectors.groupingBy(UttakUtbetalingsgrad::getArbeidsforhold));
        List<UtbetalingsgradPrAktivitetDto> res = new ArrayList<>();
        for (var entry : perArbeidsforhold.entrySet()) {
            var arbeidsforhold = lagArbeidsforhold(entry.getKey());
            var perioder = lagPerioder(periode, entry.getValue());
            res.add(new UtbetalingsgradPrAktivitetDto(arbeidsforhold, perioder));
        }
        return res;
    }

    private List<PeriodeMedUtbetalingsgradDto> lagPerioder(Periode periode, List<UttakUtbetalingsgrad> ut) {
        if (ut == null) {
            return Collections.emptyList();
        } else {
            return ut.stream().map(p -> {
                var kalkulusPeriode = new no.nav.folketrygdloven.kalkulus.felles.v1.Periode(periode.getFom(), periode.getTom());
                return new PeriodeMedUtbetalingsgradDto(kalkulusPeriode, p.getUtbetalingsgrad());
            }).collect(Collectors.toList());
        }
    }

    private UtbetalingsgradArbeidsforholdDto lagArbeidsforhold(UttakArbeidsforhold arb) {
        return new UtbetalingsgradArbeidsforholdDto(lagAktør(arb),
            arb.getArbeidsforholdId() != null ? new InternArbeidsforholdRefDto(arb.getArbeidsforholdId()) : null,
            new UttakArbeidType(arb.getType().getKode()));
    }

    private Aktør lagAktør(UttakArbeidsforhold arb) {
        if (arb.getAktørId() != null) {
            return new AktørIdPersonident(arb.getAktørId().getId());
        } else if (arb.getOrganisasjonsnummer() != null) {
            return new Organisasjon(arb.getOrganisasjonsnummer());
        } else {
            return null;
        }
    }

}
