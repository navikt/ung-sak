package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.AutomatiskOpphørDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

import java.time.LocalDate;
import java.time.YearMonth;

@Dependent
public class AutomatiskOpphørInnholdBygger implements VedtaksbrevInnholdBygger {

    private final LocalDate overrideDagensDatoForTest;

    @Inject
    public AutomatiskOpphørInnholdBygger(@KonfigVerdi(value = "BREV_DAGENS_DATO_TEST", required = false) LocalDate overrideDagensDatoForTest) {
        this.overrideDagensDatoForTest = overrideDagensDatoForTest;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var opphørStartdato = resultatTidslinje.filterValue(it -> it.resultatInfo().stream()
                .anyMatch(r -> r.detaljertResultatType() == DetaljertResultatType.ENDRING_SLUTTDATO))
            .getMinLocalDate();

        var sisteUtbetalingsdato = PeriodeBeregner.utledFremtidigUtbetalingsdato(
            opphørStartdato.minusDays(1),
            bestemInneværendeMåned());

        return new TemplateInnholdResultat(TemplateType.AUTOMATISK_OPPHOR,
            new AutomatiskOpphørDto(
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

