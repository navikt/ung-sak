package no.nav.ung.sak.web.app.tjenester.behandling.kontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.app.tjenester.behandling.GyldigePerioderForRevurderingPrÅrsakUtleder;
import no.nav.ung.sak.web.app.tjenester.behandling.ÅrsakOgPerioder;

import java.util.List;

@ApplicationScoped
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
    public ÅrsakOgPerioder utledPerioder(long fagsakId) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        return new ÅrsakOgPerioder(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, behandling.map(value -> tilkjentYtelseRepository.hentKontrollertInntektPerioder(value.getId())
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .map(KontrollertInntektPeriode::getPeriode)
            .map(p -> new Periode(p.getFomDato(), p.getTomDato()))
            .toList()).orElse(List.of()));
    }
}
