package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.OpphørVedMaksdatoDto;

import java.time.LocalDate;
import java.time.YearMonth;

@Dependent
public class OpphørVedMaksdatoInnholdBygger implements VedtaksbrevInnholdBygger {

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var opphørStartdato = resultatTidslinje.filterValue(it -> it.resultatInfo().stream()
                .anyMatch(r -> r.detaljertResultatType() == DetaljertResultatType.OPPHØR_VED_MAKSDATO))
            .getMinLocalDate();

        var sisteUtbetalingsdato = PeriodeBeregner.utledFremtidigUtbetalingsdato(
            opphørStartdato.minusDays(1),
            bestemInneværendeMåned());

        return new TemplateInnholdResultat(TemplateType.OPPHOR_VED_MAKSDATO,
            new OpphørVedMaksdatoDto(
                opphørStartdato,
                sisteUtbetalingsdato
            ));
    }

    private YearMonth bestemInneværendeMåned() {
        //Kan ikke injectes i konstruktør fordi den settes én gang for hele testkjøringen pga application scoped
        var overrideDagensDatoForTest = Environment.current().getProperty("BREV_DAGENS_DATO_TEST", LocalDate.class);
        return Environment.current().isLocal() && overrideDagensDatoForTest != null ?
            YearMonth.from(overrideDagensDatoForTest)
            : YearMonth.now();
    }
}

