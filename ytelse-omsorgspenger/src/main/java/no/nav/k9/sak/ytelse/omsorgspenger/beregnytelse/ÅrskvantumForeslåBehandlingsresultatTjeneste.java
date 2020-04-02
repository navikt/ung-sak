package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class ÅrskvantumForeslåBehandlingsresultatTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private OmsorgspengerGrunnlagRepository grunnlagRepository;

    ÅrskvantumForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public ÅrskvantumForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                        VedtakVarselRepository vedtakVarselRepository,
                                                        OmsorgspengerGrunnlagRepository grunnlagRepository,
                                                        @FagsakYtelseTypeRef RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutleder) {
        super(repositoryProvider, vedtakVarselRepository, revurderingBehandlingsresultatutleder);
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        var f = grunnlagRepository.hentOppgittFravær(behandlingId);
        var maksPeriode = f.getMaksPeriode();
        return maksPeriode;
    }

}
