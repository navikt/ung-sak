package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

public abstract class IverksetteVedtakStegYtelseFelles extends IverksetteVedtakStegFelles {

    private HistorikkRepository historikkRepository;
    private VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse;

    protected IverksetteVedtakStegYtelseFelles() {
        // for CDI proxy
    }

    public IverksetteVedtakStegYtelseFelles(BehandlingRepositoryProvider repositoryProvider,
                                            VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse) {
        super(repositoryProvider);
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.tidligereBehandlingUnderIverksettelse = tidligereBehandlingUnderIverksettelse;
    }

    @Override
    protected Optional<Venteårsak> kanBegynneIverksetting(Behandling behandling) {
        if (tidligereBehandlingUnderIverksettelse.vurder(behandling)) {
            opprettHistorikkinnslagNårIverksettelsePåVent(behandling);
            return Optional.of(Venteårsak.VENT_TIDLIGERE_BEHANDLING);
        }
        return Optional.empty();
    }

    private void opprettHistorikkinnslagNårIverksettelsePåVent(Behandling behandling) {
        HistorikkInnslagTekstBuilder delBuilder = new HistorikkInnslagTekstBuilder();
        delBuilder.medHendelse(HistorikkinnslagType.IVERKSETTELSE_VENT);
        delBuilder.medÅrsak(Venteårsak.VENT_TIDLIGERE_BEHANDLING);

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.IVERKSETTELSE_VENT);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        delBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }
}
