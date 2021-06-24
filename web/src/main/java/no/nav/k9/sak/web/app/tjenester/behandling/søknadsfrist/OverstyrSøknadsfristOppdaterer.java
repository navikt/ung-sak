package no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist;

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartKravDokument;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristRepository;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.søknadsfrist.aksjonspunkt.OverstyrtSøknadsfristDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrtSøknadsfristDto.class, adapter = Overstyringshåndterer.class)
public class OverstyrSøknadsfristOppdaterer extends AbstractOverstyringshåndterer<OverstyrtSøknadsfristDto> {

    private AvklartSøknadsfristRepository avklartSøknadsfristRepository;

    OverstyrSøknadsfristOppdaterer() {
        // CDI
    }

    @Inject
    public OverstyrSøknadsfristOppdaterer(HistorikkTjenesteAdapter historikkAdapter, AvklartSøknadsfristRepository avklartSøknadsfristRepository) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET);
        this.avklartSøknadsfristRepository = avklartSøknadsfristRepository;
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrtSøknadsfristDto dto) {

    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrtSøknadsfristDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        Set<AvklartKravDokument> overstyringer = mapTilOverstyrteKrav(dto);
        // NB! Merger ikke listene her med det som er lagret tidligere, så GUI må p.d.d. sende med alle overstyringer på saken
        avklartSøknadsfristRepository.lagreOverstyring(kontekst.getBehandlingId(), overstyringer);

        return OppdateringResultat.utenTransisjon()
            .medTotrinn()
            .build();
    }

    private Set<AvklartKravDokument> mapTilOverstyrteKrav(OverstyrtSøknadsfristDto dto) {
        var avklarteKravDokumenter = dto.getAvklarteKrav()
            .stream()
            .map(it -> new AvklartKravDokument(it.getJournalpostId(), it.getGodkjent(), it.getFraDato(), it.getBegrunnelse()))
            .collect(Collectors.toSet());

        return avklarteKravDokumenter;
    }
}
