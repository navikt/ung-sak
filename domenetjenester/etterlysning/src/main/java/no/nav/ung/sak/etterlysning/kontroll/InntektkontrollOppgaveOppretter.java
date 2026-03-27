package no.nav.ung.sak.etterlysning.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgavetypeDataDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.ung.sak.etterlysning.MidlertidigOppgaveDelegeringTjeneste;
import no.nav.ung.sak.etterlysning.OppgaveYtelsetypeMapper;
import no.nav.ung.sak.kontroll.InntekterForKilde;
import no.nav.ung.sak.kontroll.RapportertInntektMapper;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;
import no.nav.ung.sak.typer.AktørId;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Dependent
public class InntektkontrollOppgaveOppretter {

    private final MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste;
    private final RapportertInntektMapper rapportertInntektMapper;
    private final ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private final RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;

    @Inject
    public InntektkontrollOppgaveOppretter(MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste,
                                           RapportertInntektMapper rapportertInntektMapper,
                                           ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                           RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder) {
        this.delegeringTjeneste = delegeringTjeneste;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.relevanteKontrollperioderUtleder = relevanteKontrollperioderUtleder;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        var relevantKontrolltidslinje = relevanteKontrollperioderUtleder.utledPerioderRelevantForKontrollAvInntekt(behandling.getId());
        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());
        etterlysninger.stream()
            .map(mapTilDto(behandling.getId(), aktørId, relevantKontrolltidslinje, ytelsetype))
            .forEach(delegeringTjeneste::opprettOppgave);
    }

    private Function<Etterlysning, OpprettOppgaveDto> mapTilDto(long behandlingId, AktørId aktørId,
                                                                LocalDateTimeline<RelevanteKontrollperioderUtleder.InfoOmRådata> relevantKontrollTidslinje, OppgaveYtelsetype ytelsetype) {
        return etterlysning -> {
            var registerinntekter = rapportertInntektMapper.finnRegisterinntekterForPeriodeOgGrunnlag(behandlingId, etterlysning.getGrunnlagsreferanse(), etterlysning.getPeriode().toLocalDateInterval());
            List<ArbeidsgiverOpplysninger> arbeidsgiverOpplysninger = registerinntekter.stream().map(InntekterForKilde::arbeidsgiver)
                .distinct()
                .map(arbeidsgiverTjeneste::hent)
                .collect(Collectors.toList());
            LocalDateInterval etterlysningPeriode = etterlysning.getPeriode().toLocalDateInterval();
            return new OpprettOppgaveDto(
                new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
                ytelsetype,
                etterlysning.getEksternReferanse(),
                new KontrollerRegisterinntektOppgavetypeDataDto(
                    etterlysning.getPeriode().getFomDato(),
                    etterlysning.getPeriode().getTomDato(),
                    InntektKontrollOppgaveMapper.mapTilRegisterInntekter(registerinntekter, arbeidsgiverOpplysninger),
                    overlapperPeriodeDelvisMedProgramtidslinje(etterlysningPeriode, relevantKontrollTidslinje)
                ),
                etterlysning.getFrist()
            );
        };
    }

    private static boolean overlapperPeriodeDelvisMedProgramtidslinje(LocalDateInterval periode, LocalDateTimeline<RelevanteKontrollperioderUtleder.InfoOmRådata> relevantTidslinje) {
        LocalDateTimeline<RelevanteKontrollperioderUtleder.InfoOmRådata> overlapp = relevantTidslinje.intersection(periode);
        if (overlapp.size() != 1) {
            throw new IllegalStateException("Forventer nøyaktig ett segment med overlapp mot etterlysningsperiode");
        }
        return !overlapp.toSegments().first().getValue().gjelderHelePerioden();
    }


}
