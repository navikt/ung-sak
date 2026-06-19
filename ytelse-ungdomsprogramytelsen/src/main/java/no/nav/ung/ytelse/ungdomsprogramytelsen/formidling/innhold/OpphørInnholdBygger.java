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
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.OpphørDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;

@Dependent
public class OpphørInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = LoggerFactory.getLogger(OpphørInnholdBygger.class);

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var opphørStartdato = resultatTidslinje.filterValue(it -> it.resultatInfo().stream()
                .anyMatch(r -> r.detaljertResultatType() == DetaljertResultatType.ENDRING_SLUTTDATO))
            .getMinLocalDate();

        var sisteUtbetalingsdato = PeriodeBeregner.utledFremtidigUtbetalingsdato(
            opphørStartdato.minusDays(1),
            bestemInneværendeMåned());

        return new TemplateInnholdResultat(TemplateType.OPPHØR,
            new OpphørDto(
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
