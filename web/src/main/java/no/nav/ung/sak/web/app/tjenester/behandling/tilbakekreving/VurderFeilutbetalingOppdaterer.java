package no.nav.ung.sak.web.app.tjenester.behandling.tilbakekreving;

import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.kontrakt.økonomi.tilbakekreving.VurderFeilutbetalingDto;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFeilutbetalingDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFeilutbetalingOppdaterer implements AksjonspunktOppdaterer<VurderFeilutbetalingDto> {

    static final Set<TilbakekrevingVidereBehandling> LOVLIGE_VALG = Set.of(TilbakekrevingVidereBehandling.IGNORER_TILBAKEKREVING, TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING);
    private TilbakekrevingRepository repository;
    private TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger;

    VurderFeilutbetalingOppdaterer() {
        //CDI proxy
    }

    @Inject
    public VurderFeilutbetalingOppdaterer(TilbakekrevingRepository repository, TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger) {
        this.repository = repository;
        this.historikkInnslagBygger = historikkInnslagBygger;
    }

    private static void valider(VurderFeilutbetalingDto dto) {
        if (!LOVLIGE_VALG.contains(dto.getVidereBehandling())) {
            throw new IllegalArgumentException("Verdien " + dto.getVidereBehandling() + " er ikke blant lovlige verdier: " + LOVLIGE_VALG);
        }
    }

    @Override
    public OppdateringResultat oppdater(VurderFeilutbetalingDto dto, AksjonspunktOppdaterParameter param) {
        valider(dto);
        Behandling behandling = param.getBehandling();
        Long behandlingId = param.getBehandlingId();
        Optional<TilbakekrevingValg> forrigeValg = repository.hent(behandlingId);
        TilbakekrevingValg valg = TilbakekrevingValg.utenMulighetForInntrekk(dto.getVidereBehandling(), dto.getVarseltekst());
        repository.lagre(behandling, valg);

        historikkInnslagBygger.byggHistorikkinnslag(BehandlingReferanse.fra(behandling), forrigeValg, valg, dto.getBegrunnelse());

        return OppdateringResultat.nyttResultat();
    }
}
