package no.nav.k9.sak.web.app.tjenester.fordeling;

import static no.nav.vedtak.feil.LogLevel.WARN;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Function;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.kontrakter.fordel.FagsakInfomasjonDto;
import no.nav.foreldrepenger.kontrakter.fordel.JournalpostKnyttningDto;
import no.nav.k9.kodeverk.behandling.BehandlingTema;
import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.dokument.arkiv.journal.JournalTjeneste;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.mottak.FinnEllerOpprettSak;
import no.nav.k9.sak.kontrakt.mottak.JournalpostMottakDto;
import no.nav.k9.sak.kontrakt.søknad.psb.PleiepengerBarnSøknadInnsending;
import no.nav.k9.sak.kontrakt.søknad.psb.PleiepengerBarnSøknadMottatt;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerPleiepengerBarnSoknad;
import no.nav.k9.sak.mottak.dokumentmottak.InngåendeSaksdokument;
import no.nav.k9.sak.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.k9.sak.sikkerhet.abac.AppAbacAttributtType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.k9.sak.web.app.soap.sak.tjeneste.OpprettSakTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

// FIXME K9 Hei Stian!

/**
 * Mottar dokumenter fra f.eks. FPFORDEL og håndterer dispatch internt for saksbehandlingsløsningen.
 */
