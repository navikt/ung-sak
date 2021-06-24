package no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist;

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartKravDokument;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristRepository;
import no.nav.k9.sak.kontrakt.søknadsfrist.aksjonspunkt.AvklarSøknadsfristDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarSøknadsfristDto.class, adapter = Overstyringshåndterer.class)
public class AvklarSøknadsfristOppdaterer implements AksjonspunktOppdaterer<AvklarSøknadsfristDto> {

    private AvklartSøknadsfristRepository avklartSøknadsfristRepository;

    AvklarSøknadsfristOppdaterer() {
        // CDI
    }

    @Inject
    public AvklarSøknadsfristOppdaterer(AvklartSøknadsfristRepository avklartSøknadsfristRepository) {
        this.avklartSøknadsfristRepository = avklartSøknadsfristRepository;
    }

    private Set<AvklartKravDokument> mapTilOverstyrteKrav(AvklarSøknadsfristDto dto) {
        var avklarteKravDokumenter = dto.getAvklarteKrav()
            .stream()
            .map(it -> new AvklartKravDokument(it.getJournalpostId(), it.getGodkjent(), it.getFraDato(), it.getBegrunnelse()))
            .collect(Collectors.toSet());

        return avklarteKravDokumenter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarSøknadsfristDto dto, AksjonspunktOppdaterParameter param) {
        Set<AvklartKravDokument> avklaringer = mapTilOverstyrteKrav(dto);
        // NB! Merger ikke listene her med det som er lagret tidligere, så GUI må p.d.d. sende med alle avklaringer på saken
        avklartSøknadsfristRepository.lagreAvklaring(param.getBehandlingId(), avklaringer);

        return OppdateringResultat.utenTransisjon()
            .medTotrinn()
            .build();
    }
}
