package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.søknad.felles.aktivitet.Arbeidstaker;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;

@Dependent
public class LagreOppgittOpptjening {

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


    public void lagreOpptjening(Behandling behandling, ZonedDateTime tidspunkt, OmsorgspengerUtbetaling søknad) {
        Long behandlingId = behandling.getId();
        OppgittOpptjeningBuilder opptjeningBuilder = initOpptjeningBuilder(behandling.getFagsakId(), tidspunkt);

        boolean erNyeOpplysninger = false;
        if (søknad.getAktivitet().getFrilanser() != null) {
            // TODO: Frilanser
            throw new UnsupportedOperationException("Støtter ikke frilanser for OMS");
        }

        if (søknad.getAktivitet().getSelvstendigNæringsdrivende() != null) {
            var snAktiviteter = søknad.getAktivitet().getSelvstendigNæringsdrivende();
            var egenNæringBuilders = snAktiviteter.stream()
                .map(akt -> mapEgenNæring(akt))
                .collect(Collectors.toList());
            opptjeningBuilder.leggTilEgneNæringer(egenNæringBuilders);

            erNyeOpplysninger |= !snAktiviteter.isEmpty();
        }

        if (søknad.getAktivitet().getArbeidstaker() != null) {
            List<Arbeidstaker> atAktiviteter = søknad.getAktivitet().getArbeidstaker();
            atAktiviteter.stream()
                .map(akt -> mapArbeidsforhold(akt))
                .forEach(opptjeningBuilder::leggTilOppgittArbeidsforhold);

            erNyeOpplysninger |= !atAktiviteter.isEmpty();
        }

        if (erNyeOpplysninger) {
            iayTjeneste.lagreOppgittOpptjening(behandlingId, opptjeningBuilder);
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

    private EgenNæringBuilder mapEgenNæring(no.nav.k9.søknad.felles.aktivitet.SelvstendigNæringsdrivende sn) {
        var builder = EgenNæringBuilder.ny();
        sn.perioder.forEach((per, info) -> {
            builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(per.getFraOgMed(), per.getTilOgMed()));
            builder.medBruttoInntekt(info.bruttoInntekt);
            builder.medVirksomhetType(VirksomhetType.ANNEN);
            builder.medRegnskapsførerNavn(info.regnskapsførerNavn);
            builder.medRegnskapsførerTlf(info.regnskapsførerTlf);
        });
        return builder;
    }

    private OppgittArbeidsforholdBuilder mapArbeidsforhold(no.nav.k9.søknad.felles.aktivitet.Arbeidstaker arbeidstaker) {
        var builder = OppgittArbeidsforholdBuilder.ny();
        arbeidstaker.perioder.forEach((per, info) -> {
            builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(per.getFraOgMed(), per.getTilOgMed()));
            builder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
            builder.medInntekt(BigDecimal.valueOf(10000)); // TODO: Hvor får vi denne fra? Skal kanskje bare kunne settes av Frisinn? Sjekk ut FP sin løsning. Gjelder bare utenlandske arbeidsforhold?
        });
        return builder;
    }
}
