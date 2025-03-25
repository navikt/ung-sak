package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.JsonObjectMapper;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.k9.oppgave.bekreftelse.Bekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandling.prosessering.task.FortsettBehandlingTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.ung.sak.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Dependent
@OppgaveTypeRef(Bekreftelse.Type.UNG_AVVIK_REGISTERINNTEKT)
public class InntektBekreftelseHåndterer implements BekreftelseHåndterer {


    private final EtterlysningRepository etterlysningRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public InntektBekreftelseHåndterer(EtterlysningRepository etterlysningRepository, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void håndter(OppgaveBekreftelseInnhold oppgaveBekreftelseInnhold) {
        InntektBekreftelse inntektBekreftelse = oppgaveBekreftelseInnhold.oppgaveBekreftelse().getBekreftelse();

        Etterlysning etterlysning = etterlysningRepository.hentEtterlysningForEksternReferanse(inntektBekreftelse.getOppgaveId());

        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();

        if (inntektBekreftelse.harBrukerGodtattEndringen()) {
            Objects.requireNonNull(inntektBekreftelse.getUttalelseFraBruker(),
                "Uttalelsestekst fra bruker må være satt når bruker ikke har godtatt endringen");
            var abakusTask = lagOppdaterAbakusTask(oppgaveBekreftelseInnhold);
            gruppe.addNesteSekvensiell(abakusTask);
        }

        etterlysning.mottattUttalelse(
            oppgaveBekreftelseInnhold.journalpostId(),
            inntektBekreftelse.harBrukerGodtattEndringen(),
            inntektBekreftelse.getUttalelseFraBruker()
        );

        etterlysningRepository.lagre(etterlysning);
        prosessTaskTjeneste.lagre(gruppe);
    }

    /**
     * Lagrer oppgitt opptjening til abakus fra mottatt bekreftelse.
     *
     */
    private static ProsessTaskData lagOppdaterAbakusTask(OppgaveBekreftelseInnhold bekreftelseInnhold) {
        var request = mapOppgittOpptjeningRequest(bekreftelseInnhold);

        try {
            var behandling = bekreftelseInnhold.behandling();
            var enkeltTask = ProsessTaskData.forProsessTask(AsyncAbakusLagreOpptjeningTask.class);
            var payload = JsonObjectMapper.getMapper().writeValueAsString(request);
            enkeltTask.setPayload(payload);

            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.JOURNALPOST_ID, bekreftelseInnhold.journalpostId().getJournalpostId().getVerdi());
            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.BREVKODER, bekreftelseInnhold.brevkode().getKode());

            enkeltTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getAktørId());
            enkeltTask.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
            enkeltTask.setCallIdFraEksisterende();
            return enkeltTask;


        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Opprettelse av task for lagring av oppgitt opptjening i abakus feiler.", e).toException();
        }
    }

    private static OppgittOpptjeningMottattRequest mapOppgittOpptjeningRequest(OppgaveBekreftelseInnhold bekreftelse) {
        InntektBekreftelse inntektBekreftelse = bekreftelse.oppgaveBekreftelse().getBekreftelse();
        var oppgittArbeidOgFrilans = inntektBekreftelse.getOppgittePeriodeinntekter()
            .stream()
            .filter(it -> it.getArbeidstakerOgFrilansInntekt() != null)
            .map(inntekter -> OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
                .medInntekt(inntekter.getArbeidstakerOgFrilansInntekt())
                .medArbeidType(ArbeidType.VANLIG)
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(inntekter.getPeriode().getFraOgMed(), inntekter.getPeriode().getTilOgMed())))
            .toList();

        if (oppgittArbeidOgFrilans.isEmpty()) {
            throw new IllegalStateException("Mangler arbeid og frilansinntekt");

        }

        var builder = OppgittOpptjeningBuilder.ny(UUID.randomUUID(), LocalDateTime.now());
        builder.leggTilOppgittArbeidsforhold(oppgittArbeidOgFrilans);
        builder.leggTilJournalpostId(bekreftelse.journalpostId().getJournalpostId());
        builder.leggTilInnsendingstidspunkt(bekreftelse.innsendingstidspunkt());
        Behandling behandlingReferanse = bekreftelse.behandling();
        var aktør = new AktørIdPersonident(behandlingReferanse.getAktørId().getId());
        var saksnummer = behandlingReferanse.getFagsak().getSaksnummer();
        var ytelseType = YtelseType.fraKode(behandlingReferanse.getFagsakYtelseType().getKode());
        var oppgittOpptjening = new IAYTilDtoMapper(behandlingReferanse.getAktørId(), null, behandlingReferanse.getUuid()).mapTilDto(builder);
        return new OppgittOpptjeningMottattRequest(saksnummer.getVerdi(), behandlingReferanse.getUuid(), aktør, ytelseType, oppgittOpptjening);
    }

    private static ProsessTaskData fortsettBehandlingTask(Behandling behandling) {
        var fortsettTask = ProsessTaskData.forProsessTask(FortsettBehandlingTask.class);
        fortsettTask.setBehandling(behandling.getFagsakId(), behandling.getId());
        fortsettTask.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        fortsettTask.setProperty(FortsettBehandlingTask.GJENOPPTA_STEG, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT.getKode());
        return fortsettTask;
    }

}
