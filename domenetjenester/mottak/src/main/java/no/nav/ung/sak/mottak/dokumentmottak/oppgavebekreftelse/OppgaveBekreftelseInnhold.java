package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import no.nav.k9.oppgave.OppgaveBekreftelse;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;

public record OppgaveBekreftelseInnhold(
    MottattDokument mottattDokument,
    Behandling behandling,
    OppgaveBekreftelse oppgaveBekreftelse,
    Brevkode brevkode) {
}
