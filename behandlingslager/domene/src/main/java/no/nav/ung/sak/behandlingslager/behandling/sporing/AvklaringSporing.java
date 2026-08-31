package no.nav.ung.sak.behandlingslager.behandling.sporing;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Pakker inn {@link BehandingprosessSporingRepository#lagreSporing} med JSON-serialisering og feilhåndtering,
 * slik at et steg kan lagre sporing av vilkårsavklaring for feilsøking uten å la en serialiseringsfeil stoppe
 * behandlingen. Løftet ut av {@code VurderBostedVilkårSteg.lagreBehandlingprosessSporing}.
 */
@Dependent
public class AvklaringSporing {

    private static final Logger LOG = LoggerFactory.getLogger(AvklaringSporing.class);

    private final BehandingprosessSporingRepository behandingprosessSporingRepository;

    @Inject
    public AvklaringSporing(BehandingprosessSporingRepository behandingprosessSporingRepository) {
        this.behandingprosessSporingRepository = behandingprosessSporingRepository;
    }

    public void lagreSporing(long behandlingId, Object input, Object utfall, String stegKode) {
        try {
            behandingprosessSporingRepository.lagreSporing(new BehandlingprosessSporing(
                behandlingId,
                JsonObjectMapper.getJson(input),
                JsonObjectMapper.getJson(utfall),
                stegKode)
            );
        } catch (IOException e) {
            LOG.warn("Feil ved lagring av sporing for utledning av avklaring for steg {}", stegKode, e);
        }
    }
}
