package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class UtenlandsoppholdRestTjeneste {
    static final String BASE_PATH = "/behandling/uttak";

    public static final String UTTAK_UTENLANDSOPPHOLD = BASE_PATH + "/utenlandsopphold";

    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    public UtenlandsoppholdRestTjeneste() {
    }

    @Inject
    public UtenlandsoppholdRestTjeneste(UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository) {
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
    }

    


}
