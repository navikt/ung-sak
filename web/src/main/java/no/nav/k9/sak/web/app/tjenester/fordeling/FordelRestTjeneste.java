package no.nav.k9.sak.web.app.tjenester.fordeling;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.feil.LogLevel.WARN;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.kontrakter.fordel.JournalpostKnyttningDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.dokument.arkiv.journal.SafAdapter;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.mottak.FinnEllerOpprettSak;
import no.nav.k9.sak.kontrakt.mottak.FinnSak;
import no.nav.k9.sak.kontrakt.mottak.JournalpostMottakDto;
import no.nav.k9.sak.kontrakt.søknad.innsending.Innsending;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingMottatt;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.mottak.dokumentmottak.InngåendeSaksdokument;
import no.nav.k9.sak.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.k9.sak.sikkerhet.abac.AppAbacAttributtType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

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

    private Instance<SøknadMottakTjeneste<?>> søknadMottakere;

    public FordelRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public FordelRestTjeneste(SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste,
                              SafAdapter safAdapter,
                              FagsakTjeneste fagsakTjeneste,
                              @Any Instance<SøknadMottakTjeneste<?>> søknadMottakere) { // NOSONAR
        this.dokumentmottakTjeneste = dokumentmottakTjeneste;
        this.safAdapter = safAdapter;
        this.fagsakTjeneste = fagsakTjeneste;
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

        var startDato = opprettSakDto.getPeriodeStart() != null ? opprettSakDto.getPeriodeStart() : LocalDate.now();
        var søknadMottaker = finnSøknadMottakerTjeneste(ytelseType);

        var nyFagsak = søknadMottaker.finnEllerOpprettFagsak(ytelseType, aktørId, pleietrengendeAktørId, startDato);

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
        var periode = finnSakDto.getPeriode();

        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, bruker, pleietrengendeAktørId, periode.getFom(), periode.getTom());

        return fagsak.isPresent() ? new SaksnummerDto(fagsak.get().getSaksnummer().getVerdi()) : null;
    }

    private FagsakYtelseType finnYtelseType(FinnEllerOpprettSak dto) {
        return FagsakYtelseType.fraKode(dto.getYtelseType());
    }

    @SuppressWarnings({ "unchecked" })
    @POST
    @Path("/innsending")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Mottak av søknad.", tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public InnsendingMottatt innsending(@Parameter(description = "Søknad i JSON-format.") @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) @Valid Innsending innsending) {
        var saksnummer = innsending.getSaksnummer();
        var ytelseType = innsending.getYtelseType();
        var journalpostId = innsending.getJournalpostId();
        var søknadMottaker = finnSøknadMottakerTjeneste(ytelseType);
        søknadMottaker.mottaSøknad(saksnummer, journalpostId, innsending.getInnhold());
        return new InnsendingMottatt(saksnummer);
    }

    @POST
    @Path("/journalpost")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Ny journalpost skal behandles.", summary = ("Varsel om en ny journalpost som skal behandles i systemet."), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public void mottaJournalpost(@Parameter(description = "Krever saksnummer, journalpostId og behandlingstemaOffisiellKode") @Valid AbacJournalpostMottakDto mottattJournalpost) {

        InngåendeSaksdokument saksdokument = map(mottattJournalpost);
        dokumentmottakTjeneste.dokumentAnkommet(saksdokument);
    }

    @SuppressWarnings("rawtypes")
    SøknadMottakTjeneste finnSøknadMottakerTjeneste(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(søknadMottakere, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke støtte for ytelseType:" + ytelseType));
    }

    private InngåendeSaksdokument map(AbacJournalpostMottakDto mottattJournalpost) {
        JournalpostId journalpostId = mottattJournalpost.getJournalpostId();

        Saksnummer saksnummer = mottattJournalpost.getSaksnummer();
        Optional<Fagsak> fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false);
        if (fagsak.isEmpty()) {
            throw new IllegalStateException("Finner ingen fagsak for saksnummer " + saksnummer);
        }

        Optional<String> payload = mottattJournalpost.getPayload();
        InngåendeSaksdokument.Builder builder = InngåendeSaksdokument.builder()
            .medFagsakId(fagsak.get().getId())
            .medElektroniskSøknad(payload.isPresent())
            .medType(mottattJournalpost.getType())
            .medJournalpostId(mottattJournalpost.getJournalpostId());

        String referanseFraJournalpost = utledAltinnReferanseFraInntektsmelding(journalpostId);
        builder.medKanalreferanse(referanseFraJournalpost);

        if (payload.isPresent()) {
            builder.medPayload(payload.get()); // NOSONAR
        }

        builder.medForsendelseMottatt(mottattJournalpost.getForsendelseMottatt().orElse(LocalDate.now())); // NOSONAR
        builder.medForsendelseMottatt(mottattJournalpost.getForsendelseMottattTidspunkt()); // NOSONAR

        return builder.build();
    }

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

    public static class AbacJournalpostKnyttningDto extends JournalpostKnyttningDto implements AbacDto {
        public AbacJournalpostKnyttningDto() {
            super();
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.JOURNALPOST_ID, new JournalpostId(getJournalpostId()))
                .leggTil(AppAbacAttributtType.SAKSNUMMER, new Saksnummer(getSaksnummer()));
        }
    }
}
