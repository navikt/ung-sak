package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

import java.util.Collections;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderInstitusjonDto;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjonHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderInstitusjonDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderInstitusjonOppdaterer implements AksjonspunktOppdaterer<VurderInstitusjonDto> {

    private VurdertOpplæringRepository vurdertOpplæringRepository;

    public VurderInstitusjonOppdaterer() {
    }

    @Inject
    public VurderInstitusjonOppdaterer(VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderInstitusjonDto dto, AksjonspunktOppdaterParameter param) {
        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon(dto.getInstitusjon(), dto.isGodkjent(), dto.getBegrunnelse());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(param.getBehandlingId(),
            new VurdertInstitusjonHolder(Collections.singletonList(vurdertInstitusjon)),
            new VurdertOpplæringHolder(),
            dto.getBegrunnelse());
        vurdertOpplæringRepository.lagreOgFlush(param.getBehandlingId(), grunnlag);
        return OppdateringResultat.nyttResultat();
    }
}
