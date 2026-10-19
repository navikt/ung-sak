package no.nav.ung.sak.etterlysning.opphorvedmaksdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.journalforing.JournalføringDto;
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
        var saksnummer = behandling.getFagsak().getSaksnummer();
        etterlysninger.stream()
            .map(etterlysning -> mapTilDto(etterlysning, aktørId, ytelsetype, saksnummer))
            .forEach(oppgaveKlient::opprettOppgave);
    }


    private OpprettOppgaveDto mapTilDto(Etterlysning etterlysning, AktørId aktørId, OppgaveYtelsetype ytelsetype, no.nav.ung.sak.typer.Saksnummer saksnummer) {
        UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse());
        LocalDate maksdato = ungdomsprogramPeriodeGrunnlag.getPeriodeMaksDato().orElseThrow(() -> new IllegalStateException("Forventer å finne maksdato"));
        LocalDate sluttdato = utledSisteDagMedYtelse(ungdomsprogramPeriodeGrunnlag.hentForEksaktEnPeriode().getTomDato(), maksdato);
        return new OpprettOppgaveDto(
            new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
            ytelsetype,
            etterlysning.getEksternReferanse(),
            new BekreftOpphorVedMaksdatoOppgavetypeDataDto(sluttdato, maksdato),
            etterlysning.getFrist(),
            new JournalføringDto(new no.nav.ung.brukerdialog.typer.Saksnummer(saksnummer.getVerdi()))
        );
    }

    /**
     * Sluttdatoen vises til deltaker som siste dag med ytelsen. Programperioden er typisk åpen
     * (tom = TIDENES_ENDE), så siste dag med ytelse er den tidligste av programperiodens tom og maksdato.
     */
    private static LocalDate utledSisteDagMedYtelse(LocalDate programperiodeTom, LocalDate maksdato) {
        return programperiodeTom.isBefore(maksdato) ? programperiodeTom : maksdato;
    }
}

