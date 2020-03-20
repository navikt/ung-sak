package no.nav.k9.sak.mottak.kompletthettjeneste;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;

public interface KompletthetssjekkerSøknad {

    List<ManglendeVedlegg> utledManglendeVedleggForSøknad(BehandlingReferanse ref);

    Optional<LocalDateTime> erSøknadMottattForTidlig(BehandlingReferanse ref);

    Boolean erSøknadMottatt(BehandlingReferanse ref);
}
