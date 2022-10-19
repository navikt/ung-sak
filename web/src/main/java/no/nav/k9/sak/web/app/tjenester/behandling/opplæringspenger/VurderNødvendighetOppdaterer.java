package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderNødvendighetDto;
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
        List<VurdertOpplæring> vurdertOpplæring = dto.getPerioder()
            .stream()
            .map(periodeDto -> new VurdertOpplæring(periodeDto.getFom(), periodeDto.getTom(), periodeDto.isNødvendigOpplæring(), periodeDto.getBegrunnelse(), dto.getInstitusjon()))
            .toList();
        sjekkOverlappendePerioder(vurdertOpplæring);

        return new VurdertOpplæringGrunnlag(behandlingId,
            new VurdertInstitusjonHolder(),
            new VurdertOpplæringHolder(vurdertOpplæring),
            dto.getBegrunnelse());
    }

    private void sjekkOverlappendePerioder(List<VurdertOpplæring> vurdertOpplæring) {
        List<DatoIntervallEntitet> perioder = vurdertOpplæring.stream().map(VurdertOpplæring::getPeriode).toList();
        for (DatoIntervallEntitet periode : perioder) {
            for (DatoIntervallEntitet periode2 : perioder) {
                if (periode != periode2 && periode.overlapper(periode2)) {
                    throw new IllegalArgumentException("Overlapp mellom " + periode + " og " + periode2 + " i vurdert opplæring.");
                }
            }
        }
    }
}
