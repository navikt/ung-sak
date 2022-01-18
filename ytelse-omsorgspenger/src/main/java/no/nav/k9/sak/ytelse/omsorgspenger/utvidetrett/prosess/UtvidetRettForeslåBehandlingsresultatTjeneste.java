package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@FagsakYtelseTypeRef("OMP_AO")
@ApplicationScoped
public class UtvidetRettForeslåBehandlingsresultatTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private SøknadRepository søknadRepositoy;

    UtvidetRettForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public UtvidetRettForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                         VedtakVarselRepository vedtakVarselRepository,
                                                         SøknadRepository søknadRepositoy,
                                                         @FagsakYtelseTypeRef RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder) {
        super(repositoryProvider, vedtakVarselRepository, revurderingBehandlingsresultatutleder);
        this.søknadRepositoy = søknadRepositoy;

    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        var søknad = søknadRepositoy.hentSøknadHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler søknad"));

        var perioder = søknad.getSøknadsperiode();

        return DatoIntervallEntitet.fraOgMedTilOgMed(perioder.getFomDato(), perioder.getTomDato());
    }

}
