package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.util.Comparator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@ApplicationScoped
public class UtvidetRettForeslåBehandlingsresultatTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private VilkårResultatRepository vilkårResultatRepository;

    UtvidetRettForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public UtvidetRettForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                         VedtakVarselRepository vedtakVarselRepository,
                                                         VilkårResultatRepository vilkårResultatRepository,
                                                         @Any UtvidetRettRevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder) {
        super(repositoryProvider, vedtakVarselRepository, revurderingBehandlingsresultatutleder);
        this.vilkårResultatRepository = vilkårResultatRepository;

    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        Vilkårene vilkårene = vilkårResultatRepository.hent(behandlingId);
        var vilkår = vilkårene.getVilkår(VilkårType.UTVIDETRETT);
        var perioder = vilkår.orElseThrow().getPerioder();

        var fom = perioder.stream().min(Comparator.comparing(VilkårPeriode::getFom)).map(VilkårPeriode::getFom).orElseThrow();
        var tom = perioder.stream().max(Comparator.comparing(VilkårPeriode::getTom)).map(VilkårPeriode::getTom).orElseThrow();
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

}
