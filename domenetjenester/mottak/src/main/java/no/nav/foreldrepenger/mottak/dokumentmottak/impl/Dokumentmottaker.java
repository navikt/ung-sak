package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

public interface Dokumentmottaker {

    void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, DokumentTypeId dokumentTypeId, BehandlingÅrsakType behandlingÅrsakType);

    @SuppressWarnings("unused")
    default void opprettFraTidligereAvsluttetBehandling(Fagsak fagsak, Long avsluttetMedSøknadBehandlingId, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {}
}
