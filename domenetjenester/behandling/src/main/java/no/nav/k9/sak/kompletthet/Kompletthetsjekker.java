package no.nav.k9.sak.kompletthet;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface Kompletthetsjekker {

    KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref);

    KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref);

    List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref);

    default Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedleggForPerioder(BehandlingReferanse ref) {
        throw new UnsupportedOperationException("Metode ikke implementert for ytelsetype=" + ref.getFagsakYtelseType());
    }

    List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref);

    default boolean ingenSøknadsperioder(BehandlingReferanse ref) {
        return false;
    }

    boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref);

    default KompletthetResultat vurderEtterlysningInntektsmelding(@SuppressWarnings("unused") BehandlingReferanse ref) {
        return KompletthetResultat.oppfylt();
    }



    public static Kompletthetsjekker finnKompletthetsjekkerFor(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(Kompletthetsjekker.class, ytelseType, behandlingType)
            .orElseThrow(() -> new UnsupportedOperationException("Fant ikke " + Kompletthetsjekker.class.getSimpleName() + " for ytelseType=" + ytelseType + ", behandlingType=" + behandlingType));
    }

    static Kompletthetsjekker finnSjekker(Instance<Kompletthetsjekker> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(Kompletthetsjekker.class, instances, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke tjeneste for ytelseType=" + ytelseType));
    }
}
