package no.nav.foreldrepenger.web.app.tjenester.fordeling;

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
import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.journal.JournalTjeneste;
import no.nav.foreldrepenger.kontrakter.fordel.BehandlendeFagsystemDto;
import no.nav.foreldrepenger.kontrakter.fordel.FagsakInfomasjonDto;
import no.nav.foreldrepenger.kontrakter.fordel.JournalpostKnyttningDto;
import no.nav.foreldrepenger.kontrakter.fordel.OpprettSakDto;
import no.nav.foreldrepenger.kontrakter.fordel.VurderFagsystemDto;
import no.nav.foreldrepenger.mottak.dokumentmottak.InngåendeSaksdokument;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerPleiepengerBarnSoknad;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakTjeneste;
import no.nav.foreldrepenger.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.BehandlingTema;
import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.mottak.JournalpostMottakDto;
import no.nav.k9.sak.kontrakt.søknad.psb.PleiepengerBarnSøknadMottatt;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad;
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
    private static final String POST_VURDER_FAGSYSTEM = "/vurderFagsystem";
    private static final String JSON_UTF8 = "application/json; charset=UTF-8";

    private SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste;
    private JournalTjeneste journalTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private OpprettSakOrchestrator opprettSakOrchestrator;
    private OpprettSakTjeneste opprettSakTjeneste;
    private VurderFagsystemFellesTjeneste vurderFagsystemTjeneste;
    private DokumentmottakerPleiepengerBarnSoknad dokumentmottakerPleiepengerBarnSoknad;

    public FordelRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public FordelRestTjeneste(SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste,
                              JournalTjeneste journalTjeneste, FagsakTjeneste fagsakTjeneste,
                              OpprettSakOrchestrator opprettSakOrchestrator, OpprettSakTjeneste opprettSakTjeneste,
                              VurderFagsystemFellesTjeneste vurderFagsystemFellesTjeneste,
                              DokumentmottakerPleiepengerBarnSoknad dokumentmottakerPleiepengerBarnSoknad) { // NOSONAR
        this.dokumentmottakTjeneste = dokumentmottakTjeneste;
        this.journalTjeneste = journalTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.opprettSakOrchestrator = opprettSakOrchestrator;
        this.opprettSakTjeneste = opprettSakTjeneste;
        this.vurderFagsystemTjeneste = vurderFagsystemFellesTjeneste;
        this.dokumentmottakerPleiepengerBarnSoknad = dokumentmottakerPleiepengerBarnSoknad;
    }

    @POST
    @Path(POST_VURDER_FAGSYSTEM)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Informasjon om en fagsak", tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, ressurs = BeskyttetRessursResourceAttributt.FAGSAK)
    public BehandlendeFagsystemDto vurderFagsystem(@Parameter(description = "Krever behandlingstemaOffisiellKode", required = true) @Valid AbacVurderFagsystemDto vurderFagsystemDto) {
        VurderFagsystem vurderFagsystem = map(vurderFagsystemDto);
        BehandlendeFagsystem behandlendeFagsystem = vurderFagsystemTjeneste.vurderFagsystem(vurderFagsystem);
        return map(behandlendeFagsystem);

    }

    @POST
    @Path(POST_INFORMASJON_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Informasjon om en fagsak", summary = ("Varsel om en ny journalpost som skal behandles i systemet."), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, ressurs = BeskyttetRessursResourceAttributt.FAGSAK)
    public FagsakInfomasjonDto fagsak(@Parameter(description = "Saksnummeret det skal hentes saksinformasjon om") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {
        Optional<Fagsak> optFagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummerDto.getVerdi(), false);
        if (!optFagsak.isPresent() || optFagsak.get().getSkalTilInfotrygd()) {
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
    public SaksnummerDto opprettSak(@Parameter(description = "Oppretter fagsak") @Valid AbacOpprettSakDto opprettSakDto) {
        Optional<String> journalpostId = opprettSakDto.getJournalpostId();
        BehandlingTema behandlingTema = BehandlingTema.finnForKodeverkEiersKode(opprettSakDto.getBehandlingstemaOffisiellKode());

        AktørId aktørId = new AktørId(opprettSakDto.getAktørId());

        Saksnummer s;
        if (journalpostId.isPresent()) {
            s = opprettSakOrchestrator.opprettSak(new JournalpostId(journalpostId.get()), behandlingTema, aktørId);
        } else {
            s = opprettSakOrchestrator.opprettSak(behandlingTema, aktørId);
        }
        return new SaksnummerDto(s.getVerdi());
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
    public PleiepengerBarnSøknadMottatt psbSoknad(@Parameter(description = "Søknad i JSON-format.") @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) @Valid PleiepengerBarnSøknad pleiepengerBarnSoknad) {
        final Behandling behandling = dokumentmottakerPleiepengerBarnSoknad.mottaSoknad(pleiepengerBarnSoknad);
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

    private VurderFagsystem map(VurderFagsystemDto dto) {
        VurderFagsystem v = new VurderFagsystem();
        dto.getJournalpostId().map(jpi -> new JournalpostId(jpi)).ifPresent(v::setJournalpostId);
        v.setStrukturertSøknad(dto.isStrukturertSøknad());
        v.setAktørId(new AktørId(dto.getAktørId()));
        BehandlingTema behandlingTema = BehandlingTema.finnForKodeverkEiersKode(dto.getBehandlingstemaOffisiellKode());

        v.setBehandlingTema(behandlingTema);
        dto.getÅrsakInnsendingInntektsmelding().ifPresent(v::setÅrsakInnsendingInntektsmelding);
        dto.getVirksomhetsnummer().ifPresent(v::setVirksomhetsnummer);
        dto.getArbeidsgiverAktørId().map(AktørId::new).ifPresent(v::setArbeidsgiverAktørId);
        dto.getArbeidsforholdsid().ifPresent(v::setArbeidsforholdsid);
        dto.getForsendelseMottattTidspunkt().ifPresent(v::setForsendelseMottattTidspunkt);
        dto.getStartDatoForeldrepengerInntektsmelding().ifPresent(v::setStartDatoInntektsmelding);

        dto.getSaksnummer().ifPresent(sn -> v.setSaksnummer(new Saksnummer(sn)));

        v.setDokumentTypeId(DokumentTypeId.UDEFINERT);
        v.setDokumentKategori(DokumentKategori.UDEFINERT);
        if (dto.getDokumentTypeIdOffisiellKode() != null) {
            v.setDokumentTypeId(DokumentTypeId.finnForKodeverkEiersKode(dto.getDokumentTypeIdOffisiellKode()));
        }
        if (dto.getDokumentKategoriOffisiellKode() != null) {
            v.setDokumentKategori(
                DokumentKategori.finnForKodeverkEiersKode(dto.getDokumentKategoriOffisiellKode()));
        }

        return v;
    }

    private BehandlendeFagsystemDto map(BehandlendeFagsystem behandlendeFagsystem) {
        BehandlendeFagsystemDto dto;
        if (behandlendeFagsystem.getSaksnummer().isPresent()) {
            dto = new BehandlendeFagsystemDto(behandlendeFagsystem.getSaksnummer().get().getVerdi()); // NOSONAR
        } else {
            dto = new BehandlendeFagsystemDto();
        }
        switch (behandlendeFagsystem.getBehandlendeSystem()) {
            case VEDTAKSLØSNING:
                dto.setBehandlesIVedtaksløsningen(true);
                break;
            case VURDER_INFOTRYGD:
                dto.setSjekkMotInfotrygd(true);
                break;
            case MANUELL_VURDERING:
                dto.setManuellVurdering(true);
                break;
            case PRØV_IGJEN:
                dto.setPrøvIgjen(true);
                dto.setPrøvIgjenTidspunkt(behandlendeFagsystem.getPrøvIgjenTidspunkt());
                break;
            default:
                throw new IllegalArgumentException("Utviklerfeil, manglende mapping");
        }
        return dto;
    }

    private InngåendeSaksdokument map(AbacJournalpostMottakDto mottattJournalpost) {
        BehandlingTema behandlingTema = BehandlingTema.finnForKodeverkEiersKode(mottattJournalpost.getBehandlingstemaOffisiellKode());
        JournalpostId journalpostId = mottattJournalpost.getJournalpostId();

        Saksnummer saksnummer = mottattJournalpost.getSaksnummer();
        Optional<Fagsak> fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false);
        if (!fagsak.isPresent()) {
            // FIXME (u139158): PK- hvordan skal dette håndteres?
            throw new IllegalStateException("Finner ingen fagsak for saksnummer " + saksnummer);
        }

        DokumentTypeId dokumentTypeId = mottattJournalpost.getDokumentTypeIdOffisiellKode().isPresent()
            ? DokumentTypeId.finnForKodeverkEiersKode(mottattJournalpost.getDokumentTypeIdOffisiellKode().get())
            : DokumentTypeId.UDEFINERT; // NOSONAR

        DokumentKategori dokumentKategori = mottattJournalpost.getDokumentKategoriOffisiellKode() != null
            ? DokumentKategori.finnForKodeverkEiersKode(mottattJournalpost.getDokumentKategoriOffisiellKode())
            : DokumentKategori.UDEFINERT; // NOSONAR

        dokumentKategori = utledDokumentKategori(dokumentKategori, dokumentTypeId);

        Optional<String> payloadXml = mottattJournalpost.getPayloadXml();
        InngåendeSaksdokument.Builder builder = InngåendeSaksdokument.builder()
            .medFagsakId(fagsak.get().getId())
            .medBehandlingTema(behandlingTema)
            .medElektroniskSøknad(payloadXml.isPresent())
            .medJournalpostId(mottattJournalpost.getJournalpostId())
            .medDokumentTypeId(dokumentTypeId.getKode())
            .medDokumentKategori(dokumentKategori)
            .medJournalførendeEnhet(mottattJournalpost.getJournalForendeEnhet());

        if (DokumentTypeId.INNTEKTSMELDING.getKode().equals(dokumentTypeId.getKode())) {
            String referanseFraJournalpost = utledAltinnReferanseFraInntektsmelding(journalpostId);
            builder.medKanalreferanse(referanseFraJournalpost);
        }

        mottattJournalpost.getForsendelseId().ifPresent(builder::medForsendelseId);

        if (payloadXml.isPresent()) {
            builder.medPayloadXml(payloadXml.get()); // NOSONAR
        }

        builder.medForsendelseMottatt(mottattJournalpost.getForsendelseMottatt().orElse(LocalDate.now())); // NOSONAR
        builder.medForsendelseMottatt(mottattJournalpost.getForsendelseMottattTidspunkt()); // NOSONAR

        return builder.build();
    }

    private DokumentKategori utledDokumentKategori(DokumentKategori dokumentKategori, Kodeverdi dokumentTypeId) {
        if (DokumentTypeId.getSøknadTyper().contains(dokumentTypeId.getKode())) {
            return DokumentKategori.SØKNAD;
        }
        return dokumentKategori;
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

    public static class AbacVurderFagsystemDto extends VurderFagsystemDto implements AbacDto {
        public AbacVurderFagsystemDto() {
            super();
        }

        public AbacVurderFagsystemDto(String journalpostId, boolean strukturertSøknad, String aktørId, String behandlingstemaOffisiellKode) {
            super(journalpostId, strukturertSøknad, aktørId, behandlingstemaOffisiellKode);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            AbacDataAttributter abacDataAttributter = AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.AKTØR_ID, getAktørId());

            getJournalpostId().ifPresent(id -> abacDataAttributter.leggTil(AppAbacAttributtType.JOURNALPOST_ID, new JournalpostId(id)));
            getSaksnummer().ifPresent(sn -> abacDataAttributter.leggTil(AppAbacAttributtType.SAKSNUMMER, new Saksnummer(sn)));
            return abacDataAttributter;
        }
    }

    public static class AbacOpprettSakDto extends OpprettSakDto implements AbacDto {
        public AbacOpprettSakDto() {
            super();
        }

        public AbacOpprettSakDto(String journalpostId, String behandlingstemaOffisiellKode, String aktørId) {
            super(journalpostId, behandlingstemaOffisiellKode, aktørId);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, getAktørId());
        }
    }
}
