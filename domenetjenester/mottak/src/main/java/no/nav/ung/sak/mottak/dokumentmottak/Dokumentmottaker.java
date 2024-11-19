package no.nav.ung.sak.mottak.dokumentmottak;

import java.util.Collection;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;

public interface Dokumentmottaker {

    void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling);

    BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode);
}
