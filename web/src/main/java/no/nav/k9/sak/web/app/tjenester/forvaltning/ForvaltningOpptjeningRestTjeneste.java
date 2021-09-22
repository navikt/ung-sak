package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;

import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;

@Path("")
@ApplicationScoped
@Transactional
public class ForvaltningOpptjeningRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningOpptjeningRestTjeneste.class);

    private EntityManager em;
    private AksjonspunktRepository aksjonspunktRepository;
    private VilkårTjeneste vilkårTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public ForvaltningOpptjeningRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningOpptjeningRestTjeneste(EntityManager em, AksjonspunktRepository aksjonspunktRepository, VilkårTjeneste vilkårTjeneste, InntektArbeidYtelseTjeneste iayTjeneste) {
        this.em = em;
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.iayTjeneste = iayTjeneste;
    }

    @POST
    @Path("/opptjening/migrer-begrunnelser-opptjening")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Migrer begrunnelser fra manuelt vurderte opptjeningsaktiviteter over til vilkårsperioder", tags = "opptjening")
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response migrerAksjonspunkt(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) @Parameter(description = "migrerOpptjeningbegrunnelserRequest") MigrerOpptjeningbegrunnelserRequest dto) { // NOSONAR
        no.nav.k9.sak.typer.Periode periode = dto.getPeriode();
        Map<Behandling, Aksjonspunkt> behandlingerMedAksjonspunkt = aksjonspunktRepository.hentAksjonspunkterForKode(periode.getFom(), periode.getTom(), AksjonspunktKodeDefinisjon.VURDER_PERIODER_MED_OPPTJENING_KODE);


        for (Behandling behandling : behandlingerMedAksjonspunkt.keySet()) {
            var ap5051 = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
            if (!ap5051.erUtført()) {
                loggMigrering5051(behandling, "Aksjonspunkt ikke utført, dermed ikke nødvendig å migrere");
            }

            var iayGrunnlag = iayTjeneste.finnGrunnlag(behandling.getId()).orElseThrow(() -> new IllegalStateException("Skal ha IAY dersom AP 5051 er utført"));

            String migrertBegrunnelse;
            var saksbehandletVersjon = iayGrunnlag.getSaksbehandletVersjon().orElse(null);
            if (saksbehandletVersjon == null) {
                loggMigrering5051(behandling, "Ingen opptjeningsaktiviteter har blitt saksbehandlet manuelt");
                migrertBegrunnelse = "Ingen aktiviter til manuell vurdering er blitt godkjent";
            } else {
                loggMigrering5051(behandling, "Fant opptjeningsaktiviteter har blitt saksbehandlet manuelt, migrerer begrunnelser");
                migrertBegrunnelse = saksbehandletVersjon.getAktørArbeid().stream()
                    .flatMap(aktørArbeid -> aktørArbeid.hentAlleYrkesaktiviteter().stream())
                    .map(yrkesaktivitet -> {
                        var aktivitetsAvtaler = yrkesaktivitet.getAlleAktivitetsAvtaler();
                        return aktivitetsAvtaler.stream()
                            .map(aktivitetsAvtale -> "Aktivitet manuelt godkjent:" +
                                "    Arbeidstype=" + yrkesaktivitet.getArbeidType()
                                + ", arbeidsgiver=" + (yrkesaktivitet.getArbeidsgiver() != null ? yrkesaktivitet.getArbeidsgiver() : "uspesifisert")
                                + ", periode=" + aktivitetsAvtale.getPeriode()
                                + ", beskrivelse=" + aktivitetsAvtale.getBeskrivelse())
                            .collect(Collectors.joining("\n"));
                    })
                    .collect(Collectors.joining("\n"));
                if (migrertBegrunnelse.equals("")) {
                    migrertBegrunnelse = "Ingen aktiviter til manuell vurdering er blitt godkjent";
                }
            }

            var opptjeningsvilkår = vilkårTjeneste.hentVilkårResultat(behandling.getId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET)
                .orElseThrow(() -> new IllegalStateException("Må ha opptjeningsvilkår når AP 5051 er utført"));
            for (VilkårPeriode vilkårPeriode : opptjeningsvilkår.getPerioder()) {
                loggMigrering5051(behandling, "Migrerer vilkårsperiode=" + vilkårPeriode.getPeriode());
                if (dto.dryrun()) {
                    continue;
                }
                var query = em.createNativeQuery("UPDATE VR_VILKAR_PERIODE set BEGRUNNELSE = " +
                    "CASE WHEN BEGRUNNELSE LIKE '%MIGRERT%' THEN BEGRUNNELSE " + // NOOP hvis allerede migrert
                    "ELSE LEFT(CONCAT(BEGRUNNELSE, 'MIGRERT BEGRUNNELSE OPPTJENINGSAKTIVITETER: ', :migrert_begrunnelse), 4000) END " +
                    "WHERE id = :id")
                    .setParameter("id", vilkårPeriode.getId())
                    .setParameter("migrert_begrunnelse", migrertBegrunnelse);
                var antallOppdatert = query.executeUpdate();
                if (antallOppdatert != 1) {
                    throw new IllegalStateException("Forventet én oppdatering, antall oppdateringer=" + antallOppdatert);
                }
            }
        }

        return Response.ok().build();
    }

    private void loggMigrering5051(Behandling behandling, String msg) {
        logger.info("Migrering AP 5051, saksnummer={}, behandlingId={}: " + msg, behandling.getFagsak().getSaksnummer(), behandling.getId());
    }

}
