package no.nav.ung.sak.etterlysning.programperiode;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.EndretPeriodeOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.PeriodeEndringType;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.PeriodeDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_ENDE;

@Dependent
public class EndretPeriodeOppgaveOppretter {

    private final UngOppgaveKlient ungOppgaveKlient;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;


    @Inject
    public EndretPeriodeOppgaveOppretter(UngOppgaveKlient ungOppgaveKlient,
                                         UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }


    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        if (etterlysninger.isEmpty()) {
            return;
        }
        UngdomsprogramPeriodeGrunnlag initieltPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentInitiell(behandling.getId()).orElseThrow(() ->
            new IllegalStateException("Klarte ikke å innhentete originalt ungdomsprogram periodegrunnlag for behandling " + behandling.getId())
        );
        if (etterlysninger.size() > 1) {
            throw new IllegalStateException("Fant flere etterlysninger for behandling " + behandling.getId());
        }
        Etterlysning etterlysning = etterlysninger.getFirst();

        UngdomsprogramPeriodeGrunnlag gjeldeneGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse());
        Optional<UngdomsprogramPeriodeTjeneste.EndretDato> endretStartDato = finnEndretStartDato(etterlysning, initieltPeriodeGrunnlag, gjeldeneGrunnlag);
        Optional<UngdomsprogramPeriodeTjeneste.EndretDato> endretSluttDato = finnEndretSluttDato(etterlysning, initieltPeriodeGrunnlag, gjeldeneGrunnlag);

        if (endretStartDato.isPresent() && endretSluttDato.isEmpty()) {
            var oppgaveDto = mapTilStartdatoOppgaveDto(etterlysning, deltakerIdent, endretStartDato.get().nyDato(), endretStartDato.get().forrigeDato());
            ungOppgaveKlient.opprettEndretStartdatoOppgave(oppgaveDto);
        } else if (endretStartDato.isEmpty() && endretSluttDato.isPresent()) {
            var oppgaveDto = mapTilSluttdatoOppgaveDto(etterlysning, deltakerIdent, endretSluttDato.get().nyDato(), endretSluttDato.get().forrigeDato());
            ungOppgaveKlient.opprettEndretSluttdatoOppgave(oppgaveDto);
        } else {
            PeriodeDTO nyPeriode = hentPeriodeFraGrunnlag(gjeldeneGrunnlag);
            PeriodeDTO forrigePeriode = hentPeriodeFraGrunnlag(initieltPeriodeGrunnlag);
            var endringer = Set.of(PeriodeEndringType.ENDRET_STARTDATO, PeriodeEndringType.ENDRET_SLUTTDATO);
            var oppgaveDto = mapTilEndretPeriodeOppgaveDto(etterlysning, deltakerIdent, nyPeriode, forrigePeriode, endringer);
            ungOppgaveKlient.opprettEndretPeriodeOppgave(oppgaveDto);
        }

    }

    private Optional<UngdomsprogramPeriodeTjeneste.EndretDato> finnEndretStartDato(Etterlysning etterlysning, UngdomsprogramPeriodeGrunnlag initieltPeriodeGrunnlag, UngdomsprogramPeriodeGrunnlag gjeldeneGrunnlag) {
        var endretStartdatoer = UngdomsprogramPeriodeTjeneste.finnEndretStartdatoer(gjeldeneGrunnlag, initieltPeriodeGrunnlag);
        if (endretStartdatoer.isEmpty()) {
            return Optional.empty();
        }
        if (endretStartdatoer.size() > 1) {
            throw new IllegalStateException("Fant flere endrede startdatoer for etterlysning " + etterlysning.getEksternReferanse());
        }
        return Optional.of(endretStartdatoer.getFirst());
    }

    private Optional<UngdomsprogramPeriodeTjeneste.EndretDato> finnEndretSluttDato(Etterlysning etterlysning, UngdomsprogramPeriodeGrunnlag initieltPeriodeGrunnlag, UngdomsprogramPeriodeGrunnlag gjeldeneGrunnlag) {
        var endretStartdatoer = UngdomsprogramPeriodeTjeneste.finnEndretSluttdatoer(gjeldeneGrunnlag, initieltPeriodeGrunnlag);
        if (endretStartdatoer.isEmpty()) {
            return Optional.empty();
        }
        if (endretStartdatoer.size() > 1) {
            throw new IllegalStateException("Fant flere endrede startdatoer for etterlysning " + etterlysning.getEksternReferanse());
        }
        return Optional.of(endretStartdatoer.getFirst());
    }

    private EndretPeriodeOppgaveDTO mapTilEndretPeriodeOppgaveDto(Etterlysning etterlysning, PersonIdent deltakerIdent, PeriodeDTO nyPeriode, PeriodeDTO forrigePeriode, Set<PeriodeEndringType> endringer) {

        return new EndretPeriodeOppgaveDTO(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            etterlysning.getFrist(),
            nyPeriode,
            forrigePeriode,
            endringer
        );
    }

    private PeriodeDTO hentPeriodeFraGrunnlag(UngdomsprogramPeriodeGrunnlag periodeGrunnlag) {
        LocalDate fomDato = periodeGrunnlag.getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getFomDato();
        LocalDate tomDato = periodeGrunnlag.getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getTomDato();
        return new PeriodeDTO(fomDato, tomDato);
    }

    private EndretStartdatoOppgaveDTO mapTilStartdatoOppgaveDto(Etterlysning etterlysning, PersonIdent deltakerIdent, LocalDate nyStartDato, LocalDate forrigeStartDato) {
        return new EndretStartdatoOppgaveDTO(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            etterlysning.getFrist(),
            nyStartDato,
            forrigeStartDato
        );
    }

    private EndretSluttdatoOppgaveDTO mapTilSluttdatoOppgaveDto(Etterlysning etterlysning, PersonIdent deltakerIdent, LocalDate nySluttDato, LocalDate forrigeSluttDato) {
        return new EndretSluttdatoOppgaveDTO(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            etterlysning.getFrist(),
            nySluttDato,
            forrigeSluttDato.equals(TIDENES_ENDE) ? null : forrigeSluttDato
        );
    }

}
