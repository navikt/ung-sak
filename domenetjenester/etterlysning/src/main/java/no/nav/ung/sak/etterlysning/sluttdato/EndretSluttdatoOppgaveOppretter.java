package no.nav.ung.sak.etterlysning.sluttdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato.EndretSluttdatoDataDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.etterlysning.MidlertidigOppgaveDelegeringTjeneste;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.ung.sak.tid.AbstractLocalDateInterval.TIDENES_ENDE;

@Dependent
public class EndretSluttdatoOppgaveOppretter {

    private final MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public EndretSluttdatoOppgaveOppretter(
        MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste,
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.delegeringTjeneste = delegeringTjeneste;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        var originalPeriode = behandling.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag).map(UngdomsprogramPeriodeGrunnlag::hentForEksaktEnPeriode);
        etterlysninger.stream()
            .map(etterlysning -> mapTilDto(etterlysning, deltakerIdent, originalPeriode))
            .forEach(delegeringTjeneste::opprettOppgave);
    }

    private OpprettOppgaveDto mapTilDto(Etterlysning etterlysning, PersonIdent deltakerIdent, Optional<DatoIntervallEntitet> originalPeriode) {
        return new OpprettOppgaveDto(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            new EndretSluttdatoDataDto(
                hentSluttdato(etterlysning.getGrunnlagsreferanse()),
                originalPeriode.map(DatoIntervallEntitet::getTomDato).filter(d -> !d.equals(TIDENES_ENDE)).orElse(null)
            ),
            etterlysning.getFrist()
        );
    }

    private LocalDate hentSluttdato(UUID grunnlagsreferanse) {
        return ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(grunnlagsreferanse)
            .getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getTomDato();
    }
}
