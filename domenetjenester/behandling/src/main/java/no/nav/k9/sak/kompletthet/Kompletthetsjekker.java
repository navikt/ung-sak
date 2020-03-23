package no.nav.k9.sak.kompletthet;

import java.util.List;

import no.nav.k9.sak.behandling.BehandlingReferanse;

public interface Kompletthetsjekker {
    KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref);

    KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref);

    KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref);

    List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref);

    List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref);

    boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref);

    default KompletthetResultat vurderEtterlysningInntektsmelding(@SuppressWarnings("unused") BehandlingReferanse ref) {
        return KompletthetResultat.oppfylt();
    }
}
