package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EtterlysningForEndretProgramperiodeResultatHåndterer {

    private EtterlysningRepository etterlysningRepository;

    public EtterlysningForEndretProgramperiodeResultatHåndterer() {
    }

    @Inject
    public EtterlysningForEndretProgramperiodeResultatHåndterer(EtterlysningRepository etterlysningRepository) {
        this.etterlysningRepository = etterlysningRepository;
    }

    /** Håndterer utledet behov for etterlysning ved å opprette nye etterlysninger og avbryte eksisterende etterlysninger dersom det er aktuelt.
     * Etterlysninger settes her til OPPRETTET eller SKAL_AVBRYTES og det opprettes så tasker som håndterer endring av status i andre systemer.
     * @param resultat Utledet behov for nye etterlysninger
     * @param behandlingReferanse Behandlingref
     * @param etterlysningType Type etterlysning som skal opprettes eller erstattes
     * @param gjeldendeEtterlysning Gjeldende etterlysning som skal erstattes, hvis det er aktuelt
     * @param gjeldendeGrunnlag Gjeldende grunnlag for programperioden som etterlysningen gjelder for
     */
    void håndterResultat(ResultatType resultat, BehandlingReferanse behandlingReferanse,
                         EtterlysningType etterlysningType,
                         Optional<Etterlysning> gjeldendeEtterlysning,
                         UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag) {
        switch (resultat) {
            case OPPRETT_ETTERLYSNING ->
                opprettNyEtterlysning(gjeldendeGrunnlag, behandlingReferanse.getBehandlingId(), etterlysningType);
            case ERSTATT_EKSISTERENDE_ETTERLYSNING ->
                erstattEksisterende(behandlingReferanse, etterlysningType, gjeldendeEtterlysning.orElseThrow(() -> new IllegalStateException("Forventer å finne gjeldende etterlysning")), gjeldendeGrunnlag);
            case INGEN_ENDRING -> {
                // Ingen handling nødvendig, behold eksisterende etterlysning
            }
        }
    }

    private void erstattEksisterende(BehandlingReferanse behandlingReferanse,
                                     EtterlysningType etterlysningType,
                                     Etterlysning gjeldendeEtterlysning,
                                     UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag) {
        gjeldendeEtterlysning.skalAvbrytes();

        var nyEtterlysning = Etterlysning.opprettForType(
            behandlingReferanse.getBehandlingId(),
            gjeldendeGrunnlag.getGrunnlagsreferanse(),
            UUID.randomUUID(),
            gjeldendeGrunnlag.hentForEksaktEnPeriode(),
            etterlysningType
        );

        etterlysningRepository.lagre(List.of(gjeldendeEtterlysning, nyEtterlysning));
    }

    private void opprettNyEtterlysning(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, Long behandlingId, EtterlysningType etterlysningType) {
        var gjeldendePeriode = gjeldendePeriodeGrunnlag.hentForEksaktEnPeriode();
        final var nyEtterlysning = Etterlysning.opprettForType(
            behandlingId,
            gjeldendePeriodeGrunnlag.getGrunnlagsreferanse(),
            UUID.randomUUID(),
            gjeldendePeriode,
            etterlysningType
        );
        etterlysningRepository.lagre(nyEtterlysning);
    }
}
