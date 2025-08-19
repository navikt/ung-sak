package no.nav.ung.sak.økonomi.tilkjentytelse;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.k9.oppdrag.kontrakt.kodeverk.YtelseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseTilkjentYtelseUtleder;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingInntrekkEntitet;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;

@Dependent
public class TilkjentYtelseTjeneste {

    private final Logger log = LoggerFactory.getLogger(TilkjentYtelseTjeneste.class);
    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private ObjectMapper objectMapper = JsonObjectMapper.getMapper();
    private TilkjentYtelseMaskerer maskerer = new TilkjentYtelseMaskerer(objectMapper).ikkeMaskerSats();

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private TilkjentYtelseUtleder tilkjentYtelseUtleder;

    private PersonopplysningRepository personopplysningRepository;

    TilkjentYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TilkjentYtelseTjeneste(BehandlingRepository behandlingRepository,
                                  BehandlingVedtakRepository behandlingVedtakRepository,
                                  TilbakekrevingRepository tilbakekrevingRepository,
                                  UngdomsytelseTilkjentYtelseUtleder utledTilkjentYtelse,
                                  PersonopplysningRepository personopplysningRepository
    ) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.tilkjentYtelseUtleder = utledTilkjentYtelse;
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
        List<TilkjentYtelsePeriodeV1> perioder = MapperForTilkjentYtelse.mapTilkjentYtelse(tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(behandlingId));
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

    private void validate(TilkjentYtelseOppdrag tilkjentYtelseOppdrag) {
        var valideringsfeil = validator.validate(tilkjentYtelseOppdrag);
        if (!valideringsfeil.isEmpty()) {
            try {
                TilkjentYtelseOppdrag maskert = maskerer.masker(tilkjentYtelseOppdrag);
                String input = objectMapper.writeValueAsString(maskert);
                //avkorter input for å unngå at logginnslag brytes. Ser ut som loggen p.t. tåler ca 9000 tegn herfra, setter lavere for sikkerhets skyld
                String avkortetInput = begrensTilAntall(input, 6000);
                log.warn("Valideringsfeil:\"" + valideringsfeil + "\" for " + avkortetInput);
                throw new IllegalArgumentException("Valideringsfeil:" + valideringsfeil);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Det var valideringsfeil, men fikk også Json-feil i håndtering av feilen", e);
            }
        }
    }

    private static String begrensTilAntall(String uavkortet, int antallTegn) {
        if (uavkortet == null || uavkortet.length() <= antallTegn) {
            return uavkortet;
        }
        int antallFjernedeTegn = uavkortet.length() - antallTegn;
        return uavkortet.substring(0, antallTegn - 1) + "...(fjernet " + antallFjernedeTegn + " tegn)";
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
        info.setYtelseType(YtelseType.fraKode(behandling.getFagsakYtelseType().getKode()));
        info.setAnsvarligSaksbehandler(vedtak == null ? behandling.getAnsvarligSaksbehandler() : vedtak.getAnsvarligSaksbehandler());
        info.setBehandlendeEnhet(behandling.getBehandlendeEnhet());
        info.setAktørId(behandling.getAktørId().getId());
        info.setVedtaksdato(vedtak == null ? LocalDate.now() : vedtak.getVedtaksdato());
        info.setDødsdatoBruker(dødsdatoBruker);
        behandling.getOriginalBehandlingId().ifPresent(ob -> info.setForrigeBehandlingId(behandlingRepository.hentBehandling(ob).getUuid()));
        return info;
    }


}
