package no.nav.k9.sak.web.app.tjenester.fordeling;

import static no.nav.k9.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import no.nav.k9.felles.integrasjon.saf.Journalpost;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.JournalpostResponseProjection;
import no.nav.k9.felles.integrasjon.saf.Journalstatus;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.PollTaskAfterTransaction;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.dokument.arkiv.journal.SafAdapter;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.mottak.FinnEllerOpprettSak;
import no.nav.k9.sak.kontrakt.mottak.FinnEllerOpprettSakFnr;
import no.nav.k9.sak.kontrakt.mottak.FinnSak;
import no.nav.k9.sak.kontrakt.mottak.JournalpostMottakDto;
import no.nav.k9.sak.kontrakt.mottak.JournalpostMottakOpprettSakDto;
import no.nav.k9.sak.kontrakt.søknad.innsending.Innsending;
import no.nav.k9.sak.mottak.SøknadMottakTjenesteContainer;
import no.nav.k9.sak.mottak.dokumentmottak.InngåendeSaksdokument;
import no.nav.k9.sak.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.k9.sak.sikkerhet.abac.AppAbacAttributtType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.jackson.ObjectMapperFactory;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

/**
 * Mottar dokumenter fra k9-fordel og k9-punsj og håndterer dispatch internt for saksbehandlingsløsningen.
 */
