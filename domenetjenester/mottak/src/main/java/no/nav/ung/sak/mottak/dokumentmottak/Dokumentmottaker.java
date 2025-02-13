package no.nav.ung.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.List;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;

public interface Dokumentmottaker {

    void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling);

    List<Trigger> getTriggere(Collection<MottattDokument> mottattDokument);
}
