package no.nav.ung.sak.ytelse.ung.hendelsemottak;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.DoedsfallResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.FoedselsdatoResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjon;
import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjonRolle;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkResultResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.HentPersonQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.IdentGruppe;
import no.nav.k9.felles.integrasjon.pdl.IdentInformasjon;
import no.nav.k9.felles.integrasjon.pdl.IdentInformasjonResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;
import no.nav.k9.felles.integrasjon.pdl.Person;
import no.nav.k9.felles.integrasjon.pdl.PersonResponseProjection;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.ung.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;

abstract class PdlHendelseFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {


    private static final Logger logger = LoggerFactory.getLogger(PdlFødselFagsakTilVurderingUtleder.class);
    public static final Set<ForelderBarnRelasjonRolle> AKTUELLE_RELASJONSROLLER = Set.of(ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.FAR, ForelderBarnRelasjonRolle.MEDMOR);
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private PdlKlient pdlKlient;

    public PdlHendelseFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public PdlHendelseFagsakTilVurderingUtleder(FagsakRepository fagsakRepository,
                                                BehandlingRepository behandlingRepository,
                                                UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository, PdlKlient pdlKlient) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.pdlKlient = pdlKlient;
    }


    @Override
    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();
        var relevanteFagsaker = new HashSet<Fagsak>();
        var fagsakÅrsakMap = new HashMap<Fagsak, BehandlingÅrsakType>();

        for (AktørId aktør : aktører) {

            var personInfo = hentPersonInformasjon(aktør);
            var aktuellDato = finnAktuellDato(personInfo);

            // Sjekker om det gjelder dødshendelse for søker
            if (hendelse.getHendelseType().equals(HendelseType.PDL_DØDSFALL)) {
                var fagsakForAktør = hentRelevantFagsakForAktør(aktør, aktuellDato);
                if (fagsakForAktør.isPresent()) {
                    if (deltarIProgramPåHendelsedato(fagsakForAktør.get(), aktuellDato, hendelseId)) {
                        fagsakÅrsakMap.put(fagsakForAktør.get(), BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER);
                        relevanteFagsaker.add(fagsakForAktør.get());
                        break;
                    }
                }
            }

            // Sjekker om det gjelder fødsel eller dødshendelse for barn av søker
            var aktørIdenter = finnAktørIdForPersonerRelatertTil(personInfo);
            aktørIdenter.stream()
                .map(it -> hentRelevantFagsakForAktør(it, aktuellDato))
                .flatMap(Optional::stream)
                .filter(f -> deltarIProgramPåHendelsedato(f, aktuellDato, hendelseId))
                .forEach(relevanteFagsaker::add);
        }

        relevanteFagsaker.forEach(it -> fagsakÅrsakMap.put(it, getBehandlingÅrsakType()));
        return fagsakÅrsakMap;
    }

    abstract BehandlingÅrsakType getBehandlingÅrsakType();

    private Person hentPersonInformasjon(AktørId aktør) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktør.getAktørId());
        var projection = new PersonResponseProjection()
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .doedsfall(new DoedsfallResponseProjection().doedsdato())
            .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsRolle()
                .relatertPersonsIdent().minRolleForPerson());
        return pdlKlient.hentPerson(query, projection);
    }

    private Set<AktørId> finnAktørIdForPersonerRelatertTil(Person personFraPdl) {
        var relaterteIdenter = personFraPdl.getForelderBarnRelasjon()
            .stream()
            .filter(it -> AKTUELLE_RELASJONSROLLER.contains(it.getRelatertPersonsRolle()))
            .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
            .toList();

        var hentIdenterBolkQueryRequest = new HentIdenterBolkQueryRequest();
        hentIdenterBolkQueryRequest.setIdenter(relaterteIdenter);
        hentIdenterBolkQueryRequest.setGrupper(List.of(IdentGruppe.AKTORID));
        hentIdenterBolkQueryRequest.setHistorikk(true);
        var hentIdenterBolkProjection = new HentIdenterBolkResultResponseProjection().identer(
            new IdentInformasjonResponseProjection()
                .ident());
        var hentIdenterBolkResults = pdlKlient.hentIdenterBolkResults(hentIdenterBolkQueryRequest, hentIdenterBolkProjection);
        return hentIdenterBolkResults.stream()
            .flatMap(it -> it.getIdenter().stream().map(IdentInformasjon::getIdent))
            .map(AktørId::new)
            .collect(Collectors.toSet());
    }

    abstract LocalDate finnAktuellDato(Person personFraPdl);

    private Optional<Fagsak> hentRelevantFagsakForAktør(AktørId aktør, LocalDate relevantDato) {
        return fagsakRepository.hentForBruker(aktør).stream()
            .filter(f -> f.getYtelseType().equals(FagsakYtelseType.UNGDOMSYTELSE))
            .filter(f -> f.getPeriode().overlapper(relevantDato, AbstractLocalDateInterval.TIDENES_ENDE)).findFirst();
    }

    private boolean deltarIProgramPåHendelsedato(Fagsak fagsak, LocalDate relevantDato, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        var periodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
        if (periodeGrunnlag.isPresent()) {
            var harIngenPerioderEtterHendelseDato = periodeGrunnlag.get().getUngdomsprogramPerioder().getPerioder().stream().noneMatch(p -> p.getPeriode().getTomDato().isAfter(relevantDato));
            if (harIngenPerioderEtterHendelseDato) {
                logger.info("Datagrunnlag på behandling {} for {} hadde ingen perioder med ungdomsprogram etter hendelsedato. Trigget av hendelse {}.", behandling.getUuid(), fagsak.getSaksnummer(), hendelseId);
                return false;
            }
        }
        return true;
    }


}
