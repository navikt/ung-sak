package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.sak.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.IntervallMedBehandlingstema;

@Dependent
public class InfotrygdServiceImpl implements InfotrygdService {

    private InfotrygdPårørendeSykdomService pårørendeSykdomService;
    private PersonIdentTjeneste personIdentTjeneste;

    @Inject
    public InfotrygdServiceImpl(InfotrygdPårørendeSykdomService pårørendeSykdomService, PersonIdentTjeneste personIdentTjeneste) {
        this.pårørendeSykdomService = pårørendeSykdomService;
        this.personIdentTjeneste = personIdentTjeneste;
    }


    @Override
    public Map<AktørId, List<IntervallMedBehandlingstema>> finnGrunnlagsperioderForPleietrengende(AktørId pleietrengedeAktørId,
                                                                                                  Optional<AktørId> ekskludertSøker,
                                                                                                  LocalDate fom,
                                                                                                  Set<String> relevanteInfotrygdBehandlingstemaer) {

        var personIdent = personIdentTjeneste.hentFnrForAktør(pleietrengedeAktørId);
        var ekskludertSøkerIdent = ekskludertSøker.map(personIdentTjeneste::hentFnrForAktør);

        var perioderPrIdent = pårørendeSykdomService.hentRelevanteGrunnlagsperioderPrSøkerident(InfotrygdPårørendeSykdomRequest.builder()
                .fødselsnummer(personIdent.getIdent())
                .fraOgMed(fom)
                .relevanteBehandlingstemaer(relevanteInfotrygdBehandlingstemaer)
                .build(),
            ekskludertSøkerIdent);

        return perioderPrIdent.entrySet().stream()
            .filter(e -> personIdentTjeneste.hentAktørForFnr(new PersonIdent(e.getKey())).isPresent())
            .collect(Collectors.groupingBy(e -> personIdentTjeneste.hentAktørForFnr(new PersonIdent(e.getKey())).get(),
                Collectors.flatMapping(intervallMedBehandlingstemaStream(), Collectors.toList())));
    }

    private Function<Map.Entry<String, List<PeriodeMedBehandlingstema>>, Stream<? extends IntervallMedBehandlingstema>> intervallMedBehandlingstemaStream() {
        return e -> e.getValue().stream().map(this::mapTilIntervallMedBehandlingstema);
    }

    private IntervallMedBehandlingstema mapTilIntervallMedBehandlingstema(PeriodeMedBehandlingstema p) {
        return new IntervallMedBehandlingstema(
            DatoIntervallEntitet.fraOgMedTilOgMed(p.periode().getFom(), p.periode().getTom()),
            p.behandlingstema());
    }

}