@Path(FordelRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class FordelRestTjeneste {

    static final String BASE_PATH = "/fordel";
    private static final String JSON_UTF8 = "application/json; charset=UTF-8";
    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");
    private static final Logger logger = LoggerFactory.getLogger(FordelRestTjeneste.class);

    private SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste;
    private SafAdapter safAdapter;
    private SafTjeneste safTjeneste;
    private FagsakTjeneste fagsakTjeneste;

    private SøknadMottakTjenesteContainer søknadMottakere;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private AktørTjeneste aktørTjeneste;
    private ObjectWriter objectWriter = ObjectMapperFactory.createBaseObjectMapper().writerFor(Innsending.class);

    public FordelRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public FordelRestTjeneste(SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste,
                              SafAdapter safAdapter,
                              SafTjeneste safTjeneste,
                              FagsakTjeneste fagsakTjeneste,
                              MottatteDokumentRepository mottatteDokumentRepository,
                              SøknadMottakTjenesteContainer søknadMottakere,
                              AktørTjeneste aktørTjeneste) {
        this.dokumentmottakTjeneste = dokumentmottakTjeneste;
        this.safAdapter = safAdapter;
        this.safTjeneste = safTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadMottakere = søknadMottakere;
        this.aktørTjeneste = aktørTjeneste;
    }

    @POST
    @Path("/fagsak/opprett")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Finn eller opprett ny sak.", summary = ("Finn eller opprett ny fagsak"), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public SaksnummerDto opprettSak(@Parameter(description = "Oppretter fagsak") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) FinnEllerOpprettSak opprettSakDto) {
        var ytelseType = finnYtelseType(opprettSakDto);

        AktørId aktørId = new AktørId(opprettSakDto.getAktørId());
        AktørId pleietrengendeAktørId = null;
        if (opprettSakDto.getPleietrengendeAktørId() != null) {
            pleietrengendeAktørId = new AktørId(opprettSakDto.getPleietrengendeAktørId());
        }

        AktørId relatertPersonAktørId = null;
        if (opprettSakDto.getRelatertPersonAktørId() != null) {
            relatertPersonAktørId = new AktørId(opprettSakDto.getRelatertPersonAktørId());
        }

        ytelseType.validerNøkkelParametere(pleietrengendeAktørId, relatertPersonAktørId);

        Periode periode = opprettSakDto.getPeriode();
        if (periode == null) {
            throw new IllegalArgumentException("Kan ikke opprette fagsak uten å oppgi start av periode (fravær/uttak): " + opprettSakDto);
        }

        var søknadMottaker = søknadMottakere.finnSøknadMottakerTjeneste(ytelseType);

        var nyFagsak = søknadMottaker.finnEllerOpprettFagsak(ytelseType, aktørId, pleietrengendeAktørId, relatertPersonAktørId, periode.getFom(), periode.getTom());

        return new SaksnummerDto(nyFagsak.getSaksnummer().getVerdi());
    }

    @POST
    @Path("/fagsak/opprett/fnr")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Finn eller opprett ny sak basert på D-/fødselsnummer.", summary = ("Finn eller opprett ny fagsak basert på D-/fødselsnummer."), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    @Deprecated
    public SaksnummerDto opprettSakMedFnr(@Parameter(description = "Oppretter fagsak") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) FinnEllerOpprettSakFnr opprettSakDto) {
        var ytelseType = finnYtelseType(opprettSakDto);

        AktørId aktørId = aktørTjeneste.hentAktørIdForPersonIdent(PersonIdent.fra(opprettSakDto.getSøker())).orElseThrow();
        AktørId pleietrengendeAktørId = null;
        if (opprettSakDto.getPleietrengende() != null) {
            pleietrengendeAktørId = aktørTjeneste.hentAktørIdForPersonIdent(PersonIdent.fra(opprettSakDto.getPleietrengende())).orElseThrow();
        }

        AktørId relatertPersonAktørId = null;
        if (opprettSakDto.getRelatertPerson() != null) {
            relatertPersonAktørId = aktørTjeneste.hentAktørIdForPersonIdent(PersonIdent.fra(opprettSakDto.getRelatertPerson())).orElseThrow();
        }

        ytelseType.validerNøkkelParametere(pleietrengendeAktørId, relatertPersonAktørId);

        Periode periode = opprettSakDto.getPeriode();
        if (periode == null) {
            throw new IllegalArgumentException("Kan ikke opprette fagsak uten å oppgi start av periode (fravær/uttak): " + opprettSakDto);
        }

        Saksnummer saksnummer = null;
        if (opprettSakDto.getSaksnummer() != null) {
            saksnummer = new Saksnummer(opprettSakDto.getSaksnummer());
        }

        var søknadMottaker = søknadMottakere.finnSøknadMottakerTjeneste(ytelseType);

        var nyFagsak = søknadMottaker.finnEllerOpprettFagsak(ytelseType, aktørId, pleietrengendeAktørId, relatertPersonAktørId, periode.getFom(), periode.getTom());

        return new SaksnummerDto(nyFagsak.getSaksnummer().getVerdi());
    }

    @POST
    @Path("/fagsak/sok")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Finn eksisterende sak.", summary = ("Finn eksisterende fagsak"), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public SaksnummerDto finnEksisterendeFagsak(@Parameter(description = "Oppretter fagsak") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) FinnSak finnSakDto) {
        var ytelseType = finnSakDto.getYtelseType();

        AktørId bruker = finnSakDto.getAktørId();
        AktørId pleietrengendeAktørId = finnSakDto.getPleietrengendeAktørId();
        AktørId relatertPersonAktørId = finnSakDto.getRelatertPersonAktørId();
        var periode = finnSakDto.getPeriode();

        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, bruker, pleietrengendeAktørId, relatertPersonAktørId, periode.getFom(), periode.getTom());

        return fagsak.isPresent() ? new SaksnummerDto(fagsak.get().getSaksnummer().getVerdi()) : null;
    }

    @POST
    @Path("/relatertSak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(
        description = "Sjekker om det finnes en eksisterende fagsak med søker, pleietrengende og/eller relatert part.",
        summary = ("Sjekker om det finnes en eksisterende fagsak med søker, pleietrengende og/eller relatert part."),
        tags = "fordel"
    )
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = APPLIKASJON)
    public boolean finnesEksisterendeFagsakMedEnAvAktørene(@Parameter(description = "Søkeparametere") @TilpassetAbacAttributt(supplierClass = FordelRestTjeneste.AbacDataSupplier.class) @Valid FinnSak finnSakDto) {
        return fagsakTjeneste.finnesEnFagsakForMinstEnAvAktørene(
            finnSakDto.getYtelseType(),
            finnSakDto.getAktørId(),
            finnSakDto.getPleietrengendeAktørId(),
            finnSakDto.getRelatertPersonAktørId(),
            finnSakDto.getPeriode().getFom(),
            finnSakDto.getPeriode().getTom()
        );
    }

    private FagsakYtelseType finnYtelseType(FinnEllerOpprettSak dto) {
        return FagsakYtelseType.fraKode(dto.getYtelseType());
    }

    private FagsakYtelseType finnYtelseType(FinnEllerOpprettSakFnr dto) {
        return FagsakYtelseType.fraKode(dto.getYtelseType());
    }

    @POST
    @Path("/journalposter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Ny journalpost skal behandles.", summary = ("Varsel om en nye journalposter som skal behandles i systemet. Alle må tilhøre samme saksnummer, og være av samme type(brevkode, ytelsetype)"), tags = "fordel")
    @PollTaskAfterTransaction
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public void mottaJournalposter(@Parameter(description = "Krever saksnummer, journalpostId og behandlingstemaOffisiellKode") @Valid List<AbacJournalpostMottakDto> mottattJournalposter) {
        Set<Saksnummer> saksnummere = mottattJournalposter.stream().map(m -> m.getSaksnummer()).collect(Collectors.toSet());
        if (saksnummere.size() > 1) {
            throw new UnsupportedOperationException("Støtter ikke mottak av journalposter for ulike saksnummer: " + saksnummere);
        }

        Set<FagsakYtelseType> ytelseTyper = mottattJournalposter.stream().map(m -> m.getYtelseType()).collect(Collectors.toSet());
        if (ytelseTyper.size() > 1) {
            throw new UnsupportedOperationException("Støtter ikke mottak av journalposter av ulike ytelseTyper: " + ytelseTyper);
        }
        LOG_CONTEXT.add("ytelseType", ytelseTyper.iterator().next());
        LOG_CONTEXT.add("journalpostId", String.join(",", mottattJournalposter.stream().map(v -> v.getJournalpostId().getVerdi()).toList()));
        logger.info("Mottok journalposter");

        mottattJournalposter.stream().map(AbacJournalpostMottakDto::getJournalpostId).toList().forEach(this::validerAtJournalpostenErJournalført);

        List<InngåendeSaksdokument> saksdokumenter = mottattJournalposter.stream()
            .map(this::mapJournalpost)
            .sorted(Comparator.comparing(InngåendeSaksdokument::getKanalreferanse, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());

        dokumentmottakTjeneste.dokumenterAnkommet(saksdokumenter);
    }

    @POST
    @Path("/fagsak/opprett/journalpost")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Finn eller opprett sak med gitt saksnummer og motta ny journalpost", summary = ("Finn eller opprett sak med gitt saksnummer og motta ny journalpost"), tags = "fordel")
    @PollTaskAfterTransaction
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public void opprettSakOgMottaJournalpost(@Parameter(description = "Krever saksnummer, journalpostId, aktørId, periode og ytelseType") @Valid AbacJournalpostMottakOpprettSakDto journalpostMottakOpprettSakDto) {
        LOG_CONTEXT.add("ytelseType", journalpostMottakOpprettSakDto.getYtelseType());
        LOG_CONTEXT.add("journalpostId", journalpostMottakOpprettSakDto.getJournalpostId());
        LOG_CONTEXT.add("saksnummer", journalpostMottakOpprettSakDto.getSaksnummer());
        logger.info("Mottok journalpost");

        validerAtJournalpostenErJournalført(journalpostMottakOpprettSakDto.getJournalpostId());

        Fagsak fagsak = finnEllerOpprettSakGittSaksnummer(journalpostMottakOpprettSakDto);

        dokumentmottakTjeneste.dokumenterAnkommet(List.of(mapJournalpost(journalpostMottakOpprettSakDto, fagsak)));
    }

    private InngåendeSaksdokument mapJournalpost(AbacJournalpostMottakDto mottattJournalpost) {
        JournalpostId journalpostId = mottattJournalpost.getJournalpostId();

        Saksnummer saksnummer = mottattJournalpost.getSaksnummer();
        Optional<Fagsak> fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false);
        if (fagsak.isEmpty()) {
            throw new IllegalStateException("Finner ingen fagsak for saksnummer " + saksnummer);
        }
        var f = fagsak.get();

        Optional<String> payload = mottattJournalpost.getPayload();
        InngåendeSaksdokument.Builder builder = InngåendeSaksdokument.builder()
            .medFagsak(f.getId(), f.getYtelseType())
            .medElektroniskSøknad(payload.isPresent())
            .medType(mottattJournalpost.getType())
            .medJournalpostId(mottattJournalpost.getJournalpostId());

        builder.medKanalreferanse(mapTilKanalreferanse(mottattJournalpost.getKanalReferanse(), journalpostId));

        if (payload.isPresent()) {
            builder.medPayload(payload.get()); // NOSONAR
        }

        LocalDateTime mottattTidspunkt = Optional.ofNullable(mottattJournalpost.getForsendelseMottattTidspunkt())
            .orElseThrow(() -> new IllegalArgumentException("Mangler forsendelseMottattTidspunkt"));
        builder.medForsendelseMottatt(mottattTidspunkt); // NOSONAR
        builder.medForsendelseMottatt(mottattJournalpost.getForsendelseMottatt().orElse(mottattTidspunkt.toLocalDate())); // NOSONAR

        return builder.build();
    }

    private InngåendeSaksdokument mapJournalpost(AbacJournalpostMottakOpprettSakDto mottattJournalpost, Fagsak fagsak) {
        JournalpostId journalpostId = mottattJournalpost.getJournalpostId();

        Optional<String> payload = mottattJournalpost.getPayload();
        InngåendeSaksdokument.Builder builder = InngåendeSaksdokument.builder()
            .medFagsak(fagsak.getId(), fagsak.getYtelseType())
            .medElektroniskSøknad(payload.isPresent())
            .medType(mottattJournalpost.getType())
            .medJournalpostId(mottattJournalpost.getJournalpostId());

        builder.medKanalreferanse(mapTilKanalreferanse(mottattJournalpost.getKanalReferanse(), journalpostId));

        payload.ifPresent(builder::medPayload);

        LocalDateTime mottattTidspunkt = Optional.ofNullable(mottattJournalpost.getForsendelseMottattTidspunkt())
            .orElseThrow(() -> new IllegalArgumentException("Mangler forsendelseMottattTidspunkt"));
        builder.medForsendelseMottatt(mottattTidspunkt); // NOSONAR
        builder.medForsendelseMottatt(mottattJournalpost.getForsendelseMottatt().orElse(mottattTidspunkt.toLocalDate())); // NOSONAR

        return builder.build();
    }

    //TODO fjern dupliseringen ved å bruke denne metoden alle steder der fagsak opprettes
    private Fagsak finnEllerOpprettSakGittSaksnummer(AbacJournalpostMottakOpprettSakDto journalpostMottakOpprettSakDto) {
        final Saksnummer saksnummer = journalpostMottakOpprettSakDto.getSaksnummer();
        final FagsakYtelseType ytelseType = journalpostMottakOpprettSakDto.getYtelseType();

        AktørId pleietrengendeAktørId = null;
        if (journalpostMottakOpprettSakDto.getPleietrengendeAktørId() != null) {
            pleietrengendeAktørId = new AktørId(journalpostMottakOpprettSakDto.getPleietrengendeAktørId());
        }

        AktørId relatertPersonAktørId = null;
        if (journalpostMottakOpprettSakDto.getRelatertPersonAktørId() != null) {
            relatertPersonAktørId = new AktørId(journalpostMottakOpprettSakDto.getRelatertPersonAktørId());
        }

        ytelseType.validerNøkkelParametere(pleietrengendeAktørId, relatertPersonAktørId);

        Periode periode = journalpostMottakOpprettSakDto.getPeriode();
        if (periode == null) {
            throw new IllegalArgumentException("Kan ikke opprette fagsak uten å oppgi start av periode (fravær/uttak): " + journalpostMottakOpprettSakDto);
        }

        var søknadMottaker = søknadMottakere.finnSøknadMottakerTjeneste(ytelseType);

        return søknadMottaker.finnEllerOpprettFagsak(
            ytelseType,
            new AktørId(journalpostMottakOpprettSakDto.getAktørId()),
            pleietrengendeAktørId,
            relatertPersonAktørId,
            periode.getFom(),
            periode.getTom()
        );
    }

    private void validerAtJournalpostenErJournalført(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());
        var projection = new JournalpostResponseProjection().journalstatus();
        Journalpost journalpost = safTjeneste.hentJournalpostInfo(query, projection);
        if (!List.of(Journalstatus.FERDIGSTILT, Journalstatus.JOURNALFOERT).contains(journalpost.getJournalstatus())) {
            throw new IllegalArgumentException(journalpostId + " er ikke endelig journalført i Saf");
        }
    }

    /**
     * @deprecated skal bare returnere kanalReferanse når k9-fordel alltid setter kanalReferanse. Da slipper vi oppslag mot Saf her
     */
    @Deprecated(forRemoval = true)
    private String mapTilKanalreferanse(String kanalReferanse, JournalpostId journalpostId) {
        if (kanalReferanse != null) {
            return kanalReferanse;
        } else {
            return utledAltinnReferanseFraInntektsmelding(journalpostId);
        }
    }

    private String writePayload(Innsending innsending) {
        // burde kunne ha streamet direkte fra input, får bli en senere optimalisering...
        try {
            return objectWriter.writeValueAsString(innsending);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Kan ikke serialisere innsendt dokument: " + innsending, e);
        }
    }

    /**
     * @deprecated fjern denne når vi mottar fra fordel
     */
    @Deprecated(forRemoval = true)
    private String utledAltinnReferanseFraInntektsmelding(JournalpostId journalpostId) {
        ArkivJournalPost journalPost = safAdapter.hentInngåendeJournalpostHoveddokument(journalpostId);
        return journalPost != null ? journalPost.getKanalreferanse() : null;
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

    public static class AbacJournalpostMottakDto extends JournalpostMottakDto implements AbacDto {
        public AbacJournalpostMottakDto() {
            super();
        }

        static Optional<String> getPayload(String base64EncodedPayload) {
            if (base64EncodedPayload == null) {
                return Optional.empty();
            }
            byte[] bytes = Base64.getUrlDecoder().decode(base64EncodedPayload);
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        }

        @JsonIgnore
        public Optional<String> getPayload() {
            return getPayload(base64EncodedPayload);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, getSaksnummer());
        }
    }

    public static class AbacJournalpostMottakOpprettSakDto extends JournalpostMottakOpprettSakDto implements AbacDto {
        public AbacJournalpostMottakOpprettSakDto() {
            super();
        }

        static Optional<String> getPayload(String base64EncodedPayload) {
            if (base64EncodedPayload == null) {
                return Optional.empty();
            }
            byte[] bytes = Base64.getUrlDecoder().decode(base64EncodedPayload);
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        }

        @JsonIgnore
        public Optional<String> getPayload() {
            return getPayload(base64EncodedPayload);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.SAKSNUMMER, getSaksnummer())
                .leggTil(AppAbacAttributtType.AKTØR_ID, getAktørId());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                + "<journalpostId=" + getJournalpostId()
                + ", ytelseType=" + getYtelseType()
                + ", periode=" + getPeriode()
                + ">";
        }
    }

}
