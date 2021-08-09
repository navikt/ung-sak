package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.kontrakt.sykdom.SykdomPeriodeMedEndringDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingAnnenInformasjon;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingEndringDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingOpprettelseDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingVersjonDto;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument.SykdomDokumentOversiktMapper;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomPeriodeMedEndring;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomPerson;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurdering;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService.SykdomVurderingerOgPerioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;

@Dependent
public class SykdomVurderingMapper {

    private SykdomDokumentOversiktMapper dokumentMapper;


    @Inject
    public SykdomVurderingMapper(SykdomDokumentOversiktMapper dokumentMapper) {
        this.dokumentMapper = dokumentMapper;
    }


    /**
     * Mapper angitte versjoner til SykdomVurderingDto.
     *
     * @param versjoner Versjonene som skal tas med i DTOen.
     * @return En SykdomVurderingDto der kun angitte versjoner har blitt tatt med.
     */
    public SykdomVurderingDto map(AktørId aktørId, UUID behandlingUuid, List<SykdomVurderingVersjon> versjoner, List<SykdomDokument> alleDokumenter, SykdomVurderingerOgPerioder sykdomUtlededePerioder) {
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
                    mapDokumenter(aktørId, behandlingUuid, v.getDokumenter(), alleDokumenter),
                    v.getEndretAv(),
                    v.getEndretTidspunkt())
            )
            .collect(Collectors.toList());

        return new SykdomVurderingDto(
                "" + vurdering.getId(),
                vurdering.getType(),
                versjonerDto,
                new SykdomVurderingAnnenInformasjon(
                    sykdomUtlededePerioder.getResterendeVurderingsperioder(),
                    sykdomUtlededePerioder.getPerioderSomKanVurderes()
                )
            );
    }


    private List<SykdomDokumentDto> mapDokumenter(AktørId aktørId, UUID behandlingUuid, List<SykdomDokument> tilknyttedeDokumenter, List<SykdomDokument> alleDokumenter) {
        final Set<Long> ids = tilknyttedeDokumenter.stream().map(d -> d.getId()).collect(Collectors.toUnmodifiableSet());
        return dokumentMapper.mapSykdomsdokumenter(aktørId, behandlingUuid, alleDokumenter, ids);
    }


    private List<Periode> mapPerioder(List<SykdomVurderingPeriode> perioder) {
        return perioder.stream().map(p -> new Periode(p.getFom(), p.getTom())).collect(Collectors.toList());
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
        
        if (oppdatering.getTilknyttedeDokumenter().size() != versjon.getDokumenter().size()) {
            throw new IllegalStateException("Feil ved mapping: Klarte ikke å finne et av dokumentene saksbehandler har referert til.");
        }

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
        
        if (opprettelse.getTilknyttedeDokumenter().size() != versjon.getDokumenter().size()) {
            throw new IllegalStateException("Feil ved mapping: Klarte ikke å finne et av dokumentene saksbehandler har referert til.");
        }

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

        public String getEndretAv() {
            return endretAv;
        }

        public UUID getEndretBehandlingUuid() {
            return endretBehandlingUuid;
        }

        public String getEndretSaksnummer() {
            return endretSaksnummer;
        }

        public SykdomPerson getEndretForPerson() {
            return endretForPerson;
        }
    }
    
    public SykdomPeriodeMedEndringDto toSykdomPeriodeMedEndringDto(SykdomPeriodeMedEndring p) {
        return new SykdomPeriodeMedEndringDto(p.getPeriode(), p.isEndrerVurderingSammeBehandling(), p.isEndrerAnnenVurdering());
    }
}
