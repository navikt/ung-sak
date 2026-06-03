package no.nav.ung.sak.etterlysning.opphorvedmaksdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.opphorvedmaksdato.BekreftOpphorVedMaksdatoOppgavetypeDataDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.etterlysning.OppgaveYtelsetypeMapper;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;
import java.util.List;

/**
 * Oppretter oppgaver i brukerdialog for etterlysninger av type UTTALELSE_OPPHOR_VED_MAKSDATO.
 * <p>
 * Varselvindu-sjekk (om maksdato er innenfor 3 uker) gjøres oppstrøms i
 * {@code VarselOpphørVedMaksdatoTask} før revurderingen opprettes.
 * Alle etterlysninger som når dette punktet skal alltid resultere i en oppgave.
 */
@Dependent
public class OpphørVedMaksdatoOppgaveOppretter {

    private final UngBrukerdialogOppgaveKlient oppgaveKlient;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public OpphørVedMaksdatoOppgaveOppretter(UngBrukerdialogOppgaveKlient oppgaveKlient, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.oppgaveKlient = oppgaveKlient;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());
        etterlysninger.stream()
            .map(etterlysning -> mapTilDto(etterlysning, aktørId, ytelsetype))
            .forEach(oppgaveKlient::opprettOppgave);
    }


    private OpprettOppgaveDto mapTilDto(Etterlysning etterlysning, AktørId aktørId, OppgaveYtelsetype ytelsetype) {
        UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse());
        LocalDate sluttdato = ungdomsprogramPeriodeGrunnlag.hentForEksaktEnPeriode().getTomDato();
        LocalDate maksdato = ungdomsprogramPeriodeGrunnlag.getPeriodeMaksDato().orElseThrow(() -> new IllegalStateException("Forventer å finne maksdato"));
        return new OpprettOppgaveDto(
            new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
            ytelsetype,
            etterlysning.getEksternReferanse(),
            new BekreftOpphorVedMaksdatoOppgavetypeDataDto(sluttdato, maksdato),
            etterlysning.getFrist()
        );
    }
}

