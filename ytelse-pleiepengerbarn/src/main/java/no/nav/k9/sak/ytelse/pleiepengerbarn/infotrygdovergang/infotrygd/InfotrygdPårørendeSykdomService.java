package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.k9.sak.typer.PersonIdent;

public interface InfotrygdPårørendeSykdomService {
    Map<String, List<PeriodeMedBehandlingstema>> hentRelevanteGrunnlagsperioderPrSøkerident(InfotrygdPårørendeSykdomRequest request, Optional<PersonIdent> ekskludertSøkerIdent);
}
