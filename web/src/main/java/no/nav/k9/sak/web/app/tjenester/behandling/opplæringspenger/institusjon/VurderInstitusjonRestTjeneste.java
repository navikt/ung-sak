package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.institusjon;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.GodkjentOpplæringsinstitusjonTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class VurderInstitusjonRestTjeneste {

    public static final String BASEPATH = "/behandling/institusjon";

    private BehandlingRepository behandlingRepository;
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    private GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    VurderInstitusjonRestTjeneste() {
    }

    @Inject
    public VurderInstitusjonRestTjeneste(BehandlingRepository behandlingRepository,
                                         VurdertOpplæringRepository vurdertOpplæringRepository,
                                         GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste,
                                         UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.godkjentOpplæringsinstitusjonTjeneste = godkjentOpplæringsinstitusjonTjeneste;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
    }

    @GET
    @Operation(description = "Hent institusjonsvurderinger",
        summary = "Returnerer alle perioder og tilhørende institusjonsvurderinger",
        tags = "opplæringspenger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "perioder fra søknad og vurderte institusjoner",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = InstitusjonerDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @Path((BASEPATH))
    public Response hentVurdertInstitusjon(@NotNull @QueryParam(BehandlingUuidDto.NAME)
                                           @Parameter(description = BehandlingUuidDto.DESC)
                                           @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
                                           BehandlingUuidDto behandlingUuidDto) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuidDto.getBehandlingUuid());
        var referanse = BehandlingReferanse.fra(behandling);

        var grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(referanse.getBehandlingId());

        var perioderFraSøknad = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId())
            .map(uttaksPerioderGrunnlag -> uttaksPerioderGrunnlag.getRelevantSøknadsperioder().getPerioderFraSøknadene())
            .orElse(Set.of());

        return Response.ok()
            .entity(mapTilDto(grunnlag.orElse(null), perioderFraSøknad))
            .build();
    }

    private InstitusjonerDto mapTilDto(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {

        List<InstitusjonVurderingDto> vurderinger = new ArrayList<>();

        if (grunnlag != null && grunnlag.getVurdertInstitusjonHolder() != null) {
            for (VurdertInstitusjon vurdertInstitusjon : grunnlag.getVurdertInstitusjonHolder().getVurdertInstitusjon()) {
                vurderinger.add(new InstitusjonVurderingDto(
                    new JournalpostIdDto(vurdertInstitusjon.getJournalpostId().getVerdi()),
                    finnPerioderForJournalpostId(vurdertInstitusjon.getJournalpostId(), perioderFraSøknad),
                    vurdertInstitusjon.getGodkjent() ? Resultat.GODKJENT_MANUELT : Resultat.IKKE_GODKJENT_MANUELT,
                    vurdertInstitusjon.getBegrunnelse())
                );
            }
        }

        List<InstitusjonPeriodeDto> beskrivelser = new ArrayList<>();

        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {

            var kursperioder = fraSøknad.getKurs();
            var journalpostId = fraSøknad.getJournalpostId();

            for (KursPeriode kursPeriode : kursperioder) {
                beskrivelser.add(new InstitusjonPeriodeDto(
                    new Periode(kursPeriode.getPeriode().getFomDato(), kursPeriode.getPeriode().getTomDato()),
                    kursPeriode.getInstitusjon(),
                    new JournalpostIdDto(journalpostId.getVerdi()))
                );
            }

            var institusjonUuid = kursperioder.stream().findAny().orElseThrow().getInstitusjonUuid();
            var perioder = kursperioder.stream().map(KursPeriode::getPeriode).toList();

            if (godkjentIRegister(institusjonUuid, perioder)) {
                vurderinger.add(new InstitusjonVurderingDto(
                    new JournalpostIdDto(journalpostId.getVerdi()),
                    perioder.stream().map(periode -> new Periode(periode.getFomDato(), periode.getTomDato())).toList(),
                    Resultat.GODKJENT_AUTOMATISK,
                    null)
                );

            } else if (vurderinger.stream().noneMatch(vurdering -> vurdering.getJournalpostId().getJournalpostId().equals(journalpostId))) {
                vurderinger.add(new InstitusjonVurderingDto(
                    new JournalpostIdDto(journalpostId.getVerdi()),
                    perioder.stream().map(periode -> new Periode(periode.getFomDato(), periode.getTomDato())).toList(),
                    Resultat.MÅ_VURDERES,
                    null)
                );
            }
        }

        return new InstitusjonerDto(beskrivelser, vurderinger);
    }

    private List<Periode> finnPerioderForJournalpostId(JournalpostId journalpostId, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<KursPeriode> perioder = new ArrayList<>();
        for (PerioderFraSøknad periode : perioderFraSøknad) {
            if (periode.getJournalpostId().equals(journalpostId)) {
                perioder.addAll(periode.getKurs());
            }
        }
        return perioder.stream().map(kursPeriode -> new Periode(kursPeriode.getPeriode().getFomDato(), kursPeriode.getPeriode().getTomDato())).toList();
    }

    private boolean godkjentIRegister(UUID institusjonUuid, List<DatoIntervallEntitet> perioder) {
        if (institusjonUuid == null) {
            return false;
        }

        return godkjentOpplæringsinstitusjonTjeneste.hentAktivMedUuid(institusjonUuid, TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(perioder)))
            .isPresent();
    }
}
