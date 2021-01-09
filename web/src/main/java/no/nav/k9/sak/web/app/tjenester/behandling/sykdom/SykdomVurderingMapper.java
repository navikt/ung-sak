package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomPerson;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurdering;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;


@ApplicationScoped
public class SykdomVurderingMapper {

    /**
     * Mapper angitte versjoner til SykdomVurderingDto.
     * 
     * @param versjoner Versjonene som skal tas med i DTOen.
     * @return En SykdomVurderingDto der kun angitte versjoner har blitt tatt med.
     */
    public SykdomVurderingDto map(List<SykdomVurderingVersjon> versjoner) {
        final SykdomVurdering vurdering = versjoner.get(0).getSykdomVurdering();
        
        if (versjoner.stream().anyMatch(v -> v.getSykdomVurdering() != vurdering)) {
            throw new IllegalArgumentException("Utviklerfeil: Alle SykdomVurderingVersjon i parameteren 'versjoner' må tilhøre samme SykdomVurdering.");
        }
        
        var versjonerDto = versjoner.stream()
            .sorted(Collections.reverseOrder())
            .map(v -> new SykdomVurderingVersjonDto("" + v.getVersjon(),
                    v.getTekst(),
                    v.getResultat(),
                    mapPerioder(v.getPerioder()),
                    mapDokumenter(v.getDokumenter()),
                    v.getEndretAv(),
                    v.getEndretTidspunkt())
            )
            .collect(Collectors.toList());

        return new SykdomVurderingDto(
                "" + vurdering.getId(),
                vurdering.getType(), 
                versjonerDto,
                new SykdomVurderingAnnenInformasjon(
                    Arrays.asList(new Periode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(3))), // TODO: Implementer
                    Arrays.asList(new Periode(LocalDate.now().minusDays(10), LocalDate.now())) // TODO: Implementer
                )
            );
    }

    
    private List<Periode> mapPerioder(List<SykdomVurderingPeriode> perioder) {
        return perioder.stream().map(p -> new Periode(p.getFom(), p.getTom())).collect(Collectors.toList());
    }
    
    private List<SykdomDokumentDto> mapDokumenter(List<SykdomDokument> dokumenter) {
        return dokumenter.stream().map(d -> new SykdomDokumentDto(
                    "" + d.getId(),
                    SykdomDokumentType.MEDISINSKE_OPPLYSNINGER, // TODO: Legg til støtte for dokumenttyper.
                    true, // TODO: Riktig med true her, men vi må også legge til alle dokumentene som finnes.
                    true, // TODO: AnnenPartErKilde
                    LocalDate.now(), // TODO: Må få inn dateringsdato
                    true // TODO: Må finne ut om dokumentet er fremhevet (nytt dokument i behandlingen.
                )).collect(Collectors.toList());
    }
    
    public SykdomVurderingVersjon map(SykdomVurdering sykdomVurdering, SykdomVurderingEndringDto oppdatering, Sporingsinformasjon sporingsinformasjon, List<SykdomDokument> alleDokumenter) {
        if (sykdomVurdering.getSisteVersjon().getVersjon() != Long.parseLong(oppdatering.getVersjon())) {
            throw new ConcurrentModificationException("Forsøk på å oppdatere SykdomVurdering på grunnlag av utdatert versjon.");
        }
        
        final LocalDateTime endretTidspunkt = LocalDateTime.now();
        
        final SykdomVurderingVersjon versjon = new SykdomVurderingVersjon(
                sykdomVurdering,
                oppdatering.getTekst(),
                oppdatering.getResultat(),
                Long.parseLong(oppdatering.getVersjon()) + 1L,
                sporingsinformasjon.endretAv,
                endretTidspunkt,
                sporingsinformasjon.endretBehandlingUuid,
                sporingsinformasjon.endretSaksnummer,
                sporingsinformasjon.endretForPerson,
                null,
                alleDokumenter.stream().filter(d -> oppdatering.getTilknyttedeDokumenter().contains("" + d.getId())).collect(Collectors.toList()),
                oppdatering.getPerioder()
        );

        return versjon;
    }
    
    public SykdomVurdering map(SykdomVurderingOpprettelseDto opprettelse, Sporingsinformasjon sporingsinformasjon, List<SykdomDokument> alleDokumenter) {
        final LocalDateTime endretTidspunkt = LocalDateTime.now();
        
        final SykdomVurdering sykdomVurdering = new SykdomVurdering(opprettelse.getType(), Collections.emptyList(), sporingsinformasjon.endretAv, LocalDateTime.now());
        final SykdomVurderingVersjon versjon = new SykdomVurderingVersjon(
                sykdomVurdering,
                opprettelse.getTekst(),
                opprettelse.getResultat(),
                1L,
                sporingsinformasjon.endretAv,
                endretTidspunkt,
                sporingsinformasjon.endretBehandlingUuid,
                sporingsinformasjon.endretSaksnummer,
                sporingsinformasjon.endretForPerson,
                null,
                alleDokumenter.stream().filter(d -> opprettelse.getTilknyttedeDokumenter().contains("" + d.getId())).collect(Collectors.toList()),
                opprettelse.getPerioder()
        );
        sykdomVurdering.addVersjon(versjon);

        return sykdomVurdering;
    }
    
    public final static class Sporingsinformasjon {
        private String endretAv;
        private UUID endretBehandlingUuid;
        private String endretSaksnummer;
        private SykdomPerson endretForPerson;
        
        public Sporingsinformasjon(String endretAv, UUID endretBehandlingUuid, String endretSaksnummer, SykdomPerson endretForPerson) {
            this.endretAv = endretAv;
            this.endretBehandlingUuid = endretBehandlingUuid;
            this.endretSaksnummer = endretSaksnummer;
            this.endretForPerson = endretForPerson;
        }
        
    }
}
