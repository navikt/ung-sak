package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;

import java.util.List;

@ApplicationScoped
public class EtterlysningOgUttalelseTjeneste {

    private EtterlysningRepository etterlysningRepository;
    private UttalelseRepository uttalelseRepository;

    public EtterlysningOgUttalelseTjeneste() {
    }

    @Inject
    public EtterlysningOgUttalelseTjeneste(EtterlysningRepository etterlysningRepository, UttalelseRepository uttalelseRepository) {
        this.etterlysningRepository = etterlysningRepository;
        this.uttalelseRepository = uttalelseRepository;
    }


    public List<EtterlysningData> hentEtterlysningerOgUttalelser(Long behandlingId, EtterlysningType... typer) {
        // TODO: Hent ut etterlysninger og uttalelser og map til EtterlysningData
        return List.of();
    }


}
