package no.nav.ung.sak.produksjonsstyring.oppgavebehandling;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface OppgaveFeilmeldinger extends DeklarerteFeil { // NOSONAR
    OppgaveFeilmeldinger FACTORY = FeilFactory.create(OppgaveFeilmeldinger.class);

    @TekniskFeil(feilkode = "K9-395338", feilmelding = "Fant ikke oppgave med årsak=%s, som skulle vært avsluttet på behandlingId=%s.", logLevel = LogLevel.WARN)
    Feil oppgaveMedÅrsakIkkeFunnet(String navn, Long behandlingId);

    @TekniskFeil(feilkode = "K9-395339", feilmelding = "Fant ikke oppgave med id=%s, som skulle vært avsluttet på behandlingId=%s.", logLevel = LogLevel.WARN)
    Feil oppgaveMedIdIkkeFunnet(String oppgaveId, Long behandlingId);

    @TekniskFeil(feilkode = "K9-395340", feilmelding = "Feil ved henting av oppgaver for ytelseType=%s, oppgavetype=%s", logLevel = LogLevel.WARN)
    Feil feilVedHentingAvOppgaver(FagsakYtelseType ytelseType, OppgaveÅrsak oppgavetype, Exception e);
}
