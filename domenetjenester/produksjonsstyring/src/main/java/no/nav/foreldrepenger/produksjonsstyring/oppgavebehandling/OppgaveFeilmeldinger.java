package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface OppgaveFeilmeldinger extends DeklarerteFeil { // NOSONAR
    OppgaveFeilmeldinger FACTORY = FeilFactory.create(OppgaveFeilmeldinger.class);

    @TekniskFeil(feilkode = "FP-442142", feilmelding = "Fant ingen ident for aktør %s.", logLevel = LogLevel.WARN)
    Feil identIkkeFunnet(AktørId aktoerId);

    @TekniskFeil(feilkode = "FP-395338", feilmelding = "Fant ikke oppgave med årsak=%s, som skulle vært avsluttet på behandlingId=%s.", logLevel = LogLevel.WARN)
    Feil oppgaveMedÅrsakIkkeFunnet(String navn, Object behandlingId);

    @TekniskFeil(feilkode = "FP-395339", feilmelding = "Fant ikke oppgave med id=%s, som skulle vært avsluttet på behandlingId=%s.", logLevel = LogLevel.WARN)
    Feil oppgaveMedIdIkkeFunnet(String oppgaveId, Object behandlingId);

    @TekniskFeil(feilkode = "FP-395340", feilmelding = "Fant ingen underkategori for fagsakYtelseType=%s.", logLevel = LogLevel.WARN)
    Feil underkategoriIkkeFunnetForFagsakYtelseType(FagsakYtelseType fagsakYtelseType);
}
