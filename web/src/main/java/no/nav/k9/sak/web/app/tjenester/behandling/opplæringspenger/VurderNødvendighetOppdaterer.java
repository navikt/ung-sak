package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderNødvendighetDto;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjonHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringHolder;
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
        return OppdateringResultat.nyttResultat();
    }

    private VurdertOpplæringGrunnlag mapDtoTilGrunnlag(Long behandlingId, VurderNødvendighetDto dto) {
        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon(dto.getInstitusjon().getInstitusjon(), dto.getInstitusjon().isGodkjent(), dto.getInstitusjon().getBegrunnelse());
        List<VurdertOpplæring> vurdertOpplæring = dto.getPerioder()
            .stream()
            .map(periodeDto -> new VurdertOpplæring(periodeDto.getFom(), periodeDto.getTom(), periodeDto.isNødvendigOpplæring(), periodeDto.getBegrunnelse(), vurdertInstitusjon.getInstitusjon()))
            .toList();
        return new VurdertOpplæringGrunnlag(behandlingId,
            new VurdertInstitusjonHolder(Collections.singletonList(vurdertInstitusjon)),
            new VurdertOpplæringHolder(vurdertOpplæring),
            dto.getBegrunnelse());
    }
}
