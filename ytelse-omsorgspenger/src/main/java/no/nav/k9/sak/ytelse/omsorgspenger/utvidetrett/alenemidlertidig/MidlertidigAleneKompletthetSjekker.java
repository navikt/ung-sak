package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.alenemidlertidig;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;

/** Kompletthetsjekk for midlertidig alene. forventer ingen vedlegg her foreløpig, så lar ikke ligge på vent. */
@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
@BehandlingTypeRef
@ApplicationScoped
public class MidlertidigAleneKompletthetSjekker implements Kompletthetsjekker {

    @Inject
    public MidlertidigAleneKompletthetSjekker() {
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
        return KompletthetResultat.oppfylt();
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        return List.of();
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return List.of();
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        return true; // forventer ikke vedlegg
    }

}
