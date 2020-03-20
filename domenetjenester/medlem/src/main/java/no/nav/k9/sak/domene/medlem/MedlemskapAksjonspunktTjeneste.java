package no.nav.k9.sak.domene.medlem;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.medlem.impl.AvklarFortsattMedlemskapAksjonspunkt;
import no.nav.k9.sak.domene.medlem.impl.BekreftBosattVurderingAksjonspunkt;
import no.nav.k9.sak.domene.medlem.impl.BekreftErMedlemVurderingAksjonspunkt;
import no.nav.k9.sak.domene.medlem.impl.BekreftErMedlemVurderingAksjonspunktOppdaterer;
import no.nav.k9.sak.domene.medlem.impl.BekreftOppholdsrettVurderingAksjonspunkt;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.medlem.AvklarFortsattMedlemskapAksjonspunktDto;
import no.nav.k9.sak.kontrakt.medlem.BekreftBosattVurderingAksjonspunktDto;
import no.nav.k9.sak.kontrakt.medlem.BekreftOppholdVurderingAksjonspunktDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class MedlemskapAksjonspunktTjeneste {

    private BehandlingRepositoryProvider repositoryProvider;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    MedlemskapAksjonspunktTjeneste() {
        // CDI
    }

    @Inject
    public MedlemskapAksjonspunktTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                          HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public void aksjonspunktBekreftMeldlemVurdering(Long behandlingId, BekreftErMedlemVurderingAksjonspunkt adapter) {
        new BekreftErMedlemVurderingAksjonspunktOppdaterer(repositoryProvider, skjæringstidspunktTjeneste).oppdater(behandlingId, adapter);
    }

    public void aksjonspunktBekreftOppholdVurdering(Long behandlingId, BekreftOppholdVurderingAksjonspunktDto adapter) {
        new BekreftOppholdsrettVurderingAksjonspunkt(repositoryProvider, skjæringstidspunktTjeneste).oppdater(behandlingId, adapter);
    }

    public void aksjonspunktBekreftBosattVurdering(Long behandlingId, BekreftBosattVurderingAksjonspunktDto adapter) {
        new BekreftBosattVurderingAksjonspunkt(repositoryProvider, skjæringstidspunktTjeneste).oppdater(behandlingId, adapter);
    }

    public void aksjonspunktAvklarFortsattMedlemskap(Long behandlingId, AvklarFortsattMedlemskapAksjonspunktDto adapter) {
        new AvklarFortsattMedlemskapAksjonspunkt(repositoryProvider, historikkTjenesteAdapter).oppdater(behandlingId, adapter);
    }
}
