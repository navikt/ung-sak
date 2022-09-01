package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderNødvendighetDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderNødvendighetOppdaterer implements AksjonspunktOppdaterer<VurderNødvendighetDto> {

    private VurdertOpplæringRepository vurdertOpplæringRepository;

    public VurderNødvendighetOppdaterer() {
    }

    @Inject
    public VurderNødvendighetOppdaterer(VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderNødvendighetDto dto, AksjonspunktOppdaterParameter param) {
        VurdertOpplæringGrunnlag grunnlag = mapDtoTilGrunnlag(param.getBehandlingId(), dto);
        vurdertOpplæringRepository.lagreOgFlush(param.getBehandlingId(), grunnlag);
        //TODO skal vi vurdere om oppgitte perioder stemmer med perioder fra søknaden?
        return OppdateringResultat.nyttResultat();
    }

    private VurdertOpplæringGrunnlag mapDtoTilGrunnlag(Long behandlingId, VurderNødvendighetDto dto) {
        List<VurdertOpplæring> vurdertOpplæring = dto.getPerioder()
            .stream()
            .map(periodeDto -> new VurdertOpplæring(null, periodeDto.getFom(), periodeDto.getTom(), periodeDto.isNødvendigOpplæring()))
            .toList();
        return new VurdertOpplæringGrunnlag(behandlingId, dto.isGodkjentInstitusjon(), vurdertOpplæring, dto.getBegrunnelse());
    }
}
