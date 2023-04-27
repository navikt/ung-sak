package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.uttak.repo.UttaksgradRepository;
import no.nav.k9.sak.kontrakt.uttak.OverstyrUttaksgradDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrUttaksgradDto.class, adapter = AksjonspunktOppdaterer.class)
public class UttaksgradOppdaterer implements AksjonspunktOppdaterer<OverstyrUttaksgradDto> {

    private UttaksgradRepository uttaksgradRepository;
    UttaksgradOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public UttaksgradOppdaterer(UttaksgradRepository uttaksgradRepository) {
        this.uttaksgradRepository = uttaksgradRepository;
    }

    @Override
    public OppdateringResultat oppdater(OverstyrUttaksgradDto dto, AksjonspunktOppdaterParameter param) {
        lagreOverstyrtUttaksgrad(param.getRef(), dto);
        return OppdateringResultat.nyttResultat();
    }

    private void lagreOverstyrtUttaksgrad(BehandlingReferanse referanse, OverstyrUttaksgradDto dto) {
        // hente uttaksplan med de opprinnelige uttaksgradene
        // matche opprinnelige uttaksgrader med de overstyrte
        // lagre både de opprinnelige og overstyrte
        dto.getUttaksgradPerioder();
    }
}
