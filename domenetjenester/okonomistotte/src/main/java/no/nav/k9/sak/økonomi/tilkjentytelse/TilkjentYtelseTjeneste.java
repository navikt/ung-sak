package no.nav.k9.sak.økonomi.tilkjentytelse;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.oppdrag.kontrakt.Saksnummer;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.InntrekkBeslutning;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelse;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseBehandlingInfoV1;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;
import no.nav.k9.oppdrag.kontrakt.util.TilkjentYtelseMaskerer;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingInntrekkEntitet;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;

@Dependent
public class TilkjentYtelseTjeneste {

    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private ObjectMapper objectMapper = JsonMapper.getMapper();
    private TilkjentYtelseMaskerer maskerer = new TilkjentYtelseMaskerer(objectMapper).ikkeMaskerSats();

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    TilkjentYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TilkjentYtelseTjeneste(BehandlingRepository behandlingRepository,
                                  BehandlingVedtakRepository behandlingVedtakRepository,
                                  TilbakekrevingRepository tilbakekrevingRepository, BeregningsresultatRepository beregningsresultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    public TilkjentYtelseBehandlingInfoV1 hentilkjentYtelseBehandlingInfo(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingVedtak vedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandlingId)
            .orElse(null);

        return mapBehandlingsinfo(behandling, vedtak);
    }

    public TilkjentYtelse hentilkjentYtelse(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        MapperForTilkjentYtelse mapper = new MapperForTilkjentYtelse(behandling.getFagsakYtelseType());
        List<TilkjentYtelsePeriodeV1> perioder = mapper.mapTilkjentYtelse(hentTilkjentYtelsePerioder(behandling).orElse(null));
        return new TilkjentYtelse(perioder);
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

    private Optional<BeregningsresultatEntitet> hentTilkjentYtelsePerioder(Behandling behandling) {
        return beregningsresultatRepository.hentEndeligBeregningsresultat(behandling.getId());
    }

    private void validate(TilkjentYtelseOppdrag tilkjentYtelseOppdrag) {
        var valideringsfeil = validator.validate(tilkjentYtelseOppdrag);
        if (!valideringsfeil.isEmpty()) {
            try {
                TilkjentYtelseOppdrag maskert = maskerer.masker(tilkjentYtelseOppdrag);
                throw new IllegalArgumentException("Valideringsfeil:\"" + valideringsfeil + "\" for " + objectMapper.writeValueAsString(maskert));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Det var valideringsfeil, men fikk også Json-feil i håndtering av feilen", e);
            }
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
        info.setYtelseType(MapperForYtelseType.mapYtelseType(behandling.getFagsakYtelseType()));
        info.setAnsvarligSaksbehandler(vedtak == null ? behandling.getAnsvarligSaksbehandler() : vedtak.getAnsvarligSaksbehandler());
        info.setBehandlendeEnhet(behandling.getBehandlendeEnhet());
        info.setAktørId(behandling.getAktørId().getId());
        info.setVedtaksdato(vedtak == null ? LocalDate.now() : vedtak.getVedtaksdato());
        behandling.getOriginalBehandlingId().ifPresent(ob -> info.setForrigeBehandlingId(behandlingRepository.hentBehandling(ob).getUuid()));
        return info;
    }


}
