package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;

@Dependent
public class LagreOppgittOpptjening {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    LagreOppgittOpptjening(BehandlingRepository behandlingRepository,
                           InntektArbeidYtelseTjeneste iayTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
    }


    public void lagreOpptjening(Behandling behandling, ZonedDateTime tidspunkt, OmsorgspengerUtbetaling søknad) {

        Long behandlingId = behandling.getId();
        OppgittOpptjeningBuilderOgStatus builderOgStatus = initOpptjeningBuilder(behandling, tidspunkt);

        if (søknad.getAktivitet().getArbeidstaker() != null) {
            // TODO: arbeidstaker
            throw new UnsupportedOperationException("Støtter ikke arbeidstaker for OMS");
        }

        if (søknad.getAktivitet().getSelvstendigNæringsdrivende() != null) {
            var snAktiviteter = søknad.getAktivitet().getSelvstendigNæringsdrivende();
            var egenNæringBuilders = snAktiviteter.stream()
                .map(this::mapEgenNæring)
                .collect(Collectors.toList());
            builderOgStatus.builder.leggTilEgneNæringer(egenNæringBuilders);
        }
        if (søknad.getAktivitet().getFrilanser() != null) {
            builderOgStatus.builder.leggTilFrilansOpplysninger(OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny()
                .build());
        }

        if (builderOgStatus.builder.build().harOpptjening() || !builderOgStatus.erNyopprettet) {
            iayTjeneste.lagreOppgittOpptjening(behandlingId, builderOgStatus.builder);
        }
    }

    static class OppgittOpptjeningBuilderOgStatus {
        private OppgittOpptjeningBuilder builder;
        private boolean erNyopprettet;

        private OppgittOpptjeningBuilderOgStatus(OppgittOpptjeningBuilder builder, boolean erNyopprettet) {
            this.builder = builder;
            this.erNyopprettet = erNyopprettet;
        }

        static OppgittOpptjeningBuilderOgStatus ny(OppgittOpptjeningBuilder builder) {
            return new OppgittOpptjeningBuilderOgStatus(builder, true);
        }

        static OppgittOpptjeningBuilderOgStatus eksisterende(OppgittOpptjeningBuilder builder) {
            return new OppgittOpptjeningBuilderOgStatus(builder, false);
        }

    }

    private OppgittOpptjeningBuilderOgStatus initOpptjeningBuilder(Behandling behandling, ZonedDateTime tidspunkt) {
        //oppdater hvis det allerede finnes oppgitt opptjening på behandlingen
        Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag = iayTjeneste.finnGrunnlag(behandling.getId());
        if (iayGrunnlag.isPresent() && iayGrunnlag.get().getOppgittOpptjening().isPresent()) {
            OppgittOpptjening tidligerOppgittOpptjening = iayGrunnlag.get().getOppgittOpptjening().get();
            OppgittOpptjeningBuilder builder = OppgittOpptjeningBuilder.oppdater(behandling.getUuid(), tidligerOppgittOpptjening.getOpprettetTidspunkt().atOffset(ZoneOffset.UTC));
            return OppgittOpptjeningBuilderOgStatus.eksisterende(builder);
        }

        // bygg på eksisterende hvis tidligere innrapportert for denne ytelsen (sikrer at vi får med originalt rapportert inntektsgrunnlag).
        var forrigeBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());
        if (forrigeBehandling.isPresent()) {
            Optional<InntektArbeidYtelseGrunnlag> iayGrunnlagOpt = iayTjeneste.finnGrunnlag(forrigeBehandling.get().getId());
            if (iayGrunnlagOpt.isPresent() && iayGrunnlagOpt.get().getOppgittOpptjening().isPresent()) {
                OppgittOpptjening tidligereOppgittOpptjening = iayGrunnlagOpt.get().getOppgittOpptjening().get();
                OppgittOpptjeningBuilder builder = OppgittOpptjeningBuilder.nyFraEksisterende(tidligereOppgittOpptjening, UUID.randomUUID(), tidspunkt.toLocalDateTime());
                return OppgittOpptjeningBuilderOgStatus.eksisterende(builder);
            }
        }
        return OppgittOpptjeningBuilderOgStatus.ny(OppgittOpptjeningBuilder.ny(UUID.randomUUID(), tidspunkt.toLocalDateTime()));
    }

    private EgenNæringBuilder mapEgenNæring(no.nav.k9.søknad.felles.aktivitet.SelvstendigNæringsdrivende sn) {
        var builder = EgenNæringBuilder.ny();
        sn.perioder.forEach((per, info) -> {
            builder.medVirksomhet(sn.getOrganisasjonsnummer() != null ? new OrgNummer(sn.getOrganisasjonsnummer().verdi) : null);
            builder.medPeriode(per.getTilOgMed() != null
                ? DatoIntervallEntitet.fraOgMedTilOgMed(per.getFraOgMed(), per.getTilOgMed())
                : DatoIntervallEntitet.fraOgMed(per.getFraOgMed()));
            //builder.medBruttoInntekt(info.bruttoInntekt);
            builder.medVirksomhetType(VirksomhetType.ANNEN);
            builder.medRegnskapsførerNavn(info.regnskapsførerNavn);
            builder.medRegnskapsførerTlf(info.regnskapsførerTlf);
        });
        return builder;
    }

}
