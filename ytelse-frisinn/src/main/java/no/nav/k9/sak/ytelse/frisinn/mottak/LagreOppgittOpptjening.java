package no.nav.k9.sak.ytelse.frisinn.mottak;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask;
import no.nav.k9.sak.mottak.dokumentmottak.OppgittOpptjeningMapper;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.frisinn.Inntekter;
import no.nav.k9.søknad.frisinn.PeriodeInntekt;
import no.nav.k9.søknad.frisinn.SelvstendigNæringsdrivende;

@Dependent
class LagreOppgittOpptjening {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BehandlingRepository behandlingRepository;
    private OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste;
    private ProsessTaskTjeneste prosessTaskRepository;

    LagreOppgittOpptjening() {
        // for proxy
    }

    @Inject
    LagreOppgittOpptjening(BehandlingRepository behandlingRepository,
                           InntektArbeidYtelseTjeneste iayTjeneste,
                           OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste,
                           ProsessTaskTjeneste prosessTaskRepository) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.oppgittOpptjeningMapperTjeneste = oppgittOpptjeningMapperTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    void lagreOpptjening(Behandling behandling, Inntekter inntekter, ZonedDateTime tidspunkt) {
        OppgittOpptjeningBuilder opptjeningBuilder = initOpptjeningBuilder(behandling.getFagsakId(), tidspunkt);

        boolean erNyeOpplysninger = false;
        if (inntekter.getFrilanser() != null) {
            var fri = inntekter.getFrilanser();
            List<OppgittFrilansoppdrag> oppdrag = fri.getInntekterSøknadsperiode().entrySet().stream().map(entry -> {
                OppgittFrilansOppdragBuilder builder = OppgittFrilansOppdragBuilder.ny();
                return builder
                        .medInntekt(entry.getValue().getBeløp())
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(entry.getKey().getFraOgMed(), entry.getKey().getTilOgMed()))
                        .build();
            }).collect(Collectors.toList());

            OppgittFrilansBuilder frilansBuilder = opptjeningBuilder.getFrilansBuilder();
            OppgittFrilans oppgittFrilans = frilansBuilder
                    .medFrilansOppdrag(oppdrag)
                    .build();
            opptjeningBuilder.leggTilFrilansOpplysninger(oppgittFrilans);
            erNyeOpplysninger = true;
        }

        if (inntekter.getSelvstendig() != null) {
            var selv = inntekter.getSelvstendig();

            // slår sammen historiske og løpende inntekter her.  Bruker stp senere til håndtere før/etter inntektstap startet.

            var egenNæringFør = selv.getInntekterFør().entrySet().stream().map(e -> mapEgenNæring(selv, e)).collect(Collectors.toList());
            opptjeningBuilder.leggTilEgneNæringer(egenNæringFør);

            var egenNæringSøknadsperiode = selv.getInntekterSøknadsperiode().entrySet().stream().map(e -> mapEgenNæring(selv, e)).collect(Collectors.toList());
            opptjeningBuilder.leggTilEgneNæringer(egenNæringSøknadsperiode);

            erNyeOpplysninger |= !egenNæringFør.isEmpty() || !egenNæringSøknadsperiode.isEmpty();
        }

        if (inntekter.getArbeidstaker() != null) {
            var arbeidstaker = inntekter.getArbeidstaker();
            arbeidstaker.getInntekterSøknadsperiode().entrySet()
                    .stream()
                    .map(this::mapArbeidsforhold)
                    .forEach(opptjeningBuilder::leggTilOppgittArbeidsforhold);
        }

        if (erNyeOpplysninger) {
            try {
                var enkeltTask = ProsessTaskData.forProsessTask(AsyncAbakusLagreOpptjeningTask.class);
                enkeltTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getAktørId());
                enkeltTask.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
                enkeltTask.setCallIdFraEksisterende();

                var request = oppgittOpptjeningMapperTjeneste.byggRequest(behandling, opptjeningBuilder);
                var payload = IayGrunnlagJsonMapper.getMapper().writeValueAsString(request);
                enkeltTask.setPayload(payload);

                prosessTaskRepository.lagre(enkeltTask);
            } catch (IOException e) {
                throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Opprettelse av task for lagring av oppgitt opptjening i abakus feiler.", e).toException();
            }
        }
    }

    private OppgittOpptjeningBuilder initOpptjeningBuilder(Long fagsakId, ZonedDateTime tidspunkt) {
        OppgittOpptjeningBuilder builder = OppgittOpptjeningBuilder.ny(UUID.randomUUID(), tidspunkt.toLocalDateTime());

        // bygg på eksisterende hvis tidligere innrapportert for denne ytelsen (sikrer at vi får med originalt rapportert inntektsgrunnlag).
        // TODO: håndtere korreksjoner senere?  vil nå bare akkumulere innrapportert.
        var sisteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        if (sisteBehandling.isPresent()) {
            Optional<InntektArbeidYtelseGrunnlag> iayGrunnlagOpt = sisteBehandling.isPresent() ? iayTjeneste.finnGrunnlag(sisteBehandling.get().getId()) : Optional.empty();
            if (iayGrunnlagOpt.isPresent()) {
                var tidligereRegistrertOpptjening = iayGrunnlagOpt.get().getOppgittOpptjening();
                if (tidligereRegistrertOpptjening.isPresent()) {
                    builder = OppgittOpptjeningBuilder.nyFraEksisterende(tidligereRegistrertOpptjening.get(), UUID.randomUUID(), tidspunkt.toLocalDateTime());
                }
            }
        }
        return builder;
    }

    private EgenNæringBuilder mapEgenNæring(SelvstendigNæringsdrivende selvstendig, Map.Entry<Periode, PeriodeInntekt> entry) {
        Periode periode = entry.getKey();
        PeriodeInntekt inntekt = entry.getValue();

        var builder = EgenNæringBuilder.ny();
        builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()));
        builder.medBruttoInntekt(inntekt.getBeløp());
        builder.medVirksomhetType(VirksomhetType.ANNEN);
        builder.medRegnskapsførerNavn(selvstendig.getRegnskapsførerNavn());
        builder.medRegnskapsførerTlf(selvstendig.getRegnskapsførerTlf());
        return builder;
    }

    private OppgittArbeidsforholdBuilder mapArbeidsforhold(Entry<Periode, PeriodeInntekt> entry) {
        Periode periode = entry.getKey();
        PeriodeInntekt inntekt = entry.getValue();

        OppgittArbeidsforholdBuilder builder = OppgittArbeidsforholdBuilder.ny();
        builder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()))
                .medInntekt(inntekt.getBeløp());

        return builder;
    }
}
