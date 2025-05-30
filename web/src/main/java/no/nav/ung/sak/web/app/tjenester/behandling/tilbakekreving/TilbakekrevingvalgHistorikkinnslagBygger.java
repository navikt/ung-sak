package no.nav.ung.sak.web.app.tjenester.behandling.tilbakekreving;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;

import java.util.ArrayList;
import java.util.Optional;

import static no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

@ApplicationScoped
public class TilbakekrevingvalgHistorikkinnslagBygger {

    private HistorikkinnslagRepository historikkinnslagRepository;

    protected TilbakekrevingvalgHistorikkinnslagBygger() {
        // For CDI proxy
    }

    @Inject
    public TilbakekrevingvalgHistorikkinnslagBygger(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void byggHistorikkinnslag(BehandlingReferanse ref, Optional<TilbakekrevingValg> forrigeValg, TilbakekrevingValg tilbakekrevingValg, String begrunnelse) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        if (tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt() != null) {
            var fraVerdi = forrigeValg.map(TilbakekrevingValg::getErTilbakekrevingVilkårOppfylt).orElse(null);
            linjer.add(fraTilEquals("Er vilkårene for tilbakekreving oppfylt", fraVerdi, tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt()));
        }
        if (tilbakekrevingValg.getGrunnerTilReduksjon() != null) {
            var fraVerdi = forrigeValg.map(TilbakekrevingValg::getGrunnerTilReduksjon).orElse(null);
            linjer.add(fraTilEquals("Er det særlige grunner til reduksjon", fraVerdi, tilbakekrevingValg.getGrunnerTilReduksjon()));
        }
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.getFagsakId())
            .medBehandlingId(ref.getBehandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OM_SIMULERING)
            .medLinjer(linjer)
            .addLinje(fraTilEquals("Fastsett videre behandling", forrigeValg.map(TilbakekrevingValg::getVidereBehandling).orElse(null), tilbakekrevingValg.getVidereBehandling()))
            .addLinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

    }
}
