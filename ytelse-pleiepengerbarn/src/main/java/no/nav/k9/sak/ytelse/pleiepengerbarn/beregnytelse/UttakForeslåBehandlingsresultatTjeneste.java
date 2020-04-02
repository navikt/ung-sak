package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class UttakForeslåBehandlingsresultatTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private UttakRepository uttakRepository;

    UttakForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public UttakForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                   VedtakVarselRepository vedtakVarselRepository,
                                                   UttakRepository uttakRepository,
                                                   @FagsakYtelseTypeRef RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutleder) {
        super(repositoryProvider, vedtakVarselRepository, revurderingBehandlingsresultatutleder);
        this.uttakRepository = uttakRepository;
    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        var f = uttakRepository.hentOppgittUttak(behandlingId);
        var maksPeriode = f.getMaksPeriode();
        return maksPeriode;
    }

}
