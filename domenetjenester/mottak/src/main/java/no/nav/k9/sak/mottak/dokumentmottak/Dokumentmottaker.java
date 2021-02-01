package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.mottak.repo.MottattDokument;

public interface Dokumentmottaker {

    void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling);

    BehandlingÅrsakType getBehandlingÅrsakType();
}
