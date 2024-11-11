package no.nav.ung.sak.web.app.tjenester.forvaltning.rapportering;

import java.util.List;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.web.app.tjenester.forvaltning.DumpOutput;

public interface RapportGenerator {

    List<DumpOutput> generer(FagsakYtelseType ytelseType, DatoIntervallEntitet periode);
}
