package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.template.dto.OpphørDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;

@Dependent
public class OpphørInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = LoggerFactory.getLogger(OpphørInnholdBygger.class);
    private final LocalDate overrideDagensDatoForTest;

    @Inject
    public OpphørInnholdBygger(@KonfigVerdi(value = "BREV_DAGENS_DATO_TEST", required = false) LocalDate overrideDagensDatoForTest) {
        this.overrideDagensDatoForTest = overrideDagensDatoForTest;
    }


    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var opphørStartdato = resultatTidslinje.filterValue(it -> it.resultatInfo().stream()
                .anyMatch(r -> r.detaljertResultatType() == DetaljertResultatType.ENDRING_SLUTTDATO))
            .getMinLocalDate();

        var sisteUtbetalingsdato = PeriodeBeregner.utledFremtidigUtbetalingsdato(
            PeriodeBeregner.forrigeUkedag(opphørStartdato),
            bestemInneværendeMåned());

        return new TemplateInnholdResultat(DokumentMalType.ENDRING_DOK, TemplateType.OPPHØR,
            new OpphørDto(
                opphørStartdato,
                sisteUtbetalingsdato
            ));
    }

    private YearMonth bestemInneværendeMåned() {
        return Environment.current().isLocal() && overrideDagensDatoForTest != null ?
            YearMonth.from(overrideDagensDatoForTest)
            : YearMonth.now();
    }

}
