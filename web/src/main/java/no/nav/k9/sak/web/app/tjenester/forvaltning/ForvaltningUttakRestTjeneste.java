package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.felles.util.InputValideringRegex;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.trigger.ProsessTriggerForvaltningTjeneste;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag.ForvaltningBeregningRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett.EndringAnnenOmsorgspersonUtleder;

@ApplicationScoped
@Transactional
@Path("/uttak")
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningUttakRestTjeneste {
    private BehandlingRepository behandlingRepository;
    private EntityManager entityManager;
    private FagsakTjeneste fagsakTjeneste;
    private ProsessTriggerForvaltningTjeneste prosessTriggerForvaltningTjeneste;
    private ProsessTriggereRepository prosessTriggereRepository;
    private EndringAnnenOmsorgspersonUtleder endringAnnenOmsorgspersonUtleder;

    public ForvaltningUttakRestTjeneste() {
    }

    @Inject
    public ForvaltningUttakRestTjeneste(BehandlingRepository behandlingRepository,
                                        EntityManager entityManager,
                                        FagsakTjeneste fagsakTjeneste,
                                        ProsessTriggereRepository prosessTriggereRepository,
                                        EndringAnnenOmsorgspersonUtleder endringAnnenOmsorgspersonUtleder) {
        this.behandlingRepository = behandlingRepository;
        this.entityManager = entityManager;
        this.fagsakTjeneste = fagsakTjeneste;
        this.prosessTriggerForvaltningTjeneste = new ProsessTriggerForvaltningTjeneste(entityManager);
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.endringAnnenOmsorgspersonUtleder = endringAnnenOmsorgspersonUtleder;
    }

    @POST
    @Path("/hent-endringstidslinjer-fra-annen-omsorgsperson")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter tidslinjer der annen omsorgsperson påvirket sak for sak med behandlingsårsak RE_ANNEN_SAK", summary = ("Henter tidslinjer som fører til endring i sak"), tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response hentTidslinjerEndringFraAnnenOmsorgsperson(
        @Parameter(description = "Behandling-UUID")
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingIdDto behandlingIdDto) {


        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingId());


        var originalBehandlingId = behandling.getOriginalBehandlingId();

        if (originalBehandlingId.isEmpty()) {
            throw new IllegalArgumentException("Saken har ingen revurdering");
        }

        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId.get());


        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandling.getId());
        var perioderMedEndringAnnenOmsorgsperson = prosessTriggere.stream()
            .flatMap(it -> it.getTriggere().stream())
            .filter(it -> it.getÅrsak().equals(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON))
            .map(Trigger::getPeriode)
            .collect(Collectors.toSet());
        var segmenterEndringAnnenOmsorgsperson = perioderMedEndringAnnenOmsorgsperson.stream()
            .map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), Boolean.TRUE))
            .toList();

        var endringAnnenOmsorgspersonTidslinje = new LocalDateTimeline<>(segmenterEndringAnnenOmsorgsperson);


        var endringstidslinjer = endringAnnenOmsorgspersonUtleder.utledTidslinjerForEndringSomPåvirkerSak(behandling.getFagsak().getSaksnummer(), behandling.getFagsak().getPleietrengendeAktørId(), originalBehandling, endringAnnenOmsorgspersonTidslinje);

        var endringsperioderDto = new EndringsperioderDto(
            lagPeriodeListe(endringstidslinjer.harEndretSykdomTidslinje()),
            lagPeriodeListe(endringstidslinjer.harEndretEtablertTilsynTidslinje()),
            lagPeriodeListe(endringstidslinjer.harEndretNattevåkOgBeredskapTidslinje()),
            lagPeriodeListe(endringstidslinjer.harEndretUttakTidslinje()),
            lagPeriodeListe(endringstidslinjer.harEndringSomPåvirkerSakTidslinje())
        );

        loggForvaltningTjeneste(behandling.getFagsak(), "/hent-endringstidslinjer-fra-annen-omsorgsperson", "henter endringstidslinjer for sak der sak ble påvirket av annen part");


        return Response.ok(endringsperioderDto).build();

    }

    private static List<Periode> lagPeriodeListe(LocalDateTimeline<Boolean> tidslinje) {
        return tidslinje.compress()
            .getLocalDateIntervals().stream()
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .toList();
    }

    @POST
    @Path("/fjern-prosesstrigger-endring-annen-omsorgsperson")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner prosesstrigger for endring fra annen omsorgsperson", summary = ("Fjerner prosesstrigger for endring fra annen omsorgsperson"), tags = "uttak")
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    public void fjernProsessTriggerForEndringFraAnnenOmsorgsperson(@Parameter(description = "Saksnummer og skjæringstidspunkt (YYYY-MM-DD) på csv-format") @Valid SaksnummerOgSkjæringstidspunktDto saksnummerOgStp) {

        var alleSaksnummerOgSkjæringstidspunkt = Objects.requireNonNull(saksnummerOgStp.getSaksnummerOgSkjæringstidspunkt(), "saksnummerOgSkjæringstidspunkt");
        var saknummerOgSkjæringstidspunkt = new LinkedHashSet<>(Arrays.asList(alleSaksnummerOgSkjæringstidspunkt.split("\\s+")));

        for (var s : saknummerOgSkjæringstidspunkt) {
            var sakOgStpSplitt = s.split(",");
            var saksnummer = new Saksnummer(sakOgStpSplitt[0]);
            var stp = LocalDate.parse(sakOgStpSplitt[1]);
            var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow(() -> new IllegalArgumentException("finnes ikke fagsak med saksnummer: " + saksnummer.getVerdi()));

            var behandling = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId());

            if (behandling.isEmpty()) {
                throw new IllegalArgumentException("Fant ingen behandling");
            }

            if (behandling.get().erSaksbehandlingAvsluttet()) {
                throw new IllegalArgumentException("Behandling med id " + behandling.get().getId() + " hadde avsluttet saksbehandling.");
            }


            var originalBehandlingId = behandling.get().getOriginalBehandlingId();

            if (originalBehandlingId.isEmpty()) {
                throw new IllegalArgumentException("Saken har ingen revurdering");
            }

            var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId.get());


            var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandling.get().getId());
            var perioderMedEndringAnnenOmsorgsperson = prosessTriggere.stream()
                .flatMap(it -> it.getTriggere().stream())
                .filter(it -> it.getÅrsak().equals(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON))
                .map(Trigger::getPeriode)
                .collect(Collectors.toSet());
            var segmenterEndringAnnenOmsorgsperson = perioderMedEndringAnnenOmsorgsperson.stream()
                .map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), Boolean.TRUE))
                .toList();

            var endringAnnenOmsorgspersonTidslinje = new LocalDateTimeline<>(segmenterEndringAnnenOmsorgsperson);


            var endringstidslinjer = endringAnnenOmsorgspersonUtleder.utledTidslinjerForEndringSomPåvirkerSak(saksnummer, fagsak.getPleietrengendeAktørId(), originalBehandling, endringAnnenOmsorgspersonTidslinje);

            var utdaterteEndringer = endringAnnenOmsorgspersonTidslinje.disjoint(endringstidslinjer.harEndringSomPåvirkerSakTidslinje());

            var utdaterteIntervaller = utdaterteEndringer.compress().getLocalDateIntervals();

            var skjæringstidspunkterSomKanFjernes = perioderMedEndringAnnenOmsorgsperson.stream()
                .filter(p -> utdaterteIntervaller.stream().anyMatch(i -> i.getFomDato().equals(p.getFomDato()) && i.getTomDato().equals(p.getTomDato())))
                .map(DatoIntervallEntitet::getFomDato)
                .collect(Collectors.toSet());

            if (skjæringstidspunkterSomKanFjernes.stream().noneMatch(stp::equals)) {
                throw new IllegalArgumentException("Kunne ikke fjerne prosesstrigger for skjæringstidspunkt " + stp + " fordi fortsatt har endring.");
            }

            loggForvaltningTjeneste(fagsak, "/fjern-prosesstrigger-endring-annen-omsorgsperson", "fjerner prosesstrigger RE_ANNEN_SAK for skjæringstidspunkt " + stp);

            prosessTriggerForvaltningTjeneste.fjern(behandling.get().getId(), stp, BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON);

        }

    }

    public static class SaksnummerOgSkjæringstidspunktDto implements AbacDto {

        @NotNull
        @Pattern(regexp = InputValideringRegex.FRITEKST, message = "saksnummerOgSkjæringstidspunkt [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
        private String saksnummerOgSkjæringstidspunkt;

        public SaksnummerOgSkjæringstidspunktDto() {
            // empty ctor
        }

        public SaksnummerOgSkjæringstidspunktDto(@NotNull String saksnummerOgSkjæringstidspunkt) {
            this.saksnummerOgSkjæringstidspunkt = saksnummerOgSkjæringstidspunkt;
        }

        public String getSaksnummerOgSkjæringstidspunkt() {
            return saksnummerOgSkjæringstidspunkt;
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett();
        }

        @Provider
        public static class SaksnummerOgSkjæringstidspunktDtoMessageBodyReader implements MessageBodyReader<SaksnummerOgSkjæringstidspunktDto> {

            @Override
            public boolean isReadable(Class<?> type, Type genericType,
                                      Annotation[] annotations, MediaType mediaType) {
                return (type == ForvaltningBeregningRestTjeneste.OpprettManuellRevurderingBeregning.class);
            }

            @Override
            public SaksnummerOgSkjæringstidspunktDto readFrom(Class<SaksnummerOgSkjæringstidspunktDto> type, Type genericType,
                                                              Annotation[] annotations, MediaType mediaType,
                                                              MultivaluedMap<String, String> httpHeaders,
                                                              InputStream inputStream)
                throws IOException, WebApplicationException {
                var sb = new StringBuilder(200);
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream))) {
                    br.lines().forEach(l -> sb.append(l).append('\n'));
                }

                return new SaksnummerOgSkjæringstidspunktDto(sb.toString());

            }
        }
    }

    private void loggForvaltningTjeneste(Fagsak fagsak, String tjeneste, String begrunnelse) {
        /*
         * logger at tjenesten er kalt (er en forvaltnings tjeneste)
         */
        entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId(), tjeneste, begrunnelse));
        entityManager.flush();
    }


}
