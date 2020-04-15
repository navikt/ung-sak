package no.nav.k9.sak.ytelse.frisinn.mottak;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.organisasjon.VirksomhetType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.søknad.felles.Periode;
import no.nav.k9.søknad.frisinn.Inntekter;
import no.nav.k9.søknad.frisinn.PeriodeInntekt;

@Dependent
class LagreOppgittOpptjening {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BehandlingRepository behandlingRepository;

    LagreOppgittOpptjening() {
        // for proxy
    }

    @Inject
    LagreOppgittOpptjening(BehandlingRepository behandlingRepository,
                           InntektArbeidYtelseTjeneste iayTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
    }

    void lagreOpptjening(Behandling behandling, Inntekter inntekter, ZonedDateTime tidspunkt) {

        Long behandlingId = behandling.getId();

        OppgittOpptjeningBuilder opptjeningBuilder = initOpptjeningBuilder(behandling.getFagsakId(), tidspunkt);

        boolean erNyeOpplysninger = false;
        if (inntekter.getFrilanser() != null) {
            var fri = inntekter.getFrilanser();
            List<OppgittFrilansoppdrag> oppdrag = fri.getInntekterSøknadsperiode().entrySet().stream().map(entry -> {
                OppgittFrilansOppdragBuilder builder = OppgittFrilansOppdragBuilder.ny();
                return builder.medInntekt(entry.getValue().getBeløp())
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(entry.getKey().getFraOgMed(), entry.getKey().getTilOgMed()))
                        .build();
            }).collect(Collectors.toList());

            OppgittFrilansBuilder frilansBuilder = OppgittFrilansBuilder.ny();
            OppgittFrilans oppgittFrilans = frilansBuilder
                    .medErNyoppstartet(false)
                    .leggTilOppgittOppdrag(oppdrag)
                    .build();
            opptjeningBuilder.leggTilFrilansOpplysninger(oppgittFrilans);
            erNyeOpplysninger = true;
        }

        if (inntekter.getSelvstendig() != null) {
            var selv = inntekter.getSelvstendig();

            // slår sammen historiske og løpende inntekter her.  Bruker stp senere til håndtere før/etter inntektstap startet.

            var egenNæringFør = selv.getInntekterFør().entrySet().stream().map(this::mapEgenNæring).collect(Collectors.toList());
            opptjeningBuilder.leggTilEgneNæringer(egenNæringFør);

            var egenNæringSøknadsperiode = selv.getInntekterSøknadsperiode().entrySet().stream().map(this::mapEgenNæring).collect(Collectors.toList());
            opptjeningBuilder.leggTilEgneNæringer(egenNæringSøknadsperiode);

            erNyeOpplysninger |= !egenNæringFør.isEmpty() || !egenNæringSøknadsperiode.isEmpty();
        }

        if (erNyeOpplysninger) {
            // FIXME K9: håndter lagring i egen task så det blir robust kall til abakus
            iayTjeneste.lagreOppgittOpptjening(behandlingId, opptjeningBuilder);
        }
    }

    private OppgittOpptjeningBuilder initOpptjeningBuilder(Long fagsakId, ZonedDateTime tidspunkt) {
        OppgittOpptjeningBuilder builder = OppgittOpptjeningBuilder.ny(UUID.randomUUID(), tidspunkt.toLocalDateTime());

        // bygg på eksisterende hvis tidligere innrapportert for denne ytelsen (sikrer at vi får med originalt rapportert inntektsgrunnlag).
        // TODO: håndtere korreksjoner senere?  vil nå bare akkumulere innrappotert.
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

    private EgenNæringBuilder mapEgenNæring(Map.Entry<Periode, PeriodeInntekt> entry) {
        Periode periode = entry.getKey();
        PeriodeInntekt inntekt = entry.getValue();

        var builder = EgenNæringBuilder.ny();
        builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()));
        builder.medBruttoInntekt(inntekt.getBeløp());
        builder.medVirksomhetType(VirksomhetType.ENKELTPERSONFORETAK);
        return builder;
    }
}
