package no.nav.ung.sak.etterlysning.opphorvedmaksdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.opphorvedmaksdato.BekreftOpphorVedMaksdatoOppgavetypeDataDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.etterlysning.OppgaveYtelsetypeMapper;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

@Dependent
public class OpphørVedMaksdatoOppgaveOppretter {

    private static final Logger log = LoggerFactory.getLogger(OpphørVedMaksdatoOppgaveOppretter.class);
    private static final int VARSEL_UKER_FØR_MAKSDATO = 4;
    private static final int VARSEL_GRACE_DAGER_ETTER_MAKSDATO = 3;

    private final UngBrukerdialogOppgaveKlient oppgaveKlient;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public OpphørVedMaksdatoOppgaveOppretter(UngBrukerdialogOppgaveKlient oppgaveKlient,
                                            UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.oppgaveKlient = oppgaveKlient;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());
        etterlysninger.stream()
            .filter(this::erGyldigForOppgaveOpprettelse)
            .map(etterlysning -> mapTilDto(etterlysning, aktørId, ytelsetype))
            .forEach(oppgaveKlient::opprettOppgave);
    }

    private boolean erGyldigForOppgaveOpprettelse(Etterlysning etterlysning) {
        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse());
        var maksdato = grunnlag.getPeriodeMaksDato().orElse(etterlysning.getPeriode().getTomDato());
        var gyldig = erInnenforVarselvindu(maksdato, LocalDate.now());
        if (!gyldig) {
            log.info("Oppretter ikke oppgave for etterlysning {}: maksdato {} fra grunnlag {} er utenfor varselvindu",
                etterlysning.getEksternReferanse(), maksdato, etterlysning.getGrunnlagsreferanse());
        }
        return gyldig;
    }

    private boolean erInnenforVarselvindu(LocalDate maksdato, LocalDate dagensDato) {
        var fireUkerFrem = dagensDato.plusWeeks(VARSEL_UKER_FØR_MAKSDATO);
        return !maksdato.isAfter(fireUkerFrem)
            && !maksdato.isBefore(dagensDato.minusDays(VARSEL_GRACE_DAGER_ETTER_MAKSDATO));
    }

    private OpprettOppgaveDto mapTilDto(Etterlysning etterlysning, AktørId aktørId, OppgaveYtelsetype ytelsetype) {
        LocalDate sluttdato = etterlysning.getPeriode().getTomDato();
        return new OpprettOppgaveDto(
            new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
            ytelsetype,
            etterlysning.getEksternReferanse(),
            new BekreftOpphorVedMaksdatoOppgavetypeDataDto(sluttdato, sluttdato),
            etterlysning.getFrist()
        );
    }
}

