package no.nav.ung.sak.web.app.tjenester.fordeling;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.integrasjon.saf.*;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.felles.sikkerhet.abac.*;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.PollTaskAfterTransaction;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.ung.sak.dokument.arkiv.journal.SafAdapter;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.mottak.*;
import no.nav.ung.sak.mottak.SøknadMottakTjenesteContainer;
import no.nav.ung.sak.mottak.dokumentmottak.InngåendeSaksdokument;
import no.nav.ung.sak.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.ung.sak.sikkerhet.abac.AppAbacAttributtType;
import no.nav.ung.sak.typer.*;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.ung.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

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
        Periode periode = opprettSakDto.getPeriode();
        if (periode == null) {
            throw new IllegalArgumentException("Kan ikke opprette fagsak uten å oppgi start av periode (fravær/uttak): " + opprettSakDto);
        }

        var søknadMottaker = søknadMottakere.finnSøknadMottakerTjeneste(ytelseType);

        var nyFagsak = søknadMottaker.finnEllerOpprettFagsak(ytelseType, aktørId, periode.getFom(), periode.getTom());

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

        Periode periode = opprettSakDto.getPeriode();
        if (periode == null) {
            throw new IllegalArgumentException("Kan ikke opprette fagsak uten å oppgi start av periode (fravær/uttak): " + opprettSakDto);
        }

        Saksnummer saksnummer = null;
        if (opprettSakDto.getSaksnummer() != null) {
            saksnummer = new Saksnummer(opprettSakDto.getSaksnummer());
        }

        var søknadMottaker = søknadMottakere.finnSøknadMottakerTjeneste(ytelseType);

        var nyFagsak = søknadMottaker.finnEllerOpprettFagsak(ytelseType, aktørId, periode.getFom(), periode.getTom());

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
        AktørId relatertPersonAktørId = finnSakDto.getRelatertPersonAktørId();
        var periode = finnSakDto.getPeriode();

        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, bruker, periode.getFom(), periode.getTom());

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
        final FagsakYtelseType ytelseType = journalpostMottakOpprettSakDto.getYtelseType();
        Periode periode = journalpostMottakOpprettSakDto.getPeriode();
        if (periode == null) {
            throw new IllegalArgumentException("Kan ikke opprette fagsak uten å oppgi start av periode (fravær/uttak): " + journalpostMottakOpprettSakDto);
        }

        var søknadMottaker = søknadMottakere.finnSøknadMottakerTjeneste(ytelseType);

        return søknadMottaker.finnEllerOpprettFagsak(
            ytelseType,
            new AktørId(journalpostMottakOpprettSakDto.getAktørId()),
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
