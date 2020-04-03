package no.nav.k9.sak.økonomi.tilkjentytelse;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;

import no.nav.k9.oppdrag.kontrakt.Saksnummer;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.InntrekkBeslutning;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelse;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseBehandlingInfoV1;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.skjæringstidspunkt.YtelseOpphørtidspunktTjeneste;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingInntrekkEntitet;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;

@ApplicationScoped
public class TilkjentYtelseTjeneste {

    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private Instance<YtelseOpphørtidspunktTjeneste> tilkjentYtelse;

    private BeregningsresultatRepository beregningsresultatRepository;

    TilkjentYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TilkjentYtelseTjeneste(BehandlingRepository behandlingRepository,
                                  BehandlingVedtakRepository behandlingVedtakRepository,
                                  TilbakekrevingRepository tilbakekrevingRepository,
                                  BeregningsresultatRepository beregningsresultatRepository,
                                  @Any Instance<YtelseOpphørtidspunktTjeneste> tilkjentYtelse) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.tilkjentYtelse = tilkjentYtelse;
    }

    public TilkjentYtelseBehandlingInfoV1 hentilkjentYtelseBehandlingInfo(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingVedtak vedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandlingId)
            .orElse(null);

        return mapBehandlingsinfo(behandling, vedtak);
    }

    public TilkjentYtelse hentilkjentYtelse(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);

        List<TilkjentYtelsePeriodeV1> perioder = new MapperForTilkjentYtelse().mapTilkjentYtelse(hentTilkjentYtelsePerioder(behandlingId).orElse(null));

        var tjeneste = FagsakYtelseTypeRef.Lookup.find(tilkjentYtelse, ref.getFagsakYtelseType()).orElseThrow();
        boolean erOpphør = tjeneste.erOpphør(ref);
        Boolean erOpphørEtterSkjæringstidspunktet = tjeneste.erOpphørEtterSkjæringstidspunkt(ref);
        LocalDate endringsdato = hentEndringstidspunkt(behandlingId);
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

        TilkjentYtelseOppdrag tilkjentYtelseOppdrag = new TilkjentYtelseOppdrag(tilkjentYtelse, behandlingInfo, inntrekkBeslutning);
        tilkjentYtelseOppdrag.getBehandlingsinfo().setBehandlingTidspunkt(OffsetDateTime.now(ZoneId.of("UTC")));
        validate(tilkjentYtelseOppdrag);

        return tilkjentYtelseOppdrag;
    }


    private Optional<BeregningsresultatEntitet> hentTilkjentYtelsePerioder(Long behandlingId) {
        Optional<BeregningsresultatEntitet> resultatOpt = hentResultat(behandlingId);
        if (!resultatOpt.isPresent()) {
            return Optional.empty();
        }
        return resultatOpt;
    }

    private LocalDate hentEndringstidspunkt(Long behandlingId) {
        return hentResultat(behandlingId)
            .flatMap(BeregningsresultatEntitet::getEndringsdato)
            .orElse(null);
    }

    private Optional<BeregningsresultatEntitet> hentResultat(Long behandlingId) {
        return beregningsresultatRepository.hentBeregningsresultat(behandlingId);
    }


    private void validate(Object object) {
        var valideringsfeil = validator.validate(object);
        if (!valideringsfeil.isEmpty()) {
            throw new IllegalArgumentException("Kan ikke validate obj=" + object + "\n\tValideringsfeil:" + valideringsfeil);
        }
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
        // FIXME K9 avklar hvilken verdi som skal brukes i 'henvisning'.
        // den brukes til 3 formål:
        // 1 kobling til kvitteringer
        // 2 manuell avsjekk: verdien skal være synlig i GUI for K9, samt vil være synlig i GUI for Oppdragssystemet
        // 3 koble tilbakekrevingsbehandlinger til kravgrunnlag. For dette formålet må p.t. verdien være helt unik
        return behandling.getUuid().toString().substring(0, 30);
    }

}
