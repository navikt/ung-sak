package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.søknad.felles.type.Periode;

public interface Dokumentmottaker {

    void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling);

    default Map<BehandlingÅrsakType, Set<Periode>> hentPerioderMedÅrsak(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        return Collections.emptyMap();
    };


    BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode);
}
