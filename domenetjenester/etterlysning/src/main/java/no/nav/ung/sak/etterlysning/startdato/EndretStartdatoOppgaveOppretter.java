package no.nav.ung.sak.etterlysning.startdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.etterlysning.OppgaveYtelsetypeMapper;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretstartdato.EndretStartdatoDataDto;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;
import java.util.List;

@Dependent
public class EndretStartdatoOppgaveOppretter {

    private final UngBrukerdialogOppgaveKlient oppgaveKlient;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public EndretStartdatoOppgaveOppretter(UngBrukerdialogOppgaveKlient oppgaveKlient,
                                           UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.oppgaveKlient = oppgaveKlient;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        var originalPeriode = finnOriginalPeriode(behandling);
        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());
        etterlysninger.stream()
            .map(etterlysning -> mapTilDto(etterlysning, aktørId, ytelsetype, originalPeriode))
            .forEach(oppgaveKlient::opprettOppgave);
    }

    private OpprettOppgaveDto mapTilDto(Etterlysning etterlysning, AktørId aktørId, OppgaveYtelsetype ytelsetype, DatoIntervallEntitet originalPeriode) {
        return new OpprettOppgaveDto(
            new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
            ytelsetype,
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
