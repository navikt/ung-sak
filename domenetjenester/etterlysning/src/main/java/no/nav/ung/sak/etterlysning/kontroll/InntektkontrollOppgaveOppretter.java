package no.nav.ung.sak.etterlysning.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.kontroll.RapportertInntektMapper;
import no.nav.ung.sak.typer.PersonIdent;

import java.util.List;
import java.util.function.Function;

@Dependent
public class InntektkontrollOppgaveOppretter {

    private final UngOppgaveKlient ungOppgaveKlient;
    private final RapportertInntektMapper rapportertInntektMapper;

    @Inject
    public InntektkontrollOppgaveOppretter(UngOppgaveKlient ungOppgaveKlient, RapportertInntektMapper rapportertInntektMapper) {
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.rapportertInntektMapper = rapportertInntektMapper;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        var oppgaveDtoer = etterlysninger.stream().map(mapTilDto(behandling.getId(), deltakerIdent)).toList();
        oppgaveDtoer.forEach(ungOppgaveKlient::opprettKontrollerRegisterInntektOppgave);
    }

    private Function<Etterlysning, RegisterInntektOppgaveDTO> mapTilDto(long behandlingId, PersonIdent deltakerIdent) {
        return etterlysning -> {
            var registerinntekter = rapportertInntektMapper.finnRegisterinntekterForPeriodeOgGrunnlag(behandlingId, etterlysning.getGrunnlagsreferanse(), etterlysning.getPeriode().toLocalDateInterval());
            return new RegisterInntektOppgaveDTO(deltakerIdent.getIdent(),
                etterlysning.getEksternReferanse(),
                etterlysning.getFrist(),
                etterlysning.getPeriode().getFomDato(),
                etterlysning.getPeriode().getTomDato(),
                InntektKontrollOppgaveMapper.mapTilRegisterInntekter(registerinntekter));
        };
    }


}
