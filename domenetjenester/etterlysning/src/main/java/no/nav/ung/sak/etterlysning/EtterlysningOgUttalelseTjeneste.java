package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;

import java.util.*;

@ApplicationScoped
public class EtterlysningOgUttalelseTjeneste {

    private EtterlysningRepository etterlysningRepository;
    private UttalelseRepository uttalelseRepository;

    public EtterlysningOgUttalelseTjeneste() {
    }

    @Inject
    public EtterlysningOgUttalelseTjeneste(EtterlysningRepository etterlysningRepository, UttalelseRepository uttalelseRepository) {
        this.etterlysningRepository = etterlysningRepository;
        this.uttalelseRepository = uttalelseRepository;
    }


    public List<EtterlysningData> hentEtterlysningerOgUttalelser(Long behandlingId, EtterlysningType... typer) {
        // Hent ut etterlysninger og uttalelser og map til EtterlysningData
        List<Etterlysning> etterlysninger = etterlysningRepository.hentEtterlysninger(behandlingId, typer);
        List<UttalelseV2> uttalelser = uttalelseRepository.hentUttalelser(behandlingId, Arrays.stream(typer).map(this::mapTilEndringsType).toArray(EndringType[]::new));

        return etterlysninger.stream()
            .map(e -> {
                EndringType mappedType = mapTilEndringsType(e.getType());
                UttalelseData uttalelseData = uttalelser.stream()
                    .filter(u -> samsvarerMedEtterlysning(e, mappedType, u))
                    .findFirst()
                    .map(u -> new UttalelseData(
                        u.harUttalelse(),
                        u.getUttalelseBegrunnelse(),
                        u.getSvarJournalpostId()
                    ))
                    .orElse(null);
                return new EtterlysningData(
                    e.getStatus(),
                    e.getFrist(),
                    e.getGrunnlagsreferanse(),
                    e.getPeriode(),
                    e.getOpprettetTidspunkt(),
                    uttalelseData
                );
            })
            .toList();
    }

    private boolean samsvarerMedEtterlysning(Etterlysning e, EndringType mappedType, UttalelseV2 u) {
        return u.getType() == mappedType
            && Objects.equals(u.getPeriode(), e.getPeriode())
            && Objects.equals(u.getGrunnlagsreferanse(), e.getGrunnlagsreferanse());
    }

    private EndringType mapTilEndringsType(EtterlysningType etterlysningType) {
        return switch (etterlysningType) {
            case UTTALELSE_KONTROLL_INNTEKT -> EndringType.ENDRET_INNTEKT;
            case UTTALELSE_ENDRET_STARTDATO -> EndringType.ENDRET_STARTDATO;
            case UTTALELSE_ENDRET_SLUTTDATO -> EndringType.ENDRET_SLUTTDATO;
        };
    }
}
