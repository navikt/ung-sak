package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
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
import no.nav.k9.søknad.felles.aktivitet.Organisasjonsnummer;
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
                .flatMap(sn -> this.mapEgenNæring(sn).stream())
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

    private List<EgenNæringBuilder> mapEgenNæring(no.nav.k9.søknad.felles.aktivitet.SelvstendigNæringsdrivende sn) {
        if (sn.perioder.size() != 1) {
            throw new IllegalArgumentException("Må ha eksakt en periode. Størrelse var " + sn.perioder.size());
        }
        var entry = sn.perioder.entrySet().iterator().next();
        var info = entry.getValue();
        if (info.getVirksomhetstyper().isEmpty()) {
            throw new IllegalArgumentException("Må ha minst en virksomhetstype.");
        }
        var periode = entry.getKey();
        var orgnummer = sn.getOrganisasjonsnummer();
        // Mapper en egen næring pr virksomhetstype
        return info.getVirksomhetstyper().stream()
            .map(type -> this.mapNæringForVirksomhetType(periode, info, type, orgnummer))
            .collect(Collectors.toList());
    }

    private EgenNæringBuilder mapNæringForVirksomhetType(no.nav.k9.søknad.felles.type.Periode periode,
                                                         no.nav.k9.søknad.felles.aktivitet.SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo info,
                                                         no.nav.k9.søknad.felles.aktivitet.VirksomhetType type,
                                                         Organisasjonsnummer organisasjonsnummer) {
        var builder = EgenNæringBuilder.ny();
        builder.medVirksomhet(organisasjonsnummer != null ? new OrgNummer(organisasjonsnummer.verdi) : null);
        builder.medPeriode(periode.getTilOgMed() != null
            ? DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed())
            : DatoIntervallEntitet.fraOgMed(periode.getFraOgMed()));
        builder.medBruttoInntekt(info.bruttoInntekt);
        builder.medVirksomhetType(VirksomhetType.fraKode(type.getKode()));
        builder.medRegnskapsførerNavn(info.regnskapsførerNavn);
        builder.medRegnskapsførerTlf(info.regnskapsførerTlf);
        builder.medVarigEndring(info.erVarigEndring);
        builder.medNyoppstartet(info.erNyoppstartet);
        // TODO Map ny i arbeidslivet
        return builder;
    }
}
