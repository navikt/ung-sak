package no.nav.ung.sak.etterlysning.startdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretstartdato.OpprettEndretStartdatoOppgaveDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.etterlysning.MidlertidigOppgaveDelegeringTjeneste;
import no.nav.ung.sak.tid.DatoIntervallEntitet;

import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Dependent
public class EndretStartdatoOppgaveOppretter {

    private final MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;

    @Inject
    public EndretStartdatoOppgaveOppretter(MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste,
                                           UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                           UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository) {
        this.delegeringTjeneste = delegeringTjeneste;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
    }


    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        var originalPeriode = finnOriginalPeriode(behandling);
        var oppgaveDtoer = etterlysninger.stream().map(etterlysning -> mapTilDto(etterlysning, deltakerIdent, originalPeriode)).toList();
        oppgaveDtoer.forEach(delegeringTjeneste::opprettEndretStartdatoOppgave);
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

    private OpprettEndretStartdatoOppgaveDto mapTilDto(Etterlysning etterlysning, PersonIdent deltakerIdent, DatoIntervallEntitet originalPeriode) {
        return new OpprettEndretStartdatoOppgaveDto(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            hentStartdato(etterlysning),
            originalPeriode.getFomDato(),
            etterlysning.getFrist()
        );
    }

    private LocalDate hentStartdato(Etterlysning etterlysning) {
        return ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse())
            .getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getFomDato();
    }
}
