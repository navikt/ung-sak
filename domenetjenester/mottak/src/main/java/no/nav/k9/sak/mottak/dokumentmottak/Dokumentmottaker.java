package no.nav.k9.sak.mottak.dokumentmottak;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.repo.MottattDokument;

public interface Dokumentmottaker {

    void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType);
}
