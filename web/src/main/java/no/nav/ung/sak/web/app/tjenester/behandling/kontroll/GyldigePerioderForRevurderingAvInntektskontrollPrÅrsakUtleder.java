package no.nav.ung.sak.web.app.tjenester.behandling.kontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.behandling.ÅrsakOgPerioderDto;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.app.tjenester.behandling.GyldigePerioderForRevurderingPrÅrsakUtleder;

import java.util.List;
import java.util.Set;

@ApplicationScoped
@FagsakYtelseTypeRef
public class GyldigePerioderForRevurderingAvInntektskontrollPrÅrsakUtleder implements GyldigePerioderForRevurderingPrÅrsakUtleder {

    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private BehandlingRepository behandlingRepository;

    public GyldigePerioderForRevurderingAvInntektskontrollPrÅrsakUtleder() {
        // CDI
    }

    @Inject
    public GyldigePerioderForRevurderingAvInntektskontrollPrÅrsakUtleder(TilkjentYtelseRepository tilkjentYtelseRepository, BehandlingRepository behandlingRepository) {
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public ÅrsakOgPerioderDto utledPerioder(long fagsakId) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        return new ÅrsakOgPerioderDto(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, behandling.map(value -> tilkjentYtelseRepository.hentKontrollertInntektPerioder(value.getId())
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .map(KontrollertInntektPeriode::getPeriode)
            .map(p -> new Periode(p.getFomDato(), p.getTomDato()))
            .sorted()
            .toList()).orElse(List.of()));
    }

    @Override
    public Set<BehandlingÅrsakType> støttedeÅrsaker() {
        return Set.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT);
    }

    @Override
    public boolean periodeErGyldigForÅrsak(long fagsakId, DatoIntervallEntitet periode) {
        var utledtePerioder = utledPerioder(fagsakId);
        return utledtePerioder.perioder().stream()
            .map(DatoIntervallEntitet::fra)
            .anyMatch(periode::equals);
    }
}
