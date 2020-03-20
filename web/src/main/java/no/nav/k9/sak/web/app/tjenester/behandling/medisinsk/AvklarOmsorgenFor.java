package no.nav.k9.sak.web.app.tjenester.behandling.medisinsk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.behandlingslager.behandling.medisinsk.OmsorgenFor;
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.AvklarOmsorgenForDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOmsorgenForDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOmsorgenFor implements AksjonspunktOppdaterer<AvklarOmsorgenForDto> {

    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;

    AvklarOmsorgenFor() {
        // for CDI proxy
    }

    @Inject
    AvklarOmsorgenFor(MedisinskGrunnlagRepository medisinskGrunnlagRepository) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarOmsorgenForDto dto, AksjonspunktOppdaterParameter param) {
        var omsorgenFor = new OmsorgenFor(dto.getHarOmsorgenFor());

        // TODO: historikkinnslag

        medisinskGrunnlagRepository.lagre(param.getBehandlingId(), omsorgenFor);

        return OppdateringResultat.utenOveropp();
    }
}
