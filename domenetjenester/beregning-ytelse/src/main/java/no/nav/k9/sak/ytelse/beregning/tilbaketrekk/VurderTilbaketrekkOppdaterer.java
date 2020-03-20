package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.VurderTilbaketrekkDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderTilbaketrekkDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderTilbaketrekkOppdaterer implements AksjonspunktOppdaterer<VurderTilbaketrekkDto> {

    private BeregningsresultatRepository beregningsresultatRepository;
    private HistorikkTjenesteAdapter historikkAdapter;

    VurderTilbaketrekkOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderTilbaketrekkOppdaterer(BehandlingRepositoryProvider repositoryProvider, HistorikkTjenesteAdapter historikkAdapter) {
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(VurderTilbaketrekkDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        Optional<Boolean> gammelVerdi = beregningsresultatRepository.lagreMedTilbaketrekk(behandling, dto.skalHindreTilbaketrekk());
        Boolean originalSkalHindreTilbaketrekk = gammelVerdi.orElse(null);

        lagHistorikkInnslag(dto, originalSkalHindreTilbaketrekk, param);

        return OppdateringResultat.utenOveropp();
    }

    private void lagHistorikkInnslag(VurderTilbaketrekkDto dto, Boolean originalSkalUtføreTilbaketrekk, AksjonspunktOppdaterParameter param) {

        oppdaterVedEndretVerdi(finnEndretVerdiType(originalSkalUtføreTilbaketrekk), finnEndretVerdiType(dto.skalHindreTilbaketrekk()));

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.TILKJENT_YTELSE);
    }

    private HistorikkEndretFeltVerdiType finnEndretVerdiType(Boolean skalHindreTilbaketrekk) {
        if (skalHindreTilbaketrekk == null) {
            return null;
        } else return skalHindreTilbaketrekk ? HistorikkEndretFeltVerdiType.HINDRE_TILBAKETREKK : HistorikkEndretFeltVerdiType.UTFØR_TILBAKETREKK;
    }

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltVerdiType gammelVerdi, HistorikkEndretFeltVerdiType nyVerdi) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.TILBAKETREKK, gammelVerdi, nyVerdi);
    }
}
