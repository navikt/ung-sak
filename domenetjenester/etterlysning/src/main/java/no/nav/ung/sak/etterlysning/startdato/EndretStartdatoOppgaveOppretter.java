package no.nav.ung.sak.etterlysning.startdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Dependent
public class EndretStartdatoOppgaveOppretter {

    private UngOppgaveKlient ungOppgaveKlient;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public EndretStartdatoOppgaveOppretter(UngOppgaveKlient ungOppgaveKlient,
                                           UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }


    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        var originalPeriode = behandling.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag).map(UngdomsprogramPeriodeGrunnlag::hentForEksaktEnPeriode);
        var oppgaveDtoer = etterlysninger.stream().map(etterlysning -> mapTilDto(etterlysning, deltakerIdent, originalPeriode)).toList();
        oppgaveDtoer.forEach(ungOppgaveKlient::opprettEndretStartdatoOppgave);
    }

    private EndretStartdatoOppgaveDTO mapTilDto(Etterlysning etterlysning, PersonIdent deltakerIdent, Optional<DatoIntervallEntitet> originalPeriode) {
        return new EndretStartdatoOppgaveDTO(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            etterlysning.getFrist(),
            hentStartdato(etterlysning),
            originalPeriode.map(DatoIntervallEntitet::getFomDato).orElseThrow((() -> new IllegalStateException("Forventer å finne original startdato")))
        );
    }

    private LocalDate hentStartdato(Etterlysning etterlysning) {
        return ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse())
            .orElseThrow(() -> new IllegalStateException("Forventer å finne startdato for etterlysning med grunnlagsreferanse: " + etterlysning.getGrunnlagsreferanse()))
            .getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getFomDato();
    }
}
