package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerForeslåBehandlingsresultatTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private static final Logger log = LoggerFactory.getLogger(OmsorgspengerForeslåBehandlingsresultatTjeneste.class);

    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private ÅrskvantumTjeneste årskvantumTjeneste;

    OmsorgspengerForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public OmsorgspengerForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                           VedtakVarselRepository vedtakVarselRepository,
                                                           OmsorgspengerGrunnlagRepository grunnlagRepository,
                                                           ÅrskvantumTjeneste årskvantumTjeneste,
                                                           @FagsakYtelseTypeRef RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder) {
        super(repositoryProvider, vedtakVarselRepository, revurderingBehandlingsresultatutleder);
        this.grunnlagRepository = grunnlagRepository;
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        var f = grunnlagRepository.hentOppgittFravær(behandlingId);
        var maksPeriode = f.getMaksPeriode();
        return maksPeriode;
    }

    @Override
    protected boolean skalAvslåsBasertPåAndreForhold(BehandlingReferanse ref) {
        var årskvantumForbrukteDager = årskvantumTjeneste.hentÅrskvantumForBehandling(ref.getBehandlingUuid());
        var sisteUttaksplan = årskvantumForbrukteDager.getSisteUttaksplan();
        if (sisteUttaksplan == null) {
            log.warn("Avslår behandling. Har ingen uttaksplan for behandlingUuid={}", ref.getBehandlingUuid());
            return true;
        }

        List<Uttaksperiode> uttaksperioder = sisteUttaksplan.getAktiviteter()
                .stream()
                .map(Aktivitet::getUttaksperioder)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        
        boolean avslå = uttaksperioder.stream().allMatch(it -> Utfall.AVSLÅTT.equals(it.getUtfall()));
        
        if(avslå) {
            log.warn("Avslår behandling {} pga alle uttaksperioder avslått: {}", ref.getBehandlingUuid(), uttaksperioder);
        }
        return avslå;
    }
}
