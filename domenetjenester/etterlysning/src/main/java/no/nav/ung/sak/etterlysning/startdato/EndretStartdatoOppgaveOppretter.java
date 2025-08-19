package no.nav.ung.sak.etterlysning.startdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.PersonIdent;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Dependent
public class EndretStartdatoOppgaveOppretter {

    private UngOppgaveKlient ungOppgaveKlient;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;

    @Inject
    public EndretStartdatoOppgaveOppretter(UngOppgaveKlient ungOppgaveKlient,
                                           UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                           UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository) {
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
    }


    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        var originalPeriode = finnOriginalPeriode(behandling);
        var oppgaveDtoer = etterlysninger.stream().map(etterlysning -> mapTilDto(etterlysning, deltakerIdent, originalPeriode)).toList();
        oppgaveDtoer.forEach(ungOppgaveKlient::opprettEndretStartdatoOppgave);
    }

    private DatoIntervallEntitet finnOriginalPeriode(Behandling behandling) {
        var startdato = ungdomsytelseStartdatoRepository.hentGrunnlag(behandling.getId()).map(UngdomsytelseStartdatoGrunnlag::getOppgitteStartdatoer).map(UngdomsytelseStartdatoer::getStartdatoer)
            .stream()
            .flatMap(Set::stream)
            .map(UngdomsytelseSøktStartdato::getStartdato)
            .min(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("Forventer å finne startdato for behandling med id: " + behandling.getId()));
        return behandling.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag).map(UngdomsprogramPeriodeGrunnlag::hentForEksaktEnPeriode)
            .orElse(DatoIntervallEntitet.fraOgMed(startdato));
    }

    private EndretStartdatoOppgaveDTO mapTilDto(Etterlysning etterlysning, PersonIdent deltakerIdent, DatoIntervallEntitet originalPeriode) {
        return new EndretStartdatoOppgaveDTO(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            etterlysning.getFrist(),
            hentStartdato(etterlysning),
            originalPeriode.getFomDato()
        );
    }

    private LocalDate hentStartdato(Etterlysning etterlysning) {
        return ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse())
            .getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getFomDato();
    }
}
