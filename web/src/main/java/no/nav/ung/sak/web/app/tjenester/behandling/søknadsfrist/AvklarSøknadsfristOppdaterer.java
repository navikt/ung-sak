package no.nav.ung.sak.web.app.tjenester.behandling.søknadsfrist;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.søknadsfrist.AvklartKravDokument;
import no.nav.ung.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristRepository;
import no.nav.ung.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristResultat;
import no.nav.ung.sak.behandlingslager.behandling.søknadsfrist.KravDokumentHolder;
import no.nav.ung.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.ung.sak.kontrakt.søknadsfrist.aksjonspunkt.AvklarSøknadsfristDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarSøknadsfristDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarSøknadsfristOppdaterer implements AksjonspunktOppdaterer<AvklarSøknadsfristDto> {

    private AvklartSøknadsfristRepository avklartSøknadsfristRepository;
    private HistorikkTjenesteAdapter historikkAdapter;

    AvklarSøknadsfristOppdaterer() {
        // CDI
    }

    @Inject
    public AvklarSøknadsfristOppdaterer(AvklartSøknadsfristRepository avklartSøknadsfristRepository, HistorikkTjenesteAdapter historikkAdapter) {
        this.avklartSøknadsfristRepository = avklartSøknadsfristRepository;
        this.historikkAdapter = historikkAdapter;
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

        lagHistorikkInnslag(param, avklaringer);

        var builder = OppdateringResultat.builder()
            .medTotrinn().build();
        builder.rekjørSteg();
        builder.setSteg(BehandlingStegType.INIT_PERIODER);
        return builder;
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, Set<AvklartKravDokument> avklaringer) {
        for (AvklartKravDokument avklaring : avklaringer) {
            historikkAdapter.tekstBuilder()
                .medHendelse(HistorikkinnslagType.SØKNADSFRIST_VURDERT, avklaring.getUtfall() + (avklaring.getUtfall() == Utfall.OPPFYLT ? " fra " : " før ") + avklaring.getFraDato() )
                .medBegrunnelse(avklaring.getBegrunnelse(), param.erBegrunnelseEndret())
                .medSkjermlenke(SkjermlenkeType.SOEKNADSFRIST);
        }

    }
}
