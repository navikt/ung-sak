package no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartKravDokument;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristResultat;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.KravDokumentHolder;
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
        Set<AvklartKravDokument> overstyringer = mapTilOverstyrteKrav(behandling.getId(), dto);
        avklartSøknadsfristRepository.lagreOverstyring(kontekst.getBehandlingId(), overstyringer);

        var builder = OppdateringResultat.utenTransisjon()
            .medTotrinn().build();
        builder.skalRekjøreSteg();
        builder.setSteg(BehandlingStegType.INIT_PERIODER);
        return builder;
    }

    private Set<AvklartKravDokument> mapTilOverstyrteKrav(Long behandlingId, OverstyrtSøknadsfristDto dto) {
        var eksisterendeKrav = avklartSøknadsfristRepository.hentHvisEksisterer(behandlingId)
            .flatMap(AvklartSøknadsfristResultat::getOverstyrtHolder)
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
}
