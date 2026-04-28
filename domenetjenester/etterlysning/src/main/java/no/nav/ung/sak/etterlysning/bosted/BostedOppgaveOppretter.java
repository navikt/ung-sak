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
            var grunnlag = bostedsGrunnlagRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse())
                .orElseThrow(() -> new IllegalStateException("Fant ikke bostedsgrunnlag for grunnlagsreferanse: " + etterlysning.getGrunnlagsreferanse()));

            var fom = etterlysning.getPeriode().getFomDato();
            Boolean erBosattITrondheim = grunnlag.getForeslåttHolder().getAvklaringer().stream()
                .filter(a -> a.getFomDato().equals(fom))
                .findFirst()
                .map(BostedsAvklaring::erBosattITrondheim)
                .orElseThrow(() -> new IllegalStateException("Fant ikke bostedsavklaring for fom-dato " + fom + " i grunnlag med referanse " + etterlysning.getGrunnlagsreferanse()));

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
