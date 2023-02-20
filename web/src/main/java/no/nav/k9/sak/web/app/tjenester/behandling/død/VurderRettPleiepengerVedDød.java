package no.nav.k9.sak.web.app.tjenester.behandling.død;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.død.VurderingRettPleiepengerVedDødDto;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDød;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderingRettPleiepengerVedDødDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderRettPleiepengerVedDød implements AksjonspunktOppdaterer<VurderingRettPleiepengerVedDødDto> {

    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;

    VurderRettPleiepengerVedDød() {
        // CDI
    }

    @Inject
    public VurderRettPleiepengerVedDød(RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository) {
        this.rettPleiepengerVedDødRepository = rettPleiepengerVedDødRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderingRettPleiepengerVedDødDto dto, AksjonspunktOppdaterParameter param) {
        rettPleiepengerVedDødRepository.lagreOgFlush(param.getBehandlingId(), new RettPleiepengerVedDød(dto.getVurdering(), dto.getRettVedDødType()));
        return OppdateringResultat.nyttResultat();
    }

}
