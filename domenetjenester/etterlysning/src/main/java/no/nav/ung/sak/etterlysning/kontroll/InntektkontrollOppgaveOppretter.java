package no.nav.ung.sak.etterlysning.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.function.Function;

@Dependent
public class InntektkontrollOppgaveOppretter {

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private final UngOppgaveKlient ungOppgaveKlient;

    @Inject
    public InntektkontrollOppgaveOppretter(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                           UngOppgaveKlient ungOppgaveKlient) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.ungOppgaveKlient = ungOppgaveKlient;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        var oppgaveDtoer = etterlysninger.stream().map(mapTilDto(behandling.getId(), deltakerIdent)).toList();
        oppgaveDtoer.forEach(ungOppgaveKlient::opprettKontrollerRegisterInntektOppgave);
    }

    private Function<Etterlysning, RegisterInntektOppgaveDTO> mapTilDto(long behandlingId, PersonIdent deltakerIdent) {
        return etterlysning -> {
            final var grunnlag = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, etterlysning.getGrunnlagsreferanse());
            return new RegisterInntektOppgaveDTO(deltakerIdent.getIdent(),
                etterlysning.getEksternReferanse(),
                etterlysning.getFrist(),
                etterlysning.getPeriode().getFomDato(),
                etterlysning.getPeriode().getTomDato(),
                InntektKontrollOppgaveMapper.mapTilRegisterInntekter(grunnlag, mapTilHelMåned(etterlysning.getPeriode())));
        };
    }

    private DatoIntervallEntitet mapTilHelMåned(DatoIntervallEntitet periode) {
        if (periode.getFomDato().getMonth() != periode.getTomDato().getMonth() || periode.getFomDato().getYear() != periode.getTomDato().getYear()) {
            throw new IllegalArgumentException("Periode må være innenfor samme måned for inntektskontrolloppgave");
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(
            periode.getFomDato().withDayOfMonth(1),
            periode.getTomDato().with(TemporalAdjusters.lastDayOfMonth())
        );
    }


}
