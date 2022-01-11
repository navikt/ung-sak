package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang;

import no.nav.k9.kodeverk.arbeidsforhold.TemaUnderkategori;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public record IntervallMedBehandlingstema(DatoIntervallEntitet intervall, String behandlingstema) {
}
