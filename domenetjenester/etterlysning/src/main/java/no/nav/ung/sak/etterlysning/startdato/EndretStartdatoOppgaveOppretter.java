package no.nav.ung.sak.etterlysning.startdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.etterlysning.MidlertidigOppgaveDelegeringTjeneste;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretstartdato.EndretStartdatoDataDto;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;
import java.util.List;

@Dependent
public class EndretStartdatoOppgaveOppretter {

    private final MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public EndretStartdatoOppgaveOppretter(MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste,
                                           UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.delegeringTjeneste = delegeringTjeneste;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        var originalPeriode = finnOriginalPeriode(behandling);
        etterlysninger.stream()
            .map(etterlysning -> mapTilDto(etterlysning, aktørId, originalPeriode))
            .forEach(delegeringTjeneste::opprettOppgave);
    }

    private OpprettOppgaveDto mapTilDto(Etterlysning etterlysning, AktørId aktørId, DatoIntervallEntitet originalPeriode) {
        return new OpprettOppgaveDto(
            aktørId,
            etterlysning.getEksternReferanse(),
            new EndretStartdatoDataDto(hentStartdato(etterlysning), originalPeriode.getFomDato()),
            etterlysning.getFrist()
        );
    }

    private DatoIntervallEntitet finnOriginalPeriode(Behandling behandling) {
        return ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Fant ikke periodegrunnlag for behandling " + behandling.getId()))
            .hentForEksaktEnPeriodeDersomFinnes()
            .orElseThrow(() -> new IllegalStateException("Fant ikke periode for behandling " + behandling.getId()));
    }

    private LocalDate hentStartdato(Etterlysning etterlysning) {
        return ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse())
            .getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getFomDato();
    }
}
