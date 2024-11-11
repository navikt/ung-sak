package no.nav.ung.sak.domene.behandling.steg.kompletthet;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.kompletthet.KompletthetResultat;
import no.nav.ung.sak.kompletthet.Kompletthetsjekker;
import no.nav.ung.sak.kompletthet.ManglendeVedlegg;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef
public class DummyKompletthetSjekker implements Kompletthetsjekker {

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
        return false;
    }

}
