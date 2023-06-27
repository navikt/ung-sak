package no.nav.k9.sak.økonomi.tilkjentytelse;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
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

    private PersonopplysningRepository personopplysningRepository;

    TilkjentYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TilkjentYtelseTjeneste(BehandlingRepository behandlingRepository,
                                  BehandlingVedtakRepository behandlingVedtakRepository,
                                  TilbakekrevingRepository tilbakekrevingRepository,
                                  BeregningsresultatRepository beregningsresultatRepository,
                                  PersonopplysningRepository personopplysningRepository
    ) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.personopplysningRepository = personopplysningRepository;
    }

    public TilkjentYtelseBehandlingInfoV1 hentilkjentYtelseBehandlingInfo(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingVedtak vedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandlingId)
            .orElse(null);

        LocalDate dødsdato = hentBrukersDødsdato(behandling);
        return mapBehandlingsinfo(behandling, vedtak, dødsdato);
    }

    private LocalDate hentBrukersDødsdato(Behandling behandling) {
        PersonopplysningGrunnlagEntitet personopplysningerGrunnlag = personopplysningRepository.hentPersonopplysninger(behandling.getId());
        PersonopplysningEntitet brukerPersonopplysninger = personopplysningerGrunnlag.getGjeldendeVersjon().getPersonopplysning(behandling.getAktørId());
        return brukerPersonopplysninger.getDødsdato();
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

    private TilkjentYtelseBehandlingInfoV1 mapBehandlingsinfo(Behandling behandling, BehandlingVedtak vedtak, LocalDate dødsdatoBruker) {
        TilkjentYtelseBehandlingInfoV1 info = new TilkjentYtelseBehandlingInfoV1();
        info.setSaksnummer(new Saksnummer(behandling.getFagsak().getSaksnummer().getVerdi()));
        info.setBehandlingId(behandling.getUuid());
        info.setYtelseType(MapperForYtelseType.mapYtelseType(behandling.getFagsakYtelseType()));
        info.setAnsvarligSaksbehandler(vedtak == null ? behandling.getAnsvarligSaksbehandler() : vedtak.getAnsvarligSaksbehandler());
        info.setBehandlendeEnhet(behandling.getBehandlendeEnhet());
        info.setAktørId(behandling.getAktørId().getId());
        info.setVedtaksdato(vedtak == null ? LocalDate.now() : vedtak.getVedtaksdato());
        info.setDødsdatoBruker(dødsdatoBruker);
        behandling.getOriginalBehandlingId().ifPresent(ob -> info.setForrigeBehandlingId(behandlingRepository.hentBehandling(ob).getUuid()));
        return info;
    }


}
