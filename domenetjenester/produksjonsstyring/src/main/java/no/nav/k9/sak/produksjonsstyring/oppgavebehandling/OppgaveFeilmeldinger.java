package no.nav.k9.sak.produksjonsstyring.oppgavebehandling;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface OppgaveFeilmeldinger extends DeklarerteFeil { // NOSONAR
    OppgaveFeilmeldinger FACTORY = FeilFactory.create(OppgaveFeilmeldinger.class);

    @TekniskFeil(feilkode = "K9-395338", feilmelding = "Fant ikke oppgave med årsak=%s, som skulle vært avsluttet på behandlingId=%s.", logLevel = LogLevel.WARN)
    Feil oppgaveMedÅrsakIkkeFunnet(String navn, Long behandlingId);

    @TekniskFeil(feilkode = "K9-395339", feilmelding = "Fant ikke oppgave med id=%s, som skulle vært avsluttet på behandlingId=%s.", logLevel = LogLevel.WARN)
    Feil oppgaveMedIdIkkeFunnet(String oppgaveId, Long behandlingId);

    @TekniskFeil(feilkode = "K9-395340", feilmelding = "Feil ved henting av oppgaver for oppgavetype=%s.", logLevel = LogLevel.WARN)
    Feil feilVedHentingAvOppgaver(String oppgavetype);
}
