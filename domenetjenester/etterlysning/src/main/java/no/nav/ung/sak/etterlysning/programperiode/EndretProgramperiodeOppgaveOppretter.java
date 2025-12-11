package no.nav.ung.sak.etterlysning.programperiode;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.ungdomsprogram.EndretPeriodeOgTypeTjeneste;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_ENDE;

@Dependent
public class EndretProgramperiodeOppgaveOppretter {

    private final UngOppgaveKlient ungOppgaveKlient;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final EndretPeriodeOgTypeTjeneste endretPeriodeOgTypeTjeneste;


    @Inject
    public EndretProgramperiodeOppgaveOppretter(UngOppgaveKlient ungOppgaveKlient,
                                                UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                EndretPeriodeOgTypeTjeneste endretPeriodeOgTypeTjeneste) {
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.endretPeriodeOgTypeTjeneste = endretPeriodeOgTypeTjeneste;
    }


    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        var originalPeriode = behandling.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag).map(UngdomsprogramPeriodeGrunnlag::hentForEksaktEnPeriode);

        var startDatoOppgaveDtoer = etterlysninger.stream()
            .filter(e -> endretPeriodeOgTypeTjeneste.finnEndretPeriodeDatoOgEndringType(e).getEndringType().equals(EndringType.ENDRET_STARTDATO))
            .map(etterlysning -> mapTilStartdatoOppgaveDto(etterlysning, deltakerIdent, originalPeriode)).toList();
        startDatoOppgaveDtoer.forEach(ungOppgaveKlient::opprettEndretStartdatoOppgave);

        var sluttOppgaveDtoer = etterlysninger.stream()
            .filter(e -> endretPeriodeOgTypeTjeneste.finnEndretPeriodeDatoOgEndringType(e).getEndringType().equals(EndringType.ENDRET_SLUTTDATO))
            .map(etterlysning -> mapTilSluttdatoOppgaveDto(etterlysning, deltakerIdent, originalPeriode)).toList();
        sluttOppgaveDtoer.forEach(ungOppgaveKlient::opprettEndretSluttdatoOppgave);

        var endretPeriodeOppgaveDtoer = etterlysninger.stream()
            .filter(e -> endretPeriodeOgTypeTjeneste.finnEndretPeriodeDatoOgEndringType(e).getEndringType().equals(EndringType.ENDRET_SLUTTDATO))
            .map(etterlysning -> mapTilStartdatoOppgaveDto(etterlysning, deltakerIdent, originalPeriode)).toList();
        endretPeriodeOppgaveDtoer.forEach(ungOppgaveKlient::opprettEndretProgramperiodeOppgave);
    }

    private EndretStartdatoOppgaveDTO mapTilStartdatoOppgaveDto(Etterlysning etterlysning, PersonIdent deltakerIdent, Optional<DatoIntervallEntitet> originalPeriode) {
        var fom = originalPeriode.orElseThrow(() ->new IllegalArgumentException("Perioden mangler fom dato")).getFomDato();
        return new EndretStartdatoOppgaveDTO(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            etterlysning.getFrist(),
            hentStartdato(etterlysning),
            fom
        );
    }

    private LocalDate hentStartdato(Etterlysning etterlysning) {
        return ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse())
            .getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getFomDato();
    }

    private EndretSluttdatoOppgaveDTO mapTilSluttdatoOppgaveDto(Etterlysning etterlysning, PersonIdent deltakerIdent, Optional<DatoIntervallEntitet> originalPeriode) {
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
            .getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getTomDato();
    }
}
