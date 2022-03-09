package no.nav.k9.sak.web.app.tjenester.fordeling;

import static no.nav.k9.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.feil.LogLevel.WARN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.dokument.arkiv.journal.SafAdapter;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.mottak.AktørListeDto;
import no.nav.k9.sak.kontrakt.mottak.FinnEllerOpprettSak;
import no.nav.k9.sak.kontrakt.mottak.FinnEllerOpprettSakFnr;
import no.nav.k9.sak.kontrakt.mottak.FinnSak;
import no.nav.k9.sak.kontrakt.mottak.JournalpostMottakDto;
import no.nav.k9.sak.kontrakt.søknad.innsending.Innsending;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingMottatt;
import no.nav.k9.sak.mottak.SøknadMottakTjenesteContainer;
import no.nav.k9.sak.mottak.dokumentmottak.InngåendeSaksdokument;
import no.nav.k9.sak.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.k9.sak.sikkerhet.abac.AppAbacAttributtType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.jackson.JacksonJsonConfig;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.infotrygd.PsbInfotrygdRepository;

/**
 * Mottar dokumenter fra f.eks. FPFORDEL og håndterer dispatch internt for saksbehandlingsløsningen.
 */
@Path(FordelRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class FordelRestTjeneste {

    static final String BASE_PATH = "/fordel";
    private static final String JSON_UTF8 = "application/json; charset=UTF-8";

    private SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste;
    private SafAdapter safAdapter;
    private FagsakTjeneste fagsakTjeneste;

    private SøknadMottakTjenesteContainer søknadMottakere;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private PsbInfotrygdRepository psbInfotrygdRepository;
    private AktørTjeneste aktørTjeneste;
    private ObjectWriter objectWriter = new JacksonJsonConfig().getObjectMapper().writerFor(Innsending.class);

    public FordelRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public FordelRestTjeneste(SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste,
                              SafAdapter safAdapter,
                              FagsakTjeneste fagsakTjeneste,
                              MottatteDokumentRepository mottatteDokumentRepository,
                              SøknadMottakTjenesteContainer søknadMottakere,
                              PsbInfotrygdRepository psbInfotrygdRepository,
                              AktørTjeneste aktørTjeneste) {
        this.dokumentmottakTjeneste = dokumentmottakTjeneste;
        this.safAdapter = safAdapter;
        this.fagsakTjeneste = fagsakTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadMottakere = søknadMottakere;
        this.psbInfotrygdRepository= psbInfotrygdRepository;
        this.aktørTjeneste = aktørTjeneste;
    }


    @POST
    @Path("/psb-infotrygd/fnr")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Legger til fødselsnumre som skal rutes til Infotrygd for PSB.", summary = ("Legger til fødselsnumre som skal rutes til Infotrygd for PSB."), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public Response leggTilPsbInfotrygdPerson(@Parameter(description = "Fødselsnumre (skilt med mellomrom eller linjeskift") @Valid PsbInfotrygdFødselsnumre fødselsnumre) {
        final StringBuilder sb = new StringBuilder();
        final var fødselsnummerliste = Arrays.asList(Objects.requireNonNull(fødselsnumre.getFødselsnumre(), "saksnumre").split("\\s+"));
        for (var fnr : fødselsnummerliste) {
            try {
                final AktørId aktørId = aktørTjeneste.hentAktørIdForPersonIdent(PersonIdent.fra(fnr)).get();
                psbInfotrygdRepository.lagre(aktørId);
            } catch (RuntimeException e) {
                sb.append("Feil for \"" + fnr + "\": " + e.toString() + "\n");
            }
        }

        if (sb.length() > 0) {
            return Response.status(400, sb.toString()).build();
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/psb-infotrygd/aktoer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Legger til aktør-IDer som skal rutes til Infotrygd for PSB.", summary = ("Legger til fødselsnumre som skal rutes til Infotrygd for PSB."), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public void leggTilPsbInfotrygdAktør(@Parameter(description = "Liste med aktør-IDer") @TilpassetAbacAttributt(supplierClass = FordelRestTjeneste.AbacDataSupplier.class) @Valid AktørListeDto aktører) {
        for (var aktørId : aktører.getAktører()) {
            psbInfotrygdRepository.lagre(aktørId);
        }
    }

    @POST
    @Path("/psb-infotrygd/finnes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Sjekker om PSB-fordeling skal til Infotrygd for minst én av personene.", summary = ("Sjekker om PSB-fordeling skal til Infotrygd for minst én av personene."), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    public boolean sjekkPsbInfotrygdPerson(@Parameter(description = "Sjekker om PSB-fordeling skal til Infotrygd for minst én av personen)") @TilpassetAbacAttributt(supplierClass = FordelRestTjeneste.AbacDataSupplier.class) @Valid AktørListeDto aktører) {
        return aktører.getAktører().stream().map(a -> psbInfotrygdRepository.finnes(a)).anyMatch(v -> v);
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

    @SuppressWarnings({ "unchecked" })
    @POST
    @Path("/innsending")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Mottak av dokument.", tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public InnsendingMottatt innsending(@Parameter(description = "Søknad/dokument innsendt.") @TilpassetAbacAttributt(supplierClass = FordelRestTjeneste.AbacDataSupplier.class) @Valid Innsending innsending) {
        var saksnummer = innsending.getSaksnummer();
        var ytelseType = innsending.getYtelseType();
        var journalpostId = innsending.getJournalpostId();

        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true).orElseThrow(() -> new IllegalArgumentException("Finner ikke fagsak for saksnummer=" + saksnummer));
        var mottattDokument = lagreMottattDokument(innsending, journalpostId, fagsak);

        var søknadMottaker = søknadMottakere.finnSøknadMottakerTjeneste(ytelseType);
        var behandling = søknadMottaker.mottaSøknad(saksnummer, journalpostId, innsending.getInnhold());

        mottattDokument.setBehandlingId(behandling.getId());
        mottatteDokumentRepository.lagre(mottattDokument, DokumentStatus.GYLDIG);

        return new InnsendingMottatt(saksnummer);
    }

    private MottattDokument lagreMottattDokument(Innsending innsending, JournalpostId journalpostId, Fagsak fagsak) {
        String payload = writePayload(innsending);
        var builder = new MottattDokument.Builder()
            .medFagsakId(fagsak.getId())
            .medJournalPostId(journalpostId)
            .medType(innsending.getType())
            .medArbeidsgiver(null) // sender ikke inn fra arbeidsgiver på dette endepunktet ennå
            .medPayload(payload)
            .medKanalreferanse(innsending.getKanalReferanse());

        LocalDateTime mottattTidspunkt = Objects.requireNonNull(innsending.getForsendelseMottattTidspunkt(), "forsendelseMottattTidspunkt").toLocalDateTime();
        builder.medMottattTidspunkt(mottattTidspunkt);
        builder.medMottattDato(mottattTidspunkt.toLocalDate());

        MottattDokument mottattDokument = builder.build();
        mottatteDokumentRepository.lagre(mottattDokument, DokumentStatus.MOTTATT);
        return mottattDokument;
    }

    @POST
    @Path("/journalposter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Ny journalpost skal behandles.", summary = ("Varsel om en nye journalposter som skal behandles i systemet. Alle må tilhøre samme saksnummer, og være av samme type(brevkode, ytelsetype)"), tags = "fordel")
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

        List<InngåendeSaksdokument> saksdokumenter = mottattJournalposter.stream()
            .map(this::mapJournalpost)
            .sorted(Comparator.comparing(InngåendeSaksdokument::getKanalreferanse, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());

        dokumentmottakTjeneste.dokumenterAnkommet(saksdokumenter);
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

        static Optional<String> getPayloadValiderLengde(String base64EncodedPayload) {
            if (base64EncodedPayload == null) {
                return Optional.empty();
            }
            byte[] bytes = Base64.getUrlDecoder().decode(base64EncodedPayload);
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        }

        @JsonIgnore
        public Optional<String> getPayload() {
            return getPayloadValiderLengde(base64EncodedPayload);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, getSaksnummer());
        }

        interface JournalpostMottakFeil extends DeklarerteFeil {

            JournalpostMottakFeil FACTORY = FeilFactory.create(JournalpostMottakFeil.class);

            @TekniskFeil(feilkode = "F-217605", feilmelding = "Input-validering-feil: Avsender sendte payload, men oppgav ikke lengde på innhold", logLevel = WARN)
            Feil manglerPayloadLength();

            @TekniskFeil(feilkode = "F-483098", feilmelding = "Input-validering-feil: Avsender oppgav at lengde på innhold var %s, men lengden var egentlig %s", logLevel = WARN)
            Feil feilPayloadLength(Integer oppgitt, Integer faktisk);
        }
    }

    public static class PsbInfotrygdFødselsnumre implements AbacDto {

        @NotNull
        @Pattern(regexp = "^[\\p{Alnum}\\s]+$", message = "PsbInfotrygdFødselsnumre [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
        private String fødselsnumre;

        public PsbInfotrygdFødselsnumre() {
            // empty ctor
        }

        public PsbInfotrygdFødselsnumre(@NotNull String fødselsnumre) {
            this.fødselsnumre = fødselsnumre;
        }

        @NotNull
        public String getFødselsnumre() {
            return fødselsnumre;
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett();
        }

        @Provider
        public static class PsbInfotrygdFødselsnumregMessageBodyReader implements MessageBodyReader<PsbInfotrygdFødselsnumre> {

            @Override
            public boolean isReadable(Class<?> type, Type genericType,
                                      Annotation[] annotations, MediaType mediaType) {
                return (type == PsbInfotrygdFødselsnumre.class);
            }

            @Override
            public PsbInfotrygdFødselsnumre readFrom(Class<PsbInfotrygdFødselsnumre> type, Type genericType,
                                                      Annotation[] annotations, MediaType mediaType,
                                                      MultivaluedMap<String, String> httpHeaders,
                                                      InputStream inputStream)
                    throws IOException, WebApplicationException {
                var sb = new StringBuilder(200);
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream))) {
                    sb.append(br.readLine()).append('\n');
                }

                return new PsbInfotrygdFødselsnumre(sb.toString());

            }
        }
    }
}
