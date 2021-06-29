package no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartKravDokument;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristResultat;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.KravDokumentHolder;
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

    private Set<AvklartKravDokument> mapTilAvklartKrav(Long behandlingId, AvklarSøknadsfristDto dto) {
        var eksisterendeKrav = avklartSøknadsfristRepository.hentHvisEksisterer(behandlingId)
            .flatMap(AvklartSøknadsfristResultat::getAvklartHolder)
            .map(KravDokumentHolder::getDokumenter)
            .orElse(Set.of());

        var avklarteKravDokumenter = dto.getAvklarteKrav()
            .stream()
            .map(it -> new AvklartKravDokument(it.getJournalpostId(), it.getGodkjent(), it.getFraDato(), it.getBegrunnelse()))
            .collect(Collectors.toCollection(HashSet::new));

        avklarteKravDokumenter.addAll(eksisterendeKrav.stream()
            .filter(it -> avklarteKravDokumenter.stream().noneMatch(at -> it.getJournalpostId().equals(at.getJournalpostId())))
            .collect(Collectors.toSet()));

        return avklarteKravDokumenter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarSøknadsfristDto dto, AksjonspunktOppdaterParameter param) {
        var avklaringer = mapTilAvklartKrav(param.getBehandlingId(), dto);

        avklartSøknadsfristRepository.lagreAvklaring(param.getBehandlingId(), avklaringer);

        var builder = OppdateringResultat.utenTransisjon()
            .medTotrinn().build();
        builder.skalRekjøreSteg();
        builder.setSteg(BehandlingStegType.INIT_PERIODER);
        return builder;
    }
}