@Path(FordelRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class FordelRestTjeneste {

    static final String BASE_PATH = "/fordel";
    private static final String POST_INFORMASJON_PATH = "/fagsak/informasjon";
    private static final String POST_OPPRETT_PATH = "/fagsak/opprett";
    private static final String POST_KNYTT_JOURNALPOST_PATH = "/fagsak/knyttJournalpost";
    private static final String POST_JOURNALPOST_PATH = "/journalpost";
    private static final String JSON_UTF8 = "application/json; charset=UTF-8";

    private SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste;
    private JournalTjeneste journalTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private OpprettSakTjeneste opprettSakTjeneste;
    private DokumentmottakerPleiepengerBarnSoknad dokumentmottakerPleiepengerBarnSoknad;

    public FordelRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public FordelRestTjeneste(SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste,
                              JournalTjeneste journalTjeneste, FagsakTjeneste fagsakTjeneste,
                              OpprettSakOrchestrator opprettSakOrchestrator, OpprettSakTjeneste opprettSakTjeneste,
                              DokumentmottakerPleiepengerBarnSoknad dokumentmottakerPleiepengerBarnSoknad) { // NOSONAR
        this.dokumentmottakTjeneste = dokumentmottakTjeneste;
        this.journalTjeneste = journalTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.opprettSakTjeneste = opprettSakTjeneste;
        this.dokumentmottakerPleiepengerBarnSoknad = dokumentmottakerPleiepengerBarnSoknad;
    }

    @POST
    @Path(POST_INFORMASJON_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Informasjon om en fagsak", summary = ("Varsel om en ny journalpost som skal behandles i systemet."), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, ressurs = BeskyttetRessursResourceAttributt.FAGSAK)
    public FagsakInfomasjonDto fagsak(@Parameter(description = "Saksnummeret det skal hentes saksinformasjon om") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {
        Optional<Fagsak> optFagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummerDto.getVerdi(), false);
        if (optFagsak.isEmpty() || optFagsak.get().getSkalTilInfotrygd()) {
            return null;
        }
        var behandlingTemaFraKodeverksRepo = optFagsak.get().getBehandlingTema();
        String behandlingstemaOffisiellKode = behandlingTemaFraKodeverksRepo.getOffisiellKode();
        AktørId aktørId = optFagsak.get().getAktørId();

        /* FIXME K9 bytt kontrakt her */
        return new FagsakInfomasjonDto(aktørId.getId(), behandlingstemaOffisiellKode, false);
    }

    @POST
    @Path(POST_OPPRETT_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Ny journalpost skal behandles.", summary = ("Varsel om en ny journalpost som skal behandles i systemet."), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, ressurs = BeskyttetRessursResourceAttributt.FAGSAK)
    public SaksnummerDto opprettSak(@Parameter(description = "Oppretter fagsak") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) FinnEllerOpprettSak opprettSakDto) {
        BehandlingTema behandlingTema = BehandlingTema.finnForKodeverkEiersKode(opprettSakDto.getBehandlingstemaOffisiellKode());

        AktørId aktørId = new AktørId(opprettSakDto.getAktørId());
        AktørId pleietrengendeAktørId = null;
        if (opprettSakDto.getPleietrengendeAktørId() != null) {
            pleietrengendeAktørId = new AktørId(opprettSakDto.getPleietrengendeAktørId());
        }

        var startDato = opprettSakDto.getPeriodeStart() != null ? opprettSakDto.getPeriodeStart() : LocalDate.now();

        var nyFagsakFor = dokumentmottakerPleiepengerBarnSoknad.finnEllerOpprett(behandlingTema.getFagsakYtelseType(), aktørId, pleietrengendeAktørId, startDato);

        return new SaksnummerDto(nyFagsakFor.getSaksnummer().getVerdi());
    }

    @POST
    @Path(POST_KNYTT_JOURNALPOST_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Knytt journalpost til fagsak.", summary = ("Før en journalpost journalføres på en fagsak skal fagsaken oppdateres med journalposten."), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, ressurs = BeskyttetRessursResourceAttributt.FAGSAK)
    public void knyttSakOgJournalpost(@Parameter(description = "Saksnummer og JournalpostId som skal knyttes sammen") @Valid AbacJournalpostKnyttningDto journalpostKnytningDto) {
        opprettSakTjeneste.knyttSakOgJournalpost(new Saksnummer(journalpostKnytningDto.getSaksnummer()),
            new JournalpostId(journalpostKnytningDto.getJournalpostId()));
    }

    @POST
    @Path("psbSoknad")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Mottak av søknad for pleiepenger barn.", tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, ressurs = BeskyttetRessursResourceAttributt.FAGSAK)
    public PleiepengerBarnSøknadMottatt psbSoknad(@Parameter(description = "Søknad i JSON-format.") @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) @Valid PleiepengerBarnSøknadInnsending pleiepengerBarnSoknad) {
        final Behandling behandling = dokumentmottakerPleiepengerBarnSoknad.mottaSoknad(pleiepengerBarnSoknad.getSaksnummer(), pleiepengerBarnSoknad.getSøknad());
        return new PleiepengerBarnSøknadMottatt(behandling.getFagsak().getSaksnummer());
    }

    @POST
    @Path(POST_JOURNALPOST_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Ny journalpost skal behandles.", summary = ("Varsel om en ny journalpost som skal behandles i systemet."), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, ressurs = BeskyttetRessursResourceAttributt.FAGSAK)
    public void mottaJournalpost(@Parameter(description = "Krever saksnummer, journalpostId og behandlingstemaOffisiellKode") @Valid AbacJournalpostMottakDto mottattJournalpost) {

        InngåendeSaksdokument saksdokument = map(mottattJournalpost);
        dokumentmottakTjeneste.dokumentAnkommet(saksdokument);
    }

    private InngåendeSaksdokument map(AbacJournalpostMottakDto mottattJournalpost) {
        JournalpostId journalpostId = mottattJournalpost.getJournalpostId();

        Saksnummer saksnummer = mottattJournalpost.getSaksnummer();
        Optional<Fagsak> fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false);
        if (fagsak.isEmpty()) {
            throw new IllegalStateException("Finner ingen fagsak for saksnummer " + saksnummer);
        }

        DokumentKategori dokumentKategori = mottattJournalpost.getDokumentKategoriOffisiellKode() != null
            ? DokumentKategori.finnForKodeverkEiersKode(mottattJournalpost.getDokumentKategoriOffisiellKode())
            : DokumentKategori.UDEFINERT; // NOSONAR

        Optional<String> payloadXml = mottattJournalpost.getPayloadXml();
        String dokumentTypeId = mottattJournalpost.getDokumentTypeIdOffisiellKode().orElse(null);
        InngåendeSaksdokument.Builder builder = InngåendeSaksdokument.builder()
            .medFagsakId(fagsak.get().getId())
            .medElektroniskSøknad(payloadXml.isPresent())
            .medDokumentTypeId(dokumentTypeId)
            .medJournalpostId(mottattJournalpost.getJournalpostId())
            .medDokumentKategori(dokumentKategori)
            .medJournalførendeEnhet(mottattJournalpost.getJournalForendeEnhet());

        if (DokumentTypeId.INNTEKTSMELDING.getOffisiellKode().equals(dokumentTypeId)) {
            String referanseFraJournalpost = utledAltinnReferanseFraInntektsmelding(journalpostId);
            builder.medKanalreferanse(referanseFraJournalpost);
        }

        mottattJournalpost.getForsendelseId().ifPresent(builder::medForsendelseId);

        if (payloadXml.isPresent()) {
            builder.medPayload(payloadXml.get()); // NOSONAR
        }

        builder.medForsendelseMottatt(mottattJournalpost.getForsendelseMottatt().orElse(LocalDate.now())); // NOSONAR
        builder.medForsendelseMottatt(mottattJournalpost.getForsendelseMottattTidspunkt()); // NOSONAR

        return builder.build();
    }

    private String utledAltinnReferanseFraInntektsmelding(JournalpostId journalpostId) {
        ArkivJournalPost journalPost = journalTjeneste.hentInngåendeJournalpostHoveddokument(journalpostId);
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

        public AbacJournalpostMottakDto(Saksnummer saksnummer, JournalpostId journalpostId,
                                        String behandlingstemaOffisiellKode,
                                        String dokumentTypeIdOffisiellKode,
                                        LocalDateTime forsendelseMottattTidspunkt, String payloadXml) {
            super(saksnummer, journalpostId, behandlingstemaOffisiellKode, dokumentTypeIdOffisiellKode, forsendelseMottattTidspunkt, payloadXml);
        }

        static Optional<String> getPayloadValiderLengde(String base64EncodedPayload, Integer deklarertLengde) {
            if (base64EncodedPayload == null) {
                return Optional.empty();
            }
            if (deklarertLengde == null) {
                throw JournalpostMottakFeil.FACTORY.manglerPayloadLength().toException();
            }
            byte[] bytes = Base64.getUrlDecoder().decode(base64EncodedPayload);
            String streng = new String(bytes, StandardCharsets.UTF_8);
            if (streng.length() != deklarertLengde) {
                throw JournalpostMottakFeil.FACTORY.feilPayloadLength(deklarertLengde, streng.length()).toException();
            }
            return Optional.of(streng);
        }

        @JsonIgnore
        public Optional<String> getPayloadXml() {
            return getPayloadValiderLengde(base64EncodedPayloadXml, payloadLength);
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
