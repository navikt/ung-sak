package no.nav.ung.sak.web.app.tjenester.behandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.vilkår.VilkårsavklaringÅrsaker;
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

/**
 * Utleder gyldige revurderingsperioder for alle behandlingsårsakene knyttet til vilkårsavklaring,
 * jf. {@link VilkårsavklaringÅrsaker}. Logikken er identisk for alle vilkårene (hent vilkårsperiodene
 * for siste ytelsesbehandling, map til {@link Periode}), så én utleder dekker alle årsakene.
 */
@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class GyldigePerioderForRevurderingForVilkårsavklaring implements GyldigePerioderForRevurderingPrÅrsakUtleder {

    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;

    public GyldigePerioderForRevurderingForVilkårsavklaring() {
        // CDI
    }

    @Inject
    public GyldigePerioderForRevurderingForVilkårsavklaring(VilkårResultatRepository vilkårResultatRepository, BehandlingRepository behandlingRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public List<ÅrsakOgPerioderDto> utledPerioder(long fagsakId) {
        return VilkårsavklaringÅrsaker.alle().entrySet().stream()
            .map(e -> utledPerioderForVilkår(fagsakId, e.getKey(), e.getValue()))
            .toList();
    }

    private ÅrsakOgPerioderDto utledPerioderForVilkår(long fagsakId, VilkårType vilkårType, BehandlingÅrsakType årsak) {
        Optional<Behandling> sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        List<Periode> perioder = sisteBehandling.map(b -> vilkårResultatRepository.hent(b.getId()))
            .stream()
            .map(v -> v.getVilkår(vilkårType))
            .flatMap(Optional::stream)
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .map(VilkårPeriode::getPeriode)
            .map(DatoIntervallEntitet::tilPeriode)
            .toList();
        return new ÅrsakOgPerioderDto(årsak, perioder);
    }

    @Override
    public boolean støtterÅrsak(BehandlingÅrsakType årsak) {
        return VilkårsavklaringÅrsaker.alleÅrsaker().contains(årsak);
    }

    @Override
    public boolean periodeErGyldigForÅrsak(long fagsakId, Optional<DatoIntervallEntitet> periode, BehandlingÅrsakType årsak) {
        // Fordi opphør/avslag kan endres etter uttalelse fra bruker, gir det ikke mening å velge en avgrensende periode i behandlingsmenyen.
        // Hvis det likevel skulle bli valgt, valideres den mot vilkårsperioden.
        if (periode.isEmpty()) {
            return true;
        }
        LocalDateInterval inputIntervall = periode.get().toLocalDateInterval();
        return utledPerioder(fagsakId).stream()
            .filter(dto -> dto.årsak() == årsak)
            .flatMap(dto -> dto.perioder().stream())
            .map(p -> new LocalDateInterval(p.getFom(), p.getTom()))
            .anyMatch(gyldigIntervall -> gyldigIntervall.contains(inputIntervall));
    }

}
