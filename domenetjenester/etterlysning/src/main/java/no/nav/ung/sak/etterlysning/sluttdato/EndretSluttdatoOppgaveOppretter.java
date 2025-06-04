package no.nav.ung.sak.etterlysning.sluttdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
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
import java.util.UUID;

import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_ENDE;

@Dependent
public class EndretSluttdatoOppgaveOppretter {

    private final UngOppgaveKlient ungOppgaveKlient;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public EndretSluttdatoOppgaveOppretter(
                                           UngOppgaveKlient ungOppgaveKlient,
                                           UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        var originalPeriode = behandling.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag).map(UngdomsprogramPeriodeGrunnlag::hentForEksaktEnPeriode);
        var oppgaveDtoer = etterlysninger.stream().map(etterlysning -> mapTilDto(etterlysning, deltakerIdent, originalPeriode)).toList();
        oppgaveDtoer.forEach(ungOppgaveKlient::opprettEndretSluttdatoOppgave);
    }

    private EndretSluttdatoOppgaveDTO mapTilDto(Etterlysning etterlysning, PersonIdent deltakerIdent, Optional<DatoIntervallEntitet> originalPeriode) {
        return new EndretSluttdatoOppgaveDTO(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            etterlysning.getFrist(),
            hentSluttdato(etterlysning.getGrunnlagsreferanse()),
            originalPeriode.map(DatoIntervallEntitet::getTomDato).filter(d -> !d.equals(TIDENES_ENDE)).orElse(null)
        );
    }

    private LocalDate hentSluttdato(UUID grunnlagsreferanse) {
        return ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(grunnlagsreferanse)
            .orElseThrow(() -> new IllegalStateException("Forventer å finne grunnlag for etterlysning med grunnlagsreferanse: " + grunnlagsreferanse))
            .getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getTomDato();
    }
}
