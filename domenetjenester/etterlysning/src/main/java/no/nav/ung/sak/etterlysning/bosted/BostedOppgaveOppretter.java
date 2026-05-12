package no.nav.ung.sak.etterlysning.bosted;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bosted.BekreftBostedOppgavetypeDataDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.etterlysning.OppgaveYtelsetypeMapper;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;

import java.util.List;

@Dependent
public class BostedOppgaveOppretter {

    private final UngBrukerdialogOppgaveKlient oppgaveKlient;
    private final BostedsGrunnlagRepository bostedsGrunnlagRepository;

    @Inject
    public BostedOppgaveOppretter(UngBrukerdialogOppgaveKlient oppgaveKlient,
                                  BostedsGrunnlagRepository bostedsGrunnlagRepository) {
        this.oppgaveKlient = oppgaveKlient;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());

        for (Etterlysning etterlysning : etterlysninger) {
            var periodeAvklaring = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId())
                .stream()
                .flatMap(g -> g.getHolder().getPeriodeAvklaring(etterlysning.getGrunnlagsreferanse()).stream())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Fant ikke periodeAvklaring for referanse: " + etterlysning.getGrunnlagsreferanse()));

            var oppgaveDto = new OpprettOppgaveDto(
                new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
                ytelsetype,
                etterlysning.getEksternReferanse(),
                new BekreftBostedOppgavetypeDataDto(
                    etterlysning.getPeriode().getFomDato(),
                    etterlysning.getPeriode().getTomDato(),
                    periodeAvklaring.isErBosattITrondheim()
                ),
                etterlysning.getFrist()
            );
            oppgaveKlient.opprettOppgave(oppgaveDto);
        }
    }
}
