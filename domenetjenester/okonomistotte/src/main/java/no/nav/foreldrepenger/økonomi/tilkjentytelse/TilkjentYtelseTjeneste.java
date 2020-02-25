package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingInntrekkEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.oppdrag.kontrakt.Saksnummer;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.InntrekkBeslutning;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelse;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseBehandlingInfoV1;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;

@ApplicationScoped
public class TilkjentYtelseTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private Instance<YtelseTypeTilkjentYtelseTjeneste> tilkjentYtelse;

    TilkjentYtelseTjeneste() {
        //for CDI proxy
    }

    @Inject
    public TilkjentYtelseTjeneste(BehandlingRepository behandlingRepository,
                                  BehandlingVedtakRepository behandlingVedtakRepository,
                                  BehandlingsresultatRepository behandlingsresultatRepository,
                                  TilbakekrevingRepository tilbakekrevingRepository,
                                  @Any Instance<YtelseTypeTilkjentYtelseTjeneste> tilkjentYtelse) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.tilkjentYtelse = tilkjentYtelse;
    }

    public TilkjentYtelseBehandlingInfoV1 hentilkjentYtelseBehandlingInfo(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingVedtak vedtak = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandlingId)
            .orElse(null);

        return mapBehandlingsinfo(behandling, vedtak);
    }

    public TilkjentYtelse hentilkjentYtelse(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);

        YtelseTypeTilkjentYtelseTjeneste tjeneste = FagsakYtelseTypeRef.Lookup.find(tilkjentYtelse, behandling.getFagsakYtelseType()).orElseThrow();

        List<TilkjentYtelsePeriodeV1> perioder = tjeneste.hentTilkjentYtelsePerioder(behandlingId);

        boolean erOpphør = tjeneste.erOpphør(behandlingsresultat);
        Boolean erOpphørEtterSkjæringstidspunktet = tjeneste.erOpphørEtterSkjæringstidspunkt(behandling, behandlingsresultat);
        LocalDate endringsdato = tjeneste.hentEndringstidspunkt(behandlingId);
        return new TilkjentYtelse(endringsdato, perioder)
            .setErOpphørEtterSkjæringstidspunkt(erOpphørEtterSkjæringstidspunktet)
            .setErOpphør(erOpphør)
            .setEndringsdato(endringsdato);
    }

    public TilkjentYtelseOppdrag hentTilkjentYtelseOppdrag(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return hentTilkjentYtelseOppdrag(behandling);
    }

    public TilkjentYtelseOppdrag hentTilkjentYtelseOppdrag(Behandling behandling) {
        Long behandlingId = behandling.getId();
        TilkjentYtelse tilkjentYtelse = hentilkjentYtelse(behandlingId);
        TilkjentYtelseBehandlingInfoV1 behandlingInfo = hentilkjentYtelseBehandlingInfo(behandlingId);
        InntrekkBeslutning inntrekkBeslutning = utledInntrekkBeslutning(behandling);
        return new TilkjentYtelseOppdrag(tilkjentYtelse, behandlingInfo, behandling.getUuid(), inntrekkBeslutning);
    }

    private InntrekkBeslutning utledInntrekkBeslutning(Behandling behandling) {
        Optional<TilbakekrevingInntrekkEntitet> valg = tilbakekrevingRepository.hentTilbakekrevingInntrekk(behandling.getId());
        boolean erInntrekkDeaktivert = valg.isPresent() && valg.get().isAvslåttInntrekk();
        return new InntrekkBeslutning(!erInntrekkDeaktivert);
    }

    private TilkjentYtelseBehandlingInfoV1 mapBehandlingsinfo(Behandling behandling, BehandlingVedtak vedtak) {
        TilkjentYtelseBehandlingInfoV1 info = new TilkjentYtelseBehandlingInfoV1();
        info.setSaksnummer(new Saksnummer(behandling.getFagsak().getSaksnummer().getVerdi()));
        info.setBehandlingId(behandling.getUuid());
        info.setHenvisning(lagHenvisning(behandling));
        info.setYtelseType(MapperForYtelseType.mapYtelseType(behandling.getFagsakYtelseType()));
        info.setAnsvarligSaksbehandler(vedtak == null ? behandling.getAnsvarligSaksbehandler() : vedtak.getAnsvarligSaksbehandler());
        info.setAktørId(behandling.getAktørId().getId());
        info.setVedtaksdato(vedtak == null ? LocalDate.now() : vedtak.getVedtaksdato());
        behandling.getOriginalBehandling().ifPresent(ob -> info.setForrigeBehandlingId(ob.getUuid()));
        return info;
    }

    private String lagHenvisning(Behandling behandling) {
        //FIXME K9 avklar hvilken verdi som skal burkes i 'henvisning'.
        //den brukes til 2 formål:
        // 1 manuell avsjekk: verdien skal være synlig i GUI for K9, samt vil være synlig i GUI for Oppdragssystemet
        // 2 koble tilbakekrevingsbehandlinger til kravgrunnlag. For dette formålet må p.t. verdien være helt unik
        return behandling.getUuid().toString().substring(0, 30);
    }

}
