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
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.ung.sak.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask;
import no.nav.ung.sak.mottak.dokumentmottak.Trigger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Dependent
@OppgaveTypeRef(Bekreftelse.Type.UNG_AVVIK_REGISTERINNTEKT)
public class InntektBekreftelseHåndterer implements BekreftelseHåndterer {


    private final EtterlysningRepository etterlysningRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private final MottatteDokumentRepository mottatteDokumentRepository;


    @Inject
    public InntektBekreftelseHåndterer(EtterlysningRepository etterlysningRepository,
                                       ProsessTaskTjeneste prosessTaskTjeneste,
                                       MottatteDokumentRepository mottatteDokumentRepository) {
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @Override
    public void håndter(OppgaveBekreftelseInnhold oppgaveBekreftelseInnhold) {
        InntektBekreftelse inntektBekreftelse = oppgaveBekreftelseInnhold.oppgaveBekreftelse().getBekreftelse();

        Etterlysning etterlysning = etterlysningRepository.hentEtterlysningForEksternReferanse(inntektBekreftelse.getOppgaveReferanse());

        if (!etterlysning.getStatus().equals(EtterlysningStatus.VENTER)) {
            throw new IllegalStateException("Etterlysning må hå status VENTER for å motta bekreftelse. Status var " + etterlysning.getStatus());
        }

        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();

        if (inntektBekreftelse.harBrukerGodtattEndringen()) {
            var abakusTask = lagOppdaterAbakusTask(oppgaveBekreftelseInnhold);
            gruppe.addNesteSekvensiell(abakusTask);
        } else {
            mottatteDokumentRepository.oppdaterStatus(List.of(oppgaveBekreftelseInnhold.mottattDokument()), DokumentStatus.GYLDIG);
            Objects.requireNonNull(inntektBekreftelse.getUttalelseFraBruker(),
                "Uttalelsestekst fra bruker må være satt når bruker ikke har godtatt endringen");
        }

        etterlysning.mottattUttalelse(
            oppgaveBekreftelseInnhold.mottattDokument().getJournalpostId(),
            inntektBekreftelse.harBrukerGodtattEndringen(),
            inntektBekreftelse.getUttalelseFraBruker()
        );

        etterlysningRepository.lagre(etterlysning);
        prosessTaskTjeneste.lagre(gruppe);
    }

    @Override
    public Optional<Trigger> utledTrigger(UUID oppgaveId) {
        Etterlysning etterlysning = etterlysningRepository.hentEtterlysningForEksternReferanse(oppgaveId);

        return Optional.of(new Trigger(etterlysning.getPeriode(), BehandlingÅrsakType.UTTALELSE_FRA_BRUKER));
    }

    /**
     * Lagrer oppgitt opptjening til abakus fra mottatt bekreftelse.
     */
    private static ProsessTaskData lagOppdaterAbakusTask(OppgaveBekreftelseInnhold bekreftelseInnhold) {
        var request = mapOppgittOpptjeningRequest(bekreftelseInnhold);

        try {
            var behandling = bekreftelseInnhold.behandling();
            var enkeltTask = ProsessTaskData.forProsessTask(AsyncAbakusLagreOpptjeningTask.class);
            var payload = JsonObjectMapper.getMapper().writeValueAsString(request);
            enkeltTask.setPayload(payload);

            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.JOURNALPOST_ID, bekreftelseInnhold.mottattDokument().getJournalpostId().getVerdi());
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
        builder.leggTilJournalpostId(bekreftelse.mottattDokument().getJournalpostId());
        builder.leggTilInnsendingstidspunkt(bekreftelse.mottattDokument().getInnsendingstidspunkt());
        Behandling behandlingReferanse = bekreftelse.behandling();
        var aktør = new AktørIdPersonident(behandlingReferanse.getAktørId().getId());
        var saksnummer = behandlingReferanse.getFagsak().getSaksnummer();
        var ytelseType = YtelseType.fraKode(behandlingReferanse.getFagsakYtelseType().getKode());
        var oppgittOpptjening = new IAYTilDtoMapper(behandlingReferanse.getAktørId(), null, behandlingReferanse.getUuid()).mapTilDto(builder);
        return new OppgittOpptjeningMottattRequest(saksnummer.getVerdi(), behandlingReferanse.getUuid(), aktør, ytelseType, oppgittOpptjening);
    }

}
