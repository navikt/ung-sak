package no.nav.ung.sak.etterlysning.automatiskopphor;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.automatiskopphor.BekreftAutomatiskOpphorOppgavetypeDataDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.etterlysning.OppgaveYtelsetypeMapper;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;
import java.util.List;

@Dependent
public class AutomatiskOpphørOppgaveOppretter {

    private final UngBrukerdialogOppgaveKlient oppgaveKlient;

    @Inject
    public AutomatiskOpphørOppgaveOppretter(UngBrukerdialogOppgaveKlient oppgaveKlient) {
        this.oppgaveKlient = oppgaveKlient;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());
        etterlysninger.stream()
            .map(etterlysning -> mapTilDto(etterlysning, aktørId, ytelsetype))
            .forEach(oppgaveKlient::opprettOppgave);
    }

    private OpprettOppgaveDto mapTilDto(Etterlysning etterlysning, AktørId aktørId, OppgaveYtelsetype ytelsetype) {
        LocalDate sluttdato = etterlysning.getPeriode().getTomDato();
        return new OpprettOppgaveDto(
            new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
            ytelsetype,
            etterlysning.getEksternReferanse(),
            new BekreftAutomatiskOpphorOppgavetypeDataDto(sluttdato, sluttdato),
            etterlysning.getFrist()
        );
    }
}

