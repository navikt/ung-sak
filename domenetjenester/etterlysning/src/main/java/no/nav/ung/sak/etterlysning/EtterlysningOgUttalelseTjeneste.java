package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;

import java.util.*;
import java.util.stream.Collectors;

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
        // TODO: Hent ut etterlysninger og uttalelser og map til EtterlysningData
        List<Etterlysning> etterlysninger = etterlysningRepository.hentEtterlysninger(behandlingId, typer);
        List<UttalelseV2> uttalelser = uttalelseRepository.hentUttalelser(behandlingId, Arrays.stream(typer).map(this::mapTilEndringsType).toArray(EndringType[]::new));

        List<EtterlysningData> etterlysningerOgUttalelser =
            etterlysninger.stream()
                .map(e -> {
                    EndringType mappedType = mapTilEndringsType(e.getType());
                    return uttalelser.stream()
                        .filter(u -> u.getType() == mappedType)
                        .filter(u -> Objects.equals(u.getPeriode(), e.getPeriode()))
                        .filter(u -> Objects.equals(u.getGrunnlagsreferanse(), e.getGrunnlagsreferanse()))
                        .findFirst()
                        .map(u -> new EtterlysningData(
                            e.getStatus(),
                            e.getFrist(),
                            e.getGrunnlagsreferanse(),
                            e.getPeriode(),
                            e.getOpprettetTidspunkt(),
                            new UttalelseData(
                                u.harUttalelse(),
                                u.getUttalelseBegrunnelse(),
                                u.getSvarJournalpostId()
                            )
                        ))
                        .orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();

        return etterlysningerOgUttalelser;
    }

    private EndringType mapTilEndringsType(EtterlysningType etterlysningType) {
        return switch (etterlysningType) {
            case UTTALELSE_KONTROLL_INNTEKT -> EndringType.ENDRET_INNTEKT;
            case UTTALELSE_ENDRET_STARTDATO -> EndringType.ENDRET_STARTDATO;
            case UTTALELSE_ENDRET_SLUTTDATO -> EndringType.ENDRET_SLUTTDATO;
        };
    }
}
