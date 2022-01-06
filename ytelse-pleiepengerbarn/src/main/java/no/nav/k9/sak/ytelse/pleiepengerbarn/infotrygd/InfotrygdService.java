package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygd;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

public interface InfotrygdService {
    Map<AktørId, List<DatoIntervallEntitet>> finnGrunnlagsperioderForAndreAktører(AktørId pleietrengedeAktørId, AktørId ekskludertAktør, LocalDate fom);
}
