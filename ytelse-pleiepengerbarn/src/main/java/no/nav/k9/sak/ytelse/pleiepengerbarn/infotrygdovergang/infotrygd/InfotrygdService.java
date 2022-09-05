package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.IntervallMedBehandlingstema;

public interface InfotrygdService {
    Map<AktørId, List<IntervallMedBehandlingstema>> finnGrunnlagsperioderForPleietrengende(AktørId pleietrengedeAktørId,
                                                                                           Optional<AktørId> ekskludertSøker,
                                                                                           LocalDate fom,
                                                                                           Set<String> relevanteInfotrygdBehandlingstemaer);
}
