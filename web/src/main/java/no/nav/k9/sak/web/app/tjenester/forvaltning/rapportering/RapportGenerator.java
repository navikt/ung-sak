package no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering;

import java.util.List;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;

public interface RapportGenerator {

    List<DumpOutput> generer(FagsakYtelseType ytelseType, DatoIntervallEntitet periode);
}
