package no.nav.k9.sak.web.app.tjenester.fordeling;

import static no.nav.k9.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.feil.LogLevel.WARN;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.dokument.arkiv.journal.SafAdapter;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.mottak.FinnEllerOpprettSak;
import no.nav.k9.sak.kontrakt.mottak.FinnSak;
import no.nav.k9.sak.kontrakt.mottak.JournalpostMottakDto;
import no.nav.k9.sak.kontrakt.søknad.innsending.Innsending;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingMottatt;
import no.nav.k9.sak.mottak.SøknadMottakTjenesteContainer;
import no.nav.k9.sak.mottak.dokumentmottak.InngåendeSaksdokument;
import no.nav.k9.sak.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.sikkerhet.abac.AppAbacAttributtType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.jackson.JacksonJsonConfig;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

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
    private ObjectWriter objectWriter = new JacksonJsonConfig().getObjectMapper().writerFor(Innsending.class);

    public FordelRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public FordelRestTjeneste(SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste,
                              SafAdapter safAdapter,
                              FagsakTjeneste fagsakTjeneste,
                              MottatteDokumentRepository mottatteDokumentRepository,
                              SøknadMottakTjenesteContainer søknadMottakere) {
        this.dokumentmottakTjeneste = dokumentmottakTjeneste;
        this.safAdapter = safAdapter;
        this.fagsakTjeneste = fagsakTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadMottakere = søknadMottakere;
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

}
