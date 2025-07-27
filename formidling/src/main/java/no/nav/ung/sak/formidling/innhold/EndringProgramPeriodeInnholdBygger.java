package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.template.dto.EndringProgramPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.endring.programperiode.EndretSluttDato;
import no.nav.ung.sak.formidling.template.dto.endring.programperiode.EndretStartDato;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;

@Dependent
public class EndringProgramPeriodeInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = LoggerFactory.getLogger(EndringProgramPeriodeInnholdBygger.class);

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final LocalDate overrideDagensDatoForTest;

    @Inject
    public EndringProgramPeriodeInnholdBygger(
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
        @KonfigVerdi(value = "BREV_DAGENS_DATO_TEST", required = false) LocalDate overrideDagensDatoForTest) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.overrideDagensDatoForTest = overrideDagensDatoForTest;
    }


    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var denneProgramPerioder = hentProgramperiodeTidslinje(behandling.getId());
        if (denneProgramPerioder.size() > 1) {
            LOG.warn("Fant flere enn 1 programperiode={} i denne behandlingen. Bruker den siste", denneProgramPerioder);
        }

        var forrigeProgramPerioder = hentProgramperiodeTidslinje(behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Trenger forrige behandling ved endring av programperiode")));

        if (forrigeProgramPerioder.size() > 1) {
            LOG.warn("Fant flere enn 1 programperiode={} i forrige behandling. Bruker den siste", forrigeProgramPerioder);
        }

        var denneProgramperiode = denneProgramPerioder.toSegments().last();
        var forrigeProgramperiode = forrigeProgramPerioder.toSegments().last();

        var endretStartdato = !denneProgramperiode.getFom().equals(forrigeProgramperiode.getFom()) ?
            new EndretStartDato(denneProgramperiode.getFom(), forrigeProgramperiode.getFom()) : null;

        var endretSluttdato = !denneProgramperiode.getTom().equals(forrigeProgramperiode.getTom()) ?
            lagEndretSluttdato(denneProgramperiode, forrigeProgramperiode) : null;

        return new TemplateInnholdResultat(DokumentMalType.ENDRING_PROGRAMPERIODE, TemplateType.ENDRING_PROGRAMPERIODE,
            new EndringProgramPeriodeDto(
                endretStartdato, endretSluttdato,
                false
            ));
    }

    private EndretSluttDato lagEndretSluttdato(LocalDateSegment<Boolean> denneProgramperiode, LocalDateSegment<Boolean> forrigeProgramperiode) {
        var sluttDato = denneProgramperiode.getTom();
        var opphørStartDato = sluttDato.plusDays(1);

        var sisteUtbetalingsdato = PeriodeBeregner.utledFremtidigUtbetalingsdato(
            sluttDato, bestemInneværendeMåned());

        return new EndretSluttDato(sluttDato, forrigeProgramperiode.getTom(), opphørStartDato, sisteUtbetalingsdato);
    }

    private YearMonth bestemInneværendeMåned() {
        return Environment.current().isLocal() && overrideDagensDatoForTest != null ?
            YearMonth.from(overrideDagensDatoForTest)
            : YearMonth.now();
    }

    private LocalDateTimeline<Boolean> hentProgramperiodeTidslinje(Long behandlingid) {
        return new LocalDateTimeline<>(ungdomsprogramPeriodeRepository
            .hentGrunnlag(behandlingid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke grunnlag for behandling"))
            .getUngdomsprogramPerioder()
            .getPerioder().stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
            .toList()
        );
    }

}
