package no.nav.k9.sak.mottak.dokumentmottak.impl;

import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

public interface Dokumentmottaker {

    void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType);
}
