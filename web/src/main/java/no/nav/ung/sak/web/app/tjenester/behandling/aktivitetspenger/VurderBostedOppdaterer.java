package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedAvklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderBostedDto;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderBostedDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderBostedOppdaterer implements AksjonspunktOppdaterer<VurderBostedDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    VurderBostedOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderBostedOppdaterer(BehandlingRepository behandlingRepository,
                                  HistorikkinnslagRepository historikkinnslagRepository,
                                  BostedsGrunnlagRepository bostedsGrunnlagRepository,
                                  EtterlysningRepository etterlysningRepository,
                                  ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderBostedDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        Map<java.time.LocalDate, Boolean> avklaringerPerSkjæringstidspunkt = new LinkedHashMap<>();
        for (BostedAvklaringPeriodeDto avklaring : dto.getAvklaringer()) {
            avklaringerPerSkjæringstidspunkt.put(avklaring.getPeriode().getFom(), avklaring.getErBosattITrondheim());
        }

        UUID grunnlagsreferanse = bostedsGrunnlagRepository.lagreAvklaringer(behandlingId, avklaringerPerSkjæringstidspunkt);

        // Avbryt eksisterende OPPRETTET-etterlysninger for UTTALELSE_BOSTED (ved re-vurdering av grunnlag)
        var eksisterendeOpprettede = etterlysningRepository.hentOpprettetEtterlysninger(behandlingId, EtterlysningType.UTTALELSE_BOSTED);
        eksisterendeOpprettede.forEach(Etterlysning::avbryt);
        etterlysningRepository.lagre(eksisterendeOpprettede);

        // Opprett én etterlysning per avklaringsperiode
        for (BostedAvklaringPeriodeDto avklaring : dto.getAvklaringer()) {
            var etterlysning = Etterlysning.opprettForType(
                behandlingId,
                grunnlagsreferanse,
                UUID.randomUUID(),
                DatoIntervallEntitet.fraOgMedTilOgMed(avklaring.getPeriode().getFom(), avklaring.getPeriode().getTom()),
                EtterlysningType.UTTALELSE_BOSTED
            );
            etterlysningRepository.lagre(etterlysning);
        }

        // Planlegg task for å sende varsel til bruker og sette etterlysning til VENTER
        var task = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        task.setBehandling(behandling.getFagsakId(), behandlingId);
        task.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, EtterlysningType.UTTALELSE_BOSTED.getKode());
        prosessTaskTjeneste.lagre(task);

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.BOSTEDSVILKÅR)
            .addLinje("Bostedsavklaring registrert – bruker varsles")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        var resultat = OppdateringResultat.nyttResultat();
        resultat.rekjørSteg();
        return resultat;
    }

}
