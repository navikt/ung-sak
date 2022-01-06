package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import java.util.List;
import java.util.Map;

import no.nav.k9.sak.typer.PersonIdent;

public interface InfotrygdPårørendeSykdomService {
    Map<String, List<PeriodeMedBehandlingstema>> hentRelevanteGrunnlagsperioderPrSøkeridentForAndreSøkere(InfotrygdPårørendeSykdomRequest request, PersonIdent ekskludertPersonIdent);
}
