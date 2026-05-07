package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.EndringUtvidetKvoteDto;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;

import java.time.LocalDate;

@Dependent
public class UtvidetKvoteInnholdBygger implements VedtaksbrevInnholdBygger {

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @Inject
    public UtvidetKvoteInnholdBygger(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                     UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var nyProgramperiode = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId()).toSegments().last();
        LocalDate nyMaksDato = nyProgramperiode.getTom();

        var originalBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Trenger forrige behandling ved utvidelse av kvote"));

        UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(originalBehandlingId).orElseThrow();
        LocalDate opprinneligMaksDato = FagsakperiodeUtleder.finnTomDato(ungdomsprogramPeriodeGrunnlag);

        return new TemplateInnholdResultat(TemplateType.FORLENGET_PERIODE,
            new EndringUtvidetKvoteDto(opprinneligMaksDato, nyMaksDato));
    }
}
