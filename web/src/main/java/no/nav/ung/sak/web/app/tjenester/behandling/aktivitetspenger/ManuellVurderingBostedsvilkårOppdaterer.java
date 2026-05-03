package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.ManuellBostedPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.ManuellVurderingBostedsvilkårDto;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Oppdaterer for aksjonspunkt 5144 – manuell vurdering av bostedsvilkåret ved årsak ANNET.
 * Lagrer fritekstvurdering per periode og rekjører steget slik at autoVurder() kan bruke begrunnelsen.
 */
@ApplicationScoped
@DtoTilServiceAdapter(dto = ManuellVurderingBostedsvilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManuellVurderingBostedsvilkårOppdaterer implements AksjonspunktOppdaterer<ManuellVurderingBostedsvilkårDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;

    ManuellVurderingBostedsvilkårOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ManuellVurderingBostedsvilkårOppdaterer(BehandlingRepository behandlingRepository,
                                                    HistorikkinnslagRepository historikkinnslagRepository,
                                                    BostedsGrunnlagRepository bostedsGrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
    }

    @Override
    public OppdateringResultat oppdater(ManuellVurderingBostedsvilkårDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        Map<LocalDate, String> begrunnelserPerFom = dto.getPerioder().stream()
            .collect(Collectors.toMap(ManuellBostedPeriodeDto::getFom, ManuellBostedPeriodeDto::getBegrunnelse));

        bostedsGrunnlagRepository.lagreBegrunnelseVedAnnet(behandlingId, begrunnelserPerFom);

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.BOSTEDSVILKÅR)
            .addLinje("Manuell vurdering av bostedsvilkåret lagret (årsak: Annet)")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        var resultat = OppdateringResultat.nyttResultat();
        resultat.setSteg(BehandlingStegType.VURDER_BOSTED);
        resultat.rekjørSteg();
        return resultat;
    }
}
