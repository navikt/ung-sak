package no.nav.k9.sak.ytelse.pleiepengerbarn.hendelsemottak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.k9.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.k9.sak.kontrakt.hendelser.Hendelse;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
@HendelseTypeRef("PDL_DØDSFALL")
public class DødsfallFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(DødsfallFagsakTilVurderingUtleder.class);
    public static final Set<FagsakYtelseType> RELEVANTE_YTELSER = Set.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE);
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private PersoninfoAdapter personinfoAdapter;

    public DødsfallFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public DødsfallFagsakTilVurderingUtleder(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository, PersonopplysningRepository personopplysningRepository, PersoninfoAdapter personinfoAdapter) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.personopplysningRepository = personopplysningRepository;
        this.personinfoAdapter = personinfoAdapter;
    }

    @Override
    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(AktørId aktørId, Hendelse hendelse) {
        List<AktørId> dødsfallAktører = hendelse.getHendelseInfo().getAktørIder();
        LocalDate dødsdatoFraHendelse = hendelse.getHendelsePeriode().getFom();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();
        LocalDateTime hendelseOpprettetTidspunkt = hendelse.getHendelseInfo().getOpprettet();

        var fagsaker = new HashMap<Fagsak, BehandlingÅrsakType>();

        for (FagsakYtelseType fagsakYtelseType : RELEVANTE_YTELSER) {
            for (AktørId aktør : dødsfallAktører) {
                LocalDate dødsdato = hentOppdatertDødsdato(aktør, dødsdatoFraHendelse, hendelseOpprettetTidspunkt, hendelseId);
                for (Fagsak fagsak : fagsakRepository.finnFagsakRelatertTil(fagsakYtelseType, aktør, null, null, dødsdato, null)) {
                    if (erNyInformasjonIHendelsen(fagsak, aktør, dødsdato, hendelseId)) {
                        fagsaker.put(fagsak, BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER);
                    }
                }
                for (Fagsak fagsak : fagsakRepository.finnFagsakRelatertTil(fagsakYtelseType, null, aktør, null, dødsdato, null)) {
                    if (erNyInformasjonIHendelsen(fagsak, aktør, dødsdato, hendelseId)) {
                        fagsaker.put(fagsak, BehandlingÅrsakType.RE_HENDELSE_DØD_BARN);
                    }
                }
            }
        }


        return fagsaker;
    }

    /**
     * idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     * hindrer også revurdering hvis hendelsen kommer etter at behandlingen er oppdatert med ny data.
     */
    private boolean erNyInformasjonIHendelsen(Fagsak fagsak, AktørId aktør, LocalDate dødsdato, String hendelseId) {
        Behandling behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
        PersonopplysningGrunnlagEntitet personopplysninger = personopplysningRepository.hentPersonopplysninger(behandling.getId());
        if (personopplysninger != null) {
            for (PersonopplysningEntitet personopplysning : personopplysninger.getGjeldendeVersjon().getPersonopplysninger()) {
                if (aktør.equals(personopplysning.getAktørId()) && Objects.equals(dødsdato, personopplysning.getDødsdato())) {
                    logger.info("Persondata på behandling {} for {} var allerede oppdatert med riktig dødsdato. Trigget av hendelse {}.", behandling.getUuid(), fagsak.getSaksnummer(), hendelseId);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Vi forventer å håndtere hendelser som er utdaterte, eksempelvis at topic leses på nytt fra start. Derfor gjør vi en sjekk
     * mot API-et fra PDL i tilfelle dødsdato skal være korrigert etter at hendelsen ble opprettet, for å unngå å feilaktig trigge revurdering.
     */
    private LocalDate hentOppdatertDødsdato(AktørId aktør, LocalDate dødsdatoFraHendelse, LocalDateTime hendelseOpprettetTidspunkt, String hendelseId) {
        Personinfo personinfo = personinfoAdapter.hentPersoninfo(aktør);
        LocalDate dødsdatoFraApi = personinfo.getDødsdato();

        if (dødsdatoFraApi == null) {
            logger.warn("Mottok dødshendelse {} opprettet {}, men API-et fra PDL har ikke registrert dødsfall på aktuell person. Går videre med ingen dødsdato.", hendelseId, hendelseOpprettetTidspunkt);
        } else if (!dødsdatoFraApi.equals(dødsdatoFraHendelse)) {
            logger.warn("Mottok dødshendelse {} opprettet {}, men API-et fra PDL har ikke registrert samme dato på aktuell person. Går videre med dato fra API-et.", hendelseId, hendelseOpprettetTidspunkt);
        }
        return dødsdatoFraApi;
    }

}
