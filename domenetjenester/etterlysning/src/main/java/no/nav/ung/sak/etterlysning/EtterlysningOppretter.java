package no.nav.ung.sak.etterlysning;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public interface EtterlysningOppretter {


    void opprettEtterlysning(long behandlingId, DatoIntervallEntitet periode);
}
