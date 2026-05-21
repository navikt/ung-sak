package no.nav.ung.sak.web.app.tjenester.behandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.behandling.ÅrsakOgPerioderDto;
import no.nav.ung.sak.typer.Periode;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class GyldigePerioderForRevurderingForEndretBosted implements GyldigePerioderForRevurderingPrÅrsakUtleder {

    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;

    public GyldigePerioderForRevurderingForEndretBosted() {
        // CDI
    }

    @Inject
    public GyldigePerioderForRevurderingForEndretBosted(VilkårResultatRepository vilkårResultatRepository, BehandlingRepository behandlingRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public ÅrsakOgPerioderDto utledPerioder(long fagsakId) {
        Optional<Behandling> sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        List<Periode> perioder = sisteBehandling.map(b -> vilkårResultatRepository.hent(b.getId()))
            .stream()
            .map(v -> v.getVilkår(VilkårType.BOSTEDSVILKÅR))
            .flatMap(Optional::stream)
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .map(VilkårPeriode::getPeriode)
            .map(DatoIntervallEntitet::tilPeriode)
            .toList();
        return new ÅrsakOgPerioderDto(BehandlingÅrsakType.ENDRET_BOSTED, perioder);
    }

    @Override
    public Set<BehandlingÅrsakType> støttedeÅrsaker() {
        return Set.of(BehandlingÅrsakType.ENDRET_BOSTED);
    }

    @Override
    public boolean periodeErGyldigForÅrsak(long fagsakId, DatoIntervallEntitet periode) {
        LocalDateInterval inputIntervall = periode.toLocalDateInterval();
        return utledPerioder(fagsakId).perioder().stream()
            .map(p -> new LocalDateInterval(p.getFom(), p.getTom()))
            .anyMatch(gyldigIntervall -> gyldigIntervall.contains(inputIntervall));
    }

}
