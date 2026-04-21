package no.nav.ung.sak.etterlysning.bosted;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bosted.BekreftBostedOppgavetypeDataDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.etterlysning.OppgaveYtelsetypeMapper;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        var grunnlag = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Fant ikke bostedsgrunnlag for behandling: " + behandling.getId()));

        Map<java.time.LocalDate, Boolean> bosattPerSkjæringstidspunkt = grunnlag.getForeslåttHolder().getAvklaringer().stream()
            .collect(Collectors.toMap(BostedsAvklaring::getSkjæringstidspunkt, BostedsAvklaring::erBosattITrondheim));

        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());

        for (Etterlysning etterlysning : etterlysninger) {
            var fom = etterlysning.getPeriode().getFomDato();
            Boolean erBosattITrondheim = bosattPerSkjæringstidspunkt.get(fom);
            if (erBosattITrondheim == null) {
                throw new IllegalStateException("Fant ikke bostedsavklaring for skjæringstidspunkt " + fom + " i behandling " + behandling.getId());
            }
            var oppgaveDto = new OpprettOppgaveDto(
                new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
                ytelsetype,
                etterlysning.getEksternReferanse(),
                new BekreftBostedOppgavetypeDataDto(
                    fom,
                    etterlysning.getPeriode().getTomDato(),
                    erBosattITrondheim
                ),
                etterlysning.getFrist()
            );
            oppgaveKlient.opprettOppgave(oppgaveDto);
        }
    }
}
