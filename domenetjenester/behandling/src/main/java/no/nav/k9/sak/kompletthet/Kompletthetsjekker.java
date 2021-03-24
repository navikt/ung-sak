package no.nav.k9.sak.kompletthet;

import java.util.List;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;

public interface Kompletthetsjekker {

    KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref);

    KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref);

    List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref);

    List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref);

    boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref);

    default KompletthetResultat vurderEtterlysningInntektsmelding(@SuppressWarnings("unused") BehandlingReferanse ref) {
        return KompletthetResultat.oppfylt();
    }

    public static Kompletthetsjekker finnKompletthetsjekkerFor(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(Kompletthetsjekker.class, ytelseType, behandlingType)
            .orElseThrow(() -> new UnsupportedOperationException("Fant ikke " + Kompletthetsjekker.class.getSimpleName() + " for ytelseType=" + ytelseType + ", behandlingType=" + behandlingType));
    }
}
