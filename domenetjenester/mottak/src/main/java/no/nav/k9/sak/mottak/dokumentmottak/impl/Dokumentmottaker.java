package no.nav.k9.sak.mottak.dokumentmottak.impl;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.behandling.MottattDokument;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

public interface Dokumentmottaker {

    void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType);
}
