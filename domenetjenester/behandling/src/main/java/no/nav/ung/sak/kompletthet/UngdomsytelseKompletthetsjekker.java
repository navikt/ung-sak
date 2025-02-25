package no.nav.ung.sak.kompletthet;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class UngdomsytelseKompletthetsjekker implements Kompletthetsjekker {

    // TODO: Sjekk om vi har mottatt inntekt og om det er søkt for tidlig

    UngdomsytelseKompletthetsjekker() {
        // CDI
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
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        return true;
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        return Collections.emptyList();
    }

    @Override
    public Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedleggForPerioder(BehandlingReferanse ref) {
        return Map.of();
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return List.of();
    }
}
